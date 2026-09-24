# Contribuir a `llm-service`

Guía para poder aportar código o documentación a este microservicio sin tener que preguntar todo
de nuevo. Si estás por tu primera tarea, leé esto entero una vez; después usalo como referencia.

## 1. Antes de escribir una línea

1. Levantá el servicio local siguiendo el [`README.md`](README.md) (`scripts/up.sh workbench` es
   el camino más rápido para ver algo andando).
2. Leé [`docs/README.md`](docs/README.md) — el recorrido de 45 minutos y el
   [catálogo documental](docs/catalogo-documental.md). En particular, antes de tocar código:
   - [`docs/00-gobierno-y-evolucion/`](docs/00-gobierno-y-evolucion/README.md): qué documento manda
     si dos fuentes parecen decir cosas distintas.
   - [`docs/contracts/`](docs/contracts/README.md): obligatorio antes de exponer un endpoint,
     consumir datos o publicar un evento.
   - [`docs/06-operacion-calidad-y-pruebas/`](docs/06-operacion-calidad-y-pruebas/README.md):
     obligatorio antes de dar una tarea por terminada.
3. Leé [`AGENTS.md`](AGENTS.md) — comandos de verificación, límites estrictos (qué nunca hacer sin
   aprobación explícita) y los estándares de código de esta base, con ejemplos buenos y malos de
   Java y Angular. Aplica tanto si programás vos como si usás un agente de IA.

Si vas a tomar una historia de usuario formal, seguí el recorrido de
[`docs/07-planificacion-y-trabajo-equipo/`](docs/07-planificacion-y-trabajo-equipo/README.md)
(épica → historia → tarea → sprint).

## 2. Reglas que no se negocian

Tomadas de [`AGENTS.md`](AGENTS.md) — repetidas acá porque son las que más caro salen si se
rompen sin querer:

- **Las bases de datos son sagradas.** Prohibido crear migraciones Flyway, alterar tablas o
  ejecutar DDL/DML destructivo sin instrucción explícita del usuario/responsable. Nunca toques la
  base de datos de otro microservicio.
- **Cero improvisación de dominio.** Si una tarea necesita un dato que no existe en la BD o en el
  contrato OpenAPI: parate y consultá. No inventes columnas ni datos falsos permanentes.
- **Nunca saltear el AI Gateway interno.** Ningún componente llama a un proveedor LLM
  (Anthropic, OpenAI-compatible, Gemini) directamente; todo pasa por el módulo `app` que orquesta
  los `provider-*`.
- **Nunca borres ni comentes un test que falla** para que el pipeline pase en verde. Si falla, hay
  una regresión que corregir.
- **Nunca filtres secretos** (API keys, prompts del sistema, la solución esperada de un desafío) en
  logs, respuestas o commits.
- **Consultá primero** si tu cambio altera un contrato OpenAPI/AsyncAPI vigente, agrega una
  dependencia nueva, o si al resolver X encontrás que Y está roto — no lo arregles por tu cuenta
  sin avisar, repórtalo.

## 3. Flujo de trabajo (branches y commits)

El flujo de ramas y la rutina diaria del equipo están documentados en
[`docs/09-flujo-de-trabajo-del-equipo/`](docs/09-flujo-de-trabajo-del-equipo/README.md)
([gitflow](docs/09-flujo-de-trabajo-del-equipo/01-gitflow.md) y
[workflow diario](docs/09-flujo-de-trabajo-del-equipo/02-workflow-diario.md)). En resumen:

1. Ramá desde la base indicada en esa guía, con un nombre descriptivo de la tarea.
2. Commits chicos y con mensaje que explique el *por qué*, no solo el *qué* (el diff ya muestra el
   qué).
3. Antes de abrir el PR, corré la verificación completa (sección 5) y confirmá que tu cambio no
   dejó nada a medio hacer.
4. En la descripción del PR: qué cambiaste, qué pruebas corriste, y cómo verificaste el escenario
   puntual pedido (caso exitoso *y* casos de error).

## 4. Estándares de código (resumen — el detalle con ejemplos está en `AGENTS.md`)

### Backend (Java 21 + Spring Boot 3.x)

- `record` inmutable para todo DTO, comando y evento Kafka.
- Inyección por constructor. Prohibido `@Autowired` sobre campos.
- Arquitectura hexagonal estricta: `domain` no importa Spring/JPA/Kafka; las reglas de negocio
  viven ahí. `application` orquesta casos de uso. `adapter/in` y `adapter/out` son las únicas
  puertas de entrada/salida. El árbol vigente está en
  [`docs/02-arquitectura-y-plataforma/04-estructura-del-backend.md`](docs/02-arquitectura-y-plataforma/04-estructura-del-backend.md).
- Errores con `@RestControllerAdvice` devolviendo RFC 7807 (`ProblemDetail`), nunca stacktraces.
- Propagar `traceparent` y `X-Request-Id` en headers, logs y eventos.

### Frontend (Angular 21, en `llm-workbench/`)

- Componentes 100% standalone, cero `NgModule`.
- Control flow nativo (`@if`, `@for`, `@switch`) — prohibidas las directivas legacy (`*ngIf`,
  `*ngFor`).
- `signal()`/`computed()` para estado reactivo, `inject()` en vez de inyección por constructor,
  `ChangeDetectionStrategy.OnPush` siempre.
- Triada `.ts`/`.html`/`.scss` separada — `template`/`styles` inline solo para componentes
  atómicos triviales (menos de 3 líneas de markup).

## 5. Verificación antes de abrir un PR

```bash
# Backend
./mvnw clean compile
./mvnw verify                 # suite completa con Testcontainers (Postgres + Kafka)
./mvnw jacoco:report          # target/site/jacoco/index.html

# Frontend (desde llm-workbench/)
npm test                      # ng test
npm run build                 # ng build (producción por defecto en Angular 21)
```

- **Cobertura mínima: 90%** en los paquetes de dominio del backend y del frontend — requisito de
  cada PR. Convenciones exactas en
  [`docs/06-operacion-calidad-y-pruebas/02-convenciones-de-cobertura.md`](docs/06-operacion-calidad-y-pruebas/02-convenciones-de-cobertura.md).
- Si tu cambio toca `compose.yaml` o el arranque del contenedor, corré
  `bash scripts/test-compose-restart.sh` (o `scripts/test-compose-restart.ps1` en Windows) para
  verificar que el servicio reinicia limpio.
- Si tu cambio toca un endpoint o evento, verificá el escenario real de punta a punta: caso exitoso
  (2xx) **y** casos de error (400/401/403/404/5xx) — no alcanza con que compile.
- Si tu cambio toca `docs/`, seguí las convenciones de
  [`docs/00-gobierno-y-evolucion/01-fuentes-de-verdad-y-convenciones.md`](docs/00-gobierno-y-evolucion/01-fuentes-de-verdad-y-convenciones.md)
  y registrá la corrección en [`docs/registro/`](docs/registro/README.md) si cambiaste una regla
  vigente.

## 6. Contratos entre equipos

Si tu cambio afecta a otro equipo integrador (expone un endpoint nuevo, cambia un payload, publica
o consume un evento):

1. Actualizá el contrato ejecutable (`docs/contracts/llm-service.openapi.yaml` o
   `.asyncapi.yaml`) en el mismo cambio.
2. Actualizá la vista narrativa de la contraparte en
   [`docs/contracts/equipos/`](docs/contracts/equipos/README.md).
3. Registrá el cambio en [`docs/registro/`](docs/registro/README.md): qué decía antes, por qué se
   modificó, qué dice ahora.

No copies un ejemplo de payload en un documento narrativo para "explicarlo mejor": el YAML es la
única definición técnica de campos y tipos.

## 7. Dudas

- **¿No sabés si algo ya está decidido?** Buscá primero en
  [`docs/00-gobierno-y-evolucion/02-decisiones-y-pendientes.md`](docs/00-gobierno-y-evolucion/02-decisiones-y-pendientes.md)
  antes de reabrir la discusión.
- **¿Encontraste una contradicción entre dos documentos?** No seas quien decide cuál vale por
  intuición: aplicá el orden de precedencia de
  [`docs/00-gobierno-y-evolucion/01-fuentes-de-verdad-y-convenciones.md`](docs/00-gobierno-y-evolucion/01-fuentes-de-verdad-y-convenciones.md)
  y, si corregís la fuente canónica, dejá constancia en `docs/registro/`.
- **¿Te falta un dato de la cátedra (BD, contrato, alcance)?** Es un ALTO INMEDIATO — no lo
  inventes. Consultá antes de seguir (sección 2).
