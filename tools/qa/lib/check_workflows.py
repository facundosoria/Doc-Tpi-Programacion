#!/usr/bin/env python3
"""Impide que un workflow consuma minutos de GitHub Actions.

El equipo decidio correr todo en un runner self-hosted, que es gratis e ilimitado
en cualquier plan. Alcanza con que alguien escriba `runs-on: ubuntu-latest` en un
workflow para que ese trabajo pase a ejecutarse en las maquinas de GitHub y empiece
a descontar de los 2000 minutos mensuales del repositorio privado.

Es el tipo de error que nadie nota hasta que los minutos se acaban a mitad de mes y
el CI deja de correr para todo el equipo. Un grep bien puesto lo evita.

Emite un JSON por linea, el mismo protocolo que el resto del motor.
"""

import json
import os
import re
import sys

ETAPA = "workflows"
DIRECTORIO = ".github/workflows"

# Las etiquetas de maquinas provistas por GitHub. `self-hosted` y las etiquetas
# propias del equipo no estan aca a proposito: esas son las que queremos.
HOSPEDADOS = re.compile(
    r"^\s*runs-on:\s*\[?\s*[\"']?"
    r"(ubuntu|windows|macos)[-\w.]*"
    r"[\"']?",
    re.IGNORECASE,
)

RE_RUNS_ON = re.compile(r"^\s*runs-on:", re.IGNORECASE)


def revisar(directorio=DIRECTORIO):
    hallazgos = []
    if not os.path.isdir(directorio):
        return hallazgos

    for nombre in sorted(os.listdir(directorio)):
        if not nombre.endswith((".yml", ".yaml")):
            continue
        ruta = os.path.join(directorio, nombre).replace(os.sep, "/")
        try:
            with open(ruta, encoding="utf-8") as fh:
                lineas = fh.readlines()
        except (OSError, UnicodeDecodeError):
            continue

        for numero, linea in enumerate(lineas, 1):
            if not RE_RUNS_ON.match(linea):
                continue
            if HOSPEDADOS.match(linea):
                etiqueta = linea.split(":", 1)[1].strip()
                hallazgos.append({
                    "ev": "hallazgo",
                    "etapa": ETAPA,
                    "nivel": "bloquea",
                    "archivo": ruta,
                    "linea": numero,
                    "regla": "runner-hospedado",
                    "detalle": "runs-on: %s consume minutos de GitHub" % etiqueta,
                    "id": "%s|%d|runner-hospedado" % (ruta, numero),
                })

    return hallazgos


def main():
    # Acepta un directorio para que el self-test pueda apuntarle a su fixture.
    directorio = sys.argv[1] if len(sys.argv) > 1 else DIRECTORIO
    for hallazgo in revisar(directorio):
        sys.stdout.write(json.dumps(hallazgo, ensure_ascii=False) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
