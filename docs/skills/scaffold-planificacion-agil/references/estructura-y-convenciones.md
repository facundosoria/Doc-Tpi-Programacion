# Estructura de documentación y convenciones

Lo que hace que esta documentación «esté bien estructurada» no es el árbol de carpetas:
son **cuatro disciplinas**. El árbol solo las hace visibles.

## 1. Fuente única (un dato, un lugar)

Cada dato de planificación vive en **exactamente un** documento. Todo lo demás lo
**referencia**, no lo copia. El `README.md` índice lleva esta tabla:

| Dato | Fuente única |
|---|---|
| Cálculo de capacidad del sprint (fórmula y números) | `plan/plan-construccion.md` |
| Catálogo de épicas (nombre, resultado, responsable, sprints, requisitos) | `backlog/backlog-ejecutable.md` |
| Recetas de sprint (paquetes verificables, horas, gates) | `backlog/backlog-ejecutable.md` |
| Esquema de IDs internos (`Sxx-Hyy`) | `plan/plan-construccion.md` |
| Definition of Ready / Definition of Done | `dor-dod.md` |
| Método para redactar y estimar HU | `metodo/historias-de-usuario.md` |
| Método para épicas | `metodo/epicas.md` |
| Contratos HTTP / evento entre servicios | `contracts/` (o donde el proyecto los tenga) |
| Matriz de trazabilidad historia → requisito | `plan/` (matriz dedicada) |

Si dos documentos dicen algo distinto sobre el mismo dato, **manda la fuente única** y el
otro está mal y se corrige.

## 2. Fuente de verdad vs. formato de presentación

- **Documentos fuente**: el plan y el backlog ejecutable. Abren declarando que *ellos
  son la fuente* de capacidad / épicas / recetas.
- **Vistas derivadas**: fichas de épica (`epicas/ep-01.md`…), fichas de HU
  (`historias/sXX.md`), propuestas de sprint. Son **formato de presentación** para
  cargar en Taiga o presentar a cátedra. No son fuente de planificación.

Toda vista derivada abre con este bloque:

```markdown
> **Qué es.** [Formato de presentación de <X> con el template oficial de Taiga.]
>
> **Qué NO es.** No es fuente de verdad de planificación. Si un dato de acá no coincide:
>
> | Dato | Fuente única |
> |---|---|
> | ID, épica, pareja, dependencias, horas | `backlog/backlog-ejecutable.md` |
> | DoR / DoD | `dor-dod.md` |
> | Método para redactar y estimar | `metodo/historias-de-usuario.md` |
```

## 3. Trazabilidad

Cada historia `Sxx-Hyy` enlaza hacia arriba: **historia → épica** (para qué sirve) y
**historia → requisito** (`RF-*` / `PAR-*`, qué obligación cubre). El sprint dice
*cuándo*. Ninguna historia queda sin épica ni sin requisito trazable (o marcado
`*(a trazar)*` si aún no está).

## 4. Encabezado honesto

Ningún documento afirma más de lo que tiene. Un STUB dice «STUB — lo genera el skill X».
Una capacidad de referencia dice «referencia, no acredita horas reales». Una historia sin
puntos dice «*(a asignar en Sprint 0)*», no un número inventado.

---

## Árbol de referencia

```
docs/
├── README.md                      índice + tabla de fuente única
├── plan/
│   ├── plan-construccion.md        capacidad, parejas/equipos, esquema de IDs, DoR/DoD (o puntero)
│   └── matriz-trazabilidad.md      historia → requisito
├── backlog/
│   └── backlog-ejecutable.md       catálogo de épicas + Sprint 0 + recetas S1..SN
├── epicas/
│   ├── README.md                   catálogo (tabla) + estado por sprint
│   └── ep-01.md … ep-NN.md         una ficha por épica (template Taiga)
├── historias/
│   ├── README.md                   índice de sprints
│   └── s01.md … sNN.md             fichas largas de HU por sprint
├── sprints/
│   ├── sprint-0.md                 acta de arranque
│   └── s01-propuesto.md …          propuesta de Planning por sprint (registro-sprint)
├── plantillas/
│   ├── epica-taiga.md
│   ├── historia-de-usuario-taiga.md
│   └── registro-sprint.md
├── metodo/
│   ├── historias-de-usuario.md
│   └── epicas.md
├── dor-dod.md
└── contracts/                      contratos entre servicios (si aplica)
```

Adaptá nombres a lo que ya use el proyecto. Lo que **no** cambia: la tabla de fuente
única, la separación fuente / vista derivada, y el bloque de encabezado.

## Índice `README.md` — plantilla

```markdown
# Documentación de planificación — <PROYECTO>

## Qué documento es fuente de qué

| Dato | Fuente única | Vistas derivadas |
|---|---|---|
| Capacidad del sprint | plan/plan-construccion.md | sprints/sNN-propuesto.md |
| Catálogo de épicas | backlog/backlog-ejecutable.md | epicas/*.md |
| Recetas de sprint | backlog/backlog-ejecutable.md | historias/sNN.md |
| DoR / DoD | dor-dod.md | — |
| Método HU / épicas | metodo/*.md | — |

## Cómo se llena

1. `generar-backlog-y-recetas` → plan/ y backlog/
2. `generar-epicas` → epicas/
3. `generar-historias-usuario` → historias/
```
