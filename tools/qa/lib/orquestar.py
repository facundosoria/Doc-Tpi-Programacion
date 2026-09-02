#!/usr/bin/env python3
"""El motor. Decide que etapas corren, las ejecuta y emite eventos.

No imprime texto para humanos: emite un JSON por linea que consume reportar.py.
Esa separacion es lo que hace que la consola local y el resumen del CI muestren
exactamente lo mismo sin duplicar logica.

Cada herramienta externa tiene un adaptador que traduce su salida al formato de
hallazgo comun. Ese adaptador es la parte fragil del sistema -las herramientas se
actualizan y cambian su salida- y por eso cada uno tiene su fixture en el
self-test: si un adaptador deja de parsear, el chequeo pasaria a "no encontro
nada" en verde, que es la peor falla posible en un gate.
"""

import json
import os
import re
import subprocess
import sys
import time

RUTA_LIB = os.path.dirname(os.path.abspath(__file__))
RUTA_QA = os.path.dirname(RUTA_LIB)
RUTA_CONFIG = os.path.join(RUTA_QA, "config")

sys.path.insert(0, RUTA_LIB)
import scope  # noqa: E402

# Orden de ejecucion: de lo barato a lo caro. La primera etapa que bloquea corta
# la corrida, asi que conviene que los segundos se gasten al final.
ORDEN = [
    "workflows",
    "secretos",
    "ortografia",
    "markdownlint",
    "referencias",
    "links",
    "formato",
    "compila",
    "analisis_estatico",
    "duplicacion",
    "idioma_codigo",
    "tests",
    "cobertura",
]

PROYECTO_JAVA = "codigo-ejemplo/ms-evaluacion-llm"


def emitir(evento):
    sys.stdout.write(json.dumps(evento, ensure_ascii=False) + "\n")
    sys.stdout.flush()


def cargar_checks():
    ruta = os.path.join(RUTA_CONFIG, "checks.yml")
    with open(ruta, encoding="utf-8") as fh:
        try:
            import yaml

            return yaml.safe_load(fh) or {}
        except ImportError:
            sys.stderr.write("qa: falta PyYAML; no se puede leer checks.yml\n")
            return {}


def nivel_de(config, chequeo, perfil):
    entrada = (config.get("chequeos") or {}).get(chequeo) or {}
    valor = entrada.get(perfil, "off")
    if isinstance(valor, dict):
        valor = valor.get("nivel", "off")
    return _degradar(config, valor)


def _degradar(config, nivel):
    """Aplica `degradacion_ci` de checks.yml segun el entorno.

    En CI el working tree es efimero: el runner clona, corre y descarta. Un nivel
    que escribe --hoy `arregla`-- corrige alla archivos que nadie va a ver, y la
    corrida queda en verde sin que el arreglo llegue nunca a la rama.

    Aca solo se detecta el entorno; que se degrada a que lo decide checks.yml.
    GitHub Actions exporta CI=true, y qa.sh la propaga al contenedor.
    """
    if not os.environ.get("CI"):
        return nivel
    return (config.get("degradacion_ci") or {}).get(nivel, nivel)


def ejecutar(comando, cwd=None):
    try:
        return subprocess.run(
            comando, capture_output=True, text=True, check=False, cwd=cwd
        )
    except OSError as error:
        clase = type("R", (), {})()
        clase.returncode = 127
        clase.stdout = ""
        clase.stderr = str(error)
        return clase


# --------------------------------------------------------------------------
# Adaptadores: salida de cada herramienta -> lista de hallazgos
# --------------------------------------------------------------------------

RE_CSPELL = re.compile(r"^(?P<archivo>.+?):(?P<linea>\d+):(?P<col>\d+)\s+-\s+(?P<msg>.+)$")
RE_MDLINT = re.compile(r"^(?P<archivo>.+?):(?P<linea>\d+)(?::\d+)?\s+(?P<regla>MD\d+)\S*\s+(?P<msg>.+)$")
RE_JAVAC = re.compile(r"^\[ERROR\]\s+(?P<archivo>[^:\[]+\.java):\[(?P<linea>\d+),\d+\]\s+(?P<msg>.+)$")


def _hallazgo(etapa, nivel, archivo, linea, regla, detalle):
    archivo = (archivo or "?").replace("\\", "/").lstrip("./")
    return {
        "ev": "hallazgo",
        "etapa": etapa,
        "nivel": nivel,
        "archivo": archivo,
        "linea": linea,
        "regla": regla,
        "detalle": detalle,
        "id": "%s|%s|%s|%s" % (archivo, linea, regla, detalle),
    }


def adaptar_cspell(salida, etapa, nivel):
    hallazgos = []
    for linea in (salida.stdout + salida.stderr).splitlines():
        m = RE_CSPELL.match(linea.strip())
        if not m:
            continue
        regla = "cspell-code-non-english" if etapa == "idioma_codigo" else "cspell-unknown-word"
        hallazgos.append(
            _hallazgo(etapa, nivel, m.group("archivo"), int(m.group("linea")), regla, m.group("msg"))
        )
    return hallazgos


def adaptar_markdownlint(salida, etapa, nivel):
    hallazgos = []
    for linea in (salida.stdout + salida.stderr).splitlines():
        m = RE_MDLINT.match(linea.strip())
        if not m:
            continue
        hallazgos.append(
            _hallazgo(
                etapa,
                nivel,
                m.group("archivo"),
                int(m.group("linea")),
                "markdownlint",
                "%s %s" % (m.group("regla"), m.group("msg")),
            )
        )
    return hallazgos


def adaptar_lychee(salida, etapa, nivel):
    hallazgos = []
    try:
        datos = json.loads(salida.stdout or "{}")
    except json.JSONDecodeError:
        return hallazgos
    for archivo, entradas in (datos.get("error_map") or {}).items():
        for entrada in entradas:
            # status es un objeto, no un string: {"text": "Cannot find file"}.
            estado = entrada.get("status") or {}
            texto = estado.get("text") if isinstance(estado, dict) else str(estado)
            hallazgos.append(
                _hallazgo(
                    etapa,
                    nivel,
                    archivo,
                    None,
                    "lychee-404",
                    "%s (%s)" % (entrada.get("url", "?"), texto or "error"),
                )
            )
    return hallazgos


def correr_gitleaks(destino, config_dir):
    """gitleaks no vuelca de forma confiable a /dev/stdout: usa un archivo."""
    import tempfile

    reporte = os.path.join(tempfile.mkdtemp(prefix="qa-leaks-"), "leaks.json")
    salida = ejecutar([
        "gitleaks", "detect", "--no-banner", "--redact", "--no-git",
        "--source", destino,
        "--config", os.path.join(config_dir, "gitleaks.toml"),
        "--report-format", "json", "--report-path", reporte,
    ])
    if os.path.exists(reporte):
        with open(reporte, encoding="utf-8") as fh:
            salida.stdout = fh.read()
    return salida


def adaptar_gitleaks(salida, etapa, nivel):
    hallazgos = []
    try:
        datos = json.loads(salida.stdout or "[]")
    except json.JSONDecodeError:
        return hallazgos
    for fuga in datos or []:
        hallazgos.append(
            _hallazgo(
                etapa,
                nivel,
                fuga.get("File", "?"),
                fuga.get("StartLine"),
                "gitleaks",
                fuga.get("Description", "credencial detectada"),
            )
        )
    return hallazgos


def adaptar_javac(salida, etapa, nivel):
    hallazgos = []
    for linea in salida.stdout.splitlines():
        m = RE_JAVAC.match(linea.strip())
        if m:
            hallazgos.append(
                _hallazgo(
                    etapa, nivel, m.group("archivo"), int(m.group("linea")),
                    "compilation-error", m.group("msg"),
                )
            )
    return hallazgos


def adaptar_pmd(salida, etapa, nivel):
    """Lee target/pmd.xml, no la consola.

    El formato de consola de maven-pmd-plugin cambia entre versiones; el XML es un
    contrato estable. Vale lo mismo para surefire.
    """
    import xml.etree.ElementTree as ET

    reporte = os.path.join(PROYECTO_JAVA, "target", "pmd.xml")
    if not os.path.exists(reporte):
        return []

    hallazgos = []
    try:
        raiz = ET.parse(reporte).getroot()
    except ET.ParseError:
        return []

    for archivo in raiz.iter():
        if not archivo.tag.endswith("file"):
            continue
        ruta = _relativa(archivo.get("name", "?"))
        for violacion in archivo:
            if not violacion.tag.endswith("violation"):
                continue
            hallazgos.append(
                _hallazgo(
                    etapa,
                    nivel,
                    ruta,
                    int(violacion.get("beginline", 0)) or None,
                    "pmd-" + violacion.get("rule", "desconocida"),
                    (violacion.text or "").strip(),
                )
            )
    return hallazgos


def adaptar_tests(salida, etapa, nivel):
    """Lee los XML de surefire. La linea sale del stack trace del assert."""
    import glob
    import xml.etree.ElementTree as ET

    patron = os.path.join(PROYECTO_JAVA, "target", "surefire-reports", "TEST-*.xml")
    hallazgos = []

    for reporte in sorted(glob.glob(patron)):
        try:
            raiz = ET.parse(reporte).getroot()
        except ET.ParseError:
            continue
        for caso in raiz.iter("testcase"):
            for fallo in list(caso):
                if fallo.tag not in ("failure", "error"):
                    continue
                clase = caso.get("classname", "").split(".")[-1]
                traza = fallo.text or ""
                m = re.search(r"\(%s\.java:(\d+)\)" % re.escape(clase), traza)
                hallazgos.append(
                    _hallazgo(
                        etapa,
                        nivel,
                        clase + ".java",
                        int(m.group(1)) if m else None,
                        "assertion-failed",
                        "%s: %s"
                        % (caso.get("name", "?"), (fallo.get("message") or "fallo").strip()),
                    )
                )
    return hallazgos


def adaptar_cpd(salida, etapa, nivel):
    """Duplicacion de codigo, desde target/cpd.xml."""
    import xml.etree.ElementTree as ET

    reporte = os.path.join(PROYECTO_JAVA, "target", "cpd.xml")
    if not os.path.exists(reporte):
        return []

    hallazgos = []
    try:
        raiz = ET.parse(reporte).getroot()
    except ET.ParseError:
        return []

    for dup in raiz.iter():
        if not dup.tag.endswith("duplication"):
            continue
        archivos = [f for f in dup if f.tag.endswith("file")]
        if not archivos:
            continue
        lineas = dup.get("lines", "?")
        donde = ", ".join(
            "%s:%s" % (os.path.basename(_relativa(f.get("path", "?"))), f.get("line", "?"))
            for f in archivos[1:]
        )
        primero = archivos[0]
        hallazgos.append(
            _hallazgo(
                etapa, nivel, _relativa(primero.get("path", "?")),
                int(primero.get("line", 0)) or None, "cpd-duplicado",
                "%s lineas repetidas tambien en %s" % (lineas, donde or "otro lugar"),
            )
        )
    return hallazgos


def adaptar_cobertura(etapa, nivel):
    """Cobertura de las lineas que agregaste, no la del proyecto.

    Un porcentaje global es inaplicable con muchos equipos: uno lleno de DTOs
    llega al 90% sin testear una regla de negocio, y otro que escribe dominio
    complejo no llega al 60% aunque teste bien. diff-cover mide solo el diff, y
    por eso no genera deuda retroactiva: nadie tiene que ir a cubrir codigo viejo.
    """
    reporte = os.path.join(PROYECTO_JAVA, "target", "site", "jacoco", "jacoco.xml")
    if not os.path.exists(reporte):
        return []

    base = os.environ.get("QA_BASE") or "origin/main"
    umbral = int(os.environ.get("QA_COBERTURA_MINIMA", "70"))

    # --src-roots es imprescindible: JaCoCo describe las clases por paquete, y sin
    # esto diff-cover busca las fuentes en src/main/java desde la raiz del repo,
    # no encuentra nada, y reporta "sin lineas" en verde. Es justo el tipo de
    # falla silenciosa que el self-test existe para cazar.
    salida = ejecutar([
        "/opt/qa-venv/bin/diff-cover", reporte,
        "--compare-branch", base,
        "--src-roots", os.path.join(PROYECTO_JAVA, "src", "main", "java"),
        "--fail-under", str(umbral),
        "--format", "markdown:%s" % os.path.join(".qa", "cobertura.md"),
    ])

    texto = salida.stdout + salida.stderr
    m = re.search(r"Coverage:\s+([\d.]+)%", texto)
    if not m:
        # Sin lineas nuevas medibles no hay nada que reportar, y eso no es un fallo.
        return []

    porcentaje = float(m.group(1))
    if porcentaje >= umbral:
        return []

    return [_hallazgo(
        etapa, nivel, PROYECTO_JAVA, None, "cobertura-insuficiente",
        "%.0f%% de las lineas nuevas cubiertas, el minimo es %d%%" % (porcentaje, umbral),
    )]


def _relativa(ruta):
    """Ruta absoluta del contenedor -> ruta relativa al repo, que es clickeable."""
    ruta = ruta.replace("\\", "/")
    raiz = os.getcwd().replace("\\", "/")
    if ruta.startswith(raiz):
        return ruta[len(raiz):].lstrip("/")
    return ruta


def adaptar_eventos(salida, etapa, nivel):
    """Para los chequeos propios, que ya emiten el formato del motor."""
    hallazgos = []
    for linea in salida.stdout.splitlines():
        linea = linea.strip()
        if not linea:
            continue
        try:
            evento = json.loads(linea)
        except json.JSONDecodeError:
            continue
        if evento.get("ev") != "hallazgo":
            continue
        evento["etapa"] = etapa
        if evento.get("nivel") != "avisa":
            evento["nivel"] = nivel
        hallazgos.append(evento)
    return hallazgos


# --------------------------------------------------------------------------
# Etapas
# --------------------------------------------------------------------------


def hay_proyecto_java():
    return os.path.exists(os.path.join(PROYECTO_JAVA, "pom.xml"))


def maven(objetivos, perfil_completo=False):
    comando = ["mvn", "-q", "-B", "-f", os.path.join(PROYECTO_JAVA, "pom.xml")] + objetivos
    if perfil_completo:
        comando.append("-Pcompleto")
    return ejecutar(comando)


def correr_etapa(etapa, nivel, archivos, ruteo, perfil):
    """Devuelve (hallazgos, ejecutada)."""
    java = ruteo.get("formato", [])
    md = ruteo.get("ortografia", [])
    completo = perfil == "completo"
    py = sys.executable or "python3"

    if etapa == "workflows":
        if not os.path.isdir(".github/workflows"):
            return [], False
        salida = ejecutar([py, os.path.join(RUTA_LIB, "check_workflows.py")])
        return adaptar_eventos(salida, etapa, nivel), True

    if etapa == "secretos":
        # --no-git escanea el working tree, no el historial: interesa lo que
        # estas por subir, no una clave que alguien roto hace seis meses.
        return adaptar_gitleaks(correr_gitleaks(".", RUTA_CONFIG), etapa, nivel), True

    if etapa == "ortografia":
        if not md:
            return [], False
        salida = ejecutar([
            "cspell", "lint", "--no-progress", "--no-summary", "--no-color",
            "--config", os.path.join(RUTA_CONFIG, "cspell.docs.json"),
        ] + md)
        return adaptar_cspell(salida, etapa, nivel), True

    if etapa == "markdownlint":
        if not md:
            return [], False
        salida = ejecutar([
            "markdownlint-cli2", "--config",
            os.path.join(RUTA_CONFIG, ".markdownlint.jsonc"),
        ] + md)
        return adaptar_markdownlint(salida, etapa, nivel), True

    if etapa == "referencias":
        salida = ejecutar([
            py, os.path.join(RUTA_LIB, "diff_gate.py"), "--etapa", etapa, "--",
            py, os.path.join(RUTA_LIB, "check_refs.py"),
        ])
        return adaptar_eventos(salida, etapa, nivel), True

    if etapa == "links":
        # Corre sobre TODO el markdown que es nuestro, no solo sobre lo que
        # cambiaste: un link se rompe en un archivo que no tocaste.
        #
        # Los objetivos salian de una lista fija --docs/ y README.md-- y eso deja
        # de servir en cuanto esto viva en el monorepo de la materia: ahi `docs/` y
        # el README de la raiz son de la catedra, no nuestros. Sacarlos de
        # owned-paths.txt hace que la etapa siga mirando lo que corresponde sin
        # tocar una linea de codigo.
        objetivos = [a for a in scope.en_alcance(True) if a.endswith(".md")]
        objetivos = [o for o in objetivos if os.path.exists(o)]
        if not objetivos:
            return [], False
        comando = [
            "lychee", "--format", "json", "--no-progress",
            "--config", os.path.join(RUTA_CONFIG, "lychee.toml"),
        ]
        if not completo:
            comando.append("--offline")
        salida = ejecutar(comando + objetivos)
        return adaptar_lychee(salida, etapa, nivel), True

    if etapa in ("formato", "compila", "analisis_estatico", "duplicacion",
                 "idioma_codigo", "tests", "cobertura"):
        if not hay_proyecto_java():
            return [], False
        if etapa != "idioma_codigo" and not java:
            return [], False

        if etapa == "formato":
            objetivo = "spotless:apply" if nivel == "arregla" else "spotless:check"
            salida = maven([objetivo])
            if nivel == "arregla":
                return [], True
            if salida.returncode != 0:
                return [_hallazgo(etapa, nivel, PROYECTO_JAVA, None, "spotless",
                                  "hay archivos sin formatear")], True
            return [], True

        if etapa == "compila":
            salida = maven(["test-compile"])
            hallazgos = adaptar_javac(salida, etapa, nivel)
            if salida.returncode != 0 and not hallazgos:
                hallazgos = [_hallazgo(etapa, nivel, PROYECTO_JAVA, None,
                                       "compilation-error", "la compilacion fallo")]
            return hallazgos, True

        if etapa == "analisis_estatico":
            salida = maven(["pmd:check"])
            return adaptar_pmd(salida, etapa, nivel), True

        if etapa == "duplicacion":
            # CPD viene dentro de PMD: es la deteccion de copiar-pegar que mide
            # SonarQube, sin necesitar SonarQube ni una herramienta mas.
            salida = maven(["pmd:cpd"])
            return adaptar_cpd(salida, etapa, nivel), True

        if etapa == "cobertura":
            return adaptar_cobertura(etapa, nivel), True

        if etapa == "idioma_codigo":
            fuentes = [a for a in archivos if a.endswith(".java")]
            if not fuentes:
                return [], False
            salida = ejecutar([
                "cspell", "lint", "--no-progress", "--no-summary", "--no-color",
                "--config", os.path.join(RUTA_CONFIG, "cspell.code.json"),
            ] + fuentes)
            hallazgos = adaptar_cspell(salida, etapa, nivel)
            hallazgos += _no_ascii(fuentes, etapa, nivel)
            return hallazgos, True

        if etapa == "tests":
            salida = maven(["test"], perfil_completo=completo)
            hallazgos = adaptar_tests(salida, etapa, nivel)
            if salida.returncode != 0 and not hallazgos:
                hallazgos = [_hallazgo(etapa, nivel, PROYECTO_JAVA, None,
                                       "assertion-failed", "la suite fallo")]
            return hallazgos, True

    return [], False


def _no_ascii(fuentes, etapa, nivel):
    """Un acento en un .java es castellano que se colo. Es el refuerzo de cspell."""
    hallazgos = []
    for ruta in fuentes:
        try:
            with open(ruta, encoding="utf-8") as fh:
                for numero, linea in enumerate(fh, 1):
                    for caracter in linea:
                        if ord(caracter) > 127:
                            hallazgos.append(
                                _hallazgo(etapa, nivel, ruta, numero, "non-ascii-source",
                                          "caracter no-ASCII: %r" % caracter)
                            )
                            break
        except (OSError, UnicodeDecodeError):
            continue
    return hallazgos


def main():
    argumentos = sys.argv[1:]
    todo = "--all" in argumentos
    perfil = "completo" if "--perfil" in argumentos and "completo" in argumentos else "rapido"
    solo = None
    if "--only" in argumentos:
        indice = argumentos.index("--only")
        if indice + 1 < len(argumentos):
            solo = argumentos[indice + 1]

    os.chdir(scope.raiz())
    config = cargar_checks()
    archivos = scope.en_alcance(todo)
    ruteo = scope.rutear(archivos)

    corte = False
    for etapa in ORDEN:
        if solo and etapa != solo:
            continue

        nivel = nivel_de(config, etapa, perfil)
        if nivel == "off" or corte:
            emitir({"ev": "etapa_ini", "etapa": etapa})
            emitir({"ev": "etapa_fin", "etapa": etapa, "estado": "omitida", "ms": 0})
            continue

        emitir({"ev": "etapa_ini", "etapa": etapa})
        arranque = time.time()
        hallazgos, ejecutada = correr_etapa(etapa, nivel, archivos, ruteo, perfil)
        transcurrido = int((time.time() - arranque) * 1000)

        if not ejecutada:
            emitir({"ev": "etapa_fin", "etapa": etapa, "estado": "omitida", "ms": 0})
            continue

        for hallazgo in hallazgos:
            emitir(hallazgo)

        bloquea = any(h.get("nivel") == "bloquea" for h in hallazgos)
        if bloquea:
            estado = "fallo"
            # Corte temprano: no tiene sentido gastar minutos en las etapas caras
            # cuando ya sabemos que la corrida no pasa.
            corte = True
        elif hallazgos:
            estado = "aviso"
        else:
            estado = "ok"
        emitir({"ev": "etapa_fin", "etapa": etapa, "estado": estado, "ms": transcurrido})

    return 0


if __name__ == "__main__":
    sys.exit(main())
