# Fase 1 — MVP académico

Esta fase crea el primer producto que puede afectar resultados académicos. Un modelo que responde no alcanza: sus resultados deben ser calibrados, explicables, auditables y recuperables.

Antes de tomar una historia, leer [README](README.md), [contratos](../docs/contracts/) y [matriz de trazabilidad](../docs/21-matriz-trazabilidad-llm.md).
La secuencia atómica, gates, pruebas y presupuesto de cada sprint está en el [backlog ejecutable](07-backlog-ejecutable-sprints.md); las reglas que todos los cambios deben cumplir están en el [playbook](06-playbook-de-construccion.md).

## Resultado de la fase

Al cerrar S10, el alumno usa tutoría segura, cierra un intento y obtiene una evaluación desglosada. Puede apelar. El docente calibra y no puede activar un curso sin aprobación. Una caída de IA no bloquea entregas ni pierde resultados.

## S1 — Golden set y plataforma mínima

**Demo:** un docente autorizado carga y consulta un golden set después de reiniciar.

1. Crear `llm-service` definitivo con Java 21, Spring Boot, Maven, `/api/llm/**`, PostgreSQL, Flyway y Docker Compose de desarrollo.
2. Registrar Eureka, recibir tráfico por Gateway y agregar CI básico.
3. Versionar la rúbrica declarativa con cinco dimensiones y pesos; no dejarla solo en un prompt.
4. Implementar alta, carga y lectura de golden set con pantalla administrativa.
5. Publicar contrato/mocks mínimos para `admin-service`.
6. Demostrar acceso autorizado, persistencia y recuperación tras reinicio.

## S2 — Puntuación docente y versión

**Demo:** dos docentes puntúan por separado, resuelven una diferencia y publican una versión recuperable.

1. Guardar puntuaciones separadas, autor, fecha, rúbrica y versión.
2. Implementar comparación de discrepancias y resolución documentada.
3. Garantizar append-only: una puntuación histórica nunca se pisa.
4. Exportar y recuperar la versión publicada.

## S3 — Calibración de plataforma

**Demo:** ADMIN ejecuta calibración y solo habilita un modelo si pasa PAR-14.

1. Implementar AI Gateway interno con adaptador, timeout, auditoría de costo/latencia y schema de salida.
2. Implementar evaluador que trate la transcripción como datos, nunca como instrucciones.
3. Crear trabajos persistentes e idempotentes.
4. Ejecutar runner a ciegas contra golden set, calcular desvío promedio y por dimensión.
5. Persistir modelo, versión, prompt y rúbrica. Rechazar candidatos fuera de tolerancia.

PAR-14 exige desvío promedio dentro de ±5 y ninguna dimensión fuera de ±10.

## S4 — Calibración de curso

**Demo:** el docente calibra una cohorte y `courses-service` bloquea activación si no pasa.

1. Permitir partir del set base y sumar transcripciones representativas del curso.
2. Ejecutar calibración por curso sin cambiar dimensiones ni pesos globales.
3. Exponer la consulta contractual e integrarla por Gateway con `courses-service`.
4. Alertar curso próximo a iniciar sin calibración.
5. Probar que ni ADMIN activa una cohorte no calibrada.

## S5 — Tutor seguro

**Demo:** el alumno recibe ayuda según riesgo; una respuesta similar a la solución se bloquea antes de mostrarse.

1. Integrar `POST /api/llm/tutor/interactions` con `practice-service` por Gateway y M2M.
2. Recibir contexto validado, riesgo y metadata. La solución esperada llega solo al guardarraíl, nunca al prompt.
3. Aplicar cuotas, timeout, filtro de entrada, reglas de riesgo, validación de salida y anti-fuga.
4. Persistir interacción y metadata para el evaluador.
5. Mostrar `completed`, `blocked` o `unavailable` sin explicar cómo evadir la protección.
6. Ejecutar corpus de jailbreak y un caso de fuga simulada.

Si el tutor cae, el alumno continúa y se registra la indisponibilidad para el tratamiento académico neutro.

## S6 — Evaluación automática y diferida

**Demo:** cerrar un intento muestra score; si el evaluador cae, la entrega se acepta y el score llega luego una sola vez.

1. Consumir `intento_cerrado.v1` desde Kafka y deduplicar por identificador estable.
2. Calcular features, score, dimensiones, confianza, justificaciones y versiones.
3. Publicar resultado con outbox en `score_de_ia_calculado.v1`.
4. Ante falla, publicar `score_pendiente_diferido.v1`, conservar trabajo y recuperarlo.
5. Exponer pendientes por cohorte; `courses-service` bloquea el archivado cuando corresponde.

`llm-service` nunca aplica XP ni bloquea la entrega.

## S7 — Apelación y revisión

**Demo:** alumno apela y docente resuelve con toda la evidencia; el original se conserva.

1. Implementar apelación idempotente y autorización.
2. Mostrar transcripción, dimensiones, justificaciones, confianza y versiones.
3. Crear override append-only con motivo, autor y resultado previo/nuevo.
4. Propagar resolución a negocio mediante contrato acordado.

## S8 — Cambio de modelo y deriva

**Demo:** ADMIN cambia un modelo habilitado, identifica cohortes afectadas y recibe alerta si recalibración falla.

1. Implementar asignación de modelo por función y auditoría.
2. Recalibrar mensualmente y ante cambio de versión.
3. Publicar eventos de calibración y marcar cohortes con más de una versión.
4. No recalcular resultados históricos.

## S9 — Consumo, cuotas y salud

**Demo:** ADMIN consulta costo, cuota y fallas, cambia un límite y el cambio se respeta.

1. Exponer consultas por función/modelo/período y auditar cambios administrativos.
2. Probar `429`, `Retry-After`, timeout y circuit breaker con proveedor simulado.
3. Verificar que readiness no consulta al proveedor.

## S10 — Operación del MVP

**Demo:** operador recupera trabajos sin duplicar resultados; el recorrido pasa carga y restauración.

1. Listar trabajos antiguos, fallidos y reintentables.
2. Probar reinicio durante procesamiento, outbox y deduplicación.
3. Ejecutar 120 sesiones concurrentes y caída de tutor/evaluador.
4. Probar backup/restauración de Postgres y despliegue/rollback.
5. Ejecutar regresión de toda F1 y actualizar runbook.

## Salida obligatoria

- [ ] Golden sets humanos base y de cada cohorte de salida.
- [ ] Modelo habilitado y calibraciones de curso aprobadas.
- [ ] Activación bloqueada sin calibración, incluso para ADMIN.
- [ ] Tutor, score, apelación y revisión desde interfaces reales.
- [ ] Entregas aceptadas ante caída y cierre bloqueado con pendientes.
- [ ] Auditoría, correlación, idempotencia, carga, backup y rollback comprobados.

Un set sintético permite desarrollo, pero nunca reemplaza el golden set docente requerido para producción.
