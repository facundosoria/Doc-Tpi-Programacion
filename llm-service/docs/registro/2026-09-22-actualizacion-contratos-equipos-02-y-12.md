# Actualización de `tema-02` y `tema-12`, y contratos faltantes por completitud — 2026-09-22

## Decisión anterior

`docs/contracts/equipos/tema-02-cursos-y-matricula.md` y `tema-12-backoffice-admin.md` describían
rutas retiradas (`GET /ai/calibracion/{curso_cohorte_id}`, `POST /ai/calibracion`) y usaban el
envelope Kafka anterior (`evento`/`trace_id`/`curso_cohorte_id`, sin `eventType`), superado por el
estándar del PDF adoptado el 2026-09-20 (ver
[`2026-09-20-estandar-kafka-del-pdf.md`](2026-09-20-estandar-kafka-del-pdf.md)). Ninguno de los dos
llevaba el aviso de vigencia que sí tienen `tema-05-desafios-practicos.md` y `tema-11-chat.md`.

Tampoco existía documentación de contrato para Tema 06 (Sandbox de ejecución), Tema 08 y Tema 09
(microservicios por confirmar) ni Tema 10 (XP/economía) — cuatro de los doce temas de la cátedra
no tenían una entrada explícita de "sin contrato directo" en `equipos/`.

## Motivo

Se comparó `docs/contracts/` contra una carpeta externa de contratos pulidos aportada por el
usuario (`/Users/rcoleman/Documents/TPI_4to_semestre/Contratos/`, 12 archivos por tema + un
contrato general). La comparación encontró que esa carpeta externa **tampoco** estaba
completamente alineada con el estándar vigente (sus ejemplos de eventos Kafka usan el envelope
anterior a la actualización del 20/09, y algunos campos de payload no coinciden con
`llm-service.asyncapi.yaml`), así que no se copió nada de ahí sin verificar contra el schema
ejecutable (`llm-service.openapi.yaml` / `llm-service.asyncapi.yaml`).

Se usó la carpeta externa como guía de qué contenido faltaba (el catálogo de calibración de
Courses, y los cuatro temas sin documentar), pero cada dato final se verificó contra el schema
real antes de escribirlo.

## Regla vigente

- `tema-02-cursos-y-matricula.md`: rutas actualizadas a las reales del OpenAPI
  (`/api/llm/courses/{courseId}/calibrations`, etc.); agrega referencia a la nueva sección
  RQ-CS-03 de `requisitos-a-otros-micros.md` (catálogo de calibración de Courses); el evento
  `CALIBRATION_APPROVED`/`CALIBRATION_OUT_OF_TOLERANCE` queda marcado explícitamente como
  `Envelope` sin `payload` definido — no se inventan campos.
- `tema-12-backoffice-admin.md`: mismo criterio — rutas reales, envelope de 5 campos, y
  `MODEL_CHANGED`/`CALIBRATION_APPROVED`/`CALIBRATION_OUT_OF_TOLERANCE`/
  `JAILBREAK_INCIDENT_DETECTED` marcados sin `payload` acordado donde el schema así lo declara
  (verificado uno por uno contra `llm-service.asyncapi.yaml`, no asumido).
- `requisitos-a-otros-micros.md`: nueva sección **RQ-CS-03 — Catálogo de cursos y desafíos
  calibrables**, con los campos de sincronización completa/incremental que Tema 07 necesita de
  Courses.
- Nuevos: `tema-06-sandbox-ejecucion.md`, `tema-08-microservicio-por-confirmar.md`,
  `tema-09-microservicio-por-confirmar.md`, `tema-10-xp-economia.md` — los cuatro declaran
  explícitamente "sin contrato directo" / "sin fuentes disponibles", sin inventar ninguna
  integración.
- `equipos/README.md`: índice actualizado con las cuatro entradas nuevas.

## Documentos V3 corregidos o agregados

**Actualizados:** `docs/contracts/equipos/tema-02-cursos-y-matricula.md`,
`docs/contracts/equipos/tema-12-backoffice-admin.md`,
`docs/contracts/requisitos-a-otros-micros.md`, `docs/contracts/equipos/README.md`.

**Agregados:** `docs/contracts/equipos/tema-06-sandbox-ejecucion.md`,
`docs/contracts/equipos/tema-08-microservicio-por-confirmar.md`,
`docs/contracts/equipos/tema-09-microservicio-por-confirmar.md`,
`docs/contracts/equipos/tema-10-xp-economia.md`.

**No se agregó** ningún archivo histórico nuevo ni una copia paralela de la carpeta externa: la
carpeta `/Users/rcoleman/Documents/TPI_4to_semestre/Contratos/` vive fuera de este repositorio y
no se tocó.
