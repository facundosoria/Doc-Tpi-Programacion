#!/usr/bin/env python3
"""El equivalente a "codigo muerto" en un repositorio de documentacion.

Ninguna herramienta de mercado hace esto, y es el problema real de este repo:
15 documentos que se referencian entre si y se desincronizan en silencio.

Detecta tres cosas:

1. REFERENCIAS COLGADAS  RF-IA-* y PAR-* que no estan en el registro (vienen del
                         PRD, que no se versiona). ADR-* que no estan definidos
                         como heading en el documento de decisiones, que si esta
                         en el repo y por lo tanto se valida contra el.
2. ANCLAS ROTAS          Links a "#seccion" cuyo documento destino no tiene esa
                         seccion. Es lo que se rompe cuando alguien renombra un
                         titulo sin buscar quien lo apuntaba.
3. DOCUMENTOS HUERFANOS  Archivos .md que no estan linkeados desde ningun lado.

Emite un JSON por linea, el mismo protocolo que el resto del motor.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import unicodedata

RUTA_LIB = os.path.dirname(os.path.abspath(__file__))
RUTA_CONFIG = os.path.join(os.path.dirname(RUTA_LIB), "config")

ETAPA = "referencias"

RE_ID = re.compile(r"\b(RF-IA-\d+|RF-CHT-\d+|PAR-\d+|ADR-\d+)\b")
RE_LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
RE_HEADING = re.compile(r"^(#{1,6})\s+(.*?)\s*#*$", re.MULTILINE)
RE_FENCE = re.compile(r"^```.*?^```", re.MULTILINE | re.DOTALL)
RE_ADR_HEADING = re.compile(r"^#{1,6}\s+(ADR-\d+)\b", re.MULTILINE)
# GitHub honra las anclas HTML explicitas, no solo las que genera de los titulos.
# Sin esto, un <a name="q-01"></a> valido se reportaba como ancla rota.
RE_ANCLA_HTML = re.compile(r"<a\s+(?:name|id)\s*=\s*[\"']([^\"']+)[\"']", re.IGNORECASE)


def slug(texto):
    """Ancla al estilo GitHub: minusculas, sin puntuacion, espacios por guiones.

    GitHub conserva los acentos, asi que no se normaliza a ASCII.
    """
    texto = texto.strip().lower()
    texto = re.sub(r"[`*_~\[\]()]", "", texto)
    salida = []
    for caracter in texto:
        if caracter.isalnum() or caracter in "-_":
            salida.append(caracter)
        elif caracter.isspace():
            salida.append("-")
        elif unicodedata.category(caracter).startswith("M"):
            salida.append(caracter)
    return "".join(salida)


def anclas_de(texto):
    """Todas las anclas de un documento, con el sufijo -1, -2 de los repetidos."""
    vistas = {}
    anclas = set()
    limpio = sin_bloques(texto)
    for nombre in RE_ANCLA_HTML.findall(limpio):
        anclas.add(nombre.strip().lower())
    for _, titulo in RE_HEADING.findall(limpio):
        base = slug(titulo)
        if not base:
            continue
        n = vistas.get(base, 0)
        anclas.add(base if n == 0 else "%s-%d" % (base, n))
        vistas[base] = n + 1
    return anclas


def sin_bloques(texto):
    """Saca los bloques de codigo: lo de adentro es ejemplo, no referencia real."""
    return RE_FENCE.sub("", texto)


def cargar_registro():
    ruta = os.path.join(RUTA_CONFIG, "proyecto", "refs-registry.yml")
    validos = set()
    if not os.path.exists(ruta):
        return validos
    try:
        import yaml

        with open(ruta, encoding="utf-8") as fh:
            datos = yaml.safe_load(fh) or {}
        for lista in datos.values():
            for item in lista or []:
                validos.add(str(item).strip())
    except Exception:
        # Sin PyYAML se lee la lista a mano: el formato es plano a proposito.
        with open(ruta, encoding="utf-8") as fh:
            for linea in fh:
                linea = linea.strip()
                if linea.startswith("- "):
                    validos.add(linea[2:].strip())
    return validos


def archivos_markdown(raices):
    encontrados = []
    for raiz in raices:
        if os.path.isfile(raiz):
            encontrados.append(raiz)
            continue
        for base, _, nombres in os.walk(raiz):
            if ".git" in base or "node_modules" in base or "target" in base:
                continue
            for nombre in nombres:
                if nombre.endswith(".md"):
                    encontrados.append(os.path.join(base, nombre))
    return sorted(set(encontrados))


def linea_de(texto, posicion):
    return texto.count("\n", 0, posicion) + 1


def hallazgo(archivo, linea, regla, detalle, nivel="bloquea"):
    return {
        "ev": "hallazgo",
        "etapa": ETAPA,
        "nivel": nivel,
        "archivo": archivo.replace(os.sep, "/"),
        "linea": linea,
        "regla": regla,
        "detalle": detalle,
        # Identidad estable: es lo que permite comparar dos corridas y bloquear
        # solo lo que rompio este cambio.
        "id": "%s|%s|%s" % (archivo.replace(os.sep, "/"), regla, detalle),
    }


def revisar(raices, registro, adrs_definidos, comprobar_huerfanos):
    resultados = []
    documentos = archivos_markdown(raices)
    contenidos = {}
    linkeados = set()

    for ruta in documentos:
        try:
            with open(ruta, encoding="utf-8") as fh:
                contenidos[ruta] = fh.read()
        except (OSError, UnicodeDecodeError):
            continue

    for ruta, texto in contenidos.items():
        limpio = sin_bloques(texto)

        # 1. Referencias colgadas.
        for coincidencia in RE_ID.finditer(limpio):
            ident = coincidencia.group(1)
            if ident.startswith("ADR-"):
                if adrs_definidos and ident not in adrs_definidos:
                    resultados.append(
                        hallazgo(
                            ruta,
                            linea_de(limpio, coincidencia.start()),
                            "ref-desconocida",
                            "%s no esta definido como heading en el documento de decisiones"
                            % ident,
                        )
                    )
            elif registro and ident not in registro:
                resultados.append(
                    hallazgo(
                        ruta,
                        linea_de(limpio, coincidencia.start()),
                        "ref-desconocida",
                        "%s no esta en el registro de requisitos" % ident,
                    )
                )

        # 2. Anclas rotas.
        for coincidencia in RE_LINK.finditer(limpio):
            destino = coincidencia.group(1)
            if destino.startswith(("http://", "https://", "mailto:")):
                continue
            if "#" not in destino:
                if destino.endswith(".md"):
                    linkeados.add(
                        os.path.normpath(os.path.join(os.path.dirname(ruta), destino))
                    )
                continue

            archivo_destino, _, ancla = destino.partition("#")
            if archivo_destino:
                resuelto = os.path.normpath(
                    os.path.join(os.path.dirname(ruta), archivo_destino)
                )
                linkeados.add(resuelto)
            else:
                resuelto = ruta

            texto_destino = contenidos.get(resuelto)
            if texto_destino is None:
                continue  # El archivo faltante lo reporta el chequeo de links.

            if ancla and ancla not in anclas_de(texto_destino):
                resultados.append(
                    hallazgo(
                        ruta,
                        linea_de(limpio, coincidencia.start()),
                        "ancla-rota",
                        "%s no tiene la seccion #%s"
                        % (os.path.basename(resuelto), ancla),
                    )
                )

    # 3. Documentos huerfanos.
    if comprobar_huerfanos:
        for ruta in documentos:
            normal = os.path.normpath(ruta)
            if os.path.basename(ruta).lower() == "readme.md":
                continue
            if normal not in linkeados:
                resultados.append(
                    hallazgo(
                        ruta,
                        1,
                        "documento-huerfano",
                        "no esta linkeado desde ningun otro documento",
                        nivel="avisa",
                    )
                )

    return resultados


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", action="append", default=[])
    parser.add_argument("--sin-huerfanos", action="store_true")
    parser.add_argument("--decisiones", default="docs/08-decisiones-y-pendientes.md")
    args = parser.parse_args()

    raices = args.root or ["docs", "README.md"]
    raices = [r for r in raices if os.path.exists(r)]

    adrs = set()
    if os.path.exists(args.decisiones):
        with open(args.decisiones, encoding="utf-8") as fh:
            adrs = set(RE_ADR_HEADING.findall(fh.read()))

    for resultado in revisar(raices, cargar_registro(), adrs, not args.sin_huerfanos):
        sys.stdout.write(json.dumps(resultado, ensure_ascii=False) + "\n")

    return 0


if __name__ == "__main__":
    sys.exit(main())
