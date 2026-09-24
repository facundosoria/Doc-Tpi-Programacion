# Runbook — AI Gateway (llm-service)

## Cómo funciona una llamada
`ModelInvocationService.invoke` → busca la función en `function_model_config` → elige el adaptador
por proveedor → chequea presupuesto → chequea circuit breaker del proveedor → llama con timeout y
hasta 3 intentos → valida el formato → registra latencia/costo/resultado.

La política es configurable sin tocar código: `llm.gateway.resilience.*` (`max-attempts`, `backoff-ms`,
`breaker-min-calls`, `breaker-failure-rate`, `breaker-open-wait-s`, o las variables `LLM_GATEWAY_*`) y
`llm.gateway.pricing.usd-per-1k-tokens.<proveedor>`. Embeddings y el chat de prueba admin pasan por el mismo camino.

## Catálogo de modelos
- Proveedores con adaptador: `fake` (pruebas) y `groq` (real, requiere `GROQ_API_KEY`).
- Ver/cambiar el modelo de una función: `GET`/`PUT {private-path}/model-assignments/{function}`
  (`tutor`, `evaluator`, `moderator`, `generator`, `embedding`). Rige de inmediato, sin redeploy.
- Solo `TUTOR` y `EVALUATOR` tienen schema de salida; asignar otra función a un proveedor hace fallar la validación.

## Ver consumo
- `GET {private-path}/admin/gateway/usage` — resumen por función y estado del presupuesto.
- `GET {private-path}/admin/gateway/calls?limit=50` — últimas llamadas (sin contenido).
- Métricas: `llm.gateway.calls{function,outcome}`, `llm.gateway.latency`, `llm.gateway.cost.usd`.
- Los datos viven en memoria: se pierden al reiniciar.

## Qué hacer si…
| Síntoma | Causa | Acción |
|---|---|---|
| `429 budget_exceeded` | La función agotó su presupuesto diario | Se resetea a las 00:00 UTC. Para subirlo hoy, cambiar el límite en `GatewayBudget` (hoy hardcodeado) y redesplegar |
| `503 provider_unavailable` | Breaker abierto: el proveedor falló ≥50 % en las últimas llamadas | Se reintenta solo a los 30 s. Si es urgente, reasignar la función a otro proveedor con `PUT model-assignments` |
| Logs `ALERTA proveedor X breaker ... OPEN` | Idem | Revisar estado del proveedor y `GROQ_API_KEY`/cuota del lado del proveedor |
| Muchos `TIMEOUT` en `/calls` | Proveedor lento | Subir `llm.tutor.invocation-timeout-ms` o cambiar de modelo |
| `INVALID_RESPONSE` | El modelo no cumple el formato | No se reintenta; revisar el prompt o cambiar de modelo |
