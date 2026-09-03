#!/usr/bin/env python3
"""Verifica el gate contra si mismo. Se invoca con ./qa.sh --self-test

Cada chequeo tiene un fixture que lo dispara a proposito, y este programa afirma
que encuentra ESA regla y ninguna otra.

Por que existe: son diez herramientas de terceros que se actualizan solas y
cambian el formato de su salida. El dia que cspell cambie su JSON, el adaptador
deja de parsear y ese chequeo pasa a "no encontro nada" -en verde, sin error, y
nadie lo nota-. Un gate que falla en silencio es peor que no tener gate.

Los fixtures de Java se copian dentro del proyecto Maven de a uno, se corre la
etapa, se afirma el resultado y se sacan. Es lento (cada mvn son segundos) pero
prueba la cadena completa y no solo el adaptador.
"""

import io
import os
import shutil
import subprocess
import sys
import tempfile

RUTA_LIB = os.path.dirname(os.path.abspath(__file__))
RUTA_QA = os.path.dirname(RUTA_LIB)
RUTA_TESTS = os.path.join(RUTA_QA, "tests")
RUTA_FIXTURES = os.path.join(RUTA_TESTS, "fixtures")

sys.path.insert(0, RUTA_LIB)
import orquestar  # noqa: E402
import scope  # noqa: E402

PAQUETE = os.path.join(
    "src", "%s", "java", "ar", "edu", "utn", "frc", "tup", "piv", "evaluacionllm"
)

VERDE = "\033[32m"
ROJO = "\033[31m"
GRIS = "\033[90m"
RESET = "\033[0m"


def cargar_casos():
    ruta = os.path.join(RUTA_TESTS, "esperado.yml")
    with open(ruta, encoding="utf-8") as fh:
        import yaml

        return (yaml.safe_load(fh) or {}).get("casos", [])


def _reglas(hallazgos):
    return sorted({h.get("regla") for h in hallazgos if h.get("regla")})


def correr_caso_doc(caso):
    """Fixtures de documentacion: se le pasa el archivo directo al chequeo."""
    etapa = caso["etapa"]
    fixture = os.path.join(RUTA_FIXTURES, caso["fixture"])
    relativo = os.path.relpath(fixture, os.getcwd()).replace(os.sep, "/")
    config = orquestar.RUTA_CONFIG

    if etapa == "ortografia":
        salida = orquestar.ejecutar([
            "cspell", "lint", "--no-progress", "--no-summary", "--no-color",
            "--config", os.path.join(config, "cspell.docs.json"), relativo,
        ])
        return orquestar.adaptar_cspell(salida, etapa, "bloquea")

    if etapa == "links":
        salida = orquestar.ejecutar([
            "lychee", "--format", "json", "--no-progress", "--offline",
            "--config", os.path.join(config, "lychee.toml"), relativo,
        ])
        return orquestar.adaptar_lychee(salida, etapa, "bloquea")

    if etapa == "workflows":
        salida = orquestar.ejecutar([
            sys.executable, os.path.join(RUTA_LIB, "check_workflows.py"), relativo,
        ])
        return orquestar.adaptar_eventos(salida, etapa, "bloquea")

    if etapa == "referencias":
        salida = orquestar.ejecutar([
            sys.executable, os.path.join(RUTA_LIB, "check_refs.py"),
            "--root", relativo, "--sin-huerfanos",
        ])
        return orquestar.adaptar_eventos(salida, etapa, "bloquea")

    if etapa == "secretos":
        # El fixture vive en una ruta que gitleaks.toml excluye (para que el gate
        # real no delate su propio token sintetico). Se copia afuera para que la
        # exclusion no tape el chequeo y este caso pase en verde sin verificar nada.
        import tempfile

        afuera = tempfile.mkdtemp(prefix="qa-fixture-")
        copia = os.path.join(afuera, os.path.basename(fixture))
        shutil.copyfile(fixture, copia)
        try:
            salida = orquestar.correr_gitleaks(copia, config)
            return orquestar.adaptar_gitleaks(salida, etapa, "bloquea")
        finally:
            shutil.rmtree(afuera, ignore_errors=True)

    raise ValueError("etapa de documentacion desconocida: " + etapa)


def correr_caso_java(caso):
    """Fixtures de Java: se instalan en el proyecto Maven, se corre, se sacan.

    Un fixture puede ser un archivo suelto o un directorio con varios (la
    duplicacion necesita dos clases para tener algo que duplicar).
    """
    etapa = caso["etapa"]
    destino_dir = os.path.join(
        orquestar.PROYECTO_JAVA, PAQUETE % caso.get("destino", "main")
    )
    os.makedirs(destino_dir, exist_ok=True)

    origen = os.path.join(RUTA_FIXTURES, caso["fixture"])
    fuentes = (
        [os.path.join(origen, n) for n in sorted(os.listdir(origen))]
        if os.path.isdir(origen)
        else [origen]
    )

    copiados = []
    for fuente in fuentes:
        nombre = os.path.basename(fuente).replace(".java.txt", ".java")
        destino = os.path.join(destino_dir, nombre)
        shutil.copyfile(fuente, destino)
        copiados.append(destino.replace(os.sep, "/"))

    # diff-cover mide contra el diff de git, y un archivo sin trackear no aparece
    # ahi. `git add -N` lo marca como "va a existir" y lo hace visible al diff
    # sin llegar a agregar contenido al indice.
    if etapa == "cobertura":
        orquestar.ejecutar(["git", "add", "-N"] + copiados)

    try:
        ruteo = {"formato": copiados}
        # La cobertura necesita que los tests hayan corrido antes: JaCoCo escribe
        # su reporte durante la fase test.
        if etapa == "cobertura":
            orquestar.correr_etapa("tests", "avisa", copiados, ruteo, "rapido")
        hallazgos, _ = orquestar.correr_etapa(
            etapa, "bloquea", copiados, ruteo, "rapido"
        )
        return hallazgos
    finally:
        if etapa == "cobertura":
            orquestar.ejecutar(["git", "reset", "--quiet", "--"] + copiados)
        for destino in copiados:
            if os.path.exists(destino):
                os.remove(destino)
        # Los reportes quedan con datos del fixture y contaminarian la corrida
        # siguiente, que lee esos mismos XML.
        for resto in ("target/pmd.xml", "target/cpd.xml", "target/surefire-reports",
                      "target/site/jacoco"):
            ruta = os.path.join(orquestar.PROYECTO_JAVA, resto)
            if os.path.isdir(ruta):
                shutil.rmtree(ruta, ignore_errors=True)
            elif os.path.exists(ruta):
                os.remove(ruta)


class _MavenEspiado:
    """Reemplaza a orquestar.maven() y guarda con que lo llamaron.

    La etapa `formato` en nivel `arregla` ESCRIBE, asi que lo que hay que afirmar
    no es que hallazgos devuelve --no devuelve ninguno-- sino sobre que archivos
    le pidio trabajar a spotless. Correr Maven de verdad no agregaria nada y
    costaria segundos.
    """

    def __init__(self):
        # Una lista y no un solo valor: la etapa hace DOS llamadas --apply sobre
        # lo tuyo y check sobre lo ajeno-- y quedarse con la ultima esconderia
        # justo la que hay que afirmar.
        self.llamadas = []

    def __call__(self, objetivos, perfil_completo=False):
        self.llamadas.append(list(objetivos))

        class Resultado:
            returncode = 0
            stdout = ""
            stderr = ""

        return Resultado()


def _git_en(directorio, *args, **entorno):
    env = dict(os.environ)
    env.update(entorno)
    return subprocess.run(["git"] + list(args), cwd=directorio, env=env, check=True,
                          stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def correr_caso_atribucion(caso):
    """Afirma que `spotless:apply` formatea lo tuyo y NO lo de un companero.

    El fixture no es un archivo sino una situacion, y hace falta un repositorio
    de verdad para armarla: una rama con tres archivos mal formateados, uno
    commiteado por otra persona, uno commiteado por vos y uno todavia sin
    commitear.

    Lo que se prueba es el criterio, que es lo que se puede romper sin que nadie
    lo note: la etapa decide por AUTORIA, no por "lo que tenes sin commitear".
    La diferencia importa porque el gate se corre en los dos momentos --antes y
    despues de commitear-- y el segundo criterio solo acierta en el primero: si
    ya commiteaste, tus propios archivos quedarian afuera y el formato no se
    corregiria nunca.

    Va en un repositorio temporal --hace falta el commit de por medio para que
    exista la distincion-- y con Maven espiado en vez de ejecutado: lo que se
    afirma es sobre que archivos se le pidio trabajar. Que spotless formatee bien
    ya lo prueba MalFormateado.java.txt.
    """
    yo = "yo@selftest"
    companero = "companero@selftest"
    mal_formateado = io.open(
        os.path.join(RUTA_FIXTURES, "MalFormateado.java.txt"), encoding="utf-8"
    ).read()

    volver = os.getcwd()
    temporal = tempfile.mkdtemp(prefix="qa-atribucion-")
    maven_real = orquestar.maven
    autor_previo = os.environ.get("QA_AUTOR")
    try:
        _git_en(temporal, "init", "-q")
        _git_en(temporal, "config", "user.name", "selftest")
        _git_en(temporal, "config", "user.email", yo)

        destino = os.path.join(temporal, orquestar.PROYECTO_JAVA, "src", "main", "java")
        os.makedirs(destino, exist_ok=True)
        # hay_proyecto_java() mira que exista el pom; su contenido no importa aca.
        io.open(os.path.join(temporal, orquestar.PROYECTO_JAVA, "pom.xml"), "w",
                encoding="utf-8").write("<project/>")

        def escribir(nombre):
            ruta = os.path.join(destino, nombre)
            io.open(ruta, "w", encoding="utf-8", newline="\n").write(mal_formateado)
            return os.path.relpath(ruta, temporal).replace(os.sep, "/")

        # La base: una rama main con el pom, para que base_de_comparacion() tenga
        # contra que comparar. Sin base, todo se veria como "sin commits" y por
        # lo tanto tuyo, y el caso no probaria nada.
        _git_en(temporal, "checkout", "-q", "-b", "main")
        _git_en(temporal, "add", "-A")
        _git_en(temporal, "commit", "-q", "-m", "base")
        _git_en(temporal, "checkout", "-q", "-b", "rama")

        ruta_ajeno = escribir("DeUnCompanero.java")
        _git_en(temporal, "add", "-A")
        _git_en(temporal, "commit", "-q", "-m", "companero",
                GIT_AUTHOR_EMAIL=companero, GIT_AUTHOR_NAME="companero",
                GIT_COMMITTER_EMAIL=companero, GIT_COMMITTER_NAME="companero")

        ruta_commiteado = escribir("MioCommiteado.java")
        _git_en(temporal, "add", "-A")
        _git_en(temporal, "commit", "-q", "-m", "mio")

        ruta_sucio = escribir("MioSinCommitear.java")

        os.chdir(temporal)
        os.environ["QA_AUTOR"] = yo
        orquestar.maven = _MavenEspiado()
        # Los tres llegan en la lista: para el resto del gate los tres cambiaron
        # respecto de la base. Quien tiene que dejar afuera al ajeno es la etapa.
        todos = [ruta_ajeno, ruta_commiteado, ruta_sucio]
        orquestar.correr_etapa("formato", "arregla", todos,
                               {"formato": todos}, "rapido")
        llamadas = list(orquestar.maven.llamadas)
    finally:
        orquestar.maven = maven_real
        if autor_previo is None:
            os.environ.pop("QA_AUTOR", None)
        else:
            os.environ["QA_AUTOR"] = autor_previo
        os.chdir(volver)
        shutil.rmtree(temporal, ignore_errors=True)

    apply = next((c for c in llamadas if c and c[0] == "spotless:apply"), None)
    if apply is None:
        return [{"regla": "no-corrio-apply"}]

    filtro = next((o for o in apply if o.startswith("-DspotlessFiles=")), None)
    if filtro is None:
        # Sin filtro, spotless reformatea el modulo entero: es exactamente el bug.
        return [{"regla": "sin-filtro-de-archivos"}]

    # El filtro son regex con los puntos escapados ("Mio\\.java"). Se comparan sin
    # las barras para no atar el self-test a como se arma el patron.
    plano = filtro.replace("\\", "")
    if "DeUnCompanero.java" in plano:
        return [{"regla": "formatea-lo-ajeno"}]
    if "MioCommiteado.java" not in plano:
        return [{"regla": "no-formatea-lo-tuyo-commiteado"}]
    if "MioSinCommitear.java" not in plano:
        return [{"regla": "no-formatea-lo-tuyo-sin-commitear"}]
    return []


def main():
    os.chdir(scope.raiz())
    casos = cargar_casos()
    if not casos:
        sys.stderr.write("self-test: no hay casos en esperado.yml\n")
        return 2

    fallidos = []
    print()
    for caso in casos:
        fixture = caso["fixture"]
        esperadas = sorted(caso.get("reglas") or [])

        try:
            # `destino` (main o test) es lo que distingue un fixture de Java: el
            # nombre no sirve, porque uno de ellos es un directorio con dos clases.
            if caso.get("por_autoria"):
                hallazgos = correr_caso_atribucion(caso)
            elif caso.get("destino"):
                hallazgos = correr_caso_java(caso)
            else:
                hallazgos = correr_caso_doc(caso)
        except Exception as error:  # noqa: BLE001
            fallidos.append((fixture, esperadas, ["EXCEPCION: %s" % error]))
            print("  %sx%s %-32s excepcion: %s" % (ROJO, RESET, fixture, error))
            continue

        obtenidas = _reglas(hallazgos)

        if obtenidas == esperadas:
            detalle = "sin hallazgos" if not esperadas else ", ".join(esperadas)
            print("  %sv%s %-32s %s%s%s" % (VERDE, RESET, fixture, GRIS, detalle, RESET))
        else:
            fallidos.append((fixture, esperadas, obtenidas))
            print("  %sx%s %-32s esperaba %s, obtuvo %s"
                  % (ROJO, RESET, fixture, esperadas or "nada", obtenidas or "nada"))

    print()
    if fallidos:
        print("%s%d de %d fixtures fallaron.%s" % (ROJO, len(fallidos), len(casos), RESET))
        print()
        print("Un fixture que esperaba algo y no obtuvo nada casi siempre significa")
        print("que el adaptador de esa herramienta dejo de parsear su salida: el")
        print("chequeo esta pasando en verde sin mirar nada.")
        print()
        return 1

    print("%s%d fixtures, todos correctos.%s" % (VERDE, len(casos), RESET))
    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
