---
name: generar-tareas
description: >-
  Desglosa una Historia de Usuario en sus Tareas técnicas, cada una en el formato
  oficial de Tarea de la Wiki de Taiga y redactada con el método SMART
  (Específica, Medible, Alcanzable, Relevante, Acotada en el tiempo), partida en
  una jornada efectiva o menos y trazada a un criterio de aceptación o escenario
  BDD de la historia padre. Úsalo cuando pidan "desglosar la historia en tareas",
  "las tareas SMART del sprint", "pasar los pasos técnicos a fichas de Taiga",
  "tareas de la HU Sxx-Hyy" o "el desglose de una historia en tareas".
---

# Generar tareas (formato Taiga, método SMART)

Convierte una **Historia de Usuario ya redactada** (con sus criterios de aceptación y sus
escenarios BDD) en el conjunto de **Tareas** técnicas que la construyen. El skill es
**autónomo y reutilizable**: no depende de la documentación de ningún proyecto. Todo el
contexto lo aporta quien lo invoca (ver «Entradas que necesito»).

Una **tarea** es un paso técnico interno del equipo de desarrollo. **No** se escribe en
`Como / Quiero / Para`, **no** se estima en puntos Fibonacci y **no** entrega valor
perceptible por sí sola: eso es de las historias. En Taiga la tarea **cuelga de su HU**
(o de la tarea de sprint, si el padre es un habilitador).

Lee primero, en este orden:

1. [`references/guia-metodo-smart.md`](references/guia-metodo-smart.md) — qué es SMART para
   tareas, la relación épica/historia/tarea y qué **no** es una tarea.
2. [`references/plantilla-tarea-taiga.md`](references/plantilla-tarea-taiga.md) — el template
   en blanco que hay que rellenar por tarea.
3. [`references/ejemplo-tareas.md`](references/ejemplo-tareas.md) — una historia resuelta en
   sus tareas SMART, como referencia de tono y nivel de detalle.

## Entradas que necesito (las pide el equipo al invocar)

Mínimo imprescindible:

1. **La Historia de Usuario**: su título, sus **criterios de aceptación** y sus **escenarios
   BDD** (sin CA/BDD no se puede trazar ni cerrar una tarea).
2. **Prefijo de ID**: cómo se numeran las tareas (`LLM-S01-H01-T01`, `S3-H12-T4`, …).
3. **Referencia de horas de la historia**: la columna *h* del plan / receta de sprint (las
   horas de las tareas **suman** esa referencia y no se inventan).

Opcional pero mejora el resultado:

4. **Orden de construcción** del equipo (si tiene uno). Por defecto: contrato y amenaza →
   dominio y migración → caso de uso → adaptadores → seguridad y resiliencia →
   observabilidad → prueba E2E → demo y evidencia.
5. **Contrato(s)** HTTP/evento que la historia toca: endpoints, esquema, auth, idempotencia.
6. **Dependencias** entre tareas o con otras historias.
7. **Convención de "una jornada"** propia del equipo si difiere de "≤ 1 jornada efectiva".

Si falta 1, 2 o 3, **pídelos antes de generar**. Si faltan los opcionales: generá igual,
usá el orden de construcción por defecto y marcá lo que no sepas como `*(a confirmar)*`.
**No inventes** contratos, endpoints ni horas.

## Método (paso a paso)

1. **Releer la historia.** Subrayá en los CA y en los escenarios BDD **cada cosa distinta
   que hay que construir o probar**: un endpoint, una validación, una regla de seguridad, un
   caso negativo, una migración, una prueba. Cada una es una tarea candidata.
2. **Agrupar por paso de construcción.** Ordená las candidatas según el orden de
   construcción (entrada 4). Fusioná las que son el mismo paso técnico; separá las que
   mezclan dos pasos (p. ej. "implementar y probar" → dos tareas si la prueba es sustancial).
3. **Una tarea = un paso ≤ 1 jornada.** Si una candidata no entra en una jornada efectiva,
   partila. Si dos candidatas juntas no llegan a media jornada y comparten contexto,
   fusionalas.
4. **Redactar cada tarea con la plantilla SMART** ([`plantilla-tarea-taiga.md`](references/plantilla-tarea-taiga.md)):
   - **Título**: verbo + resultado concreto. Identifica la tarea sin leer el detalle.
   - **Específica**: un solo paso técnico. Sin `Como/Quiero/Para`, sin nombres de clases.
   - **Medible**: el *criterio de terminado* se responde sí/no sin discusión.
   - **Alcanzable**: cabe en ≤ 1 jornada, para una persona o pareja.
   - **Relevante**: a qué **CA o escenario BDD** de la historia sirve (la trazabilidad).
   - **Acotada en el tiempo**: estimación en horas; se parte si supera la jornada.
   - **Pasos / alcance**: 2–5 sub-pasos, y qué queda **fuera** si hay riesgo de confusión.
   - **Criterio de terminado (Done)**: condición observable (prueba en verde, endpoint
     responde, migración corre, revisión aprueba…).
   - **Estimación y dependencias**: horas, paso del orden de construcción, `Depende de`
     (T## / otra historia / —), `Traza` (CA## y/o Escenario BDD).
5. **Cuadrar las horas.** La suma de las horas de las tareas = la referencia de la historia
   (entrada 3). Si no cierra, revisá el corte; **no** ajustes números a mano sin explicarlo.
6. **Cobertura.** Cada CA y cada escenario BDD de la historia queda cubierto por al menos una
   tarea (los negativos también: casi siempre son una tarea de seguridad o de validación).
7. **Encabezado + índice.** El documento abre con un blockquote "Qué es / método / fuente que
   manda" y, si agrupa varias historias, un índice `Historia · Tareas · h`. Cada historia es
   un `## Sxx-Hyy — Título` con un puntero a su ficha, y debajo sus tareas como `### T## —
   Título`.
8. **Autocontrol** con la checklist de abajo.

## Salida

Un documento por **agrupador** (típicamente por épica: `docs/tareas/ep-01.md`), o
`docs/tareas/sXX.md` si el equipo agrupa por sprint. Estructura:

```markdown
# Tareas SMART — <agrupador>

> Qué es · método SMART · fuente que manda (horas → plan; CA/BDD → ficha de la HU).

## Índice
| Historia | Tareas | h |
|---|---:|---:|
| [Sxx-Hyy](../historias/...#...) | N | H |

---

## Sxx-Hyy — <Título>
**Historia padre:** [../historias/...](...) · Pareja P_ · H h.

### T1 — <verbo + resultado>
- **Específica:** …
- **Medible:** …
- **Alcanzable:** … h, una persona/pareja.
- **Relevante:** … (a qué CA/escenario sirve)
- **Acotada:** … h · paso <orden de construcción>. **Depende de:** … · **Traza:** CA##, Escenario N.

> **Total Sxx-Hyy:** H h.
```

- Cada `### T##` sigue [`references/plantilla-tarea-taiga.md`](references/plantilla-tarea-taiga.md).
  Para un desglose compacto (varias historias en un archivo) se admite la variante de
  bullets de arriba; para cargar una tarea sola en Taiga se usa la plantilla completa.
- Regla de encabezado: el documento es **formato de presentación**, no fuente de verdad de
  planificación. Si una hora no coincide con el plan, **manda el plan**.

## Reglas de oro

- Una tarea = **un paso técnico ≤ 1 jornada efectiva**. Si necesita «y» / «además», son dos.
- **Sin `Como / Quiero / Para`**, sin puntos Fibonacci, sin prioridad MoSCoW: eso es de la HU.
- Cada tarea **traza** a un CA o escenario BDD de su historia. Una tarea que no traza a nada
  o sobra, o falta un criterio en la historia.
- El *criterio de terminado* se responde **sí/no** (Medible). «Avanzar el caso de uso» no; «el
  `POST` devuelve `201` con `Location` y la prueba de integración pasa» sí.
- Las **horas suman** la referencia de la historia y **no se inventan**.
- Los **casos negativos** de la historia se cubren con tareas explícitas (validación,
  seguridad, idempotencia), no se dan por incluidos en el camino feliz.

## Checklist antes de entregar

- [ ] Cada tarea tiene título de verbo + resultado, y las cinco letras SMART explícitas.
- [ ] Ninguna tarea usa `Como / Quiero / Para` ni trae puntos / MoSCoW / INVEST.
- [ ] Cada tarea entra en ≤ 1 jornada efectiva; las que no, están partidas.
- [ ] Cada tarea tiene *criterio de terminado* que se responde sí/no.
- [ ] Cada tarea traza a un CA## y/o Escenario BDD de la historia padre.
- [ ] Todos los CA y escenarios BDD de la historia (incluidos los negativos) están cubiertos.
- [ ] La suma de horas de las tareas = la referencia de la historia (o la diferencia está
      explicada).
- [ ] Las tareas están ordenadas por el paso de construcción (contrato → … → demo).
- [ ] `Depende de` está puesto donde hay orden obligatorio.
- [ ] Documento con encabezado (qué es / método / fuente) e índice si agrupa varias historias.

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/guia-metodo-smart.md`](references/guia-metodo-smart.md) | SMART para tareas condensado + épica/historia/tarea + qué NO es una tarea. |
| [`references/plantilla-tarea-taiga.md`](references/plantilla-tarea-taiga.md) | Template oficial de Tarea de Taiga en blanco. Copiar y completar. |
| [`references/ejemplo-tareas.md`](references/ejemplo-tareas.md) | Una historia resuelta en sus tareas SMART, como referencia de tono. |

## Relación con historias y épicas

Las historias que este skill desglosa las produce **`generar-historias-usuario`**; el
catálogo de épicas al que pertenecen, **`generar-epicas`**. Este skill es el último paso del
pipeline: épica → historia → **tarea**.
