# Épicas del `llm-service` — fichas en formato Taiga

> **Qué es esta carpeta.** Las **diez épicas** del producto (EP-01…EP-10), cada una en su
> propio archivo con el formato exacto del [template oficial de Épica de la Wiki de
> Taiga](../plantillas/epica-taiga.md): *Objetivo*, *Suposiciones y Restricciones*,
> *Criterios de Aceptación a nivel Épico* y *Dependencias / Impactos*.
>
> **Cómo se usan.** Copiar cada archivo a partir de `# [GXX] — EP-0X: …` al crear la épica
> en el módulo **Epics** de Taiga. Reemplazar `GXX` por el número de grupo asignado por la
> cátedra (`G01`, `G02`, …) y borrar los textos de ejemplo antes de publicar.
>
> **Fuente de verdad.** El catálogo maestro (resultado que habilita, pareja líder,
> sprints, requisitos) vive en
> [`Plan de ejecucion/07` · «Épicas»](<../../Plan de ejecucion/07-backlog-ejecutable-sprints.md>)
> y en [30 · §4](../30-arranque-agil-y-sprint-0.md). La tabla **Catálogo** de esta página
> es su copia en el repo (con la columna **Fase**) y es lo que las fichas referencian:
> pareja, sprints, fase y requisitos **no** se repiten en cada `ep-0X.md`. Si un dato de
> una ficha no coincide con esta tabla o con la fuente maestra, **manda la fuente**.
>
> **Reglas de cátedra.**
> - Las épicas **no se estiman en puntos Fibonacci ni se comprometen en un sprint**: se
>   cierran cuando **todas sus historias** cumplen la DoD ([23 · §9.2](../23-plan-construccion-producto-llm.md)).
> - El template oficial de épica **no** usa `Como / Quiero / Para` ni escenarios BDD: eso
>   es exclusivo de las Historias de Usuario ([29 · §4](../29-guia-catedra-historias-de-usuario.md),
>   [30 · §4](../30-arranque-agil-y-sprint-0.md)).
> - Cada historia `LLM-Sxx-Hyy` pertenece a **una** épica: el **sprint** dice *cuándo* se
>   construye y la **épica**, *para qué sirve en el producto*.
> - **MoSCoW, INVEST y puntos Fibonacci son criterios de Historia de Usuario, no de
>   épica** ([29 · §3 y §6](../29-guia-catedra-historias-de-usuario.md)). La priorización a
>   nivel épica es **fase (F1/F2/F3) + sprint**, y vive en este catálogo, no en la ficha.

---

## Catálogo

Pareja líder, sprints donde aporta, fase y requisitos que cubre cada épica: viven **acá**,
no en la ficha. Si un dato de una ficha no coincide con esta tabla, **manda la tabla**.

| Épica | Ficha | Nombre | Fase | Pareja líder | Sprints | Requisitos (orientativo) |
|---|---|---|---|---|---|---|
| **EP-01** | [`ep-01.md`](ep-01.md) | Plataforma, contratos e integración | F1 (+ operación integral) | P1 | S1, S3, S6, S10, S19 | RF-NFR-01/03/04/09/10; contratos v1 |
| **EP-02** | [`ep-02.md`](ep-02.md) | AI Gateway, modelos y resiliencia | F1 | P2 | S3, S8, S9 | RF-IA-22/23/24/35 |
| **EP-03** | [`ep-03.md`](ep-03.md) | Golden set y referencia humana | F1 | P5 + P4 | S1, S2 | RF-IA-29/30 a 36 |
| **EP-04** | [`ep-04.md`](ep-04.md) | Calibración y gobernanza del modelo | F1 | P4 | S3, S4, S8 | RF-IA-30 a 36; PAR-14 |
| **EP-05** | [`ep-05.md`](ep-05.md) | Tutor seguro y guardarraíles | F1 | P3 + P2 | S5 | RF-IA-01/02/04/19/20 |
| **EP-06** | [`ep-06.md`](ep-06.md) | Evaluación, score y auditoría académica | F1 | P4 + P1 | S6, S7 | RF-IA-12 a 18/25/27/34 |
| **EP-07** | [`ep-07.md`](ep-07.md) | Operación, cuotas y observabilidad | F1 | P2 + P1 | S9, S10 | RF-IA-22/25; RF-NFR-01/03/04 |
| **EP-08** | [`ep-08.md`](ep-08.md) | Moderación integrada (F2) | F2 | P3 + P2 + P1 | S11–S13 | RF-CHT-09 a 14 |
| **EP-09** | [`ep-09.md`](ep-09.md) | RAG y consulta de material (F3) | F3 | P5 + P3 | S14–S16 | RF-IA-06/07/08 |
| **EP-10** | [`ep-10.md`](ep-10.md) | Personalización y agente (F3) | F3 | P5 + P4 + P3 | S17–S18 | RF-DES-05; RF-CHT-05/08 |

> Los números de RF son orientativos; la traza fina historia → requisito se mantiene en
> [21 · matriz de trazabilidad](../21-matriz-trazabilidad-llm.md). Las parejas P1–P5 se
> definen en [23 · §3](../23-plan-construccion-producto-llm.md). Las fases F2 y F3 **no se
> adelantan por disponibilidad técnica**: dependen del contrato y de la fase previa.

**Fronteras entre épicas** (para no duplicar historias):

- La **resiliencia diferida** (RF-IA-27: caída del evaluador → score diferido) es de
  **EP-06**, no de EP-02. EP-02 cubre la resiliencia *síncrona* de la llamada al modelo.
- La **trazabilidad multi-modelo y cohortes afectadas** (RF-IA-33) es de **EP-04**, no de
  EP-07. EP-07 cubre el panel operativo y la recuperación de trabajos.
- La **definición de la rúbrica** (pesos, dimensiones) se acuerda en **EP-03**; **EP-04**
  y **EP-06** la consumen, no la cambian.

## Estado por sprint

| Sprint | Épicas activas | Historias | Fichas de HU |
|---|---|---|---|
| **S1** | EP-01 (H01–H04, H08, H09) · EP-03 (H05–H07) | 9 | [`historias/s01.md`](../historias/s01.md) |
| **S2** | EP-03 | *(a desglosar en el Refinamiento previo a S2)* | — |
| **S3–S19** | ver catálogo | *(a desglosar sprint a sprint)* | — |

Las historias de un sprint se derivan de la **receta** correspondiente en
[`Plan de ejecucion/07`](<../../Plan de ejecucion/07-backlog-ejecutable-sprints.md>) durante
el Refinamiento, no se pre-cargan (evita inventar estimaciones sin contrato).
