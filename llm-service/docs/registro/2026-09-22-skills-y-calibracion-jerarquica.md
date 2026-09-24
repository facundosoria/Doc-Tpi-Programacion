# Skills adjuntables y calibración jerárquica por desafío — 2026-09-22

## Decisión anterior

`docsV2/03-capacidades-de-ia/golden-set-y-calibracion/02-especificacion-funcional.md` §3 y
`03-modelo-de-dominio-y-transiciones.md` (entidad `RubricDimension`) fijaban una rúbrica de **cinco
dimensiones fijas**, sin subcriterios ponderables: cada dimensión tiene un único campo de criterio
(texto) y un único peso. §11 de la especificación funcional describía la asociación de un desafío a
una calibración como un vínculo simple (`ChallengeCalibrationAssignment`) copiado de la calibración
activa del curso, bloqueado con el primer intento. No existía ningún documento de skills adjuntables
en `docsV2`.

## Motivo

`llm-service/docs/03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md` (166 decisiones de Producto) introduce: (1) skills
adjuntables en Markdown con catálogo, visibilidad, versionado, favoritos y clonado; (2)
subcalibración por desafío, híbrida y jerárquica, sobre un modelo de dos capas
(`CourseBaseline` + `ChallengeOverlay` + `EffectiveEvaluationProfile`), con herencia editable,
cascada de recalibración, rebase de personalizaciones y una máquina de estados de tres agregados
(`CalibrationRun`, `CalibrationActivation`, `CalibrationVerification`); (3) una rúbrica jerárquica que
permite **agregar dimensiones nuevas** en una subcalibración (D-57) y **subcriterios ponderables**
dentro de cada dimensión (D-64), lo que el propio plan identifica como contradicción directa con el
modelo de cinco dimensiones fijas de docsV2 (D-62) y exige explícitamente un ADR, contrato y
migración aprobados antes de implementar (D-64).

Por instrucción del coordinador de esta tarea, **donde el plan contradice a docsV2, gana el plan**;
donde es aditivo, se agrega completo con su detalle completo (no resumido).

## Regla vigente

- El **modelo de rúbrica** evoluciona de "cinco dimensiones fijas sin subcriterios" a "dimensiones de
  origen `COURSE` o `CHALLENGE`, cada una con subcriterios ponderables internos que suman 100 % de su
  dimensión" — ver
  [ADR-021](../00-gobierno-y-evolucion/adr/ADR-021-rubrica-jerarquica-dimensiones-extendidas-y-subcriterios-ponderables.md)
  y [07 — Rúbrica ponderada y subcriterios](../03-capacidades-de-ia/golden-set-y-calibracion/07-rubrica-ponderada-y-subcriterios.md).
  **No se ha tocado código, contrato ni persistencia**: el ADR deja explícitamente pendiente el
  contrato y la migración aprobados antes de implementar.
- La **asociación desafío↔calibración** evoluciona de un vínculo simple a un modelo de dos capas con
  herencia, overlay y compilador determinista — ver
  [06 — Subcalibración jerárquica](../03-capacidades-de-ia/golden-set-y-calibracion/06-subcalibracion-jerarquica.md)
  y [08 — Máquina de estados de corridas y activaciones](../03-capacidades-de-ia/golden-set-y-calibracion/08-maquina-de-estados-corridas-y-activaciones.md).
- Se agrega, de forma aditiva, el dominio completo de **skills**: formato, catálogo, visibilidad,
  versionado, favoritos, clonado, metadatos, seguridad/confianza cero, publicación/archivado y reglas
  de contenido — ver
  [05 — Skills: catálogo y formato](../03-capacidades-de-ia/golden-set-y-calibracion/05-skills-catalogo-y-formato.md)
  y [04 — Seguridad y gobierno de skills](../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md).
- Se agregan, de forma aditiva, los flujos de UI del módulo de skills y de los formularios de
  calibración — ver
  [09 — Flujos de UI](../03-capacidades-de-ia/golden-set-y-calibracion/09-flujos-ui-calibracion-y-gestion-skills.md).
- Los tres bloques técnicos que el propio plan declara **fuera de alcance del sprint actual** (libro
  de cuota, pipeline de sanitización de Markdown, observabilidad/auditoría/métricas/pruebas) se
  documentan íntegros como referencia futura, marcados explícitamente como pendientes, sin generar
  tareas, contratos ni migraciones — ver la nota de alcance en
  [04 — Seguridad y gobierno de skills §6](../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md#6-reglas-de-contenido-y-sanitización-síncrona-d-128-a-d-133)
  y en [08 — Máquina de estados §7](../03-capacidades-de-ia/golden-set-y-calibracion/08-maquina-de-estados-corridas-y-activaciones.md#7-concurrencia-de-calibraciones-con-reserva-agregada-de-cuota-d-158).

## Fuentes

`llm-service/docs/03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md` (D-01 a D-20, D-39 a D-77, D-120 a D-161 — rango de este
agente; el resto del plan, D-21 a D-38, D-78 a D-119 y D-162 a D-166, corresponde a otro agente en
paralelo sobre otras carpetas de `docsV3`).

## Documentos V3 corregidos o agregados

**Nuevos:**
`03-capacidades-de-ia/golden-set-y-calibracion/{05-skills-catalogo-y-formato,06-subcalibracion-jerarquica,07-rubrica-ponderada-y-subcriterios,08-maquina-de-estados-corridas-y-activaciones,09-flujos-ui-calibracion-y-gestion-skills}.md`,
`04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md`,
`00-gobierno-y-evolucion/adr/ADR-021-rubrica-jerarquica-dimensiones-extendidas-y-subcriterios-ponderables.md`.

**Anotados con nota de evolución (no reescritos, migración pendiente de aprobación):**
`03-capacidades-de-ia/golden-set-y-calibracion/02-especificacion-funcional.md` (§3 y §11),
`03-capacidades-de-ia/golden-set-y-calibracion/03-modelo-de-dominio-y-transiciones.md` (encabezado).

**READMEs de carpeta actualizados con el listado de archivos nuevos:**
`03-capacidades-de-ia/golden-set-y-calibracion/README.md`,
`04-seguridad-datos-y-cumplimiento/README.md`.

**Pendiente de reconciliación por el coordinador (no editado por este agente):**
`docsV3/README.md` y `docsV3/catalogo-documental.md` — deberían sumar una entrada de segundo nivel
tipo "Skills y calibración jerárquica de desafíos → `03-capacidades-de-ia/golden-set-y-calibracion/README.md`
(sección Skills)" y otra para "Seguridad y gobierno de skills →
`04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md`".
