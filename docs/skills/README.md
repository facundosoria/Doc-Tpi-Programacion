# Bundle de skills — estructura de documentación de proyecto

**Qué es esto.** Un conjunto de skills portables para que **cualquier equipo arme su
documentación con la misma estructura y método** que usamos acá. **No** distribuyen
nuestra documentación: el equipo aporta su contexto (brief, requisitos, funciones, datos
del equipo) y el skill produce los documentos en el formato de la casa, o le dice cómo
producirlos.

**Para qué sirve.** Subirlo al **skill hub** del equipo, o copiar la carpeta a otro repo.
Cada skill es **autónomo**: trae su copia del material fijo y declara en su `SKILL.md`
qué entradas necesita.

**Todo lo generado vive dentro de `docs/`.** El orden de precedencia (qué fuente decide
qué) lo fija `docs/00-fuentes-de-verdad-y-convenciones.md`, que crea el scaffold.

## Dos tipos

| Tipo | Qué hace | Cuáles |
|---|---|---|
| **Arranque** | Crea el esqueleto de `docs/` una vez | `scaffold-planificacion-agil` |
| **Referencia** | Reglas fijas a seguir + plantillas a rellenar. No transforma nada. | `contratos-api-gateway`, `contratos-kafka` |
| **Generador** | Toma una entrada del equipo y produce un documento derivado | `generar-vision-y-alcance`, `generar-backlog-y-recetas`, `generar-epicas`, `generar-historias-usuario` |

## Pipeline

```
scaffold-planificacion-agil   → docs/ (estructura + docs/00 + material fijo)
        │
generar-vision-y-alcance      → docs/vision/
        │
        ├── contratos-api-gateway (referencia, HTTP)   ─┐
        ├── contratos-kafka       (referencia, eventos) ─┼→ docs/contracts/
        └── generar-backlog-y-recetas                    → docs/plan/ + docs/backlog/
                    │
                    ├─→ generar-epicas          → docs/epicas/
                    └─→ generar-historias-usuario → docs/historias/
```

| Skill | Tipo | Entrada | Salida |
|---|---|---|---|
| [`scaffold-planificacion-agil`](scaffold-planificacion-agil/SKILL.md) | arranque | nombre del proyecto + identidad del servicio | `docs/00` (capas + identidad canónica), árbol de `docs/`, `docs/README.md` con tabla de *fuente única*, material fijo copiado, STUBs |
| [`generar-vision-y-alcance`](generar-vision-y-alcance/SKILL.md) | generador | brief + fases | `docs/vision/` — problema, alcance in/out por fase, funciones, fronteras (3 columnas), objetivos, riesgos, glosario |
| [`contratos-api-gateway`](contratos-api-gateway/SKILL.md) | **referencia** | — (se consulta) | reglas del Gateway (de plataforma) + estilo de la casa + esqueleto OpenAPI + plantillas inter-equipos/adenda → `docs/contracts/` (canal HTTP) |
| [`contratos-kafka`](contratos-kafka/SKILL.md) | **referencia** | — (se consulta) | reglas del bus de eventos (de plataforma) + esqueleto AsyncAPI → `docs/contracts/` (canal asíncrono) |
| [`generar-backlog-y-recetas`](generar-backlog-y-recetas/SKILL.md) | generador | brief + requisitos + fases + datos del equipo | `docs/plan/` y `docs/backlog/` — capacidad, catálogo de épicas, Sprint 0, receta por sprint |
| [`generar-epicas`](generar-epicas/SKILL.md) | generador | el catálogo de épicas | `docs/epicas/` — fichas de épica en formato Taiga, una por archivo |
| [`generar-historias-usuario`](generar-historias-usuario/SKILL.md) | generador | una receta de sprint | `docs/historias/` — fichas de HU **en lenguaje simple** (estilo `s01-test.md`) + tareas; variante técnica bajo pedido |

## Anatomía de cada skill

```
<skill>/
├── SKILL.md            frontmatter (name + description) + método/uso + "Entradas que necesito" + checklist
└── references/
    ├── <método>.md      el método o las reglas, condensados (genéricos)
    ├── <plantilla>.md   templates en blanco con placeholders <...>
    └── ejemplo-*.md     un caso resuelto — ILUSTRACIÓN, no algo a copiar
```

Formato de frontmatter tomado del skill `ui-ux-design-guide` que ya estaba en el repo.

## Huecos conocidos (próximos skills)

- `generar-doc-arquitectura` — stack, límites de módulos, ADR, diagramas de integración.
- `generar-normas-de-trabajo` — cobertura, GitFlow, guía de Wiki, convenciones de PR.

## De dónde sale el método

Condensado de la documentación de este repo (que queda como fuente, no se distribuye):
[`docs/29`](../29-guia-catedra-historias-de-usuario.md),
[`docs/23` §9.2](../23-plan-construccion-producto-llm.md),
[`Plan de ejecucion/07`](<../../Plan de ejecucion/07-backlog-ejecutable-sprints.md>),
[`docs/18`](../18-contratos-inter-equipos.md),
[`docs/contracts/`](../contracts/),
[`docs/gateway-y-discovery/`](../gateway-y-discovery/) *(de otro equipo)*,
[`docs/plantillas/`](../plantillas/).

Si el método de cátedra o las convenciones de plataforma cambian, los skills se
actualizan a mano.
