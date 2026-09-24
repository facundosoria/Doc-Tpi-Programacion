# Guía de Carga en Taiga — Historias y Tareas de Sprint 1 (Grupo G03)

> **Propósito del documento:** Facilitar la carga manual y unificada de las Historias de Usuario (HU) y Tareas de Sprint 1 en el backlog de **Taiga**, permitiendo identificar de forma unívoca a qué **Épica** pertenece cada ítem, qué rol cumple, su tipo (HU de valor vs. Tarea técnica habilitadora) y el contenido listo para copiar y pegar en el formulario de Taiga.
>
> **Ámbito:** `llm-service` (Tema 07 — Plataforma de Aprendizaje Gamificado de Programación).  
> **Identificador de Grupo:** `G03` (prefijo obligatorio según [27 · Guía Wiki Taiga](../../../07-guia-wiki-taiga.md)).  
> **Plantilla base oficial:** [plantillas/historia-de-usuario-taiga.md](../../../10-plantillas/historia-de-usuario-taiga.md).  
> **Historia canónica de referencia para Planning Poker:** `LLM-EP03-H02` (*Consulta del golden set persistente tras reinicio*).

---

## 1. Convenciones obligatorias para la carga en Taiga

1. **Título de la tarjeta:**
   - Debe iniciar obligatoriamente con el prefijo del grupo: `# G03 — <Título de la historia>` (o en el campo de texto simple: `G03 — <Título de la historia>`).
   - **No** incluir corchetes ni el código interno `LLM-Sxx-Hyy` en el título (el código interno va en la primera línea de la descripción).
2. **Vincular a la Épica correspondiente:**
   - En el desplegable **Epic** de Taiga, seleccionar la épica correspondiente (`EP-01`, `EP-02`, `EP-03`, etc.).
3. **Tipo de ítem en Taiga:**
   - **HU (Historia de Usuario de valor):** Tiene un rol de usuario final real (*docente* o *alumno*), genera valor funcional directo y se estima en **Puntos Fibonacci** mediante Planning Poker.
   - **Tarea (Habilitador técnico / Spike):** Sujeto técnico (*"Como equipo..."*, *"Como sistema..."* o *"Como chat-service..."*). No otorga valor directo a un usuario final pero habilita la arquitectura, base de datos o contratos inter-equipos.
4. **Asignación al Sprint:**
   - Asignar el ítem al **Sprint 1**.
5. **Criterios de Aceptación:**
   - En Taiga, mantener los criterios en una sola línea con `- [ ]` para evitar problemas con el motor de renderizado Markdown de Taiga.

---

## 2. Matriz Consolidada de Trazabilidad para Taiga

| Épica en Taiga | ID Interno | Título oficial a pegar en Taiga | Tipo | Pareja | Horas (Plan) | Rol (Como) |
|---|---|---|---|:---:|:---:|---|
| **EP-08 · Moderación (F2)** *(Incorporada)* | LLM-S11-H01 | `G03 — Que un mensaje del chat se permita o bloquee antes de entregarse` | Tarea (contrato) | P3+P2+P1 | ~34 h | Sistema / `chat-service` |
| **EP-08 · Moderación (F2)** *(Incorporada)* | LLM-S11-H02 | `G03 — Detectar spam, contenido ofensivo e intentos de ocultar código` | Tarea (motor) | P3 | ~28 h | Sistema (`llm-service`) |
| **EP-08 · Moderación (F2)** *(Incorporada)* | LLM-S12-H01 | `G03 — Apelar un mensaje que bloqueó la moderación` | **HU de valor** | P3 | ~26 h | Alumno del curso |
| **EP-08 · Moderación (F2)** *(Incorporada)* | LLM-S12-H02 | `G03 — Que el docente revise un incidente de moderación y lo resuelva` | **HU de valor** | P3 | ~30 h | Docente del curso |
| **EP-08 · Moderación (F2)** *(Incorporada)* | LLM-S13-H01 | `G03 — Que la moderación siga funcionando en modo degradado si el clasificador contextual falla` | Tarea (resiliencia) | P3+P2 | ~24 h | Sistema (`llm-service`) |
| **EP-08 · Moderación (F2)** *(Incorporada)* | LLM-S13-H02 | `G03 — Que la evidencia de moderación se retenga el tiempo necesario y luego se purgue` | Tarea (retención) | P1 | ~18 h | Sistema (auditoría) |
| **EP-01 · Plataforma** | LLM-EP01-H01 | `G03 — ADR de arquitectura y convenciones técnicas` | Tarea | P1 | 16 h | Equipo de desarrollo |
| **EP-01 · Plataforma** | LLM-EP01-H02 | `G03 — Entorno reproducible con un comando` | Tarea | P1 | 30 h | Equipo de desarrollo |
| **EP-01 · Plataforma** | LLM-EP01-H03 | `G03 — Esqueleto transversal del servicio` | Tarea | P1 | 34 h | Equipo de desarrollo |
| **EP-01 · Plataforma** | LLM-EP01-H04 | `G03 — Esquema inicial versionado con auditoría` | Tarea | P1 | 38 h | Equipo de desarrollo |
| **EP-01 · Plataforma** | LLM-EP01-H05 | `G03 — Contrato OpenAPI y mock del golden set publicados` | Tarea | P1 | 10 h | Equipo de desarrollo |
| **EP-01 · Plataforma** | LLM-EP01-H06 | `G03 — Suite de pruebas y guía de demo de S1` | Tarea | P1 (todos) | 18 h | Equipo de desarrollo |
| **EP-02 · AI Gateway** | LLM-EP02-H01 | `G03 — Puerto del proveedor de modelos (AI Gateway) y simulador para pruebas` | Tarea | P2 | 32 h | Equipo de desarrollo |
| **EP-02 · AI Gateway** | LLM-EP02-H02 | `G03 — Conectar un proveedor real detrás de la conexión con el modelo` | Tarea | P2 | 20 h | Equipo de desarrollo |
| **EP-03 · Golden Set** | LLM-EP03-H01 | `G03 — Alta de golden set y carga de entradas` | **HU de valor** | P5 | 24 h | Docente del curso |
| **EP-03 · Golden Set** | LLM-EP03-H02 | `G03 — Consulta del golden set persistente tras reinicio` | **HU (Canónica)** | P5 | 14 h | Docente del curso |
| **EP-03 · Golden Set** | LLM-EP03-H03 | `G03 — Pantalla docente mínima del banco de casos` | **HU de valor** | P5 | 24 h | Docente del curso |
| **EP-03 · Golden Set** | LLM-EP03-H04 | `G03 — Rúbrica versionada por curso` | **HU de valor** | P5 | 15 h | Docente del curso |
| **EP-03 · Golden Set** | LLM-EP03-H05 | `G03 — Banco de casos versionado por curso, con copia desde la base de plataforma` | **HU de valor** | P5 | 15 h | Docente del curso |
| **EP-04 · Calibración** | LLM-EP04-H01 | `G03 — Correr y activar una calibración de curso con métrica PAR-14` | **HU de valor** | P4 | 46 h | Docente del curso |
| **EP-04 · Calibración** | LLM-EP04-H02 | `G03 — Correr una calibración de plataforma y ver su reporte` | **HU de valor** | P4 | 20 h | Administrador |
| **EP-04 · Calibración** | LLM-EP04-H03 | `G03 — Que una calibración vigente venza cuando cambia la referencia` | **HU de valor** | P4 | 15 h | Docente / Admin |
| **EP-04 · Calibración** | LLM-EP04-H04 | `G03 — Enterarme de que mi curso tiene evaluaciones frenadas` | **HU de valor** | P4 | 20 h | Docente del curso |
| **EP-05 · Tutor Seguro** | LLM-EP05-H01 | `G03 — Recibir una respuesta socrática del tutor, filtrada por los dos guardarraíles` | **HU de valor** | P3 | 28 h | Alumno del curso |
| **EP-05 · Tutor Seguro** | LLM-EP05-H02 | `G03 — Guardarraíles anti-jailbreak y anti-fuga` | **HU de valor** | P3 | 26 h | Alumno del curso |
| **EP-05 · Tutor Seguro** | LLM-EP05-H03 | `G03 — Retomar una conversación anterior con el tutor sin perder el contexto` | **HU de valor** | P3 | 25 h | Alumno del curso |
| **EP-06 · Evaluación** | LLM-EP06-H01 | `G03 — Consumir el cierre de un intento y encolar su evaluación` | Tarea | P5 | 24 h | Sistema (`llm-service`) |
| **EP-06 · Evaluación** | LLM-EP06-H02 | `G03 — Recibir un puntaje explicado en las cinco dimensiones al cerrar mi intento` | **HU de valor** | P5 | 25 h | Alumno del curso |
| **EP-06 · Evaluación** | LLM-EP06-H03 | `G03 — Que mi entrega se acepte igual si el evaluador está caído` | **HU de valor** | P5 | 15 h | Alumno del curso |
| **EP-07 · Costos/Cuotas** | LLM-S09-H01 | `G03 — Consultar el panel de costos, cuotas y fallas del servicio` | **HU de valor** | P1+P2 | 36 h | Operador / Admin |
| **EP-07 · Costos/Cuotas** | LLM-S09-H02 | `G03 — Configurar un límite de cuota versionado y auditado` | **HU de valor** | P2 | 28 h | Administrador |
| **EP-07 · Costos/Cuotas** | LLM-S09-H03 | `G03 — Que el sistema aplique 429 y Retry-After en todas las rutas con límite` | Tarea (contrato) | P2 | 22 h | Sistema (`llm-service`) |
| **EP-09 · RAG Material** | LLM-EP09-H01 | `G03 — Ingesta e indexado de un PDF como fuente de consulta` | **HU de valor** | P3 | 12 h | Docente del curso |
| **EP-09 · RAG Material** | LLM-EP09-H02 | `G03 — Consulta al tutor con citas de fuente/página y abstención` | **HU de valor** | P3 | 12 h | Alumno del curso |

---

## 3. Fichas de EP-08 · Moderación integrada (F2) — Listas para Taiga

> **Épica en Taiga:** `EP-08 · Moderación integrada (F2)`  
> **Objetivo de la Épica:** Que los mensajes del chat real se permitan o bloqueen antes de entregarse, con las apelaciones revisadas por un docente y una degradación acordada; al archivar, se conserva solo la evidencia necesaria.

```
================================================================================
TARJETA TAIGA 1: LLM-S11-H01
================================================================================
Título:
G03 — Que un mensaje del chat se permita o bloquee antes de entregarse

Épica:
EP-08 · Moderación integrada (F2)

Tipo en Taiga:
Tarea (Habilitador técnico / Contrato inter-equipos)

Descripción para pegar en Taiga:
```
**ID interno:** LLM-S11-H01  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 + P2 + P1 | **Estimación:** ~34 h  
**Contrato / Requisito:** RF-CHT-09; contrato `chat-service ↔ llm-service` v1

---

## Descripción (Como / Quiero / Para)

- **Como:** sistema / `chat-service` (actor técnico que envía el mensaje para decisión)
- **Quiero:** que cada mensaje del chat pase por una decisión de moderación (ALLOW / BLOCK) antes de entregarse al destinatario, con el incidente auditado y sin retener más contexto del necesario
- **Para:** garantizar que ningún mensaje bloqueado llegue a mostrarse, y que el chat-service tenga una decisión clara antes de publicar

---

## Criterios de Aceptación (CA)

- [ ] CA1: Dado un mensaje válido enviado por chat-service, POST /moderation/v1/decisions responde 200 OK con decision: ALLOW o BLOCK dentro de 800 ms de timeout configurado.
- [ ] CA2: Toda decisión emitida genera registro de auditoría con message_id, decision, timestamp, reason_code y classifier_used, sin incluir texto completo si es ALLOW.
- [ ] CA3: Si el motor no resuelve en timeout, devuelve decision: PENDING y chat-service no publica el mensaje.
- [ ] CA4: El mismo message_id enviado múltiples veces devuelve siempre la misma decisión (idempotencia).
- [ ] CA5: Endpoint rechaza peticiones sin JWT válido con scope moderation:decide con 401 Unauthorized.
- [ ] CA_negativo_1: Un mensaje con ALLOW que es retirado sin revisión explícita viola el contrato y se registra como error de protocolo.
- [ ] CA_negativo_2: Mismo message_id enviado dos veces con textos distintos no produce dos decisiones diferentes.

---

## BDD Resumido

- **Dado** que chat-service tiene JWT válido con scope moderation:decide y envía message_id: "msg-001"
- **Cuando** llama a POST /moderation/v1/decisions con payload mínimo
- **Entonces** responde 200 OK con ALLOW o BLOCK en < 800 ms y se persiste la auditoría.
*(Ficha completa en repo: docs/historias/ep-08/h01.md)*
```
================================================================================
TARJETA TAIGA 2: LLM-S11-H02
================================================================================
Título:
G03 — Detectar spam, contenido ofensivo e intentos de ocultar código

Épica:
EP-08 · Moderación integrada (F2)

Tipo en Taiga:
Tarea (Habilitador técnico / Motor determinista)

Descripción para pegar en Taiga:
```
**ID interno:** LLM-S11-H02  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 | **Estimación:** ~28 h  
**Contrato / Requisito:** RF-CHT-10; RF-CHT-11

---

## Descripción (Como / Quiero / Para)

- **Como:** sistema (motor de moderación dentro de `llm-service`)
- **Quiero:** detectores deterministas que clasifiquen un mensaje como spam, ofensivo o intento de ocultar código en menos de 50 ms, con las reglas de detección nunca filtradas en los mensajes de bloqueo devueltos al cliente
- **Para:** bloquear los casos evidentes sin necesidad del clasificador contextual (más lento y costoso), reduciendo la latencia promedio de moderación y el consumo de recursos de IA

---

## Criterios de Aceptación (CA)

- [ ] CA1: Módulo de detección determinista evalúa un mensaje y devuelve PASS/FAIL + reason_code en menos de 50 ms (p99) bajo carga normal.
- [ ] CA2: Mensaje con texto codificado en Base64 con código fuente se clasifica como FAIL con reason_code: CODE_OBFUSCATION.
- [ ] CA3: Mensaje con alta densidad de URLs (>= 3 URLs en < 200 chars) se clasifica FAIL con reason_code: SPAM.
- [ ] CA4: Mensaje con términos ofensivos que supera umbral configurado se clasifica FAIL con reason_code: OFFENSIVE.
- [ ] CA5: Mensaje de bloqueo no expone expresiones regulares, pesos ni detalle interno de reglas.
- [ ] CA_negativo_1: Si el mensaje devuelto incluye el regex o regla activada, la prueba de seguridad falla.

---

## BDD Resumido

- **Dado** un mensaje de chat con payload ofuscado en base64
- **Cuando** pasa por el pipeline determinista
- **Entonces** es bloqueado en menos de 50 ms sin invocar al LLM externo.
*(Ficha completa en repo: docs/historias/ep-08/h02.md)*
```
================================================================================
TARJETA TAIGA 3: LLM-S12-H01
================================================================================
Título:
G03 — Apelar un mensaje que bloqueó la moderación

Épica:
EP-08 · Moderación integrada (F2)

Tipo en Taiga:
Historia de Usuario (HU de valor)

Descripción para pegar en Taiga:
```
**ID interno:** LLM-S12-H01  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 | **Estimación:** ~26 h  
**Contrato / Requisito:** RF-CHT-12

---

## Descripción (Como / Quiero / Para)

- **Como:** alumno cuyo mensaje fue bloqueado por la moderación
- **Quiero:** poder apelar el bloqueo indicando por qué considero que mi mensaje era legítimo, y recibir una confirmación de que la apelación fue registrada con un identificador de seguimiento
- **Para:** tener una vía de reclamo formal cuando considero que la moderación fue un error, sin tener que contactar al soporte manualmente y pudiendo hacer seguimiento del estado de mi apelación

---

## Criterios de Aceptación (CA)

- [ ] CA1: Alumno autenticado apela incidente BLOCK propio con motivo entre 20 y 1000 caracteres y recibe 201 Created con appeal_id y status: PENDING_REVIEW.
- [ ] CA2: Apelación queda registrada en base con incident_id, user_id, appeal_reason y status: PENDING_REVIEW.
- [ ] CA3: Alumno consulta GET /moderation/v1/appeals/{appeal_id} y obtiene estado actual (PENDING_REVIEW, CONFIRMED, REVERSED).
- [ ] CA4: Valida que incident_id pertenece al alumno autenticado; si es de otro usuario devuelve 403 Forbidden.
- [ ] CA_negativo_1: Si intenta apelar el mismo incident_id por segunda vez devuelve 409 Conflict.
- [ ] CA_negativo_2: Si appeal_reason tiene menos de 20 caracteres devuelve 400 Bad Request.

---

## BDD Resumido

- **Dado** un alumno con un mensaje legítimo bloqueado por falso positivo
- **Cuando** envía su apelación con motivo fundado
- **Entonces** se crea la apelación en estado PENDING_REVIEW y se asigna a revisión docente.
*(Ficha completa en repo: docs/historias/ep-08/h03.md)*
```
================================================================================
TARJETA TAIGA 4: LLM-S12-H02
================================================================================
Título:
G03 — Que el docente revise un incidente de moderación y lo resuelva

Épica:
EP-08 · Moderación integrada (F2)

Tipo en Taiga:
Historia de Usuario (HU de valor)

Descripción para pegar en Taiga:
```
**ID interno:** LLM-S12-H02  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 | **Estimación:** ~30 h  
**Contrato / Requisito:** RF-CHT-13; RF-CHT-14

---

## Descripción (Como / Quiero / Para)

- **Como:** docente del curso
- **Quiero:** ver los incidentes de moderación de mi curso (con el contexto mínimo necesario y la decisión del sistema), resolver cada uno como confirmado (bloqueo correcto) o revertido (falso positivo), y que la resolución quede auditada con mi identidad y motivo
- **Para:** ejercer supervisión real sobre la moderación automática sin necesitar acceder al historial completo del chat del alumno, y sin exponer información más allá de lo necesario para tomar la decisión

---

## Criterios de Aceptación (CA)

- [ ] CA1: Docente lista incidentes de sus cursos vía GET /moderation/v1/incidents?course_id={id}&status=PENDING_REVIEW paginado (máx 50).
- [ ] CA2: Cada incidente muestra preview de máx 200 chars, reason_code, decision, apelación si existe; nunca el historial completo del chat.
- [ ] CA3: Docente resuelve vía POST /moderation/v1/incidents/{incident_id}/resolve enviando CONFIRMED o REVERSED con motivo y recibe 200 OK.
- [ ] CA4: Resolución auditada con resolved_by, resolution, resolution_reason y resolved_at.
- [ ] CA5: Si es REVERSED, marca mensaje original como desbloqueado y dispara notificación al alumno.
- [ ] CA_negativo_1: Intento de resolver incidentes de un curso ajeno devuelve 403 Forbidden.
- [ ] CA_negativo_2: Envío de resolución sin motivo o con menos de 20 caracteres devuelve 400 Bad Request.

---

## BDD Resumido

- **Dado** un docente revisando incidentes de su curso
- **Cuando** confirma que un bloqueo fue un falso positivo y resuelve REVERSED con justificativo
- **Entonces** el mensaje se desbloquea en el chat y se notifica al alumno.
*(Ficha completa en repo: docs/historias/ep-08/h04.md)*
```
================================================================================
TARJETA TAIGA 5: LLM-S13-H01
================================================================================
Título:
G03 — Que la moderación siga funcionando en modo degradado si el clasificador contextual falla

Épica:
EP-08 · Moderación integrada (F2)

Tipo en Taiga:
Tarea (Habilitador técnico / Resiliencia y Circuit Breaker)

Descripción para pegar en Taiga:
```
**ID interno:** LLM-S13-H01  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 + P2 | **Estimación:** ~24 h  
**Contrato / Requisito:** RF-CHT-09 (modo degradado)

---

## Descripción (Como / Quiero / Para)

- **Como:** sistema (motor de moderación dentro de `llm-service`)
- **Quiero:** seguir decidiendo con los detectores deterministas cuando el clasificador contextual no está disponible, marcando como pendiente de revisión humana solo los casos que de verdad necesitaban el análisis más profundo
- **Para:** que una caída del clasificador contextual no bloquee todo el chat ni deje pasar mensajes dudosos sin ningún control

---

## Criterios de Aceptación (CA)

- [ ] CA1: Si clasificador contextual falla o timeout, detectores deterministas siguen operando; casos dudosos pasan a PENDING_REVIEW con classifier_used: fallback.
- [ ] CA2: Circuit breaker abre ante > 50% de fallos en 60s en clasificador contextual evitando llamadas redundantes.
- [ ] CA3: Ante caída total de moderación, la respuesta nunca es ALLOW por defecto; chat-service recibe PENDING.
- [ ] CA_negativo_1: Ante indisponibilidad del modelo, ningún mensaje sospechoso se aprueba automáticamente.

---

## BDD Resumido

- **Dado** el servicio contextual de IA caído
- **Cuando** ingresa un mensaje con spam evidente
- **Entonces** el detector determinista lo bloquea inmediatamente; si es dudoso queda en PENDING_REVIEW para docente.
*(Ficha completa en repo: docs/historias/ep-08/h05.md)*
```
================================================================================
TARJETA TAIGA 6: LLM-S13-H02
================================================================================
Título:
G03 — Que la evidencia de moderación se retenga el tiempo necesario y luego se purgue

Épica:
EP-08 · Moderación integrada (F2)

Tipo en Taiga:
Tarea (Habilitador técnico / Minimización de datos y retención)

Descripción para pegar en Taiga:
```
**ID interno:** LLM-S13-H02  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P1 | **Estimación:** ~18 h  
**Contrato / Requisito:** RF-CHT-09 (política de purga y privacidad)

---

## Descripción (Como / Quiero / Para)

- **Como:** sistema (tarea habilitadora de cumplimiento y minimización de datos)
- **Quiero:** purgar automáticamente el texto y evidencia de un incidente de moderación cuando se cumple su período de retención, conservando solo lo mínimo necesario para auditoría agregada
- **Para:** cumplir con minimización de datos sin perder la capacidad de auditar cuántos incidentes hubo, de qué tipo y cómo se resolvieron

---

## Criterios de Aceptación (CA)

- [ ] CA1: Trabajo asíncrono purga texto de mensajes, motivos libres y evidencias vencidas según período configurado.
- [ ] CA2: Conserva métricas y registros agregados (curso, fecha, tipo de incidente, resolución) sin texto sensible.
- [ ] CA3: Decisión ALLOW no almacena texto de mensaje desde el origen (CA2 de H01).
- [ ] CA_negativo_1: Una vez purgado, el texto no puede ser recuperado ni expuesto.

---

## BDD Resumido

- **Dado** un lote de incidentes cuya antigüedad supera el período legal de retención
- **Cuando** se ejecuta el proceso de purga programada
- **Entonces** el contenido textual se destruye irreversibly y las métricas estadísticas permanecen intactas.
*(Ficha completa en repo: docs/historias/ep-08/h06.md)*
```

---

## 4. Fichas de las Épicas Troncales del Sprint 1

A continuación se resumen las historias de usuario y habilitadores de las épicas activas en Sprint 1 para su carga inmediata en Taiga:

### 4.1 EP-01 · Plataforma, Infraestructura y Seguridad Transversal
* **LLM-EP01-H01 (`G03 — ADR de arquitectura y convenciones técnicas`)**: Tarea técnica (P1, 16 h). Registro formal de convenciones de arquitectura (ADR-001 a ADR-018), fronteras de microservicio, Eureka y API Gateway.
* **LLM-EP01-H02 (`G03 — Entorno reproducible con un comando`)**: Tarea técnica (P1, 30 h). Docker Compose con PostgreSQL, variables de entorno validadas, migraciones Flyway automáticas y healthchecks.
* **LLM-EP01-H03 (`G03 — Esqueleto transversal del servicio`)**: Tarea técnica (P1, 34 h). Filtros de seguridad para headers de identidad delegada (`X-User-Id`, `X-User-Roles`, `X-Delegated-User`), Problem Details RFC 7807 y correlación `traceparent`.
* **LLM-EP01-H04 (`G03 — Esquema inicial versionado con auditoría`)**: Tarea técnica (P1, 38 h). DDL Flyway append-only para golden sets, rúbricas y auditoría inmutable.
* **LLM-EP01-H05 (`G03 — Contrato OpenAPI y mock del golden set publicados`)**: Tarea técnica (P1, 10 h). Contrato OpenAPI 3.0 bajo `/api/llm/**` y mock activo para desacoplar a frontend y admin-service.
* **LLM-EP01-H06 (`G03 — Suite de pruebas y guía de demo de S1`)**: Tarea técnica (P1/Todos, 18 h). Pruebas de integración Testcontainers, verificación reproducible y guía de demo para el docente.

### 4.2 EP-02 · Proveedor de Modelos e Inteligencia Artificial
* **LLM-EP02-H01 (`G03 — Puerto del proveedor de modelos (AI Gateway) y simulador para pruebas`)**: Tarea técnica (P2, 32 h). Interfaz desacoplada `LlmClientPort`, adaptador simulador (*fake*) determinista sin consumo de saldo para CI/CD.
* **LLM-EP02-H02 (`G03 — Conectar un proveedor real detrás de la conexión con el modelo`)**: Tarea técnica (P2, 20 h). Adaptador real LangChain4j con proveedor Groq/OpenAI, fallback automático ante rate limit o indisponibilidad.

### 4.3 EP-03 · Banco de Casos de Referencia (Golden Set Docente)
* **LLM-EP03-H01 (`G03 — Alta de golden set y carga de entradas`)**: HU de valor (P5, 24 h). *Como docente, quiero crear un banco de casos y cargar entradas con código, consigna y puntuación esperada, para tener un patrón de calibración.*
* **LLM-EP03-H02 (`G03 — Consulta del golden set persistente tras reinicio`) [HISTORIA CANÓNICA]**: HU de valor (P5, 14 h). *Como docente, quiero consultar mis casos tras reiniciar el contenedor, para verificar la persistencia real.*
* **LLM-EP03-H03 (`G03 — Pantalla docente mínima del banco de casos`)**: HU de valor (P5, 24 h). *Como docente, quiero una interfaz web simple para cargar y ver los casos del golden set sin usar Postman o curl.*
* **LLM-EP03-H04 (`G03 — Rúbrica versionada por curso`)**: HU de valor (P5, 15 h). *Como docente, quiero definir y versionar las 5 dimensiones y pesos de la rúbrica de mi curso.*
* **LLM-EP03-H05 (`G03 — Banco de casos versionado por curso, con copia desde la base de plataforma`)**: HU de valor (P5, 15 h). *Como docente, quiero clonar el golden set base de la plataforma a mi curso y personalizarlo.*

### 4.4 EP-04 · Calibración y Gobernanza del Modelo
* **LLM-EP04-H01 (`G03 — Correr y activar una calibración de curso con métrica PAR-14`)**: HU de valor (P4, 46 h). *Como docente, quiero ejecutar la calibración de mi curso contra el golden set y activarla si el error PAR-14 es menor a 14 puntos.*
* **LLM-EP04-H02 (`G03 — Correr una calibración de plataforma y ver su reporte`)**: HU de valor (P4, 20 h). *Como administrador, quiero calibrar un modelo base contra los casos globales de la cátedra.*
* **LLM-EP04-H03 (`G03 — Que una calibración vigente venza cuando cambia la referencia`)**: HU de valor (P4, 15 h). *Como docente, quiero que la calibración se invalide automáticamente si edito la rúbrica o los casos.*
* **LLM-EP04-H04 (`G03 — Enterarme de que mi curso tiene evaluaciones frenadas`)**: HU de valor (P4, 20 h). *Como docente, quiero recibir un aviso si el curso no tiene calibración válida y las entregas quedan en espera.*

### 4.5 EP-05 · Tutor Seguro y Guardarraíles
* **LLM-EP05-H01 (`G03 — Recibir una respuesta socrática del tutor, filtrada por los dos guardarraíles`)**: HU de valor (P3, 28 h). *Como alumno, quiero interactuar con el tutor para recibir orientación socrática sin que revele el código de solución.*
* **LLM-EP05-H02 (`G03 — Guardarraíles anti-jailbreak y anti-fuga`)**: HU de valor (P3, 26 h). *Como alumno/sistema, quiero que cualquier intento de inyección de prompt o extracción de rúbrica sea neutralizado con respuesta de advertencia.*
* **LLM-EP05-H03 (`G03 — Retomar una conversación anterior con el tutor sin perder el contexto`)**: HU de valor (P3, 25 h). *Como alumno, quiero continuar una sesión de tutoría previa conservando el historial pedagógico.*

### 4.6 EP-06 · Consumo y Evaluación Académica
* **LLM-EP06-H01 (`G03 — Consumir el cierre de un intento y encolar su evaluación`)**: Tarea técnica (P5, 24 h). *Como sistema, quiero escuchar el evento `intento_cerrado.v1` de Kafka con deduplicación y encolar la evaluación.*
* **LLM-EP06-H02 (`G03 — Recibir un puntaje explicado en las cinco dimensiones al cerrar mi intento`)**: HU de valor (P5, 25 h). *Como alumno, quiero ver el desglose en 5 dimensiones y retroalimentación cualitativa al corregirse mi entrega.*
* **LLM-EP06-H03 (`G03 — Que mi entrega se acepte igual si el evaluador está caído`)**: HU de valor (P5, 15 h). *Como alumno, quiero que mi entrega se registre exitosamente aunque el motor de IA sufra una demora o corte transitorio.*

### 4.7 EP-07 · Operación, Cuotas y Observabilidad
* **LLM-S09-H01 (`G03 — Consultar el panel de costos, cuotas y fallas del servicio`)**: HU de valor (P1+P2, 36 h). *Como operador, quiero ver consumo de tokens, costos estimados y tasa de error del proveedor de IA.*
* **LLM-S09-H02 (`G03 — Configurar un límite de cuota versionado y auditado`)**: HU de valor (P2, 28 h). *Como admin, quiero establecer límites diarios/mensuales de llamadas a IA por curso o alumno.*
* **LLM-S09-H03 (`G03 — Que el sistema aplique 429 y Retry-After en todas las rutas con límite`)**: Tarea técnica (P2, 22 h). *Como sistema, quiero rechazar con HTTP 429 cuando se excede la cuota indicando cuándo reintentar.*

### 4.8 EP-09 · RAG y Consulta de Material
* **LLM-EP09-H01 (`G03 — Ingesta e indexado de un PDF como fuente de consulta`)**: HU de valor (P3, 12 h). *Como docente, quiero subir un apunte en PDF para que el tutor responda preguntas basándose en la bibliografía oficial.*
* **LLM-EP09-H02 (`G03 — Consulta al tutor con citas de fuente/página y abstención`)**: HU de valor (P3, 12 h). *Como alumno, quiero que el tutor cite el número de página al responder y se abstenga si la respuesta no está en el material.*

---

## 5. Recomendaciones para la carga masiva en Taiga

1. **Crear primero las Épicas en Taiga** (módulo *Epics*):
   - Verificar que existan `EP-01`, `EP-02`, `EP-03`, `EP-04`, `EP-05`, `EP-06`, `EP-07`, `EP-08` y `EP-09`.
2. **Cargar las HU en el Backlog**:
   - Pegar el título con formato `G03 — <Título>`.
   - Seleccionar la Épica y asignar al **Sprint 1**.
   - Pegar el bloque de descripción Markdown correspondiente.
3. **Planning Poker (Estimación Fibonacci)**:
   - Tomar **LLM-EP03-H02** (*Consulta del golden set persistente tras reinicio*) como **3 o 5 puntos** (historia canónica de dificultad y alcance conocido).
   - Estimar el resto de forma relativa respecto a la canónica.
