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


def archivos_sin_commitear():
    """Solo lo que tenes en el working tree: modificado o todavia sin trackear."""
    encontrados = _git("diff", "--name-only", "HEAD").splitlines()
    encontrados += _git("ls-files", "--others", "--exclude-standard").splitlines()

    vistos = set()
    unicos = []
    for archivo in encontrados:
        if archivo and archivo not in vistos and os.path.exists(archivo):
            vistos.add(archivo)
            unicos.append(archivo)
    return unicos


def autor_actual():
    """Tu mail de git, normalizado. Vacio si no se puede saber.

    QA_AUTOR lo pone qa.sh desde el host, porque adentro del contenedor el
    `git config` global no existe: el `user.email` de tu maquina no se ve desde
    ahi, y sin el no se puede distinguir tu commit del de un companero.
    """
    valor = os.environ.get("QA_AUTOR") or _git("config", "user.email")
    return valor.strip().lower()


def partir_por_autoria(candidatos, base=None):
    """Separa (tuyos, ajenos) segun quien commiteo cada archivo en esta rama.

    Devuelve (None, None) si no se puede saber quien sos: sin esa certeza,
    adivinar seria peor que no hacer nada.

    Por que la autoria y no "lo que tenes sin commitear": porque el gate se corre
    en los dos momentos, antes y despues de commitear, y "sin commitear" solo
    acierta en el primero. Si ya commiteaste, tus propios archivos quedarian
    afuera y el formato no se corregiria nunca.

    Un archivo es tuyo si TODOS los commits de la rama que lo tocaron son tuyos.
    Alcanza con que un companero lo haya tocado una vez para que quede afuera:
    reformatearlo te lo pondria a tu nombre en el `git blame`, y a este equipo lo
    evaluan por lineas por persona.

    Un archivo sin commits en la rama --recien creado, o solo modificado en el
    working tree-- es tuyo: lo estas escribiendo vos ahora.
    """
    yo = autor_actual()
    if not yo:
        return None, None

    if base is None:
        base = base_de_comparacion()

    tuyos, ajenos = [], []
    for archivo in candidatos:
        autores = set()
        if base:
            salida = _git("log", "--format=%ae", base + "..HEAD", "--", archivo)
            autores = {a.strip().lower() for a in salida.splitlines() if a.strip()}
        if not autores or autores == {yo}:
            tuyos.append(archivo)
        else:
            ajenos.append(archivo)
    return tuyos, ajenos


def quienes_tocaron(archivos, base=None):
    """Los mails de quienes commitearon estos archivos en la rama, ordenados."""
    if base is None:
        base = base_de_comparacion()
    if not base:
        return []
    autores = set()
    for archivo in archivos:
        salida = _git("log", "--format=%ae", base + "..HEAD", "--", archivo)
        autores |= {a.strip() for a in salida.splitlines() if a.strip()}
    return sorted(autores)


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


def _primera_linea_util(ruta):
    if not os.path.exists(ruta):
        return ""
    with open(ruta, encoding="utf-8") as fh:
        for linea in fh:
            linea = linea.strip()
            if linea and not linea.startswith("#"):
                return linea
    return ""


def prefijo():
    """La carpeta del equipo dentro del repositorio. Vacio = somos la raiz.

    Todas las rutas de la config son relativas a la raiz del repositorio porque
    hoy el equipo ES la raiz. En el monorepo de la materia deja de serlo: todo
    pasa a colgar de una carpeta, y sin este valor habria que reescribir a mano
    los globs de owned-paths.txt, la ruta del proyecto Java y las raices de
    check_refs.py --y alcanza con olvidarse de una para que el gate deje de
    mirar algo sin avisar.

    El valor vive versionado en prefijo.txt y no en el entorno de cada persona,
    justamente para que no dependa de que alguien se acuerde de exportarlo.
    QA_PREFIJO lo pisa, y es para probar el cambio antes de commitearlo.
    """
    valor = os.environ.get("QA_PREFIJO")
    if valor is None:
        valor = _primera_linea_util(
            os.path.join(RUTA_CONFIG, "proyecto", "prefijo.txt")
        )
    return valor.strip().strip("/")


def con_prefijo(ruta_relativa):
    """Antepone el prefijo a una ruta de la config. Sin prefijo, no toca nada."""
    p = prefijo()
    if not p:
        return ruta_relativa
    if ruta_relativa.startswith("!"):
        return "!%s/%s" % (p, ruta_relativa[1:])
    return "%s/%s" % (p, ruta_relativa)


def verificar_prefijo():
    """Devuelve un mensaje de error, o None si esta bien.

    Un prefijo que apunta a una carpeta inexistente deja TODOS los globs sin
    matchear: la corrida termina en verde sin haber mirado un archivo. Es el
    modo de falla silencioso que este gate existe para no tener, asi que se
    chequea antes de empezar y se reporta fuerte.
    """
    p = prefijo()
    if not p:
        return None
    if not os.path.isdir(os.path.join(raiz(), p)):
        return (
            "el prefijo '%s' no existe en el repositorio. Sin eso el gate no "
            "mira ni un archivo. Revisa tools/qa/config/proyecto/prefijo.txt "
            "o la variable QA_PREFIJO." % p
        )
    return None


def globs_propios():
    """Devuelve (incluidos, excluidos). Una linea con '!' delante excluye.

    Cada patron sale prefijado con la carpeta del equipo, asi que owned-paths.txt
    se sigue escribiendo como si fueramos la raiz aunque no lo seamos.
    """
    ruta = os.path.join(RUTA_CONFIG, "proyecto", "owned-paths.txt")
    incluidos, excluidos = [], []
    if os.path.exists(ruta):
        with open(ruta, encoding="utf-8") as fh:
            for linea in fh:
                linea = linea.strip()
                if not linea or linea.startswith("#"):
                    continue
                if linea.startswith("!"):
                    excluidos.append(con_prefijo(linea[1:].strip()))
                else:
                    incluidos.append(con_prefijo(linea))
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
