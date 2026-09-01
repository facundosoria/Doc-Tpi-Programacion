#!/usr/bin/env bash
# Orquestador, DENTRO del contenedor. Lo invoca qa.sh; no se llama a mano.
#
# La logica de despacho vive en lib/orquestar.py: parsear YAML y adaptar la salida
# de seis herramientas en shell seria fragil. Este script solo conecta el motor con
# el reporte y decide el codigo de salida.
set -uo pipefail

QA_LIB="tools/qa/lib"

# El repo esta montado desde el host, asi que adentro del contenedor los archivos
# pertenecen a otro UID y git se niega a operar por "dubious ownership".
git config --global --add safe.directory /work 2>/dev/null || true

# Fines de linea. El alcance del gate sale de `git diff`, asi que si el contenedor
# normaliza distinto que el host, ve otro conjunto de archivos cambiados para el
# mismo commit: local y CI dejan de coincidir. La solucion de fondo es el
# .gitattributes del repo; esto cubre los working trees en CRLF que quedaron de
# antes de que existiera.
git config --global core.autocrlf true 2>/dev/null || true

# El self-test es su propio programa: verifica el gate, no el repo.
for argumento in "$@"; do
  if [ "$argumento" = "--self-test" ]; then
    exec python3 "$QA_LIB/selftest.py"
  fi
done

# Aviso de atribucion. Un user.email mal configurado hace que los commits de esa
# persona no se vinculen a su cuenta de GitHub, y se descubre tarde.
if [ -z "$(git config user.email 2>/dev/null)" ] || [ -z "$(git config user.name 2>/dev/null)" ]; then
  echo "qa: aviso - git user.name o user.email sin configurar." >&2
  echo "    Tus commits van a aparecer sin atribucion en GitHub." >&2
fi

# --json vuelca los eventos crudos: es el contrato del que dependen el reporte,
# el self-test y cualquier cosa que se construya encima.
for argumento in "$@"; do
  if [ "$argumento" = "--json" ]; then
    exec python3 "$QA_LIB/orquestar.py" "$@"
  fi
done

# Con que flags corrio, para el registro que va al buzon. Es la unica forma de
# distinguir en el front una corrida `rapido` de una `completo` sin adivinarlo por
# las etapas que faltan.
export QA_INVOCACION="./qa.sh $*"

# PIPESTATUS para que el veredicto lo decida reportar.py (que sabe que hallazgos
# bloquean) y no el motor (que solo los emite).
python3 "$QA_LIB/orquestar.py" "$@" | python3 "$QA_LIB/reportar.py"
exit "${PIPESTATUS[1]}"
