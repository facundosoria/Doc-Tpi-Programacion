#!/usr/bin/env python3
"""Decide sobre que archivos corre el gate.

Tres filtros encadenados, en este orden:

1. DIFF      Solo lo que cambio. La deuda vieja no bloquea a nadie y se paga
             cuando se toca el archivo (el modelo "Clean as You Code").
2. PROPIEDAD Solo lo que es nuestro. Lo que cae fuera de owned-paths.txt se
             reporta como informativo pero nunca bloquea, ni aunque lo arrastre
             un merge.
3. RUTEO     Cada archivo al chequeo que le corresponde, por extension y ruta.

Uso:
    scope.py archivos [--all]     lista los archivos en alcance, uno por linea
    scope.py ruteo    [--all]     JSON {chequeo: [archivos]}
"""

import json
import os
import re
import subprocess
import sys

RUTA_LIB = os.path.dirname(os.path.abspath(__file__))
RUTA_QA = os.path.dirname(RUTA_LIB)
RUTA_CONFIG = os.path.join(RUTA_QA, "config")


def _git(*args):
    try:
        salida = subprocess.run(
            ["git"] + list(args),
            capture_output=True,
            text=True,
            check=False,
        )
        return salida.stdout.strip() if salida.returncode == 0 else ""
    except OSError:
        return ""


def raiz():
    return _git("rev-parse", "--show-toplevel") or os.getcwd()


def base_de_comparacion():
    """Contra que se compara el diff.

    En un PR, el CI define QA_BASE. En local, el upstream de la rama si existe;
    si no, el merge-base con origin/main, que es el caso de una rama recien
    creada que todavia no se pusheo.
    """
    if os.environ.get("QA_BASE"):
        return os.environ["QA_BASE"]

    upstream = _git("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}")
    if upstream:
        base = _git("merge-base", upstream, "HEAD")
        if base:
            return base

    for candidata in ("origin/main", "origin/master", "main", "master"):
        base = _git("merge-base", candidata, "HEAD")
        if base:
            return base

    return ""


def archivos_cambiados():
    """Lo commiteado desde la base, mas lo que este sin commitear.

    Incluir el working tree importa: el gate se corre a mitad de una tarea, que es
    justo cuando todavia no commiteaste nada.
    """
    encontrados = []
    base = base_de_comparacion()

    if base:
        encontrados += _git("diff", "--name-only", base, "HEAD").splitlines()

    encontrados += _git("diff", "--name-only", "HEAD").splitlines()
    encontrados += _git("ls-files", "--others", "--exclude-standard").splitlines()

    vistos = set()
    unicos = []
    for archivo in encontrados:
        if archivo and archivo not in vistos and os.path.exists(archivo):
            vistos.add(archivo)
            unicos.append(archivo)
    return unicos


def todos_los_archivos():
    """Trackeados MAS los no trackeados que no estan ignorados.

    Solo `git ls-files` seria un bug silencioso: el codigo recien agregado y
    todavia sin commitear quedaria fuera de --all, y el CI reportaria verde sobre
    archivos que nunca miro.
    """
    encontrados = _git("ls-files").splitlines()
    encontrados += _git("ls-files", "--others", "--exclude-standard").splitlines()

    vistos = set()
    unicos = []
    for archivo in encontrados:
        if archivo and archivo not in vistos and os.path.exists(archivo):
            vistos.add(archivo)
            unicos.append(archivo)
    return unicos


def globs_propios():
    """Devuelve (incluidos, excluidos). Una linea con '!' delante excluye."""
    ruta = os.path.join(RUTA_CONFIG, "proyecto", "owned-paths.txt")
    incluidos, excluidos = [], []
    if os.path.exists(ruta):
        with open(ruta, encoding="utf-8") as fh:
            for linea in fh:
                linea = linea.strip()
                if not linea or linea.startswith("#"):
                    continue
                if linea.startswith("!"):
                    excluidos.append(linea[1:].strip())
                else:
                    incluidos.append(linea)
    return incluidos, excluidos


def _a_regex(patron):
    """Glob -> regex con semantica de rutas.

    fnmatch NO sirve para esto: su `*` cruza las barras, asi que `*.md` matchea
    `otro-equipo/documento.md` y el filtro de propiedad deja pasar todo el repo.
    Aca `*` se queda dentro de un segmento y solo `**` cruza directorios.
    """
    partes = []
    i = 0
    while i < len(patron):
        caracter = patron[i]
        if patron.startswith("**/", i):
            partes.append("(?:.*/)?")
            i += 3
        elif patron.startswith("**", i):
            partes.append(".*")
            i += 2
        elif caracter == "*":
            partes.append("[^/]*")
            i += 1
        elif caracter == "?":
            partes.append("[^/]")
            i += 1
        else:
            partes.append(re.escape(caracter))
            i += 1
    return re.compile("^" + "".join(partes) + "$")


_CACHE_REGEX = {}


def _matchea(archivo, patron):
    if patron not in _CACHE_REGEX:
        _CACHE_REGEX[patron] = _a_regex(patron)
    if _CACHE_REGEX[patron].match(archivo):
        return True
    # "docs/**" tiene que matchear "docs/01.md" ademas de "docs/a/b.md".
    if patron.endswith("/**") and archivo.startswith(patron[:-2]):
        return True
    return False


def es_propio(archivo, patrones):
    incluidos, excluidos = patrones
    # La exclusion gana: los fixtures del self-test estan rotos a proposito y el
    # gate no se puede chequear a si mismo con ellos.
    if any(_matchea(archivo, p) for p in excluidos):
        return False
    return any(_matchea(archivo, p) for p in incluidos)


# Ruteo por extension. Las rutas concretas del proyecto no viven aca sino en
# owned-paths.txt: este modulo no sabe que existe "docs/" ni "ms-evaluacion-llm/".
RUTEO = {
    ".md": ["ortografia", "markdownlint"],
    ".java": ["formato", "compila", "tests", "analisis_estatico", "idioma_codigo"],
}


def rutear(archivos):
    mapa = {}
    for archivo in archivos:
        _, ext = os.path.splitext(archivo)
        for chequeo in RUTEO.get(ext, []):
            mapa.setdefault(chequeo, []).append(archivo)
    return mapa


def en_alcance(todo=False):
    candidatos = todos_los_archivos() if todo else archivos_cambiados()
    patrones = globs_propios()
    return [a for a in candidatos if es_propio(a, patrones)]


def main():
    argumentos = sys.argv[1:]
    comando = argumentos[0] if argumentos else "archivos"
    todo = "--all" in argumentos

    os.chdir(raiz())
    archivos = en_alcance(todo)

    if comando == "ruteo":
        print(json.dumps(rutear(archivos), indent=2, sort_keys=True))
    else:
        for archivo in archivos:
            print(archivo)
    return 0


if __name__ == "__main__":
    sys.exit(main())
