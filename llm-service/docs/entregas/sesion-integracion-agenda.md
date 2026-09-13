# Sesión de integración — agenda y tabla de pendientes

> **Qué es.** Agenda ejecutable para la sesión de integración entre equipos. Consolida los
> ítems que [17 — Mapa de integración §8](../17-mapa-de-integracion.md#8-lo-que-estos-diagramas-dejaron-a-la-vista)
> (tabla I-01 a I-16) y [18 — Contratos inter-equipos §6](../18-contratos-inter-equipos.md#6-agenda-mínima-para-la-sesión-de-integración)
> marcan como **"decide: sesión de integración"**, ordenados para correr la reunión, no solo
> para leerlos.
>
> **Por qué existe.** Las dos tablas de origen están para *documentar* el pendiente. Esta
> agrega lo que falta para *usarlas en una reunión*: quién tiene que estar, en qué orden, con
> qué propuesta de apertura, y un lugar para anotar lo que se decidió — para no tener que
> volver a los 17 documentos después.
>
> **Cómo se usa.** Se corre en el orden de la sección 2. Cada ítem tiene una propuesta
> nuestra para abrir la discusión (no es la decisión, es el punto de partida) y una fila
> **Decidido:** vacía para completar en vivo. Al cerrar la sesión, los "Decidido" se vuelcan a
> [18 — Contratos inter-equipos](../18-contratos-inter-equipos.md) y, si tocan el schema, a
> los YAML de [`docs/contracts/`](../contracts/) — ver checklist en la sección 5.
>
> **Fecha de esta versión:** 2026-09-12. Fuente: doc 17 §8 y doc 18 §6, sin cambios de fondo.

---

## 1. Quién tiene que estar

| Ítem | Equipos imprescindibles | Opcional |
|---|---|---|
| I-04 | Tema 07 (nosotros), **Tema 03** | Backend de negocio |
| I-05 | Tema 07, **Tema 11** | — |
| I-08 | Tema 07, **Tema 11**, P5 | — |
| I-09 | Tema 07, **Tema 02** | — |
| I-14 | Tema 07 + **todos los equipos que llaman a un servicio ajeno** | API Gateway (Tema 01) |
| I-06 | Tema 07, **Product Owner / ADMIN** | — |
| I-15 | **Product Owner**, Front End, Tema 07, Tema 12 | — |

Si algún equipo de la columna "imprescindibles" no puede venir, ese ítem se pospone — no se
decide con la mitad de la mesa. Es justamente lo que dice doc 18 §0: nadie llega directo, y
un contrato acordado sin el otro lado es un contrato inventado.

---

## 2. Orden de la sesión

Los primeros cuatro son los que doc 17 marca explícitamente como **"los cuatro que van
primero"**: I-04 porque bloquea a un equipo entero, I-05 e I-09 porque cierran contratos que
después no se renegocian, I-08 porque el dato no capturado hoy no se recupera nunca.

### 🔴 1. I-04 — Cómo llega el score al motor de desafíos

**Timebox:** 15 min. **Bloquea:** Tema 03 no puede empezar su lado.

- **El problema:** hay cuatro mecanismos escritos (evento Kafka, callback HTTP, polling,
  consulta directa a base) y ninguno tiene payload definido.
- **Nuestra propuesta de apertura:** evento Kafka `score_de_ia_calculado.v1` (ya en
  [`contracts/llm-service-v1.asyncapi.yaml`](../contracts/llm-service-v1.asyncapi.yaml)),
  payload de doc 18 §2.1. Un solo mecanismo, el que ya es contrato ejecutable.
- **Lo que no se puede resolver hoy:** si Tema 03 insiste en callback síncrono, tiene que
  quedar explícito que es *su* llamada hacia nosotros (`GET /ai/jobs/{job_id}` o
  `GET /api/llm/evaluations/{evaluationId}`), no al revés.

**Decidido:** _____________________________________________

---

### 🔴 2. I-05 — Qué enum viaja en `estado`

**Timebox:** 10 min. **Bloquea:** el contrato de eventos queda ambiguo si no se cierra.

- **El problema:** el campo `estado` aparece en request/response y en eventos sin un enum
  cerrado (doc 17 §7.2 lo señala igual para "los dos").
- **Nuestra propuesta:** `completado | en_proceso | fallido | aplicado | diferido` — cubre
  los estados de job (doc 18 §1.4) y los de evento (`score_de_ia_calculado.estado: aplicado`,
  §2.1). Un solo enum, no uno por endpoint.

**Decidido:** _____________________________________________

---

### 🔴 3. I-09 — `curso_id` vs `curso_cohorte_id` vs `curso_template_id`

**Timebox:** 15 min. **Bloquea:** si Tema 02 modela sin esta clave, después no hay forma de
acotar sin migrar datos.

- **El problema:** tres nombres para relaciones potencialmente distintas — de qué cuelga el
  chunk del RAG, de qué cuelga la calibración.
- **Nuestra propuesta (ya recomendada en doc 08, ítem B-7):** el material didáctico cuelga
  del **template** (se reutiliza entre cohortes); calibración y evaluaciones son de la
  **cohorte**. El chunk lleva las dos claves.
- **Confirmar con Tema 02** el ejemplo textual de la cátedra: *"calibración que se copia pero
  se reaprueba"* al clonar un curso — si eso no es así del lado de Tema 02, la propuesta cae.

**Decidido:** _____________________________________________

---

### 🔴 4. I-08 — Esquema de la tabla `mensaje`

**Timebox:** 15 min. **Bloquea:** es el único ítem de la lista que se pierde para siempre si
se posterga (dato no capturado no se recupera).

- **El problema:** tres versiones incompatibles escritas en distintos documentos.
- **Quién lo trae preparado:** P5 tiene el ítem asignado para resolver esta semana (doc 17
  tabla I-08) — llega a la sesión con una propuesta concreta, no se diseña en vivo.
- **Lo que Tema 07 necesita que quede adentro:** los campos que alimentan moderación y
  auditoría — `trace_id`, timestamp, autor, y espacio para el veredicto del moderador
  (doc 18 §4.4) sin que el moderador termine dueño de la tabla.

**Decidido:** _____________________________________________

---

### 🔴 5. I-14 — Autenticación entre servicios

**Timebox:** 15 min. **Bloquea:** hoy ningún endpoint dice qué rol puede llamarlo.

- **El problema:** "token interno" mencionado sin formato, header ni emisor. El único
  OpenAPI publicado (`llm-service-v1.openapi.yaml`) no declara `security` para M2M.
- **Nuestra propuesta:** JWT M2M con `aud=llm-service`, propagado por el API Gateway, nunca
  parámetros de cliente para identidad (regla ya fija en doc 18 §0 y §7). Falta acordar: qué
  claim identifica al servicio llamante, y si el gateway firma o solo reenvía.
- **Regla que no se negocia en esta sesión** (ya es ADR-001/ADR-015): nadie llega directo a
  `llm-service`, todo pasa por el gateway.

**Decidido:** _____________________________________________

---

### 🟡 6. I-06 — Techo de cuota diaria (RF-IA-22)

**Timebox:** 10 min. **Cambia:** el presupuesto del cuatrimestre.

- **El problema:** tres números distintos circulando — 15 (decisión), 10 (inventario), 8
  (presupuesto).
- **Nuestra propuesta (doc 08, P-05):** 15 mensajes/desafío, 60/día. Empezar ahí y calibrar
  con datos reales.
- **Quién decide en la sala:** Product Owner / ADMIN — nosotros no cerramos este número
  solos.

**Decidido:** _____________________________________________

---

### 🟡 7. I-15 — Quién construye la pantalla del golden set

**Timebox:** 10 min. **Destraba:** el ítem de plazo más largo del proyecto.

- **El problema:** cuatro documentos le asignan cuatro dueños distintos.
- **Nota:** esto no es solo un pendiente técnico — cuelga del mismo bloqueo que **P-04 /
  C-1** (doc 08): sin alguien nombrado para producir el golden set *y* construir su pantalla,
  ningún curso activa. Si el Product Owner no puede resolver los dos en la misma sesión,
  priorizar el **responsable del contenido** (P-04) por sobre la pantalla — la pantalla sin
  contenido no sirve, el contenido sin pantalla se puede cargar a mano una vez.

**Decidido:** _____________________________________________

---

## 3. Lo que NO entra a esta sesión

Para no perder tiempo de equipos ajenos en cosas que resolvemos solos o que dependen de un
tercero que no está en la sala:

| Ítem | Por qué no | Dueño real |
|---|---|---|
| I-01, I-02, I-03, I-10, I-12, I-13 | Doc 17 los marca "Nosotros" | Tema 07, sin la sala |
| I-11 | Asignado a P1 | P1, sin la sala |
| I-07 | Ya cerrado (corrector fuera de alcance) | — |
| I-16 | Es consulta legal sobre el free tier del moderador | Legal / PO, no una sesión técnica |
| P-04 / C-1 (golden set: quién y para cuándo) | Es agenda de Product Owner, no de equipos técnicos — pero condiciona a I-15 (ver arriba) | Product Owner |

---

## 4. Riesgo si la sesión no cierra algo

Repetido de doc 17 porque es el argumento para no salir de la sala sin decidir I-04/I-05/I-09:
**una vez cerrado un contrato de eventos, pedir un campo nuevo es renegociar con los cinco
equipos que ya lo consumen** (doc 18 §3, nota sobre Tema 11). El costo de no decidir hoy no es
"decidir mañana" — es decidir después con más gente en la mesa.

---

## 5. Checklist de cierre (después de la sesión)

- [ ] Volcar cada fila "Decidido" de la sección 2 a la tabla de doc 18 §6.
- [ ] Si I-04/I-05 cambian un payload: actualizar
      [`llm-service-v1.asyncapi.yaml`](../contracts/llm-service-v1.asyncapi.yaml) y avisar a
      los consumidores listados en doc 18 §2.
- [ ] Si I-09 fija la clave del chunk: actualizar el esquema antes de escribir código de
      ingesta (evita migración de datos después).
- [ ] Si I-14 fija el formato del token: agregar `security` al
      [`llm-service-v1.openapi.yaml`](../contracts/llm-service-v1.openapi.yaml).
- [ ] Si I-08 cierra el esquema de `mensaje`: avisar a P5 y actualizar
      [`llm-service-v1-moderacion-borrador.yaml`](../contracts/llm-service-v1-moderacion-borrador.yaml)
      si corresponde (sigue siendo borrador, EP-08 no arrancó).
- [ ] Actualizar la fecha y el estado de I-04/I-05/I-08/I-09/I-14 en
      [17 — Mapa de integración §8](../17-mapa-de-integracion.md).
- [ ] Si I-06 o I-15 quedaron sin cerrar, agendar seguimiento puntual con el Product Owner —
      no vuelven a esta agenda, van a doc 08 Parte B/C.

---

*Este documento no decide nada nuevo — ordena para la reunión lo que doc 17 §8 y doc 18 §6 ya
tenían escrito. Las propuestas de apertura son recomendaciones de Tema 07, no acuerdos.*
