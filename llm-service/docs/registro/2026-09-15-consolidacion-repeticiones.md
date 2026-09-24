# Consolidación de repeticiones documentales — 2026-09-15

## Objetivo

Dejar `docsV2` con una única ubicación editable para cada regla, contrato o decisión repetida,
sin borrar información única ni modificar la documentación V1. Los documentos que cumplen otra
función (plantilla, instancia, historia ejecutable, vista histórica o audiencia distinta) se
mantienen aunque compartan vocabulario.

## Situación anterior

La revisión encontró copias o solapamientos que podían divergir:

- El README de planificación de epics/historias/tareas contenía una guía completa que también
  vivía en `tareas/README.md`.
- `historias/PREGUNTAS-ABIERTAS.md` repetía el archivo canónico de preguntas.
- El contrato de Angular repetía la tabla normativa de errores y estados de operación.
- El mapa histórico incluía diagramas completos que también estaban en los contratos de equipo.
- El contrato de Desafíos Prácticos repetía el objeto JSON del score del evaluador.
- Los nueve contratos completos por contraparte estaban bajo
  `07-planificacion-y-trabajo-equipo/11-equipos/`, fuera de la carpeta dedicada a contratos.
- Sprint 0 repetía literalmente la política de no presentar una funcionalidad parcial como
  terminada, y H01/H02 de EP-09 repetían la misma estimación y prioridad.
- Algunos enlaces apuntaban a las ubicaciones anteriores o a nombres de archivos que ya no eran
  canónicos.

## Decisiones y motivo

Se eligió una fuente canónica por cada repetición. La copia redundante se convirtió en un enlace o
puntero que conserva la ruta de lectura y el contexto necesario. El texto exclusivo, fechas,
excepciones, dependencias, ejemplos históricos y criterios de aceptación se conservaron.

| Tema | Fuente canónica | Acción aplicada |
|---|---|---|
| Guía de tareas | `07.../09-epicas-historias-tareas-sprints/tareas/README.md` | El README de la raíz pasó a ser índice; la guía completa no se eliminó. |
| Preguntas abiertas | `07.../PREGUNTAS-ABIERTAS.md` | El archivo bajo `historias/` quedó como puntero. |
| Errores y UX comunes | `06.../01-operacion-e-ingenieria.md` (§6) | Angular conserva solo su mapeo de pantallas y enlaza a la regla común. |
| Flujos de integración | `contracts/equipos/tema-05-desafios-practicos.md` y `contracts/equipos/tema-11-chat.md` | El mapa histórico conserva contexto transversal e I-04 y enlaza a los detalles. |
| Score del evaluador | `contracts/llm-service.asyncapi.yaml` (`ScoreCalculated`) | T05 referencia el schema; el ejemplo `snake_case` queda únicamente como antecedente histórico en `91`. |
| Contratos por contraparte | `contracts/equipos/*.md` | Se movió el contenido completo a `contracts/`; cada ruta antigua contiene un puntero. |
| Política de aceptación | `07.../03-plan-de-construccion-del-producto.md` (§9) | Sprint 0 conserva su aplicación y referencia la fuente, sin copiar el texto. |
| Estimación EP-09 | `historias/ep-09/h01.md` | H02 conserva la sección y referencia H01 con los mismos valores. |
| Relación épica–historia | `07.../04-backlog-ejecutable.md`, sección «Épicas» | Sprint 0 conserva su tabla propia y referencia la explicación común. |

## Resultado

- `llm-service/docs/` (V1) no se modificó como parte de esta consolidación; sus antecedentes siguen
  disponibles.
- Los contratos actuales tienen una sola carpeta canónica: `docsV2/contracts/`, con las vistas
  narrativas completas en `contracts/equipos/` y los schemas ejecutables en la raíz.
- Los punteros antiguos no contienen una segunda copia editable.
- Los documentos históricos `90` y `91` conservan su función de evidencia y contexto; no se
  presentan como contrato vigente.
- Las plantillas y sus instancias, las historias independientes y los documentos de distinta
  audiencia o nivel temporal permanecen deliberadamente. No son duplicados del mismo documento y
  eliminarlos reduciría información operativa.

## Verificación

Se ejecutaron estas comprobaciones sobre `llm-service/docsV2` después de los cambios:

1. SHA-256 de todos los archivos Markdown, incluidos README: **0 grupos de contenido idéntico**.
2. Verificador de enlaces Markdown relativos: **0 destinos faltantes**.
3. Búsqueda de carpetas llamadas `contracts`: una única carpeta raíz (`docsV2/contracts/`) y su
   subcarpeta organizada `contracts/equipos/`; no existen dos raíces paralelas de contratos.
4. Búsqueda de referencias al antiguo `recursos-completos/`: **0 resultados**.

En una futura corrección se debe editar primero la fuente canónica, actualizar el schema si aplica,
registrar el antes/porqué/después en `registro/` y volver a ejecutar las dos primeras verificaciones.
