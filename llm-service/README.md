# Tema 07 — Evaluación LLM (`llm-service`)

Microservicio de inteligencia artificial de la **Plataforma de Aprendizaje Gamificado**: pone la
nota a cómo un alumno usó la IA en un desafío, evalúa con Golden Set y rúbrica calibrada, modera el
chat, y expone un tutor socrático. Java 21 + Spring Boot 3.x, arquitectura hexagonal, integrado por
API Gateway a la plataforma de la cátedra.

**UTN FRC · Tecnicatura Universitaria en Programación · Programación IV — Back End · 2.º año, 4.º cuatrimestre**

Esta carpeta es autocontenida: todo el TP vive acá adentro (backend multi-módulo, frontend de
prueba, documentación, laboratorio de integración y scripts). Se puede copiar/mover `llm-service/`
entera a otro lado y las rutas de este README siguen funcionando.

> **¿Sos nuevo/a en el proyecto?** Después de levantar el servicio (abajo), andá directo a
> [`docs/README.md`](docs/README.md) — es la puerta de entrada a toda la documentación vigente, con
> un recorrido guiado de 45 minutos y un [catálogo completo](docs/catalogo-documental.md) para
> encontrar cualquier tema sin explorar carpetas al azar. Para aportar código, leé
> [`CONTRIBUTING.md`](CONTRIBUTING.md).

---

## Cómo levantar el servicio

### Antes de la primera vez (una sola vez por máquina)

```bash
cp .env.example .env
openssl rand -base64 32        # pegar el resultado en LLM_CREDENTIALS_MASTER_KEY dentro de .env
```

Sin `LLM_CREDENTIALS_MASTER_KEY` ningún perfil arranca: `compose.yaml` la exige sin valor por
defecto.

### Los perfiles disponibles (`scripts/up.sh` / `scripts/down.sh`)

En vez de recordar qué combinación de `-f compose.*.yaml` corresponde a cada caso, usá el
dispatcher. Valida las variables de entorno necesarias, crea la red externa `tpi-platform` si
falta, y bloquea combinaciones inválidas (por ejemplo `mesh` + `server` juntos, que pisarían la
misma instancia en Eureka).

| Perfil | Qué levanta | Comando |
|---|---|---|
| `local` | Solo backend: Postgres + Kafka local + `llm-service` (sin frontend ni mocks) | `scripts/up.sh local` |
| `workbench` | + frontend Angular con hot reload, gateway-mock y courses-mock — el [laboratorio de integración local](docs/02-arquitectura-y-plataforma/07-laboratorio-integracion-local.md) | `scripts/up.sh workbench` |
| `debug` | Overlay: publica 5432/8086/8087 al host (cliente SQL, breakpoints) — combinable | `scripts/up.sh workbench debug` |
| `groq` | Overlay: usa el proveedor Groq real en vez del adaptador `fake` — combinable | `scripts/up.sh workbench groq` |
| `mesh` | Deploy en la mesh Tailscale, build local, se une a la plataforma real de la cátedra | `scripts/up.sh mesh` |
| `server` | Igual que `mesh`, pero con la imagen ya publicada (para correr en un servidor) | `scripts/up.sh server` |
| `shadow` | Segunda instancia aislada para recibir tráfico espejado sin efectos reales | `scripts/up.sh shadow` |

Los perfiles `local`/`workbench`/`debug`/`groq` se combinan entre sí (comparten `compose.yaml`
como base); `mesh`/`server`/`shadow` son stacks propios y no se mezclan con los anteriores.
`scripts/up.sh --help` lista todo, y `scripts/down.sh <mismos perfiles>` baja exactamente lo que
subió `up.sh`.

Con `workbench` arriba, abrir `http://localhost:4200/docente`. El navegador sólo usa rutas
relativas `/api/**`; el gateway-mock agrega la identidad delegada y rutea `/api/llm/**` al backend
real y `/api/courses/**` al mock de Cursos. Backend y Postgres nunca se exponen al navegador — en
la plataforma real, sólo el API Gateway publica puertos (ADR-015).

### Si preferís Docker Compose directo (sin el dispatcher)

```bash
docker network create tpi-platform     # una vez por máquina
docker compose up --build              # solo backend
docker compose -f compose.yaml -f compose.workbench.yaml up --build   # + frontend
docker compose down                    # apagar (agregar los mismos -f si se combinaron overlays)
```

Una vez que los tres servicios estén en pie, verificar que el backend responde:

```bash
# Requiere el overlay debug (compose.debug.yaml) para exponer el puerto al host,
# o ejecutar desde dentro de la red Docker:
curl -s http://localhost:8087/actuator/health | grep -q UP && echo "UP" || echo "DOWN"
```

Con el perfil `workbench` el gateway-mock expone el puerto; con el perfil `local` el puerto de management (8087) solo está accesible dentro de la red Docker o con el overlay `debug`.

Levanta tres servicios *healthy*: `postgres`, `kafka-local` (broker local para probar sin la
plataforma) y `llm-service`. `compose.yaml` fija `container_name: llm-service` (el nombre del
servicio, igual que en Eureka): si ya existe un contenedor con ese nombre de otro proyecto,
borrarlo (`docker rm llm-service`) o el `up` falla con `container name "/llm-service" is already
in use`. El smoke `scripts/smoke-compose.sh` no tiene ese problema: usa nombres propios y clave
descartable.

### Persistencia y debug local

Los datos de PostgreSQL viven en el volumen `llm-postgres-data` y sobreviven a `docker compose
down`; se borran solo con `docker compose down --volumes`.

Para conectarte a Postgres con un cliente SQL desde el host (uso de debug, no para la plataforma
real — ver [`compose.debug.yaml`](compose.debug.yaml)):

```bash
docker compose -f compose.yaml -f compose.debug.yaml up --build
# psql -h localhost -U llm -d llm
```

## Pruebas y cobertura

```bash
./mvnw clean compile                    # compilación rápida
./mvnw test -Dtest=NombreDelTest         # una clase puntual
./mvnw verify                            # suite completa con Testcontainers (Postgres + Kafka)
./mvnw jacoco:report                     # reporte en target/site/jacoco/index.html
```

Cobertura mínima exigida: **90%** en los paquetes de dominio (requisito de cada PR — ver
[`CONTRIBUTING.md`](CONTRIBUTING.md) y
[`docs/06-operacion-calidad-y-pruebas/02-convenciones-de-cobertura.md`](docs/06-operacion-calidad-y-pruebas/02-convenciones-de-cobertura.md)).

## Estructura del repositorio

| Carpeta | Qué es |
|---|---|
| [`app/`](app/) | Módulo principal: dominio, aplicación y adaptadores (arquitectura hexagonal) |
| [`provider-spi/`](provider-spi/), [`provider-anthropic/`](provider-anthropic/), [`provider-gemini/`](provider-gemini/), [`provider-openai-compatible/`](provider-openai-compatible/) | Módulos del SPI de proveedores de IA — cada uno un adaptador intercambiable detrás del AI Gateway interno |
| [`llm-workbench/`](llm-workbench/) | Frontend Angular 21 de referencia (docente, golden set, calibración) — no es el frontend final de la plataforma |
| [`docs/`](docs/) | Documentación vigente completa — empezar por [`docs/README.md`](docs/README.md) |
| [`docs/contracts/`](docs/contracts/) | Contratos ejecutables (OpenAPI/AsyncAPI) y vistas narrativas por equipo integrador |
| [`lab/`](lab/) | Configuración compartida del laboratorio de integración local (gateway-mock, courses-mock) que usa el perfil `workbench` |
| [`scripts/`](scripts/) | `up.sh`/`down.sh` (dispatcher de compose), smoke tests, verificación de la mesh Tailscale |
| [`AGENTS.md`](AGENTS.md) | Directrices operativas para agentes de IA que trabajen en este código (comandos, límites estrictos, estándares) |

## El problema

**Construimos el servicio que le pone nota a *cómo* un alumno usó la IA, y tenemos que demostrar que
esa nota es confiable.** Lo que lo vuelve difícil no es integrar un modelo de lenguaje:

1. **Es una IA evaluando a otra IA**, y el resultado modifica el XP — que define si un alumno
   promociona. Una nota mal puesta no es un bug: es un resultado académico que no se deshace.
2. **Hay que demostrar que evalúa como un humano.** Y para eso primero hay que lograr que **dos
   humanos se pongan de acuerdo entre ellos**, que es más difícil que el problema técnico.
3. **La restricción es asimétrica:** un único evaluador activo, sin fallback — pero su caída **no
   puede bloquear al alumno** (escalera de degradación). Un solo camino, y prohibido cortarlo.
4. **Hay que evitar que la IA filtre la solución, sin que la IA vea la solución.**
5. **El texto del alumno es a la vez el dato evaluado y un vector de ataque** (prompt injection).
6. **Dependemos de otros equipos y de docentes que no controlamos** para calibrar y aprobar.

## Decisiones cerradas (resumen)

| Decisión | Fundamento |
|---|---|
| **Java Spring Boot** para el servicio | La cátedra exige Java; la integración con Spring Cloud pesa más que el ecosistema de IA de Python |
| **Un microservicio, no varios** | Todas las funciones de IA comparten AI Gateway interno, guardarraíles, cuotas y log |
| **Sin orquestador basado en LLM** | La ruta la decide la UI; un router LLM agrega latencia, costo y superficie de injection |
| **Sincrónico solo para tutor y moderador** | El resto va por cola diferida (outbox + worker) |
| **langchain4j** como cliente de LLM | API uniforme por proveedor y salida estructurada, sin atar el servicio a un SDK propietario |
| **Kafka** para el bus de eventos de plataforma | La cola interna de trabajo diferido es aparte (Postgres `SKIP LOCKED`) |
| **La solución de referencia nunca entra al contexto del tutor** | No se puede filtrar lo que no se tiene |
| **El perímetro temático lo hace cumplir el retrieval, no el prompt** | Una instrucción se sortea hablando; un filtro en el servidor no |

El detalle y las decisiones vigentes completas —con fecha, motivo y qué las supera— viven en
[`docs/00-gobierno-y-evolucion/`](docs/00-gobierno-y-evolucion/README.md) (ADR aceptados en
[`docs/00-gobierno-y-evolucion/adr/`](docs/00-gobierno-y-evolucion/adr/)). Este resumen puede
desactualizarse; esa carpeta no.

## Fuentes

- [`docs/fuentes/PRD-Plataforma-Gamificada-TP.pdf`](docs/fuentes/) y
  [`docs/fuentes/TUP_PIV_BE_PROPUESTA_ARQ.pdf`](docs/fuentes/) — material de origen de la cátedra.
  Están **gitignorados** (`*.pdf`): viajan si copiás la carpeta a mano, no si clonás el repo. Se
  distribuyen también por los canales oficiales de la cátedra.

---

*Documentación viva. La fuente de verdad de la documentación técnica es [`docs/`](docs/README.md);
este README es la puerta de entrada operativa, no un índice completo.*
