---
name: generar-backlog-y-recetas
description: >-
  Genera el plan de construcción y el backlog ejecutable de un proyecto: cálculo
  de capacidad del sprint (fórmula disponibilidad − reuniones − reserva),
  catálogo de épicas (EP-01…EP-NN con resultado que habilita, responsable,
  sprints, requisitos) y una receta por sprint (objetivo, recorrido de demo,
  paquetes verificables con horas, gates, aceptación negativa). Su salida es la
  entrada de los skills generar-epicas y generar-historias-usuario. Úsalo cuando
  pidan "armar el backlog", "las recetas de sprint", "el plan de construcción",
  "el catálogo de épicas" o "cuánta capacidad tiene el sprint".
---

# Generar backlog ejecutable y recetas de sprint

Convierte un **brief de producto + requisitos + datos del equipo** en el **plan** y el
**backlog ejecutable** (catálogo de épicas + Sprint 0 + recetas S1..SN). El skill es
**autónomo**: trae su propia copia del cálculo de capacidad y del formato de receta.

Se **encadena** hacia abajo con:

- `generar-epicas` → toma el **catálogo de épicas** que produce este skill.
- `generar-historias-usuario` → toma **cada receta de sprint** que produce este skill.

Y hacia arriba con `scaffold-planificacion-agil`, que deja los STUB de `plan/` y
`backlog/` esperando este contenido.

## Entradas que necesito (las pide el equipo al invocar)

Mínimo imprescindible:

1. **Brief del producto**: qué es, para quién, y las **fases / releases** con su alcance
   grueso.
2. **Requisitos** `RF-*` / `PAR-*` / RNF, aunque sea una lista sin depurar.
3. **Datos del equipo para capacidad**: nº de integrantes, h-persona/semana declaradas
   (o planilla de disponibilidad), semanas por sprint, y el esquema de reuniones del
   ciclo (cuáles, cuántas, duración, participantes).
4. **Cantidad de sprints** o el calendario (fecha de inicio + duración de fases).

Opcional pero mejora el resultado:

5. **Modelo de equipos/parejas** (P1…P5, squads, …) y quién lidera qué.
6. **Arquitectura / servicios** involucrados y sus contratos.
7. **Dependencias externas** conocidas (otros equipos, datos, infraestructura).
8. **Historia canónica** candidata para anclar la estimación en el futuro.
9. **Convención de IDs** (`LLM-Sxx-Hyy`, `S3-H12`, …) y prefijo de épica.

Si falta 1, 2, 3 o 4, **pídelos antes de generar**. No inventes requisitos, capacidad ni
alcance de fase.

## Método (paso a paso)

### A. Cálculo de capacidad

Seguí [`references/calculo-capacidad.md`](references/calculo-capacidad.md):

```
nominal      = h-persona/semana declaradas × semanas por sprint
reuniones    = Σ(cantidad × duración × participantes internos)  del esquema del equipo
base         = nominal − reuniones − soporte conocido
reserva      = 20 % de la base
capacidad    = máximo(0, base × 0,80)      ← capacidad comprometible del sprint
```

- El resultado es **en horas**. Los **puntos Fibonacci** son otra escala y **no se
  convierten**.
- La capacidad es un **techo de compromiso**, no un presupuesto a llenar.
- Documentá la fórmula y la regla de **recalcular en cada Planning** con la planilla
  real; los números iniciales son referencia.

### B. Catálogo de épicas

1. De las **fases y el alcance** del brief, derivá los **grandes resultados de
   producto**. Regla de corte: si dos candidatas cierran con la misma evidencia, son
   una. Apuntá a 6–12 épicas.
2. Para cada una: `EP-0X` · nombre · **resultado que habilita** (frase de valor, será el
   *Objetivo* en Taiga) · equipo/pareja responsable · sprints en que vive · requisitos
   que cubre (orientativo, citando `RF-*`).
3. Dejá escrito que las épicas **no se estiman ni se comprometen**; se cierran cuando
   todas sus historias pasan la DoD.
4. Esta tabla es **fuente**: las fichas de `generar-epicas` se le subordinan.

### C. Sprint 0

Redactá el checklist de arranque (no produce incremento): disponibilidad declarada,
nombres de equipos/roles, cálculo de capacidad de S1, DoR/DoD ratificadas, historia
canónica elegida y estimada, épicas revisadas y mapeadas, entorno y dependencias con
dueño y fecha, repos y ramas protegidas, carpeta de evidencia.

### D. Recetas de sprint (S1..SN)

Una por sprint, con el formato de
[`references/plantilla-receta-sprint.md`](references/plantilla-receta-sprint.md):

1. **Objetivo del sprint** y **usuario beneficiado** (rol real).
2. **Recorrido funcional de la demo**: los pasos concretos que se mostrarán en la Review.
3. **Límites del incremento**: qué queda explícitamente afuera (y para qué sprint es).
4. **No iniciar sin**: precondiciones y dependencias con dueño.
5. **Paquetes verificables** en orden de construcción, cada uno con: nombre, horas
   estimadas (trabajo de dev+pruebas+revisión+doc+demo, **no** reuniones), y salida /
   prueba que lo hace verificable. La suma es el **trabajo estimado del sprint** (un
   **piso**, no la capacidad).
6. **Gates**: reglas que no se saltan (no copiar código de tal lado, congelar campos de
   contrato antes de publicar, …).
7. **Aceptación negativa**: qué NO debe pasar (token sin rol, clave repetida, dato
   inválido, caída) — es la semilla de los escenarios negativos de las historias.
8. **Historias derivadas** (opcional): una línea por historia candidata (ID, título,
   tipo HU/tarea, épica, pareja, dep., h) — el desglose fino lo hace
   `generar-historias-usuario` en el Refinamiento.

### E. Ensamblado

- `docs/plan/plan-construccion.md`: capacidad (A) + modelo de equipos + esquema de IDs +
  puntero a `docs/dor-dod.md`.
- `docs/backlog/backlog-ejecutable.md`: catálogo de épicas (B) + Sprint 0 (C) + recetas
  (D).
- Todo bajo `docs/`. Respetá la numeración si el proyecto la usa
  (`docs/23-plan-construccion.md`).
- Encabezado de cada uno: **este documento es la fuente** de tal cosa.
- Autocontrol con la checklist.

## Reglas de oro

- **Capacidad ≠ trabajo estimado.** La receta suma un piso; la diferencia con la
  capacidad es margen explícito. No reescales los paquetes para «llenar» la capacidad.
- **Horas y puntos no se convierten.**
- **Las épicas no se estiman.**
- Las recetas se **derivan de requisitos y contratos**, no de imaginar features.
- Lo que el equipo no dio (fase, requisito, dependencia) se marca `*(a definir)*`.
- Los sprints lejanos se dejan **gruesos**; se refinan al acercarse.
- No pre-cargues puntos Fibonacci en las historias derivadas: se asignan en Sprint 0.

## Checklist antes de entregar

- [ ] Capacidad con fórmula visible, números de referencia y regla de recálculo.
- [ ] Aclaración horas vs puntos (no se convierten).
- [ ] Catálogo de épicas: 6–12, cada una con resultado de valor, responsable, sprints y
      requisitos; nota de que no se estiman.
- [ ] Sprint 0 con checklist de salida (incluye elegir historia canónica).
- [ ] Una receta por sprint con objetivo, recorrido de demo, límites, precondiciones,
      paquetes con horas y prueba, gates y aceptación negativa.
- [ ] Suma de paquetes marcada como **piso**, comparada con la capacidad.
- [ ] Cada épica y cada receta trazan a `RF-*` / `PAR-*` (o marcado `*(a trazar)*`).
- [ ] `plan/` y `backlog/` con encabezado de «documento fuente».
- [ ] Nada inventado: fases, requisitos y dependencias vienen de las entradas.

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/calculo-capacidad.md`](references/calculo-capacidad.md) | Fórmula de capacidad paso a paso + ejemplo resuelto + errores comunes. |
| [`references/plantilla-receta-sprint.md`](references/plantilla-receta-sprint.md) | Formato de una receta de sprint en blanco. |
| [`references/ejemplo-backlog.md`](references/ejemplo-backlog.md) | Catálogo de épicas + receta de S1 resueltos, con notas de por qué quedan así. |
