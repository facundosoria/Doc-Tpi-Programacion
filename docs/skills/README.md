# Skills de planificación ágil

Cuatro skills portables para **regenerar la documentación de planificación** (plan,
backlog, épicas, historias) con el mismo método y los mismos templates de Taiga con que
se armó la de este repo. Pensados para subir al **skill hub** del equipo; acá viven como
entregable y referencia.

Cada skill es **autónomo** (trae su copia del material fijo) y tiene una sección
«Entradas que necesito» en su `SKILL.md`. Se pueden **encadenar** o usar sueltos.

## Pipeline

| # | Skill | Entrada | Salida |
|---|---|---|---|
| 1 | [`scaffold-planificacion-agil`](scaffold-planificacion-agil/SKILL.md) | raíz de docs + nombre del proyecto | árbol de carpetas + índice de *fuente única* + convención de encabezado + copia del material fijo (templates, métodos, DoR/DoD starter, registro de sprint). Deja STUBs. |
| 2 | [`generar-backlog-y-recetas`](generar-backlog-y-recetas/SKILL.md) | brief + requisitos + fases + datos del equipo | cálculo de capacidad + catálogo de épicas + Sprint 0 + una receta por sprint (objetivo, demo, paquetes con horas, gates, aceptación negativa) |
| 3 | [`generar-epicas`](generar-epicas/SKILL.md) | el catálogo de épicas (#2) | fichas de épica en formato Taiga, una por archivo |
| 4 | [`generar-historias-usuario`](generar-historias-usuario/SKILL.md) | una receta de sprint (#2) | fichas de HU largas + desglose en tareas |

```
scaffold → backlog-y-recetas → ├─ generar-epicas
                               └─ generar-historias-usuario
```

## Fuentes que condensan

El método y los templates salen de la documentación de este repo:
[`docs/29`](../29-guia-catedra-historias-de-usuario.md) (método de HU),
[`docs/23` §9.2](../23-plan-construccion-producto-llm.md) (DoR/DoD y capacidad),
[`Plan de ejecucion/07`](<../../Plan de ejecucion/07-backlog-ejecutable-sprints.md>)
(backlog y recetas), [`docs/30`](../30-arranque-agil-y-sprint-0.md) (vista de arranque),
[`docs/plantillas/`](../plantillas/) (templates oficiales de Taiga).

Los skills llevan una **copia condensada**: si el método de cátedra cambia, se
actualizan a mano (no se generan desde el original).
