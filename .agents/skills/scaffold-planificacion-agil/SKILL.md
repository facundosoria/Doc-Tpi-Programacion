---
name: scaffold-planificacion-agil
description: >-
  Deja lista la estructura de documentación de un proyecto dentro de docs/, con
  las mismas capas y convenciones probadas: el documento ancla de fuentes de
  verdad y precedencia (producto → plataforma → docs/00 + contratos + ADR →
  resto), el árbol de carpetas, el índice, la convención de encabezado de los
  documentos derivados, la disciplina de fuente única, y la copia del material
  fijo (templates de Épica y de Historia de Usuario de Taiga, guía de método,
  registro de sprint, starter de DoR/DoD). Úsalo al arrancar un proyecto nuevo o
  al ordenar la documentación de uno existente. No genera visión, contratos,
  backlog ni fichas: eso es de los otros skills del pipeline.
---

# Scaffold de documentación (estructura y capas)

Deja el **esqueleto de `docs/`** sobre el que después se generan visión, contratos,
backlog, épicas e historias. **Todo lo que produce este skill y los que le siguen vive
dentro de `docs/`.** El skill es **autónomo**: trae su propia copia del material fijo.

Lee [`references/estructura-y-convenciones.md`](references/estructura-y-convenciones.md)
antes de empezar: tiene las capas, el árbol completo y las plantillas de índice.

Se **encadena** con (cada uno también funciona solo):

1. `generar-vision-y-alcance` → `docs/vision/`
2. `contratos-api-gateway` (HTTP) + `contratos-kafka` (eventos) → `docs/contracts/`
3. `generar-backlog-y-recetas` → `docs/plan/` y `docs/backlog/`
4. `generar-epicas` → `docs/epicas/`
5. `generar-historias-usuario` → `docs/historias/`

## Las capas (lo que hace que «esté bien estructurado»)

El documento ancla **`docs/00-fuentes-de-verdad-y-convenciones.md`** fija el **orden de
precedencia**: qué fuente decide qué.

| Prioridad | Fuente | Decide |
|---|---|---|
| 1 | Documento(s) de producto / PRD | Alcance funcional, fases, reglas de negocio, release. |
| 2 | Documento(s) de plataforma / arquitectura transversal | Red, Gateway, discovery, identidad, rutas, observabilidad, integración. |
| 3 | `docs/00`, contratos v1, ADR | Aplicación concreta de 1 y 2 a este servicio. |
| 4 | Resto de `docs/` | Explicación y detalle. **No puede contradecir** a 1–3. |

Material de otros equipos → `docs/importado/`: histórico, **no participa de la jerarquía
y no se modifica**. `docs/00` fija además la **identidad canónica** (repo,
`spring.application.name`, prefijo `/api/<n>/**`, `aud`) que usan los demás skills.

## Qué deja montado (todo bajo `docs/`)

```
docs/
├── README.md                             índice + tabla de fuente única + resumen de capas
├── 00-fuentes-de-verdad-y-convenciones.md  capas (precedencia) + identidad canónica
├── vision/vision-y-alcance.md             STUB → generar-vision-y-alcance
├── plan/
│   ├── plan-construccion.md               STUB → generar-backlog-y-recetas
│   └── matriz-trazabilidad.md             STUB → historia → requisito
├── backlog/backlog-ejecutable.md          STUB → catálogo de épicas + Sprint 0 + recetas
├── contracts/
│   ├── README.md                          STUB → contratos-api-gateway + contratos-kafka
│   └── NN-contratos-inter-equipos.md      STUB
├── epicas/README.md                       STUB → catálogo; una ep-01.md… por épica
├── historias/README.md                    STUB → una sXX.md por sprint (lenguaje simple)
├── sprints/sprint-0.md                    STUB → acta de arranque
├── prototipos/                            (vacío) bocetos que citan las HU
├── plantillas/
│   ├── epica-taiga.md                     FIJO
│   ├── historia-de-usuario-taiga.md       FIJO (lenguaje simple)
│   └── registro-sprint.md                 FIJO
├── metodo/
│   ├── historias-de-usuario.md            FIJO
│   ├── estilo-tecnico-historias.md        FIJO (glosario simple↔técnico)
│   └── epicas.md                          FIJO
├── dor-dod.md                             STARTER (se ratifica en Sprint 0)
└── importado/                             material de otros equipos — no se modifica
```

Adaptá los **nombres** a lo que ya use el proyecto (algunos equipos numeran:
`docs/23-plan-construccion.md`). Lo que **no** cambia: todo bajo `docs/`, la jerarquía de
`docs/00`, la tabla de fuente única, la separación fuente / vista derivada.

## Entradas que necesito

Mínimo:

1. **Nombre del proyecto / producto** y, si aplica, **número de grupo** `GXX`.
2. **Identidad del servicio** para `docs/00`: nombre del servicio (`<n>-service`), prefijo
   público (`/api/<n>/**`), audiencia del token (`aud`). Si no la tenés, dejá el bloque
   marcado `*(a fijar con el equipo de plataforma)*`.

Opcional:

3. Fuentes de producto y de plataforma que ya existan (nombres de archivo / links) para
   la tabla de precedencia.
4. Convención de IDs internos (`LLM-Sxx-Hyy`, `S3-H12`, …).
5. Nombres de carpeta / numeración ya en uso que haya que mantener.

## Método (paso a paso)

1. **Crear el árbol** bajo `docs/`, respetando lo que ya exista. **No pisar** archivos
   con contenido: si un archivo ya existe, proponé merge, no overwrite.
2. **Escribir `docs/00-fuentes-de-verdad-y-convenciones.md`**: la tabla de precedencia
   (capa 1–4), la regla de `docs/importado/`, y la tabla de identidad canónica del
   servicio (lo que se sepa; el resto marcado a fijar).
3. **Copiar el material fijo** desde `references/` a `docs/plantillas/`, `docs/metodo/` y
   `docs/dor-dod.md`. Ajustá `GXX` en los templates de Taiga.
4. **Escribir los STUB** de `vision/`, `plan/`, `backlog/`, `contracts/`, `epicas/README`,
   `historias/README`, `sprints/sprint-0` con: (a) qué va acá, (b) qué skill lo genera,
   (c) el bloque de encabezado de convención. Sin inventar contenido.
5. **Escribir `docs/README.md`**: resumen de las capas (apunta a `docs/00`) + la tabla de
   **fuente única** (una fila por dato) + «cómo se llena» (orden de los skills). Plantilla
   en `references/estructura-y-convenciones.md`.
6. **Dejar por escrito la convención de encabezado** (abajo) en el README.
7. **Autocontrol** con la checklist.

## Convención de encabezado de los documentos derivados

Toda **vista derivada** (fichas de épica, fichas de HU, propuestas de sprint) abre con un
bloque `>`: **Qué es** (formato de presentación de X) · **Qué NO es** (no es fuente de
verdad) · **Fuente que manda** (tabla `Dato → Fuente única`). Los documentos **fuente**
(`docs/00`, visión, plan, backlog, contratos) abren declarando que *ellos* son la fuente.

## Reglas de oro

- **Todo bajo `docs/`.** Ningún skill del pipeline escribe fuera de `docs/`.
- **Un dato, un lugar.** Nadie repite el número/dato de otro documento.
- **Las capas mandan.** La capa 4 no contradice a la 1–3; `docs/importado/` no se toca.
- **Fijo se copia, generado se stubbea.** Este skill no escribe planificación real.
- **No pisar contenido existente** sin proponer merge.

## Checklist antes de entregar

- [ ] Todo el árbol bajo `docs/`, sin pisar archivos con contenido.
- [ ] `docs/00-fuentes-de-verdad-y-convenciones.md` con la tabla de precedencia (4 capas),
      la regla de `docs/importado/` y la identidad canónica del servicio.
- [ ] `docs/plantillas/`, `docs/metodo/` y `docs/dor-dod.md` con el material fijo; `GXX`
      puesto.
- [ ] STUBs de vision/plan/backlog/contracts/epicas/historias/sprints con «qué va acá +
      qué skill lo genera».
- [ ] `docs/README.md` con resumen de capas + tabla de fuente única + orden de skills.
- [ ] Convención de encabezado escrita.
- [ ] Ningún STUB contiene planificación inventada (capacidad, épicas, recetas, contratos).

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/estructura-y-convenciones.md`](references/estructura-y-convenciones.md) | Capas (precedencia), árbol de `docs/`, tabla de fuente única, convención de encabezado, trazabilidad, plantilla de `docs/README.md`. |
| [`references/plantilla-epica.md`](references/plantilla-epica.md) | Template oficial de Épica de Taiga. FIJO. |
| [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md) | Template de Historia de Usuario de Taiga, lenguaje simple. FIJO. |
| [`references/estilo-tecnico-historias.md`](references/estilo-tecnico-historias.md) | Glosario de traducción simple↔técnico para las HU. FIJO. |
| [`references/plantilla-registro-sprint.md`](references/plantilla-registro-sprint.md) | Registro de sprint (Planning / ejecución / cierre), parametrizable. FIJO. |
| [`references/metodo-historias-de-usuario.md`](references/metodo-historias-de-usuario.md) | Método de cátedra condensado para HU. FIJO. |
| [`references/metodo-epicas.md`](references/metodo-epicas.md) | Método para épicas. FIJO. |
| [`references/dor-dod-starter.md`](references/dor-dod-starter.md) | DoR y DoD de arranque; se ratifican en Sprint 0. |
