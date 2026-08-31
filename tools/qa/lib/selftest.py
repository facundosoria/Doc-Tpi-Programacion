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

import os
import shutil
import sys

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
            if caso.get("destino"):
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
