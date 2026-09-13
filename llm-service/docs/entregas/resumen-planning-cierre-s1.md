# Resumen para la Planning — Sprint de cierre de S1 + Entrega 1

> **Qué es este documento.** Una página para llevar directo a la Planning: lo que ya está resuelto
> (para no volver a discutirlo en la reunión), el backlog con sus horas, y lo poco que falta
> completar en vivo. El detalle completo de cada punto vive en los documentos de origen — este
> resumen no reemplaza a ninguno, solo evita tener que abrir seis archivos durante la reunión.
>
> **Fuentes:** [`sprints/s1-cierre.md`](../sprints/s1-cierre.md) (registro completo),
> [`entregas/backlog-priorizado-cierre-s1.md`](backlog-priorizado-cierre-s1.md) (las dos vistas de
> orden), [`entregas/checklist-cierre-s1.md`](checklist-cierre-s1.md) y
> [`entregas/entrega-1.md`](entrega-1.md) (de dónde salió cada ítem),
> [`08-decisiones-y-pendientes.md`](../08-decisiones-y-pendientes.md) (decisiones ratificadas).
>
> **Fecha de esta versión:** 2026-09-12.

---

## 1. Objetivo, en una frase

> Un docente autorizado publica rúbrica y golden set de su curso, encola una calibración contra el
> adaptador simulado (H10), y la calibración cierra con `PASSED`/`FAILED` y su métrica visible —
> sin haber gastado un centavo en un proveedor real.

No es la demo original de S1 (golden set simple) — es más, porque el código ya construyó más de lo
que S1 pedía. Detalle de por qué en [`entrega-1.md`](entrega-1.md).

---

## 2. Decisiones ya resueltas — no discutir de nuevo en la Planning

| Decisión | Resolución |
|---|---|
| ¿`function_model_config` o `model_deployment` para la calibración? | `function_model_config` — confirmado leyendo el código, no bloquea nada |
| ¿Qué pasa con `llm-service-v1.openapi.yaml`? | Histórico, reemplazado por v2 |
| D01 (Eureka/Gateway) no confirmado todavía | **No bloquea el arranque** — se desarrolla contra el Eureka local de `compose.yaml` (A-4); solo la validación final de demo depende de la confirmación real |
| A-5, P-02/C-5, P-05, P-07, P-08, P-09/P-10/P-11 | Ratificadas el 2026-09-12, ver [08](../08-decisiones-y-pendientes.md) |

**Lo único que de verdad sigue abierto y puede afectar este sprint puntual:** confirmar D01 con
quien mantiene la plataforma compartida — no para arrancar, sí para la demo final en ambiente
integrado.

**Decisiones que siguen abiertas pero NO son de este sprint** (no las traigan a esta Planning, van
a la sesión de integración o al PO por separado): A-1/A-2/A-3, B-1 a B-7, C-1 (golden set), C-2
(free tier, legal), **C-6 (¿EP-06 entra en esta entrega? — cambia ~416 h, ver
[`ep-06/evaluacion-y-apelacion.md`](../estado-implementacion/ep-06/evaluacion-y-apelacion.md))**.

---

## 3. El backlog — 14 ítems, 100 h

| # | Ítem | Responsable | h | Bloque |
|---:|---|---|---:|---|
| 1 | Registro en Eureka (contra local mientras se confirma D01) | P1 | 5 | Cierre S1 |
| 2 | `401` real para token sin scope | P1 | 6 | Cierre S1 |
| 3 | `404` real para "no existe" | P1 | 3 | Cierre S1 |
| 4 | Pipeline CI en verde | P1 | 4 | Cierre S1 |
| 5 | ADR de arquitectura y convenciones | P1 | 16 | Cierre S1 |
| 6 | `.env.example` + documentar `down` | P1 | 7 | Cierre S1 |
| 7 | **Conectar calibración con H10** | P4 | 14 | 🔴 Entrega 1 |
| 8 | Catálogo de adaptadores real (lectura) | P2 | 8 | Entrega 1 |
| 9 | Mock levantable + check CI de contrato | P1 | 4 | Cierre S1 |
| 10 | Marcar `v1.openapi.yaml` histórico, validar v2 | P1 | 2 | Cierre S1 |
| 11 | Prueba automatizada de reinicio de Compose | Todas (1 persona) | 3 | Cierre S1 |
| 12 | Guía de demo + gate de cobertura JaCoCo | Todas (1 persona) | 2 | Cierre S1 |
| 13 | Alta real de adaptador (`POST /admin/model-adapters`) | P2 | 12 | Ampliación |
| 14 | Reescribir H05/H06/H07 + verificación nueva | P1 | 14 | Ampliación |

**Orden de ejecución (por dependencia técnica, no por número de fila):** 1→2→3→4 (mismo módulo,
secuencial) mientras 7 arranca en paralelo desde el día 1 (no depende de nada de lo anterior); 8 y
13 en paralelo entre sí; 5, 6, 9, 10 sin orden estricto; 11 y 12 al final, porque necesitan que el
resto exista para poder probarlo/documentarlo. Detalle completo en
[`backlog-priorizado-cierre-s1.md` · Vista 2](backlog-priorizado-cierre-s1.md#vista-2--equipo-orden-por-dependencia-técnica-real).

---

## 4. Capacidad

| Pareja | h de este sprint | Capacidad de referencia | % ocupado |
|---|---:|---:|---:|
| P1 | 66 | ~116,3 h | ~57% |
| P4 | 14 | ~116,3 h | ~12% |
| P2 | 20 | ~116,3 h | ~17% |
| **Total** | **100** | 581,6 h (equipo completo) | — |

Nadie supera el 60% de su capacidad — entra en 2 semanas con margen real para imprevistos, sin
frenar el resto del trabajo en curso (EP-04 más allá de la fila 7, EP-05).

---

## 5. Lo que falta completar EN VIVO durante la Planning (esto sí es tarea de la reunión)

- [ ] Fecha de inicio y cierre del ciclo (2 semanas).
- [ ] Nombres reales de quiénes son P1, P2 y P4 en este sprint puntual.
- [ ] Disponibilidad declarada de cada integrante (tabla de la sección 2 de
      [`s1-cierre.md`](../sprints/s1-cierre.md) — hoy con la referencia de 116,3 h/pareja, a
      confirmar contra la planilla real del ciclo).
- [ ] Referente de producto / facilitador de este sprint.
- [ ] Confirmar con quien mantiene la plataforma compartida si D01 (Eureka/Gateway) va a estar
      disponible antes del cierre del sprint — si no, se documenta la limitación y se demuestra en
      local (ya decidido, no hay que resolverlo en la reunión, solo confirmar la fecha).

## 6. Checklist de "listo para arrancar" (DoR)

- [x] El trabajo cabe en la capacidad total y por pareja.
- [x] Existe un caso de uso completo (fila 7), no solo infraestructura.
- [x] Las tres dependencias técnicas que tenía el sprint están resueltas o mitigadas.
- [ ] Disponibilidad individual declarada para el ciclo (sección 5 de arriba).
- [ ] Historias/ítems asignados a un responsable con nombre (hoy están por pareja, falta la
      persona).

---

*Con la sección 5 completada, este sprint está listo para pasar de "propuesta técnica" a
"comprometido". Nada de lo pendiente en esa sección es una decisión — es información que solo el
equipo tiene el día de la Planning.*
