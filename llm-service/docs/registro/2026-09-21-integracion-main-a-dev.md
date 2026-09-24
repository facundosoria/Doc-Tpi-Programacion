# Integración `main` → `dev` sobre la estructura multi-módulo — 2026-09-21

## Decisión anterior

`main` y `dev` venían construyendo el mismo servicio con dos arquitecturas distintas y ninguna
de las dos ramas era descartable:

- **`main`** (commits de Brf93 y Quevedo, 2026-09-14 → 2026-09-21) refactorizó `llm-service` a
  reactor Maven multi-módulo con un SPI de proveedores (`afecaf93`), reemplazó el catálogo de
  cursos simulado por resolución real contra courses-service (`495f148a`), trajo su propio
  esqueleto Kafka (`V24__kafka_inbox_deduplication`, `V25__kafka_transactional_outbox`) y el
  vencimiento de calibraciones (`V27`, Épica 364).
- **`dev`** siguió sobre el árbol plano `llm-service/src` con `GroqModelAdapter`,
  `ProviderLlmGateway` y `WorkbenchDemoCatalog`, y construyó encima moderación (EP-08), RAG
  (EP-09), mención al agente (EP-10), shadow runs y su propio esqueleto Kafka con outbox
  transaccional, deduplicación por `eventId` y dead-letter en tabla (`V29`–`V32` de `dev`).

## Motivo

Entregar una sola rama. Sostener las dos obligaba a portar cada historia nueva dos veces y dejaba
sin correr los ITs de `dev` contra la arquitectura que realmente se va a entregar.

## Regla vigente

La rama `integracion/main-a-dev` (merge `d3575967` + 9 commits de corrección) **adopta la
arquitectura de `main` y conserva las funcionalidades de `dev`**. Lo que quedó decidido:

| Tema | Gana | Qué se fue |
|---|---|---|
| Estructura | Reactor multi-módulo de `main` (`app` + `provider-spi` + 3 adaptadores) | El árbol plano `llm-service/src` |
| Proveedores LLM | SPI (`ProviderRegistry`, `ProviderInvocationGateway`, módulos `provider-*`) | `GroqModelAdapter`, `ProviderLlmGateway` y sus 4 tests |
| Mensajería | Kafka de `dev` (outbox + dedup + dead-letter, `V35`/`V36`/`V38`) | `V24`/`V25` de `main` y sus 4 clases (`KafkaEventPublisher`, `KafkaOutboxDispatcher`, `KafkaAttemptEventsListener`, `ProcessedEventRepository`) |
| Tópico de entrada | `practice-events`, como dice el AsyncAPI vigente | `intento_cerrado.v1` (nombre histórico que usaba `main`) |
| Cursos | `GatewayCoursesMembershipClient` contra courses-service | `CourseContextController` (`GET /api/llm/courses`) y `WorkbenchDemoCatalog` |
| Embeddings | `FakeEmbeddingAdapter` pasa a `app/src/main` como bean de producción | — |
| Scope del tutor | `llm.tutor.interact` (el que usan el contrato y el código) | `llm.tutor.invoke` |

Además, tres correcciones de producción que aparecieron al correr por primera vez los ITs de
`dev` contra el esquema de `main`: el mapeo de `ProviderException` en `ApiExceptionHandler`
(422/503 en vez de 500), el seed nulo de `CalibrationRunRepository.execution` (rompía toda corrida
del worker) y `baseUrl` HTTP admitido sólo contra loopback en el adaptador OpenAI-compatible.

### Consecuencia operativa: las migraciones se renumeraron

Para dejar lugar a `V23`, `V26` y `V27` de `main`, **las 11 migraciones de `dev` desde la `V22`
corrieron +6**:

```
V22 tutor_conversations_and_rag        -> V28      V28 add_degradation_reason  -> V34
V23 seed_evaluator_function            -> V29      V29 event_outbox            -> V35
V24 moderation_decisions_audit         -> V30      V30 kafka_consumed_events   -> V36
V25 create_moderation_appeals_table    -> V31      V31 shadow_evaluation       -> V37
V26 create_moderation_resolutions_table-> V32      V32 event_outbox_drop_...   -> V38
V27 moderation_retention_and_purge     -> V33
```

La cadena vigente es `V1`–`V23`, `V26`–`V38`: **el hueco `V24`/`V25` es intencional** (eran las de
Kafka de `main`, descartadas).

> **Toda base de datos creada antes de esta integración queda inválida.** Venga de `dev` (números
> corridos) o de `main` (`V24`/`V25` aplicadas y sin archivo local), Flyway va a fallar la
> validación al arrancar. Hay que recrear la base: `docker compose down -v` y volver a levantar.
> Las bases limpias —CI, Testcontainers— no se ven afectadas.

## Fuentes

Ramas `origin/main` y `origin/dev` del repositorio; commits `d3575967`…`b4e5c1c5`.

## Documentos V2 corregidos

`02-arquitectura-y-plataforma/04-estructura-del-backend.md` (§3 y §4: paquetes reales tras la
integración), `02-arquitectura-y-plataforma/05-servicios-docker.md` y `contracts/MOCK.md`
(sin `WorkbenchDemoCatalog`), `06-operacion-calidad-y-pruebas/04-estado-de-implementacion/`
(`README.md`, `ep-01/h08.md`, `ep-02/README.md`, `pendiente-de-epica/`),
`registro/2026-09-20-estandar-kafka-del-pdf.md` (la dead-letter es `V38`, no `V32`).

El contrato `contracts/llm-service.openapi.yaml` **no necesitó cambios**: ya declaraba
`/discover-models` y `/test-model`, y nunca publicó ni `POST /admin/provider-credentials/{id}/test`
ni `GET /api/llm/courses` — los dos endpoints que desaparecieron eran internos del workbench.

## Responsables

Merge y corrección de tests: Facundo Soria. Correcciones del SPI, seed y scope: Chachagua.
Arquitectura de origen: Brf93 (multi-módulo, SPI, cursos) y Quevedo (vencimiento de calibraciones).

## Evidencia de prueba

Verificado el 2026-09-21 sobre `b4e5c1c5`:

- `mvn clean install` — BUILD SUCCESS en los 6 módulos (277 fuentes de `main`, 150 de test).
- **674 tests unitarios**, 0 fallos, 2 skipped.
- **99 tests de integración** en 20 clases `*IT`, 0 fallos, con Testcontainers (Postgres+pgvector)
  y EmbeddedKafka: `ContextLoadsIT` levanta el contexto con las 34 migraciones aplicadas, y
  `CalibrationFlowIT`, `TutorRagIT`, `ProviderChatIT`, `CredentialFlowIT`, `EventOutboxKafkaFlowIT`,
  `AgentMentionControllerIT` y los 4 de moderación cubren el flujo de cada épica.
- Inventario de rutas HTTP: **82 en la rama integrada contra 81 en `dev`**; ninguna función de
  equipo se perdió por accidente. Las 12 clases de `dev` que desaparecieron corresponden a los
  reemplazos deliberados de la tabla de arriba.

## Pendientes que deja abiertos

1. `FakeEmbeddingAdapter` es la única implementación de `EmbeddingPort` y ahora es bean de
   producción sin `@Profile`: en cualquier despliegue los embeddings del RAG son falsos hasta que
   exista el adaptador real (EP-09).
2. `ProviderCredentialControllerTest` se perdió en el merge sin reemplazo unitario; hoy el
   controller queda cubierto sólo por `CredentialFlowIT`.
3. Quedan 4 clases en `infrastructure/` (agente y RAG) fuera del árbol `adapter/` que fija
   [`04-estructura-del-backend.md`](../02-arquitectura-y-plataforma/04-estructura-del-backend.md).
