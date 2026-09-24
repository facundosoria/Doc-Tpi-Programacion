# Backlog priorizado — Sprint 1 (Hilos A–F), en dos vistas

> **Qué es.** Los 28 ítems comprometidos de los Hilos A–F de S1 (ver
> [`s1-historias.md`](s1-historias.md)), ordenados dos veces: como los ordenaría un **Product
> Owner** (qué importa primero para que la demo de S1 se pueda *mostrar*) y como los ordena el
> **equipo** (qué hay que resolver primero porque técnicamente lo de abajo depende de eso). Son
> la misma lista — cambia el criterio de orden, no el contenido. Mismo formato que
> [`backlog-priorizado-cierre-s1.md`](../../../../01-vision-alcance-y-entrega/03-entregas/backlog-priorizado-cierre-s1.md), aplicado
> acá al sprint completo en vez de solo al cierre de huecos.
>
> **Qué NO cubre.** El **Hilo G** (extensión de EP-09, ingesta visual) queda **afuera** de las
> dos vistas — no tiene ficha de historia todavía, así que no hay nada que priorizar (ver
> [`s1-historias.md`](s1-historias.md) · Hilo G). Cuando se redacte su ficha en la Planning,
> agregarlo acá en el lugar que le corresponda por dependencia (depende de `EP09-H01`/`H02`, que
> sí están priorizadas abajo).
>
> **Fuente de los ítems, horas y dependencias:** [`s1-historias.md`](s1-historias.md) · "Índice
> Consolidado por Hilos". Si una hora o una dependencia cambia ahí, esta vista queda
> desactualizada hasta que se reordene a mano — no se recalcula solo.

---

## Vista 1 — Product Owner: orden por valor de demo

Qué tan cerca pone cada ítem al recorrido de demo de S1 descripto en [`README.md`](README.md):
*"un docente crea un golden set, lo calibra, carga un PDF de material, y consulta el tutor. El
tutor responde con citas del PDF. Un intento de jailbreak se bloquea. Todo con el proveedor real
de IA."* — más lo que aporta el Hilo F si avanza a tiempo.

### Tier 1 — Corazón del recorrido de demo (lo que se muestra en vivo)

|   # | Ítem                                                               | Hilo | Por qué este lugar                                                                              |                  h |
| --: | ------------------------------------------------------------------ | ---- | ----------------------------------------------------------------------------------------------- | -----------------: |
|   1 | **Rúbrica versionada por curso** (`EP03-H04`)                      | E    | Sin rúbrica publicada no hay contra qué calibrar ni evaluar — su propia ficha la marca **Must** |      *(a estimar)* |
|   2 | **Golden set versionado por curso** (`EP03-H05`)                   | E    | Insumo directo de la calibración — también **Must** en su ficha                                 |      *(a estimar)* |
|   3 | **Alta de golden set y carga de entradas** (`EP03-H01`)            | E    | Es la acción central del guion: "el docente crea su golden set"                                 |                 24 |
|   4 | **Consulta del golden set persistente tras reinicio** (`EP03-H02`) | E    | La historia **canónica** de todo S1 — es literalmente el criterio de aceptación de la demo      |                 14 |
|   5 | **Pantalla docente mínima del golden set** (`EP03-H03`)            | E    | Sin pantalla no hay nada que mostrar en vivo                                                    |                 24 |
|   6 | **Calibración de curso con PAR-14** (`EP04-H01`)                   | D    | "El docente lo calibra" — segundo paso explícito del guion de demo                              |                 46 |
|   7 | **Ingesta e indexado de un PDF** (`EP09-H01`)                      | C    | "Carga un PDF de material" — tercer paso del guion                                              |    *(sin asignar)* |
|   8 | **Consulta al tutor con citas de fuente/página** (`EP09-H02`)      | C    | "El tutor responde con citas del PDF" — cuarto paso, literal en el guion                        | *(~20–26 con H01)* |
|   9 | **Interacción socrática del tutor** (`EP05-H01`)                   | C    | "Consulta al tutor" — quinto paso del guion                                                     |                 28 |
|  10 | **Guardarraíles anti-jailbreak** (`EP05-H02`)                      | C    | "Un intento de jailbreak se bloquea" — último paso explícito del guion                          |                 26 |
|  11 | **Proveedor real de IA (Groq/LangChain4j)** (`EP02-H02`)           | B    | "Todo con el proveedor real de IA" es una condición explícita de la demo, no un detalle técnico |                 20 |

### Tier 2 — Sostiene la demo pero no se ve en pantalla (habilitadores obligatorios)

|   # | Ítem                                                      | Hilo | Por qué este lugar                                                                                                              |   h |
| --: | --------------------------------------------------------- | ---- | ------------------------------------------------------------------------------------------------------------------------------- | --: |
|  12 | **Puerto de modelos y fake** (`EP02-H01`)                 | B    | Toda la IA de arriba (calibración, tutor) invoca este puerto — invisible, pero nada de Tier 1 funciona sin él                   |  32 |
|  13 | **Esquema inicial versionado con auditoría** (`EP01-H04`) | A    | Toda la persistencia de golden set/rúbrica de Tier 1 corre sobre este esquema                                                   |  38 |
|  14 | **Esqueleto transversal del servicio** (`EP01-H03`)       | A    | Condición de la propia definición de demo: corre en **ambiente integrado** (Gateway/Eureka/Security), no en una máquina aislada |  34 |
|  15 | **Entorno reproducible con un comando** (`EP01-H02`)      | A    | Sin esto nadie levanta el ambiente donde se hace la demo                                                                        |  30 |

### Tier 3 — Complementa la demo si hay tiempo (Hilo F, y UX secundaria)

| # | Ítem | Hilo | Por qué este lugar | h |
|---:|---|---|---|---:|
| 16 | **Panel de costos, cuotas y fallas** (`EP07-H01`, ex `S09-H01`) | F | El propio guion de demo dice *"si F avanza, el panel muestra costo y cuotas"* | 36 |
| 17 | **Retomar el hilo sin perder contexto** (`EP05-H03`) | C | Mejora de UX del tutor — no está en el guion base de la demo | 20–25 |
| 18 | **Panel docente/admin de calibración** (`EP04-H04`) | D | Visibilidad extra de calibración, no crítica para el guion mínimo | 15–20 |
| 19 | **Puntaje explicado en las cinco dimensiones** (`EP06-H02`) | E | Su ficha lo llama "el corazón del producto", pero el guion de demo de S1 no incluye evaluar entregas todavía | *(a estimar)* |
| 20 | **Calibración a nivel plataforma** (`EP04-H02`) | D | Cierra un hueco de la épica; el nivel curso (Tier 1) ya calibra sin depender de esto | *(a estimar)* |
| 21 | **Vencimiento y ciclo de vida de calibración** (`EP04-H03`) | D | Gobernanza — no se ve en una demo en vivo de un solo recorrido | 12–15 |
| 22 | **Límite de cuota versionado y auditado** (`EP07-H02`, ex `S09-H02`) | F | Gobernanza operativa del Hilo F, menos vistosa que el panel (#16) | 28 |
| 23 | **Evaluador caído no bloquea la entrega** (`EP06-H03`) | E | Resiliencia — solo se nota si algo falla durante la demo, no en el camino feliz | 15 |

### Tier 4 — Invisible en la demo, importa para la nota de proceso/calidad

|   # | Ítem                                                                 | Hilo | Por qué este lugar                                                                                                              |   h |
| --: | -------------------------------------------------------------------- | ---- | ------------------------------------------------------------------------------------------------------------------------------- | --: |
|  24 | **ADR de arquitectura y convenciones** (`EP01-H01`)                  | A    | Documentación de proceso — igual que en el cierre de S1, un PO de producto lo prioriza último aunque a la cátedra sí le importe |  16 |
|  25 | **Contrato OpenAPI y mock del golden set** (`EP01-H05`)              | A    | Sirve a `admin-service` para avanzar en paralelo, no a quien mira la demo                                                       |  10 |
|  26 | **Suite de pruebas y guía de demo** (`EP01-H06`)                     | A    | Es la evidencia de que todo lo de arriba funciona — se escribe último porque necesita que el resto ya exista                    |  18 |
|  27 | **Consumo asíncrono de eventos de entrega** (`EP06-H01`)             | E    | Plomería backend pura, sin nada visible en pantalla                                                                             |  24 |
|  28 | **429 y Retry-After en rutas con límite** (`EP07-H03`, ex `S09-H03`) | F    | El detalle técnico menos vistoso del Hilo F                                                                                     |  22 |

> **Nota importante — la misma que en el cierre de S1.** Esta vista asume un PO que solo mira "¿el
> recorrido de la demo funciona?". **La cátedra de este TP no es ese PO** — exige ADR, CI y DoD
> como parte de lo evaluable, no como adorno (ver
> [`00-fuentes-de-verdad-y-convenciones.md`](../../../../00-gobierno-y-evolucion/01-fuentes-de-verdad-y-convenciones.md)). Esta
> vista sirve para decidir **qué mostrar primero si el tiempo aprieta**, no para decidir qué
> **saltear** del sprint.

---

## Vista 2 — Equipo: orden por dependencia técnica real

Qué hay que resolver antes de qué, sin importar cuánto "se note" en la demo. Es la misma
información de la columna **Dep.** de [`s1-historias.md`](s1-historias.md), leída como secuencia
por hilo más las compuertas cruzadas entre hilos.

```
Hilo A (P1) — columna vertebral, todo lo demás cuelga de acá:
  H01 ADR (16h, sin dependencias — en paralelo con el resto)
    │
    └─→ H02 Entorno reproducible (30h) ─→ H03 Esqueleto transversal (34h) ─→ H04 Esquema (38h)
                                                    │                              │
                                                    └─→ H05 Contrato+mock (10h)    │
                                                                                   │
        H06 Suite de pruebas + guía de demo (18h) ←── necesita H04 y toda EP-03 ───┘
        [va al final: es la evidencia de que el resto ya funciona]

Hilo B (P2) — depende del ADR de Hilo A, después es propio:
  H01 Puerto de modelos + fake (32h, dep. EP01·H01) ─→ H02 Proveedor real Groq (20h)

Hilo C (P3) — dos cadenas en paralelo, ambas gatilladas por Hilo B:
  EP05-H01 Tutor socrático (28h, dep. EP02·H01) ─→ EP05-H02 Guardarraíles (26h) ─→ EP05-H03 (20–25h)
  EP09-H01 Ingesta PDF (dep. EP02·H01) ─→ EP09-H02 Consulta con citas (dep. EP09-H01)

Hilo D (P4) — la calibración de curso es la más restringida de todo el sprint:
  EP04-H01 Calibración de curso (46h) ─ dep. EP02·H01 **y** EP03-H04/H05 (Hilo E)
      → no puede arrancar en serio hasta que Hilo B y el arranque de Hilo E existan
  EP04-H02 Calibración de plataforma (dep. solo EP02·H01) ─→ EP04-H03 (12–15h) y EP04-H04 (15–20h)
      → esta cadena SÍ puede ir en paralelo a EP04-H01, no depende de Hilo E

Hilo E (P5) — la rúbrica y el golden set versionado son la base de todo Hilo D y parte de C:
  EP03-H04 Rúbrica (dep. EP01·H04) ─→ EP03-H05 Golden set versionado (dep. EP01·H04 + H04)
  EP03-H01 Alta de golden set (dep. EP01·H04) ─→ EP03-H02 Consulta persistente ─→ EP03-H03 Pantalla
  EP06-H01 Consumo async (dep. EP01·H01/H04) ─→ EP06-H02 Puntaje explicado (dep. H01 + EP03-H04) ─→ EP06-H03

Hilo F (P1+P2) — depende de que Hilo A ya tenga el borde de seguridad:
  EP07-H01 Panel de costos (36h, dep. EP01·H03) ─→ EP07-H02 Límite de cuota (28h) ─→ EP07-H03 429/Retry-After (22h)
```

**Por qué este orden y no otro:**

1. **El ADR no bloquea nada técnicamente, pero conviene cerrarlo temprano** — mismo criterio que
   en el cierre de S1: condiciona las variables de entorno que usa el resto de Hilo A.
2. **`EP04-H01` (calibración de curso) es el ítem más restringido del sprint completo** — depende
   de un hilo entero (B) y de la mitad de otro (E, la rúbrica y el golden set versionado). Si
   Hilo E se atrasa, el ítem #6 de la Vista 1 (que es el segundo paso literal del guion de demo)
   se atrasa con él. Es el primer punto de riesgo real a vigilar en los Dailies.
3. **La cadena de `EP04-H02/H03/H04` es independiente de `EP04-H01`** — puede avanzar en paralelo
   sin esperar a Hilo E, igual que en el cierre de S1 se evitó hacer esperar un ítem de alto valor
   por una dependencia que no le correspondía.
4. **Hilo F depende de `EP01-H03`, no de todo Hilo A** — puede arrancar apenas el esqueleto
   transversal esté listo, sin esperar a H04/H05/H06.
5. **`EP01-H06` (suite de pruebas + guía de demo) va al final en ambas vistas** — es la única
   tarea cuya "medible" depende literalmente de que el resto ya exista.

---

## Riesgo de capacidad por pareja (remite al detalle ya auditado)

Esta vista no repite el cálculo de horas por pareja — ya está en
[`s1.md` · Balance de Horas por Pareja](s1.md#balance-de-horas-por-pareja-tope-individual-1163-h)
y se corrigió en esta misma sesión (Hilo A: 66 h reales, no 55 ni 146). En resumen, para decidir
**en qué orden mirar el riesgo en la Planning**:

1. **P5** (Hilo E) — ya en ~87% de su capacidad solo con lo cuantificado, antes de sumar 3 ítems
   sin estimar y su parte del Hilo G.
2. **P3** (Hilo C) — 64–68% solo con lo cuantificado, antes de sumar 2 ítems sin estimar y su
   parte del Hilo G.
3. **P2** — sube de ~52h (Hilo B solo) a ~52–95h al sumar su parte de Hilo F.
4. **P1** — con el número corregido (66h, no 55h) sube de "47–84%" a **57–94%** de su capacidad
   individual al sumar su parte de Hilo F — más ajustado de lo que decían las versiones
   anteriores de este documento.
