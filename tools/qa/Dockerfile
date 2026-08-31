# Imagen unica del gate de calidad.
# Todo lo que necesitan los chequeos vive aca adentro: el equipo solo instala Docker.
FROM maven:3.9-eclipse-temurin-21

ARG GITLEAKS_VERSION=8.28.0
ARG LYCHEE_VERSION=0.18.1

ENV DEBIAN_FRONTEND=noninteractive \
    NODE_PATH=/opt/qa-node/node_modules \
    PATH=/opt/qa-node/node_modules/.bin:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates git python3 python3-yaml python3-venv nodejs npm \
    && rm -rf /var/lib/apt/lists/*

# diff-cover mide la cobertura de las lineas que agregaste, no la del proyecto.
# Va en un venv propio: Debian marca el Python del sistema como gestionado y
# ademas asi no se pisa el python3 que usa el motor.
RUN python3 -m venv /opt/qa-venv && \
    /opt/qa-venv/bin/pip install --no-cache-dir --quiet diff-cover

# Chequeos de documentacion. Se instalan en un prefijo fijo para que
# NODE_PATH resuelva los diccionarios de cspell de forma deterministica.
RUN mkdir -p /opt/qa-node && cd /opt/qa-node && npm init -y >/dev/null && \
    npm install --no-fund --no-audit \
      cspell@8 \
      @cspell/dict-es-es \
      markdownlint-cli2@0.14

# Deteccion de secretos.
RUN curl -sSL "https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz" \
      | tar -xz -C /usr/local/bin gitleaks && chmod +x /usr/local/bin/gitleaks

# Verificacion de links.
RUN curl -sSL "https://github.com/lycheeverse/lychee/releases/download/lychee-v${LYCHEE_VERSION}/lychee-x86_64-unknown-linux-gnu.tar.gz" \
      | tar -xz -C /usr/local/bin lychee && chmod +x /usr/local/bin/lychee

# Maven baja las dependencias a un volumen montado, no a la imagen.
ENV MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository"

WORKDIR /work
