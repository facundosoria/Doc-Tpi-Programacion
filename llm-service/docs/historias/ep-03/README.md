# Historias de usuario — EP-03 · Golden set y referencia humana

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../plantillas/historia-de-usuario-taiga.md). Estas tres historias (**H05–H07**)
> son **HU de valor**: la protagoniza un **docente** que percibe el resultado. En Taiga
> se cargan como **HU** con puntos Fibonacci contra la historia canónica
> `LLM-S01-H06` e INVEST verificado.
>
> **Fuente de verdad.** ID, épica, pareja, dependencias y horas mandan desde
> [`35` · «S1»](../../35-backlog-ejecutable.md).
> DoR/DoD, desde [23 · §9.2](../../23-plan-construccion-producto-llm.md). Método, desde
> [29](../../29-guia-catedra-historias-de-usuario.md). Contrato HTTP y adenda S1, desde
> [`../../contracts/`](../../contracts/) · [adenda golden set](../../contracts/llm-service-v1-s1-golden-set-adenda.md).
> El desglose en **tareas SMART** está en [`../../tareas/ep-03/`](../../tareas/ep-03/README.md).
>
> **Estimación en puntos.** Ninguna ficha trae puntos Fibonacci: se asignan en el
> **Sprint 0** con Planning Poker contra `LLM-S01-H06` (candidata a canónica). La columna
> *h* del plan es la referencia de planificación y **no se convierte** a puntos.

## Índice — S1 (vigente en la receta original)

| ID | Título | Tipo | Pareja | Dep. | h |
| --- | --- | --- | --- | --- | --: |
| [LLM-S01-H05](h05.md) | Alta de golden set y carga de entradas | **HU** | P5 | H04 | 24 |
| [LLM-S01-H06](h06.md) | Consulta del golden set que sobrevive al reinicio | **HU** *(canónica)* | P5 | H04 | 14 |
| [LLM-S01-H07](h07.md) | Pantalla docente mínima del golden set | **HU** | P5 | H05, H06 | 24 |

> **H05–H07 quedan como registro histórico.** El commit `605f381` reemplazó ese golden set
> simple por el flujo versionado por curso de `LLM-S02-H02` (ver
> [decision-605f381.md](../../entregas/decision-605f381.md)). No re-derivar tareas nuevas
> contra H05–H07 sin revisar primero si `LLM-S02-H02` ya las cubre.

## Índice — S2 (escritas a posteriori, código ya construido)

| ID | Título | Tipo | Pareja | Dep. | Estado |
| --- | --- | --- | --- | --- | --- |
| [LLM-S02-H01](s02-h01.md) | Rúbrica versionada por curso | **HU** | P4 | H04 | 🟢 Construida — pendiente confirmar invariante de pesos |
| [LLM-S02-H02](s02-h02.md) | Golden set versionado por curso, con copia desde la base de plataforma | **HU** | P5 | H04, H05, H06 | 🟢 Construida — pendiente confirmar origen de la base de plataforma |

> Los habilitadores de S1 (**H01–H04, H08, H09**, EP-01) están en [`../ep-01/`](../ep-01/README.md).
> La calibración que consume estas dos historias es [`LLM-S03-H01`](../ep-04/README.md) (EP-04).
> La vista completa del sprint 1 (índice de las 9, demo) está en
> [`../../sprints/s1-historias.md`](../../sprints/s1-historias.md).
