# 38 — Plan vigente: máximo 5 sprints

> **Recalibración del 2026-09-13.** El horizonte de **19 sprints** de
> [23](03-plan-de-construccion-del-producto.md) y las recetas **S1–S19** de
> [35](04-backlog-ejecutable.md) quedan **retirados como compromiso de calendario**. A partir de
> acá, **el plan tiene un techo duro de 5 sprints quincenales (10 semanas)** — no una aspiración,
> un límite. Si algo no entra en 5 sprints, **no se construye este cuatrimestre**, se anota como
> fuera de alcance (Parte 4 de este documento) y punto.
>
> **Qué sigue vigente de 23 y 35, y qué no:**
>
> | Sigue vigente | Queda retirado |
> |---|---|
> | El modelo de capacidad de [`capacidad-sprints.md`](09-epicas-historias-tareas-sprints/sprints/capacidad-sprints.md) (h por pareja, factor de foco 80%) — con la salvedad del §5 de abajo | El horizonte de 19 sprints / 3 fases como compromiso de calendario |
> | El equipo de 5 parejas (P1–P5) de [23 · §3](03-plan-de-construccion-del-producto.md)| Las recetas de paquetes **S6, S14 (ya cubierta), S19** de [35](04-backlog-ejecutable.md) — quedan como *antecedente de diseño técnico*. Las recetas **S9–S13, S15–S18** sí se ejecutan, renumeradas: **S9–S10 y S15–S16 a Sprints 1–2**, **S11–S13 y S17–S18 a Sprints 4–5** (ver la segunda y tercera recalibración abajo y la Parte 3) |
> | La DoR/DoD de [23 · §9.2](03-plan-de-construccion-del-producto.md) | Cualquier mención de "F1 en S1–S10" — la fase ya no organiza el calendario, este documento sí |
> | Las recetas **S1–S4** de [35](04-backlog-ejecutable.md) donde no contradigan la Parte 2 de acá (la auditoría de código real manda sobre la receta escrita) | — |
>
> **Por qué existe este documento y no una edición de 23/35.** Reescribir 19 sprints de recetas
> detalladas para que quepan en 5 inventaría trabajo que nadie pidió. Este documento parte al
> revés: primero audita **qué hay construido de verdad** (commit `605f381` y las sesiones
> posteriores adelantaron trabajo de S1 a S3, S14 y parcialmente S6), después separa **lo
> obligatorio** de **lo deseado**, y recién ahí arma 5 sprints con lo que sobra de margen — no al
> revés.
>
> **Segunda recalibración, 2026-09-13 (noche).** Se reservaron
> Sprints 3–5 como colchón genuino (~120–190 h de las ~1 744,8 h de capacidad de referencia de
> esos tres sprints), citando la regla de [`capacidad-sprints.md` §1.4](09-epicas-historias-tareas-sprints/sprints/capacidad-sprints.md)
> ("Capacidad ≠ Presupuesto a Llenar"). **Decisión explícita de esta sesión: se descarta esa
> lectura.** Se carga en Sprints 3–5 todo lo que la Parte 4 anterior declaraba fuera de alcance y
> que además tiene épica e historias ya redactadas (EP-07, EP-08, EP-10) — y, donde el resultado
> supera el techo de 581,6 h/sprint de 10 devs, **se supera igual**: el techo de este documento es
> el **calendario (5 sprints)**, no las horas. La Parte 4 queda reducida a lo que
> *ni siquiera tiene épica o ficha vigente* — no hay nada que planificar ahí, con horas o sin
> ellas. Ver Parte 3 para el detalle sprint por sprint y la Parte 4 para qué quedó realmente
> afuera y por qué.
>
> **Tercera recalibración, 2026-09-13 (noche, 2ª vuelta).** La segunda recalibración había puesto
> EP-07 y la extensión de EP-09 en Sprint 3, dejando Sprint 1 y Sprint 2 relativamente livianos
> (~224–284 h y ~70–90 h de 581,6 h cada uno) mientras Sprint 3 saltaba a ~872–897 h. **A pedido
> explícito de esta sesión, se adelanta EP-07 y la extensión de EP-09 a Sprints 1–2** — repartidas
> por mitades (S1 arranca, S2 cierra) porque ahí es donde P1, P2, P3 y P5 tenían hueco real sin
> forzar nada. EP-08 y EP-10 **no** se adelantan por la misma razón que antes: su dependencia es
> de contrato con otro equipo, no de hueco de horas, y adelantarlas no acerca esa dependencia.
>
> **Corrección, la misma noche, más tarde.** Al escribir las fichas de detalle de S1
> ([`s1.md`](09-epicas-historias-tareas-sprints/sprints/sprint-1/s1.md)) se encontró que EP-07, EP-08 y EP-10 ya tenían **fichas
> reales con horas propias** — y esas horas son **26–41% de la referencia histórica de `35`** que
> se había usado hasta ese momento (~208 h/paquete) para calcular todo lo de arriba. Con los
> números reales: EP-07 son 156 h, no 416; EP-08 son 160 h, no 624; EP-10 son 116 h, no 416. Se
> corrige todo el documento con estas cifras. **Conclusión que cambia:** el plan ya no necesita
> superar el techo de 581,6 h/sprint en Sprint 4 ni en Sprint 5 — solo Sprint 1 queda por encima
> (~114–117%, y solo por el Hilo G, que sigue sin ficha propia). La extensión de EP-09 (S15/S16)
> tampoco tiene ficha todavía, así que su referencia histórica (~208 h/mitad) queda sin corregir
> — probablemente también sobreestimada, a la luz del patrón de las otras tres épicas.

---

## Parte 1 — Lo obligatorio: los 6 ítems del alcance estricto de la cátedra

Vienen de [`sprints/README.md`](09-epicas-historias-tareas-sprints/sprints/README.md) § *"Reprogramación a 8 semanas"*,
que reconoce un corte más chico que 19 sprints.

| # | Ítem | Estado auditado al 2026-09-13 | Evidencia |
|---|---|---|---|
| 1 | Rúbrica versionada | 🟢 hecho | `LLM-S02-H01` |
| 2 | Golden set versionado | 🟢 hecho | `LLM-S01-H05/H06`, `LLM-S02-H02` |
| 3 | Invocación real del modelo | 🔴 **el único bloqueante duro** | [`ep-02/h10.md`](../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-02/h10.md): *"`FakeModelAdapter` — nunca llama a un proveedor real"* |
| 4 | Calibración | 🟢 a nivel curso (alcanza el ítem) | [`ep-04/s03-h01.md`](../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-04/s03-h01.md), T7 cerrada 2026-09-13 |
| 5 | Bloqueo de activación | 🟡 mitad propia hecha, falta el otro equipo | El endpoint existe; `courses-service` todavía no lo consume |
| 6 | Salvaguarda anti-fuga | 🟢 hecho, con tests | [`ep-05/interactions.md`](../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-05/interactions.md): `InputGuard` + `OutputAntiLeakGuard` |

**De los 6, solo el #3 es un hueco de código propio sin depender de nadie más.** El #5 depende de
que otro equipo confirme un contrato. Esto es lo que hace que 5 sprints alcancen y sobren para lo
obligatorio — el resto del presupuesto de sprints es para lo deseado (Parte 2).

---

## Parte 2 — Lo deseado: todo lo demás que existe o se propuso

No es parte del corte de la cátedra, pero es trabajo real, ya construido en buena parte, o con
ficha lista. Se ordena por **cuánto ya existe**, no por número de épica.

| Bloque | Qué es | Estado | Dónde está la ficha/auditoría |
|---|---|---|---|
| EP-05 · Tutor | Interacciones síncronas + conversaciones, con los dos guardarraíles | 🟡 código completo, **sin historia formal** | [`estado-implementacion/ep-05/`](../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-05/README.md)
| EP-09 · RAG | Ingesta + chat con citas | 🟡 código completo, cobertura de integración real al 0% en 4 clases | [`estado-implementacion/ep-09/`](../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-09/README.md)
| EP-04 · Calibración de plataforma | Falta el nivel ADMIN (solo existe el de curso) | ⚪ ficha lista, sin construir | [`historias/ep-04/h02.md`](09-epicas-historias-tareas-sprints/historias/ep-04/h02.md)
| EP-04 · Vencimiento, avisos, UI | Paquetes 2, 4 y 5 de la receta S4 original, nunca hechos en ningún nivel | 🔴 | — |
| EP-06 · Evaluación real de un intento | Consumir el cierre, calcular las 5 dimensiones, resiliencia si el proveedor cae | 🔴 **no iniciado en absoluto** | [`estado-implementacion/ep-06/evaluacion-y-apelacion.md`](../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-06/evaluacion-y-apelacion.md); fichas en [`historias/ep-06/`](09-epicas-historias-tareas-sprints/historias/ep-06/README.md) (`H01`, `H02`, `H03`) |
| EP-06 · Apelación, override, bloqueo de cierre de curso | El resto de la épica, sobre la evaluación real de arriba | 🔴, sin ficha todavía | — |

---

## Parte 3 — Los 5 sprints

> **Capacidad de referencia:** 581,6 h/sprint si el equipo activo son 10 devs en 5 parejas
> ([`capacidad-sprints.md`](09-epicas-historias-tareas-sprints/sprints/capacidad-sprints.md)). **Advertencia real:** esa planilla
> es del modelo de cátedra completo; si el equipo que efectivamente está escribiendo código hoy
> es más chico, la capacidad de cada sprint de abajo se reduce proporcional — recortar el
> compromiso en la Planning, no estirar el sprint.

> **Corrección del 2026-09-13 (tarde).** Sprint 1 **todavía no arrancó formalmente** — no existe
> el registro `s1.md` de la Planning (`sprints/README.md` § Índice: *"crear desde plantilla en
> la Planning de S1"*), solo el cierre de huecos auditados en `s1-cierre.md`. Como no arrancó,
> **nada obliga a que sea solo el hilo A** (Plataforma): ninguno de los otros cuatro hilos
> (B, C, D, E) depende de que Eureka esté registrado, los códigos `401`/`404` sean reales, el CI
> esté en verde o el ADR esté escrito — esas son tareas internas de EP-01, no dependencias
> técnicas de EP-02/04/05/06/09. Por eso Sprint 1 pasa a ser **el arranque de los 5 hilos en
> paralelo**, no solo el cierre de P1.

### Sprint 1 · Arranque en paralelo *(todavía sin Planning — es el momento de decidir esto)*

> **Tercera recalibración, 2026-09-13 (noche, 2ª vuelta).** Los cinco hilos A-E de abajo dejaban
> ~297–357 h de las 581,6 h de referencia sin usar, sobre todo en P1 y P2 (hilos A y B) y con
> algo de margen en P3 (hilo C). En vez de dejar ese hueco para más adelante, se le suma la
> **primera mitad de EP-07** (P1+P2) y la **primera mitad de la extensión de EP-09** (P3+P5) —
> el detalle historia por historia vive en [`sprint-1/README.md`](09-epicas-historias-tareas-sprints/sprints/sprint-1/README.md)
> que es la fuente de verdad operativa de S1 (esta tabla queda como resumen).
>
> **Corrección del mismo realineado (más tarde la misma noche).** Las horas de F y G de abajo
> venían de la receta histórica de [`35`](04-backlog-ejecutable.md) (~208 h por paquete). Al
> revisar las **fichas reales** de EP-07 ([`historias/ep-07/`](09-epicas-historias-tareas-sprints/historias/ep-07/README.md)) para
> alinear [`s1.md`](09-epicas-historias-tareas-sprints/sprints/sprint-1/s1.md), la primera mitad (Hilo F) sale en **86 h**, no 208 —
> un tercio de la referencia histórica. Se corrige F acá con el número real; G sigue sin ficha
> propia (se mantiene su referencia gruesa, marcada como probablemente sobreestimada también).
> Además, cuatro fichas de los Hilos C/D/E (`EP09-H01/H02` sin desglose individual, `EP04-H02`,
> `EP03-H04`, `EP03-H05`, `EP06-H02`) dicen explícitamente *"estimar en Refinamiento, no
> inventar el número"* — sus horas quedan pendientes, no en 0 ni rellenadas a ojo.

| Hilo | Pareja | Qué hace en Sprint 1 | h (referencia) |
|---|---|---:|---:|
| **A — Plataforma** | P1 | Cierre de [`s1-cierre.md`](09-epicas-historias-tareas-sprints/sprints/sprint-1/s1-cierre.md): Eureka/Gateway, `401`/`404`, CI verde, ADR, `.env`, mock, prueba de reinicio, guía de demo + abrir el contrato con `courses-service` | ~52–55 |
| **B — AI Gateway** | P2 | `LLM-S03-H11` — adaptador real de proveedor, construido y probado contra el `GroqAdapter` de referencia | 15–22 |
| **C — Tutor y RAG** | P3 | EP-05 completa (3 historias) + EP-09 base (2, sin desglose individual) | ~74–79 + 2 sin estimar |
| **D — Calibración** | P4 | EP-04 completa (4 historias: curso 46 h real + plataforma a estimar + vencimiento 12–15 + avisos 15–20) | ~73–81 + 1 a estimar |
| **E — Evaluación real** | P5 | EP-03 rúbrica/golden set/base (5 historias, 2 a estimar) + EP-06 base (3, 1 a estimar) | ~101 + 3 a estimar |
| **F — EP-07, primera mitad** | P1+P2 | `LLM-S09-H01/H02/H03` — panel de costos/cuotas/fallas, límite de cuota versionado, `429`/`Retry-After` | **86 (real: 36+28+22)** |
| **G — EP-09, extensión, primera mitad** | P3+P5 | Ingesta visual con control de calidad (OCR/tablas/figuras) + embeddings reales | ~208 (referencia histórica, sin ficha, probablemente sobreestimada) |
| | | **Total Sprint 1** | **~660–681 h + ~9 ítems a estimar/sin ficha** |

Los hilos A-E, corridos por su propia pareja, quedan **debajo de su capacidad individual de
sprint** (~116,3 h) usando solo lo ya cuantificado — el más cargado de los cinco (D) usa ~63–70%
de una pareja antes de sumar lo que falta estimar. Los hilos F y G empujan a P2 y P5 por encima
de esa capacidad individual (ver la nota de concentración en
[`sprint-1/README.md`](09-epicas-historias-tareas-sprints/sprints/sprint-1/README.md)) porque F y G se suman **encima** de B y E,
no en lugar de. **Total del sprint: ~660–681 h contra 581,6 h de referencia (~114–117%)**, sin
contar lo que aún falta estimar — la decisión explícita de esta recalibración es que S1 sea el
sprint más cargado del plan en términos relativos, porque es donde más parejas tenían hueco
real, aunque con el número real de EP-07 el margen sobre el techo es bastante menor de lo que se
pensó en la segunda recalibración (~142% con la referencia histórica).

### Sprint 2 · Cerrar EP-06, EP-07 y la extensión de EP-09; volver todo real

Con Sprint 1 llevándose casi todo el volumen de lo obligatorio y lo ya deseado, a Sprint 2 le
queda la **cola de cada hilo** — lo que no cerró en Sprint 1 por secuencia propia, no por falta
de horas — **más el resto de EP-07 y de la extensión de EP-09** que los Hilos F/G de S1 dejaron
a mitad camino:

| Paquete | Pareja | h (referencia) | Qué entrega |
|---|---|---:|---|
| EP-06 — apelación, override, bloqueo de cierre de curso | P5 | 55–70 | Cierra EP-06 **completa** — el alumno apela, el docente resuelve, el resultado original se conserva, un curso no cierra con pendientes |
| EP-07 — segunda mitad (`LLM-S10-H01/H02/H03`) | P1+P2 | **70 (real: 30+14+26)** | Cierra EP-07 **completa**: recuperación de trabajos sin duplicar, salud útil sin proveedor, prueba de carga y backup/restore |
| EP-09 — extensión, segunda mitad | P3+P5 | ~208 (referencia histórica, sin ficha) | Cierra la extensión **completa**: versionado y retiro documental sin mezclar versiones |
| Enchufar `LLM-S03-H11` en tutor, calibración y RAG + repetir las suites contra el proveedor real | P2 | 15–20 | Lo construido en Sprint 1 contra el fake pasa a correr contra el proveedor real de verdad |
| Bloqueo real de punta a punta con `courses-service` | P1 | variable | Si respondieron en Sprint 1 — si no, sigue como riesgo declarado, no bloquea el sprint |
| | **Total** | **~348–368 h + variable** | |

**Con esto, el trabajo pendiente identificado esta sesión — las 6 épicas obligatorias/deseadas
más EP-07 y la extensión de EP-09 — queda funcionalmente completo al cierre de Sprint 2**, muy
por debajo del techo de 581,6 h en este sprint (S2 usa ~60–63% de su referencia, con margen
todavía más amplio que el estimado en la recalibración anterior porque EP-07 real (70 h) es un
tercio de su referencia histórica (~208 h)).

### Sprints 3–5 · Lo que sigue: EP-08, EP-10 y el colchón real

EP-07 y la extensión de EP-09 se adelantan a S1+S2 (arriba) porque
ahí había hueco genuino sin forzar el techo. Lo que queda para S3-S5 es **EP-08 y EP-10** —
las dos épicas con dependencia real de contrato externo, que no tenía sentido adelantar porque
ese contrato no depende de en qué sprint se las ponga. El orden respeta la única dependencia
técnica real entre ellas: **EP-10 necesita EP-08 lista** (moderación disponible antes que el
agente pueda publicar).

**Advertencia que no cambia por cargar más horas:** EP-08 necesita el contrato de moderación
cerrado con `chat-service` ([`ep-08.md`](09-epicas-historias-tareas-sprints/epicas/ep-08.md) · Suposiciones); EP-10 necesita ese
mismo contrato más el de `challenges-service` ([`ep-10.md`](09-epicas-historias-tareas-sprints/epicas/ep-10.md) · Suposiciones).
Ninguna de las dos dependencias es de horas — son de otro equipo, y **superar el techo de
581,6 h/sprint no las resuelve**. Se cargan igual porque la decisión de esta sesión es
calendario ante todo; el riesgo de contrato externo queda declarado, no oculto.

- **Sprint 3 — colchón real (todas las parejas):**
  - Regresión de EP-01 a EP-07 + EP-09 completa contra el proveedor real, cobertura de
    integración al 95%, y la última oportunidad para lo que `courses-service` o la ventana de
    pruebas de carga de EP-07 no hayan cerrado en S1/S2.
  - **Total Sprint 3: ~40–65 h** — muy por debajo del techo de referencia, a propósito: es el
    único sprint de los cinco sin backlog nuevo pendiente de asignar.
- **Sprint 4 — EP-08 completa (P1+P2+P3):**
  - `LLM-S11-H01/H02`, `LLM-S12-H01/H02`, `LLM-S13-H01/H02` — [`historias/ep-08/`](09-epicas-historias-tareas-sprints/historias/ep-08/README.md): bloqueo/permiso antes de entregar, reglas simples + clasificador contextual, apelación del alumno, resolución docente, degradación si el clasificador falla, retención y purga de evidencia. **160 h reales** (34+28+26+30+24+18) — la receta histórica S11+S12+S13 daba ~624 h; la ficha real es 26% de eso.
  - Más la integración E2E en ambiente compartido (~45–70 h).
  - **Total Sprint 4: ~205–230 h** — con el número real, **no** supera el techo de referencia: queda en ~35–40%.
- **Sprint 5 — EP-10 completa (P3+P4+P5) + streaming del tutor + demo/defensa:**
  - `LLM-S17-H01/H02`, `LLM-S18-H01/H02` — [`historias/ep-10/`](09-epicas-historias-tareas-sprints/historias/ep-10/README.md): desafío personalizado generado desde el material del curso, entrega durable sin duplicar, mención a `@agente` con respuesta citada y moderada, el agente nunca responde a otro bot. **116 h reales** (38+32+30+16) — la receta histórica S17+S18 daba ~416 h; la ficha real es 28% de eso. Depende de que Sprint 4 haya cerrado EP-08 — si no cerró, EP-10 se recorta en la Planning de S5, no se adelanta igual.
  - Streaming/SSE del tutor (Buffer Interceptor) — declarado fuera del contrato ejecutable hasta que se fusione (ver Parte 4 histórica); se carga acá como paquete chico. **Sin ficha todavía — se redacta en la Planning de S5.** ~50 h.
  - Más la demo grabada y el deck de defensa (~35–55 h).
  - **Total Sprint 5: ~201–221 h** — queda muy por debajo del techo de referencia (581,6 h, ~35–38%): con el número real de EP-10 hay más margen del que se pensó para absorber lo que EP-10 herede recortado de un Sprint 4 que no cerró, más el ensayo de la demo final.

### La cuenta completa, de una sola vez

> **Corregida en la 2ª vuelta de la tercera recalibración.** Las filas de EP-07, EP-08 y EP-10
> usaban la receta histórica de [`35`](04-backlog-ejecutable.md) (~208 h/paquete). Con fichas
> reales escritas para las tres, salen en **26–41% de esa referencia** — se reemplaza el número
> viejo por el real en toda la tabla. La extensión de EP-09 (S15/S16) sigue sin ficha propia, así
> que se mantiene su referencia gruesa, marcada como probablemente también sobreestimada.

| Bloque | h (referencia) | Sprint |
|---|---:|---|
| Cierre de Sprint 1 (`s1-cierre.md`) + contrato `courses-service` | ~55 | 1 |
| EP-02 — adaptador real | 15–22 | 1 |
| EP-04 — plataforma + vencimiento + avisos + UI (completa) | ~73–81 + 1 a estimar | 1 |
| EP-05 — historias formales | ~74–79 + 2 a estimar | 1 |
| EP-09 — cobertura de integración real (base) | 15–20 | 1 |
| EP-06 — H01+H02+H03 | 65–85 + 1 a estimar | 1 |
| EP-07 — primera mitad (`LLM-S09-*`) | **86 (real)** | 1 |
| EP-09 — extensión, primera mitad (ingesta visual) | ~208 (referencia, sin ficha) | 1 |
| EP-06 — apelación, override, bloqueo de cierre (completa la épica) | 55–70 | 2 |
| EP-07 — segunda mitad (`LLM-S10-*`, completa la épica) | **70 (real)** | 2 |
| EP-09 — extensión, segunda mitad (versionado documental, completa la extensión) | ~208 (referencia, sin ficha) | 2 |
| Enchufar el adaptador real en todo + bloqueo real con `courses-service` | 15–20 + variable | 2 |
| Regresión y cobertura de S1/S2 | 40–65 | 3 |
| Integración E2E en ambiente compartido | 45–70 | 4 |
| EP-08 — moderación integrada (completa) | **160 (real)** | 4 |
| EP-10 — personalización y agente (completa) | **116 (real)** | 5 |
| Streaming/SSE del tutor | ~50 | 5 |
| Demo grabada + deck de defensa | 35–55 | 5 |
| **Total** | **~1 512–1 585 h + varios ítems a estimar** | — |

Contra **581,6 h/sprint × 5 sprints = 2 908 h de capacidad de referencia total**, la cuenta de
arriba usa solo **~52–55%** del total del plan. **Con los números reales, el plan deja de necesitar exceder el techo
en ningún sprint salvo Sprint 1** (~114–117%, y solo por el Hilo G sin ficha — si G también
resultara ~30% de su referencia como las otras tres épicas, S1 tampoco excedería el techo).

**La única variable real sigue siendo el tamaño del equipo, no las horas.** Si son efectivamente
5 parejas de 2 (10 devs), todo el backlog identificado esta sesión — obligatorio, deseado, y las
tres épicas que antes estaban en la Parte 4 — cabe dentro del techo de referencia en Sprints 2,
3, 4 y 5, con Sprint 1 apenas por encima. Si el equipo activo es más chico, cada hilo deja de
correr en paralelo con los otros y hay que recortar en la Planning — pero la prioridad declarada
en esta recalibración es **primero lo obligatorio (Parte 1), después Sprint 2, después
EP-08→EP-10 en ese orden**, no repartir por partes iguales.

---

## Parte 4 — Lo que sigue sin entrar, incluso cargando todo lo demás

**Recalibrado el 2026-09-13 (noche).** EP-07, EP-08, EP-10 y la extensión de EP-09 (S15/S16)
**salieron de esta parte y pasaron a la Parte 3** — tienen épica y, salvo la extensión de EP-09,
ficha de historia escrita, así que hay algo concreto para programar aunque falte redactar dos
paquetes. Lo que queda realmente afuera es lo que **ni siquiera tiene eso**, y cargar más horas
no lo resuelve porque el problema no es de horas:

- El **generador de parciales** (viejo EP-09/E09 del plan superado) — no tiene épica vigente:
  el objetivo actual de EP-09 ([`epicas/ep-09.md`](09-epicas-historias-tareas-sprints/epicas/ep-09.md)) es ingesta + consulta con
  citas y abstención, no generación de exámenes. No hay ficha, no hay requisito RF que lo pida,
  no hay nada que reprogramar. Si el curso lo quiere de vuelta, primero necesita una épica nueva,
  no un lugar en el calendario.
- **EP-08** en su versión de moderación *externa* con `chat-service` y **EP-10** en su versión
  de agente publicando en el chat real — la Parte 3 ya los carga en horas, pero el contrato con
  `chat-service` (EP-08) y con `challenges-service` (EP-10) sigue siendo de otro equipo. Si esos
  contratos no cierran a tiempo, el trabajo propio (reglas, endpoints, tests contra un stub) se
  hace igual y queda desconectado del sistema real hasta que el otro equipo entregue — eso no es
  "no entra en 5 sprints", es "entra construido, pero no integrado", y se declara así en la
  Planning de S4/S5, no se esconde detrás de la palabra "completo".

---

*Documento vigente desde el 2026-09-13. No se vuelve a mencionar "19 sprints" como compromiso en
ningún documento de este repositorio — donde aparezca, es un error a corregir, no una fuente de
verdad alternativa. La cifra de horas por sprint de la Parte 3 tampoco es un techo a partir de
esta segunda recalibración: donde el backlog con épica y ficha vigente supera los 581,6 h/sprint,
se registra igual y se resuelve en la Planning recortando alcance de otro sprint o aceptando el
sobre-compromiso, no estirando el calendario más allá de 5 sprints.*
