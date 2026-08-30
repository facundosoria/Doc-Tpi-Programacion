# 14 — Sincronización con la guía didáctica

> Comparación con `Plan anti jailbreak/guia_didactica_ia/` (7 capítulos + `estructura_sistema_ia.md`),
> del **2026-08-30**.
>
> **Veredicto: los dos sets son complementarios, no competidores.** La guía tiene profundidad de
> implementación que nosotros no tenemos; nosotros tenemos decisiones, alcance y economía que la guía
> no cubre. **Pero hay seis conflictos reales que hay que resolver antes de escribir código.**

## 1. Qué es cada set

| | **Nuestra documentación** | **La guía didáctica** |
|---|---|---|
| Foco | **Decisiones y fundamento**: qué construir, por qué, qué falta definir | **Implementación**: cómo se escribe, con código real |
| Formato | Análisis con recomendación y contraargumento | Manual técnico con Python, DDL y diagramas |
| Fortaleza | Alcance, costos, riesgos, coordinación entre equipos | Detalle técnico, hiperparámetros, esquema de base |
| Debilidad | **Poco código concreto** | **Asume decisiones que no están tomadas** |

**Leerlas juntas es mejor que leer cualquiera de las dos sola.**

---

## 2. 🔴 Los seis conflictos a resolver

### C-1 — Python FastAPI vs Java Spring Boot

| | |
|---|---|
| **La guía** | Todo el stack es Python 3.12 + FastAPI + Uvicorn/uvloop + Celery. Hay código real |
| **Nosotros** | Java Spring Boot, porque Programación IV es materia de Java y la integración con Spring Cloud pesa más ([02](02-arquitectura-y-stack.md), Parte 2) |
| **Impacto** | 🔴 **Total.** Si se resuelve por Java, el código de la guía no se usa tal cual — pero **los algoritmos y el diseño sí** |

> **La buena noticia:** la guía documenta **decisiones de diseño**, no solo sintaxis. El buffer
> interceptor, la máquina de estados, la comparación de AST, el esquema de base y los hiperparámetros
> **se traducen a Java sin perder nada**. Lo que se pierde es poder copiar y pegar.

**A resolver:** confirmar con la cátedra si el servicio puede ir en Python. **Es la pregunta que
decide, y hasta que se responda los dos sets divergen en todo lo demás.**

---

### C-2 — ¿Streaming sí o no? — 🏆 Acá la guía nos gana

| | |
|---|---|
| **Nosotros** | Dijimos **sin streaming** en desafíos prácticos: RF-IA-20 obliga a bufferear, y "streaming con retención selectiva" quedó como mejora de Fase 2 |
| **La guía** | Diseña ese mecanismo en detalle: **Buffer Interceptor** con máquina de estados `OUTSIDE_CODE` / `INSIDE_CODE`. La prosa fluye en vivo; **al detectar la apertura de un bloque de código congela la emisión y acumula en RAM**; al cerrarlo, parsea el AST, compara contra la solución y decide si emitir o descartar |

> ### ✅ Adoptamos el enfoque de la guía
>
> **Es mejor que nuestra recomendación y resuelve el problema que habíamos declarado bloqueante.** El
> alumno ve la explicación aparecer en vivo y solo espera en los bloques de código — que es
> exactamente donde la espera se justifica.
>
> Y hay un beneficio pedagógico que ninguno de los dos había notado: **el alumno percibe que el
> sistema "piensa" antes de darle código**, lo que refuerza el mensaje de RF-IA-04.

**A ajustar en nuestros docs:** [05 · Seguridad](05-seguridad.md) y el ADR-009 de
[08](08-decisiones-y-pendientes.md) dicen "sin streaming en el MVP". **Hay que revisarlo.**

---

### C-3 — Los modelos recomendados están desactualizados

| | |
|---|---|
| **La guía** | Gemini 3.1 Flash/Pro · GPT-5 · "Claude 4 Sonnet" · "GPT-4.5" |
| **Nosotros** | Gemini **3.5** Flash-Lite · Claude **Haiku 4.5** · GPT-5 nano, con precios verificados al 2026-08-30 |
| **Impacto** | 🟡 Medio. "Claude 4 Sonnet" y "GPT-4.5" ya no son los vigentes |

**Resolución: prevalece nuestro catálogo** ([03](03-modelos-costos-y-contexto.md)), que tiene precios
verificados, costo por consulta y **la advertencia de que dos modelos baratos se apagan dentro de la
vida del proyecto**.

Pero con un matiz importante: **la guía elige gama alta para el evaluador (Gemini Pro / GPT-5) y
nosotros gama media (Haiku 4.5).** **Ninguno de los dos decide esto: lo decide el golden set.**

---

### C-4 — El backend central: Node/Go/Django vs Java

| | |
|---|---|
| **La guía** | *"Backend Central (Node.js / Go / Django)"* |
| **La cátedra** | Programación IV — Back End, con Spring Cloud implícito en su vocabulario |
| **Impacto** | 🟡 Medio, y no es nuestra decisión — pero **cambia cómo nos integramos** |

---

### C-5 — Falta la infraestructura que la cátedra impone

**La guía no menciona Service Discovery ni el contrato de eventos del Tema 11.** Su diagrama pone el
Core API como gateway y la comunicación es directa.

| Regla no negociable de la cátedra | ¿Aparece en la guía? |
|---|---|
| API Gateway como única puerta | 🟡 Parcial — usa el Core API |
| **Registro dinámico en Service Discovery** | ❌ **No** |
| Sin comunicación directa entre microservicios | ❌ No |
| Bus de eventos con contrato compartido (Tema 11) | 🟡 Usa RabbitMQ, pero sin el contrato común |

**Resolución: prevalece la cátedra.** Son reglas declaradas no negociables
([02](02-arquitectura-y-stack.md), Parte 1).

---

### C-6 — Redis: obligatorio vs quizás innecesario

| | |
|---|---|
| **La guía** | Redis 7.2 Cluster para sesiones, semáforos, cuotas y caché |
| **Nosotros** | A 120 usuarios probablemente **no haga falta**: Postgres con `FOR UPDATE SKIP LOCKED` + caché en memoria alcanza ([06](06-operacion-e-ingenieria.md), Parte 6) |
| **Impacto** | 🟢 Bajo — es reversible en cualquier momento |

**A resolver midiendo**, no discutiendo. Si los contadores se ponen calientes, entra Redis.

---

## 3. ✅ Lo que la guía tiene y nosotros no — hay que incorporarlo

**Esto es lo más valioso del ejercicio.** Nueve cosas que nos faltaban:

| # | Qué | Por qué importa | Dónde va |
|---|---|---|---|
| **A-1** | **Buffer Interceptor en streaming** con máquina de estados | Resuelve lo que declaramos bloqueante | [05](05-seguridad.md) |
| **A-2** | 🔴 **Hiperparámetros por rol**: temperatura, top-p, top-k, **seed** | **`temperature: 0` y `seed` fijo en el evaluador atacan directamente el problema de reproducibilidad** que marcamos como debilidad de los LLM para poner notas | [13](13-rubrica-y-prompts.md) |
| **A-3** | **Timeouts concretos por rol** (moderador 1 s, tutor 45 s, evaluador 120 s) | Nosotros solo dimos objetivos de latencia, no timeouts | [06](06-operacion-e-ingenieria.md) |
| **A-4** | **DDL completo de PostgreSQL** con `UUID`, `JSONB` e índices GIN | Nosotros describimos las tablas, ellos las escribieron | [12](12-almacenamiento-e-ingesta.md) |
| **A-5** | 🔴 **Triggers `BEFORE UPDATE OR DELETE`** para forzar inmutabilidad de notas | **Es mejor que nuestra regla "append-only por convención"**: lo hace cumplir la base, no la disciplina del equipo | [12](12-almacenamiento-e-ingesta.md) |
| **A-6** | **`tokens_usage_ledger`** — tabla de consumo por usuario y día | Nosotros dijimos "contadores en Redis"; una tabla es auditable y sobrevive reinicios | [12](12-almacenamiento-e-ingesta.md) |
| **A-7** | **PII scrubber con regex** antes de mandar al proveedor | Nosotros dijimos "no mandes PII"; ellos lo implementan como capa | [05](05-seguridad.md) |
| **A-8** | **Arquitectura interna Onion / Clean** | Nosotros dimos 8 módulos sin estructura de capas adentro | [02](02-arquitectura-y-stack.md) |
| **A-9** | **Calibración nocturna programada** (Celery Beat) | Nosotros dijimos "mensual" (PAR-15); ellos la automatizan | [04](04-funciones-de-ia.md) |

> ### 🏆 A-2 y A-5 son los dos mejores aportes de la guía
>
> **A-2 — `temperature: 0` + `seed` fijo:** dijimos que "un LLM puede darle 65 y 80 a la misma
> transcripción" y que por eso convenía calcular dimensiones con código. **Temperatura cero y semilla
> fija reducen mucho esa varianza sin renunciar al juicio semántico.** No reemplaza al scoring
> híbrido, pero lo complementa — y es una línea de configuración.
>
> **A-5 — triggers de inmutabilidad:** nuestra regla de que un override agrega en vez de pisar era
> **una convención que alguien puede romper sin darse cuenta**. Un trigger la vuelve imposible. Para
> algo que tiene que sostenerse en una apelación ocho meses después, la diferencia es enorme.

---

## 4. Lo que nosotros tenemos y la guía no

Para que no se pierda al integrar:

| # | Qué | Por qué importa |
|---|---|---|
| B-1 | **Análisis de alcance del Tema 07** | La guía asume que hacemos las 5 funciones. **El reparto oficial de la cátedra es más angosto**, y el tutor y el RAG no están asignados a nadie |
| B-2 | **Fundamento Java vs Python** | La guía asume Python sin justificarlo contra el entorno |
| B-3 | **Costo por consulta y palancas** | La guía menciona FinOps pero no cuánto cuesta cada cosa |
| B-4 | **Free tier con desborde** | USD 0 para la demo |
| B-5 | **Scoring híbrido determinístico** | Que el 45-60% de la rúbrica se puede calcular sin LLM |
| B-6 | **Esquema de eventos de IDE** | Ediciones y ejecuciones **antes del primer mensaje** — la evidencia más limpia de autonomía |
| B-7 | **Glosario y las 7 colisiones** | *"Evaluación"* significa cosas distintas en tres temas |
| B-8 | **Proceso de calibración humana** | Dos docentes por separado, y **donde difieren se arregla el ancla, no el puntaje** |
| B-9 | **Las anclas concretas de las 5 dimensiones** | La guía tiene la fórmula; nosotros el contenido |
| B-10 | **Retrieval por cobertura** | Que para generar un parcial querés máxima **disimilitud**, no similitud |
| B-11 | **Plan de trabajo para 6 personas** | |
| B-12 | **T&C y análisis de retención** | |
| B-13 | **Ingesta de PDF con imágenes** | El caso traicionero: texto + diagramas |
| B-14 | **Inventario de contenido borrador** | |

---

## 5. Dónde coinciden — y eso es buena señal

Las dos documentaciones llegaron por separado a lo mismo:

| Coincidencia |
|---|
| Un microservicio de IA aislado, no cinco |
| Los 5 roles de RF-IA-23 con modelo asignado por función |
| PostgreSQL + pgvector, sin base vectorial dedicada |
| El evaluador corre **asincrónico y desacoplado**, disparado por evento |
| Comparación de AST contra la solución, umbral 70% (PAR-11) |
| Delimitadores explícitos para el texto del alumno (`<untrusted_student_input>` ≈ nuestros bloques marcados) |
| El evaluador con **un solo modelo activo** (RF-IA-25) |
| Salida estructurada validada contra schema |
| Overrides auditados en tabla aparte |
| 120 concurrentes como objetivo (RF-NFR-03) |

**Que dos análisis independientes coincidan en diez decisiones estructurales es la mejor validación
que se puede pedir.**

---

## 6. Qué hacer para sincronizar

### Inmediato

| # | Acción | Quién |
|---|---|---|
| 1 | 🔴 **Resolver Python vs Java** — es la que bloquea todo lo demás | Equipo + cátedra |
| 2 | **Adoptar el Buffer Interceptor** y revisar el ADR-009 | P5 |
| 3 | **Adoptar `temperature: 0` + `seed` fijo** en evaluador y moderador | P3 |
| 4 | **Adoptar los triggers de inmutabilidad** | P5 |
| 5 | **Incorporar los timeouts por rol** | P1 |
| 6 | **Avisarle al autor de la guía** los conflictos C-4 y C-5: le falta la infraestructura que la cátedra impone | Quien corresponda |

### Cómo conviven los dos sets

**Recomendación: no fusionarlos. Referenciarlos.**

| Set | Rol |
|---|---|
| **Nuestra documentación** | **El "qué" y el "por qué"**: alcance, decisiones, economía, riesgos, coordinación, qué falta definir |
| **La guía didáctica** | **El "cómo"**: algoritmos, esquema de base, hiperparámetros, estructura de capas |

Fusionarlos produciría un documento enorme y borraría la distinción entre *decisión abierta* y
*implementación propuesta* — que es justamente lo que hay que mantener claro.

**Lo que sí hay que hacer: que cada uno diga que el otro existe**, y que los seis conflictos queden
resueltos en un solo lugar. Este documento es ese lugar.

---

## 7. Advertencia sobre el código de la guía

**Si se resuelve por Java, el código Python de la guía no se usa — pero no lo descarten.**

Lo que sobrevive a la traducción:

| Sobrevive | No sobrevive |
|---|---|
| La máquina de estados del buffer | La sintaxis de `AsyncGenerator` |
| El algoritmo de comparación de AST | `ast.parse` de Python → `JavaParser` |
| El esquema de base y los triggers | Nada: **el DDL es idéntico** |
| Los hiperparámetros | Nada: son parámetros de la API |
| Los timeouts y la estructura de capas | La sintaxis |

> **El DDL de PostgreSQL es 100% reutilizable sin importar el lenguaje.** Es probablemente el aporte
> más directamente aprovechable de toda la guía.
