# Bundle de skills — estructura de documentación de proyecto

**Qué es esto.** Un conjunto de skills portables para que **cualquier equipo arme su
documentación con la misma estructura y método** que usamos acá. **No** distribuyen
nuestra documentación: el equipo aporta su contexto (brief, requisitos, funciones, datos
del equipo) y el skill produce los documentos en el formato de la casa, o le dice cómo
producirlos.

**Dónde viven.** Estos 8 skills están en `.agents/skills/` — la ubicación estándar de
Claude Code, donde se **auto-descubren e invocan** (igual que `ui-ux-design-guide` y
`web-design-reviewer`). Cada uno es **autónomo**: trae su copia del material fijo y
declara en su `SKILL.md` qué entradas necesita.

**Para llevarlos a otro repo:** copiar las carpetas a `.agents/skills/` de ese repo. Para
el **skill hub**: se suben las convenciones condensadas (`type: reference` / `convention`),
no los bundles enteros — el hub hoy guarda un `.md` por entrada, no carpetas.

**Todo lo generado vive dentro de `docs/`.** El orden de precedencia (qué fuente decide
qué) lo fija `docs/00-fuentes-de-verdad-y-convenciones.md`, que crea el scaffold. Este
bundle **no** es parte del árbol `docs/`: es la herramienta que lo produce.

## Dos tipos

| Tipo | Qué hace | Cuáles |
|---|---|---|
| **Arranque** | Crea el esqueleto de `docs/` una vez | `scaffold-planificacion-agil` |
| **Referencia** | Reglas fijas a seguir + plantillas a rellenar. No transforma nada. | `contratos-api-gateway`, `contratos-kafka` |
| **Generador** | Toma una entrada del equipo y produce un documento derivado | `generar-vision-y-alcance`, `generar-backlog-y-recetas`, `generar-epicas`, `generar-historias-usuario`, `generar-tareas` |

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
                                │
                                └─→ generar-tareas → docs/tareas/
```

| Skill | Tipo | Entrada | Salida |
|---|---|---|---|
| [`scaffold-planificacion-agil`](scaffold-planificacion-agil/SKILL.md) | arranque | nombre del proyecto + identidad del servicio | `docs/00` (capas + identidad canónica), árbol de `docs/`, `docs/README.md` con tabla de *fuente única*, material fijo copiado, STUBs |
| [`generar-vision-y-alcance`](generar-vision-y-alcance/SKILL.md) | generador | brief + fases | `docs/vision/` — problema, alcance in/out por fase, funciones, fronteras (3 columnas), objetivos, riesgos, glosario |
| [`contratos-api-gateway`](contratos-api-gateway/SKILL.md) | **referencia** | — (se consulta) | reglas del Gateway (de plataforma) + estilo de la casa + esqueleto OpenAPI + plantillas inter-equipos/adenda → `docs/contracts/` (canal HTTP) |
| [`contratos-kafka`](contratos-kafka/SKILL.md) | **referencia** | — (se consulta) | reglas del bus de eventos (de plataforma) + esqueleto AsyncAPI → `docs/contracts/` (canal asíncrono) |
| [`generar-backlog-y-recetas`](generar-backlog-y-recetas/SKILL.md) | generador | brief + requisitos + fases + datos del equipo | `docs/plan/` y `docs/backlog/` — capacidad, catálogo de épicas, Sprint 0, receta por sprint |
| [`generar-epicas`](generar-epicas/SKILL.md) | generador | el catálogo de épicas | `docs/epicas/` — fichas de épica en formato Taiga, una por archivo |
| [`generar-historias-usuario`](generar-historias-usuario/SKILL.md) | generador | una receta de sprint | `docs/historias/` — fichas de HU **en lenguaje simple** (variante técnica bajo pedido); el desglose en tareas va aparte (`generar-tareas`) |
| [`generar-tareas`](generar-tareas/SKILL.md) | generador | una HU con sus CA y su BDD | `docs/tareas/` — tareas **SMART** por historia en formato de Tarea de Taiga, cada una trazada a un CA o escenario y partida en ≤ 1 jornada |

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

## Material compartido (mantener en sync)

Cada skill es autónomo y trae su propia copia del material fijo, así que hay archivos
**duplicados byte a byte** entre skills. El original vive en `scaffold-planificacion-agil/`
(es el skill que copia ese material a `docs/`); los demás son copias. Si tocás uno,
replicá el cambio en su par.

| Contenido | Original (en `scaffold-planificacion-agil/references/`) | Copia |
|---|---|---|
| Template de Épica de Taiga | `plantilla-epica.md` | `generar-epicas/references/plantilla-epica.md` |
| Template de HU de Taiga (lenguaje simple) | `plantilla-historia-usuario.md` | `generar-historias-usuario/references/plantilla-historia-usuario.md` |
| Template de Tarea de Taiga (SMART) | `plantilla-tarea-taiga.md` | `generar-tareas/references/plantilla-tarea-taiga.md` |
| Método SMART para tareas | `metodo-tareas-smart.md` | `generar-tareas/references/guia-metodo-smart.md` |
| Glosario simple↔técnico de HU | `estilo-tecnico-historias.md` | `generar-historias-usuario/references/referencia-estilo-tecnico.md` |
| Método de cátedra para HU | `metodo-historias-de-usuario.md` | `generar-historias-usuario/references/guia-metodo.md` |
| DoR/DoD de arranque | `dor-dod-starter.md` | `generar-historias-usuario/references/dor-dod.md` |

Chequeo: un `diff` entre cada original y su copia no debe devolver nada.

## Huecos conocidos (próximos skills)

- `generar-doc-arquitectura` — stack, límites de módulos, ADR, diagramas de integración.
- `generar-normas-de-trabajo` — cobertura, GitFlow, guía de Wiki, convenciones de PR.

## De dónde sale el método

Condensado de la documentación de este repo (que queda como fuente, no se distribuye):
[`docs/29`](../../docs/29-guia-catedra-historias-de-usuario.md),
[`docs/23` §9.2](../../docs/23-plan-construccion-producto-llm.md),
[`docs/35`](../../docs/35-backlog-ejecutable.md),
[`docs/18`](../../docs/18-contratos-inter-equipos.md),
[`docs/contracts/`](../../docs/contracts/),
[`docs/gateway-y-discovery/`](../../docs/gateway-y-discovery/) *(de otro equipo)*,
[`docs/plantillas/`](../../docs/plantillas/).

Si el método de cátedra o las convenciones de plataforma cambian, los skills se
actualizan a mano.
