# Plantilla — Historia de Usuario (lenguaje simple, estructura de Taiga)

> **Estilo por defecto del skill.** Misma estructura del template oficial de Historia de
> Usuario de la Wiki de Taiga (Como/Quiero/Para, Notas, Criterios de Aceptación,
> escenarios BDD, Prototipo, Estimación, Dependencias), pero contada **sin jerga
> técnica**: como si se la explicara a alguien que nunca programó. Los códigos internos
> (`EP-01`, `P1`, `Sxx-Hyy`, `RF-*`) se dejan porque son parte del formato de Taiga, pero
> no hace falta entenderlos para seguir la ficha.
>
> Para el estilo con jerga precisa (endpoints, códigos HTTP, nombres exactos), ver
> [`referencia-estilo-tecnico.md`](referencia-estilo-tecnico.md).
>
> Las compuertas (DoR / DoD) están en `dor-dod.md`. Cómo se redacta y estima cada
> apartado: `guia-metodo.md`.

---

# Sxx-Hyy — [TÍTULO EN PALABRAS SIMPLES]

> Título en Taiga: `GXX — TÍTULO`. `GXX` = número de grupo/equipo.

| | |
|---|---|
| **Grupo de trabajo** | [nombre de la épica] ([EP-0X]) |
| **Pareja a cargo** | [P_] |
| **Depende de** | [Hnn, … o «Nada»] |
| **Trabajo estimado** | [N] horas |
| **Tipo** | [Tarea interna (no la «vive» un usuario final) / Historia de valor (la protagoniza [rol real])] |
| **Responsable del producto** | Se nombra en el Sprint 0 |

## Descripción (Como / Quiero / Para)

- **Como:** [ROL REAL en palabras — «profesor autorizado de un curso», «cualquier
  programador del equipo». No «el sistema».]
- **Quiero:** [lo que la persona quiere hacer, no cómo se resuelve por dentro]
- **Para:** [el beneficio real, no repetir la acción]

## Notas / Observaciones

- **Reglas de trabajo:** [las reglas del negocio contadas en palabras: qué tiene que
  pasar, sin nombrar tablas ni clases]
- **Cómo se controla:** [qué revisa el sistema antes de aceptar; qué casos rechaza]
- **Qué tiene que incluir sí o sí:** [los datos obligatorios, en palabras]
- **Tiempos / volumen:** [si importa la rapidez o la cantidad; si no, «no aplica»]
- **Seguridad:** [quién puede y quién no; qué pasa con un pedido sin permiso
  («prohibido» / «no autorizado»)]
- **Accesibilidad:** [pensada para alguien que navega con teclado o usa lector de
  pantalla; o «no aplica» + motivo]
- **Otros:** [lo que aclaró el cliente y no entra arriba]
- **Operaciones nuevas:** [si el trabajo agrega formas de pedirle algo al servicio,
  nombrarlas en palabras: «crear la colección», «consultar el detalle»]

## Criterios de Aceptación (CA)

- **CA1:** [condición concreta y verificable, en palabras]
- **CA2:** [·]
- **CA3:** [·]
- **CA4 (caso que debe fallar):** [pedido sin permiso / dato inválido / algo repetido /
  una pieza caída]
- **CA5 (caso que debe fallar):** [otro caso que debe fallar]

> Siempre al menos un caso que sale bien y **dos que deben fallar**.

## BDD (mínimo 3 escenarios)

**Qué se prueba:** [en una frase, qué comportamiento se está validando]

### Escenario 1 — [título del camino esperado]

- **Dado:** [la situación de partida]
- **Cuando:** [lo que hace la persona o el sistema]
- **Entonces:** [lo que se ve: una pantalla, un mensaje o un dato guardado]
- **Y:** [opcional — continúa el paso anterior]

### Escenario 2 — [título de un caso que debe fallar]

- **Dado:** [situación]
- **Cuando:** [acción]
- **Entonces:** [lo que se ve]

### Escenario 3 — [título de otro caso que debe fallar]

- **Dado:** [situación]
- **Cuando:** [acción]
- **Entonces:** [lo que se ve]

## Prototipo

- **Capturas / bocetos:** [bocetos simples de las pantallas principales, o «no aplica» +
  motivo si es una pieza interna sin pantalla]
- **Maqueta / documentación:** [enlace a la maqueta, la demo o el documento del acuerdo;
  o «no aplica»]

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0, con la técnica de estimación del
  equipo [/ comparando contra la historia patrón].
- **Prioridad:** [imprescindible / deseable / se puede posponer].

> El «Trabajo estimado» en horas de la ficha es la referencia del plan; **no** se
> convierte a puntos.

## Dependencias / Impactos

- **Partes involucradas:** [qué piezas del sistema o servicios entran en juego]
- **Otros equipos / aprobaciones:** [de quién se depende, o «ninguna externa»]
- **Impacto en los datos:** [si crea, cambia o solo lee datos]
- **Riesgos:** [qué puede salir mal y cómo se acota]

## Tareas (los pasos técnicos)

> Los pasos concretos en los que el equipo de desarrollo parte el trabajo. **No** se
> escriben en formato Como/Quiero/Para. Cada una dura **un día de trabajo o menos**. Las
> horas son aproximadas y suman el «Trabajo estimado» de la ficha.

| # | Tarea | h |
|---|---|--:|
| T1 | [·] | · |
| T2 | [·] | · |
| T3 | [·] | · |
| | **Total** | **·** |
