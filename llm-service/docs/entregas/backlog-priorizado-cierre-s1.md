# Backlog priorizado — cierre de S1 y Entrega 1, en dos vistas

> **Qué es.** Los mismos 12 ítems del [Sprint Backlog de cierre](../sprints/s1-cierre.md),
> ordenados dos veces: como los ordenaría un **Product Owner** (qué importa primero para que la
> Entrega 1 se pueda *mostrar*) y como los ordena el **equipo** (qué hay que resolver primero
> porque técnicamente lo de abajo depende de eso). Son la misma lista — cambia el criterio de
> orden, no el contenido.
>
> **Por qué dos vistas y no una.** Un PO prioriza por valor visible: le importa poco si Eureka
> está integrado si el docente igual puede ver una calibración cerrar con `PASSED`. Al equipo,
> en cambio, sí le importa el orden técnico: escribir la guía de demo (ítem de alto valor para
> el PO) no tiene sentido antes de que exista algo que demostrar. Ninguna vista está "mal" — sirven
> para cosas distintas y por eso conviene tener las dos, no elegir una.
>
> **Fuente de los ítems y sus horas:** [`checklist-cierre-s1.md`](checklist-cierre-s1.md),
> [`entrega-1.md`](entrega-1.md) y las fichas de tarea firmadas el 2026-09-12
> ([`ep-04/s03-h01.md`](../tareas/ep-04/s03-h01.md) T7,
> [`ep-02/model-catalog-real.md`](../tareas/ep-02/model-catalog-real.md),
> [`ep-01/h03.md`](../tareas/ep-01/h03.md) T9).

---

## Vista 1 — Product Owner: orden por valor de demo

Qué tan cerca pone cada ítem a "mostrar la Entrega 1 funcionando", de más a menos crítico.

| # | Ítem | Por qué este lugar | h |
|---:|---|---|---:|
| 1 | **Conectar calibración con H10** | Sin esto no hay Entrega 1: todo run queda en `RUNNING` para siempre. Es literalmente lo único que el PO ve en la pantalla | 14 |
| 2 | **Catálogo de adaptadores real (lectura)** | Resuelto el 2026-09-12: no bloquea la calibración (T7 usa `function_model_config`, no `model_deployments`) — baja a deuda de calidad, no de demo | 8 |
| 3 | **Guía de demo + prueba de reinicio** | Es la diferencia entre "andó una vez en mi máquina" y "evidencia reproducible" — lo que un PO/cátedra realmente audita en la Review | 5 |
| 4 | **`404` real para "no existe"** | Chico, pero es el tipo de detalle que un evaluador prueba a mano en dos minutos (pedir un id que no existe) y que hoy falla visiblemente | 3 |
| 5 | **Registro en Eureka** | Solo importa si la demo se corre en el ambiente integrado real. Si se demuestra en local, un PO no lo nota — pero la propia definición de "demo de S1" exige ambiente integrado, así que no se puede posponer del todo | 5 |
| 6 | **`401` real para token sin scope** | Es un detalle de seguridad que un evaluador exigente puede pedir ver, pero no cambia si la demo "funciona" | 6 |
| 7 | **Pipeline CI en verde** | Invisible en la demo en vivo; importa para la nota de proceso/calidad, no para el recorrido funcional | 4 |
| 8 | **Mock + check de CI de contrato** | Sirve a `admin-service`, no a quien mira la demo | 4 |
| 9 | **ADR de arquitectura** | Es puramente documentación de proceso — un PO orientado a producto lo prioriza último, aunque a la cátedra sí le puede importar (ver nota) | 16 |
| 10 | **`.env.example` + documentar `down`** | Cosmético para la demo | 7 |
| 11 | **Reescribir contrato v1→v2** | Deuda de documentación pura, cero impacto en lo que se muestra | 2 |
| 12 | **Gate de cobertura JaCoCo** | Es el que menos se nota en una demo en vivo — importa para sostener calidad a futuro, no para el momento de la Review | *(incluido en la fila 3)* |
| 13 | **Alta real de adaptador (`POST /admin/model-adapters`)** *(ampliación)* | No se nota en el recorrido de la demo, pero es la pieza que repara el pitch central del AI Gateway (RF-IA-11) si alguien pregunta "¿puedo agregar un modelo sin tocar código?" | 12 |
| 14 | **Reescribir H05/H06/H07 + verificación nueva** *(ampliación)* | Bajo impacto en la demo en vivo, alto impacto si alguien de la cátedra abre `docs/historias/` y encuentra endpoints borrados | 14 |

**Nota importante:** esta vista asume un PO que solo mira "¿funciona el recorrido de la demo?".
**La cátedra de este TP no es ese PO** — [00-fuentes-de-verdad-y-convenciones.md](../00-fuentes-de-verdad-y-convenciones.md)
y la guía de historias de usuario exigen ADR, CI y DoD como parte de lo evaluable, no como
adorno. Por eso esta vista es útil para decidir **qué mostrar primero si el tiempo aprieta**, no
para decidir qué **saltear**.

---

## Vista 2 — Equipo: orden por dependencia técnica real

Qué hay que resolver antes de qué, sin importar cuánto "se note" en la demo. Es el orden que
usa la tabla del [Sprint Backlog §4](../sprints/s1-cierre.md#4-historiastareas-comprometidas-y-dependencias).

```
Día 1 ──┬─ P1: Eureka (5h) ─→ 401 (6h) ─→ 404 (3h) ─→ CI verde (4h)   [secuencial, mismo módulo]
        │        │
        │        └─ (en paralelo) ADR (16h) — no depende de nada, pero conviene
        │           cerrarlo temprano porque condiciona .env.example (variables del ADR)
        │
        ├─ P4: Conectar calibración con H10 (14h) — arranca el día 1, no depende de P1
        │        │
        │        └─ resuelto 2026-09-12: usa `function_model_config` (mismo patrón que el
        │           tutor); la fila de P2 de al lado ya no es su dependencia
        │
        └─ P2: Catálogo real T1 (6h) ─→ T3 contrato (2h) — 8h en total, independiente de P4

Semana 2 ── una vez lo de arriba cierra:
        ├─ P1: .env.example + doc `down` (7h) ─→ mock + check CI (4h) ─→ rework contrato v1→v2 (2h)
        │        │
        │        └─ (en paralelo, ampliación) reescribir H05/H06/H07 + verificación (14h) —
        │           espera el 404 real (T9, día 1) para que la ficha reescrita no vuelva a
        │           prometer algo que el código no cumple
        │
        ├─ P2: (ampliación) alta real de adaptador — POST /admin/model-adapters (12h) —
        │        arranca apenas T1 (lectura del catálogo) esté lista, no necesita esperar a T2/T3
        │
        └─ Todas: prueba de reinicio (3h) ─→ guía de demo + JaCoCo (2h)
                  [esto va al final porque necesita algo real que probar/documentar]
```

**Por qué este orden y no otro:**

1. **Eureka antes que nada en P1** porque es la única pieza de la que depende *dónde* se puede
   correr la demo — todo lo demás de P1 se puede validar en local sin él.
2. **La fila de P4 (calibración) es independiente de P1 por completo.** No hay razón técnica para
   que espere — es el error más común en el reparto de este tipo de sprint: hacer esperar al que
   tiene el ítem de mayor valor porque "va después en la lista".
3. **La pregunta abierta de P2/P4 se resuelve el día 1, no a mitad de sprint.** Si se descubre en
   la semana 2 que hacía falta sembrar un `model_deployment`, ya no hay margen para hacerlo sin
   negociar alcance.
4. **La guía de demo y la prueba de reinicio van última porque son las únicas dos tareas cuya
   "medible" depende literalmente de que el resto exista.** Escribirlas antes sería documentar
   algo que todavía no se puede ejecutar.

---

## Veredicto de capacidad (actualizado 2026-09-12: catálogo bajó de 12h a 8h al descartar su T2)

| Pareja | h base (bloqueante/deuda) | h ampliación aceptada | Total | Capacidad de referencia | % ocupado |
|---|---:|---:|---:|---:|---:|
| P1 | 52 | +14 (fila 14) | 66 | ~116,3 h | ~57% |
| P4 | 14 | — | 14 | ~116,3 h | ~12% |
| P2 | 8 | +12 (fila 13) | 20 | ~116,3 h | ~17% |
| **Total backlog** | **74** | **+26** | **100** | 581,6 h (equipo completo) | — |

**Quedaron fuera de esta ampliación**, sin costo por postergarlas (siguiente ciclo): formalizar
T3/T5 de [`ep-01/h09.md`](../tareas/ep-01/h09.md) (~6 h) y unificar "cohorte"/"curso" en el resto
de la documentación (~5 h, [checklist §3.2](checklist-cierre-s1.md)).

Con la ampliación, P1 pasa de ~45% a ~57% de su capacidad — sigue teniendo margen, pero ya no es
tan holgado como antes de sumar la reescritura de fichas. P2 baja a ~17% (menos de lo que parecía,
tras descartar la tarea que no hacía falta). **Sigue entrando cómodo en 2 semanas**: nadie supera
el 60% de su capacidad de sprint, lo que deja aire real para imprevistos sin tener que frenar el
resto del trabajo en curso (EP-04 más allá de T7, EP-05).

**De los dos riesgos que se habían marcado, uno ya se cerró:** la pregunta técnica de T7/catálogo
se resolvió leyendo el código (no hacía falta esperar a Planning). **Solo queda un riesgo real
para este sprint: D01** (disponibilidad de Eureka/Gateway del lado de la plataforma compartida) —
no es algo que se resuelva desde la documentación, hay que confirmarlo con quien la mantiene.

**Si D01 no está disponible cuando P1 lo necesita:** correr la fila 1 igual en cuanto esté
disponible y demostrar el resto en local mientras tanto, documentando la limitación — no bloquear
todo el sprint por esto.

---

*Este documento no inventa prioridad nueva — reordena lo que ya está en
[`checklist-cierre-s1.md`](checklist-cierre-s1.md) y [`entrega-1.md`](entrega-1.md). Si cambia una
hora o una dependencia ahí, esta vista queda desactualizada hasta que se reordene a mano.*
