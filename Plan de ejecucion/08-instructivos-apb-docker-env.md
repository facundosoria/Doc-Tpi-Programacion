# Instructivos APB: entorno, credenciales y Docker

Este documento explica desde cero cómo configurar y levantar llm-service. Cada sección indica qué hace, qué archivo interviene y cómo comprobar el resultado.

## 1. Requisitos

Verificar antes de iniciar:

~~~bash
git --version
docker --version
docker compose version
java -version
~~~

Se requiere Java 21 para desarrollo y Docker Compose para el entorno. Hasta que S1 cree el servicio definitivo, solo es ejecutable el laboratorio histórico indicado en 04-docker-y-pruebas.md.

## 2. Variables de entorno

Una variable de entorno es un valor que un proceso recibe al arrancar. Ejemplos: POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD, JWT_SECRET, LLM_PROVIDER_API_KEY y SPRING_PROFILES_ACTIVE.

El código conoce el nombre, pero cada ambiente proporciona un valor distinto. Así no se escriben contraseñas en Java, YAML ni Compose versionado.

## 3. Archivo .env

El archivo .env contiene pares NOMBRE=valor:

~~~dotenv
POSTGRES_DB=llm_dev
POSTGRES_USER=llm_app
POSTGRES_PASSWORD=cambiar-solo-localmente
JWT_SECRET=clave-local-de-al-menos-32-caracteres
LLM_PROVIDER_API_KEY=mock
~~~

Reglas:

- .env no se sube a Git; .env.example sí.
- Crear el local con cp .env.example .env.
- No agregar claves reales a commits, capturas, logs, issues ni PR.
- Si una clave se filtra, revocarla y cambiarla; borrarla del archivo no borra el historial.
- No inventar nombres: buscar la variable en Compose, application.yml y la documentación de S1.

## 4. Cómo Compose recibe las variables

Esta expresión obliga a definir un valor:

~~~yaml
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Definí POSTGRES_PASSWORD}
~~~

Si falta, Compose detiene el arranque y muestra qué variable falta.

Esta expresión permite un valor seguro de desarrollo:

~~~yaml
environment:
  LLM_PROVIDER_API_KEY: ${LLM_PROVIDER_API_KEY:-mock}
~~~

Usar mock solo en desarrollo. Producción debe requerir una credencial desde el almacén de secretos.

Conceptos distintos:

| Elemento | Función |
|---|---|
| Interpolación ${VAR} | Compose reemplaza el valor antes de crear el contenedor. |
| environment | Entrega la variable resuelta al proceso del contenedor. |
| env_file | Inyecta muchas variables desde un archivo; no convierte el archivo en seguro. |
| build.args | Solo sirve durante docker build; nunca usarlo para secretos. |

Para evitar ambigüedad:

~~~bash
docker compose --env-file .env config --quiet
docker compose --env-file .env up --build -d
~~~

## 5. Credenciales entre contenedores

El Compose definitivo debe parametrizar PostgreSQL y Spring con los mismos valores:

~~~yaml
services:
  postgres:
    environment:
      POSTGRES_DB: ${POSTGRES_DB:?Falta POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER:?Falta POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Falta POSTGRES_PASSWORD}
  llm-service:
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      JWT_SECRET: ${JWT_SECRET:?Falta JWT_SECRET}
      LLM_PROVIDER_API_KEY: ${LLM_PROVIDER_API_KEY:-mock}
~~~

Al levantar:

1. Compose lee .env o --env-file.
2. Resuelve las expresiones.
3. Falla si una variable marcada ?: falta.
4. Crea la red.
5. Inicia PostgreSQL.
6. Inicia llm-service usando postgres como hostname.
7. Spring ejecuta Flyway.
8. Los healthchecks determinan si el servicio está listo.

Dentro de Docker, localhost significa el propio contenedor. Para PostgreSQL se usa postgres:5432, no localhost:5432.

Con un volumen ya inicializado, cambiar POSTGRES_PASSWORD no recrea automáticamente el usuario. Usar el procedimiento de rotación aprobado o, únicamente en un entorno local descartable, eliminar el volumen después de confirmar que no contiene evidencia necesaria.

## 6. .env, env_file y secretos

- .env ayuda a Compose a interpolar; no se inyecta solo.
- env_file inyecta variables dentro del contenedor; quien inspeccione el contenedor puede leerlas.
- CI debe obtener credenciales desde su almacén de secretos.
- Docker secrets o un secret manager son preferibles en ambientes reales.
- Nunca pasar API keys mediante build.args: pueden quedar en capas o historial de imagen.

## 7. Qué es un Dockerfile

Un Dockerfile es la receta para construir una imagen: base, archivos, compilación y proceso de arranque.

~~~dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "app.jar"]
~~~

Lectura APB: FROM elige la base; WORKDIR fija la carpeta; COPY copia archivos; RUN ejecuta durante build; USER evita root; ENTRYPOINT define el proceso; EXPOSE solo documenta un puerto.

El build de dos etapas deja Maven fuera de la imagen final. Crear .dockerignore para excluir .env, target, logs y archivos locales.

## 8. Red, puertos y perfiles

Los servicios se encuentran por nombre: llm-service conecta a postgres:5432 y kafka:9092. Un puerto host:contenedor publica al host; en integración final solo Gateway debe quedar expuesto.

Los perfiles son capacidades opcionales:

~~~bash
docker compose --env-file .env --profile integration up --build -d
~~~

No asumir nombres de perfil hasta que estén definidos en el Compose de S1.

## 9. Arranque y comprobación

Cuando exista el Compose de S1:

~~~bash
docker compose --env-file .env up --build -d
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100 llm-service
~~~

Comprobar en orden: PostgreSQL healthy; Flyway sin error; API y worker con perfil correcto; readiness sin consultar al proveedor; ruteo Gateway; registro Eureka; headers traceparent y X-Request-Id.

Detener conservando datos:

~~~bash
docker compose --env-file .env down
~~~

Eliminar volúmenes solo después de confirmar:

~~~bash
docker compose --env-file .env down -v
~~~

down -v elimina datos locales, golden sets y resultados. Nunca usarlo en un ambiente compartido.

## 10. Diagnóstico APB

| Síntoma | Qué revisar |
|---|---|
| Falta una variable | .env, nombre exacto y --env-file. |
| Password rechazado | Mismos valores en Postgres/Spring y volumen inicializado. |
| No conecta a base | Usar postgres, no localhost; healthcheck y Flyway. |
| Puerto ocupado | Cambiar solo puerto del host o detener proceso autorizado. |
| Contenedor reinicia | logs --tail=200 y proceso ENTRYPOINT. |
| Proveedor no responde | mock/perfil, timeout, cuota y credencial aprobada. |
| Clave filtrada | Revocar inmediatamente y notificar seguridad. |

Para pedir ayuda, compartir comando, servicio, síntoma, logs sin secretos, X-Request-Id y pasos para reproducir. Nunca compartir el contenido completo de .env.

