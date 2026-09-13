# Sprint Backlog — Cierre de S1 + Entrega 1

> **Qué es este documento.** El **registro de sprint** (copia de
> [`plantillas/sprint-llm.md`](../plantillas/sprint-llm.md)) para el ciclo que cierra los huecos
> de S1 y construye lo que falta de la Entrega 1. No es un sprint numerado de la receta original
> ([`35`](../35-backlog-ejecutable.md)) — nace de dos documentos ya escritos:
>
> | Fuente | Qué aporta |
> |---|---|
> | [`entregas/checklist-cierre-s1.md`](../entregas/checklist-cierre-s1.md) | Los 6 bloqueantes/huecos de S1 tal cual se definió originalmente |
> | [`entregas/entrega-1.md`](../entregas/entrega-1.md) | Los 3 ítems que faltan para la Entrega 1 real (más ambiciosa que S1 original) |
> | [`entregas/backlog-priorizado-cierre-s1.md`](../entregas/backlog-priorizado-cierre-s1.md) | El orden — dos vistas: valor de demo (PO) y dependencia técnica (equipo) |
>
> **Qué NO es.** No reabre `605f381` ni el flujo v2 de golden set — eso ya está resuelto
> ([decision-605f381.md](../entregas/decision-605f381.md)). Tampoco reemplaza las 240 h de S1
> original en [`35`](../35-backlog-ejecutable.md): las suma, porque son huecos sobre ese mismo
> compromiso, no un sprint nuevo del plan de 19.
>
> **Completar en la Planning real** (fecha, disponibilidad declarada, responsables con nombre):
> lo de abajo es la propuesta técnica, no el acta.

## 1. Identificación y objetivo

| Campo | Valor a completar |
|---|---|
| Sprint / fase | Cierre S1 / F1 (fuera de la numeración S1–S19) |
| Inicio y cierre del ciclo | *(a completar en Planning — 2 semanas)* |
| Fecha de Planning previa a ejecución | *(a completar)* |
| Objetivo y usuario beneficiado | Un docente autorizado publica rúbrica y golden set de su curso, encola una calibración contra el adaptador simulado (H10) y la calibración cierra con `PASSED`/`FAILED` y su métrica visible — sin haber gastado un centavo en un proveedor real ([entrega-1.md](../entregas/entrega-1.md)) |
| Recorrido funcional que se demostrará | Publicar rúbrica → publicar golden set → encolar calibración → la calibración deja de quedar en `RUNNING` para siempre y cierra con resultado |
| Límites del incremento | No incluye proveedor real, interacciones elegibles reales, casos sintéticos reales ni el flujo completo de "base de plataforma" de S02-H02·T7 (queda para después, no es Entrega 1) |
| Versión anterior que debe seguir funcionando | Golden set/rúbrica versionados por curso (S2, ya construido) — no se toca |
| Referente de producto / facilitador | *(a nombrar)* |
| Ambiente de integración | Ambiente integrado (no máquina local aislada) — condición explícita de la demo de S1 y de la 1.4 de este backlog |

## 2. Capacidad individual

> Referencia del modelo oficial ([`capacidad-sprints.md`](capacidad-sprints.md)): **581,6 h**
> comprometibles entre los 10 devs por sprint quincenal, **~116,3 h por pareja**. Esta tabla se
> completa en la Planning real con la disponibilidad declarada de ese ciclo puntual — los números
> de abajo son la referencia, no el compromiso.

| Integrante | Pareja / suplente | Disponibilidad declarada (h) | Reuniones (h) | Soporte conocido (h) | Base restante (h) | Reserva 20% (h) | Entregables (h) |
|---|---|---:|---:|---:|---:|---:|---:|
| *(P1, ambos integrantes)* | P1 | | | | | | ~116,3 (referencia) |
| *(P2, ambos integrantes)* | P2 | | | | | | ~116,3 (referencia) |
| *(P4, ambos integrantes)* | P4 | | | | | | ~116,3 (referencia) |
| **Total real planificado** | | | | | | | |

## 3. Reuniones del ciclo

Usar la tabla estándar de [`plantillas/sprint-llm.md` §3](../plantillas/sprint-llm.md) sin
cambios (102 h-persona de referencia). No se agregan reuniones extra por ser un sprint de cierre.

## 4. Historias/tareas comprometidas y dependencias

> Agrupa el trabajo por **pieza técnica**, no por historia formal, porque varios ítems son deuda
> sobre historias ya "cerradas" (H01–H09) o tareas sin ficha de HU (catálogo de modelos). El orden
> de la columna **Orden** es la **vista técnica** de
> [`backlog-priorizado-cierre-s1.md`](../entregas/backlog-priorizado-cierre-s1.md) — no el de
> valor de demo, que ese mismo documento explica por separado.

| Orden | Ítem | Historia / archivo | Responsable | Dependencias | h | Sprint objetivo | Estado |
|---:|---|---|---|---|---:|---|---|
| 1 | Registro en Eureka + ruta por Gateway | [`ep-01/h03.md`](../tareas/ep-01/h03.md) T3 | P1 | D01 (Gateway/Eureka disponible) | 5 | Cierre S1 | Pendiente |
| 2 | `401` real para token sin scope | [`ep-01/h03.md`](../tareas/ep-01/h03.md) T4 | P1 | — | 6 | Cierre S1 | Pendiente |
| 3 | `404` real para "no existe" (no `409`/`403`) | [`ep-01/h03.md`](../tareas/ep-01/h03.md) T9 | P1 | T6 (ya hecha) | 3 | Cierre S1 | Pendiente |
| 4 | Pipeline CI en verde (build+tests+cobertura) | [`ep-01/h03.md`](../tareas/ep-01/h03.md) T8 | P1 | 1–3 | 4 | Cierre S1 | Pendiente |
| 5 | ADR de arquitectura y convenciones | [`ep-01/h01.md`](../tareas/ep-01/h01.md) T1–T5 | P1 | — (en paralelo a 1–4) | 16 | Cierre S1 | Pendiente |
| 6 | Confirmar `.env.example` + documentar `down` | [`ep-01/h02.md`](../tareas/ep-01/h02.md) T3, T6 | P1 | H01·T3 (variables del ADR) | 7 | Cierre S1 | Pendiente |
| 7 | **Conectar calibración con el puerto de H10** | [`ep-04/s03-h01.md`](../tareas/ep-04/s03-h01.md) T7 | P4 | H10 (✅). Ya no depende de la fila 8 (resuelto: usa `function_model_config`) | 14 | Entrega 1 | 🔴 Bloqueante |
| 8 | Catálogo de adaptadores real (lectura) | [`ep-02/model-catalog-real.md`](../tareas/ep-02/model-catalog-real.md) T1+T3 | P2 | — (independiente de la fila 7) | 8 | Entrega 1 | Pendiente |
| 9 | Mock levantable + check CI de contrato no acordado | [`ep-01/h08.md`](../tareas/ep-01/h08.md) T3, T4 | P1 | — | 4 | Cierre S1 | Pendiente |
| 10 | Reescribir T1 de h08 contra el contrato v2 (no v1) | [`ep-01/h08.md`](../tareas/ep-01/h08.md) T1 (rework) | P1 | — (decidido: `v1` histórico, [checklist §2.5](../entregas/checklist-cierre-s1.md)) | 2 | Cierre S1 | Pendiente — ya desbloqueada |
| 11 | Prueba automatizada de reinicio de Compose | [`ep-01/h09.md`](../tareas/ep-01/h09.md) T4 | Todas (1 persona) | 1 (Eureka no es prerequisito real de esto) | 3 | Cierre S1 | Pendiente |
| 12 | Guía de demo + activar gate de cobertura JaCoCo | [`ep-01/h09.md`](../tareas/ep-01/h09.md) T6 | Todas (1 persona) | 1–11 (necesita todo lo demás terminado para poder escribirla) | 2 | Cierre S1 | Pendiente |
| 13 | Alta real de adaptador (`POST /admin/model-adapters`) | [`ep-02/model-catalog-real.md`](../tareas/ep-02/model-catalog-real.md) T4 | P2 | Fila 8 (T1) | 12 | Ampliación | Aceptada 2026-09-12 |
| 14 | Reescribir H05/H06/H07 + verificación nueva contra v2 | [`ep-03/rewrite-h05-h06-h07-v2.md`](../tareas/ep-03/rewrite-h05-h06-h07-v2.md) | P1 | Fila 3 (T9, para que el `404` de la ficha reescrita sea real) | 14 | Ampliación | Aceptada 2026-09-12 |
| | | | | **Total (con ampliación)** | **100** | | |

**Actualización 2026-09-12:** la fila 8 bajó de 12 h a 8 h al descartar su T2 (leyendo el código
se confirmó que la fila 7 no la necesita — ver [`model-catalog-real.md`](../tareas/ep-02/model-catalog-real.md)).
Total del sprint: 104 h → **100 h**.

**Ampliación decidida el 2026-09-12** ([`backlog-priorizado-cierre-s1.md`](../entregas/backlog-priorizado-cierre-s1.md)):
de las 4 candidatas que usaban el margen de capacidad sobrante, se aceptaron las filas 13 y 14
—porque una repara una capacidad real del producto (RF-IA-11) y la otra repara un riesgo de
credibilidad documental—. **Quedaron para el siguiente ciclo**, sin apuro porque ninguna tiene
costo de postergarlas: formalizar T3/T5 de [`ep-01/h09.md`](../tareas/ep-01/h09.md) (contrato
WireMock + evidencia BDD) y unificar "cohorte"/"curso" en el resto de la documentación
([checklist §3.2](../entregas/checklist-cierre-s1.md)).

**Por qué este orden (vista técnica, no de valor):** Eureka (1) es la única dependencia real para
correr la demo en ambiente integrado, así que va primero aunque no sea lo más vistoso. La fila 7
(conectar calibración) es independiente de 1–6 — puede arrancar en paralelo desde el día 1, no
tiene que esperar a que P1 termine. La fila 8 tiene una pregunta abierta que puede reducir su
alcance a la mitad; no bloquear el resto del sprint esperando esa respuesta. Las filas 11–12 van
al final porque necesitan que el resto exista para poder probarlo/documentarlo.

| Dependencia | Responsable interno / contraparte | Necesaria para | Fecha requerida | Contrato / evidencia | Estado / acción si falta |
|---|---|---|---|---|---|
| D01 — Eureka/Gateway disponible | P1 / equipo de plataforma compartida | Fila 1 | Antes de la demo real (no antes de arrancar) | [08 · decisiones · A-4](../08-decisiones-y-pendientes.md) | **Ya no bloquea el arranque (2026-09-12):** la fila 1 se desarrolla y prueba contra el Eureka local de `compose.yaml` mientras se espera confirmación — mismo criterio que A-4. Sigue pendiente **solo** para la validación final de que el Gateway compartido rutea de verdad |
| ~~Respuesta técnica: ¿`function_model_config` o `model_deployment`?~~ | — | ~~Filas 7 y 8~~ | — | [`s03-h01.md`](../tareas/ep-04/s03-h01.md) T7, nota | ✅ Resuelta el 2026-09-12 leyendo el código: `function_model_config`. Las filas 7 y 8 quedaron independientes entre sí |
| ~~Decisión sobre `llm-service-v1.openapi.yaml`~~ | ~~Equipo completo~~ | ~~Fila 10~~ | — | [`checklist-cierre-s1.md` §2.5](../entregas/checklist-cierre-s1.md) | ✅ Resuelta el 2026-09-12: histórico. Fila 10 ya puede arrancar |

- [ ] El trabajo cabe en la capacidad individual y total (con la ampliación: **P1 ≈66 h**, **P4
      14 h**, **P2 ≈20 h**, todas sobre ~116 h/pareja — **cabe con margen**, ver
      [`backlog-priorizado-cierre-s1.md` · veredicto de capacidad](../entregas/backlog-priorizado-cierre-s1.md)).
- [ ] Existe un caso de uso completo, no solo tareas de infraestructura: la fila 7 (calibración)
      es el caso de uso real de la Entrega 1; el resto es habilitador/deuda.
- [ ] Las integraciones comprometidas tienen disponibilidad comprobada o acuerdo explícito: D01
      (Eureka/Gateway) queda anotado como riesgo si no se confirma en el día 1.
- [ ] Un mock no se confunde con evidencia de integración finalizada: la fila 9 es explícitamente
      un mock, no reemplaza la fila 1 (Eureka real).
- [ ] Las mejoras acordadas en la retro anterior consumen capacidad asignada: no aplica (primer
      registro de este tipo de sprint).

## 5. Seguimiento y reserva

| Fecha | Bloqueo / decisión / actividad no prevista | Responsable | Horas-persona | Impacto en objetivo | Próxima acción y fecha |
|---|---|---|---:|---|---|
| | | | | | |

| Control | Planificado | Real al cierre |
|---|---:|---:|
| Disponibilidad | | |
| Reuniones programadas | | |
| Soporte conocido | | |
| Reserva | | |
| Entregables | 100 h (referencia técnica, con ampliación) | |

## 6. Review y demo

| Evidencia | Registro |
|---|---|
| Versión desplegada / PR / ambiente | |
| Datos de prueba y usuario autorizado | |
| Pasos reproducibles del recorrido | Ver guía de demo de la fila 12 una vez escrita |
| Resultado esperado y observado | Calibración encolada contra el adaptador fake cierra en `PASSED`/`FAILED` con MAE y error máximo visibles |
| Pruebas de contrato, integración y regresión | |
| Fallas, permisos e idempotencia probados | |
| Evidencia de calidad del modelo, si aplica | MAE ≤ 5, error máximo por dimensión ≤ 10 (PAR-14) |
| Feedback y nuevas historias | |
| Historias aceptadas / no terminadas | |

- [ ] Interfaz y persistencia reales; recorrido completo.
- [ ] Consumidores incluidos en el compromiso integrados realmente.
- [ ] Contratos y migraciones coinciden con la implementación.
- [ ] Pruebas aplicables pasan; incrementos anteriores funcionan.
- [ ] Recuperación y documentación disponibles.
- [ ] No se presenta como terminado trabajo parcial o sin evidencia.

## 7. Retrospectiva y siguiente Planning

| Pregunta | Registro |
|---|---|
| ¿Qué ayudó a terminar el objetivo? | |
| ¿Qué consumió más tiempo del previsto? | |
| ¿Qué dependencias bloquearon y durante cuánto tiempo? | |
| ¿Cuánto trabajo quedó sin terminar y qué falta exactamente? | |
| Mejora concreta / responsable / fecha | |
| Horas necesarias para aplicar la mejora | |
| Ajuste de capacidad y previsión del producto | |
