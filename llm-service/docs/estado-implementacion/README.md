# Estado de implementación — historias vs. código real

> **Qué es esta carpeta.** El tablero de **qué se está "quemando"**: cruza cada historia de
> [`docs/historias/`](../historias/README.md) (y cada pieza de código que **todavía no tiene
> ficha**) contra lo que hay hoy en `llm-service/` y `llm-workbench/`. Mismo patrón de carpetas
> que `docs/historias/`: este `README.md` + una subcarpeta por épica con un `.md` por historia (o
> por subsistema, cuando no hay ficha).
>
> **🟢 2026-09-12 — `codigo-ejemplo/` se consolidó y se eliminó.** Lo que servía (puerto de
> invocación de modelos, guardarraíles del tutor) se portó a `llm-service/` — ver
> [`ep-02/h10.md`](ep-02/h10.md) y [`ep-05/`](ep-05/README.md). El análisis de qué era cada
> proyecto y qué se portó queda registrado en [`codigo-ejemplo/`](codigo-ejemplo/README.md) como
> historia, aunque la carpeta de código ya no existe en el repo.
>
> **Qué NO es.** No es la fuente de verdad del *qué* se acordó construir — eso sigue viviendo en
> [`docs/historias/`](../historias/README.md) (CA/BDD) y [`docs/epicas/`](../epicas/README.md)
> (objetivo de la épica). Tampoco reemplaza la narrativa de decisión de
> [`docs/entregas/`](../entregas/) (por qué pasó lo que pasó, p. ej.
> [`decision-605f381.md`](../entregas/decision-605f381.md)) ni el detalle exhaustivo, ítem por
> ítem, de las auditorías fuente (ver tabla de abajo). Este tablero **resume y organiza por
> historia**, con link a la fuente para el detalle.
> No se tocó código de `llm-service/`, `llm-workbench/` ni `codigo-ejemplo/` para armarlo
> ([[no-tocar-codigo-ajeno]]): es lectura + documentación.
>
> **Fuentes que consolida** (no las repite línea por línea, referencia lo puntual):
>
> | Fuente | Qué cubre |
> |---|---|
> | [`docs/entregas/decision-605f381.md`](../entregas/decision-605f381.md) | Qué reemplazó el commit `605f381` y por qué |
> | [`docs/entregas/verificacion-v2-golden-set-calibracion.md`](../entregas/verificacion-v2-golden-set-calibracion.md) | Auditoría de golden set/rúbrica/calibración v2 (EP-03, EP-04) |
> | [`llm-service/CORRECCIONES-SUGERIDAS.md`](../../llm-service/CORRECCIONES-SUGERIDAS.md) | 22 hallazgos puntuales de código, incluida la auditoría CA-por-CA de EP-01 (H01–H04, H08, H09) y EP-02 (H10) que ninguna otra ficha tenía todavía |
> | Informe del agente sobre `codigo-ejemplo/` (2026-09-12, ver [`codigo-ejemplo/README.md`](codigo-ejemplo/README.md)) | Qué son los dos proyectos de referencia y a qué épica se parecen |
>
> **Cómo se actualiza.** Cuando el código cambie, re-auditar la historia puntual (no hace falta
> reescribir todo el tablero) y actualizar su `.md` y la fila del índice de su `README.md` de
> épica. Igual que aclara `verificacion-v2-golden-set-calibracion.md`: esto es una foto, no una
> promesa — se re-audita cuando hace falta, no automáticamente.

## Leyenda de estado

| Ícono | Significa |
|---|---|
| 🟢 | Construida / cumple lo que pide la ficha |
| 🟡 | Parcial — hay código real, pero con huecos concretos |
| 🔴 | Hueco crítico — falta la pieza central, o el código no cumple lo que promete |
| ⚪ | No auditada en profundidad todavía |
| 🕓 | Histórica — la ficha describe código que ya no existe (superada por otra) |
| ⬜ | No iniciada — no hay código ni ficha |

## Resumen por épica

| Épica | Carpeta | Estado global | Última auditoría |
|---|---|---|---|
| **EP-01** · Plataforma, contratos e integración | [`ep-01/`](ep-01/README.md) | 🟡 desigual — H04 🟢, resto con huecos concretos (ver carpeta) | 2026-09-12 |
| **EP-02** · AI Gateway, modelos y resiliencia | [`ep-02/`](ep-02/README.md) | 🟢 H10 construida (puerto + fake); 🔴 catálogo de modelos por curso sigue hardcodeado | 2026-09-12 |
| **EP-03** · Golden set y referencia humana | [`ep-03/`](ep-03/README.md) | 🟢 golden set y rúbrica por curso sólidos; 🔴 dos placeholders sin LLM real | 2026-09-12 |
| **EP-04** · Calibración y gobernanza del modelo | [`ep-04/`](ep-04/README.md) | 🔴 esqueleto sólido, pero ninguna calibración puede terminar (H10 existe, la calibración no la usa todavía) | 2026-09-12 |
| **EP-05** · Tutor seguro y guardarraíles | [`ep-05/`](ep-05/README.md) | 🟡 interacción síncrona con guardarraíles construida; sin historia formal ni streaming | 2026-09-12 |
| EP-06 · Evaluación, score y auditoría académica | — | ⬜ / ⚪ ver [`pendiente-de-epica/course-evaluation-status.md`](pendiente-de-epica/course-evaluation-status.md) | 2026-09-12 |
| EP-07 · Operación, cuotas y observabilidad | — | ⬜ sin código encontrado | — |
| EP-08 · Moderación integrada (F2) | — | ⬜ sin código; contrato de referencia preservado en [`docs/contracts/llm-service-v1-moderacion-borrador.yaml`](../contracts/llm-service-v1-moderacion-borrador.yaml) | 2026-09-12 |
| EP-09 · RAG y consulta de material (F3) | — | ⬜ sin código encontrado | — |
| EP-10 · Personalización y agente (F3) | — | ⬜ sin código encontrado | — |

> Subsistemas que el código ya construyó pero que ninguna épica reclama con certeza:
> [`pendiente-de-epica/`](pendiente-de-epica/README.md). Análisis de los dos proyectos de
> referencia: [`codigo-ejemplo/`](codigo-ejemplo/README.md).

## El hallazgo transversal más importante

El código de `llm-service` **no avanza parejo con el backlog**: en EP-03/EP-04 va muy
adelantado (construyó golden set versionado, rúbrica versionada y el esqueleto completo de
calibración con métrica PAR-14 — funcionalidad de S2/S3 — sin que existieran las fichas), pero
en EP-01 sigue con huecos que las fichas de S1 dan por hechos (Eureka, `401`, JaCoCo — ver
[`ep-01/`](ep-01/README.md)). El puerto de invocación de modelos de `LLM-S01-H10` (EP-02), que
estaba en 0 de 6 tareas, se cerró el 2026-09-12 portando código de `codigo-ejemplo/` — pero
**la calibración de EP-04 todavía no lo usa**: hoy todo run sigue quedando `RUNNING` para siempre,
porque falta la pieza que conecte `CalibrationWorkflowService` con `ModelInvocationService`. Ver
[`ep-02/h10.md`](ep-02/h10.md) y [`ep-04/s03-h01.md`](ep-04/s03-h01.md).
