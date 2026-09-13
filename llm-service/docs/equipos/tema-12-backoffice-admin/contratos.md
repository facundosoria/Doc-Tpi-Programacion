# Tema 12 — Backoffice / ADMIN — contratos

> Fuente completa: [18 §4.5](../../18-contratos-inter-equipos.md#45-tema-12--backoffice--admin).

## Qué nos llama

- `GET /ai/calibracion/{curso_cohorte_id}` → `GET /api/llm/course-cohorts/{courseCohortId}/calibration`
  — ver estado (mismo cuerpo que en
  [`tema-02-cursos-y-matricula/contratos.md`](../tema-02-cursos-y-matricula/contratos.md)).
- `POST /ai/calibracion` → `POST /api/llm/calibrations` — disparar recalibración.

### Cuerpo de la solicitud ✅ (schema real de `/calibrations`)

```json
{
  "courseCohortId": "b1e2c3d4-0003-4a00-8000-000000000003",
  "goldenSetId": "b1e2c3d4-0008-4a00-8000-000000000008"
}
```

Respuesta: `202` con header `Location` apuntando al job (`GET /jobs/{jobId}`, ejemplo de
[`tema-03-motor-de-desafios/contratos.md`](../tema-03-motor-de-desafios/contratos.md) — mismo
schema `Job`, no trae el resultado de la calibración, solo el `state`).

## Qué nos da

- Evento `modelo_llm_cambiado.v1` — dispara recalibración automática (RF-IA-32).

### Cuerpo del evento 🟡 (propuesta — el schema ejecutable (`ModelChanged`) no define campos)

```json
{
  "eventId": "6d1f7a10-0000-4000-8000-00000000009a",
  "version": "1.0",
  "occurredAt": "2026-09-12T09:00:00Z",
  "producer": "admin-service",
  "data": {
    "provider": "anthropic",
    "modelId": "claude-haiku-4.5",
    "previousModelId": "claude-haiku-4.0",
    "scope": "platform"
  }
}
```

`scope` (`platform` vs. `curso_cohorte_id` puntual) es una suposición nuestra — RF-IA-32 no
aclara si el cambio de modelo dispara recalibración a nivel plataforma o por curso; es parte
de lo que falta definir con ellos.

## Qué le damos

- Eventos `calibracion_aprobada.v1` / `calibracion_fuera_de_tolerancia.v1` (consumidores:
  Tema 12).
- Evento `incidente_de_jailbreak.v1` (consumidores: Tema 12 + equipo de seguridad).
- Sin estos endpoints/eventos, Tema 12 "no tiene nada demostrable" (17 §7.3).

### Cuerpo de los eventos que publicamos

✅ **Contenido acordado, `calibracion_aprobada`** (doc 18 §2.3) — mismo aviso que en Tema 02:
el schema ejecutable (`CalibrationResult`) hoy es `Envelope` + objeto vacío, este contenido
está acordado en doc 18 pero no volcado al YAML todavía:

```json
{
  "evento":           "calibracion_aprobada",
  "version":          "1.0",
  "trace_id":         "6d1f7a10-0000-4000-8000-00000000009b",
  "timestamp":        "2026-09-12T09:05:00Z",
  "curso_cohorte_id": "b1e2c3d4-0003-4a00-8000-000000000003",
  "rubric_version":   "v1.1",
  "kappa":            0.82,
  "muestras":         30
}
```

`calibracion_fuera_de_tolerancia` usa el mismo cuerpo (evento distinto, mismos campos).

🟡 **Propuesta — `incidente_de_jailbreak`** (contenido de doc 18 §2.4, mismo estado: falta
volcarlo al schema `Incident`):

```json
{
  "evento":           "incidente_de_jailbreak",
  "version":          "1.0",
  "trace_id":         "6d1f7a10-0000-4000-8000-00000000009c",
  "timestamp":        "2026-09-12T09:10:00Z",
  "curso_cohorte_id": "b1e2c3d4-0003-4a00-8000-000000000003",
  "alumno_id":        "b1e2c3d4-0004-4a00-8000-000000000004",
  "funcion":          "tutor",
  "tipo":             "injection",
  "severidad":        "alta"
}
```

## Qué pasa si esto falla

Técnica común en
[transversales del README](../README.md#resiliencia-y-manejo-de-errores-técnica-común-a-todos-los-endpoints).
Una corrida de calibración llama al proveedor una vez por caso del golden set — falla igual
que el evaluador ([`tema-03-motor-de-desafios/contratos.md`](../tema-03-motor-de-desafios/contratos.md)):
Circuit Breaker por proveedor, backoff con tope, y el job queda `fallido` si se agota el
margen (nunca "degradación funcional": una calibración con modelo local o degradado no sirve
para aprobar RF-IA-36).

Si Tema 12 dispara `POST /calibrations` y el job termina `fallido`, no hay hoy un evento
específico de "calibración no se pudo correr" — solo `calibracion_fuera_de_tolerancia` (que es
un resultado, no un error de infraestructura). Confirmar si hace falta distinguir "no corrió"
de "corrió y no pasó PAR-14" — hoy se pierde esa distinción.

## Acordado

- Tema 12 es dueño de la pantalla de configuración del proveedor LLM.
