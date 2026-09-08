---
name: scaffold-planificacion-agil
description: >-
  Deja lista la estructura de documentación de planificación ágil de un proyecto
  con las mismas convenciones probadas: árbol de carpetas, índice, convención de
  encabezado de los documentos derivados ("qué es / qué no es / fuente que
  manda"), disciplina de fuente única, y copia del material fijo (templates de
  Épica y de Historia de Usuario de Taiga, guía de método, registro de sprint,
  starter de DoR/DoD). Úsalo al arrancar un proyecto nuevo o al ordenar la
  documentación de uno existente. No genera el backlog ni las fichas: eso es de
  los skills generar-backlog-y-recetas, generar-epicas y generar-historias-usuario.
---

# Scaffold de planificación ágil

Deja el **esqueleto de documentación** sobre el que después se generan el backlog, las
épicas y las historias. El skill es **autónomo**: trae su propia copia del material fijo.

Se puede **encadenar** con:

1. `generar-backlog-y-recetas` → llena `plan/` y `backlog/` (capacidad, catálogo de
   épicas, recetas de sprint).
2. `generar-epicas` → llena `epicas/` a partir del catálogo.
3. `generar-historias-usuario` → llena `historias/` a partir de cada receta de sprint.

Cada uno funciona también por separado.

## Qué deja montado

```
docs/  (o la raíz de doc que use el proyecto)
├── README.md                      índice: qué documento es fuente de qué
├── plan/
│   └── plan-construccion.md       STUB → lo genera generar-backlog-y-recetas
├── backlog/
│   └── backlog-ejecutable.md      STUB → catálogo de épicas + recetas S1..SN
├── epicas/
│   └── README.md                  STUB → catálogo; una ficha ep-01.md… por épica
├── historias/
│   └── README.md                  STUB → una ficha sXX.md por sprint
├── plantillas/
│   ├── epica-taiga.md             FIJO (copiado por este skill)
│   ├── historia-de-usuario-taiga.md   FIJO
│   └── registro-sprint.md         FIJO
├── metodo/
│   ├── historias-de-usuario.md    FIJO (método de cátedra condensado)
│   └── epicas.md                  FIJO
└── dor-dod.md                     STARTER (el equipo lo ratifica en Sprint 0)
```

Adaptá los nombres de carpeta a los que ya use el proyecto; lo que no cambia es **qué es
fuente de qué** y la **convención de encabezado**.

## Entradas que necesito

Mínimo:

1. **Raíz de documentación** del proyecto (`docs/`, `Plan de ejecucion/`, …) y si ya
   existe algo que haya que respetar.
2. **Nombre del proyecto / producto** y, si aplica, **número de grupo** `GXX` para los
   títulos de Taiga.

Opcional:

3. Convención de IDs internos (`LLM-Sxx-Hyy`, `S3-H12`, …).
4. Nombres de carpeta ya en uso que haya que mantener.

## Método (paso a paso)

1. **Crear el árbol** de carpetas de arriba, respetando lo que ya exista. No pisar
   archivos con contenido: si un archivo ya existe, proponé merge, no overwrite.
2. **Copiar el material fijo** desde `references/` de este skill a `plantillas/`,
   `metodo/` y `dor-dod.md`. Ajustá `GXX` en los templates de Taiga.
3. **Escribir los STUB** de `plan/`, `backlog/`, `epicas/README.md`,
   `historias/README.md` con: (a) una línea de qué va acá, (b) qué skill lo genera,
   (c) el encabezado de convención (abajo). Sin inventar contenido de planificación.
4. **Escribir el `README.md` índice** con la tabla de **fuente única**: para cada dato
   (capacidad, catálogo de épicas, IDs, DoR/DoD, método, contratos) una sola fila que
   diga **dónde vive**. Ver [`references/estructura-y-convenciones.md`](references/estructura-y-convenciones.md).
5. **Dejar por escrito la convención de encabezado** (ver abajo) en el README y como
   plantilla reutilizable.
6. **Autocontrol** con la checklist.

## Convención de encabezado de los documentos derivados

Todo documento que sea **vista derivada** (fichas de épica, fichas de HU, propuestas de
sprint) abre con un bloque `>` que dice:

- **Qué es** este documento (formato de presentación de X).
- **Qué NO es** (no es fuente de verdad de planificación).
- **Fuente que manda**: una tabla `Dato → Fuente única`. Si un valor no coincide, manda
  la fuente.

Los documentos **fuente** (plan, backlog ejecutable) abren diciendo que **ellos son la
fuente** de tal cosa y que las vistas derivadas se les subordinan.

## Reglas de oro

- **Un dato, un lugar.** Capacidad en el plan; catálogo de épicas en el backlog; método
  en `metodo/`; DoR/DoD en `dor-dod.md`. Nadie repite el número de otro.
- **Fijo se copia, generado se stubbea.** Este skill no escribe planificación real.
- **No pisar contenido existente** sin proponer merge.
- Los templates de Taiga y el método son **material fijo**: no se "mejoran" acá, se
  copian tal cual.

## Checklist antes de entregar

- [ ] Árbol creado sin pisar archivos con contenido.
- [ ] `plantillas/`, `metodo/` y `dor-dod.md` con el material fijo copiado; `GXX` puesto.
- [ ] STUBs de plan/backlog/epicas/historias con «qué va acá + qué skill lo genera».
- [ ] `README.md` índice con la tabla de fuente única (una fila por dato).
- [ ] Convención de encabezado escrita y con plantilla reutilizable.
- [ ] Ningún STUB contiene planificación inventada (capacidad, épicas, recetas).

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/estructura-y-convenciones.md`](references/estructura-y-convenciones.md) | Árbol, índice de fuente única, convención de encabezado, disciplina de trazabilidad. |
| [`references/plantilla-epica.md`](references/plantilla-epica.md) | Template oficial de Épica de Taiga. FIJO. |
| [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md) | Template oficial de Historia de Usuario de Taiga. FIJO. |
| [`references/plantilla-registro-sprint.md`](references/plantilla-registro-sprint.md) | Registro de sprint (Planning / ejecución / cierre), parametrizable. FIJO. |
| [`references/metodo-historias-de-usuario.md`](references/metodo-historias-de-usuario.md) | Método de cátedra condensado para HU. FIJO. |
| [`references/metodo-epicas.md`](references/metodo-epicas.md) | Método para épicas (qué es / qué no / CA a nivel épico). FIJO. |
| [`references/dor-dod-starter.md`](references/dor-dod-starter.md) | DoR y DoD de arranque; el equipo las ratifica en Sprint 0. |
