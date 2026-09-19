# EP-01 · Plataforma, contratos e integración — estado

> Fichas fuente: [`docs/historias/ep-01/`](../../historias/ep-01/README.md). Auditoría de
> código: [`llm-service/CORRECCIONES-SUGERIDAS.md`](../../../CORRECCIONES-SUGERIDAS.md)
> ítems 16–22 (primera vez que estos habilitadores se contrastan contra código; hasta el
> 2026-09-12 ninguna ficha de EP-01 tenía marcador de estado).

## Actualización 2026-09-18 (revisión contra el código actual)

> Lo que sigue **reemplaza** el estado anterior de las fichas h01–h09 de esta carpeta, que quedó
> desactualizado (decían "sin ArchUnit", "sin Eureka", "sin JaCoCo", "no existe `.env`"). Umbral de
> cobertura vigente: **90 %**.

| ID | Título | Estado | Nota en una línea |
|---|---|---|---|
| H01 | ADR y convenciones | 🟡 | ADR escrito en [`../../adr/ADR-001-...`](../../adr/ADR-001-arquitectura-y-convenciones-llm-service.md) (falta PR con revisión); ArchUnit (7 reglas) ya existe |
| H02 | Entorno con un comando | 🟢 | Corregidos puertos de health (8087) en smoke, restart, README y Dockerfile; `.env.example` completo |
| H03 | Esqueleto transversal | 🟢 | Eureka, 401/403 `problem+json` y `X-*` del Gateway OK; Bearer sin firma apagado por defecto (`app.security.trust-unsigned-bearer`). `ModerationCourseAuthorization` ahora respeta el mismo flag: sin `trust-unsigned-bearer` ignora el Bearer sin firma (identidad, roles y cursos) — cerrado 2026-09-19 |
| H04 | Esquema versionado | 🟢 | Triggers append-only e idempotencia en V1; `V13` duplicada renombrada a `V13_1` (ya commiteada; Flyway aplica V13 y luego V13_1) |
| H05 | Contrato y mock | 🟢 | `docs/contracts/` + `MOCK.md` (Prism) |
| H06 | Tests y cobertura | 🟡 | 457 tests en verde; cobertura total ≈59 % vs 90 %. Gate JaCoCo con piso 55 % (ratchet) y CI en `.github/workflows/llm-service-ci.yml` |
| H07 | Kafka + dedupe | 🟢 | Implementado 2026-09-18: outbox transaccional (`event_outbox`, V29) + `EventOutboxRelay` (CA1), dedup consumidor (`kafka_consumed_events`, V30) vía `KafkaConsumedEventsRepository` (CA2/CA3), dead-letter en `<topic>.dlt` (CA4). `ModerationEventPublisher` publica `MESSAGE-UNBLOCKED` en `moderation-events` (key=`courseId`); `PracticeAttemptClosedListener` consume `practice-events` con fixture provisorio. Cubierto por `EventOutboxKafkaFlowIT` (`EmbeddedKafkaBroker`). Apagado por defecto (`llm.kafka.enabled=false`; `docker compose` lo enciende) hasta integrar con el broker real de cátedra — ver [`docs/contracts/KAFKA_EVENT_STANDARD.md`](../../contracts/KAFKA_EVENT_STANDARD.md) y [`llm-service-v1.asyncapi.yaml`](../../contracts/llm-service-v1.asyncapi.yaml) v2.0.0. CA5 verificado en `scripts/test-compose-restart.sh` (paso 4b/7: inserta un `eventId` antes del `docker compose restart` y comprueba que sigue tras el reinicio; solo versión bash, falta el `.ps1`). Pendiente: contrato real de `practice-events` sigue en fixture hasta que Tema 05 lo congele |

---

*Detalle histórico previo:*

## Índice

| ID | Título | Estado | Nota en una línea |
|---|---|---|---|
| [H01](h01.md) | ADR de arquitectura y convenciones técnicas | 🔴 | No existe ningún ADR de `llm-service` en el repo |
| [H02](h02.md) | Entorno reproducible con un comando | 🟡 | `docker compose` existe; falta `.env.example` y documentar `down` |
| [H03](h03.md) | Esqueleto transversal del servicio | 🔴 | No se registra en Eureka; nunca devuelve `401` (todo es `403`) |
| [H04](h04.md) | Esquema inicial versionado con auditoría | 🟢 | Migración `V1` cumple lo que pide la ficha |
| [H08](h08.md) | Contrato OpenAPI y mock del golden set publicados | 🟡 | Contratos publicados; no hay mock levantable con un comando |
| [H09](h09.md) | Suite de pruebas y guía de demo de S1 | 🔴 | 32 clases de test, pero sin JaCoCo no hay gate de cobertura; sin guía de demo |
