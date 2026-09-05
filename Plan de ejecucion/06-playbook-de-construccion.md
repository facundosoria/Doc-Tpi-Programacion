# Playbook de construcción para agentes

Este documento transforma las fases en reglas de ejecución. Se lee antes de tomar una tarea de `07-backlog-ejecutable-sprints.md`. No reemplaza los contratos ni autoriza a inventar campos, rutas o eventos.

## 1. Regla de lectura y autoridad

1. Leer el requisito y el sprint asignado.
2. Leer el contrato OpenAPI o AsyncAPI mencionado por la tarea.
3. Leer la fuente temática enlazada en la tarea (seguridad, datos, operación o RAG).
4. Inspeccionar el repositorio y proponer el cambio mínimo compatible.
5. Si falta un dato que debe venir de otro servicio, abrir una dependencia; no fabricar un valor ni ampliar el contrato unilateralmente.

Orden de precedencia: PRD e `idea.pptx.pdf`; `docs/00-fuentes-de-verdad-y-convenciones.md`; contratos v1; decisiones aprobadas; este plan. Los documentos de `docs/importado/` son antecedentes y pueden contradecir las fuentes vigentes.

## 2. Arquitectura objetivo y fronteras

```text
Consumidor -> API Gateway -> /api/llm/** -> controller -> aplicación/dominio
                                                   |         |
                                              PostgreSQL    AI Gateway interno -> proveedor
                                                   |
Kafka <-> consumer/outbox publisher <-------------+
```

- El Gateway es la única entrada HTTP. No publicar puertos internos como integración de producto.
- `llm-service` posee su PostgreSQL y sus migraciones Flyway. Ningún servicio lee sus tablas.
- Kafka transporta hechos de integración; la tabla de trabajos durables coordina trabajos internos y no sustituye Kafka.
- El AI Gateway interno concentra cliente HTTP, modelos, timeout, presupuesto, auditoría y validación. Controllers, workers y Angular no llaman a un proveedor.
- Los datos que vienen de usuario, chat, documentos o transcripciones son **datos no confiables**, nunca instrucciones de sistema.

La estructura de paquetes se decide en S1, pero debe conservar estas fronteras: `api`, `application`, `domain`, `infrastructure/persistence`, `infrastructure/messaging`, `infrastructure/ai`, `security` y `configuration`. Los nombres físicos pueden variar; las dependencias no: `api` no conoce JPA, y `domain` no conoce Spring, Kafka ni proveedores.

## 3. Definition of Ready (DoR)

Una historia puede entrar al sprint solo si tiene:

- Usuario, resultado observable y criterios de aceptación negativos además de los felices.
- Requisito(s) trazables (`RF-*` o `PAR-*`) y dueño de producto.
- Consumidor/productor, esquema, autenticación, correlación e idempotencia definidos si cruza servicios.
- Datos de prueba autorizados o un fixture sintético claramente etiquetado.
- Dueño externo, fecha de disponibilidad y alternativa explícita si existe una dependencia.
- Estimación dentro de la capacidad del sprint; si supera unas 40 h, se divide verticalmente.

Un mock puede destrabar desarrollo interno, pero no cierra un entregable cuya demo exige integración real.

## 4. Definition of Done (DoD) para cada historia

- Implementación revisada mediante PR, sin secretos, con migración versionada si cambia persistencia.
- Autorización por audiencia, scope/rol y ownership; validar el recurso referenciado, no confiar en IDs del body.
- Validación Bean Validation y respuesta RFC 7807 con `X-Request-Id` propagado.
- Idempotencia: la misma `Idempotency-Key` o `eventId` no duplica efectos; el reintento devuelve/recupera el resultado correspondiente.
- Trazas `traceparent` y `X-Request-Id` en HTTP, jobs y eventos. Logs estructurados sin prompt, solución, token ni dato sensible innecesario.
- Pruebas unitarias de reglas; integración de persistencia/migración; contrato con WireMock o consumer pactado; prueba de autorización y de fallo relevante.
- Métrica, health/readiness razonable y runbook si introduce trabajo asíncrono, proveedor, dato retenido u operación manual.
- Demo reproducible desde Docker y evidencia enlazada en la ficha del sprint.

No se cierra una tarea por compilar. El incremento se cierra únicamente si el recorrido de demo del sprint pasa en un ambiente integrado.

## 5. Secuencia obligatoria para una capacidad nueva

1. **Contrato y amenaza.** Identificar entrada, actor, ownership, datos retenidos, clasificación de riesgo, productor/consumidor y fallas.
2. **Dominio y migración.** Definir estados, invariantes, historial append-only y claves únicas de idempotencia antes del controller/worker.
3. **Caso de uso.** Implementar la operación en aplicación; mantener el dominio testeable sin red.
4. **Adaptadores.** Conectar HTTP, Kafka, base, proveedor o MinIO detrás de puertos/interfaces.
5. **Seguridad y resiliencia.** Añadir autorización, rate limit, timeout, circuito, reintento y degradación indicados por el caso de uso.
6. **Observabilidad.** Añadir métricas, correlación, auditoría y consulta/estado que necesita el operador.
7. **Prueba de extremo a extremo.** Ejecutar caso feliz, duplicado, no autorizado, dependencia caída y recuperación.
8. **Demo y evidencia.** Actualizar contrato/runbook/changelog de sprint y mostrar la capacidad desde la interfaz o consumidor real.

## 6. Patrones que no se negocian

### HTTP y errores

Rutas privadas bajo `/api/llm/**`; token M2M con `aud=llm-service`; `Idempotency-Key` cuando el contrato lo requiere. Propagar `traceparent` y `X-Request-Id`. Los errores son Problem Details, sin revelar reglas de seguridad, prompts ni soluciones. `429` incluye `Retry-After`; una indisponibilidad del proveedor se expresa sin atribuir culpa al alumno.

### Eventos, outbox y jobs

Al consumir Kafka: guardar `eventId` procesado y tomar decisión idempotente en la misma transacción de negocio. Al publicar: guardar evento en outbox dentro de la transacción y marcarlo publicado solo tras confirmación. El worker toma trabajos con bloqueo concurrente, tiene estados `queued/running/completed/failed`, límite de intentos, backoff, dueño/lease y recuperación de leases vencidos. Nunca publicar un resultado dos veces por reintento.

### IA y seguridad

La solución esperada se entrega exclusivamente al guardia de salida, jamás al prompt. Aplicar filtros deterministas, separación estructural, clasificador de intención, contexto mínimo y guardia anti-fuga desde F1. En F3 añadir aislamiento de retrieval por cohorte/material autorizado, sanitización y citación. Registrar incidente y resultado de guardias sin almacenar más texto del necesario. Ver [seguridad](../docs/05-seguridad.md).

### Datos y retención

Toda entidad académica conserva versión, autor, instante y relación con rúbrica/modelo/prompt cuando corresponda. Overrides y puntuaciones no sobrescriben el original. Documentos binarios viven en el dueño/MinIO; LLM conserva referencias, hashes y metadatos. La purga del chat no elimina evidencia académica o de incidente que tenga retención aprobada.

## 7. Pruebas mínimas por tipo

| Tipo | Debe demostrar |
|---|---|
| Unidad | Cálculo de rúbrica, transición de estado, filtros y políticas sin Spring/red. |
| Integración | Flyway desde base vacía, repositorio, transacción outbox/dedupe y worker concurrente con PostgreSQL real. |
| Contrato | Request/response y headers HTTP; envelope/version/eventos Kafka; consumidor y productor compatibles. |
| Seguridad | Sin token, audiencia/scope incorrecto, ownership ajeno, payload inválido, jailbreak y fuga. |
| Resiliencia | Timeout, 429, proveedor caído, mensaje duplicado, reinicio en ejecución y recuperación. |
| E2E/demo | Usuario real completa el recorrido y el operador puede observar el estado. |

## 8. Gestión de bloqueos

Registrar el bloqueo el mismo día con: sprint, historia, dueño externo, contrato/requisito afectado, evidencia, fecha prometida y decisión necesaria. P1 lo lleva a coordinación semanal. Si no se resuelve antes de la mitad del sprint, se conserva el objetivo con una rebanada vertical que no falsee la integración; si no existe, se renegocia el objetivo en vez de declarar terminado un mock.

## 9. Evidencia por sprint

En la carpeta de evidencia acordada por el equipo, registrar: enlace al PR, resultado de CI, versión de migración, contrato/modificación aprobada, comandos Docker ejecutados, capturas o video de demo, resultados de carga/seguridad si aplican, y decisiones/bloqueos abiertos. La Review valida evidencia, no solo una presentación.
