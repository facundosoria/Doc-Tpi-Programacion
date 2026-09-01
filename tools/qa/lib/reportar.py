#!/usr/bin/env python3
"""Convierte el stream de eventos del motor en las dos vistas que consume la gente.

El motor no imprime texto libre: emite un JSON por linea. Este modulo es el unico
que sabe como se ve un hallazgo. De ahi salen la consola y el resumen Markdown sin
duplicar logica, y por eso ./qa.sh local y el CI nunca muestran cosas distintas.

Eventos que entiende:
    {"ev":"etapa_ini","etapa":"tests"}
    {"ev":"hallazgo","etapa":"tests","nivel":"bloquea","archivo":"X.java",
     "linea":87,"regla":"assertion-failed","detalle":"esperado 72, obtenido 65"}
    {"ev":"etapa_fin","etapa":"tests","estado":"fallo","ms":21000}
"""

import json
import os
import subprocess
import sys
import time

RUTA_LIB = os.path.dirname(os.path.abspath(__file__))
RUTA_CONFIG = os.path.join(os.path.dirname(RUTA_LIB), "config")

# Cuantos dias sobreviven los registros del buzon. Un directorio que solo crece es
# una bomba de tiempo en un server que nadie mira: el barrido va en la escritura,
# que es el unico momento en que sabemos que alguien esta usando esto.
DIAS_BUZON = 7

# Cuando arranco esta corrida. Se toma al importar, que es cuando el orquestador
# arranca del otro lado del pipe.
INICIO = time.time()

# El registro de una corrida se reescribe muchas veces --una por etapa, para que el
# front la vea avanzar-- y todas tienen que caer en el MISMO archivo, o el front
# mostraria trece corridas en vez de una. El contexto (rama, commit, usuario) se
# resuelve una sola vez: son subprocesos de git y no cambian a mitad de corrida.
_BUZON = {"archivo": None, "contexto": None}

# Un chequeo puede terminar de cuatro formas, y confundir "omitida" con "ok" es el
# error mas peligroso de un CI: una etapa que no corrio no dijo que todo estaba
# bien. Por eso llevan simbolo distinto y se reportan aparte.
SIMBOLOS = {
    "ok": ("v", "\033[32m"),
    "fallo": ("x", "\033[31m"),
    "aviso": ("!", "\033[33m"),
    "omitida": ("-", "\033[90m"),
}
RESET = "\033[0m"


def _color_activo():
    return sys.stdout.isatty() and os.environ.get("NO_COLOR") is None


def cargar_reglas():
    """Mapa regla -> {que_paso, arreglo}. Sin PyYAML usable, degrada a vacio."""
    ruta = os.path.join(RUTA_CONFIG, "reglas.yml")
    try:
        import yaml

        with open(ruta, encoding="utf-8") as fh:
            return yaml.safe_load(fh) or {}
    except Exception:
        return {}


class Reporte:
    def __init__(self):
        self.etapas = []
        self.estado = {}
        self.duracion = {}
        self.hallazgos = []
        self.reglas = cargar_reglas()
        # Que etapa esta corriendo ahora mismo. Solo lo usa el registro del buzon:
        # `estado` no se toca para que la consola y el Markdown no tengan que
        # conocer un estado que, cuando ellos se imprimen, ya no existe.
        self.en_curso = None

    def consumir(self, evento):
        tipo = evento.get("ev")
        if tipo == "etapa_ini":
            etapa = evento["etapa"]
            if etapa not in self.etapas:
                self.etapas.append(etapa)
            self.estado.setdefault(etapa, "omitida")
            self.en_curso = etapa
        elif tipo == "hallazgo":
            self.hallazgos.append(evento)
        elif tipo == "etapa_fin":
            etapa = evento["etapa"]
            if etapa not in self.etapas:
                self.etapas.append(etapa)
            self.estado[etapa] = evento.get("estado", "omitida")
            self.duracion[etapa] = evento.get("ms", 0)
            if self.en_curso == etapa:
                self.en_curso = None

    def _diagnostico(self, hallazgo):
        """Que paso y como se arregla, desde reglas.yml.

        Una regla sin entrada cae a un mensaje generico con el codigo crudo: sirve
        para lo raro, pero lo frecuente deberia estar mapeado.
        """
        entrada = self.reglas.get(hallazgo.get("regla", "")) or {}
        que_paso = (
            entrada.get("que_paso")
            or hallazgo.get("detalle")
            or hallazgo.get("regla", "sin detalle")
        )
        return que_paso, entrada.get("arreglo")

    def consola(self, salida=sys.stdout):
        usar_color = _color_activo()
        salida.write("\n")

        for etapa in self.etapas:
            estado = self.estado.get(etapa, "omitida")
            simbolo, color = SIMBOLOS[estado]
            ms = self.duracion.get(etapa)
            tiempo = "%.0fs" % (ms / 1000.0) if ms else "-"
            cuenta = sum(1 for h in self.hallazgos if h.get("etapa") == etapa)
            if estado == "omitida":
                resumen = "no ejecutada"
            elif cuenta:
                resumen = "%d hallazgo%s" % (cuenta, "s" if cuenta != 1 else "")
            else:
                resumen = ""

            linea = "  %s %-20s %6s   %s" % (simbolo, etapa, tiempo, resumen)
            if usar_color:
                linea = color + linea + RESET
            salida.write(linea.rstrip() + "\n")

        if self.hallazgos:
            salida.write("\n")
            for h in self.hallazgos:
                self._hallazgo_consola(h, salida, usar_color)

        salida.write("\n")

    def _hallazgo_consola(self, h, salida, usar_color):
        que_paso, arreglo = self._diagnostico(h)
        clave = "fallo" if h.get("nivel") == "bloquea" else "aviso"
        color = SIMBOLOS[clave][1]

        ubicacion = h.get("archivo", "?")
        if h.get("linea"):
            ubicacion = "%s:%s" % (ubicacion, h["linea"])

        cabecera = "  [%s] %s" % (h.get("nivel", "avisa"), ubicacion)
        if usar_color:
            cabecera = color + cabecera + RESET
        salida.write(cabecera + "\n")
        salida.write("      " + que_paso + "\n")
        if h.get("detalle") and h["detalle"] != que_paso:
            salida.write("      " + str(h["detalle"]) + "\n")
        if arreglo:
            salida.write("      -> " + arreglo + "\n")
        salida.write("\n")

    def markdown(self):
        iconos = {"ok": "OK", "fallo": "FALLA", "aviso": "AVISA", "omitida": "-"}
        lineas = [
            "## Gate de calidad",
            "",
            "| | Etapa | Tiempo | Hallazgos |",
            "|---|---|---|---|",
        ]

        for etapa in self.etapas:
            estado = self.estado.get(etapa, "omitida")
            ms = self.duracion.get(etapa)
            tiempo = "%.0fs" % (ms / 1000.0) if ms else "-"
            cuenta = sum(1 for h in self.hallazgos if h.get("etapa") == etapa)
            if estado == "omitida":
                detalle = "no ejecutada"
            else:
                detalle = str(cuenta) if cuenta else "-"
            lineas.append(
                "| %s | %s | %s | %s |" % (iconos[estado], etapa, tiempo, detalle)
            )

        if self.hallazgos:
            lineas += ["", "## Hallazgos", ""]
            for h in self.hallazgos:
                que_paso, arreglo = self._diagnostico(h)
                ubicacion = h.get("archivo", "?")
                if h.get("linea"):
                    ubicacion = "%s:%s" % (ubicacion, h["linea"])
                lineas.append("**`%s`** - %s" % (ubicacion, h.get("nivel", "avisa")))
                lineas.append("")
                lineas.append("- Que paso: " + que_paso)
                if h.get("detalle") and h["detalle"] != que_paso:
                    lineas.append("- Detalle: `%s`" % h["detalle"])
                if arreglo:
                    lineas.append("- Arreglo: " + arreglo)
                lineas.append("")

        return "\n".join(lineas) + "\n"

    def bloqueado(self):
        return any(h.get("nivel") == "bloquea" for h in self.hallazgos)

    def registro(self, terminado=True):
        """El mismo dato que el Markdown, en forma de maquina.

        Existe para el buzon del server: el front del CI necesita saber que etapa
        corrio y cual no, y hasta ahora eso moria en el resumen.

        La lista trae SIEMPRE todas las etapas, incluidas las omitidas. Es la razon
        de ser de esto: sin el `no ejecutada` explicito, una corrida `rapido` y una
        `completo` se ven iguales, y una etapa que no corrio no dijo que todo
        estaba bien.
        """
        etapas = [
            {
                "nombre": etapa,
                "estado": "corriendo"
                if etapa == self.en_curso
                else self.estado.get(etapa, "omitida"),
                "ms": self.duracion.get(etapa, 0),
                "hallazgos": sum(
                    1 for h in self.hallazgos if h.get("etapa") == etapa
                ),
            }
            for etapa in self.etapas
        ]
        return {
            "version": 1,
            "terminado": terminado,
            "etapas": etapas,
            "hallazgos": {
                "bloquea": sum(
                    1 for h in self.hallazgos if h.get("nivel") == "bloquea"
                ),
                "avisa": sum(
                    1 for h in self.hallazgos if h.get("nivel") != "bloquea"
                ),
            },
            "bloqueado": self.bloqueado(),
            "duracion_s": round(sum(self.duracion.values()) / 1000.0),
        }


def _git(*argumentos):
    """Un dato de git, o cadena vacia. Nunca revienta: esto es telemetria."""
    try:
        salida = subprocess.run(
            ["git"] + list(argumentos),
            capture_output=True,
            text=True,
            timeout=5,
        )
        return salida.stdout.strip() if salida.returncode == 0 else ""
    except Exception:  # noqa: BLE001
        return ""


def escribir_en_buzon(registro, final=True):
    """Deja el registro en QA_SPOOL, si esa variable apunta a un directorio real.

    Se llama muchas veces por corrida: una por cada etapa que arranca o termina, y
    una al final. Asi el front puede mostrar la corrida avanzando en vez de que
    aparezca entera recien cuando termino. Siempre sobre el mismo archivo.

    Las dos puntas que corren en el server --el runner y `./qa.sh --remoto`--
    escriben el MISMO formato en el MISMO lugar. Por eso el front puede ponerlas
    una al lado de la otra y comparar etapa contra etapa sin traducir nada.

    Es telemetria, no parte del veredicto: cualquier error se traga. Que el buzon
    no exista, este lleno o venga montado de solo lectura no puede cambiar el
    resultado de una corrida.
    """
    buzon = os.environ.get("QA_SPOOL", "").strip()
    if not buzon or not os.path.isdir(buzon):
        return

    try:
        if _BUZON["contexto"] is None:
            _BUZON["contexto"] = {
                # El origen viene de afuera porque adentro del contenedor no hay
                # forma de distinguir un runner de un `--remoto`: los dos son el
                # mismo docker run en el mismo host.
                "origen": os.environ.get("QA_ORIGEN")
                or ("ci" if os.environ.get("CI") else "local"),
                "usuario": os.environ.get("QA_USUARIO") or "?",
                "invocacion": os.environ.get("QA_INVOCACION", "").strip() or "./qa.sh",
                "rama": _git("rev-parse", "--abbrev-ref", "HEAD") or "?",
                "commit": _git("rev-parse", "--short", "HEAD"),
                # Lo que mas se malinterpreta de `--remoto`: viaja el working tree,
                # asi que el commit dice poco si ademas habia cambios sin commitear.
                "sucio": bool(_git("status", "--porcelain")),
                "iniciado": int(INICIO),
            }
        registro.update(_BUZON["contexto"])

        if _BUZON["archivo"] is None:
            _BUZON["archivo"] = "%d-%s-%d.json" % (
                INICIO,
                registro["origen"],
                os.getpid(),
            )
        destino = os.path.join(buzon, _BUZON["archivo"])
        # Se escribe aparte y se renombra: el front lee este directorio cada pocos
        # segundos y un JSON a medio escribir le explotaria en la cara.
        parcial = destino + ".parcial"
        with open(parcial, "w", encoding="utf-8") as fh:
            json.dump(registro, fh, ensure_ascii=False)
        os.replace(parcial, destino)

        if not final:
            return

        # El barrido va archivo por archivo porque el buzon tiene el sticky bit,
        # como remoto/: cada uno puede borrar lo suyo y nada mas. Un permiso
        # denegado sobre el registro de otra persona no puede cortar la limpieza
        # de los propios.
        limite = time.time() - DIAS_BUZON * 86400
        for archivo in os.listdir(buzon):
            # Los .parcial son de una corrida que murio entre el volcado y el
            # renombre. No los lee nadie, pero sin barrerlos se acumulan para
            # siempre.
            if not archivo.endswith((".json", ".parcial")):
                continue
            try:
                ruta = os.path.join(buzon, archivo)
                if os.path.getmtime(ruta) < limite:
                    os.remove(ruta)
            except OSError:
                continue
    except Exception:  # noqa: BLE001
        pass


def main():
    reporte = Reporte()
    # readline en vez de iterar sys.stdin: iterar lee de a bloques, y con eso los
    # eventos llegarian todos juntos al final. El motor los emite con flush
    # justamente para que se puedan seguir en vivo.
    for linea in iter(sys.stdin.readline, ""):
        linea = linea.strip()
        if not linea:
            continue
        try:
            evento = json.loads(linea)
            reporte.consumir(evento)
            # Una escritura por cambio de etapa, no por hallazgo: son trece etapas
            # y pueden ser cientos de hallazgos.
            if evento.get("ev") in ("etapa_ini", "etapa_fin"):
                escribir_en_buzon(reporte.registro(terminado=False), final=False)
        except json.JSONDecodeError:
            # Ruido de una herramienta que escribio fuera del protocolo: se muestra
            # tal cual en vez de tragarselo, para que no se pierda un error real.
            sys.stderr.write(linea + "\n")

    reporte.consola()
    md = reporte.markdown()

    local = os.path.join(os.getcwd(), ".qa", "resumen.md")
    os.makedirs(os.path.dirname(local), exist_ok=True)
    with open(local, "w", encoding="utf-8") as fh:
        fh.write(md)

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as fh:
            fh.write(md)

    escribir_en_buzon(reporte.registro())

    return 1 if reporte.bloqueado() else 0


if __name__ == "__main__":
    sys.exit(main())
