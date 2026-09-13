# Tema 03 — Motor de Desafíos — contratos

> Fuente completa: [18 §4.2](../../18-contratos-inter-equipos.md#42-tema-03--motor-de-desafíos),
> [17 §5](../../17-mapa-de-integracion.md#5-camino-asincrónico--el-evaluador) (diagrama completo).

## Qué nos llama

- No hay un `POST` directo de "pedir evaluación" en el contrato v1 vigente — la evaluación se
  dispara **solo** al consumir el evento `intento_cerrado` (ver "Qué nos da"). El viejo
  `POST /ai/evaluador` era del contrato de seis endpoints, retirado.
- `GET /api/llm/evaluations/{evaluationId}` — para consultar el detalle después.
- `GET /api/llm/jobs/{jobId}` — para consultar el estado del trabajo asincrónico.

## Qué nos da

- Publica el evento `intento_cerrado.v1`, que dispara la evaluación asincrónica. Estructura
  mínima obligatoria: `trace_id`, `curso_cohorte_id`, `intento_id`, `alumno_id`,
  `rubric_version`, `transcripcion` — todos marcados **OBLIGATORIO** en 18 §3.

### Cuerpo del evento que necesitamos ✅ (contenido acordado en doc 18 §3)

```json
{
  "evento":           "intento_cerrado",
  "version":          "1.x",
  "trace_id":         "6d1f7a10-0000-4000-8000-000000000010",
  "timestamp":        "2026-09-12T14:32:00Z",
  "curso_cohorte_id": "b1e2c3d4-0003-4a00-8000-000000000003",
  "intento_id":       "b1e2c3d4-0005-4a00-8000-000000000005",
  "alumno_id":        "b1e2c3d4-0004-4a00-8000-000000000004",
  "rubric_version":   "v1.1",
  "transcripcion":    []
}
```

⚠️ El schema ejecutable (`AttemptClosed` en
[`llm-service-v1.asyncapi.yaml`](../../contracts/llm-service-v1.asyncapi.yaml)) hoy solo declara
`attemptId`, `courseCohortId`, `learnerId`, `transcript` como obligatorios — **`rubric_version`
y `trace_id` todavía no están en el YAML ejecutable**, aunque doc 18 los pida como
"OBLIGATORIO". Es parte de lo que hay que cerrar en I-04/I-05.

## Qué le damos

- `score_agregado` (0–100) con desglose por dimensión vía el evento `score_de_ia_calculado.v1`.
- **Nunca devolvemos XP.** El modificador (PAR-05) lo aplica el motor de desafíos, no nosotros.
- Si el evaluador no puede completar: `score_pendiente_diferido`, con `motivo` y
  `reintentar_desde`.

### Cuerpo de los eventos que publicamos

✅ **Contenido acordado, `score_de_ia_calculado`** (doc 18 §2.1):

```json
{
  "evento":           "score_de_ia_calculado",
  "version":          "1.0",
  "trace_id":         "6d1f7a10-0000-4000-8000-000000000010",
  "timestamp":        "2026-09-12T14:35:00Z",
  "curso_cohorte_id": "b1e2c3d4-0003-4a00-8000-000000000003",
  "intento_id":       "b1e2c3d4-0005-4a00-8000-000000000005",
  "alumno_id":        "b1e2c3d4-0004-4a00-8000-000000000004",
  "score_agregado":   78,
  "dimensiones": { "claridad": 25, "autonomia": 30, "progresion": 20, "cumplimiento": 15, "eficiencia": 10 },
  "confianza":        0.92,
  "rubric_version":   "v1.1",
  "model_id":         "claude-haiku-4.5",
  "model_version":    "batch-2025-05",
  "estado":           "aplicado"
}
```

⚠️ Igual que arriba: el schema ejecutable (`ScoreResult`) hoy es `Envelope` + `data: object`
**sin campos declarados** — este JSON es el contenido acordado en doc 18, todavía no volcado al
YAML. Es justamente **I-04**: mientras no se cierre, ni siquiera el schema tiene los campos.

🟡 **Propuesta — `score_pendiente_diferido`** (mismo estado: contenido en doc 18 §2.2, sin
volcar al schema ejecutable):

```json
{
  "evento":           "score_pendiente_diferido",
  "version":          "1.0",
  "trace_id":         "6d1f7a10-0000-4000-8000-000000000010",
  "timestamp":        "2026-09-12T14:35:00Z",
  "curso_cohorte_id": "b1e2c3d4-0003-4a00-8000-000000000003",
  "intento_id":       "b1e2c3d4-0005-4a00-8000-000000000005",
  "alumno_id":        "b1e2c3d4-0004-4a00-8000-000000000004",
  "motivo":           "proveedor_no_disponible",
  "reintentar_desde": "2026-09-12T15:00:00Z"
}
```

### Respuesta de `GET /evaluations/{evaluationId}` ✅ (schema real: `Evaluation`)

```json
{
  "id": "b1e2c3d4-0006-4a00-8000-000000000006",
  "aggregateScore": 78,
  "dimensions": { "clarity": 25, "autonomy": 30, "progression": 20, "compliance": 15, "efficiency": 10 },
  "confidence": 0.92,
  "rubricVersion": "v1.1",
  "modelId": "claude-haiku-4.5",
  "modelVersion": "batch-2025-05"
}
```

### Respuesta de `GET /jobs/{jobId}` ✅ (schema real: `Job` — más angosto de lo esperado)

```json
{
  "id": "b1e2c3d4-0007-4a00-8000-000000000007",
  "state": "completed"
}
```

⚠️ Notar que `Job` **no incluye el resultado** (`score_agregado` ni nada parecido) — solo el
`state` (`queued|running|completed|failed`). Si Tema 03 pensaba usar el polling de `jobs` para
obtener el score, hoy no alcanza: hay que ir a `GET /evaluations/{evaluationId}` aparte, o
cerrar I-04 con el evento. Otro argumento más para resolver I-04 con un solo mecanismo.

## Qué pasa si esto falla

Técnica común en
[transversales del README](../README.md#resiliencia-y-manejo-de-errores-técnica-común-a-todos-los-endpoints).
El evaluador **no tiene la escalera completa de degradación** — a diferencia del tutor, no
puede caer a "modelo local" ni a "degradación funcional": el score o es real, o no existe
todavía (04 línea 619). Por eso su única degradación válida es la **cola diferida**:

1. El worker llama al proveedor con `Idempotency-Key` — un reintento por timeout nunca duplica
   la evaluación.
2. Si el proveedor falla, reintenta con **backoff y tope**. Después de N intentos, el job pasa a
   `fallido`/`reintentando` según corresponda y **nunca reintenta infinito**.
3. Si se agota el margen, publicamos `score_pendiente_diferido` (payload en la sección
   anterior) — **el Backend/Tema 03 tiene que aceptar la entrega igual, con `score_agregado =
   null`** (ver [`backend-de-negocio/contratos.md`](../backend-de-negocio/contratos.md)).
4. `GET /course-cohorts/{courseCohortId}/pending-evaluations` — el contador de pendientes —
   sigue devolviendo esa evaluación mientras no se resuelva; **bloquea el cierre del curso**
   hasta que se resuelva o el docente haga un override manual.

## Acordado, no técnico

- **Aceptar la entrega con el evaluador caído**: si respondemos `503`, el backend acepta igual
  con `score_agregado = null` y espera `score_pendiente_diferido`. La resiliencia de este punto
  es del lado que escribe en la base académica, no del nuestro.
