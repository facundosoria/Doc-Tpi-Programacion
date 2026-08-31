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
import sys

RUTA_LIB = os.path.dirname(os.path.abspath(__file__))
RUTA_CONFIG = os.path.join(os.path.dirname(RUTA_LIB), "config")

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

    def consumir(self, evento):
        tipo = evento.get("ev")
        if tipo == "etapa_ini":
            etapa = evento["etapa"]
            if etapa not in self.etapas:
                self.etapas.append(etapa)
            self.estado.setdefault(etapa, "omitida")
        elif tipo == "hallazgo":
            self.hallazgos.append(evento)
        elif tipo == "etapa_fin":
            etapa = evento["etapa"]
            if etapa not in self.etapas:
                self.etapas.append(etapa)
            self.estado[etapa] = evento.get("estado", "omitida")
            self.duracion[etapa] = evento.get("ms", 0)

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


def main():
    reporte = Reporte()
    for linea in sys.stdin:
        linea = linea.strip()
        if not linea:
            continue
        try:
            reporte.consumir(json.loads(linea))
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

    return 1 if reporte.bloqueado() else 0


if __name__ == "__main__":
    sys.exit(main())
