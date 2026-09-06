# Fase 3 — RAG, personalización y agentes

RAG permite que la asistencia use material docente y cite fuentes. No es un chat libre: cada búsqueda se limita a la cohorte, al material autorizado y a un umbral de similitud.

Antes de comenzar, leer [almacenamiento e ingesta](../docs/12-almacenamiento-e-ingesta.md) y [seguridad](../docs/05-seguridad.md).
Para el orden atómico de S14–S19, sus gates y pruebas, consultar el [backlog ejecutable](07-backlog-ejecutable-sprints.md) y el [playbook](06-playbook-de-construccion.md).

## Resultado de la fase

Al cerrar S19, docentes administran material versionado; alumnos reciben respuestas con fuente, solicitan desafíos personalizados compatibles con el motor existente e invocan un agente en un canal. Todo se recupera después de fallas.

## S14 — PDF de texto a respuesta citada

**Demo:** docente incorpora PDF; alumno consulta con página/fuente; consulta sin respaldo se abstiene.

1. Acordar referencia autorizada al objeto, hash y permisos con el dueño del material.
2. Ingerir con Tika/PDFBox, crear chunks con cohorte, unidad, tema, página, tipo y versión.
3. Generar embeddings locales con ONNX Runtime y guardar en PostgreSQL con pgvector.
4. Buscar top-k con filtro obligatorio por cohorte y umbral mínimo de similitud.
5. Conectar retrieval al proveedor de contexto del tutor y mostrar fuentes.
6. Probar aislamiento, página correcta, abstención y `recall@3`.

## S15 — Escaneos, tablas y figuras

**Demo:** docente incorpora material visual, revisa calidad de extracción y alumno consulta contenido citado.

1. Detectar texto, escaneo, tabla o figura por página.
2. Usar PDFBox/Tika para texto, Tess4J para OCR y el AI Gateway multimodal cuando corresponda.
3. Guardar referencias a imágenes, no binarios en PostgreSQL.
4. Generar reporte de calidad antes de activar el índice.
5. Probar páginas fallidas y ejemplos reales de tablas/diagramas.

## S16 — Versiones y retiro

**Demo:** docente reemplaza o retira material y búsqueda deja de usar la versión anterior.

1. Comparar hash y crear versión sin mezclar chunks.
2. Implementar estado de ingesta, reintentos y activación atómica.
3. Invalidar cachés al cambiar o retirar material.
4. Detener trabajos y retirar material al archivar curso según política.

## S17 — Desafío personalizado

**Demo:** alumno solicita, resuelve y entrega un desafío; el motor existente aplica efectos una sola vez.

1. Acordar con `challenges-service` formatos que puede corregir sin corrector LLM.
2. Generar en trabajo persistente con RAG, dificultad, cobertura y cuotas restrictivas.
3. Validar schema, costo, seguridad e idempotencia.
4. Entregar al motor por contrato y crear pantalla de solicitud/estado.

## S18 — Agente por mención

**Demo:** `@agente` responde con fuente; sin mención no actúa; interacción se conserva correctamente.

1. Acordar formato de mención, contexto permitido, publicación, usuario actor y prevención de bucles.
2. Implementar un agente inicial por curso que requiere mención válida.
3. Usar RAG filtrado, cuotas, moderación de entrada/salida y anti-fuga.
4. Conservar pares mención/respuesta como interacción IA, diferenciados de chat social purgable.

## S19 — Operación completa

**Demo:** ADMIN supervisa y recupera las tres fases tras reinicio, restauración y despliegue.

1. Consolidar estado de ingestas, índices, trabajos, calibraciones, incidentes y costos.
2. Probar backup/restauración, rollback, cache vacío y reproceso idempotente.
3. Ejecutar regresión F1–F3 y carga combinada.
4. Actualizar contratos, runbooks, evidencia de demo y responsables.

## Salida obligatoria

- [ ] Búsqueda aislada por cohorte, fuentes/páginas y abstención comprobadas.
- [ ] Ingesta textual/visual, reportes y versiones correctas.
- [ ] Desafíos personalizados sin efectos duplicados.
- [ ] Agente por mención moderado y con retención correcta.
- [ ] Recuperación y regresión completa demostradas.
