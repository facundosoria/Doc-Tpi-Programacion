# LLM Workbench

Frontend Angular temporal para desarrollar y probar golden sets en S1. No reemplaza el monolito Angular de la plataforma.

## Espacio docente: prototipo funcional

La ruta `/docente` (también la página inicial) permite gestionar lotes de exámenes,
presets completos de rúbricas y calibraciones. `/golden-sets` conserva la integración S1.

Para ejecutar el workbench contra el backend real en local:

```bash
cd ../llm-service
docker compose -f compose.yaml -f compose.workbench.yaml up --build
```

Abrir `http://localhost:4200/docente`. El backend, PostgreSQL y Flyway son reales. El navegador
usa sólo `/api/**` y el proxy de desarrollo lo envía al Gateway mock, nunca directamente a
`llm-service`.

El Gateway mock representa exclusivamente la frontera de plataforma: entrega la identidad
delegada de desarrollo, borra headers de identidad falsificados y propaga correlación. Cursos es
un MockServer externo; no contiene rutas ni respuestas de `llm-service`. Por lo tanto cada llamada
`/api/llm/**` ejercita los controllers, autorización, casos de uso, persistencia y migraciones
reales.

### Recorrido de prueba

1. En **Lotes de exámenes**, abrir el lote de demostración o crear uno. Cargar
   contenido en texto y cinco puntajes humanos de 0 a 100. Guardar un examen
   limpia el formulario para continuar; cada edición crea una versión del lote.
2. En **Presets de rúbricas**, abrir una versión o crear un preset con título,
   descripción, porcentajes y prompts por dimensión. Las cinco dimensiones y sus
   criterios son fijos. Los borradores incompletos se pueden guardar.
3. Usar **Ajustar automáticamente** cuando los pesos no sumen 100%. Seleccionar
   qué porcentajes mantener, revisar la propuesta y aplicarla antes de guardar.
4. En **Calibraciones**, seleccionar una versión completa de preset y un lote.
   Revisar las referencias y ejecutar. El lote de ejemplo y el escenario estándar
   permiten explorar el visto bueno; los otros escenarios muestran divergencia y fallos.
5. Consultar el MAE global y por dimensión, y las diferencias por examen. Dar el
   visto bueno y luego **Activar para el curso**. Cada curso conserva su selección.
6. Exportar un preset completo a JSON o copiarlo a otro curso desde su tarjeta.
   Importar siempre crea una copia independiente que requiere otra calibración.

### Reglas del prototipo

- **IA simulada:** el adaptador genera puntuaciones deterministas a partir del
  texto y los prompts, sin recibir puntajes humanos. No interpreta la exigencia,
  no entrena modelos y no debe usarse para emitir notas reales.
- El MAE global es el promedio de errores absolutos entre totales ponderados;
  el MAE por dimensión compara puntuaciones sin ponderar. Se aprueba técnicamente
  con MAE global ≤ 5 y todos los MAE de dimensión ≤ 10; el visto bueno es explícito.
  No se redondea antes de comparar umbrales.
- El ajuste proporcional usa centésimas enteras y mayores restos para completar
  exactamente 100%. No asigna pesos aleatorios ni reparte porcentajes entre ceros
  si no existe una proporción definida.
- Una calibración conserva copias del preset, lote, modelo y resultados. Las
  nuevas versiones no heredan aprobación. Archivar es borrado lógico; una versión
  activa debe desactivarse o sustituirse primero. El historial no se recalcula.
- Los datos se guardan en `localStorage`, clave `llm-workbench.teacher.v1`, en este
  navegador y origen. La exportación respalda la configuración de un preset completo,
  no los exámenes ni las aprobaciones. Importación JSON: máximo 1 MB, formato
  `teacher-rubric-preset`, versión 1, cinco dimensiones y total 100%.
- Los cambios pendientes se conservan entre secciones y se advierte antes de
  cambiar de curso o salir. Un fallo de guardado mantiene el formulario; una
  ejecución interrumpida al recargar se recupera como fallida. Los conflictos
  entre pestañas no sobrescriben silenciosamente los datos.

### Integración futura

`src/app/teacher/teacher.model.ts` contiene contratos y cálculos puros;
`teacher.store.ts` encapsula persistencia, versiones y activación;
`calibration-simulator.ts` es el adaptador reemplazable por ejecución real.
La persistencia y la validación autoritativa deberán trasladarse al backend,
con identidad real del docente y resultados de trabajos asincrónicos.

Esta función incorpora la decisión de producto de permitir porcentajes y prompts
docentes por curso. Amplía el contrato anterior de pesos fijos de plataforma,
sin modificar la API S1 existente. La regla MAE deriva del antecedente
`docs/importado/especificacion-tecnica/04_EVALUACION_ANALITICA_SCORING_HIBRIDO_Y_LLMOPS.md`;
el límite por dimensión está en `docs/epicas/ep-04.md`. Este prototipo adopta ese
límite como MAE por dimensión, separado del MAE del total ponderado.

Validación:

```bash
npm test -- --watch=false
npm run build
```

## Tutor + RAG (prueba)

La ruta `/docente/cursos/:courseId/tutor-rag` (también accesible desde el link "Tutor + RAG" en
el nav del espacio docente) permite probar el tutor con histórico multi-turno (EP-05) y el RAG de
consulta de material (EP-09) portados de `demoLLMSpringAi`. No reemplaza el frontend final de la
plataforma — es, igual que el resto de `llm-workbench`, un banco de pruebas.

### Recorrido de prueba

1. En la pestaña **Fuentes**, subí uno o más PDFs. Cada uno queda indexado (texto + diagramas
   detectados automáticamente) para el curso actual.
2. Marcá los checkboxes de las fuentes que querés que participen de la consulta.
3. Pasá a la pestaña **Chat** y hacé una pregunta sobre el contenido de esas fuentes. La respuesta
   cita documento y página; las citas se pueden expandir para ver el fragmento exacto usado.
4. En la pestaña **Diagramas**, elegí un documento y decodificá cualquier imagen detectada — la
   decodificación es 100% determinística (heurística de bounding boxes), no usa IA. Un diagrama
   decodificado se puede indexar como chunk semántico adicional para que el chat lo cite.

### Reglas del prototipo

- **Sin proveedor real de IA:** tanto la respuesta del tutor como los embeddings de búsqueda son
  simulados (`FakeModelAdapter`/`FakeEmbeddingAdapter`, ver
  [`docs/estado-implementacion/ep-09/`](../docs/06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-09/README.md)) — la
  calidad de la respuesta no refleja un proveedor real todavía.
- **`learnerId` de prueba:** no hay identidad real de alumno en el workbench; se genera un UUID
  estable por pestaña del navegador (`sessionStorage`, clave
  `llm-workbench.tutor-rag.learner-id`), igual de efímero que el resto del estado del prototipo.
- **`courseCohortId` = `courseId` de la ruta:** el contrato de RAG pide `courseCohortId` como
  partición obligatoria; el workbench no distingue curso de cohorte, así que reusa el mismo id de
  curso para ambos campos.
- El PDF subido se guarda como bytes en la base del backend (`rag_documents.pdf_bytes`), no en
  este frontend ni en `localStorage` — a diferencia de golden set/rúbricas, esta sección no
  funciona sin backend levantado.

## Contenedor de desarrollo

```bash
cd ../llm-service
docker compose -f compose.yaml -f compose.workbench.yaml up --build
```

Abrir `http://localhost:4200`. El código fuente se monta en el contenedor y Angular recarga los cambios. El navegador llama siempre a `/api/**`; `proxy.workbench.json` reenvía esas solicitudes al Gateway mock por la red de Docker.

## Integración futura

El monolito final conserva las mismas rutas relativas `/api/llm/**` y `/api/courses/**`. Cuando API Gateway exista, el host del frontend debe enrutar ambas hacia el Gateway. `proxy.gateway.example.json` muestra la configuración equivalente para desarrollo. No se modifican componentes ni servicios Angular y el navegador no fabrica headers de identidad.
