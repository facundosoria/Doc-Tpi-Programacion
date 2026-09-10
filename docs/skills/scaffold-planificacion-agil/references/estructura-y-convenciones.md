# Estructura de `docs/` y convenciones

Todo lo que generan estos skills vive **dentro de `docs/`**. Lo que hace que la
documentación «esté bien estructurada» no es el árbol de carpetas: son **una jerarquía de
capas** y **cuatro disciplinas**. El árbol solo las hace visibles.

---

## 1. Las capas (orden de precedencia)

Documento ancla: **`docs/00-fuentes-de-verdad-y-convenciones.md`**. Fija qué fuente
decide qué, de arriba hacia abajo:

| Prioridad | Fuente | Decide |
|---|---|---|
| 1 | Documento(s) de producto / PRD del cliente | Alcance funcional, fases, reglas de negocio, criterios de release. |
| 2 | Documento(s) de plataforma / arquitectura transversal | Red, Gateway, discovery, identidad, rutas, observabilidad, pruebas de integración. |
| 3 | `docs/00`, los **contratos v1** y los **ADR** | Aplicación concreta de 1 y 2 a este servicio. |
| 4 | Resto de `docs/` | Explicación y detalle. **No puede contradecir** a 1–3. |

Material importado de otros equipos va en `docs/importado/` (o `docs/referencia/`): es
histórico, **no participa de la jerarquía y no se modifica** (las correcciones van en un
`.md` aparte).

`docs/00` también fija la **identidad canónica** del servicio (repo, `spring.application.name`,
prefijo `/api/<n>/**`, `aud`), que después usan `generar-contratos-servicio` y las demás.

## 2. Fuente única (un dato, un lugar)

Cada dato vive en **exactamente un** documento; todo lo demás lo **referencia**. El
`docs/README.md` lleva esta tabla:

| Dato | Fuente única |
|---|---|
| Precedencia de fuentes e identidad canónica | `docs/00-fuentes-de-verdad-y-convenciones.md` |
| Problema, alcance in/out, funciones, objetivos | `docs/vision/vision-y-alcance.md` |
| Contrato HTTP (OpenAPI) y de eventos (AsyncAPI) | `docs/contracts/<servicio>-v1.openapi.yaml` / `.asyncapi.yaml` |
| Contrato con el Gateway y con cada equipo | `docs/contracts/NN-contratos-inter-equipos.md` |
| Cálculo de capacidad del sprint | `docs/plan/plan-construccion.md` |
| Catálogo de épicas y recetas de sprint | `docs/backlog/backlog-ejecutable.md` |
| Esquema de IDs internos (`Sxx-Hyy`) | `docs/plan/plan-construccion.md` |
| Definition of Ready / Definition of Done | `docs/dor-dod.md` |
| Método para redactar HU / épicas | `docs/metodo/*.md` |
| Matriz de trazabilidad historia → requisito | `docs/plan/matriz-trazabilidad.md` |

Si dos documentos dicen algo distinto sobre el mismo dato, **manda la fuente única**.

## 3. Fuente de verdad vs. formato de presentación

- **Documentos fuente**: `docs/00`, visión, plan, backlog, contratos. Abren declarando
  que *ellos son la fuente* de tal cosa.
- **Vistas derivadas**: fichas de épica (`docs/epicas/ep-01.md`…), fichas de HU
  (`docs/historias/sXX.md`), propuestas de sprint (`docs/sprints/`). Son **formato de
  presentación** para cargar en Taiga o presentar. No son fuente de planificación.

Toda vista derivada abre con:

```markdown
> **Qué es.** [Formato de presentación de <X> con el template oficial de Taiga.]
>
> **Qué NO es.** No es fuente de verdad de planificación. Si un dato no coincide:
>
> | Dato | Fuente única |
> |---|---|
> | ID, épica, pareja, dependencias, horas | `docs/backlog/backlog-ejecutable.md` |
> | DoR / DoD | `docs/dor-dod.md` |
> | Método para redactar y estimar | `docs/metodo/historias-de-usuario.md` |
```

## 4. Trazabilidad

Cada historia `Sxx-Hyy` enlaza hacia arriba: **historia → épica** (para qué sirve) y
**historia → requisito** (`RF-*` / `PAR-*`). El sprint dice *cuándo*. Ninguna historia
queda sin épica ni sin requisito (o marcado `*(a trazar)*`).

## 5. Encabezado honesto

Ningún documento afirma más de lo que tiene. Un STUB dice «STUB — lo genera el skill X».
Una capacidad de referencia dice «referencia, no acredita horas reales». Una historia sin
puntos dice «se asignan en el Sprint 0», no un número inventado.

---

## Árbol de `docs/`

```
docs/
├── README.md                          índice + tabla de fuente única
├── 00-fuentes-de-verdad-y-convenciones.md   capas (precedencia) + identidad canónica
├── vision/
│   └── vision-y-alcance.md             problema, alcance in/out, funciones, objetivos, glosario
├── plan/
│   ├── plan-construccion.md            capacidad, parejas/equipos, esquema de IDs, DoR/DoD (o puntero)
│   └── matriz-trazabilidad.md          historia → requisito
├── backlog/
│   └── backlog-ejecutable.md           catálogo de épicas + Sprint 0 + recetas S1..SN
├── contracts/
│   ├── <servicio>-v1.openapi.yaml      HTTP por recursos
│   ├── <servicio>-v1.asyncapi.yaml     eventos
│   ├── <servicio>-v1-sNN-*-adenda.md   cambios por sprint
│   └── NN-contratos-inter-equipos.md   Gateway + qué pedimos/damos a cada equipo
├── epicas/
│   ├── README.md                       catálogo (tabla) + estado por sprint
│   └── ep-01.md … ep-NN.md             una ficha por épica (template Taiga)
├── historias/
│   ├── README.md                       índice de sprints
│   └── s01.md … sNN.md                 fichas de HU por sprint (lenguaje simple por defecto)
├── sprints/
│   ├── sprint-0.md                     acta de arranque
│   └── s01-propuesto.md …              propuesta de Planning por sprint (registro-sprint)
├── prototipos/
│   └── wireframes-*.md                 bocetos de baja fidelidad citados por las HU
├── plantillas/
│   ├── epica-taiga.md
│   ├── historia-de-usuario-taiga.md
│   └── registro-sprint.md
├── metodo/
│   ├── historias-de-usuario.md
│   ├── estilo-tecnico-historias.md
│   └── epicas.md
├── dor-dod.md
└── importado/                          material de otros equipos — histórico, no se modifica
```

Adaptá los **nombres** a lo que ya use el proyecto (algunos equipos numeran los docs:
`docs/23-plan-construccion.md` en vez de `docs/plan/plan-construccion.md`). Lo que **no**
cambia: todo bajo `docs/`, la jerarquía de capas de `docs/00`, la tabla de fuente única,
la separación fuente / vista derivada.

## Índice `docs/README.md` — plantilla

```markdown
# Documentación — <PROYECTO>

## Capas (qué fuente decide qué)

Ver `docs/00-fuentes-de-verdad-y-convenciones.md`. Resumen: producto → plataforma →
docs/00 + contratos + ADR → resto de docs/.

## Qué documento es fuente de qué

| Dato | Fuente única | Vistas derivadas |
|---|---|---|
| Precedencia e identidad canónica | docs/00-fuentes-de-verdad-y-convenciones.md | — |
| Visión y alcance | docs/vision/vision-y-alcance.md | — |
| Contratos | docs/contracts/*.yaml, docs/contracts/NN-contratos-inter-equipos.md | docs/historias/sNN.md |
| Capacidad del sprint | docs/plan/plan-construccion.md | docs/sprints/sNN-propuesto.md |
| Catálogo de épicas | docs/backlog/backlog-ejecutable.md | docs/epicas/*.md |
| Recetas de sprint | docs/backlog/backlog-ejecutable.md | docs/historias/sNN.md |
| DoR / DoD | docs/dor-dod.md | — |

## Cómo se llena

1. `scaffold-planificacion-agil` → estructura + docs/00 + material fijo
2. `generar-vision-y-alcance` → docs/vision/
3. `generar-contratos-servicio` → docs/contracts/
4. `generar-backlog-y-recetas` → docs/plan/ y docs/backlog/
5. `generar-epicas` → docs/epicas/
6. `generar-historias-usuario` → docs/historias/
```
