# Sprint 1 — propuesta del equipo (recomendación, no entregable)

> **Estado:** borrador para la **Planning de S1**. Sigue la
> [plantilla de sprint LLM](../plantillas/sprint-llm.md); los valores de referencia **no
> acreditan** asistencia, horas reales ni funcionalidad terminada. Los números reales de
> capacidad salen de la planilla de disponibilidad en el **Sprint 0**
> ([sprints/sprint-0.md · §4](../sprints/sprint-0.md)).
>
> **Fuentes:** receta de S1 en [`Plan de ejecucion/07`](<../../Plan de ejecucion/07-backlog-ejecutable-sprints.md>) ·
> historias en [`historias/`](../historias/README.md) (índice del sprint en [`sprints/s1-historias.md`](../sprints/s1-historias.md)) · épicas [`ep-01.md`](../epicas/ep-01.md) /
> [`ep-03.md`](../epicas/ep-03.md) · DoR/DoD [23 · §9.2](../23-plan-construccion-producto-llm.md).

---

## 1. Identificación y objetivo

| Campo | Valor propuesto |
|---|---|
| Sprint / fase | **S1 / F1** (Tutoría y evaluación académica) |
| Inicio y cierre del ciclo | *(fijar en Planning; 2 semanas)* |
| Fecha de Planning previa a ejecución | *(fijar; sus 24 h-persona se cargan a S1)* |
| Objetivo y usuario beneficiado | Un **docente autorizado** carga y consulta casos de referencia (*golden set*) y **los datos sobreviven al reinicio** del servicio. |
| Recorrido funcional que se demostrará | Acceso autorizado por Gateway → alta de golden set para la cohorte → carga de una entrada (transcripción + 5 puntajes) → `docker compose restart` → consulta que devuelve exactamente lo cargado. |
| Límites del incremento | Solo golden set base (sin doble puntuación ciega ni publicación de versiones: eso es S2). Sin funciones de IA (tutor/evaluador). Un solo idioma (`es`), rúbrica 1.0. |
| Versión anterior que debe seguir funcionando | Ninguna (es el primer incremento). |
| Referente de producto / facilitador | *(nombrar en Sprint 0)* |
| Ambiente de integración | Ambiente compartido con Gateway, Eureka y PostgreSQL reales (no máquina local aislada). |

**Trabajo estimado de la receta:** ~208 h de paquetes (piso, no tope) sobre una capacidad de
referencia de ≈ 571 h.

---

## 2. Capacidad individual

Se completa en el Sprint 0 con la planilla de disponibilidad de los 12 integrantes. Acá van
solo los **valores de referencia**.

| Integrante | Pareja / suplente | Disponibilidad declarada (h) | Reuniones (h) | Soporte conocido (h) | Base restante (h) | Reserva 20% (h) | Entregables (h) |
|---|---|---:|---:|---:|---:|---:|---:|
| 1–12 | P1–P5 + referente + facilitador | *(planilla)* | *(planilla)* | *(planilla)* | *(planilla)* | *(planilla)* | *(planilla)* |
| **Referencia del sprint** | — | **816** | **102** | *(según sprint)* | **714** | **143** | **≈ 571** |

```text
Base        = disponibilidad − reuniones − soporte conocido
Reserva     = 20 % de la Base
Entregables = máximo(0, Base × 0,80)   → referencia sin soporte conocido: (816 − 102) × 0,80 ≈ 571 h
Trabajo estimado de la receta S1: ~208 h (piso). Margen ≈ 363 h.
```

Referencia por pareja: **≈ 114 h** (571 ÷ 5), no 571 para cada una. Revisar bases
individuales negativas; no trasladarlas en silencio.

---

## 3. Reuniones del ciclo

| Reunión | Cantidad | Minutos por sesión | Asistentes internos | Referencia horas-persona | Fecha(s) | Horas-persona reales |
|---|---:|---:|---:|---:|---|---:|
| Planning | 1 | 120 | 12 | 24 | | |
| Daily / sincronización (incl. dependencias) | 4 | 45 | 12 | 36 | | |
| Review con demo | 1 | 90 | 12 | 18 | | |
| Retrospectiva | 1 | 60 | 12 | 12 | | |
| Refinamiento | 2 | 30 | 12 | 12 | | |
| **Total programado** | | | | **102** | | |

La Planning se carga a este sprint aunque ocurra el día anterior al comienzo operativo. La
demo pertenece a la Review.

---

## 4. Historias propuestas y dependencias

Derivadas de la receta de S1. Épica **EP-01** (H01–H04, H08, H09) y **EP-03** (H05–H07). La
columna *h* es la referencia de planificación; los **puntos Fibonacci** se asignan en
Planning contra la canónica `LLM-S01-H06`.

| ID | Usuario y resultado | RF / contrato | Aceptación (incluye negativa) | Responsable / suplente | Dependencias | Est. (h) | Puntos | Estado |
|---|---|---|---|---|---|---:|---|---|
| `LLM-S01-H01` | *(equipo)* ADR con árbol de módulos y convenciones | RF-NFR-01/09/10 | ADR registrado (Java 21, Boot/Maven, límites de paquetes, variables de entorno); `domain` sin Spring/Kafka/proveedores. **Neg:** import prohibido en `domain` → CI/revisión lo rechaza | **P1** / *(suplente)* | — | 16 | *(Planning)* | Pendiente |
| `LLM-S01-H02` | *(desarrollador)* levantar el entorno con un comando | RF-NFR-03/04 | `docker compose up` deja todo *healthy*; `up`/health/`down` documentados. **Neg:** sin Docker falla con mensaje claro; puerto ocupado nombra el conflicto | **P1** | H01 | 30 | *(Planning)* | Pendiente |
| `LLM-S01-H03` | *(plataforma)* esqueleto transversal por el Gateway | RF-NFR-01/03/04; contratos v1 | Eureka `llm-service`, `/api/llm/**` sin reescritura, M2M `aud=llm-service`, Problem Details, correlación, Actuator, CI verde. **Neg:** token sin scope → `401`; identidad falsificada → `403` | **P1** | H02 | 34 | *(Planning)* | Pendiente |
| `LLM-S01-H04` | *(plataforma)* esquema inicial versionado con auditoría | RF-IA-30 a 36; RF-NFR-01 | Flyway desde base vacía crea rúbrica 1.0 (5 dimensiones/pesos), golden set, entrada, idempotencia y auditoría; reproducible. **Neg:** `UPDATE`/`DELETE` en tabla append-only rechazado; puntuación incompleta o fuera de rango rechazada | **P1** | H03 | 38 | *(Planning)* | Pendiente |
| `LLM-S01-H05` | **docente autorizado** da de alta un golden set y carga entradas | RF-IA-30 a 36; [adenda golden set](../contracts/llm-service-v1-s1-golden-set-adenda.md) | Alta y carga por Gateway; valida actor, ownership de cohorte e idempotencia. **Neg:** misma `Idempotency-Key` no duplica; docente de otra cohorte → `403`; `referenceScores` incompleto → `400` | **P5** / *(suplente)* | H04 | 24 | *(Planning)* | Pendiente |
| `LLM-S01-H06` *(canónica)* | **docente autorizado** consulta su golden set aunque el servicio se reinicie | RF-IA-30 a 36; adenda golden set | Devuelve lo cargado tras `docker compose restart`; paginado y ordenado por fecha desc. **Neg:** cohorte ajena no aparece; `goldenSetId` inexistente → `404` (no `500`); `size=500` → `400` | **P5** | H04 | 14 | **estimar primero** | Pendiente |
| `LLM-S01-H07` | **docente** usa una pantalla mínima para alta, carga y consulta | RF-IA-30 a 36 | Formulario/listado real por Gateway, estados de carga/error, autorización visible, WCAG AA. **Neg:** backend caído → aviso claro y navegable; transcripción no-JSON no se envía; `403` → «no autorizado» | **P5** | H05, H06 | 24 | *(Planning)* | Pendiente |
| `LLM-S01-H08` | *(equipo de integración)* contrato OpenAPI y mock del golden set publicados | contratos v1; RF-NFR-01 | OpenAPI con **solo** operaciones existentes; mock con una línea documentada; adenda revisada con `admin-service`. **Neg:** PR con operación no implementada o campo no acordado → bloqueado | **P1** | H03 | 10 | *(Planning)* | Pendiente |
| `LLM-S01-H09` | *(equipo)* suite de pruebas y guía de demo de S1 | [24](../24-convenciones-cobertura.md); [25](../25-matriz-pruebas-infraestructura.md) | Unitarias + Testcontainers/Flyway + WireMock del Gateway + prueba de reinicio de Compose; cobertura ≥ 95 %; guía de demo reproducible. **Neg:** cobertura bajo umbral → CI falla; pérdida de datos en reinicio → falla y bloquea la Review | **P1** (una persona por pareja) | H04–H07 | 18 | *(Planning)* | Pendiente |
| | | | | | **Total** | **208** | | |

### Dependencias

| Dep. | Contraparte / referente LLM | Necesaria para | Fecha requerida | Contrato / evidencia | Estado / acción si falta |
|---|---|---|---|---|---|
| **D01** | Gateway / Eureka / ambiente / frontend compartido · P1 | H02, H03, H07 | Primeros días de S1 | Ruta `/api/llm/**`, `aud=llm-service`, scopes, headers de confianza | Bloquea el borde: sin esto, H03 y H07 no cierran demo real |
| **D02** | Docentes + cátedra · P5 + P4 | H04, H05 | Antes de migrar `V1` | Definición de las 5 dimensiones y pesos de la rúbrica 1.0 | Si demora: usar rúbrica 1.0 provisional y congelar antes de publicar contrato |
| **D03** | `practice-service` · P3 | *(solo contrato en S1)* | Contrato en S1 | Contexto/solución/metadata (se integra en S5) | No bloquea S1; se deja el contrato acordado |
| **D04** | `challenges-service` / `courses-service` · P1 + P4 | *(solo contrato en S1)* | Contrato temprano en S1 | `intento_cerrado.v1` y regla de cierre (se integra en S6) | No bloquea S1; se deja el contrato acordado |

### Checklist de compromiso (DoR del sprint)

- [ ] El trabajo cabe en la capacidad individual y total (revisar tras estimar en puntos).
- [ ] Existe un caso de uso completo (H05–H07), no solo infraestructura.
- [ ] D01 y D02 tienen responsable y disponibilidad comprobada.
- [ ] Un mock (H08) no se confunde con integración terminada.
- [ ] La estimación incluye esqueleto, migración, contrato, interfaz, pruebas e integración.
- [ ] Cada HU tiene BDD de camino feliz + 2 negativos y responsable/suplente.

---

## 5. Seguimiento y reserva

| Fecha | Bloqueo / decisión / actividad no prevista | Responsable | Horas-persona | Impacto en objetivo | Próxima acción y fecha |
|---|---|---|---:|---|---|
| | | | | | |

| Control | Planificado | Real al cierre |
|---|---:|---:|
| Disponibilidad | *(planilla)* | |
| Reuniones programadas | 102 | |
| Soporte conocido | *(según sprint)* | |
| Reserva | ≈ 143 | |
| Entregables | ≈ 571 (trabajo estimado ~208) | |

Si se agota la reserva, registrar la renegociación del alcance. No se rebajan pruebas ni
controles académicos para cumplir fecha.

---

## 6. Review y demo

| Evidencia | Registro (completar en la Review) |
|---|---|
| Versión desplegada / PR / ambiente | |
| Datos de prueba y usuario autorizado | Docente autorizado sobre su cohorte; fixture de transcripción sintético etiquetado |
| Pasos reproducibles del recorrido | 1) login docente por Gateway · 2) `POST /api/llm/golden-sets` · 3) `POST …/entries` con 5 puntajes · 4) `docker compose restart` · 5) `GET …/{id}` devuelve lo cargado |
| Resultado esperado y observado | La entrada consultada tras el reinicio es idéntica a la cargada | 
| Pruebas de contrato, integración y regresión | WireMock del Gateway · Testcontainers/Flyway · prueba automatizada de reinicio de Compose |
| Fallas, permisos e idempotencia probados | `401`/`403`, segunda `Idempotency-Key`, rúbrica inválida, `404` vs `500` |
| Evidencia de calidad del modelo | No aplica en S1 (sin funciones de IA) |
| Feedback y nuevas historias | |
| Historias aceptadas / no terminadas | |

- [ ] Interfaz y persistencia reales; recorrido completo.
- [ ] `admin-service` (consumidor del contrato) al tanto de la adenda S1.
- [ ] Contratos y migración coinciden con lo desplegado.
- [ ] Pruebas aplicables pasan; cobertura ≥ 95 %.
- [ ] Guía de recuperación (reinicio) documentada.

---

## 7. Retrospectiva y siguiente Planning

| Pregunta | Registro (completar en la Retro) |
|---|---|
| ¿Qué ayudó a terminar el objetivo? | |
| ¿Qué consumió más tiempo del previsto? | |
| ¿Qué dependencias bloquearon y cuánto tiempo? | |
| ¿Cuánto trabajo quedó sin terminar y qué falta exactamente? | |
| Mejora concreta / responsable / fecha | |
| Horas necesarias para aplicar la mejora | |
| Ajuste de capacidad y previsión (revisar tras S2) | |

El trabajo pendiente se reestima por lo que falta y consume capacidad de S2. La Planning de
S2 se contabiliza una sola vez, en S2.
