#!/usr/bin/env python3
"""Front de solo lectura del CI. Lo expone el server del equipo.

NO ejecuta nada: la cola, los workers y la ejecucion los sigue poniendo GitHub
Actions sobre el runner self-hosted. Este servicio solo lee la API, normaliza la
respuesta y la sirve. Si se cae, el CI sigue andando igual.

La normalizacion es la decision de diseno importante: el front consume un formato
propio, no la respuesta cruda de GitHub. Por eso los datos de prueba y los reales
son indistinguibles para la pagina, y conectar la API no toca el front.

Sin dependencias: solo biblioteca estandar, para que corra en un python:alpine.

Hay DOS fuentes, y son independientes a proposito:

    GitHub    lo que se disparo con un push o un pull request.
    El buzon  lo que se corrio en el server con ./qa.sh --remoto, y ademas las
              corridas del runner, que tambien lo escriben.

El buzon es un directorio donde el gate deja un JSON por corrida (ver QA_SPOOL en
reportar.py). Existe porque la API de Actions no sabe nada de una corrida
`--remoto` --nunca paso por GitHub-- y porque de las que si conoce solo expone los
tres steps del workflow, no las 13 etapas del gate.

Por eso el detalle por etapa de LAS DOS listas sale del buzon: la de GitHub se
empareja con su registro por commit. A la API se le pide solo la lista de corridas,
un pedido por ventana de cache en vez de uno por corrida.

Configuracion por entorno:
    CI_REPO         owner/repo. Sin esto, sirve datos de prueba.
    CI_TOKEN        token de GitHub con permiso de lectura de Actions.
    CI_RUNNERS      cuantos runners hay instalados (default 1).
    CI_PUERTO       default 8099.
    CI_CACHE_S      segundos de cache de la API (default 15).
    CI_BUZON        directorio de registros (default /opt/TP-Pipelines/corridas).
    CI_BUZON_MAX    cuantas corridas del buzon se muestran (default 12).
"""

import json
import os
import ssl
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

RUTA = os.path.dirname(os.path.abspath(__file__))
ESTATICOS = os.path.join(RUTA, "static")

REPO = os.environ.get("CI_REPO", "").strip()
TOKEN = os.environ.get("CI_TOKEN", "").strip()
RUNNERS = int(os.environ.get("CI_RUNNERS", "1"))
PUERTO = int(os.environ.get("CI_PUERTO", "8099"))
CACHE_S = int(os.environ.get("CI_CACHE_S", "15"))
BUZON = os.environ.get("CI_BUZON", "/opt/TP-Pipelines/corridas")
BUZON_MAX = int(os.environ.get("CI_BUZON_MAX", "12"))

# El gate marca una etapa que no corrio como `omitida`; el front, que ademas
# muestra pasos de la API de GitHub, la llama `pendiente`. Son la misma casilla
# vacia y se dibujan igual, asi que se traduce en el borde y no en la pagina.
ESTADO_ETAPA = {
    "ok": "ok",
    "aviso": "aviso",
    "fallo": "fallo",
    "omitida": "pendiente",
    # La etapa que esta corriendo AHORA. Sin esta entrada caia en el default y se
    # dibujaba gris, como si no hubiera corrido: se veia que la corrida estaba en
    # curso, pero no en que etapa iba, que es la mitad del sentido de mirar esto.
    "corriendo": "corriendo",
}

ORIGENES = {"remoto": "qa.sh --remoto", "ci": "runner", "local": "qa.sh local"}

# Cuanto puede pasar sin que una corrida escriba nada antes de darla por muerta. El
# presupuesto mas largo del gate es 600 s para la corrida entera, asi que 600 s de
# silencio en una sola etapa ya es otra cosa.
CORRIDA_ZOMBI_S = 600

_cache = {"datos": None, "ts": 0.0}
_lock = threading.Lock()


# ---------------------------------------------------------------------------
# Traduccion: API de GitHub -> formato del front
# ---------------------------------------------------------------------------

def _estado_corrida(status, conclusion):
    if status == "queued":
        return "en_cola"
    if status in ("in_progress", "requested", "waiting"):
        return "corriendo"
    if conclusion == "success":
        return "ok"
    if conclusion in ("cancelled", "skipped"):
        return "cancelada"
    return "fallo"


def _segundos(desde, hasta):
    from datetime import datetime

    if not desde:
        return 0
    fmt = "%Y-%m-%dT%H:%M:%SZ"
    try:
        a = datetime.strptime(desde, fmt)
        b = datetime.strptime(hasta, fmt) if hasta else datetime.utcnow()
        return max(0, int((b - a).total_seconds()))
    except (ValueError, TypeError):
        return 0


def _pedir(ruta):
    url = "https://api.github.com" + ruta
    pedido = urllib.request.Request(url)
    pedido.add_header("Accept", "application/vnd.github+json")
    pedido.add_header("X-GitHub-Api-Version", "2022-11-28")
    pedido.add_header("User-Agent", "tpi-ci-front")
    if TOKEN:
        pedido.add_header("Authorization", "Bearer " + TOKEN)
    contexto = ssl.create_default_context()
    with urllib.request.urlopen(pedido, timeout=15, context=contexto) as r:
        return json.loads(r.read().decode("utf-8"))


def desde_github():
    """Trae las ultimas corridas y las traduce al formato del front."""
    crudo = _pedir("/repos/%s/actions/runs?per_page=12" % REPO)
    corridas = []

    for run in crudo.get("workflow_runs", []):
        estado = _estado_corrida(run.get("status"), run.get("conclusion"))
        commit = run.get("head_commit") or {}
        autor = commit.get("author") or {}

        # Las etapas NO salen de la API: las pone obtener() desde el buzon.
        #
        # GitHub expone los steps del workflow --checkout, elegir base, elegir
        # perfil, verificar-- y las 13 etapas del gate viven todas adentro del
        # ultimo. Preguntarle a la API por ellas devolvia una lista vacia.
        #
        # Ademas costaba un pedido POR CORRIDA: 13 por ventana de cache, unos 3120
        # por hora contra un limite de 5000. Ahora es uno solo, y el detalle que se
        # muestra es mejor que el que daba la API.
        corridas.append({
            "numero": run.get("run_number"),
            "estado": estado,
            "rama": run.get("head_branch") or "?",
            "commit": (run.get("head_sha") or "")[:7],
            "mensaje": (commit.get("message") or "").split("\n")[0][:90],
            # El nombre de git, que es lo que pidio el equipo: quien escribio el
            # codigo. El login de GitHub va aparte porque no siempre coinciden.
            "autor_git": autor.get("name") or "?",
            "autor_github": (run.get("actor") or {}).get("login") or "",
            "espera_s": _segundos(run.get("created_at"), run.get("run_started_at")),
            "duracion_s": _segundos(run.get("run_started_at"), run.get("updated_at")
                                    if estado not in ("corriendo", "en_cola") else None),
            "hallazgos": {"bloquea": 0, "avisa": 0},
            "etapas": [],
            "url": run.get("html_url"),
        })

    ocupados = sum(1 for c in corridas if c["estado"] == "corriendo")
    return {
        "fuente": "github",
        "repo": REPO,
        "runners": {"total": RUNNERS, "ocupados": ocupados},
        "corridas": corridas,
    }


def _corrida_de_registro(registro, visto_hace):
    """Un registro del gate, con la misma forma que una corrida de GitHub.

    La pagina dibuja las dos listas con el mismo componente. Si las formas se
    separan, el front pasa a tener que saber de donde vino cada cosa, que es
    justo lo que esta normalizacion evita.
    """
    hallazgos = registro.get("hallazgos") or {}
    mensaje = registro.get("invocacion") or "./qa.sh"
    if registro.get("sucio"):
        # Cambia como se lee el commit: en --remoto viaja el working tree, asi que
        # lo verificado puede no ser lo que dice ese hash.
        mensaje += "  ·  con cambios sin commitear"

    if registro.get("terminado", True):
        estado = "fallo" if registro.get("bloqueado") else "ok"
        duracion = registro.get("duracion_s", 0)
    else:
        # El gate reescribe el registro en cada cambio de etapa. Si hace rato que
        # nadie lo toca, esa corrida no esta corriendo: se colgo, la mataron o murio
        # el contenedor. Sin esto quedaria girando en la pantalla para siempre.
        estado = "cancelada" if visto_hace > CORRIDA_ZOMBI_S else "corriendo"
        # Mientras corre, lo que importa es hace cuanto arranco, no cuanto sumaron
        # las etapas que ya terminaron.
        duracion = max(0, int(time.time() - registro.get("iniciado", 0)))

    return {
        "numero": None,
        "origen": registro.get("origen", "local"),
        "etiqueta": ORIGENES.get(registro.get("origen"), "?"),
        "estado": estado,
        "rama": registro.get("rama", "?"),
        "commit": registro.get("commit", ""),
        "mensaje": mensaje,
        "autor_git": registro.get("usuario", "?"),
        "autor_github": "",
        "espera_s": 0,
        "duracion_s": duracion,
        "hallazgos": {
            "bloquea": hallazgos.get("bloquea", 0),
            "avisa": hallazgos.get("avisa", 0),
        },
        "etapas": [
            {
                "nombre": etapa.get("nombre", "?"),
                "estado": ESTADO_ETAPA.get(etapa.get("estado"), "pendiente"),
                "ms": etapa.get("ms", 0),
            }
            for etapa in registro.get("etapas", [])
        ],
        "url": None,
    }


def desde_buzon():
    """Las ultimas corridas que dejaron rastro en el server, mas nuevas primero.

    Tolera que el buzon no exista (devuelve vacio) y que un archivo este roto (lo
    saltea). Es una lista de mas: no puede tumbar la pagina.
    """
    try:
        archivos = [a for a in os.listdir(BUZON) if a.endswith(".json")]
    except OSError:
        return []

    corridas = []
    # El nombre arranca con el epoch, asi que ordenar por nombre es ordenar por
    # fecha sin abrir un solo archivo.
    ahora = time.time()
    for archivo in sorted(archivos, reverse=True)[:BUZON_MAX]:
        ruta = os.path.join(BUZON, archivo)
        try:
            with open(ruta, encoding="utf-8") as fh:
                registro = json.load(fh)
            corridas.append(
                _corrida_de_registro(registro, ahora - os.path.getmtime(ruta))
            )
        except (OSError, ValueError):
            continue
    return corridas


def desde_prueba():
    with open(os.path.join(RUTA, "datos-prueba.json"), encoding="utf-8") as fh:
        datos = json.load(fh)
    datos.pop("_comentario", None)
    return datos


def _github_con_cache():
    """La parte que SI necesita cache: la API de GitHub tiene rate limit."""
    with _lock:
        ahora = time.time()
        if _cache["datos"] and ahora - _cache["ts"] < CACHE_S:
            return _cache["datos"]

        if REPO and TOKEN:
            try:
                datos = desde_github()
                datos["aviso"] = None
            except Exception as error:  # noqa: BLE001
                # Degradar a datos de prueba seria mentir: mejor decir que la API
                # no responde y mostrar lo ultimo que se pudo leer.
                datos = _cache["datos"] or desde_prueba()
                datos["aviso"] = "No se pudo leer la API de GitHub: %s" % error
        else:
            datos = desde_prueba()
            datos["aviso"] = (
                "Datos de prueba. Configura CI_REPO y CI_TOKEN para leer las "
                "corridas reales."
            )

        _cache["datos"] = datos
        _cache["ts"] = ahora
        return datos


def obtener():
    """Lo que consume la pagina: la API cacheada, y el buzon SIN cache.

    El buzon se lee en cada pedido a proposito. Es un directorio local, no tiene
    rate limit que cuidar, y meterlo en el cache de 15 s arruinaria justamente lo
    que hace util esta pantalla: una corrida `rapido` dura unos 9 segundos, asi que
    entraria entera en una sola ventana de cache y se veria ya terminada, sin
    etapas pasando. Sin cache, la unica demora es el refresco de la pagina.

    Ademas es independiente de GitHub: una corrida `--remoto` existe aunque no haya
    token y aunque la API este caida.
    """
    # Copia: sin esto, el aviso que se arma aca quedaria pegado al dict cacheado.
    datos = dict(_github_con_cache())

    corridas_qa = desde_buzon()

    # Las etapas de una corrida de GitHub salen del registro que esa misma corrida
    # dejo en el buzon, emparejado por commit. El runner escribe el registro
    # mientras corre, asi que la lista de GitHub tambien avanza etapa por etapa, y
    # sin gastar un pedido de API por corrida.
    #
    # El join va aca y no adentro del cache: si no, las etapas quedarian congeladas
    # 15 segundos y se perderia justamente el avance en vivo.
    # Los contadores de hallazgos viajan con las etapas: la API tampoco los expone,
    # y el registro los tiene.
    por_commit = {}
    for corrida in corridas_qa:
        if corrida["origen"] == "ci" and corrida["commit"]:
            # desde_buzon() viene de la mas nueva a la mas vieja, asi que la
            # primera de cada commit es la corrida mas reciente sobre el.
            por_commit.setdefault(
                corrida["commit"][:7],
                {"etapas": corrida["etapas"], "hallazgos": corrida["hallazgos"]},
            )

    if por_commit:
        # Copias de cada corrida: las de adentro son el mismo objeto que quedo en
        # el cache, y escribirles encima lo iria ensuciando corrida tras corrida.
        datos["corridas"] = [
            dict(c, **por_commit[(c.get("commit") or "")[:7]])
            if (c.get("commit") or "")[:7] in por_commit
            else c
            for c in datos.get("corridas", [])
        ]
    if corridas_qa:
        datos["corridas_qa"] = corridas_qa
        if not (REPO and TOKEN):
            # Mezclar corridas reales con inventadas sin decirlo seria la peor
            # version de esta pantalla.
            datos["aviso"] = (
                "Las corridas de GitHub son datos de prueba: faltan CI_REPO y "
                "CI_TOKEN. Las del server son reales."
            )
    else:
        datos.setdefault("corridas_qa", [])

    datos["generado"] = time.strftime("%H:%M:%S")
    return datos


# ---------------------------------------------------------------------------
# HTTP
# ---------------------------------------------------------------------------

class Manejador(BaseHTTPRequestHandler):
    def _responder(self, codigo, cuerpo, tipo):
        self.send_response(codigo)
        self.send_header("Content-Type", tipo)
        self.send_header("Content-Length", str(len(cuerpo)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(cuerpo)

    def do_GET(self):
        ruta = self.path.split("?")[0]

        if ruta == "/api/corridas":
            cuerpo = json.dumps(obtener(), ensure_ascii=False).encode("utf-8")
            return self._responder(200, cuerpo, "application/json; charset=utf-8")

        if ruta == "/salud":
            return self._responder(200, b"ok", "text/plain")

        archivo = "index.html" if ruta == "/" else ruta.lstrip("/")
        destino = os.path.normpath(os.path.join(ESTATICOS, archivo))
        if not destino.startswith(ESTATICOS) or not os.path.isfile(destino):
            return self._responder(404, b"no encontrado", "text/plain")

        tipos = {".html": "text/html; charset=utf-8", ".css": "text/css",
                 ".js": "application/javascript", ".svg": "image/svg+xml"}
        _, ext = os.path.splitext(destino)
        with open(destino, "rb") as fh:
            return self._responder(200, fh.read(), tipos.get(ext, "text/plain"))

    def log_message(self, formato, *args):
        pass  # Sin log de acceso: no aporta y ensucia la salida del contenedor.


def main():
    fuente = "GitHub (%s)" % REPO if REPO and TOKEN else "datos de prueba"
    print("front del CI en http://0.0.0.0:%d  ·  fuente: %s" % (PUERTO, fuente), flush=True)
    ThreadingHTTPServer(("0.0.0.0", PUERTO), Manejador).serve_forever()


if __name__ == "__main__":
    main()
