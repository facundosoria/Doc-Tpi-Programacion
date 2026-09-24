# Historias de Usuario y Tareas Agregadas al Sprint 1 (`llm-service`)

> **Ubicación oficial en Sprint 1:** este archivo.  
> **Propósito:** Documentar en un único lugar dentro de la carpeta de **Sprint 1** todas las Historias de Usuario (HU) y Tareas técnicas que fueron agregadas e incorporadas al sprint (con especial énfasis en la **incorporación de EP-08 · Moderación integrada F2** y la **expansión multihilo Hilos B a G**), facilitando su identificación por épica, sus tareas técnicas SMART asociadas y su carga directa en Taiga.  
> **Identificador de Grupo en Taiga:** `G03` (prefijo obligatorio: `G03 — <Título>`).  
> **Historia canónica de referencia para Planning Poker:** `LLM-EP03-H02` (*Consulta del golden set persistente tras reinicio*).

---

## 1. Resumen Ejecutivo: ¿Qué US se agregaron al Sprint 1?

El Sprint 1 originalmente contemplaba únicamente la base de plataforma (`EP-01`) y el alta/consulta de golden set (`EP-03`). Para aprovechar la capacidad total del equipo de 10 desarrolladores (581,6 h netas) y cumplir con los requerimientos acordados, se incorporaron dos grandes bloques:

### Bloque A: Incorporación de EP-08 · Moderación integrada del Sistema LLM (F2) *(Novedad)*
Se incorporó el paquete completo de la **Épica 08** (Moderación del Chat), que cubre la inspección previa a la entrega de mensajes, la detección determinística rápida, la clasificación contextual de toxicidad, las apelaciones alumno/docente y la resiliencia en modo degradado:
* **6 Historias / Tareas de alto nivel:** `LLM-S11-H01`, `LLM-S11-H02`, `LLM-S12-H01`, `LLM-S12-H02`, `LLM-S13-H01`, `LLM-S13-H02`.
* **31 Tareas Técnicas SMART** desglosadas (exactamente **160 horas** de trabajo técnico):
  * `LLM-S11-H01` (Contrato y decisión previa): 7 tareas (34 h) en [`../../tareas/ep-08/h01.md`](../../tareas/ep-08/h01.md)
  * `LLM-S11-H02` (Detectores deterministas en memoria): 5 tareas (28 h) en [`../../tareas/ep-08/h02.md`](../../tareas/ep-08/h02.md)
  * `LLM-S12-H01` (Flujo de apelación del alumno): 5 tareas (26 h) en [`../../tareas/ep-08/h03.md`](../../tareas/ep-08/h03.md)
  * `LLM-S12-H02` (Bandeja de resolución docente): 5 tareas (30 h) en [`../../tareas/ep-08/h04.md`](../../tareas/ep-08/h04.md)
  * `LLM-S13-H01` (Resiliencia y degradación elegante): 5 tareas (24 h) en [`../../tareas/ep-08/h05.md`](../../tareas/ep-08/h05.md)
  * `LLM-S13-H02` (Minimización y purga de evidencia): 4 tareas (18 h) en [`../../tareas/ep-08/h06.md`](../../tareas/ep-08/h06.md)

### Bloque B: Expansión en 5 Hilos Paralelos (Hilos B a G)
Para evitar la ociosidad de las parejas P2, P3, P4 y P5 mientras P1 consolidaba la plataforma, se agregaron las siguientes historias:
* **Hilo B (P2) · EP-02 AI Gateway:** Conexión de proveedor real Groq/LangChain4j con fallback (`LLM-EP02-H02`).
* **Hilo C (P3) · EP-05 Tutor & EP-09 RAG:** Interacción socrática (`LLM-EP05-H01`), guardarraíles anti-fuga (`LLM-EP05-H02`), persistencia conversacional (`LLM-EP05-H03`) e ingesta/citas de PDF (`LLM-EP09-H01`, `LLM-EP09-H02`).
* **Hilo D (P4) · EP-04 Calibración:** Calibración con métrica PAR-14 (`LLM-EP04-H01`), calibración de plataforma (`LLM-EP04-H02`), ciclo de vida (`LLM-EP04-H03`) y panel docente (`LLM-EP04-H04`).
* **Hilo E (P5) · EP-03 Rúbrica & EP-06 Eval:** Rúbrica versionada (`LLM-EP03-H04`), golden set versionado (`LLM-EP03-H05`), consumo de eventos Kafka (`LLM-EP06-H01`), retroalimentación en 5 dimensiones (`LLM-EP06-H02`) y aceptación resiliente (`LLM-EP06-H03`).
* **Hilo F (P1+P2) · EP-07 Costos y Cuotas:** Panel de costos (`LLM-S09-H01`), configuración de cuotas (`LLM-S09-H02`) y rate limiting 429 con Retry-After (`LLM-S09-H03`).

---

## 1.1 Arquitectura y Criterios de Seguridad Contemplados en las US

La seguridad en el Sprint 1 **no es una tarjeta aislada**, sino una arquitectura defensiva en profundidad distribuida a través de las historias y tareas técnicas:

### 1. Seguridad de Borde, M2M e Identidad Delegada (`EP-01 · Hilo A`)
* **Validación M2M estricta (`LLM-EP01-H03` · T4):** Rechazo inmediato con `401 Unauthorized` si el JWT no contiene `aud=llm-service` o carece del scope necesario (`moderation:decide`, `evaluation:run`, `golden-set:write`).
* **Prevención de Suplantación (*Anti-Spoofing*) (`LLM-EP01-H03` · T5):** Validación de que `X-Delegated-User`, `X-User-Roles` y `X-User-Id` provengan únicamente del Gateway confiable. Si un cliente externo los inyecta, se responde `403 Forbidden`.
* **Sanitización y Problem Details RFC 7807 (`LLM-EP01-H03` · T6):** Ningún error expone stacktraces ni detalles del servidor; filtros de logging impiden escribir tokens JWT o secretos en consola.
* **Gestión de Secretos (`LLM-EP01-H01`/`H02`):** Claves API de proveedores (Groq, OpenAI) inyectadas por variables de entorno `.env`, excluidas del repositorio Git.

### 2. Seguridad en IA: Guardarraíles y Anti-Trampa del Tutor (`EP-05 · Hilo C`)
* **Guardarraíl de Entrada Anti-Jailbreak (`LLM-EP05-H02` / `LLM-EP05-H01` · T4):** Normalización Unicode y análisis léxico previo que corta intentos de prompt injection (*"Ignore previous instructions"*, DAN) **antes** de invocar al LLM, protegiendo los tokens.
* **Guardarraíl de Salida Anti-Fuga de Solución (`LLM-EP05-H01` · T5):** Análisis sintáctico con `JavaParser` (AST) que asegura que la respuesta del tutor tenga similitud $\le 70\%$ con la solución esperada del desafío (`RF-IA-20`). Si supera el umbral, se frena y se reemplaza por una pista socrática guiada.
* **Idempotencia defensiva (`LLM-EP05-H01` · T3):** Cabecera `Idempotency-Key` para evitar ataques de repetición o doble procesamiento de consultas.

### 3. Seguridad de Contenidos, Detección y Privacidad (`EP-08 · Hilo H`)
* **Detección Determinística en Memoria (`LLM-S11-H02` · T1-T4):** Normalización NFKD y leet speak (`p3l0tud0`), autómata Aho-Corasick para insultos, cálculo de **entropía de Shannon** para Base64 (> 4.5 con palabras clave de código) y control de spam de URLs, resolviendo el 85% del tráfico en < 50 ms a costo $0.
* **Confidencialidad de Reglas Defensivas (`LLM-S11-H02` · T5, CA5, CA_negativo_1):** Prohibición absoluta de revelar expresiones regulares, pesos o listas negras en los mensajes devueltos al cliente para evitar ingeniería inversa de evasión.
* **Autorización Granular en Apelaciones (`LLM-S12-H01` · T2 / `LLM-S12-H02` · T1):** Validación de propiedad (`user_id` autenticado vs incidente apelado, `403 Forbidden` si intenta apelar uno ajeno); docentes restringidos únicamente a sus cursos asignados.
* **Modo Degradado Fail-Safe (`LLM-S13-H01` · T1, T2, T5):** Circuit Breaker de Resilience4j. Ante fallos del clasificador de IA, **nunca** se emite un `ALLOW` por defecto; el mensaje pasa a `PENDING_REVIEW`.
* **Minimización de Datos y Privacidad GDPR (`LLM-S13-H02` · T1-T4):** Mensajes con `ALLOW` no persisten texto completo en auditoría; proceso programado de purga destruye evidencias vencidas conservando solo estadísticas anonimizadas.

### 4. Seguridad Operativa y Anti-Denial of Wallet (`EP-07 · Hilo F`)
* **Control de Cuotas y Rate Limiting (`LLM-S09-H02`, `LLM-S09-H03`):** Políticas de cuotas diarias por alumno y curso para blindar el techo presupuestario de USD 20/mes; respuesta automática con `HTTP 429 Too Many Requests` y cabecera `Retry-After`.

---

---

## 2. Matriz Consolidada de Trazabilidad para Taiga

Esta tabla relaciona cada ítem agregado con su **Épica**, el título exacto a colocar en Taiga, su tipo, el rol de usuario, la pareja responsable, la estimación en horas y los enlaces a sus fichas de historia y tareas SMART.

| Épica en Taiga | ID Interno | Título oficial a pegar en Taiga | Tipo | Pareja | Horas | Rol (Como) | Ficha Historia | Tareas SMART |
|---|---|---|---|:---:|:---:|---|:---:|:---:|
| **EP-08 · Moderación (F2)** *(Nueva)* | LLM-S11-H01 | `G03 — Que un mensaje del chat se permita o bloquee antes de entregarse` | Tarea | P3+P2+P1 | ~34 h | Sistema (`chat-service`) | [h01.md](../../historias/ep-08/h01.md) | [tareas](../../tareas/ep-08/h01.md) |
| **EP-08 · Moderación (F2)** *(Nueva)* | LLM-S11-H02 | `G03 — Detectar spam, contenido ofensivo e intentos de ocultar código` | Tarea | P3 | ~28 h | Sistema (`llm-service`) | [h02.md](../../historias/ep-08/h02.md) | [tareas](../../tareas/ep-08/h02.md) |
| **EP-08 · Moderación (F2)** *(Nueva)* | LLM-S12-H01 | `G03 — Apelar un mensaje que bloqueó la moderación` | **HU** | P3 | ~26 h | Alumno del curso | [h03.md](../../historias/ep-08/h03.md) | [tareas](../../tareas/ep-08/h03.md) |
| **EP-08 · Moderación (F2)** *(Nueva)* | LLM-S12-H02 | `G03 — Que el docente revise un incidente de moderación y lo resuelva` | **HU** | P3 | ~30 h | Docente del curso | [h04.md](../../historias/ep-08/h04.md) | [tareas](../../tareas/ep-08/h04.md) |
| **EP-08 · Moderación (F2)** *(Nueva)* | LLM-S13-H01 | `G03 — Que la moderación siga funcionando en modo degradado si el clasificador contextual falla` | Tarea | P3+P2 | ~24 h | Sistema (`llm-service`) | [h05.md](../../historias/ep-08/h05.md) | [tareas](../../tareas/ep-08/h05.md) |
| **EP-08 · Moderación (F2)** *(Nueva)* | LLM-S13-H02 | `G03 — Que la evidencia de moderación se retenga el tiempo necesario y luego se purgue` | Tarea | P1 | ~18 h | Sistema (Auditoría) | [h06.md](../../historias/ep-08/h06.md) | [tareas](../../tareas/ep-08/h06.md) |
| **EP-01 · Plataforma** | LLM-EP01-H01 | `G03 — ADR de arquitectura y convenciones técnicas` | Tarea | P1 | 16 h | Equipo desarrollo | [h01.md](../../historias/ep-01/h01.md) | [tareas](../../tareas/ep-01/h01.md) |
| **EP-01 · Plataforma** | LLM-EP01-H02 | `G03 — Entorno reproducible con un comando` | Tarea | P1 | 30 h | Equipo desarrollo | [h02.md](../../historias/ep-01/h02.md) | [tareas](../../tareas/ep-01/h02.md) |
| **EP-01 · Plataforma** | LLM-EP01-H03 | `G03 — Esqueleto transversal del servicio` | Tarea | P1 | 34 h | Equipo desarrollo | [h03.md](../../historias/ep-01/h03.md) | [tareas](../../tareas/ep-01/h03.md) |
| **EP-01 · Plataforma** | LLM-EP01-H04 | `G03 — Esquema inicial versionado con auditoría` | Tarea | P1 | 38 h | Equipo desarrollo | [h04.md](../../historias/ep-01/h04.md) | [tareas](../../tareas/ep-01/h04.md) |
| **EP-01 · Plataforma** | LLM-EP01-H05 | `G03 — Contrato OpenAPI y mock del golden set publicados` | Tarea | P1 | 10 h | Equipo desarrollo | [h05.md](../../historias/ep-01/h05.md) | [tareas](../../tareas/ep-01/h05.md) |
| **EP-01 · Plataforma** | LLM-EP01-H06 | `G03 — Suite de pruebas y guía de demo de S1` | Tarea | P1 (todos) | 18 h | Equipo desarrollo | [h06.md](../../historias/ep-01/h06.md) | [tareas](../../tareas/ep-01/h06.md) |
| **EP-02 · AI Gateway** | LLM-EP02-H01 | `G03 — Puerto del proveedor de modelos (AI Gateway) y simulador para pruebas` | Tarea | P2 | 32 h | Equipo desarrollo | [h01.md](../../historias/ep-02/h01.md) | [tareas](../../tareas/ep-02/h01.md) |
| **EP-02 · AI Gateway** *(Ext)* | LLM-EP02-H02 | `G03 — Conectar un proveedor real detrás de la conexión con el modelo` | Tarea | P2 | 20 h | Equipo desarrollo | [h02.md](../../historias/ep-02/h02.md) | — |
| **EP-03 · Golden Set** | LLM-EP03-H01 | `G03 — Alta de golden set y carga de entradas` | **HU** | P5 | 24 h | Docente | [h01.md](../../historias/ep-03/h01.md) | [tareas](../../tareas/ep-03/h01.md) |
| **EP-03 · Golden Set** | LLM-EP03-H02 | `G03 — Consulta del golden set persistente tras reinicio` | **HU (Canónica)** | P5 | 14 h | Docente | [h02.md](../../historias/ep-03/h02.md) | [tareas](../../tareas/ep-03/h02.md) |
| **EP-03 · Golden Set** | LLM-EP03-H03 | `G03 — Pantalla docente mínima del banco de casos` | **HU** | P5 | 24 h | Docente | [h03.md](../../historias/ep-03/h03.md) | [tareas](../../tareas/ep-03/h03.md) |
| **EP-03 · Golden Set** *(Ext)* | LLM-EP03-H04 | `G03 — Rúbrica versionada por curso` | **HU** | P5 | 15 h | Docente | [h04.md](../../historias/ep-03/h04.md) | — |
| **EP-03 · Golden Set** *(Ext)* | LLM-EP03-H05 | `G03 — Banco de casos versionado por curso, con copia desde la base de plataforma` | **HU** | P5 | 15 h | Docente | [h05.md](../../historias/ep-03/h05.md) | — |
| **EP-04 · Calibración** *(Ext)* | LLM-EP04-H01 | `G03 — Correr y activar una calibración de curso con métrica PAR-14` | **HU** | P4 | 46 h | Docente | [h01.md](../../historias/ep-04/h01.md) | — |
| **EP-04 · Calibración** *(Ext)* | LLM-EP04-H02 | `G03 — Correr una calibración de plataforma y ver su reporte` | **HU** | P4 | 20 h | Administrador | [h02.md](../../historias/ep-04/h02.md) | — |
| **EP-04 · Calibración** *(Ext)* | LLM-EP04-H03 | `G03 — Que una calibración vigente venza cuando cambia la referencia` | **HU** | P4 | 15 h | Docente / Admin | [h03.md](../../historias/ep-04/h03.md) | — |
| **EP-04 · Calibración** *(Ext)* | LLM-EP04-H04 | `G03 — Enterarme de que mi curso tiene evaluaciones frenadas` | **HU** | P4 | 20 h | Docente | [h04.md](../../historias/ep-04/h04.md) | — |
| **EP-05 · Tutor Seguro** *(Ext)* | LLM-EP05-H01 | `G03 — Recibir una respuesta socrática del tutor, filtrada por los dos guardarraíles` | **HU** | P3 | 28 h | Alumno | [h01.md](../../historias/ep-05/h01.md) | — |
| **EP-05 · Tutor Seguro** *(Ext)* | LLM-EP05-H02 | `G03 — Guardarraíles anti-jailbreak y anti-fuga` | **HU** | P3 | 26 h | Alumno | [h02.md](../../historias/ep-05/h02.md) | — |
| **EP-05 · Tutor Seguro** *(Ext)* | LLM-EP05-H03 | `G03 — Retomar una conversación anterior con el tutor sin perder el contexto` | **HU** | P3 | 25 h | Alumno | [h03.md](../../historias/ep-05/h03.md) | — |
| **EP-06 · Evaluación** *(Ext)* | LLM-EP06-H01 | `G03 — Consumir el cierre de un intento y encolar su evaluación` | Tarea | P5 | 24 h | Sistema (`llm-service`) | [h01.md](../../historias/ep-06/h01.md) | — |
| **EP-06 · Evaluación** *(Ext)* | LLM-EP06-H02 | `G03 — Recibir un puntaje explicado en las cinco dimensiones al cerrar mi intento` | **HU** | P5 | 25 h | Alumno | [h02.md](../../historias/ep-06/h02.md) | — |
| **EP-06 · Evaluación** *(Ext)* | LLM-EP06-H03 | `G03 — Que mi entrega se acepte igual si el evaluador está caído` | **HU** | P5 | 15 h | Alumno | [h03.md](../../historias/ep-06/h03.md) | — |
| **EP-07 · Costos/Cuotas** *(Ext)* | LLM-S09-H01 | `G03 — Consultar el panel de costos, cuotas y fallas del servicio` | **HU** | P1+P2 | 36 h | Operador / Admin | [h01.md](../../historias/ep-07/h01.md) | — |
| **EP-07 · Costos/Cuotas** *(Ext)* | LLM-S09-H02 | `G03 — Configurar un límite de cuota versionado y auditado` | **HU** | P2 | 28 h | Administrador | [h02.md](../../historias/ep-07/h02.md) | — |
| **EP-07 · Costos/Cuotas** *(Ext)* | LLM-S09-H03 | `G03 — Que el sistema aplique 429 y Retry-After en todas las rutas con límite` | Tarea | P2 | 22 h | Sistema (`llm-service`) | [h03.md](../../historias/ep-07/h03.md) | — |
| **EP-09 · RAG Material** *(Ext)* | LLM-EP09-H01 | `G03 — Ingesta e indexado de un PDF como fuente de consulta` | **HU** | P3 | 12 h | Docente | [h01.md](../../historias/ep-09/h01.md) | — |
| **EP-09 · RAG Material** *(Ext)* | LLM-EP09-H02 | `G03 — Consulta al tutor con citas de fuente/página y abstención` | **HU** | P3 | 12 h | Alumno | [h02.md](../../historias/ep-09/h02.md) | — |

---

## 3. Fichas de Carga Rápida para Taiga: EP-08 · Moderación Integrada (F2)

Copia y pega los siguientes bloques directamente en Taiga. Cada bloque contiene el título normalizado con prefijo `G03 —`, la Épica, el Tipo, la descripción estructurada, los Criterios de Aceptación con casillas `- [ ]` y el desglose de tareas SMART.

```
================================================================================
TARJETA TAIGA 1: LLM-S11-H01
================================================================================
Título:
G03 — Que un mensaje del chat se permita o bloquee antes de entregarse

Épica en Taiga:
EP-08 · Moderación integrada del Sistema LLM (F2)

Tipo en Taiga:
Tarea (Habilitador técnico / Contrato inter-equipos)

Sprint:
Sprint 1

Descripción:
**ID interno:** LLM-S11-H01  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 + P2 + P1 | **Estimación:** ~34 h  
**Contrato / Requisito:** RF-CHT-09; contrato chat-service <-> llm-service v1

---

### Descripción (Como / Quiero / Para)
- **Como:** sistema / chat-service (actor técnico que envía el mensaje para decisión)
- **Quiero:** que cada mensaje del chat pase por una decisión de moderación (ALLOW / BLOCK) antes de entregarse al destinatario, con el incidente auditado y sin retener más contexto del necesario
- **Para:** garantizar que ningún mensaje bloqueado llegue a mostrarse, y que el chat-service tenga una decisión clara antes de publicar

---

### Criterios de Aceptación (CA)
- [ ] CA1: Dado un mensaje válido enviado por chat-service, POST /moderation/v1/decisions responde 200 OK con decision: ALLOW o BLOCK dentro de 800 ms de timeout configurado.
- [ ] CA2: Toda decisión emitida genera registro de auditoría con message_id, decision, timestamp, reason_code y classifier_used, sin incluir texto completo si es ALLOW.
- [ ] CA3: Si el motor no resuelve en timeout, devuelve decision: PENDING y chat-service no publica el mensaje.
- [ ] CA4: El mismo message_id enviado múltiples veces devuelve siempre la misma decisión (idempotencia).
- [ ] CA5: Endpoint rechaza peticiones sin JWT válido con scope moderation:decide con 401 Unauthorized.
- [ ] CA_negativo_1: Un mensaje con ALLOW que es retirado sin revisión explícita viola el contrato y se registra como error de protocolo.
- [ ] CA_negativo_2: Mismo message_id enviado dos veces con textos distintos no produce dos decisiones diferentes.

---

### Tareas SMART desglosadas (34 h totales)
- [ ] T01 (4 h): Contrato OpenAPI y DTOs de decisión de moderación.
- [ ] T02 (6 h): Endpoint POST /moderation/v1/decisions y validación de scopes JWT.
- [ ] T03 (6 h): Orquestador del pipeline de moderación con timeout de 800 ms.
- [ ] T04 (5 h): Mecanismo de idempotencia con clave message_id en Redis/Cache.
- [ ] T05 (5 h): Registro inmutable de auditoría de decisiones.
- [ ] T06 (4 h): Pruebas unitarias de pipeline, timeout y validaciones de DTO.
- [ ] T07 (4 h): Prueba de integración end-to-end simulando chat-service con Testcontainers.
```

```
================================================================================
TARJETA TAIGA 2: LLM-S11-H02
================================================================================
Título:
G03 — Detectar spam, contenido ofensivo e intentos de ocultar código

Épica en Taiga:
EP-08 · Moderación integrada del Sistema LLM (F2)

Tipo en Taiga:
Tarea (Habilitador técnico / Motor determinista en memoria)

Sprint:
Sprint 1

Descripción:
**ID interno:** LLM-S11-H02  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 | **Estimación:** ~28 h  
**Contrato / Requisito:** RF-CHT-10; RF-CHT-11

---

### Descripción (Como / Quiero / Para)
- **Como:** sistema (motor de moderación dentro de llm-service)
- **Quiero:** detectores deterministas que clasifiquen un mensaje como spam, ofensivo o intento de ocultar código en menos de 50 ms, con las reglas de detección nunca filtradas en los mensajes de bloqueo devueltos al cliente
- **Para:** bloquear los casos evidentes sin necesidad del clasificador contextual (más lento y costoso), reduciendo la latencia promedio de moderación y el consumo de recursos de IA

---

### Criterios de Aceptación (CA)
- [ ] CA1: Módulo de detección determinista evalúa un mensaje y devuelve PASS/FAIL + reason_code en menos de 50 ms (p99) bajo carga normal.
- [ ] CA2: Mensaje con texto codificado en Base64 con código fuente se clasifica como FAIL con reason_code: CODE_OBFUSCATION.
- [ ] CA3: Mensaje con alta densidad de URLs (>= 3 URLs en < 200 chars) se clasifica FAIL con reason_code: SPAM.
- [ ] CA4: Mensaje con términos ofensivos que supera umbral configurado se clasifica FAIL con reason_code: OFFENSIVE.
- [ ] CA5: Mensaje de bloqueo no expone expresiones regulares, pesos ni detalle interno de reglas.
- [ ] CA_negativo_1: Si el mensaje devuelto incluye el regex o regla activada, la prueba de seguridad falla.

---

### Tareas SMART desglosadas (28 h totales)
- [ ] T01 (6 h): Motor de normalización de texto y detector de ofuscación Base64 con entropía.
- [ ] T02 (6 h): Filtro léxico multilingüe con Aho-Corasick para términos ofensivos.
- [ ] T03 (5 h): Heurísticas anti-spam de repetición y densidad de enlaces.
- [ ] T04 (5 h): Enmascaramiento estricto de razones de rechazo y mensajes seguros.
- [ ] T05 (6 h): Benchmark de rendimiento de detección determinista (< 50 ms).
```

```
================================================================================
TARJETA TAIGA 3: LLM-S12-H01
================================================================================
Título:
G03 — Apelar un mensaje que bloqueó la moderación

Épica en Taiga:
EP-08 · Moderación integrada del Sistema LLM (F2)

Tipo en Taiga:
Historia de Usuario (HU de valor)

Sprint:
Sprint 1

Descripción:
**ID interno:** LLM-S12-H01  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 | **Estimación:** ~26 h  
**Contrato / Requisito:** RF-CHT-12

---

### Descripción (Como / Quiero / Para)
- **Como:** alumno cuyo mensaje fue bloqueado por la moderación
- **Quiero:** poder apelar el bloqueo indicando por qué considero que mi mensaje era legítimo, y recibir una confirmación de que la apelación fue registrada con un identificador de seguimiento
- **Para:** tener una vía de reclamo formal cuando considero que la moderación fue un error, sin tener que contactar al soporte manualmente y pudiendo hacer seguimiento del estado de mi apelación

---

### Criterios de Aceptación (CA)
- [ ] CA1: Alumno autenticado apela incidente BLOCK propio con motivo entre 20 y 1000 caracteres y recibe 201 Created con appeal_id y status: PENDING_REVIEW.
- [ ] CA2: Apelación queda registrada en base con incident_id, user_id, appeal_reason y status: PENDING_REVIEW.
- [ ] CA3: Alumno consulta GET /moderation/v1/appeals/{appeal_id} y obtiene estado actual (PENDING_REVIEW, CONFIRMED, REVERSED).
- [ ] CA4: Valida que incident_id pertenece al alumno autenticado; si es de otro usuario devuelve 403 Forbidden.
- [ ] CA_negativo_1: Si intenta apelar el mismo incident_id por segunda vez devuelve 409 Conflict.
- [ ] CA_negativo_2: Si appeal_reason tiene menos de 20 caracteres devuelve 400 Bad Request.

---

### Tareas SMART desglosadas (26 h totales)
- [ ] T01 (5 h): Modelo de datos Flyway y entidades JPA para apelaciones de alumnos.
- [ ] T02 (6 h): Endpoint POST /moderation/v1/appeals con validación de propiedad de incidente.
- [ ] T03 (5 h): Endpoint GET /moderation/v1/appeals/{appealId} para seguimiento por el alumno.
- [ ] T04 (5 h): Notificación de evento appeal_created para bandeja de revisión docente.
- [ ] T05 (5 h): Pruebas de integración con MockMvc y validación de seguridad (403, 409).
```

```
================================================================================
TARJETA TAIGA 4: LLM-S12-H02
================================================================================
Título:
G03 — Que el docente revise un incidente de moderación y lo resuelva

Épica en Taiga:
EP-08 · Moderación integrada del Sistema LLM (F2)

Tipo en Taiga:
Historia de Usuario (HU de valor)

Sprint:
Sprint 1

Descripción:
**ID interno:** LLM-S12-H02  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 | **Estimación:** ~30 h  
**Contrato / Requisito:** RF-CHT-13; RF-CHT-14

---

### Descripción (Como / Quiero / Para)
- **Como:** docente del curso
- **Quiero:** ver los incidentes de moderación de mi curso (con el contexto mínimo necesario y la decisión del sistema), resolver cada uno como confirmado (bloqueo correcto) o revertido (falso positivo), y que la resolución quede auditada con mi identidad y motivo
- **Para:** ejercer supervisión real sobre la moderación automática sin necesitar acceder al historial completo del chat del alumno, y sin exponer información más allá de lo necesario para tomar la decisión

---

### Criterios de Aceptación (CA)
- [ ] CA1: Docente lista incidentes de sus cursos vía GET /moderation/v1/incidents?course_id={id}&status=PENDING_REVIEW paginado (máx 50).
- [ ] CA2: Cada incidente muestra preview de máx 200 chars, reason_code, decision, apelación si existe; nunca el historial completo del chat.
- [ ] CA3: Docente resuelve vía POST /moderation/v1/incidents/{incident_id}/resolve enviando CONFIRMED o REVERSED con motivo y recibe 200 OK.
- [ ] CA4: Resolución auditada con resolved_by, resolution, resolution_reason y resolved_at.
- [ ] CA5: Si es REVERSED, marca mensaje original como desbloqueado y dispara notificación al alumno.
- [ ] CA_negativo_1: Intento de resolver incidentes de un curso ajeno devuelve 403 Forbidden.
- [ ] CA_negativo_2: Envío de resolución sin motivo o con menos de 20 caracteres devuelve 400 Bad Request.

---

### Tareas SMART desglosadas (30 h totales)
- [ ] T01 (6 h): Endpoint GET /moderation/v1/incidents con filtrado por curso y autorización docente.
- [ ] T02 (7 h): Endpoint POST /moderation/v1/incidents/{incidentId}/resolve con auditoría inmutable.
- [ ] T03 (6 h): Publicación de eventos de dominio incidente_resuelto hacia Kafka para chat-service.
- [ ] T04 (6 h): Interfaz web mínima docente (Thymeleaf/HTMX) para revisión y resolución de apelaciones.
- [ ] T05 (5 h): Pruebas de integración de flujo completo de revisión docente.
```

```
================================================================================
TARJETA TAIGA 5: LLM-S13-H01
================================================================================
Título:
G03 — Que la moderación siga funcionando en modo degradado si el clasificador contextual falla

Épica en Taiga:
EP-08 · Moderación integrada del Sistema LLM (F2)

Tipo en Taiga:
Tarea (Habilitador técnico / Resiliencia y Fallback)

Sprint:
Sprint 1

Descripción:
**ID interno:** LLM-S13-H01  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P3 + P2 | **Estimación:** ~24 h  
**Contrato / Requisito:** RF-CHT-09 (modo degradado)

---

### Descripción (Como / Quiero / Para)
- **Como:** sistema (motor de moderación dentro de llm-service)
- **Quiero:** seguir decidiendo con los detectores deterministas cuando el clasificador contextual no está disponible, marcando como pendiente de revisión humana solo los casos que de verdad necesitaban el análisis más profundo
- **Para:** que una caída del clasificador contextual no bloquee todo el chat ni deje pasar mensajes dudosos sin ningún control

---

### Criterios de Aceptación (CA)
- [ ] CA1: Si clasificador contextual falla o timeout, detectores deterministas siguen operando; casos dudosos pasan a PENDING_REVIEW con classifier_used: fallback.
- [ ] CA2: Circuit breaker abre ante > 50% de fallos en 60s en clasificador contextual evitando llamadas redundantes.
- [ ] CA3: Ante caída total de moderación, la respuesta nunca es ALLOW por defecto; chat-service recibe PENDING.
- [ ] CA_negativo_1: Ante indisponibilidad del modelo, ningún mensaje sospechoso se aprueba automáticamente.

---

### Tareas SMART desglosadas (24 h totales)
- [ ] T01 (6 h): Integración de Resilience4j Circuit Breaker sobre cliente contextual de IA.
- [ ] T02 (5 h): Mecanismo de fallback a modo determinista estricto con marcado PENDING_REVIEW.
- [ ] T03 (5 h): Métricas Micrometer de degradación (moderation_fallback_total) y health indicator.
- [ ] T04 (4 h): Pruebas de simulación de cortes de red y fallos de API externa con WireMock.
- [ ] T05 (4 h): Verificación de política fail-safe: ningún mensaje dudoso marcado como ALLOW.
```

```
================================================================================
TARJETA TAIGA 6: LLM-S13-H02
================================================================================
Título:
G03 — Que la evidencia de moderación se retenga el tiempo necesario y luego se purgue

Épica en Taiga:
EP-08 · Moderación integrada del Sistema LLM (F2)

Tipo en Taiga:
Tarea (Habilitador técnico / Minimización de datos y retención)

Sprint:
Sprint 1

Descripción:
**ID interno:** LLM-S13-H02  
**Épica:** EP-08 · Moderación integrada (F2)  
**Pareja:** P1 | **Estimación:** ~18 h  
**Contrato / Requisito:** RF-CHT-09 (política de purga y privacidad)

---

### Descripción (Como / Quiero / Para)
- **Como:** sistema (tarea habilitadora de cumplimiento y minimización de datos)
- **Quiero:** purgar automáticamente el texto y evidencia de un incidente de moderación cuando se cumple su período de retención, conservando solo lo mínimo necesario para auditoría agregada
- **Para:** cumplir con minimización de datos sin perder la capacidad de auditar cuántos incidentes hubo, de qué tipo y cómo se resolvieron

---

### Criterios de Aceptación (CA)
- [ ] CA1: Trabajo asíncrono purga texto de mensajes, motivos libres y evidencias vencidas según período configurado.
- [ ] CA2: Conserva métricas y registros agregados (curso, fecha, tipo de incidente, resolución) sin texto sensible.
- [ ] CA3: Decisión ALLOW no almacena texto de mensaje desde el origen (CA2 de H01).
- [ ] CA_negativo_1: Una vez purgado, el texto no puede ser recuperado ni expuesto.

---

### Tareas SMART desglosadas (18 h totales)
- [ ] T01 (5 h): Job programado de purga de texto con Spring Scheduler y ShedLock.
- [ ] T02 (4 h): Tabla de estadísticas agregadas inmutables y migración Flyway.
- [ ] T03 (5 h): Sanitización estricta de logs y eventos para evitar fuga de textos censurados.
- [ ] T04 (4 h): Pruebas de integración de ciclo de vida y verificación de irreversibilidad de purga.
```

---

## 4. Instrucciones para la Carga en Taiga

1. **Crear o seleccionar las Épicas en Taiga:**
   - Asegurarse de tener creadas en el módulo **Epics** de Taiga:
     * `EP-01 · Plataforma, Infraestructura y Seguridad Transversal`
     * `EP-02 · Proveedor de Modelos e Inteligencia Artificial`
     * `EP-03 · Banco de Casos de Referencia (Golden Set Docente)`
     * `EP-04 · Calibración y Gobernanza del Modelo`
     * `EP-05 · Tutor Seguro y Guardarraíles`
     * `EP-06 · Consumo y Evaluación Académica`
     * `EP-07 · Operación, Cuotas y Observabilidad`
     * `EP-08 · Moderación integrada del Sistema LLM (F2)`
     * `EP-09 · RAG y Consulta de Material Docente`
2. **Crear cada Tarjeta:**
   - En el **Backlog** de Taiga, hacer clic en *Add User Story* (o *New Story*).
   - En **Subject** (Título), pegar la línea `G03 — <Título>` (ej: `G03 — Que un mensaje del chat se permita o bloquee antes de entregarse`).
   - En **Epic**, seleccionar la épica respectiva (ej: `EP-08`).
   - En **Sprint**, asignar a **Sprint 1**.
   - En **Description**, pegar el bloque Markdown completo provisto en este documento.
3. **Cargar las Tareas Técnicas SMART:**
   - Abrir la tarjeta creada en Taiga.
   - Ir a la sección **Tasks** (Tareas) y crear las tareas con su código (`T01`, `T02`, etc.) y las horas estimadas.
4. **Estimación en Puntos Fibonacci (Planning Poker):**
   - Utilizar como referencia la historia canónica `LLM-EP03-H02` (*Consulta del golden set persistente tras reinicio*), fijándola en **3 o 5 puntos**.
   - Asignar los puntos de las demás HU de valor de forma relativa. Las tareas habilitadoras técnicas no llevan puntos Fibonacci de usuario.
