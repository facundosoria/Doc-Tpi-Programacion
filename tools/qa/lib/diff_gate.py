#!/usr/bin/env python3
"""Hace que un chequeo global bloquee solo lo que ESTE cambio rompio.

Filtrar por archivo modificado tiene un agujero real en un repo de documentacion
cruzada: renombras un titulo en tu documento y rompes un ancla en un documento que
no tocaste. Con 198 links internos entre 15 archivos, va a pasar, y es tu cambio el
que lo rompio aunque el archivo roto no aparezca en tu diff.

La solucion es correr el chequeo dos veces y comparar:

    en HEAD (con tu working tree)  ->  hallazgos actuales
    en el merge-base               ->  hallazgos que ya existian

    nuevo      -> bloquea. Es tuyo.
    preexiste  -> informativo. Deuda vieja, se paga cuando se toque el archivo.

El arbol de la base se materializa con `git worktree`, que no toca tu working tree.
El chequeo corre con la configuracion ACTUAL sobre el contenido VIEJO: los scripts
resuelven su config por __file__, y solo el cwd cambia.

Uso:
    diff_gate.py --etapa referencias -- py lib/check_refs.py
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile


def _git(*args, **kwargs):
    return subprocess.run(
        ["git"] + list(args), capture_output=True, text=True, check=False, **kwargs
    )


def base_de_comparacion():
    if os.environ.get("QA_BASE"):
        return os.environ["QA_BASE"]
    upstream = _git("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}").stdout.strip()
    if upstream:
        base = _git("merge-base", upstream, "HEAD").stdout.strip()
        if base:
            return base
    for candidata in ("origin/main", "origin/master", "main", "master"):
        base = _git("merge-base", candidata, "HEAD").stdout.strip()
        if base:
            return base
    return ""


def correr(comando, cwd=None):
    """Ejecuta un chequeo y devuelve sus hallazgos. Un fallo se propaga como vacio.

    Devolver vacio ante un error es deliberado: si el chequeo se rompe en la base,
    lo peor que pasa es que todo se considere nuevo y se reporte de mas. Al reves
    -tragarse hallazgos actuales- pasaria algo roto en verde.
    """
    try:
        salida = subprocess.run(
            comando, capture_output=True, text=True, check=False, cwd=cwd
        )
    except OSError:
        return []

    hallazgos = []
    for linea in salida.stdout.splitlines():
        linea = linea.strip()
        if not linea:
            continue
        try:
            hallazgos.append(json.loads(linea))
        except json.JSONDecodeError:
            continue
    return hallazgos


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--etapa", required=True)
    parser.add_argument("comando", nargs=argparse.REMAINDER)
    args = parser.parse_args()

    comando = [a for a in args.comando if a != "--"]
    if not comando:
        sys.stderr.write("diff_gate: falta el comando a ejecutar\n")
        return 2

    actuales = correr(comando)

    base = base_de_comparacion()
    previos = set()
    if base:
        temporal = tempfile.mkdtemp(prefix="qa-base-")
        arbol = os.path.join(temporal, "arbol")
        creado = _git("worktree", "add", "--detach", "--quiet", arbol, base)
        if creado.returncode == 0:
            previos = {h.get("id") for h in correr(comando, cwd=arbol)}
            _git("worktree", "remove", "--force", arbol)
        shutil.rmtree(temporal, ignore_errors=True)

    nuevos = 0
    for hallazgo in actuales:
        if hallazgo.get("id") in previos:
            # Ya estaba roto antes de tu cambio: se muestra, no frena.
            hallazgo["nivel"] = "avisa"
            hallazgo["detalle"] = (hallazgo.get("detalle") or "") + " (ya existia)"
        else:
            nuevos += 1
        sys.stdout.write(json.dumps(hallazgo, ensure_ascii=False) + "\n")

    sys.stderr.write(
        "%s: %d hallazgos, %d nuevos con este cambio\n"
        % (args.etapa, len(actuales), nuevos)
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
