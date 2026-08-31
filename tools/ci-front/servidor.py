#!/usr/bin/env python3
"""Front de solo lectura del CI. Lo expone el server del equipo.

NO ejecuta nada: la cola, los workers y la ejecucion los sigue poniendo GitHub
Actions sobre el runner self-hosted. Este servicio solo lee la API, normaliza la
respuesta y la sirve. Si se cae, el CI sigue andando igual.

La normalizacion es la decision de diseno importante: el front consume un formato
propio, no la respuesta cruda de GitHub. Por eso los datos de prueba y los reales
son indistinguibles para la pagina, y conectar la API no toca el front.

Sin dependencias: solo biblioteca estandar, para que corra en un python:alpine.

Configuracion por entorno:
    CI_REPO         owner/repo. Sin esto, sirve datos de prueba.
    CI_TOKEN        token de GitHub con permiso de lectura de Actions.
    CI_RUNNERS      cuantos runners hay instalados (default 1).
    CI_PUERTO       default 8099.
    CI_CACHE_S      segundos de cache de la API (default 15).
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

# El nombre de las etapas del gate, en el orden en que corren. Se usa para
# ordenar los steps que devuelve GitHub, que no garantiza orden.
ORDEN_ETAPAS = [
    "secretos", "ortografia", "markdownlint", "referencias", "links",
    "formato", "compila", "analisis_estatico", "idioma_codigo", "tests",
]

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


def _estado_paso(status, conclusion):
    if status == "queued":
        return "pendiente"
    if status == "in_progress":
        return "corriendo"
    if conclusion == "success":
        return "ok"
    if conclusion == "skipped":
        return "pendiente"
    if conclusion is None:
        return "pendiente"
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

        etapas = []
        # Los pasos solo existen una vez que el job arranco. Una corrida en cola
        # no tiene nada que mostrar todavia, y eso es informacion, no un error.
        if estado != "en_cola":
            try:
                jobs = _pedir("/repos/%s/actions/runs/%s/jobs" % (REPO, run["id"]))
                pasos = {}
                for job in jobs.get("jobs", []):
                    for paso in job.get("steps", []):
                        nombre = (paso.get("name") or "").strip().lower()
                        pasos[nombre] = paso
                for nombre in ORDEN_ETAPAS:
                    paso = pasos.get(nombre)
                    if not paso:
                        continue
                    etapas.append({
                        "nombre": nombre,
                        "estado": _estado_paso(paso.get("status"), paso.get("conclusion")),
                        "ms": _segundos(paso.get("started_at"), paso.get("completed_at")) * 1000,
                    })
            except (urllib.error.URLError, urllib.error.HTTPError, KeyError):
                pass  # Sin detalle de pasos igual se muestra la corrida.

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
            "etapas": etapas,
            "url": run.get("html_url"),
        })

    ocupados = sum(1 for c in corridas if c["estado"] == "corriendo")
    return {
        "fuente": "github",
        "repo": REPO,
        "runners": {"total": RUNNERS, "ocupados": ocupados},
        "corridas": corridas,
    }


def desde_prueba():
    with open(os.path.join(RUTA, "datos-prueba.json"), encoding="utf-8") as fh:
        datos = json.load(fh)
    datos.pop("_comentario", None)
    return datos


def obtener():
    """Datos con cache. El cache existe para no comerse el rate limit de GitHub."""
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

        datos["generado"] = time.strftime("%H:%M:%S")
        _cache["datos"] = datos
        _cache["ts"] = ahora
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
