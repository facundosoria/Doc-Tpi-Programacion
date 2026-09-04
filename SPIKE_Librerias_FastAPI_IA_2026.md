# SPIKE: Librerías FastAPI para el Motor de IA del LLM — Investigación 2026

> **Propósito**: Descubrir y evaluar librerías Python que optimicen, ayuden, simplifiquen o resuelvan problemas y requisitos funcionales (RF) dentro del **microservicio de IA `ms-evaluacion-llm`**.
>
> **Alcance**: Motor interno **FastAPI Core Engine (:8082)** en arquitectura híbrida con Spring Boot Sidecar (:8081), plataforma gamificada UTN FRC.
>
> **Fecha de vigencia**: Septiembre 2026.

---

## Tabla de contenido

1. [Resumen ejecutivo y contexto del proyecto](#1-resumen-ejecutivo-y-contexto-del-proyecto)
2. [Tabla maestra de librerías](#2-tabla-maestra-de-librerías)
3. [Categoría 1 — Orquestación LLM / Agentes](#3-categoría-1--orquestación-llm--agentes)
4. [Categoría 2 — Guardrails y seguridad anti-jailbreak](#4-categoría-2--guardrails-y-seguridad-anti-jailbreak)
5. [Categoría 3 — Streaming SSE](#5-categoría-3--streaming-sse)
6. [Categoría 4 — Análisis AST y similitud de código](#6-categoría-4--análisis-ast-y-similitud-de-código)
7. [Categoría 5 — RAG y búsqueda vectorial](#7-categoría-5--rag-y-búsqueda-vectorial)
8. [Categoría 6 — Tareas asíncronas y workers](#8-categoría-6--tareas-asíncronas-y-workers)
9. [Categoría 7 — Sanitización de PII](#9-categoría-7--sanitización-de-pii)
10. [Categoría 8 — Resiliencia y rate-limiting](#10-categoría-8--resiliencia-y-rate-limiting)
11. [Categoría 9 — Observabilidad y LLMOps](#11-categoría-9--observabilidad-y-llmops)
12. [Decisión final: stack combinado recomendado](#12-decisión-final-stack-combinado-recomendado)
13. [Mapa de trazabilidad librería → RF-IA → fase](#13-mapa-de-trazabilidad-librería--rf-ia--fase)
14. [Referencias 2026](#14-referencias-2026)

---

## 1. Resumen ejecutivo y contexto del proyecto

### 1.1. Qué es el proyecto

La plataforma gamificada de la **UTN FRC (Programación IV / Back End)** integra un microservicio de IA para tutoría socrática, evaluación forense de LLM y ciberseguridad anti-jailbreak. La arquitectura es **híbrida de dos runtimes**:

```
Frontend (Angular/React, Monaco IDE)  ──HTTPS/WSS──▶  Spring Cloud Gateway (:8080)
                                                          │  Netflix Eureka (:8761)
                                                          ▼
                                            ms-evaluacion-llm
                                              ├─ Spring Boot Sidecar (:8081)  ← Eureka, Keycloak JWT, AMQP
                                              └─ FastAPI Core Engine (:8082)  ← Motor de IA/LLM (Python)
                                                          │
                                    PostgreSQL 16 + pgvector · Redis 7.2 · LLMs cloud (Gemini/OpenAI/Claude)
```

La cosede exige Java para el perímetro (Eureka/AMQP/Keycloak), pero el **ecosistema de IA/AST/embeddings es superior en Python**, por eso el **Motor FastAPI** es el foco de este SPIKE (`ADR-005`).

### 1.2. Los 5 roles de IA (RF-IA-23) que el motor FastAPI debe atender

| # | Rol | Tiempo límite | Temperatura | Modelo | Naturaleza |
|---|-----|---------------|--------------|--------|------------|
| 1 | Moderador Chat | 1.0 s | 0.00 | Gemini Flash-Lite | Sync, fail-closed (portero) |
| 2 | **Tutor Socrático** | 45.0 s | 0.25 | Flash-Lite | **Streaming SSE + Buffer AST** |
| 3 | **Evaluador 5D** | 120.0 s | 0.00 (Seed=42) | Claude Haiku 4.5 | **Asíncrono (Celery/AMQP)** |
| 4 | Generador de desafíos | 30.0 s | 0.70 | Flash-Lite | On-demand |
| 5 | Asistente RAG | 5.0 s | 0.10 | pgvector | Recuperación semántica |

### 1.3. Requisitos funcionales (RF-IA) clave que orientan la selección

- **RF-IA-11 / 26 / 27**: Agnosticismo multi-proveedor, cascada de fallback, tolerancia a fallos con factor neutro 1.0.
- **RF-IA-20 + PAR-11**: Salvaguarda técnica **anti-fuga** — buffer AST que bloquea código con similitud ≥ 70% en streaming.
- **RF-IA-05 / 07 / 10 / 14**: Filtro de intención, medidas anti-jailbreak, bloqueo silencioso, anti-manipulación en evaluación.
- **RF-IA-08 / ADR-004 / 006**: RAG con **pgvector**, embeddings locales multilingües, índice HNSW.
- **RF-IA-27 / 32 / 34**: Evaluación diferida, **drift por Golden Set (MAE)** nocturno, bloqueo de cierre.
- **RF-IA-22 / ADR-013**: Cuotas FinOps (50k tokens/alumno, HTTP 402), token bucket en Redis.
- **RF-IA-30 / 31**: Golden Set en dos niveles y calibración con tolerancia PAR-14 (±5 pts).

---

## 2. Tabla maestra de librerías

| Categoría | Librería | Para qué sirve | RF-IA que resuelve | Licencia | 2026 |
|-----------|----------|----------------|--------------------|----------|------|
| Orquestación LLM | **Pydantic AI** | Agentes tipados en Python, salida estructurada validada, streaming async nativo | 12, 13, 15, 23 | MIT | Activo, V2 (jun 2026) |
| Orquestación LLM | **LiteLLM** | Gateway unificado 100+ proveedores, failover, cost-tracking | 11, 26, 27 | MIT | Activo |
| Orquestación LLM | LangChain/LangGraph | Abstracción amplia, orquestación por grafos | 08, 11 | MIT | Activo (v1.0) |
| Guardrails | **Guardrails AI** | Validadores composables de I/O, output schema, prompt injection | 05, 07, 10, 14, 20 | Apache 2.0 | Activo |
| Guardrails | NeMo Guardrails | Control de flujo conversacional (dialogue rails) | 05, 07 | Apache 2.0 | Activo |
| Streaming | **sse-starlette** | SSE estándar W3C para FastAPI, ping/disconnect/shutdown | 01, ADR-009 | BSD | Activo (v3.4.x) |
| Streaming | **FastAPI `fastapi.sse`** | SSE nativo (FastAPI ≥ 0.135), `EventSourceResponse` | 01, ADR-009 | MIT | Nativo |
| AST / similitud | **tree-sitter + tree-sitter-languages** | Parser sintáctico incremental multilingüe (C) | 20, PAR-11 | MIT | Activo |
| AST / similitud | `ast.parse` + Levenshtein | Parsing nativo Python + distancia normalizada | 20 | stdlib | — |
| RAG | **pgvector** | Vectores en PostgreSQL + HNSW (sin SaaS extra) | 08, ADR-004 | Postgres | Activo |
| RAG | **sentence-transformers** | Embeddings locales multilingües, cross-encoder re-rank | 08, 06, ADR-006 | Apache 2.0 | Activo |
| RAG | LlamaIndex | Framework RAG integral (chunking, retrieval) | 08 | MIT | Activo |
| Workers | **Celery + Celery Beat** | Cola distribuida madura, cron, routing, multi-broker | 27, 32, 34, ADR-003 | BSD | Activo (estándar) |
| Workers | ARQ | Cola asyncio-nativa sobre Redis, tareas async | 27, 32 | MIT | **Maintenance-only** |
| PII | **Microsoft Presidio** | Detección + anonimización de datos personales | 05 (capa 2), GDPR | MIT | Activo (2.2.x) |
| Resiliencia | **Tenacity** | Retries con backoff + jitter (anti Thundering Herd) | 27, 22 | Apache 2.0 | Activo |
| Resiliencia | `asyncio.Semaphore` | Límite de concurrencia (25 slots) | NFR-03 | stdlib | — |
| Resiliencia | **redis (token bucket)** | Contabilidad atómica de cuotas FinOps | 22, ADR-013 | MIT | Activo |
| Observabilidad | **Pydantic Logfire** | Trazas de agentes, costos, latency, evals | 30, 32 | MIT | Activo |
| Observabilidad | **OpenTelemetry / Prometheus** | Métricas estandarizadas, drift, dashboards | 32, Plan-10 | Apache 2.0 | Activo |

> Marca **negrita** = recomendación principal por cada categoría; ~~tachado~~ o anotado = alternativa con advertencia.

---

## 3. Categoría 1 — Orquestación LLM / Agentes

### 3.1. Pydantic AI ⭐ (Recomendado)

- **Para qué sirve**: SDK/framework Python para construir **agentes tipados** con LLMs. Es la evolución natural del ecosistema Pydantic (la base de validación de FastAPI) aplicado a agentes. Convierte una llamada LLM en una función bien tipada: defines `result_type` como modelo Pydantic y el framework **valida, repite (retry) y devuelve un objeto Python tipado**.

- **Caso de uso en tu proyecto**: Es el reemplazo ideal de los adaptadores Java que hoy usan `BeanOutputConverter`. El **Evaluador 5D (RF-IA-15)** necesita que el LLM devuelva exactamente 5 dimensiones con pesos fijos (30/25/20/15/10) y un `DesgloseScore`. Con Pydantic AI defines ese modelo y el framework **garantiza** el contrato: si el LLM responde JSON inválido o con `"twenty"` en vez de `20.0`, reintenta hasta devolver un objeto válido. También encaja para el **Portero/Clasificador** (RF-IA-05) y el **Tutor** (streaming async).

- **Cómo se integra (ejemplo — Evaluador 5D determinista con T=0, Seed=42)**:

```python
from pydantic import BaseModel, Field
from pydantic_ai import Agent, RunContext

class Score5D(BaseModel):
    autonomia: int = Field(ge=0, le=100)      # peso 30%
    claridad_prompts: int = Field(ge=0, le=100)  # peso 25%
    progresion_logica: int = Field(ge=0, le=100) # peso 20%
    cumplimiento_anti_jailbreak: int = Field(ge=0, le=100)  # peso 15%
    eficiencia: int = Field(ge=0, le=100)      # peso 10%
    justificacion: str

# Un solo proveedor: Claude Haiku T=0, Seed=42 (ADR-010 reproducebilidad)
agent = Agent(
    "anthropic:claude-haiku-4-5",
    result_type=Score5D,
    model_settings={"temperature": 0.0, "seed": 42},
)

async def evaluar(transcripcion: str, rubrica: Rubrica) -> Score5D:
    # La rubrica viaja como dependency tipada; la transcripcion como data hermética
    result = await agent.run(
        f"Evalúa esta transcripción contra la rúbrica. {rubrica}",
        deps={"transcript": transcripcion},  # separa "datos" de "instrucciones" (RF-IA-14)
    )
    return result.data  # SIEMPRE es un Score5D válido o lanza error
```

- **Beneficios**:
  - **Type safety de punta a punta**: `result.data` siempre es un `Score5D`; tupo y tests lo verifican. (Benchmarks Nextbuild 2026: Pydantic AI atrapó 23 bugs de producción que LangChain dejó pasar; DX 8/10 vs 5/10 de LangChain.)
  - **Validation retries automáticos**: si el LLM intenta devolver `"veinte"`, reintenta hasta `20`; ideal para que las notas nunca sean `NaN`.
  - **Streaming async nativo** compatible con `StreamingResponse` de FastAPI (Tutor, ~40 líneas vs ~80 con LangChain).
  - **Soporte multi-modelo nativo**: OpenAI, Anthropic, Gemini, Groq, Ollama — cambia proveedor con un string (RF-IA-11/28).
  - **Dependency injection** (`RunContext`): testing trivial, sin estado global.
  - **Usage Limits** de fábrica (cap de tokens) — alineado con FinOps `RF-IA-22`.

- **Por qué usarla**: Tu stack ya es Python + FastAPI + Pydantic v2. Pydantic AI es "el FastAPI feeling aplicado a GenAI" (mismo equipo Pydantic). Cumple el **ADR-010** (determinismo T=0/Seed), **RF-IA-13/29** (rúbrica declarativa versionada) y **RF-IA-14** (separación instrucción/dato), con la menor fricción y mejorabilidad de seguridad/tipo de todas las opciones. Es la recomendación #1 del ecosistema 2026 para proyectos FastAPI nuevos de producción.

---

### 3.2. LiteLLM ⭐ (Recomendado — capa de infraestructura)

- **Para qué sirve**: Es un *gateway* / adaptador universal de LLMs: una **única API estilo OpenAI** para 100+ proveedores. Piensa en él como el "Nginx de las llamadas LLM": **rutea, balancea, cachea y registra costos** entre proveedores.

- **Caso de uso en tu proyecto**: Resuelve directamente **RF-IA-11** (agnosticismo de proveedor), **RF-IA-26** (multimodelo en pool para roles operativos) y **RF-IA-27/28** (fallback automático en cascada y cambio en caliente de proveedores sin tocar código). Si Gemini responde `429`, LiteLLM **reintenta con Claude automáticamente**; lleva **cost tracking por usuario**, ideal para el presupuesto de USD 15-22 del cuatrimestre (FinOps).

- **Cómo se integra** (modo Proxy: `POST /chat/completions` unificado):
```
# docker-compose: litellm proxy en :4000 bloquea la complejidad multi-proveedor
litellm:
  image: ghcr.io/berriai/litellm:main
  environment:
    GEMINI_API_KEY: ${GEMINI_API_KEY}
    ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
    OPENAI_API_KEY: ${OPENAI_API_KEY}
    LITELLM_MASTER_KEY: ${LITELLM_MASTER_KEY}
```
```python
# En FastAPI: asignamos rol -> modelo (RF-IA-23) vía LiteLLM
from openai import AsyncOpenAI

client = AsyncOpenAI(base_url="http://litellm:4000", api_key="sk-your-key")

ROLES = {
    "portero": "gemini/gemini-3.5-flash-lite",
    "tutor": "gemini/gemini-3.5-flash-lite",
    "evaluador": "anthropic/claude-haiku-4-5",   # RF-IA-25: UNICO activo global
    "generador": "gemini/gemini-3.5-flash",
}

async def call(role: str, prompt: str, *, stream: bool = False):
    resp = await client.chat.completions.create(
        model=ROLES[role],
        messages=[{"role": "user", "content": prompt}],
        stream=stream,
        temperature=0.25 if role == "tutor" else 0.0,
    )
    return resp
```

- **Beneficios**:
  - **Failover/cascada**: fallback entre proveedores sin cambiar código (`RF-IA-27`).
  - **Cost tracking por request**: tokens in/out + costo + latencia por rol/clave.
  - **Rate limiting y caching** a nivel de gateway: reduce 429 y costo.
  - **Router con estrategias** (round-robin, por latencia, por costo) → `RF-IA-26`.

- **Por qué usarla**: No compite con Pydantic AI; **componen**. **LiteLLM = infraestructura** (routing/failover/costos) y **Pydantic AI = capa de aplicación** (tipos/validación). La arquitectura de producción 2026 recomendada es: **agentes Pydantic AI → apuntan al proxy LiteLLM → LiteLLM enruta a Gemini/Claude/OpenAI**. Tu proyecto cumple así RF-IA-11/26/27/35 con una sola integración y sin acoplarte a SDK propietario.

---

### 3.3. LangChain / LangGraph (Considerar — con advertencia)

- **Para qué sirve**: Framework de orquestación LLM con el ecosistema más grande (600-1000+ integraciones: vector stores, document loaders, memory, parsers) y **LangGraph** para flujos por grafos con estado, checkpoints y human-in-the-loop.

- **Caso de uso en tu proyecto**: Potencial en **RAG complejo** (RF-IA-08, Fase 3) con document loaders y retrievers listos, o en **LangGraph** para pipelines evaluativos multi-paso con persistencia.

- **Cómo se integraría** (RAG simple):
```python
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import PGVector

vectorstore = PGVector.from_existing_index(
    embedding=OpenAIEmbeddings(),
    collection_name="curso_embeddings",
    connection_string="postgresql+psycopg://...",
)
retriever = vectorstore.as_retriever(search_kwargs={"k": 3})
```

- **Beneficios**: máxima amplitud de integraciones; enormes comunidad/tutoriales; LangSmith para tracing.

- **Por qué NO usarla como base aquí (2026)**:
  - **Dependencias pesadas** y abstracciones (LCEL/Runnables) que oscurecen qué ocurre; DX 5/10 en benchmark Nextbuild.
  - **Structured outputs como add-on** (`.with_structured_output()`), no forzado ni validado por defecto en todos los proveedores — crítico para la integridad de notas del Evaluador 5D.
  - **Curva de aprendizaje media-alta**; para tu caso (5 roles bien definidos + RAG sobre pgvector + seguridad custom) el peso de LangChain no se justifica.

- **Conclusión**: úsalo **solo** si en Fase 3 necesitas document loaders de muchos formatos o grafos evaluativos complejos con LangGraph. Para el núcleo, **Pydantic AI + pgvector directo** es más liviano, tipado y auditable.

---

## 4. Categoría 2 — Guardrails y seguridad anti-jailbreak

### 4.1. Guardrails AI ⭐ (Recomendado — capa de validación I/O)

- **Para qué sirve**: Framework Python de **validación de entradas y salidas de LLMs** mediante *validators* composables (como "Pydantic para seguridad de LLM"). Detecta prompt injection, filtra toxicidad, redacta PII y **fuerza schema de salida**, con modos `pass / filter / fix / exception` por validación.

- **Caso de uso en tu proyecto**: Complementa (no reemplaza) tu **pipeline de 5 capas**. Aporta validadores probados para **CAPA 1 (Harmlessness)** y **CAPA 2 (PII)** y garantiza que el JSON del **Evaluador** cumpla el esquema antes de persistir. Benchmark 2026: ~84% detección de inyección, 92% PII, 5% falsos positivos, ~320ms overhead (p50).

- **Cómo se integra**:
```python
from guardrails import Guard
from guardrails.hub import DetectPromptInjection, PIIRedaction, ValidJSON

guard = Guard().use_many(
    DetectPromptInjection(on_fail="exception"),
    PIIRedaction(on_fail="fix"),        # CAPA 2
    ValidJSON(on_fail="exception"),      # Evaluador 5D
)

@router.post("/api/v1/tutor/stream")
async def tutor(req: TutorRequest):
    validated = await guard.validate_async(req.prompt)
    return stream_tutor(validated.validated_output)
```

- **Beneficios**:
  - **Hub con 50+ validadores** (regex, PII, JSON schema, toxicidad, SQL injection).
  - **Componible**: agrega una verificación con una línea; granularidad de fallo por validador.
  - **Bajo overhead** para validadores regex (sub-ms); modelo para detección de inyección.
  - Integración con Pydantic para **structured output enforcement**.

- **Por qué usarla**: Es la capa "Pydantic para LLM safety" que encaja con tu arquitectura determinista (mismo patrón mental que tus guards deterministas de PAR-11). Añade detección de inyección de prompt **de la comunidad red-team** que hoy ya intentas emular con regex manuales en `Capa 1`.

> **Nota 2026 importante**: **LLM Guard** (otra opción popular de escáneres) fue **archivado por Protect AI el 09/07/2026** (read-only, sin mantenimiento). Recomendación: **no adoptarlo como dependencia nueva** en 2026; si el repo ya lo usaba, migrar los escáneres a Guardrails AI o LLMArmor.

---

### 4.2. NeMo Guardrails (Considerar — capa de flujo conversacional)

- **Para qué sirve**: Tool de NVIDIA para definir **rails de diálogo** con un DSL llamado **Colang**. Controla el *flujo* de la conversación: qué temas están permitidos, cuándo rechazar, cómo redirigir. 5 tipos de rails: input, dialog, retrieval, execution, output.

- **Caso de uso en tu proyecto**: Fuerte para el **Tutor Socrático multi-turno** que debe mantenerse *on-topic* (RF-IA-05/06) y neutralizar **Crescendo** (inercia progresiva en 5 turnos). Su *retrieval rail* es útil para filtrar chunks del RAG antes de que lleguen al modelo.

- **Cómo se integraría** (archivo Colang):
```colang
define user ask for solution
  "dame el código"
  "resuelve la función"

define flow tutor refusals
  user ask for solution
  bot inform "Te guío con el algoritmo, pero no escribiré la solución (RF-IA-04)."
```

- **Beneficios**: excelente para control de tópicos y flujo multi-turno; fuerte anti-jailbreak conversacional; <50ms por check en GPU NVIDIA.

- **Por qué evaluarla con cautela**: requiere aprender **Colang** (curva de aprendizaje) y el patrón `dialog matching` puede ser evadido por inyecciones sofisticadas. Benchmark: ~71% detección de inyección, 82% jailbreak, pero **940ms (p50) de overhead** — relevante para tu <800ms TTFT. **Mejor:** usar Guardrails AI para I/O y sumar tu lógica anti-Crescendo en Redis (ya diseñada) en lugar de meter Colang.

---

## 5. Categoría 3 — Streaming SSE

### 5.1. sse-starlette ⭐ (Recomendado) + FastAPI `fastapi.sse` nativo

- **Para qué sirve**: **Server-Sent Events** estándar W3C para Starlette/FastAPI. Permite transmitir tokens **token a token** al navegador (Monaco Editor) con manejo de *ping keep-alive*, detección de desconexión del cliente y **shutdown cooperativo** — exactamente lo que exige tu **Tutor Socrático (<800ms TTFT)** y el **Buffer AST interceptor**.

- **Caso de uso en tu proyecto**: Es el transporte del **Tutor (ADR-009: Streaming SSE con Buffer Interceptor AST)**. El flujo: la prosa fluye en vivo; al detectar apertura de bloque de código (```` ``` ````) **pausa y acumula en RAM**; al cerrar, parsea AST y decide si emite o destruye (PAR-11). sse-starlette da el control fino de eventos con `id`, `retry`, `comment` y `send_timeout`.

- **Cómo se integra** (Buffer interceptor + sse-starlette):
```python
from sse_starlette.sse import EventSourceResponse, ServerSentEvent

@router.get("/api/v1/tutor/stream")
async def tutor_stream(session: SSESession = Depends(get_session)) -> EventSourceResponse:
    async def event_gen():
        state = "OUTSIDE_CODE"; code_buf = []
        async for token in agent.run_stream(session.prompt):  # Pydantic AI
            if state == "OUTSIDE_CODE":
                if "```" in token:
                    state = "INSIDE_CODE"; code_buf.append(token)
                else:
                    yield ServerSentEvent(data=token, event="token")
            else:  # INSIDE_CODE: buffer AST en RAM
                code_buf.append(token)
                if "".join(code_buf).count("```") >= 2:
                    sim = ASTSimilarity.calculate(extract_code(code_buf), session.solution)
                    if sim < 0.70:   # PAR-11 seguro
                        yield ServerSentEvent(data="".join(code_buf), event="code")
                    else:
                        yield ServerSentEvent(data="[Pista: plantea el algoritmo]", event="hint")
                    code_buf, state = [], "OUTSIDE_CODE"
        yield ServerSentEvent(raw_data="[DONE]", event="done")

    return EventSourceResponse(event_gen(), ping=15, send_timeout=5)
```

- **Beneficios**:
  - Cumple el **estándar W3C SSE** (reconexión automática con `Last-Event-ID`).
  - Detección automática de desconexión + `send_timeout` (evita conexiones colgadas).
  - **Graceful shutdown** de los streams al apagar el worker.
  - `JSONServerSentEvent` para enviar objetos tipados.

- **Alternativa nativa**: Desde **FastAPI ≥ 0.135** (2026) existe **`fastapi.sse`** con `EventSourceResponse` y `ServerSentEvent` directamente en FastAPI, soporte **SSE sobre `POST`** (compatible con MCP streaming) y `raw_data` para enviar `[DONE]` sin escapado. Recomendación: usar **`fastapi.sse` nativo** (menos dependencias) y recurrir a **sse-starlette** solo si necesitas funciones avanzadas de control de desconexión multi-loop.

- **Por qué usarla**: Es el estándar de facto para streaming SSE en FastAPI, maduro, mantenido, y resuelve los tres requisitos duros del Tutor: latencia <800ms, keep-alive sobre proxies (Nginx) y cancelación limpia en cortes.

---

## 6. Categoría 4 — Análisis AST y similitud de código

### 6.1. tree-sitter + tree-sitter-languages ⭐ (Recomendado)

- **Para qué sirve**: **Parser sintáctico incremental de alto rendimiento** (C) que construye el **AST** de bloques de código. `tree-sitter-languages` provee wheels binarios con **todas las gramáticas** — crítico porque tu plataforma es **políglota (Java, Python, C#)** (`Plan-08`).

- **Caso de uso en tu proyecto**: Es el motor del **Buffer AST Egress (CAPA 5, RF-IA-20)** y del **AntiFugaService**. Compila gramáticas en C para parsings de bloques de ~100 líneas en **<25ms** (tu DoD del Plan-08), y permite **AST structural clone detection** que ignora renombrados de variables (a diferencia de Levenshtein a nivel de texto). La similitud se computa sobre la estructura del árbol, no sobre strings.

- **Cómo se integra**:
```python
from tree_sitter_languages import get_parser, get_language

parser = get_parser("python")          # también java, c_sharp (Plan-08)

def norm_ast(source: str) -> str:
    tree = parser.parse(source.encode())
    root = tree.root_node
    # walk y emitir solo tipos de nodo + estructura (sin identificadores literales)
    return " ".join(child.type for child in root)

def ast_similarity(gen: str, solution: str) -> float:
    # Similitud estructural normalizada (1 - edit distance normalizada del AST)
    return normalized_tree_edit_distance(norm_ast(gen), norm_ast(solution))
```

- **Beneficios**:
  - **Incremental y rápido** (pensado para streaming).
  - **Multilingüe** con una sola dependencia (`tree-sitter-languages`).
  - Detección de **clones estructurales** (ignora nombres/constantes/comentarios → mejor que Levenshtein).
  - Sin compilar gramáticas manualmente (wheels precompilados).

- **Por qué usarla**: Es la pieza que tu `ADR-005` ya justifica ("ecosistema IA/AST superior en Python"). Complementa `ast.parse` (Python nativo) para el caso monoglota y aporta la cobertura Java/C# que `ast` nativo no tiene. Es el estándar de facto del análisis sintáctico multilingüe en 2026.

---

### 6.2. `ast.parse` + Levenshtein (Complemento)

- **Para qué sirve**: El módulo `ast` de la **stdlib** de Python parsea código Python nativo; la distancia de Levenshtein normalizada mide similitud de texto.

- **Caso de uso**: Serves como **primera pasada barata** (sin dependencias) para bloques Python y como *string-similarity* complementaria al AST. Tu `PRD RF-IA-20` menciona explícitamente "AST o distancia de Levenshtein normalizada".

- **Cómo se integra**:
```python
import ast
from rapidfuzz import fuzz   # o difflib.SequenceMatcher

def syntactically_valid(code: str) -> bool:
    try:
        ast.parse(code); return True
    except SyntaxError:
        return False

def levenshtein_similarity(gen: str, sol: str) -> float:
    return fuzz.ratio(gen, sol) / 100.0  # 0..1
```

- **Beneficios**: cada plataforma ya lo tiene; determinista y testeable.

- **Por qué usarla**: como **primera barrera determinista** de bajo costo antes de invocar el parser C completo, y para validar sintaxis del bloque generado antes del análisis estructural.

---

## 7. Categoría 5 — RAG y búsqueda vectorial

### 7.1. pgvector + asyncpg ⭐ (Recomendado)

- **Para qué sirve**: Extensión de PostgreSQL que añade **vectores + similitud** con índice **HNSW**. Ya definido en tu **ADR-004** ("pgvector en PostgreSQL dedicado del microservicio", corpus <50k chunks, <8ms con ACID nativo, costo cero).

- **Caso de uso en tu proyecto**: Backend del **Asistente RAG (RF-IA-08, Fase 3)** y del **chat social RAG**. Cálculo de embeddings locales multilingües, almacenamiento en `curso_embeddings`, búsqueda por **similitud coseno** con filtro por `course_id` (partición obligatoria).

- **Cómo se integra**:
```python
import asyncpg
from pgvector.asyncpg import register_vector

pool = await asyncpg.create_pool(dsn=URL)

async def search(conn, query_vec, course_id: int, top_k: int = 3):
    await register_vector(conn)
    rows = await conn.fetch(
        """
        SELECT content, 1 - (embedding <=> $1::vector) AS sim
        FROM curso_embeddings
        WHERE course_id = $2
        ORDER BY embedding <=> $1::vector
        LIMIT $3
        """,
        query_vec, course_id, top_k,
    )
    return rows
```
```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE INDEX ON curso_embeddings USING hnsw (embedding vector_cosine_ops)
  WITH (m = 16, ef_construction = 64);
```

- **Beneficios**:
  - **Cero SaaS extra**: un solo Postgres = un backup, un pool, transacciones ACID (relevante para tu inmutabilidad de notas).
  - **HNSW** → búsqueda O(log n) y sub-10ms para 50k chunks.
  - Filtros por metadatos = **SQL ordinario**.
  - Soporte `asyncpg` (driver asíncrono nativo de FastAPI).

- **Por qué usarla**: Tu `ADR-004` ya rechazó Pinecone/Qdrant por costo y vendor-lock; pgvector cumple igual para <50k chunks. Es la opción correcta mientras el repositorio no supere ~500k-1M documentos (umbral indicado para delegar a un motor dedicado).

---

### 7.2. sentence-transformers + cross-encoder re-rank ⭐ (Recommended)

- **Para qué sirve**: Genera **embeddings** de oraciones y ofrece **cross-encoders** para re-rankear resultados. Modelos multilingües (p.ej. `paraphrase-multilingual-MiniLM-L12-v2`, 384D).

- **Caso de uso**: Vectorización on-premise de apuntes (tu `ADR-006`). Un modelo local multilingüe respeta privacidad institucional y costo despreciable. El **cross-encoder re-rank** mejora mucho la precisión del RAG (recall→precisión).

- **Cómo se integra**:
```python
from sentence_transformers import SentenceTransformer, CrossEncoder

embedder = SentenceTransformer("paraphrase-multilingual-MiniLM-L12-v2")
reranker = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")

async def embed_query(text: str):
    return await asyncio.to_thread(embedder.encode, text, normalize_embeddings=True)

async def rerank(query: str, candidates, top_k: int = 3):
    scores = await asyncio.to_thread(reranker.predict, [(query, c) for c in candidates])
    ranked = [c for _, c in sorted(zip(scores, candidates), key=lambda x: -x[0])]
    return ranked[:top_k]
```

- **Beneficios**: embeddings locales (privacidad, `ADR-006`), multi-idioma (apuntes UTN en español), y `asyncio.to_thread` evita bloquear el event loop (CPU-bound).

- **Por qué usarla**: es el estándar para embeddings locales + re-rank en 2026 y encaja con tu decisión de **no** llamar APIs externas por cada query.

---

### 7.3. LlamaIndex (Opcional Fase 3)

- **Para qué sirve**: framework RAG *todo-en-uno*: loaders de PDF, chunking semántico, retrievers, agentes RAG.

- **Caso de uso**: para simplificar la ingesta de PDFs de la coseda (RF-IA-08) y el pipeline de chunking/embedding/retrieval sin escribir cada pieza.

- **Por qué evaluarla**: si el contenido de los apuntes es heterogéneo (PDFs, DOCX) y quieres acelerar Fase 3. Riesgo: más dependencias. Para corpus simple y control total del `course_id` (partición), **pgvector + sentence-transformers directo** es más liviano y testeable.

---

## 8. Categoría 6 — Tareas asíncronas y workers

### 8.1. Celery + Celery Beat ⭐ (Recomendado — decisión de consistency con el stack)

- **Para qué sirve**: **Cola de tareas distribuida** madura con múltiples brokers (Redis, RabbitMQ, SQS), routing por colas y prioridades, **Celery Beat** para tareas programadas (cron). Es el *workhorse* empresarial de Python desde hace +10 años.

- **Caso de uso en tu proyecto**: Ya está presente en tu arquitectura para:
  - **Evaluador asíncrono (ADR-003)**: consume `student.challenge.submitted` (vía RabbitMQ) y ejecuta el Scoring 5D en background, publicando `ai.evaluation.completed`.
  - **Drift nocturno (RF-IA-32, ADR-014)**: **Celery Beat a las 03:00** re-evalúa el **Golden Set** y calcula el **MAE**; si > 5.0 abre circuit breaker (HTTP 503).
  - **Evaluación diferida (RF-IA-27)**: cola `evaluaciones_pendientes` con backoff y reprocesamiento.

- **Cómo se integra**:
```python
# tasks.py
from celery import Celery
celery_app = Celery("ms_eval", broker="pyamqp://guest@rabbitmq//", backend="redis://redis:6379/0")

@celery_app.task(bind=True, max_retries=3, default_retry_delay=60)
def evaluar_5d(self, session_id: str):
    try:
        score = run_scoring_5d(session_id)   # Pydantic AI + T=0
        guard_validar(score)
        persistir_inmutable(score)           # scores_ia (trigger inmutabilidad)
        publicar_amqp("ai.evaluation.completed", score)
    except ProviderDown as exc:
        raise self.retry(exc=exc)

# beat — drift nocturno
celery_app.conf.beat_schedule = {
    "drift-golden-set": {
        "task": "tasks.calcular_mae_golden",
        "schedule": crontab(hour=3, minute=0),
    }
}
```
```python
# producer en FastAPI (worker separado)
from tasks import evaluar_5d
@router.post("/api/v1/evaluations/submit")
async def submit(sid: str):
    session_id = evaluar_5d.delay(sid)   # devuelve de inmediato → factor 1.0 de XP (RF-IA-27)
    return {"status": "diferido", "job_id": session_id.id}
```

- **Beneficios** (2026): sigue siendo el **estándar de la industria**; Flower para monitoreo, retry con backoff, throughput p95 de ~48ms en I/O (benchmark 2026), multi-broker, y `Beat` como reemplazo de cron. Si ya usas Celery y no te causa dolor, **no hay argumento para migrar** (consenso 2026).

- **Por qué usarla aquí**: consistencia con el stack ya propuesto en tus ADR-003/014/Plan-02/03. Es la opción segura y con ecosistema completo (Stack Overflow, Flower, Sentry). **Advertencia** en tu caso: tus workers son mayoritariamente **I/O-bound** (llamadas LLM async) — Celery con `prefork` paga un pequeño peaje de sincronización (benchmark 2026: ~12-68 tasks/s vs ~190-250 de ARQ/streaq en I/O puro). Para tu volumen (120 alumnos, picos de ~500 entregas), Celery es más que suficiente y aporta madurez.

---

### 8.2. ARQ (Alternativa async-nativa — con advertencia 2026)

- **Para qué sirve**: cola asyncio-nativa sobre **Redis**; las tareas son coroutines, un solo worker maneja alta concurrencia I/O sin un thread por tarea.

- **Caso de uso**: si quisieras el máximo rendimiento I/O dentro de la instancia FastAPI (integración con el event loop).

- **Advertencia importante (2026)**: **ARQ está en modo maintenance-only** — sin features nuevas ni desarrollo activo. Múltiples fuentes (Dev.to, Medium) recomiendan **pensarlo dos veces para proyectos nuevos** por riesgo de largo plazo. Alternativas modernas async-nativas: **Taskiq** (DX + FastAPI, DI en workers, multi-broker) y **streaq** (basado en Redis streams, claim 5x más rápido que ARQ).

- **Por qué NO recomendarla aquí**: por riesgo de mantenimiento y porque tu stack ya depende de RabbitMQ (bus AMQP del tema 11) para la evaluación; Celery conecta nativo con ese broker. Si en el futuro priorizas async-puro, evaluar **Taskiq** antes que ARQ.

---

## 9. Categoría 7 — Sanitización de PII

### 9.1. Microsoft Presidio ⭐ (Recomendado)

- **Para qué sirve**: SDK open-source (MIT) de **detección + anonimización de datos personales** (PII). Separa **Analyzer** (encuentra spans con `entity_type` y `score`) de **Anonymizer** (aplica operadores: `replace`, `mask`, `redact`, `encrypt`). La pieza clave de tu **CAPA 2 (Sanitizador PII)**.

- **Caso de uso en tu proyecto**: antes de mandar el input a cualquier LLM cloud (**RF-IA-05** capa 2, mínimo privilegio / blast radius), anonimiza **DNI, mails, nombres** que el plan ya menciona. Alineado con GDPR/Res. 26 y privacidad por diseño (Art. 25) y con el estándar `session_id` UUID. Corre on-premise (los datos nunca salen del perímetro).

- **Cómo se integra**:
```python
from presidio_analyzer import AnalyzerEngine
from presidio_anonymizer import AnonymizerEngine, OperatorConfig

analyzer = AnalyzerEngine()
anonymizer = AnonymizerEngine()

# Reconocedor custom para DNI argentino / legajo UTN
from presidio_analyzer import Pattern, PatternRecognizer

dni = PatternRecognizer(
    supported_entity="DNI",
    patterns=[Pattern("DNI-7-8", r"\b\d{7,8}\b", 0.6)],
)
analyzer.registry.add_recognizer(dni)

async def sanitize(text: str) -> str:
    results = await asyncio.to_thread(analyzer.analyze, text, language="es")
    out = await asyncio.to_thread(
        anonymizer.anonymize, text, results,
        operators={"DEFAULT": OperatorConfig("replace", {"new_value": "<REDACTED>"})},
    )
    return out.text
```

- **Beneficios**:
  - **Reconocedores regex + NER (spaCy)** deterministas para structuras (emails, teléfonos, tarjetas).
  - **Custom recognizers** por cargador: DNIs, legajos, matrículas (tu caso argentino).
  - **Operadores reversibles** (`encrypt`) → permiten **proxy de privacidad reversibles** donde el LLM trabaja con placeholders y el usuario final ve datos reales (patrón LiteLLM + Presidio).
  - Corre 100% local → cumple "los datos nunca salen del perímetro" (`ADR-016` mínimo privilegio).

- **Por qué usarla**: es el estándar open-source de facto desde 2020, MIT, maduro y el colaborador natural de LiteLLM. **Nota**: los recognizers default están tuneados para inglés/USA; para español argentino necesitas el modelo de lenguaje `es` + few custom recognizers (décadas de minutos). Es **pseudonimización** (no anonimización total) — ayuda al GDPR Art. 25 pero no reemplaza la evaluación legal.

---

## 10. Categoría 8 — Resiliencia y rate-limiting

### 10.1. Tenacity ⭐ (Recomendado)

- **Para qué sirve**: librería de **retries** con políticas explícitas: backoff exponencial, jitter aleatorio, condiciones de retry, límite de intentos. Ya mencionada en tu pipeline para el **Trade-off anti Thundering Herd** ante `429`.

- **Caso de uso**: todas las llamadas a proveedores cloud: si la API devuelve 429/503/5xx, reintenta con backoff + **jitter** para no golpear al proveedor en "rebano" (su exacto caso del pico de 120 alumnos a las 09:00).

- **Cómo se integra**:
```python
from tenacity import retry, stop_after_attempt, wait_random_exponential, retry_if_exception_type

@retry(
    stop=stop_after_attempt(3),
    wait=wait_random_exponential(multiplier=0.5, max=8),  # backoff + jitter
    retry=retry_if_exception_type(ProviderRateLimit),
)
async def call_llm(role: str, prompt: str):
    async with semaphore:                       # ver 10.2
        return await litellm_call(role, prompt)

# Fallback en cascada si TODOS los proveedores fallan → factor neutro 1.0 (RF-IA-27)
def cascade(role, prompt):
    for provider in ROLES_CASCADE[role]:
        try: return call_llm(provider, prompt)
        except ProviderDown: continue
    return NEUTRAL_FACTOR_1_0
```

- **Beneficios**: control fino y declarativo; jitter evita thundering herd; combinable con semáforo y circuit breaker.

- **Por qué usarla**: resuelve directamente tu **RF-IA-27** (tolerancia a fallos) y el requisito de resiliencia ante picos; es la herramienta estándar.

---

### 10.2. `asyncio.Semaphore` (stdlib) ⭐

- **Para qué sirve**: limita la concurrencia de corutinas. Tu **RF-NFR-03** exige resistir 120 alumnos; el **`asyncio.Semaphore(25)`** limita a 25 conexiones concurrentes máximas al proveedor para no saturar cuota/costos.

- **Cómo se integra**: usa `async with semaphore:` alrededor de cada llamada (ya mostrado arriba). Es stdlib — sin dependencia.

### 10.3. redis — Token Bucket FinOps (RF-IA-22 / ADR-013) ⭐

- **Para qué sirve**: contabilidad **atómica** de cuotas en Redis (`INCR` + TTL). Previene ataques Denial of Wallet y fuerza el límite **50k tokens/alumno/día** (~100 consultas) con **HTTP 402**.

- **Cómo se integra**:
```python
import redis.asyncio as aioredis
redis = aioredis.from_url("redis://redis:6379/0")

async def check_finops(user_id: str, tokens: int) -> bool:
    key = f"finops:daily_tokens:{user_id}"
    used = await redis.incrby(key, tokens)
    await redis.expire(key, 86_400, nx=True)
    if used > 50_000:
        raise HTTPException(status_code=402, detail="Cuota diaria agotada")
    return True
```
(Elimina el `incrby` si el request ya se procesó — validar cuota **antes** de armar el prompt para ahorrar $ y latencia, como indica RF-IA-22.)

---

## 11. Categoría 9 — Observabilidad y LLMOps

### 11.1. Pydantic Logfire ⭐ (Recomendado — complemento de Pydantic AI)

- **Para qué sirve**: observabilidad del ecosistema Pydantic: **trazas de los agentes** (cada `agent.run()`), costos por token, latencia, y **evals**. 100% compatible con Pydantic AI e instrumenta FastAPI con una línea.

- **Caso de uso**: depura el Evaluador, trackea costos por rol (FinOps) y sirve de base para el **drift**. Al elegir Pydantic AI, Logfire es el observability nativo.

- **Cómo se integra**:
```python
import logfire
logfire.configure(token=LOG_FIRE_TOKEN, send_to_logfire="if-token-present")
logfire.instrument_fastapi(app)
logfire.instrument_pydantic_ai()   # trazas/costos de cada agente
```

- **Por qué usarla**: mínima fricción si adoptas Pydantic AI; trazas y cost tracking out-of-the-box.

### 11.2. OpenTelemetry + Prometheus (Recomendado para métricas de drift)

- **Para qué sirve**: métricas estandarizadas (OpenTelemetry spans) + scraping por **Prometheus** + dashboards **Grafana**. Tu **Plan-06/10** ya contempla `ASTBufferStreamFilter` con métricas `Prometheus` para calibrar el umbral PAR-11 y el dashboard ejecutivo de salud (`status: UP, mae_actual: 3.2, gasto_usd: 12.40`).

- **Cómo se integra**:
```python
from prometheus_client import Histogram, Counter, start_http_server

MAE = Histogram("llm_golden_mae", "MAE del Golden Set", buckets=[1, 2, 3, 4, 5, 6, 8, 10])
FUGAS = Counter("llm_ast_fugas", "Bloqueos por similitud >= 70%")

@router.get("/api/v1/admin/health/calibration")
async def health():
    return {"status": "UP", "mae_actual": MAE_actual, "gasto_usd": gasto}
```

- **Por qué usarla**: completa el **LLMOps** (RF-IA-30/32) y materializa el Plan-10. Es el estándar de observabilidad de infraestructura.

---

## 12. Decisión final: stack combinado recomendado

Arquitectura de capas propuesta para el **FastAPI Core Engine (:8082)**:

```
                    ┌──────────────────────────────────────────────────┐
   REST/SSE         │  FASTAPI CORE ENGINE (:8082)                     │
 ─────────────────▶ │                                                    │
                    │  ┌─ Capa Streaming ─────────────────────────────┐  │
   Tutor (SSE)      │  │  fastapi.sse / sse-starlette + Pydantic AI   │  │
   <800ms TTFT      │  │  + Buffer AST (tree-sitter)  [ADR-009/PAR-11]│  │
                    │  └──────────────────────────────────────────────┘  │
                    │  ┌─ Capa Seguridad (PIPELINE 5 CAPAS) ──────────┐  │
                    │  │ 1 Harmlessness: Guardrails AI DetectPromptInj│  │
                    │  │ 2 PII:            Presidio (custom DNI)      │  │
                    │  │ 3 XML:            delimitación manual        │  │
                    │  │ 4 Anti-Crescendo: Redis window (manual)      │  │
                    │  │ 5 AST Egress:     tree-sitter + Levenshtein  │  │
                    │  └──────────────────────────────────────────────┘  │
                    │  ┌─ Capa Orquestación LLM ──────────────────────┐  │
                    │  │ Pydantic AI (agentes tipados, T=0, seed=42)  │  │
                    │  │      └──▶ LiteLLM Proxy (:4000) ───▶ Gemini │  │
                    │  │           (failover, costos, routing)       /   │
                    │  │                        Claude / OpenAI       │  │
                    │  └──────────────────────────────────────────────┘  │
                    │  ┌─ Capa RAG (Fase 3) ──────────────────────────┐  │
                    │  │ pgvector + asyncpg + sentence-transformers   │  │
                    │  │ + cross-encoder re-rank (course_id partition)│  │
                    │  └──────────────────────────────────────────────┘  │
                    │  ┌─ Capa Workers / LLMOps ──────────────────────┐  │
                    │  │ Celery + Beat: Evaluador 5D, Drift MAE (03h) │  │
                    │  │ Redis: FinOps token-bucket, anti-crescendo   │  │
                    │  └──────────────────────────────────────────────┘  │
                    │  ┌─ Capa Resiliencia / Observabilidad ──────────┐  │
                    │  │ Tenacity (backoff+jitter) + Semaphore(25)    │  │
                    │  │ Logfire + OpenTelemetry + Prometheus         │  │
                    │  └──────────────────────────────────────────────┘  │
                    └────────────────────────────────────────────────────┘
                        RabbitMQ (AMQP) · PostgreSQL 16 + pgvector
```

**Prioridades de adopción** (recomendación por fases):

| Prioridad | Librería | Fase | Justificación |
|-----------|----------|------|---------------|
| 🔴 Alta | **Pydantic AI** | 1-2 | Reemplaza adaptadores manuales; tipado de notas (Evaluador 5D, RF-IA-13/15) |
| 🔴 Alta | **LiteLLM** | 1 | Agnosticismo + fallback + costos (RF-IA-11/26/27/35) |
| 🔴 Alta | **sse-starlette / fastapi.sse** | 2 | Transporte del Tutor (ADR-009) |
| 🔴 Alta | **tree-sitter-languages** | 2 | Buffer AST multilingüe (RF-IA-20/PAR-11, Plan-08) |
| 🟠 Media | **Presidio** | 2 | Capa PII (RF-IA-05, GDPR) |
| 🟠 Media | **Tenacity + Semaphore** | 2 | Resiliencia picos (RF-IA-27/NFR-03) |
| 🟠 Media | **pgvector + sentence-transformers** | 3 | RAG (RF-IA-08, Fase 3) |
| 🟠 Media | **Celery + Beat** | 3 | Evaluador async + drift MAE (RF-IA-27/32, ADR-003/014) |
| 🟡 Baja | **Guardrails AI** | 2-3 | Refuerza Capa 1/8 (opcional, complementa guards manuales) |
| 🟡 Baja | **Logfire / OpenTelemetry** | 3-4 | Observabilidad + dashboard (Plan-10) |

---

## 13. Mapa de trazabilidad librería → RF-IA → fase

| RF-IA | Requisito | Librería principal | Fase | Componente |
|-------|-----------|--------------------|------|-----------|
| RF-IA-01 | Asistencia socrática / streaming | fastapi.sse + Pydantic AI | 1-2 | Tutor SSE |
| RF-IA-04 | Prohibir soluciones | Pydantic AI (system prompt) + GST Buffer | 2 | Prompt Engine |
| RF-IA-05 | Filtro intención / off-topic | LiteLLM (modelo barato) + Guardrails AI | 1 | PorteroService |
| RF-IA-07 | Anti-jailbreak activo | Guardrails AI | 2 | Camada 1-3 |
| RF-IA-08 | RAG / conocimiento | pgvector + sentence-transformers | 3 | VectorStore |
| RF-IA-10 | Bloqueo silencioso + auditoría | Guardrails AI + Logfire | 2 | incidentes_ia |
| RF-IA-11 | Agnosticismo proveedor | LiteLLM | 1 | AI Gateway |
| RF-IA-12 | Separación Tutor/Evaluador | Pydantic AI (2 agentes) | 1-2 | Evaluador |
| RF-IA-13 | Rúbrica con anclas Few-Shot | Pydantic AI (result_type) | 1 | RubricaService |
| RF-IA-14 | Anti-manipulación evaluación | Pydantic AI (deps separadas) | 1 | Sanitización |
| RF-IA-15 | Pesos fijos 5D | Pydantic AI (Schema Score5D) | 1 | EvaluadorService |
| RF-IA-20 | Salvaguarda AST anti-fuga | tree-sitter + Levenshtein | 2 | AntiFugaService |
| RF-IA-22 | Cuotas FinOps | redis (token bucket) | 1 | FinOps |
| RF-IA-23 | Mapeo rol-modelo | LiteLLM (config) | 1 | LlmProperties |
| RF-IA-26 | Multimodelo pool | LiteLLM (router) | 1 | Cascada |
| RF-IA-27 | Tolerancia a fallos / diferido | Tenacity + Celery | 2-3 | DiferidoService |
| RF-IA-32 | Detección drift | Celery Beat + Prometheus MAE | 3 | DriftJob |
| RF-IA-34 | Bloqueo de cierre por pendientes | Celery (cola) | 3 | 409 lock |

---

## 14. Referencias 2026

- **Pydantic AI** — pydantic.dev/pydantic-ai · GitHub pydantic/pydantic-ai (V2, "harness-first", jun 2026) · betas de `pydantic-ai-litellm`.
- **LiteLLM** — docs.litellm.ai · "LiteLLM vs Pydantic AI: understanding the difference" (2026) · "Adding LiteLLM as model wrap" (issue #1496).
- **LangChain / LangGraph** — blog.jetbrains.com/pycharm "Best Python AI Frameworks in 2026" (11/06/2026) · speakeasy.com comparación 7 frameworks (03/2026) · "Pydantic AI vs LangChain 2026".
- **sse-starlette** — github.com/sysid/sse-starlette (v3.4.x) · FastAPI docs SSE (fastapi.sse, añadido en 0.135.0).
- **tree-sitter** — github.com/tree-sitter/py-tree-sitter · pypi tree-sitter-languages · tree-sitter-analyzer (v1.18) · tools de clone detection (biston, treepeat).
- **pgvector** — github.com/pgvector/pgvector-python · "Build a RAG API with FastAPI and PostgreSQL pgvector" (2026-08) · RAG production con cross-encoder (2026-03).
- **Presidio** — microsoft/presidio (2.2.x) · "Microsoft Presidio: The Open-Source Privacy Shield for AI" (2026-06) · "OpenAI Privacy Filter vs Presidio" (2026-04).
- **Celery / ARQ / Taskiq / streaq** — "Celery vs ARQ vs RQ 2026 benchmarks" (Medium, 2026-05) · "Celery Is Not Always the Answer" (bytay, 2026-05) · fastapi-patterns.com (ARQ maintenance-only) · "Background Jobs in Python 2026" (Dev.to, 2026-06).
- **Guardrails AI / NeMo / LLM Guard** — "Evaluating Guardrail Frameworks" (2026-02) · "Open Source LLM Guardrails: A 2026 Comparison" (LLM Armor, 2026-04) · "Guardrails vs NeMo" (genai.qa, 2026-06) · nota: **LLM Guard archivado 09/07/2026** (decryptiondigest).
- **Tenacity** — tenacity.readthedocs.io · **Prometheus/OpenTelemetry** — prometheus.io · opentelemetry.io · **Pydantic Logfire** — pydantic.dev/logfire.

---

> **Firma SPIKE**: Documento de investigación v1.0 — Septiembre 2026 · Tema 07 UTN FRC (Microservicio IA & Ciberseguridad LLM).
> Siguiente paso sugerido: PoC de **Pydantic AI + LiteLLM** para el Evaluador 5D determinista (validación de `Schema Score5D` contra el Golden Set) antes de integrar el resto de capas.
