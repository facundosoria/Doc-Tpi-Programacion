# Backlog ejecutable S1–S19

Cada bloque es una receta de implementación. Las horas son el máximo de entregables por sprint y suman **208 h**; incluyen desarrollo, pruebas, revisión, documentación y demo, no las reuniones ni reserva. Ajustarlas en Planning según disponibilidad real. Leer antes el [playbook](06-playbook-de-construccion.md).

## S1 — Base operable y golden set (208 h)

**No iniciar sin:** responsables de `admin-service`, Gateway/Eureka, PostgreSQL local y definición docente inicial de las cinco dimensiones. **Demo:** docente autorizado crea un golden set, carga una entrada y la consulta tras reiniciar.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | ADR técnico y árbol de módulos | 16 | Java 21, Boot/Maven, límites de paquetes, variables de entorno y decisiones registradas. |
| 2 | Entorno reproducible | 30 | Compose con Postgres, Kafka, Eureka/Gateway según plataforma; `up`, health y `down` documentados. |
| 3 | Esqueleto transversal | 34 | Nombre Eureka, `/api/llm`, security M2M, Problem Details, correlación, Actuator y pipeline CI. |
| 4 | Migración y dominio | 38 | Flyway crea rúbrica, golden set, entrada, versión/auditoría e índice de idempotencia desde DB vacía. |
| 5 | API golden set v1 | 38 | Implementar únicamente operaciones existentes del OpenAPI; validar actor/ownership/idempotencia. |
| 6 | Interfaz docente mínima | 24 | Formulario/listado real vía Gateway, estados de carga/error y autorización visible. |
| 7 | Pruebas y demo | 28 | Unitarias, Testcontainers/Flyway, WireMock Gateway, reinicio de Compose y guía de demo. |

**Gates:** no copiar `/ai/*` del ejemplo histórico; congelar cualquier campo ausente con `admin-service` antes de publicarlo. **Aceptación negativa:** token sin rol, segunda clave igual, rúbrica inválida y reinicio no crean/duplican datos.

## S2 — Referencia humana versionada (208 h)

**No iniciar sin:** S1 integrado y protocolo docente de doble puntuación. **Demo:** dos docentes puntúan, resuelven diferencia y recuperan/exportan versión publicada.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Modelo append-only | 34 | Puntuación individual, resolución, publicación y snapshot con autor/fecha/rúbrica; prohibir update destructivo. |
| 2 | Casos de uso y reglas | 38 | Dos puntuaciones independientes; detectar diferencias por dimensión; solo rol autorizado resuelve. |
| 3 | API/contrato acordado | 30 | Completar adenda necesaria con admin; mocks y pruebas de compatibilidad. |
| 4 | Interfaz de doble ciego | 40 | Docente no ve puntuación ajena hasta enviar; comparador y motivo de resolución. |
| 5 | Exportación/recuperación | 24 | Exporta versión inmutable y restaura lectura, nunca pisa referencias publicadas. |
| 6 | Calidad y demo | 42 | Pruebas de concurrencia, acceso cruzado, auditoría, migración y recorrido real. |

**Aceptación negativa:** un docente no edita su nota publicada ni consulta una cohorte ajena; discrepancia sin resolución no habilita publicación.

## S3 — Calibración de plataforma (208 h)

**No iniciar sin:** S2 con golden set publicado, credenciales de proveedor administradas fuera del repo y tolerancias PAR-14 confirmadas. **Demo:** ADMIN inicia runner, consulta job/reporte y un modelo fuera de tolerancia no se habilita.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Puerto AI Gateway y fake | 32 | Interfaz proveedor, `RestClient`, fake/WireMock, timeout y schema estricto de salida. |
| 2 | Catálogo/asignación/auditoría | 28 | Modelo, versión, función, costo/latencia y habilitación separados de configuración de secretos. |
| 3 | Jobs durables | 42 | Migración, lease, reintentos, endpoint de estado y recuperación tras reinicio. |
| 4 | Evaluación y métrica PAR-14 | 42 | Transcripción como dato; promedio y dimensión; evidencia modelo/prompt/rúbrica. |
| 5 | API, permisos y reporte | 28 | `POST /calibrations`, `GET /jobs`; ADMIN autorizado; reporte explicable. |
| 6 | Pruebas y demo | 36 | Aprobado, rechazado, provider timeout, job duplicado/reiniciado y costo registrado. |

**Aceptación negativa:** no hay fallback automático de modelo; fallo no habilita; prompt no interpreta transcript como instrucción.

## S4 — Calibración por curso y bloqueo real (208 h)

**No iniciar sin:** S3 aprobado y compromiso de `courses-service` sobre consulta/bloqueo. **Demo:** curso no calibrado no activa; uno aprobado activa mediante integración real.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Extensión de set de cohorte | 34 | Referencias específicas sin cambiar pesos/dimensiones globales. |
| 2 | Runner por cohorte | 36 | Reutiliza motor S3; estado, evidencia y vencimiento por curso. |
| 3 | Consulta contractual | 32 | `GET /course-cohorts/{id}/calibration`, autorización y respuesta acordada con cursos. |
| 4 | Integración y avisos | 38 | Courses bloquea inclusive ADMIN; notificación/alerta de inicio sin calibrar. |
| 5 | Interfaz docente/admin | 24 | Iniciar, seguir estado, ver tolerancias y evidencia. |
| 6 | Pruebas y demo | 44 | Cohorte ajena, expiración, job duplicado, integración Gateway y bloqueo extremo a extremo. |

## S5 — Tutor seguro (208 h)

**No iniciar sin:** contexto validado y canal separado de solución desde `practice-service`, corpus mínimo de ataque y política de cuota. **Demo:** alumno recibe ayuda socrática; fuga/jailbreak bloquean; caída no impide continuar.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Adenda con práctica | 22 | Contexto, riesgo, ownership, idempotencia, evidencia de indisponibilidad y solución solo para guardia. |
| 2 | Dominio/interacción/cuota | 34 | Estados `completed/blocked/unavailable`, metadata, límite por alumno y auditoría. |
| 3 | Guardia de entrada | 28 | Filtros deterministas, separación de datos, clasificador de intención y respuesta segura. |
| 4 | Orquestación tutor | 36 | Prompt con contexto mínimo, AI Gateway, timeout/circuito y respuesta socrática. |
| 5 | Guardia de salida | 36 | AST/similitud contra solución fuera de prompt; bloquear/regenerar y registrar incidente. |
| 6 | UI práctica y degradación | 20 | Estados claros, no explica evasión, permite continuar sin tutor. |
| 7 | Seguridad/pruebas/demo | 32 | Corpus jailbreak, fuga simulada, cuota 429, ownership, timeout y trazabilidad. |

**Aceptación negativa:** solución nunca llega al prompt/log; alto riesgo no devuelve streaming plaintext; respuesta bloqueada no llega al navegador.

## S6 — Evaluación asíncrona y diferida (208 h)

**No iniciar sin:** S3, contrato `intento_cerrado.v1`, consumidor `challenges-service` y regla de cierre de cursos. **Demo:** cierre produce score; con proveedor caído la entrega se acepta y el score llega una sola vez después.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Consumidor/dedupe | 34 | Envelope, headers, `eventId` y `attemptId` únicos; rechazo seguro de versión inválida. |
| 2 | Evaluador y evidencia | 38 | Dimensiones, agregado, confianza, justificaciones y versión de rúbrica/modelo/prompt. |
| 3 | Outbox/resultados | 38 | `score_de_ia_calculado.v1` atómico, reintento sin doble evento. |
| 4 | Diferido/recuperación | 34 | `score_pendiente_diferido.v1`, job durable y reanudación. |
| 5 | Consulta pendientes + cursos | 24 | Conteo contractual y bloqueo de archivado por el dueño. |
| 6 | UI/observabilidad | 16 | Score/pendiente visible y estado operativo. |
| 7 | Pruebas/demo | 24 | Duplicado, caída, reinicio, outbox, no XP y recorrido integrado. |

## S7 — Apelación y override auditables (208 h)

**No iniciar sin:** S6 y decisión de negocio sobre propagación de revisión. **Demo:** alumno apela, docente ve evidencia y resuelve; original y cambio son recuperables.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Datos append-only | 32 | Apelación, evidencia congelada, override y auditoría con original/nuevo. |
| 2 | API y ownership | 32 | Endpoints v1, idempotencia, alumno solo propio; docente solo cohorte autorizada. |
| 3 | Bandeja docente | 38 | Filtros por baja confianza/impacto, transcripción, dimensiones, versiones y motivo. |
| 4 | Propagación a negocio | 32 | Evento/contrato acordado, outbox y efecto una vez en dueño. |
| 5 | Política/muestreo | 22 | Priorización documentada, sin recalcular pasado automáticamente. |
| 6 | Pruebas/demo | 52 | Apelación repetida, acceso indebido, conflicto, auditoría y actualización real. |

## S8 — Cambio de modelo y deriva (208 h)

**No iniciar sin:** S3/S4 y segundo adaptador o simulador compatible. **Demo:** ADMIN cambia modelo habilitado, conoce cohortes afectadas y recibe resultado de recalibración.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Política de asignación | 30 | Un evaluador activo, elegibilidad solo tras calibración y bitácora inmutable. |
| 2 | Adaptador portable | 34 | Segundo adaptador/fake que prueba aislamiento de proveedor. |
| 3 | Recalibración/deriva | 44 | Programada y por cambio; compara resultados, no reescribe históricos. |
| 4 | Cohortes afectadas/alertas | 32 | Reporte, señal a responsables y estado verificable. |
| 5 | API/UI admin | 24 | Cambio auditado, motivo, revisión de impacto. |
| 6 | Pruebas/demo | 44 | Modelo no habilitado, falla de recalibración, cambio concurrente y preservación histórica. |

## S9 — Consumo, cuotas y salud (208 h)

**No iniciar sin:** instrumentación de S3–S8. **Demo:** ADMIN consulta costos/fallas, cambia límite y sistema aplica 429/`Retry-After`; readiness sigue útil sin proveedor.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Métricas/ledger | 38 | Tokens, costo, latencia, errores, cuotas por función/modelo/período. |
| 2 | Configuración auditada | 32 | Límites versionados, permisos admin, caché/invalidez controlada. |
| 3 | Resiliencia uniforme | 38 | Timeouts, circuito, 429 y backoff para rutas/procesos pertinentes. |
| 4 | Consultas/UI operación | 36 | Panel con filtros, estados y explicaciones sin datos sensibles. |
| 5 | Health/alertas | 22 | Liveness/readiness sin llamada a proveedor; umbrales acordados. |
| 6 | Pruebas/demo | 42 | Cambio inmediato, saturación, 429, circuito abierto y health aislado. |

## S10 — Hardening y release F1 (208 h)

**No iniciar sin:** F1 integrada y ventana de pruebas con consumidores. **Demo:** operador recupera trabajos sin duplicar bajo carga; backup/restauración y rollback pasan.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Consola de jobs/runbook | 32 | Ver, reintentar y explicar fallidos/leases; permisos y auditoría. |
| 2 | Escenarios de recuperación | 36 | Reinicio en worker/outbox, reentrega Kafka, proveedor caído/vuelto. |
| 3 | Carga y seguridad F1 | 36 | 120 sesiones, corpus regresión, cuotas y resultados medidos. |
| 4 | Backup/restore/rollback | 36 | Procedimiento ensayado con Flyway/versiones y evidencia. |
| 5 | E2E consumidores | 36 | Golden→calibración→tutor→cierre→diferido→apelación. |
| 6 | Release y demo | 32 | Checklist F1, defectos priorizados, runbooks y decisión de salida. |

## S11 — Moderación previa a entrega (208 h)

**No iniciar sin:** contrato/política con `chat-service` y notificaciones; no usar contrato MVP para inventar F2. **Demo:** chat real permite/bloquea antes de publicar y avisa incidentes graves.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Contrato F2 y corpus | 30 | Mensaje, contexto mínimo, categorías, severidad, retención, idempotencia y feedback. |
| 2 | Detectores clásicos | 34 | Spam, ofensivo, Base64 y código en desafío; reglas versionadas. |
| 3 | Clasificador contextual | 32 | Solo escalamiento de indeterminados, timeout y confianza. |
| 4 | Decisión/incidente/evento | 36 | Bloqueo previo, auditoría, outbox y notificación grave. |
| 5 | Integración/UI docente | 30 | Chat recibe decisión; bandeja mínima de incidentes. |
| 6 | Pruebas/demo | 46 | Permitido, bloqueos, duplicado, caída de clasificador y no filtración de reglas. |

## S12 — Revisión de moderación (208 h)

**No iniciar sin:** S11, autorización de contexto inmediato y corpus etiquetado inicial. **Demo:** usuario apela y docente resuelve con evidencia permitida; chat refleja la resolución.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Apelación/evidencia | 34 | Datos auditables, plazo/estado y ownership. |
| 2 | Contexto mínimo | 28 | Solicitar/retener solo ventana aprobada, nunca historial completo implícito. |
| 3 | Bandeja/resolución | 38 | Categoría, confianza, evidencia, motivo y sincronización con chat. |
| 4 | Calidad de corpus | 30 | Etiquetado, falsos positivos/negativos y criterio de revisión docente. |
| 5 | Métricas/política | 20 | Tasa de error, tiempos, reiteración sin perfilado excesivo. |
| 6 | Pruebas/demo | 58 | Apelación doble, permisos, resolución, dato excesivo y casos de corpus. |

## S13 — Degradación y retención de moderación (208 h)

**No iniciar sin:** política de caída aprobada por producto/seguridad y acuerdo de archivado del chat. **Demo:** caída aplica política aprobada y al archivar conserva sólo evidencia requerida.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Política ejecutable | 24 | Tabla por detector/clasificador/servicio total y decisión fail-open/fail-closed aprobada. |
| 2 | Degradación/remoderación | 38 | Capa clásica, marca de revisión, trabajo idempotente de recuperación. |
| 3 | Retención/purga | 36 | Eventos de archivado, evidencia media/alta y purga selectiva verificable. |
| 4 | Operación/alertas | 28 | Cola de revisión, métricas y runbook. |
| 5 | Regresión F2 | 38 | Corpus, recuperación y carga acordada. |
| 6 | Demo/cierre F2 | 44 | Chat integrado, revisión, archivo y evidencia de política. |

## S14 — RAG textual aislado (208 h)

**No iniciar sin:** objeto/referencia autorizada, pgvector disponible y métricas de calidad acordadas. **Demo:** PDF textual genera respuesta citada; sin fuente suficiente el tutor se abstiene.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Contrato de ingesta/permiso | 28 | URI/referencia, hash, cohorte, unidad, versión y ownership. |
| 2 | Esquema documental/vector | 34 | Documento, versión, chunk, página, fuente, estado e índices pgvector. |
| 3 | Extracción/chunking | 32 | PDFBox/Tika, límites, hash, metadatos y fallas por página. |
| 4 | Embeddings/retrieval | 38 | ONNX local, top-k, umbral y filtro obligatorio de cohorte/material. |
| 5 | Contexto tutor/citas | 28 | Puerto de contexto S5, fuentes/página y abstención sin inventar respaldo. |
| 6 | UI/operación | 16 | Estado de ingesta y fuentes visibles. |
| 7 | Pruebas/demo | 32 | Aislamiento entre cohortes, fuente/página, umbral, `recall@3` y reinicio. |

## S15 — Ingesta visual con control de calidad (208 h)

**No iniciar sin:** S14 y corpus autorizado de escaneos, tablas y figuras. **Demo:** docente activa índice visual tras revisar reporte; respuesta cita fuente correcta.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Clasificación por página | 28 | Texto/escaneo/tabla/figura y ruta de extracción trazable. |
| 2 | OCR y extracción visual | 42 | Tess4J, PDFBox/Tika y AI Gateway multimodal solo cuando aplique. |
| 3 | Referencias/objetos | 22 | Sin binarios en Postgres; hashes y referencias autorizadas. |
| 4 | Reporte y activación | 34 | Cobertura, errores, umbral de calidad y activación atómica docente. |
| 5 | Retrieval integrado | 30 | Chunks visuales citables sin degradar aislamiento S14. |
| 6 | Pruebas/demo | 52 | Tabla, diagrama, OCR fallido, página parcial, reintento e índice no activado. |

## S16 — Versionado y retiro documental (208 h)

**No iniciar sin:** S14/S15 y política de archivo de cursos. **Demo:** nueva versión sustituye resultados; retiro elimina la versión de búsqueda sin dañar historial.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Detección/hash/versiones | 34 | Igual hash no duplica; nuevo hash crea versión aislada. |
| 2 | Máquina de estados | 34 | queued/running/failed/ready/active/retired, transiciones autorizadas. |
| 3 | Activación atómica/cache | 34 | Swap de versión e invalidación; consultas no mezclan chunks. |
| 4 | Retiro/curso archivado | 32 | Cancela jobs, retira índice conforme política y conserva auditoría. |
| 5 | UI/admin | 22 | Versiones, estado, error/reintento y retiro explícito. |
| 6 | Pruebas/demo | 52 | Carrera de ingestas, rollback, cache vacío, archivado y aislamiento. |

## S17 — Desafío personalizado seguro (208 h)

**No iniciar sin:** formatos corregibles acordados por `challenges-service`, S14 y política de cuota/costo. **Demo:** alumno solicita/resuelve/entrega un desafío y motor aplica efectos una vez.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Contrato y catálogo formatos | 30 | Entrada/salida, dificultad, cobertura, ownership, Idempotency-Key y entrega. |
| 2 | Job de generación | 38 | Trabajo durable, RAG filtrado, cuota y AI Gateway. |
| 3 | Validadores de resultado | 34 | Schema, formato corregible, seguridad y rechazo seguro. |
| 4 | Entrega al motor/outbox | 32 | Efecto idempotente, reintento sin duplicar desafío/recompensa. |
| 5 | UI solicitud/estado | 22 | Solicitar, esperar, error y desafío recibido. |
| 6 | Pruebas/demo | 52 | Duplicado, formato inválido, cuota, caída, fuente ajena y entrega real. |

## S18 — Agente por mención (208 h)

**No iniciar sin:** contrato con chat, S11–S13, S14 y reglas contra bucles. **Demo:** sólo `@agente` válido responde citado y moderado; la interacción IA se retiene correctamente.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Contrato/mención/actor | 30 | Sintaxis, curso, usuario actor, idempotencia, publicación y no responder bots. |
| 2 | Detección y anti-bucle | 26 | Mención válida únicamente; marcador de origen/TTL para impedir recursión. |
| 3 | Orquestación agente | 38 | RAG filtrado, cuotas, contexto mínimo, citas y abstención. |
| 4 | Guardias/moderación | 34 | Entrada/salida F1 + moderación F2 antes de publicar. |
| 5 | Retención diferenciada | 28 | Par mención/respuesta IA separado de chat social purgable. |
| 6 | Pruebas/UI/demo | 52 | Sin mención, mención ajena, bucle, jailbreak, fuente ajena, caída y archivo. |

## S19 — Operación integral y salida de producto (208 h)

**No iniciar sin:** F1–F3 integradas, responsables operativos y datos de prueba no productivos. **Demo:** ADMIN observa/reanuda el producto completo tras restore/rollback y regresión combinada.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Vista operativa unificada | 34 | Jobs, ingestas, índices, calibraciones, incidentes y costos con permisos. |
| 2 | Recuperación integral | 38 | Backup/restore, rollback, cache vacía, reproceso y dedupe de las tres fases. |
| 3 | Carga/regresión combinada | 42 | Tutor/evaluador/chat/RAG/agente bajo perfil acordado y resultados medidos. |
| 4 | Seguridad/retención final | 30 | Corpus actualizado, permisos, purga/retención y secretos revisados. |
| 5 | Contratos/runbooks | 26 | OpenAPI/AsyncAPI/adendas F2/F3, operación y propietarios actualizados. |
| 6 | Release review/demo | 38 | Checklist de salida, evidencia, riesgos aceptados y backlog posterior. |

## Cierre de cada sprint

No trasladar automáticamente una fila incompleta: dividirla, medir horas restantes y decidir en Review/Planning siguiente. Cada sprint conserva la regresión de los anteriores. Las F2/F3 no se adelantan por disponibilidad técnica: se habilitan solo al cumplir los gates del sprint y las condiciones de salida de su fase.
