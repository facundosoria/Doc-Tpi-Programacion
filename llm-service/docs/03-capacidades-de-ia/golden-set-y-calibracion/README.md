# Golden Set y calibración

Leé los documentos en orden: primero el recorrido, luego la regla funcional y finalmente el modelo.

1. [Plan de revisión](01-plan-de-revision.md)
2. [Especificación funcional](02-especificacion-funcional.md)
3. [Modelo de dominio y transiciones](03-modelo-de-dominio-y-transiciones.md)
4. [Shadow del evaluador (E-31)](04-shadow-del-evaluador.md)

## Skills y calibración jerárquica

Directivas de Producto que se suman a 02 y 03, y que en el caso de la rúbrica jerárquica los
**contradicen y evolucionan** (ver nota de evolución en cada uno y el ADR-021). Fuente completa de
las 166 directivas (D-01 a D-166): [`plan-skills-calibracion.md`](plan-skills-calibracion.md) —
registro incremental de decisiones de Producto, distingue confirmadas de propuestas. **Todavía no
se implementó en código**; los docs 05-09 de abajo son la especificación derivada, ya vigente como
documentación. Para retomar la implementación, usar
[`prompt-agente-implementacion-skills-calibracion.md`](prompt-agente-implementacion-skills-calibracion.md)
(prompt listo para pegarle a un agente implementador, con el orden técnico y las reglas de alcance
del sprint).

Léelos en este orden si vas a tocar skills, subcalibración por desafío o rúbrica con subcriterios:

5. [Skills: catálogo y formato](05-skills-catalogo-y-formato.md) — formato, catálogo, visibilidad,
   versionado, favoritos, asociación a cursos, clonado y metadatos.
6. [Subcalibración jerárquica](06-subcalibracion-jerarquica.md) — precondición, herencia editable,
   cascada, modelo `CourseBaseline`/`ChallengeOverlay`, rebase.
7. [Rúbrica ponderada y subcriterios](07-rubrica-ponderada-y-subcriterios.md) — pesos por dimensión,
   subcriterios ponderables y Golden Set derivado. Requiere
   [ADR-021](../../00-gobierno-y-evolucion/adr/ADR-021-rubrica-jerarquica-dimensiones-extendidas-y-subcriterios-ponderables.md).
8. [Máquina de estados de corridas y activaciones](08-maquina-de-estados-corridas-y-activaciones.md) —
   `CalibrationRun`, `CalibrationActivation`, `CalibrationVerification` y reglas de concurrencia.
9. [Flujos de UI de calibración y gestión de skills](09-flujos-ui-calibracion-y-gestion-skills.md) —
   módulo de skills, formularios, borradores y autorización.

La seguridad y gobierno del contenido de skills (auditoría, confianza cero, publicación/archivado,
reglas de sanitización) vive en
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md).

## Plan de calibración real y activación (PAR14)

[`plan-calibracion-real-y-activacion-par14.md`](plan-calibracion-real-y-activacion-par14.md) —
**Pendiente de implementación** (2026-09-10). Decisiones PAR14-01 a PAR14-07: umbral MAE≤5, tope de
error por caso, evidencia institucional+curso, autorización única y cifrado de API key. No sustituye
a 05-09 ni al ADR-021; léelo como propuesta a confirmar, no como regla vigente.

## Antecedente de anclas y prompts

El material concreto de anclas y prompts está preservado en
[`rubricas-prompts-borrador/`](../../08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/rubricas-prompts-borrador/).
Su archivo central se autodeclara **BORRADOR**: sirve como punto de partida para revisión docente,
pero no sustituye la especificación vigente ni publica una versión final de rúbrica o prompt.
