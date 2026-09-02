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


def _es_nuestro(ruta):
    """Si el workflow cae fuera de owned-paths.txt, no es asunto nuestro.

    Hoy no cambia nada: el repo es del equipo y todos los workflows son nuestros.
    Importa el dia que esto viva en el monorepo de la materia, donde los otros
    equipos van a tener sus propios workflows con `runs-on: ubuntu-latest` --que
    para ellos es lo normal-- y este chequeo bloquea. Sin este filtro, nuestras
    corridas se pondrian rojas por archivos ajenos, y sobre una decision que ya no
    seria nuestra: si el repo gasta minutos de Actions no lo decidimos nosotros.

    Si scope.py no esta disponible, se revisa todo: es el comportamiento de
    siempre, y este chequeo no puede quedarse sin correr.
    """
    try:
        import scope
    except ImportError:
        return True
    return scope.es_propio(ruta, scope.globs_propios())


def revisar(directorio=DIRECTORIO, solo_nuestros=True):
    hallazgos = []
    if not os.path.isdir(directorio):
        return hallazgos

    for nombre in sorted(os.listdir(directorio)):
        if not nombre.endswith((".yml", ".yaml")):
            continue
        ruta = os.path.join(directorio, nombre).replace(os.sep, "/")
        if solo_nuestros and not _es_nuestro(ruta):
            continue
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
    # Cuando alguien apunta a un directorio explicito, se revisa TODO lo que hay
    # adentro: el filtro de propiedad es para la corrida normal, y los fixtures
    # del self-test estan justamente excluidos de owned-paths.
    explicito = len(sys.argv) > 1
    directorio = sys.argv[1] if explicito else DIRECTORIO
    for hallazgo in revisar(directorio, solo_nuestros=not explicito):
        sys.stdout.write(json.dumps(hallazgo, ensure_ascii=False) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
