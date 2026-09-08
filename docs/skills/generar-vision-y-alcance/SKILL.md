---
name: generar-vision-y-alcance
description: >-
  Genera el documento de visión y alcance de un proyecto/servicio dentro de
  docs/vision/: el problema, el alcance dentro/fuera por fase, las funciones o
  capacidades, los objetivos medibles, los roles, las fronteras (qué construimos
  / qué se negocia / qué depende de otros) y los riesgos del recorte de alcance.
  Es la capa 1 del pipeline: su salida alimenta a generar-contratos-servicio y a
  generar-backlog-y-recetas. Úsalo cuando pidan "el alcance", "la visión", "qué
  entra y qué no", "las funciones del servicio" o "el documento de arranque".
---

# Generar visión y alcance

Convierte un **brief de producto / PRD / notas de arranque** en el documento de **visión
y alcance** (`docs/vision/vision-y-alcance.md`). Es la **capa 1** (ver
`docs/00-fuentes-de-verdad-y-convenciones.md`): lo que decide alcance funcional, fases y
reglas. El skill es **autónomo**.

Se **encadena** hacia abajo: `generar-contratos-servicio` toma las **funciones** y las
**fronteras**; `generar-backlog-y-recetas` toma las **fases** y los **objetivos** para
armar el catálogo de épicas y las recetas.

## Entradas que necesito (las pide el equipo al invocar)

Mínimo imprescindible:

1. **Brief / PRD / notas**: qué es el producto, para quién, qué problema resuelve.
2. **Fases o releases** con su alcance grueso (MVP / Fase 2 / Fase 3, o v1 / v2).
3. **De qué se hace cargo este equipo/servicio** y de qué no (aunque sea a grandes
   rasgos).

Opcional:

4. **Requisitos** `RF-*` / `PAR-*` para trazar cada función.
5. **Roles** de usuario del producto.
6. **Otros equipos** con los que se cruza y qué se les pide / se les da.
7. **Criterios de release / DoD de producto** del cliente.
8. **Objetivos medibles** (KPIs, umbrales) si ya existen.

Si falta 1, 2 o 3, **pídelos antes de generar**. No inventes funciones, fases, KPIs ni
fronteras: lo que no esté va como `*(a definir con el PO)*`.

## Método (paso a paso)

1. **El problema, primero.** 1–2 párrafos: qué duele hoy, para quién, y qué cambia si
   esto existe. Si el brief trae un hallazgo crítico (una dependencia que puede hundir el
   release), va arriba de todo.
2. **Alcance dentro / fuera, por fase.** Una tabla por fase: qué **entra** (con su
   `RF-*`) y qué queda **explícitamente afuera** (y para qué fase). «Explícitamente
   afuera» evita que alguien lo asuma incluido.
3. **Funciones / capacidades.** Una fila por función: nombre, qué hace, sync/async si
   aplica, requisitos que cubre, nota. Estas filas son la entrada de
   `generar-contratos-servicio` y del catálogo de épicas.
4. **Fronteras.** Tres columnas: **lo que construimos y decidimos** · **zona de
   negociación** (decidir con otro equipo: contrato, dueño de la cola, quién persiste,
   quién hace las pantallas) · **lo que depende de otros**. Es lo que más se malinterpreta
   entre equipos; nómbralo explícito.
5. **Objetivos medibles.** Qué tiene que ser cierto para decir «esto funciona»: KPIs,
   umbrales, criterios de release. Si el equipo no los dio, `*(a definir)*` — no los
   inventes.
6. **Roles.** Quién usa el producto y con qué objetivo (alimenta el COMO de las
   historias).
7. **Riesgos del recorte de alcance.** Los puntos donde cada equipo asume que lo hace el
   otro, con su mitigación y dueño.
8. **Glosario** de los términos del dominio (lo reusa la variante en lenguaje simple de
   las historias).
9. **Encabezado de documento fuente**: este documento **es la fuente** de alcance,
   funciones y fases; las épicas y las historias se le subordinan.
10. **Autocontrol** con la checklist.

## Salida

`docs/vision/vision-y-alcance.md` con: Problema · Alcance por fase (dentro/fuera) ·
Funciones/capacidades · Fronteras (construimos / negociamos / dependemos) · Objetivos
medibles · Roles · Riesgos del recorte · Glosario. Ver
[`references/plantilla-vision.md`](references/plantilla-vision.md).

## Reglas de oro

- El alcance **fuera** se escribe explícito, por fase. Lo no dicho se asume incluido.
- Funciones = qué hace para el usuario, no cómo se implementa.
- No inventes fases, KPIs, requisitos ni fronteras: `*(a definir con el PO)*`.
- Las fronteras entre equipos se nombran, no se dejan implícitas.
- Este documento no baja a diseño técnico (eso es arquitectura / contratos / ADR).

## Checklist antes de entregar

- [ ] Problema en 1–2 párrafos, con el hallazgo crítico arriba si lo hay.
- [ ] Alcance dentro/fuera **por fase**, con `RF-*` donde se pueda.
- [ ] Tabla de funciones/capacidades (nombre, qué hace, sync/async, requisitos).
- [ ] Fronteras en 3 columnas (construimos / negociamos / dependemos).
- [ ] Objetivos medibles (o marcados `*(a definir)*`).
- [ ] Roles con su objetivo.
- [ ] Riesgos del recorte con mitigación y dueño.
- [ ] Glosario del dominio.
- [ ] Encabezado de «documento fuente».
- [ ] Nada inventado.

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/plantilla-vision.md`](references/plantilla-vision.md) | Documento de visión y alcance en blanco. |
| [`references/ejemplo-vision.md`](references/ejemplo-vision.md) | Visión resuelta (extracto) con notas de por qué queda así. |
