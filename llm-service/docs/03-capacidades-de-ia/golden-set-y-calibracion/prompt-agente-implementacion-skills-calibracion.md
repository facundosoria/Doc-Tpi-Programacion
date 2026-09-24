# Prompt para agente implementador — Skills y calibración

Copiá el siguiente prompt completo al agente que vaya a trabajar sobre
`llm-service`.

---

Sos el agente implementador senior de `llm-service` (Tema 07). Tu tarea es
evolucionar la aplicación **sobre la implementación de calibración que ya
existe**; no la reemplaces por un proyecto paralelo ni supongas que la
documentación prueba que una función está implementada.

## Objetivo

Implementá incrementalmente las decisiones confirmadas de skills, calibración
global por curso y subcalibración por desafío. Conservá compatibilidad con los
datos, endpoints, eventos y resultados históricos existentes. El sistema evalúa
el uso pedagógico de IA: nunca decide corrección académica, nota, XP ni monedas.

## Lectura obligatoria, en este orden

1. `llm-service/AGENTS.md`: reglas de arquitectura, seguridad, persistencia,
   contratos y calidad.
2. `llm-service/docs/03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md`:
   fuente de decisiones de Producto. Es un registro incremental; distinguí
   decisiones confirmadas de propuestas.
3. `llm-service/docs/03-capacidades-de-ia/golden-set-y-calibracion/`:
   especificación funcional y modelo vigente (docs 01 a 09 de esa carpeta).
4. `llm-service/docs/contracts/README.md`,
   `00-mapa-de-integracion.md`, `01-inventario-y-brechas.md` y
   `requisitos-a-otros-micros.md`.
5. `llm-service/docs/contracts/llm-service.openapi.yaml` y
   `llm-service/docs/contracts/llm-service.asyncapi.yaml`: son los schemas
   ejecutables actuales, no los sustituyas por texto narrativo.
6. `llm-service/docs/contracts/02-contratos-skills-y-calibracion.md`:
   consolidación de contratos existentes y propuestas de endpoints/eventos.
7. Código, migraciones Flyway y pruebas existentes antes de modificar cualquier
   archivo. Identificá qué partes están implementadas y qué partes son sólo
   documentación.

## Regla de alcance

El plan contiene decisiones de varios horizontes. Para el sprint actual,
**ignorá por completo** los puntos siguientes, salvo que el usuario los
reactive explícitamente:

- punto 2: libro de cuota, reserva, vencimiento y conciliación;
- punto 4: pipeline y estados de sanitización de Markdown;
- punto 9: observabilidad, auditoría, métricas y matriz de pruebas.

No crees tablas, workers, endpoints, eventos, alarmas, métricas ni criterios de
aceptación para esos tres puntos. Si una funcionalidad depende estrictamente de
ellos —por ejemplo, publicación de una skill que requiere sanitización— detenete
e informá la dependencia; no inventes un bypass inseguro.

## Estado de las decisiones del plan

- D-161 está **confirmada**: implementa tres agregados separados para corrida,
  activación y verificación. Una corrida `PASSED` no se vuelve `ACTIVE`; la
  activación es un puntero versionado por alcance. No existe cancelación manual
  de corridas. Usá transiciones condicionales, revisión y lease de worker.
- D-162, D-163, D-164, D-165 y D-166 son **propuestas pendientes de
  confirmación**. No las implementes, no generes Flyway ni cambies OpenAPI o
  AsyncAPI hasta que el usuario confirme cada decisión. Podés analizarlas y
  preparar un delta técnico sin modificar estado.
- Los endpoints de Skills, borradores, estado, resumen de diferidos y
  verificaciones en `02-contratos-skills-y-calibracion.md` son **PROPUESTOS**;
  no son autorización para publicarlos.

## Forma de trabajo obligatoria

1. Empezá con un relevamiento breve y basado en archivos: agregados existentes,
   tablas/migraciones, endpoints, consumidores/productores Kafka, workers y
   pruebas afectadas. Informá el delta contra D-161 antes de editar.
2. Implementá sólo una decisión confirmada y su vertical slice por vez. No
   mezcles refactors ajenos ni amplíes el alcance.
3. Si falta una columna, tabla, entidad o migration para la decisión, explicá
   exactamente qué hace falta y pedí autorización explícita antes de modificar
   Flyway o el esquema.
4. Si hace falta cambiar OpenAPI, AsyncAPI, un evento publicado/consumido o un
   contrato de otro microservicio, detenete y pedí aprobación. Los campos deben
   congelarse con el consumidor antes de escribir el schema.
5. Para integraciones HTTP, usá sólo API Gateway bajo `/api/llm/**`, JWT M2M
   `aud=llm-service`, scopes mínimos, `traceparent`, `X-Request-Id`,
   `Idempotency-Key` y `ProblemDetail` RFC 7807.
6. Para Kafka, usá topic `<evento>.v<major>`, envelope estándar, correlación en
   headers, outbox, entrega at-least-once y deduplicación por `eventId`. Nunca
   trates Kafka como llamada síncrona.
7. Ningún controller, worker o frontend llama al proveedor LLM directamente:
   todo pasa por el AI Gateway interno. No expongas secretos, prompts de
   sistema, soluciones esperadas ni contenido sensible en logs o respuestas.
8. No accedas ni escribas bases de otros microservicios. `challenges-service`
   no se consulta directamente desde LLM.

## Orden técnico cuando el usuario autorice las decisiones

1. D-161: consolidar máquina de estados, activación por alcance, verificación
   separada, locks/revisions y leases sin alterar resultados históricos.
2. D-162: cola durable e idempotente de score diferido, sin doble score y sin
   mecanismo de omisión/cierre forzado.
3. D-163: planificador de verificaciones, una ejecución abierta por ventana y
   degradación sin fallback del evaluador.
4. D-166: contrato interno de salida JSON estricta del modelo y validación
   antes de calcular score o PAR-14.
5. D-164: sólo tras congelar consumidores, evolucionar OpenAPI y luego
   controllers/DTOs/mocks.
6. D-165: sólo tras acordar consumidores, evolucionar AsyncAPI, outbox, relay
   y deduplicación de consumidores.
7. Endpoints de Skills: implementar únicamente la parte que no dependa del
   punto 4 y sólo después de confirmar el contrato correspondiente.

## Reglas de dominio que no podés violar

- Una sola activación vigente por curso y una por desafío; activaciones previas
  se preservan como historia.
- El primer intento bloquea la asociación desafío-calibración. Una migración o
  recalibración posterior nunca modifica ese intento.
- Recalibrar crea una nueva corrida/snapshot; nunca reescribe una corrida,
  rúbrica, Golden Set, skill o resultado histórico.
- Sólo una corrida `PASSED` puede activar una configuración.
- Si una verificación falla por tolerancia, se requiere nueva corrida aprobada
  y activación explícita; no hay fallback automático de modelo.
- Un score diferido conserva su identidad y no puede publicarse dos veces.
- Las skills son contenido declarativo no confiable; no ejecutan código, red,
  herramientas ni alteran las instrucciones de sistema.

## Cierre de cada cambio autorizado

Ejecutá las verificaciones pertinentes antes de afirmar que terminaste:

```bash
./mvnw test -Dtest=<PruebaAfectada>
./mvnw verify
```

Si hay frontend afectado, ejecutá además lint, pruebas y build de producción
según `AGENTS.md`. Informá archivos cambiados, evidencia de pruebas, decisiones
aplicadas y bloqueos. Nunca borres pruebas para obtener verde.

---

## Uso de este prompt

El prompt intencionalmente prioriza el relevamiento y la autorización por encima
de modificar contratos o persistencia. Si el usuario confirma una de las
propuestas D-162 a D-166, actualizá primero su estado en
`plan-skills-calibracion.md` (mismo directorio) y recién después ejecutá su implementación siguiendo
las reglas anteriores.
