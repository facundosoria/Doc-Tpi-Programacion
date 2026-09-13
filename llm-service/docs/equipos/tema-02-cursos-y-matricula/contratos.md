# Tema 02 — Cursos y Matrícula — contratos

> Fuente completa: [18 §4.1](../../18-contratos-inter-equipos.md#41-tema-02--cursos-y-matrícula).

## Qué nos llama

- `GET /ai/calibracion/{curso_cohorte_id}` → hoy es
  `GET /api/llm/course-cohorts/{courseCohortId}/calibration` en el contrato v1. Verifica si la
  calibración está aprobada antes de activar el curso. Es uno de los dos endpoints de mayor
  prioridad de entrega del contrato: **bloquea la activación de cursos si no existe**, aunque
  devuelva un mock.

### Cuerpo de la respuesta 🟡 (propuesta — el schema real no define campos todavía)

El OpenAPI vigente solo describe este `200` como *"Estado de calibración"*, sin schema. Hasta
que se formalice, esta es la forma que ya circula en doc 18 §2.3 para los eventos equivalentes,
adaptada a respuesta síncrona:

```json
{
  "aprobada": true,
  "rubricVersion": "v1.1",
  "kappa": 0.82,
  "muestras": 30,
  "calibradoEn": "2026-09-10T14:00:00Z"
}
```

## Qué nos da

- Confirmación del modelo `curso_template_id` vs `curso_cohorte_id` — ver
  [`pendientes.md`](pendientes.md) (I-09).
- Evento `curso_archivado.v1` — frena todos los trabajos pendientes de ese curso-cohorte.

### Cuerpo del evento 🟡 (propuesta — el schema ejecutable hoy no define campos)

`CourseArchived` en el AsyncAPI vigente es `Envelope` + objeto vacío: no hay ni un campo
propio declarado todavía. Propuesta mínima, simétrica con `AttemptClosed` (el único evento que
sí tiene campos formalizados):

```json
{
  "eventId": "6d1f7a10-0000-4000-8000-000000000099",
  "version": "1.0",
  "occurredAt": "2026-09-12T14:00:00Z",
  "producer": "courses-service",
  "data": {
    "courseCohortId": "b1e2c3d4-0003-4a00-8000-000000000003"
  }
}
```

⚠️ **Nota sobre `POST /ai/ingesta`:** el contrato de seis endpoints (retirado) tenía un
endpoint dedicado para que Tema 02 mande el material del curso a indexar. **El contrato v1
vigente no lo tiene** — no hay ninguna ruta de ingesta en
[`llm-service-v1.openapi.yaml`](../../contracts/llm-service-v1.openapi.yaml). Como el RAG es de
Fase 2/3 (fuera del MVP, doc 18 §"Estado vigente"), esto puede ser intencional — pero si Tema 02
todavía espera este endpoint, es un pendiente nuevo a confirmar, no solo un olvido de
redacción. Ver [`pendientes.md`](pendientes.md).

## Qué le damos

- Estado de calibración vía `GET /course-cohorts/{courseCohortId}/calibration` (ejemplo arriba).
- Eventos `calibracion_aprobada.v1` / `calibracion_fuera_de_tolerancia.v1`, con el mismo
  contenido propuesto (`rubric_version`, `kappa`, `muestras`) — ver
  [`tema-12-backoffice-admin/contratos.md`](../tema-12-backoffice-admin/contratos.md), que
  detalla ese payload porque Tema 12 es el otro consumidor.

## 🔴 Bloqueo crítico (no es un pendiente de definición, es una dependencia externa)

El **golden set** (muestras del docente para calibrar la rúbrica). Sin esto ningún curso puede
activarse — es la dependencia con el plazo más largo del proyecto, y no es trabajo de
desarrollo de ningún equipo. Ver [`product-owner/pendientes.md`](../product-owner/pendientes.md).
