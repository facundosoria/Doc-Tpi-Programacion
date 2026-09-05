# Tema 07 — Microservicio de Evaluación LLM y Asistencia Socrática
## Guion y Estructura para Defensa Oral (20 Diapositivas)

**Cátedra:** Programación IV — Back End · UTN FRC  
**Contexto:** Trabajo Práctico Integrador · 4.º Cuatrimestre  
**Deck Interactivo de Proyección:** [`presentacion-microservicio-tema07.html`](presentacion-microservicio-tema07.html) (Optimizado para proyector de bajo contraste, lectura lejana y cero scroll)

---

### Diapositiva 1: Portada y Misión Estratégica
* **Titular:** Microservicio `llm-service` (Tema 07 · Repo: `tpi-llm`)
* **Idea Fuerza Oral:** *"Construimos el servicio que le pone nota a cómo un alumno interactúa con la IA y acompaña su aprendizaje de forma socrática, demostrando matemáticamente que la calificación es justa, reproducible y auditable."*
* **Métricas Clave:**
  * **5 Roles de IA:** Agentes especializados con temperaturas y prompts independientes.
  * **6 Endpoints REST:** Prefijo `/api/llm/**` a través del API Gateway.
  * **Kafka Event Bus:** Procesamiento diferido y auditoría inmutable.
  * **PAR-14 Golden Set:** Calibración docente humana ($\le \pm 5$ global, $\le \pm 10$ por dimensión).

---

### Diapositiva 2: ¿Qué ROL tiene este microservicio? (Qué es vs Qué NO es)
* **Titular:** Deslinde Claro de Responsabilidades (`llm-service`)
* **Lo que SÍ construye Tema 07:**
  1. *Tutor Socrático en Tiempo Real:* Guía pedagógica en el IDE sin dar código resuelto (SSE).
  2. *Evaluador Forense de Interacción:* Emite calificación numérica (0-100) en 5 dimensiones tras la entrega.
  3. *Salvaguarda Anti-Fuga (RF-IA-20):* Buffer AST en memoria para impedir entrega de soluciones completas.
  4. *Calibración Matemática:* Runner de Golden Set docente con tolerancias PAR-14.
* **Lo que NO es de Tema 07:**
  * ❌ *Usuarios y Auth:* Propiedad de `users-service (T01)` (Identidad).
  * ❌ *Cursos y Matrícula:* Propiedad de `courses-service (T02)` (Cursos).
  * ❌ *Asignación de XP y Economía:* Propiedad de `challenges-service (T03)` (la IA da números; el motor aplica XP).
  * ❌ *Sandbox Docker:* Propiedad de Tema 06.

---

### Diapositiva 3: ¿Cómo nos conectamos con la plataforma? (Borde y M2M)
* **Titular:** Comunicación Síncrona Segura Micro a Micro
* **Flujo Síncrono M2M:**
  $$\text{Micro Emisor (users, courses, challenges...)} \xrightarrow{\text{Token M2M (60s)}} \text{API Gateway} \xrightarrow{\text{Eureka Discovery}} \text{llm-service}$$
* **Reglas de Oro:**
  * **Cero llamadas directas:** Ningún micro habla directo por IP con otro microservicio. Toda llamada pasa por el Gateway.
  * **Tokens Técnicos M2M:** JWT `type=service`, vida ultra corta (60 segundos), `aud=llm-service` y scopes específicos.
  * **Headers de Confianza:** El Gateway valida y propaga `X-Principal-Type: service`, `traceparent` y `X-Request-Id`.

---

### Diapositiva 4: MAPA VISUAL: ¿Con qué microservicios nos conectamos?
* **Titular:** Mapa de Integración Hub-and-Spoke (`llm-service`)
* **Esquema de Conexiones:**
  * 🔵 **users-service (T01):** `Tokens M2M (60s)` & `JWT RS256` (Síncrono vía Gateway).
  * 🔴 **courses-service (T02):** `GET /api/llm/calibracion` *(¡Bloquea Activación!)* & `POST /api/llm/rag` (RAG).
  * 🟢 **challenges-service (T03):** `intento_cerrado` $\rightarrow$ `score_de_ia_calculado (0-100)` (Asíncrono vía Kafka).
  * 🔵 **practice-service (T05):** `POST /api/llm/stream` (Streaming SSE) & Solución esperada para AST.
  * 🔵 **chat-service (T11):** `POST /api/llm/moderador` (< 300 ms Fail-Closed).
  * 🟣 **admin-service (T12):** Configuración de modelos LLM, disparo de calibración y deriva.

---

### Diapositiva 5: Integración Asincrónica (Apache Kafka / Bus de Eventos)
* **Titular:** Desacoplamiento y Resiliencia con Kafka
* **Eventos Publicados (Outbound):**
  * `score_de_ia_calculado`: Score 0-100, confianza y desglose 5D hacia `challenges-service (T03)`.
  * `score_pendiente_diferido`: Notifica degradación controlada ante caída del proveedor LLM (RF-IA-27).
  * `calibracion_aprobada` / `rechazada`: Notifica a `courses-service (T02)` y `admin-service (T12)` para autorizar paso a *Activo*.
  * `incidente_de_jailbreak`: Alerta al Backoffice ante intentos de bypass.
* **Eventos Consumidos (Inbound):**
  * `intento_cerrado` (`challenges-service`): Dispara la calificación analítica en workers.
  * `curso_archivado` (`courses-service`): Cancela y purga jobs de cohortes concluidas.
  * `modelo_llm_cambiado` (`admin-service`): Dispara recalibración automática (RF-IA-32).
* **Garantía:** Partición por `curso_cohorte_id` para orden secuencial estricto y trazabilidad `traceparent & X-Request-Id`.

---

### Diapositiva 6: ¿Cómo nos comunicamos con el Front End? (Visión General)
* **Titular:** Arquitectura General con el Front End
* **Los 3 Modos de Conexión:**
  1. **Streaming Reactivo (SSE):** Respuestas token a token para el Tutor en el IDE web (`Flux<String>`).
  2. **REST Asíncrono (202 Job):** Carga masiva de PDFs y ejecución de calibración docente con polling/notificación.
  3. **REST Síncrono (200 OK):** Consulta inmediata de la rúbrica 5D y envío de apelaciones.
* **Borde Seguro:** El navegador solo habla con Nginx y el API Gateway; la SPA Angular 18+ nunca conoce IPs de backend.

---

### Diapositiva 7: Front End Modo 1 — Streaming Reactivo SSE (Tutor en IDE Web)
* **Titular:** Streaming Reactivo SSE: Tutor en IDE Web
* **Flujo Gráfico de Conexión:**
  $$\text{1. IDE Web (Chat)} \xrightarrow{\text{POST /api/llm/stream}} \text{2. Gateway} \xrightarrow{} \text{3. Tutor Service (LLM)} \xrightarrow{} \text{4. Guardarraíl AST} \xrightarrow{\text{text/event-stream}} \text{5. Render Tokens}$$
* **Puntos Clave para Exposición:**
  * **En Front End:** Servicio Angular con `EventSourcePolyfill` o `fetch()` con `ReadableStream` para procesar tokens progresivos.
  * **En Back End:** Spring WebFlux emitiendo `Flux<String>` con MIME type `text/event-stream`.
  * **Cero bloqueo:** El alumno experimenta baja latencia percibida al ver la respuesta construirse en tiempo real.

---

### Diapositiva 8: Front End Modo 2 — REST Asíncrono (Workbench Docente & Calibración)
* **Titular:** REST Asíncrono: Workbench Docente & Calibración
* **Flujo Gráfico de Conexión:**
  $$\text{1. Panel Docente} \xrightarrow{\text{POST /api/llm/calibracion (JWT Usuario)}} \text{2. Gateway} \xrightarrow{} \text{3. Redis Queue (202 Accepted)} \xrightarrow{} \text{4. Worker Spring (PAR-14)} \xrightarrow{\text{Polling GET /api/llm/calibracion/\{job\_id\}}} \text{5. Matriz Discrepancias}$$
* **Puntos Clave para Exposición:**
  * **Por qué es Asíncrono:** La calibración evalúa 40 transcripciones a ciegas (toma ~30 seg). El front recibe `job_id` inmediatamente (`HTTP 202`).
  * **Visualizador de Discrepancias:** Al terminar, Angular renderiza la matriz de correlación resaltando discrepancias inter-docente superiores a $\pm 10$ puntos.

---

### Diapositiva 9: Front End Modo 3 — REST Síncrono (Visor de Rúbrica & Apelación)
* **Titular:** REST Síncrono: Visor de Rúbrica & Apelación
* **Flujo Gráfico de Conexión:**
  $$\text{1. Vista Alumno} \xrightarrow{\text{GET /api/llm/desglose/\{id\}}} \text{2. Gateway} \xrightarrow{} \text{3. PostgreSQL Read} \xrightarrow{\text{200 OK}} \text{4. Rúbrica 5D} \xrightarrow{\text{POST /api/llm/apelar}} \text{5. Encolado Apelación}$$
* **Puntos Clave para Exposición:**
  * **Transparencia Total (RF-IA-16):** El alumno consulta de inmediato su nota (0-100), el desglose ponderado de las 5 dimensiones y la justificación.
  * **Garantía de Apelación (RF-IA-18):** Si el estudiante no está de acuerdo, presiona "Apelar", lo que encola una auditoría manual docente con congelamiento preventivo de nota.

---

### Diapositiva 10: Stack Tecnológico Backend (Java 21 / Spring Boot 3.4 en 3 Columnas)
* **Titular:** Backend Empresarial: Java 21 / Spring Boot 3.4
* **Desglose en 3 Columnas Arquitectónicas:**
  1. **Columna 1 (Capa Web & Reactiva / Edge):**
     * `Spring WebFlux (SSE)`: Streaming reactivo de tokens con `Flux<String>` para el tutor socrático (latencia < 2s).
     * `Controladores REST (/api/llm/**)`: `/stream`, `/evaluaciones`, `/rag`, `/moderador`.
     * `Seguridad M2M & Cuotas`: JWT firmado, Token M2M (60s `aud=llm-service`), headers `X-Principal-Type` y `Bucket4j` (15 req/min).
  2. **Columna 2 (Inferencia & Guardarraíles):**
     * `Spring AI ChatClient`: Abstracción agnóstica multi-modelo (Gemini 2.5 Pro/Flash, GPT-4o, Ollama contingencia).
     * `Enrutamiento Dinámico`: Mapeo `funcion -> proveedor + modelo` administrado en base de datos sin redeploy.
     * `Buffer AST en Memoria`: Egress Interceptor con `JavaParser` que compara árboles sintácticos y bloquea entregas con similitud > 70%.
  3. **Columna 3 (EDA, Resiliencia & Datos):**
     * `Apache Kafka (Event-Driven)`: Listeners `entrega_realizada` y Producers `evaluacion_completada` particionados por `curso_cohorte_id`.
     * `Eureka & OpenFeign M2M`: Service Discovery e invocaciones cliente-servidor nativas Spring Cloud sin fricción.
     * `Resilience4j (RF-IA-27)`: Circuit Breaker con fallback de degradación elegante (XP base + encolado diferido), Retry y DLQ.
* **Fundamento de Cátedra:** 8 de las 12 capas son enterprise nativas en Java, con tipado estricto y parsing AST nativo.

---

### Diapositiva 11: Base de Datos Propia (PostgreSQL 16 + pgvector + Redis en 3 Columnas)
* **Titular:** Persistencia y Almacenamiento
* **Desglose en 3 Columnas de Almacenamiento Especializado:**
  1. **Columna 1 (PostgreSQL 16 Transaccional ACID):**
     * `evaluaciones_5d`: Registro inmutable de notas (0-100), ponderaciones 5D, feedback Markdown y hashes de auditoría.
     * `intentos_sesion`: Log completo de interacciones socráticas, consumo de tokens, timestamps y commit Git evaluado.
     * `rubricas_config & golden_set`: Rúbricas activas versionadas y casos canónicos para calibración PAR-14 ($\le \pm 5$).
     * *Integridad:* Spring Data JPA + Flyway Migrations con trazabilidad absoluta.
  2. **Columna 2 (pgvector RAG Semántico HNSW):**
     * `Chunks Curriculares`: Fragmentos de apuntes y guías de cátedra vectorizados con metadatos.
     * `Índice HNSW Nativo`: Búsqueda de similitud coseno ultrarrápida (< 5 ms) embebida en Postgres sin bases externas.
     * `Perímetro por curso_id`: Filtrado estricto en BD (Capa 3) que impide contaminación de contexto entre materias.
  3. **Columna 3 (Redis 7 en Memoria, Colas & Rate Limit):**
     * `Cola Interna de Jobs`: Buffer para peticiones `HTTP 202 Accepted` de evaluación antes de workers asíncronos.
     * `Token Bucket Distribuido`: Backend atómico para `Bucket4j`, protegiendo costos y cuotas por alumno.
     * `Caché de System Prompts`: Almacenamiento en RAM de plantillas versionadas con latencia sub-milimétrica (< 1 ms).
* **Regla de Cátedra (Database-per-Service):** `llm-service` es dueño absoluto de su base de datos con aislamiento total.

---

### Diapositiva 12: Panorámica General de los 5 Roles de IA (RF-IA-23)
* **Titular:** Matriz de Especialización de Agentes (RF-IA-23)
* **Idea Fuerza Oral:** *"No existe un único 'prompt mágico'; la inteligencia artificial se desacopla en 5 agentes especializados con parámetros, SLA y guardarraíles independientes."*
* **Los 5 Roles en Pantalla:**
  1. **1. Moderador:** `Sync Fail-Closed` | $T = 0.0$ | $< 300\text{ ms}$ | Canal Chat & Borde
  2. **2. Tutor Socrático:** `Sync Stream SSE` | $T = 0.25$ | $< 2\text{ s}$ | Canal IDE Web
  3. **3. Evaluador 5D:** `Async Worker EDA` | $T = 0.0, \text{Seed}=42$ | $\text{PAR-14} \le \pm 5$ | Canal Kafka Bus
  4. **4. Generador:** `Async + Gate Humano` | $T = 0.70$ | Taxonomía Bloom 1-5 | Canal Backoffice
  5. **5. Asistente RAG:** `Sync pgvector HNSW` | $T = 0.10$ | Retrieval $< 5\text{ ms}$ | Canal REST Gateway

---

### Diapositiva 13: Rol 1 — Moderador de Contenidos y Chat (RF-IA-23)
* **Titular:** Moderador Perimetral & Anti-Jailbreak
* **Parámetros:** Síncrono `Fail-Closed`, $T = 0.0$, Timeout $< 300\text{ ms}$, Retención Preventiva ante caída.
* **Prompt & Guardarraíles:** Aislamiento `<message_content>`, Capa 1 (Regex ~1ms) + Capa 4 (Clasificador paralelo).
* **Salida:** JSON estructurado `{"approved": false, "flag": "CODE_LEAK", "action": "BLOCK"}`.
* **Principio Clave:** Si el moderador falla o tarda más de 300 ms, el mensaje se retiene preventivamente para proteger la plataforma.

---

### Diapositiva 14: Rol 2 — Tutor Socrático en Vivo (RF-IA-01 / RF-IA-04)
* **Titular:** Tutor Socrático en Tiempo Real (IDE Web)
* **Parámetros:** Síncrono Streaming SSE (`Flux<String>`), $T = 0.25$, Latencia 1er token $< 2\text{ s}$.
* **Prompt & Guardarraíles:** **Minimización de Contexto (Capa 5)**: *La solución de referencia jamás entra al prompt*. Directiva socrática de repreguntas y pistas graduales. Guardarraíl Capa 6 (Buffer AST `JavaParser` $\le 70\%$).
* **Salida:** Streaming token a token en el IDE orientando al alumno sin entregar código terminado (`data: {"token": "..."}`).

---

### Diapositiva 15: Rol 3 — Evaluador Forense Multi-Criterio 5D (RF-IA-12 / RF-IA-16)
* **Titular:** Evaluador Multi-Criterio 5D y Golden Set
* **Parámetros:** Asíncrono Worker EDA (Kafka topic `entrega_realizada`), $T = 0.0$, $\text{Seed} = 42$ (determinismo estricto).
* **Prompt & Guardarraíles:** Rúbrica Oficial 5D ponderada (Autonomía 30%, Claridad 25%, Progresión 20%, Cumplimiento 15%, Eficiencia 10%), Rúbrica declarativa (RF-IA-15/29), Golden Set bloqueante PAR-14 ($\le \pm 5$).
* **Salida:** JSON estructurado con vector 5D (`auton, clar, prog, cump, efic`), score 0-100 y `deterministic: true`.

---

### Diapositiva 16: Rol 4 — Generador de Desafíos & Preguntas (RF-IA-07)
* **Titular:** Generador Procedural Asistido con Human-in-the-Loop
* **Parámetros:** Asíncrono con *Human-in-the-Loop Gate*, $T = 0.70$ (variabilidad creativa), SLA diferido.
* **Prompt & Guardarraíles:** Taxonomía de Bloom (dificultad 1-5), inyección curricular, control docente obligatorio antes de publicación.
* **Salida:** JSON con estado `PENDIENTE_DOCENTE`.

---

### Diapositiva 17: Rol 5 — Asistente RAG Curricular (RF-IA-06)
* **Titular:** Asistente RAG Curricular Acotado a Apuntes Oficiales
* **Parámetros:** Síncrono pgvector HNSW, $T = 0.10$ (fidelidad extrema), Timeout $< 5\text{ s}$, Retrieval $< 5\text{ ms}$.
* **Prompt & Guardarraíles:** Grounding férreo a apuntes oficiales, Capa 3 (Perímetro por `curso_id`), Cero Alucinación.
* **Salida:** Respuesta con citación bibliográfica oficial verificable (`{"respuesta": "...", "fuente": "Unidad 4, pág. 12"}`).

---

### Diapositiva 18: Pipeline de Seguridad: Las 6 Capas de Defensa
* **Titular:** Defensa en Profundidad (5 Capas de Entrada + 1 Capa de Salida)
* **Las 6 Capas Consecutivas:**
  * **Capa 1 (Entrada):** Filtros Determinísticos Regex (~1 ms) contra patrones directos y base64.
  * **Capa 2 (Estructura):** Separación Estructural (0 ms) aislando el input en `<untrusted_data>`.
  * **Capa 3 (Retrieval):** Perímetro Temático en PostgreSQL por `curso_id` (RF-IA-06).
  * **Capa 4 (Semántica):** Clasificador de Intención en Paralelo (evalúa jailbreak reteniendo salida).
  * **Capa 5 (Contexto):** Minimización Absoluta de Contexto (**la solución de referencia NUNCA entra al prompt del tutor**).
  * **Capa 6 (Salida):** Guardarraíl Anti-Fuga (RF-IA-20) con análisis AST en memoria y umbral del 70%.

---

### Diapositiva 19: Salvaguarda Anti-Fuga (RF-IA-20) y Análisis AST
* **Titular:** Anti-Fuga de Solución: Buffer AST con JavaParser
* **Diagrama de Secuencia del Interceptor:**
  $$\text{1. Retención en Memoria} \longrightarrow \text{2. JavaParser AST} \longrightarrow \text{3. Test Similitud > 70\%} \longrightarrow \text{4. Entrega o Regenera}$$
* **Por qué AST:** Un alumno puede cambiar nombres de variables y formato; la comparación de AST verifica la **estructura lógica y algoritmo real**.

---

### Diapositiva 20: Gobernanza, Calibración, Frontera del XP y Resiliencia (RF-IA-27)
* **Titular:** Resumen Ejecutivo Integral
* **Rate Limiting en 3 Capas:** Red (`Bucket4j` 15 req/min), Negocio (10-20 preguntas/desafío) y Proveedor (`Resilience4j` anti-DoW).
* **Rúbrica 5D & Golden Set:** Scoring híbrido (60% código determinístico), tolerancias PAR-14 ($\le \pm 5$ global, $\le \pm 10$ individual), activación bloqueante (RF-IA-36).
* **Frontera de Gamificación y XP:** Tema 07 emite vector numérico (0-100); Tema 03 es el único dueño de la economía y aplica `PAR-05 (±20%)`.
* **Resiliencia Diferida (RF-IA-27):** Caída del proveedor LLM nunca bloquea la entrega del alumno (XP base otorgado de inmediato, scoring diferido).
* **Cierre:** Arquitectura empresarial desacoplada, auditable, segura y pedagógicamente centrada en el aprendizaje del estudiante.

