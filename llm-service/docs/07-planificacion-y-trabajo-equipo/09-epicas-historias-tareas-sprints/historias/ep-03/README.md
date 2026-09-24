# Historias de usuario — EP-03 · Golden set y referencia humana

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../../10-plantillas/historia-de-usuario-taiga.md). Estas historias (**H01–H05**)
> son **HU de valor**: la protagoniza un **docente** que percibe el resultado. En Taiga
> se cargan como **HU** con puntos Fibonacci contra la historia canónica
> `LLM-EP03-H02` (ex `LLM-S01-H06`) e INVEST verificado.
>
> **Fuente de verdad.** ID, épica, pareja, dependencias y horas mandan desde
> [`35` · «S1»](../../../04-backlog-ejecutable.md).
> DoR/DoD, desde [23 · §9.2](../../../03-plan-de-construccion-del-producto.md). Método, desde
> [29](../../../08-guia-de-historias-de-usuario.md). Contrato HTTP y adenda S1, desde
> [`../../contracts/`](../../../../contracts) (contrato vigente; la adenda S1 es histórica, retirada).
> El desglose en **tareas SMART** está en [`../../tareas/ep-03/`](../../tareas/ep-03/README.md).
>
> **Estimación en puntos.** Ninguna ficha trae puntos Fibonacci: se asignan en el
> **Sprint 0** con Planning Poker contra `LLM-EP03-H02` (candidata a canónica). La columna
> *h* del plan es la referencia de planificación y **no se convierte** a puntos.

## Índice — S1 (base inicial del golden set, registro histórico)

| ID | Título | Tipo | Pareja | Dep. | h |
| --- | --- | --- | --- | --- | --: |
| [LLM-EP03-H01](h01.md) | Alta de golden set y carga de entradas *(ex-S01-H05)* | **HU** | P5 | EP-01·H04 | 24 |
| [LLM-EP03-H02](h02.md) | Consulta del golden set que sobrevive al reinicio *(canónica, ex-S01-H06)* | **HU** *(canónica)* | P5 | EP-01·H04 | 14 |
| [LLM-EP03-H03](h03.md) | Pantalla docente mínima del golden set *(ex-S01-H07)* | **HU** | P5 | H01, H02 | 24 |

> **H01–H03 quedan como registro histórico.** El commit `605f381` reemplazó ese golden set
> simple por el flujo versionado por curso de `LLM-EP03-H05` (ver
> [decision-605f381.md](../../../../01-vision-alcance-y-entrega/03-entregas/decision-605f381.md)). No re-derivar tareas nuevas
> contra H01–H03 sin revisar primero si `LLM-EP03-H05` ya las cubre.

## Índice — S2 (versionado por curso, código ya construido y vigente)

| ID | Título | Tipo | Pareja | Dep. | Estado |
| --- | --- | --- | --- | --- | --- |
| [LLM-EP03-H04](h04.md) | Rúbrica versionada por curso *(ex-S02-H01)* | **HU** | P4 | EP-01·H04 | 🟢 Construida — pendiente confirmar invariante de pesos |
| [LLM-EP03-H05](h05.md) | Golden set versionado por curso, con copia desde plataforma *(ex-S02-H02)* | **HU** | P5 | EP-01·H04, H01, H02 | 🟢 Construida — pendiente confirmar origen de base de plataforma |
| [LLM-EP03-H06](h06.md) *(propuesta)* | Doble puntuación independiente y resolución de discrepancias | **HU** | P5 | H05 | ⚪ Propuesta — sin alta en `35` |

> **H06 es una propuesta, no backlog confirmado.** Cierra la mitad del objetivo de épica que
> exige dos docentes puntuando el mismo caso de forma independiente, con discrepancias resueltas
> antes de publicar — hoy ninguna otra ficha lo cubre.

> Los habilitadores de S1 (**EP-01**, H01–H06) están en [`../ep-01/`](../ep-01/README.md).
> La calibración que consume estas dos historias es [`LLM-EP04-H01`](../ep-04/README.md) (EP-04).
> La vista completa del sprint 1 (índice, demo) está en
> [`../../sprints/sprint-1/s1-historias.md`](../../sprints/sprint-1/s1-historias.md).
