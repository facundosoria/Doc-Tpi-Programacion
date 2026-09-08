# Skills de estructura de proyecto — bundle para el equipo

**Qué es esto.** Un conjunto de skills portables para que **cualquier equipo arme su
documentación de proyecto con la misma estructura y método** que usamos acá. **No**
distribuyen nuestra documentación: son **generadores**. Cada equipo aporta su contexto
(brief, requisitos, funciones, datos del equipo) y el skill produce los documentos en el
formato de la casa.

**Para qué sirve.** Subirlo al **skill hub** del equipo, o copiar la carpeta
`docs/skills/` a otro repo. Cada skill es **autónomo**: trae su propia copia del material
fijo (templates de Taiga, métodos, convenciones) y declara en su `SKILL.md` qué entradas
necesita y qué produce.

**Todo lo que generan vive dentro de `docs/`.** El orden de precedencia (qué fuente
decide qué) lo fija `docs/00-fuentes-de-verdad-y-convenciones.md`, que crea el scaffold.

## Pipeline

```
scaffold-planificacion-agil   → docs/ (estructura + docs/00 + material fijo)
        │
generar-vision-y-alcance ─┐    → docs/vision/
                          ├─→ generar-contratos-servicio ─┐  → docs/contracts/
                          └─→ generar-backlog-y-recetas ───┼─→ generar-epicas          → docs/epicas/
                                                           └─→ generar-historias-usuario → docs/historias/
```

| # | Skill | Entrada | Salida (estructura, no contenido nuestro) |
|---|---|---|---|
| 0 | [`scaffold-planificacion-agil`](scaffold-planificacion-agil/SKILL.md) | nombre del proyecto + identidad del servicio | `docs/00` (capas + identidad canónica), árbol de `docs/`, `docs/README.md` con tabla de *fuente única*, convención de encabezado, material fijo copiado, STUBs |
| 1 | [`generar-vision-y-alcance`](generar-vision-y-alcance/SKILL.md) *(hueco)* | brief + fases | `docs/vision/` — problema, alcance in/out, funciones, objetivos medibles, glosario |
| 2 | [`generar-contratos-servicio`](generar-contratos-servicio/SKILL.md) | nombre del servicio + funciones/recursos + integraciones | `docs/contracts/` — OpenAPI v1 (recursos, no RPC), AsyncAPI v1, contrato con el API Gateway, doc inter-equipos, adenda por sprint |
| 3 | [`generar-backlog-y-recetas`](generar-backlog-y-recetas/SKILL.md) | brief + requisitos + fases + datos del equipo | `docs/plan/` y `docs/backlog/` — capacidad, catálogo de épicas, Sprint 0, receta por sprint |
| 4 | [`generar-epicas`](generar-epicas/SKILL.md) | el catálogo de épicas (#3) | `docs/epicas/` — fichas de épica en formato Taiga, una por archivo |
| 5 | [`generar-historias-usuario`](generar-historias-usuario/SKILL.md) | una receta de sprint (#3) | `docs/historias/` — fichas de HU **en lenguaje simple** (estilo `s01-test.md`) + tareas; variante técnica bajo pedido |

## Anatomía de cada skill

```
<skill>/
├── SKILL.md            frontmatter (name + description) + método + "Entradas que necesito" + checklist
└── references/
    ├── <método>.md      el método condensado (genérico)
    ├── <plantilla>.md   templates en blanco con placeholders <...>
    └── ejemplo-*.md     un caso resuelto — ILUSTRACIÓN, no algo a copiar
```

Formato de frontmatter tomado del skill que ya teníamos en `.agents/skills/`.

## Huecos conocidos (próximos skills)

- `generar-vision-y-alcance` — brief → problema, alcance in/out, funciones, objetivos
  medibles, glosario. Aguas arriba de todo. **No hecho aún.**
- `generar-doc-arquitectura` — stack, límites de módulos, ADR, diagramas de integración.
- `generar-normas-de-trabajo` — cobertura, GitFlow, guía de Wiki, convenciones de PR.

## De dónde sale el método

Condensado de la documentación de este repo (que queda como fuente, no se distribuye):
[`docs/29`](../29-guia-catedra-historias-de-usuario.md),
[`docs/23` §9.2](../23-plan-construccion-producto-llm.md),
[`Plan de ejecucion/07`](<../../Plan de ejecucion/07-backlog-ejecutable-sprints.md>),
[`docs/18`](../18-contratos-inter-equipos.md),
[`docs/contracts/`](../contracts/),
[`docs/gateway-y-discovery/`](../gateway-y-discovery/),
[`docs/plantillas/`](../plantillas/).

Si el método de cátedra o las convenciones de plataforma cambian, los skills se
actualizan a mano (no se regeneran desde el original).
