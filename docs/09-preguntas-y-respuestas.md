# 09 — Preguntas y respuestas

> Registro vivo de las preguntas que fuimos haciendo y por qué se respondieron así.
>
> **Cada entrada tiene el caso a favor y el caso en contra.** El caso en contra no es relleno: es lo
> que te van a preguntar en la defensa del TP, y es lo que te permite darte cuenta cuando una
> decisión dejó de tener sentido. Una decisión de la que solo sabés los argumentos a favor es una
> decisión que no entendés.
>
> Formato: **Pregunta → Respuesta → Por qué sí → Por qué no (honesto) → Qué la cambiaría.**

---

## Índice de preguntas

| # | Pregunta | Respuesta corta |
|---|---|---|
| [Q-01](#q-01) | ¿Usar un orquestador de IA que divida las tareas? | **No** un router LLM; **sí** un gateway determinístico |
| [Q-02](#q-02) | ¿Una IA para cada función o una sola para todo? | **Una por función, detrás del mismo gateway** |
| [Q-03](#q-03) | ¿El asistente rápido y la corrección lenta ahorra plata? | **Sí, y es más importante de lo que parece** |
| [Q-04](#q-04) | ¿Qué modelos usar? ¿Los más baratos? | **Baratos en 4 funciones, no en el evaluador** |
| [Q-05](#q-05) | ¿Se puede gastar USD 15-20 en vez de 125? | **Sí. La palanca son los tokens, no el modelo** |
| [Q-06](#q-06) | ¿Se puede gastar muchísimo menos todavía? | **Sí, ~USD 3, con tres condiciones** |
| [Q-07](#q-07) | ¿Free tier primero y desborde a pago? | **Sí, salvo en el evaluador** |
| [Q-08](#q-08) | ¿Java Spring Boot o Python para el back? | ⚠️ **Desactualizada — ADR-005 la revirtió a Java** |
| [Q-09](#q-09) | ¿Modelo local? ¿Cuánto hardware? | **Solo el moderador. Nunca el evaluador** |
| [Q-10](#q-10) | ¿Cómo crecer en horarios pico? | **Con una cola. Casi no hace falta escalar** |
| [Q-11](#q-11) | ¿Cómo resolver prompt injection y fuga? | **Defensa en capas; la clave es no darle la solución al modelo** |
| [Q-12](#q-12) | ¿El documento "entrena" al modelo? | **No. Es RAG, no fine-tuning** |
| [Q-13](#q-13) | ¿Base vectorial dedicada? | **No. pgvector en el Postgres que ya existe** |
| [Q-14](#q-14) | ¿El tutor responde con streaming? | **No en desafíos prácticos. Lo prohíbe RF-IA-20** |
| [Q-15](#q-15) | ¿Microservicios con API Gateway y Service Discovery? | **Sí — la cátedra los impone.** Antes había recomendado lo contrario |
| [Q-16](#q-16) | ¿Una cola en el generador ahorra plata? | **No directamente.** Hacela por UX y robustez, no por costo |
| [Q-17](#q-17) | ¿"Docentes" son personas o la IA? | **Personas físicas. Nunca un modelo** |
| [Q-18](#q-18) | Si el docente arma el golden set, ¿qué construimos? | **La herramienta. Y va antes que el runner** |
| [Q-19](#q-19) | ¿Cómo se maneja que todo pase al mismo tiempo? | **Con prioridades en la cola, no solo con más workers** |
| [Q-20](#q-20) | ¿El RAG es la fuente de verdad de todo? | **De tres funciones. La cuarta —la nuestra— no lo usa** |
| [Q-21](#q-21) | ¿La salvaguarda va antes o después del modelo? | **Después. Revisa la respuesta del propio modelo** |
| [Q-22](#q-22) | ¿Alguna política nos impide guardar los chats académicos? | **No. El PRD los exige.** El riesgo está en otro lado |
| [Q-23](#q-23) | ¿No existe ya una librería para las malas palabras? | **Sí, y cubre 4 de 6 categorías.** Lo que no cubre no se arregla con más listas |
| [Q-24](#q-24) | El clasificador de moderación, ¿es agregar otra IA? | **No. El moderador quedó con menos IA que antes** |
| [Q-25](#q-25) | ¿Resolverlo sin IA para no gastar tokens? | **Sin IA sí, pero por latencia y resiliencia. La API es gratis** |
| [Q-26](#q-26) | Para el moderador, ¿pipeline o cadena? | **Pipeline, con un único corte de cadena antes de la red** |

---

<a name="q-01"></a>
## Q-01 — ¿Usar un orquestador de IA que divida las tareas de generador, corrector y asistente?

**Respuesta:** No un orquestador basado en LLM. Sí un **gateway determinístico**.

### Por qué NO un router basado en LLM

1. **La ruta ya se conoce.** Las cinco funciones se disparan desde pantallas o eventos distintos. Si
   el profesor apretó "generar parcial", no hay ambigüedad que resolver. Pagar un modelo para deducir
   algo que sabés con certeza es gasto puro.
2. **Latencia en el peor lugar.** El tutor necesita responder en menos de 2 segundos. Un router
   agrega una ida y vuelta completa *antes* de que el tutor empiece.
3. **Punto de falla único.** RF-IA-27 exige que ninguna dependencia externa bloquee al alumno. Un
   router está *antes* de todo: si se cae, se cae todo.
4. **Superficie de prompt injection.** Un router lee texto del alumno y toma una **decisión de
   control de flujo** con eso. Es el escenario clásico de ataque.

### El caso a favor del router (para ser justos)

Si algún día existiera una sola caja de chat donde el alumno escribe cualquier cosa —"ayudame con
esto", "generame un ejercicio", "cómo voy en el ranking"— y el sistema tuviera que inferir la
intención, **un router sería la respuesta correcta**. Ese producto no es este.

### Por qué SÍ el gateway determinístico

El PRD pide seis propiedades que no pertenecen a ninguna función: agnosticismo de proveedor
(RF-IA-11), asignación modelo→función por ADMIN (RF-IA-23/24), registro de toda interacción
(RF-IA-02), límites por usuario (RF-IA-22), trazabilidad de versiones (RF-IA-25), degradación
(RF-IA-27). **Son requerimientos del canal, no del caso de uso.** Centralizarlos es implementarlos
una vez en vez de cinco.

### Qué cambiaría la respuesta

Que aparezca una caja de chat de intención libre. Registrado como ADR-002.

📄 [06](06-operacion-e-ingenieria.md)

---

<a name="q-02"></a>
## Q-02 — ¿Una IA para cada tarea, o una sola para todo?

**Respuesta:** Una por función — pero **la separación es de configuración, no de código duplicado**.

### Por qué SÍ separar

- **RF-IA-24 lo exige.** La asignación modelo→función es configuración global de ADMIN.
- **Los perfiles son genuinamente distintos.** El tutor necesita 2 segundos de latencia; el evaluador
  puede tardar 10 minutos. El moderador procesa 38.000 mensajes; el generador, 900. Forzar un mismo
  modelo para los dos extremos significa pagar velocidad donde no hace falta o sufrir lentitud donde
  duele.
- **RF-IA-25 obliga.** El evaluador tiene una restricción propia —un solo modelo activo— que las
  otras funciones no tienen. Si fueran una sola IA, esa restricción sería inaplicable.

### Por qué NO cinco servicios separados

El error simétrico sería construir cinco microservicios. **Eso multiplica por cinco los guardarraíles,
el logging, los reintentos y el manejo de cuotas.** Son cinco *funciones* dentro de un servicio,
detrás de un gateway común.

### Qué cambiaría la respuesta

Nada previsible. La separación por función es un requerimiento explícito.

📄 [02](02-arquitectura-y-stack.md) §4

---

<a name="q-03"></a>
## Q-03 — El asistente tiene que ser rápido, pero la corrección y la generación pueden tardar. ¿Eso ahorra plata?

**Respuesta:** Sí. Y es una de las decisiones más rentables del proyecto, por una razón que no es
solo el ahorro.

### Por qué SÍ

Esa intuición no es una optimización oportunista: es **la línea que separa lo sincrónico de lo que va
por cola**. Y de ese único corte salen **tres beneficios distintos**:

| Beneficio | Detalle |
|---|---|
| **Costo** | Batch API es 50% menos en Anthropic, OpenAI y Google. Aplica justo donde usás los modelos caros |
| **Resiliencia gratis** | RF-IA-27 pide textualmente que el score quede "pendiente de cálculo diferido" si el evaluador no está. **Eso es una cola.** Si arrancás con cola, el requerimiento ya está implementado antes de leerlo |
| **Pico resuelto** | La cola absorbe "30 profesores generan parciales a la vez" sin escalar nada |

### El caso en contra

- **Complejidad.** Una cola persistente, workers, estados, reintentos y una dead letter queue son más
  piezas que un endpoint sincrónico.
- **UX más pobre.** El profesor no ve el parcial al instante: ve "generando...". Hay que construir
  polling y notificación.

**Por qué igual gana:** esa complejidad la ibas a pagar de todos modos para cumplir RF-IA-27. La
opción no es "cola vs simple", es "cola bien hecha desde el principio vs cola improvisada en marzo".

### Qué cambiaría la respuesta

Que el producto llegue a necesitar corrección instantánea visible al alumno. Registrado como ADR-003.

📄 [06](06-operacion-e-ingenieria.md)

---

<a name="q-04"></a>
## Q-04 — ¿Qué modelos usar? ¿Conviene el más barato?

**Respuesta:** Barato en cuatro funciones. **En el evaluador, no.**

### Por qué SÍ barato en tutor, moderador y generador

- **Moderador:** es clasificación pura sobre texto corto, con 6 categorías fijas (RF-CHT-10). No
  requiere razonamiento. Un modelo nano lo hace bien.
- **Generador:** **hay gate humano obligatorio.** El profesor revisa todo antes de publicar. La
  revisión humana es lo que te compra el derecho a usar un modelo barato acá.
- **Tutor:** es el 75% del volumen, así que ahí se decide el costo total.

### Por qué NO barato en el evaluador

Esta es la excepción y tiene un fundamento duro, no estético:

- El score modifica XP (PAR-05, ±20%).
- El XP determina promoción y regularidad.
- **RF-IA-31 y RF-IA-36 son bloqueantes:** un modelo que no pasa la calibración de PAR-14 (±5
  promedio, ±10 por dimensión) **no se puede activar**, y un curso que no calibra **no arranca**.
  Sin override, ni siquiera de ADMIN.

Si elegís un modelo barato y no pasa PAR-14, el resultado no es "evalúa un poco peor". Es **que el
curso no puede pasar de draft a activo**. El ahorro de USD 11 puede costar el arranque del
cuatrimestre.

### El caso en contra de gastar en el evaluador

Es honesto decirlo: **puede que un modelo barato sí pase la calibración.** No lo sabés hasta
medirlo. Si Flash-Lite pasa PAR-14 de forma reproducible sobre el golden set, usalo y ahorrate los
USD 11.

**La regla no es "gastá en el evaluador". Es "no bajes el evaluador sin medir contra el golden
set".** La calibración es el árbitro, no la opinión.

📄 [03](03-modelos-costos-y-contexto.md) §3

---

<a name="q-05"></a>
## Q-05 — ¿Se puede gastar USD 15-20 en vez de 125?

**Respuesta:** Sí, y la palanca principal no es la que uno esperaría.

### El hallazgo

> **Recortar el contexto del tutor de 6.000 a 3.000 tokens ahorra más que cambiar de modelo. Y no
> cuesta calidad.**

De los 154 M de tokens de entrada, **115 M son del tutor: el 75%**. Y de los 6.000 tokens por
llamada, casi la mitad era grasa:

| Recorte | Ahorro |
|---|---|
| Chunks del RAG: 8 → 3 | ~USD 28 |
| Historial completo → ventana de 4 mensajes | ~USD 20 |
| Prompt caching bien puesto | ~USD 18 |
| Respuestas del tutor: 400 → 250 tokens | ~USD 12 |
| Batch API en lo asincrónico | ~USD 21 |

**Las cinco son gratis.** Ninguna negocia calidad.

### Por qué recortar el RAG no empeora nada (al contrario)

Recuperar 8 chunks cuando 3 alcanzan **no da más información: da más ruido**. El modelo tiene que
decidir qué es relevante en vez de usar lo relevante. Sobre-recuperar es un error de calidad que
además cuesta plata.

### Y por qué las respuestas cortas son mejores

RF-IA-04 pide pistas, preguntas socráticas y documentación — **no ensayos**. Una pista concisa enseña
más que un muro de texto. Recortar de 400 a 250 tokens es simultáneamente más barato y mejor
pedagogía.

### El dato contraintuitivo

Una vez optimizado el contexto, **se invierte qué importa del precio**. Con 1.380 tokens
equivalentes de entrada y 250 de salida, **la salida pasa a ser el 60% del costo del tutor**. Por eso
Gemini Flash-Lite ($0,15 in / $1,25 out) deja de ser obviamente mejor que GPT-5 nano ($0,05 / $0,40).

**Al comparar modelos para el tutor, mirá la columna de output primero.**

📄 [03](03-modelos-costos-y-contexto.md) §4 y §8

---

<a name="q-06"></a>
## Q-06 — ¿Se puede gastar muchísimo menos todavía?

**Respuesta:** Sí, hasta ~USD 3. Hay tres caminos y **cada uno cobra en una moneda distinta**.

| Camino | Baja a | Qué pagás en vez de plata |
|---|---|---|
| **Supuestos realistas de volumen** | USD 15-17 | **Nada.** Ver abajo |
| **Free tier con desborde** | ~USD 3 | Riesgo legal de datos + se cae en el pico |
| **Local para tutor y moderador** | ~USD 3 | Hardware, operación, calidad del tutor |

### El camino gratis: los supuestos estaban inflados

Asumí 10 mensajes de tutor por desafío. **El PRD mismo duda de la adopción** — al descartar un KPI
candidato dice que la adopción del tutor es *"el indicador que diría si el componente más caro y más
riesgoso del producto realmente se usa"*. Los autores no dan por sentado que se use.

Con 5-6 mensajes promedio, **el tutor baja a la mitad**. Y **RF-IA-22 te obliga a poner un techo de
todos modos**: con 15 mensajes por desafío, el peor caso está acotado por diseño, no por la buena
voluntad de los alumnos.

### Por qué NO bajar más allá de ahí

Llegado a USD 15, cada dólar siguiente cuesta más de lo que vale:

| Idea | Ahorra | Cuesta |
|---|---|---|
| Evaluar solo una muestra de desafíos | ~USD 5 | Desarma el mecanismo académico central (RF-IA-15) |
| Evaluador en el modelo más barato | USD 11 | Riesgo de que el curso no arranque (RF-IA-36) |
| Sacar el moderador | USD 0,30 | Viola RF-CHT-09 |
| Contexto del tutor por debajo de 2.500 tokens | centavos | El tutor deja de ver el código del alumno |

📄 [03](03-modelos-costos-y-contexto.md) §4b

---

<a name="q-07"></a>
## Q-07 — ¿Usar el free tier y pasar a pago cuando se agota la cuota?

**Respuesta:** Sí, y funciona mejor de lo que parece. **Salvo en el evaluador.**

### Por qué SÍ

**321 llamadas por día contra un techo de 1.500: 21% de utilización.** El free tier tiene 4,7x de
margen sobre el volumen. Lo único que lo rompe son los picos concentrados.

Y ahí está la clave:

> **Un pico solo es un problema si alguien está esperando.**

- **Funciones asincrónicas** (evaluador, generador, corrector): si se acaba la cuota, la cola
  **espera**. No hay desborde, no hay costo, no hay cambio de modelo. Son 47 llamadas por día entre
  las tres.
- **Funciones sincrónicas** (tutor, moderador): el alumno está esperando, así que ahí sí desbordás a
  pago — y **solo pagás el desborde**.

### Por qué NO en el evaluador

**RF-IA-25:** *"un único modelo activo a la vez (no admite pool ni enrutamiento entre modelos)"*.
Una cascada por cuota **es** enrutamiento. Si el evaluador desbordara, dos alumnos del mismo curso
quedarían evaluados por modelos distintos — que es exactamente lo que la restricción existe para
impedir.

**Pero no hace falta:** son 21 llamadas por día en una cola. Se estrangula a 15 RPM y listo. **Un
solo modelo, sin desborde, RF-IA-25 respetado.** La estrategia y la restricción no chocan.

### El caso en contra (y es serio)

**El problema del free tier no es la cuota: es la política de datos.** Los free tiers habitualmente
permiten al proveedor usar lo enviado para mejorar sus modelos. Acá enviás código de alumnos,
transcripciones completas y PII.

Choca con **RF-NFR-09** (declarar en T&C qué proveedores reciben el material) y **RSK-01 / Ley
25.326**. **Es una decisión legal, no técnica.** Si la respuesta es que no, la estrategia entera
desaparece.

Y dos advertencias más:
- **El free tier no es un contrato.** La cuota puede cambiar sin aviso.
- **Se cae justo el día del examen.** 15 RPM contra ~225 necesarios en el pico.

### Veredicto por contexto

| Contexto | Free tier |
|---|---|
| Demo y desarrollo con datos sintéticos | ✅ **Sin objeción. Usalo** |
| Producción con alumnos reales | ❓ Depende del análisis de datos (P-06) |

📄 [03](03-modelos-costos-y-contexto.md) §4c

---

<a name="q-08"></a>
## Q-08 — ¿Java Spring Boot o Python para el backend?

**Respuesta:** Para el backend de negocio, **no es decisión del equipo de IA**. Para el
`ai-service`, **Python**.

> ⚠️ **Esta respuesta quedó desactualizada y se conserva para que se vea el cambio.** **ADR-005 la
> revirtió: el `ms-evaluacion-llm` va en Java Spring Boot** —figura entre las siete decisiones
> revisadas de [08](08-decisiones-y-pendientes.md), fila 3—, el `pom.xml` existe y el esqueleto
> compila. El argumento de `tree-sitter` de acá abajo perdió peso cuando se asumió que los desafíos
> son en Java, donde JavaParser es mejor.
>
> Lo que ADR-005 **sí** deja abierto es que un **componente interno** pueda ser Python cuando haga
> falta —embeddings locales, o el modelo local de moderación de ADR-012—. Componente interno, **no
> microservicio**. La misma desactualización aparece en C-1 de
> [14](14-sincronizacion-guia-didactica.md).

### Por qué el `ai-service` en Python

El argumento decisivo no es "Python tiene más librerías". Es concreto:

> **RF-IA-20 exige comparar ASTs entre el código de la IA y la solución esperada. Y los desafíos no
> van a ser todos en Java.** `tree-sitter` tiene bindings de Python maduros y cubre decenas de
> lenguajes; JavaParser solo entiende Java.

Más: ingesta de PDF/OCR/PPT, embeddings locales en tres líneas, e iteración rápida sobre prompts —
que vas a hacer cincuenta veces.

### Por qué el núcleo en Java (si te preguntan)

- La economía de gamificación necesita ACID: XP, monedas, vidas, compras.
- RF-NFR-02 pide 2FA obligatorio para los tres roles: Spring Security lo tiene resuelto.
- Percentiles P90, cascada de desempate, curva de niveles: muchas reglas entrelazadas donde el
  tipado fuerte paga.

### El caso en contra del híbrido

Es real: dos lenguajes, dos builds, un contrato que mantener, debugging distribuido. **Y requiere al
menos una persona cómoda en Python.**

Contrapeso: si el equipo de IA es el que hace el `ai-service`, **el híbrido no le agrega complejidad
a nadie más** — para el otro equipo es una caja negra con 6 endpoints.

### Qué cambiaría la respuesta

Que nadie en el equipo de IA esté cómodo en Python. Ahí: todo Java, con la IA como módulo de
frontera dura.

📄 [02](02-arquitectura-y-stack.md), [01](01-problema-y-alcance.md) §2

---

<a name="q-09"></a>
## Q-09 — ¿Conviene un modelo local? ¿Cuánto hardware haría falta?

**Respuesta:** Para el moderador sí. Para el tutor, como plan B. **Para el evaluador, nunca.**

### Por qué NO por costo

| Opción | Costo |
|---|---|
| API, escenario recomendado | **USD 21 / cuatrimestre** |
| RTX 4090 24GB | USD 1.800 - 2.200 |
| RTX 3090 24GB usada | USD 800 - 1.000 |

**El repago serían décadas**, sin contar electricidad ni horas de operación. Y en ese plazo la GPU es
chatarra. Por costo, local nunca cierra a esta escala.

### Por qué SÍ, por otras razones (y son buenas)

1. **Soberanía de datos.** RF-NFR-09 obliga a declarar qué proveedores reciben el código del alumno,
   y RSK-01 deja abierto el cumplimiento de Ley 25.326. **Un modelo local no envía nada a nadie.**
   Ese argumento vale más que el costo y es el más defendible frente a un jurado.
2. **Independencia de cuota.** RSK-06 es riesgo Alto: agotamiento de cuota durante una instancia
   evaluada. Un modelo local no tiene rate limit.
3. **Valor académico.** Es un TP: levantar un modelo con vLLM y medirlo tiene valor propio.

### Qué corre en qué

| Función | ¿Local? | Modelo | VRAM |
|---|---|---|---|
| Moderador | ✅ **Sí** | Qwen3 4B / Phi-4-mini | 8 GB |
| Tutor | ⚠️ Plan B | Qwen3 30B-A3B | 24 GB |
| Evaluador | ❌ **Nunca** | — | — |

### Por qué el evaluador nunca

**PAR-14 lo mata.** ±5 de desviación promedio y ±10 por dimensión contra el golden set es una
tolerancia estrecha. Un modelo abierto de 30B no la sostiene de forma estable — y sin pasarla, el
curso no arranca.

**Condición de revisión:** si un modelo abierto pasa la calibración de forma reproducible, cambia la
respuesta. Se mide, no se opina.

📄 [03](03-modelos-costos-y-contexto.md) §7

---

<a name="q-10"></a>
## Q-10 — ¿Cómo hacer crecer el servicio en horarios pico? ¿Y si muchos profes generan parciales a la vez?

**Respuesta:** Con una cola. Y para la parte web, **casi no hace falta escalar nada**.

### Por qué casi no hace falta

**RF-NFR-03: 120 usuarios, 120 sesiones concurrentes.** Eso es poquísimo tráfico web. Un contenedor
de Spring Boot y uno de Postgres lo aguantan sin despeinarse.

Y el PRD dice dónde está el problema real, textualmente:

> *"El escenario crítico no es el tráfico web sino las invocaciones concurrentes de IA y las cuotas
> del proveedor."*

**Toda la ingeniería de escalado va del lado de la IA. Nada del lado web.** Nada de Kubernetes, ni
autoescalado, ni CDN, ni sharding.

### Qué pasa con 30 profesores generando a la vez

1. Los 30 pedidos se encolan en **milisegundos**. Nadie espera un HTTP.
2. 4 workers drenan la cola; los 30 parciales salen en ~10-15 minutos.
3. La cuota del proveedor **no se satura de golpe**: la cola es el amortiguador.

### Sin cola, el mismo escenario

- 30 conexiones HTTP abiertas de 2 minutos cada una.
- Timeouts de nginx y del navegador (típicamente 60 s).
- 450 llamadas al LLM en el mismo segundo → **429 Too Many Requests**.
- Reintentos automáticos → más 429 → **efecto avalancha**.

**La cola no es una optimización: es lo que evita que este escenario tumbe el servicio.**

### El escalado que sí existe

`ai-service` y `ai-worker` son **la misma imagen con distinto comando**. Escalar el pico es
`docker compose up --scale ai-worker=6`. No hay código nuevo.

### Lo que sí es un límite real

**La cuota del proveedor.** En el pico hacen falta ~225 RPM. El free tier da 15. Por eso: **cuenta
paga para cualquier curso real** — no por el gasto, por el límite.

📄 [06](06-operacion-e-ingenieria.md)

---

<a name="q-11"></a>
## Q-11 — ¿Cómo resolver prompt injection y fuga de información?

**Respuesta:** Defensa en capas. Pero la capa que más rinde es de diseño, no de seguridad.

### El principio

> **Todo texto que viene de un usuario es DATO. Nunca es una instrucción.**

El error se comete al escribir el prompt: `f"Sos un tutor. El alumno pregunta: {mensaje}"`. En esa
línea el mensaje del alumno y tu instrucción quedaron en el mismo plano — el modelo no tiene forma de
distinguirlos. **La solución no es un prompt más firme: es estructural.**

### Las dos capas que más rinden

**1. Minimización de contexto — la defensa definitiva contra RF-IA-04**

> **El tutor no puede filtrar lo que no tiene.**

El tutor ve enunciado, código del alumno, teoría y reglas. **Nunca** la solución de referencia ni los
tests ocultos. Ningún jailbreak extrae algo que el modelo no vio.

*¿Y RF-IA-20, que pide comparar contra la solución?* Esa comparación la hace **el guardarraíl de
salida, en tu código, fuera del contexto del modelo.** La solución vive en el gateway, nunca en un
prompt.

**2. El perímetro temático lo hace cumplir el retrieval, no el prompt**

RF-IA-06 pide que el modelo opere solo dentro del perímetro del curso. El reflejo es escribirlo en
el system prompt — **y eso se sortea hablando**.

La versión robusta: si el retrieval (filtrado por `curso_id` **en el servidor**) no devuelve nada
sobre el piso de similitud, no hay contexto que darle al modelo, y el tutor no responde. **La
decisión la tomó tu código, no el modelo.** No hay prompt que lo convenza.

### La regla que hace que funcione

> `curso_id` y `usuario_id` se derivan **de la sesión**, jamás de un parámetro del cliente. Un
> `curso_id` que viene del navegador es un `curso_id` que el alumno puede cambiar.

### El truco de latencia

El clasificador de intención agregaría 200-400 ms al camino crítico si corriera antes del tutor.
Solución: **corre en paralelo**, y se retiene la respuesta hasta que conteste. Como el guardarraíl de
salida ya obliga a retener, **el clasificador sale gratis**. Las dos restricciones se pagan con la
misma moneda.

📄 [05](05-seguridad.md)

---

<a name="q-12"></a>
## Q-12 — Se sube un documento y "de ahí se entrena". ¿Eso es entrenar un modelo?

**Respuesta:** No. Es **RAG**, no fine-tuning. Y la diferencia importa.

| | Fine-tuning | RAG |
|---|---|---|
| Qué hace | Modifica los pesos del modelo | Busca fragmentos y los mete en el prompt |
| Costo | Alto: horas de GPU, dataset | Casi cero |
| Actualizar el material | Reentrenar todo | Reindexar el documento |
| ¿Cita la fuente? | No, y alucina con confianza | **Sí, con página y sección** |

### Por qué NO fine-tuning acá

1. **RF-IA-29 lo prohíbe en la práctica.** Exige que la rúbrica sea *"un artefacto declarativo
   versionado y único, no un prompt ajustado a un modelo particular"*, y **prohíbe mantener variantes
   de criterio por modelo**. Un modelo fine-tuneado es exactamente eso.
2. **Perdés la trazabilidad**, que acá es un requisito real: el profesor tiene que ver de qué parte
   del apunte salió cada pregunta.
3. **Ninguna plataforma de este tipo lo hace.** Todas hacen RAG.

### Cómo explicárselo al equipo en 3 segundos

Mostrale la pantalla de revisión con **la pregunta generada y el fragmento del apunte al lado**.
Cuando alguien ve eso, entiende RAG y deja de preguntar si el modelo "se entrenó".

📄 [04](04-funciones-de-ia.md) §1

---

<a name="q-13"></a>
## Q-13 — ¿Usar una base vectorial dedicada (Pinecone, Qdrant, Weaviate)?

**Respuesta:** No. `pgvector` en el Postgres que ya existe.

### Por qué NO la dedicada

A 120 usuarios y unos pocos cursos, el corpus son **miles de chunks, no millones**. Una base dedicada
sería: un contenedor más, un backup más, una configuración más y una falla más — **sin ningún
beneficio medible a esta escala**.

### Por qué SÍ pgvector

- Ya vas a tener Postgres.
- Podés hacer joins entre chunks y metadata del curso en una sola consulta.
- Un solo backup, un solo modelo de permisos.

### Qué cambiaría la respuesta

Corpus por encima de ~500.000 chunks, o latencia de búsqueda por encima de 100 ms. Registrado como
ADR-004 con esas condiciones explícitas.

📄 [02](02-arquitectura-y-stack.md) §5

---

<a name="q-14"></a>
## Q-14 — ¿El tutor responde con streaming, como ChatGPT?

**Respuesta:** No en desafíos prácticos. **Lo prohíbe RF-IA-20**, aunque el requerimiento no hable de
streaming.

### El razonamiento

RF-IA-20 exige comparar la respuesta del tutor contra la solución esperada y, si supera el 70% de
similitud, **bloquearla y regenerarla antes de mostrarla al alumno**.

> **No podés bloquear una respuesta que el alumno ya está leyendo.** Para comparar, la necesitás
> completa; si la tenés completa, no la mostraste.

Es una restricción de UX que sale directo del requerimiento, y **casi nadie la ve hasta que ya
construyó el streaming**.

### Qué hacer en cambio

| Opción | Cuándo |
|---|---|
| **Sin streaming**, indicador de "pensando" | **MVP.** Lo más simple y seguro |
| Streaming con retención selectiva: prosa sí, bloques de código retenidos | Fase 2 |
| Streaming pleno | Solo en desafíos de **riesgo bajo** (hackathon, code review), donde no hay una única solución contra la cual comparar |

### Cómo compensarlo

**Con velocidad de modelo.** Es una de las razones por las que el tutor va en un modelo rápido y con
prompt caching: si no podés mostrar de a poco, mostrá rápido.

📄 [05](05-seguridad.md) §3, ADR-009

---

<a name="q-15"></a>
## Q-15 — ¿Microservicios con API Gateway y Service Discovery?

**Respuesta:** Sí. **Y esta respuesta cambió.**

### Lo que había recomendado, y por qué

Monolito modular + un servicio de IA aparte. El argumento más fuerte: **una base por microservicio
rompe la atomicidad de la economía**. Otorgar XP + monedas + vidas + insignia + nivel es un solo
acto; con bases separadas se convierte en transacción distribuida con sagas y compensaciones.

### Por qué cambió

Apareció la propuesta de arquitectura de la cátedra, con una sección titulada **"Reglas no
negociables"**: API Gateway como única puerta, registro dinámico, sin comunicación directa entre
servicios, base exclusiva por servicio, bus de eventos para lo asincrónico.

**No es una recomendación con la que se pueda discutir: es el marco del TP.**

### Qué sobrevive del análisis anterior

- **El argumento de la atomicidad sigue siendo cierto**, y es exactamente lo que hay que decir en la
  defensa: *"elegimos no partir la gamificación porque otorgar XP, monedas y vidas es atómico"*.
  Demuestra más criterio que dibujar seis cajitas.
- La cátedra misma llega a la misma conclusión: *"si el 04 o el 05 pudieran otorgar XP por su cuenta,
  las reglas de la economía quedarían escritas en tres lugares"*.
- **Python para nuestro servicio se mantiene**: la cátedra dice expresamente que *"cada equipo decide
  el diseño interno de su servicio"*.

### Qué quedó anulado

**ADR-004**: pgvector ya no va en un esquema del Postgres compartido, va en **nuestra base propia y
exclusiva**.

📄 [02](02-arquitectura-y-stack.md), [02](02-arquitectura-y-stack.md)

---

<a name="q-16"></a>
## Q-16 — ¿Poner el generador en una cola hace que consuma menos de algún modelo?

**Respuesta:** No directamente. La cola no reduce ni un token.

### Lo que la cola sí habilita

| Beneficio | Cuánto |
|---|---|
| **Batch API** | −50%. Es el ahorro real, y requiere tolerar minutos — que es justo lo que la cola da |
| **Estrangular a la cuota** | Podés quedarte adentro del free tier |
| **Reintentos sanos** | En vez de una avalancha de 429 que empeora la caída |

### Pero seamos honestos con el número

El generador son ~60 parciales por cuatrimestre: **USD 0,70**. La cola te ahorra 35 centavos.
**No la hagas por plata.**

### Por qué hacerla igual

1. **Generar 15 preguntas tarda minutos**, y una petición HTTP de 3 minutos se muere en el timeout de
   nginx o del navegador. Es un problema de funcionamiento, no de costo.
2. **Cuando 30 profesores generen a la vez**, la cola es lo que evita que se caiga todo.

### Y por qué NO hacerla ahora

**Para la demo local no hagas cola.** Que bloquee y tarde. Agregala cuando un timeout te moleste de
verdad. Es complejidad que no compra nada en una demo de un parcial por vez.

📄 [10](10-entregables-y-plan.md)

---

<a name="q-17"></a>
## Q-17 — Cuando decís "docentes", ¿te referís a la IA o a una persona física?

**Respuesta:** **Personas físicas. Reales. Nunca un modelo.**

RF-IA-30 es explícito: *"puntaje por dimensión **acordado por docentes, nunca generado por un
modelo**"*.

### Por qué no puede ser un modelo

El golden set es **la vara con la que medís al modelo**. Si un modelo hace la vara, estás midiendo al
modelo contra sí mismo — es corregir tu propio examen con tus propias respuestas.

Toda la calibración existe para responder una pregunta: **¿este modelo puntúa como puntuaría un
humano?** Sin un humano del otro lado, la pregunta no tiene sentido.

### Lo que importa no es el título

| Sí | No |
|---|---|
| Personas reales | Un modelo generando puntajes |
| **Al menos dos**, por separado | Una sola (no podés medir el acuerdo) |
| Que puntúen **antes** de ver el puntaje del modelo | Que "ajusten" al número del modelo |

### En un TP, ¿quién los hace?

Puede que no haya profesores reales. **La versión reducida es válida y defendible:** 10
transcripciones, 2 integrantes del equipo actuando como docentes, ~4 horas.

Es el mismo mecanismo a escala chica. Lo que demostrás es: dos humanos puntuaron por separado,
midieron su acuerdo, el modelo puntuó a ciegas, se calculó la desviación contra PAR-14.

**Lo único sin atajo:** los puntajes no los genera un modelo, ni siquiera en la versión reducida.

📄 [04](04-funciones-de-ia.md) §4b

---

<a name="q-18"></a>
## Q-18 — Si el docente arma el golden set, ¿qué construimos nosotros?

**Respuesta:** El docente produce el contenido. **Nosotros producimos el lugar donde ponerlo.**

| Quién | Qué |
|---|---|
| Docente | Lee las transcripciones, puntúa, discute los desacuerdos |
| Nosotros | Modelo de datos, pantalla de carga y puntuación, runner de calibración, versionado, historial |

### La consecuencia de orden que reordena el plan

> **La herramienta tiene que existir antes de que el trabajo docente pueda empezar.**

Es fácil postergarla porque parece "una pantalla de administración más". Pero es lo que **destraba el
ítem de plazo más largo del proyecto**: si está lista tarde, los docentes empiezan tarde.

**Para P4 eso significa: primero la herramienta, después el runner.** Al revés de lo intuitivo — el
runner no le sirve a nadie hasta que haya algo que correr.

### Los dos bloqueos que hacen que la calibración mida algo

- **El docente B no ve los puntajes de A.**
- **El docente no ve el puntaje del modelo** antes de puntuar.

No son permisos, son diseño. Sin ellos, la persona se ancla al número que ya vio y la calibración da
bien siempre — verificando nada.

📄 [04](04-funciones-de-ia.md) §4c

---

<a name="q-19"></a>
## Q-19 — En la plataforma va a haber gente haciendo desafíos, otros parciales, otros evaluaciones, y hay que corregir todo. ¿Cómo se maneja?

**Respuesta:** Con **prioridades en la cola**, no con más workers.

### El problema

Si la cola es FIFO, la recalibración mensual que arrancó a las 10:00 hace esperar a 30 correcciones
de alumnos que están mirando la pantalla.

### El orden correcto

| Prioridad | Trabajo | Por qué |
|---|---|---|
| **1** | Corrección de una entrega recién hecha | **El alumno está esperando.** Es la única cola con alguien mirando |
| **2** | Evaluación de uso de IA | RF-IA-27 permite diferirla |
| **3** | Evaluaciones de cursos por cerrar | RF-IA-34 bloquea el cierre. Se prioriza por **fecha de cierre**, no por antigüedad |
| **4** | Generación de parciales | El profesor sabe que tarda |
| **5** | Recalibración | Programarla fuera de horario pico |

### Y hace falta reservar capacidad, no solo priorizar

Con prioridades solas, una avalancha de correcciones deja las generaciones sin correr por horas.
**Reservá un worker** para las prioridades bajas: así una tormenta de correcciones no deja a un
profesor esperando dos horas.

### El peor momento

El cierre de un parcial: 120 entregas casi simultáneas → ~240 trabajos → con 4 workers, **~20 minutos
de drenado**.

Es aceptable **si el alumno lo sabe**. La UI dice *"tu entrega fue registrada, la corrección estará
en unos minutos"* — que es exactamente lo que RF-IA-27 ya prevé al separar la aceptación de la
entrega del cálculo del score.

📄 [06](06-operacion-e-ingenieria.md) §4b

---

<a name="q-20"></a>
## Q-20 — ¿El RAG es el contexto en el que se basa todo: responder, corregir y armar parciales?

**Respuesta:** Sí para esas tres. **No para la nuestra.**

### Misma fuente, tres formas distintas de consultarla

| Función | Cómo recupera | Si lo hacés mal |
|---|---|---|
| **Tutor** | Búsqueda semántica por la consulta del alumno | Traés 8 chunks y el modelo se pierde en el ruido |
| **Generar parcial** | **Por cobertura**: repartís sobre los temas, eligiendo chunks *distintos* entre sí | Traés los más similares → **15 preguntas del mismo tema** |
| **Corregir** | **Por id**: el chunk exacto del que salió la pregunta | Búsqueda libre → la guía la respuesta del alumno y le traés material que respalda su error |

La del medio es la más contraintuitiva: para generar un parcial querés **máxima disimilitud**, no
máxima similitud.

### La excepción, y es justo el Tema 07

El **evaluador de uso de IA** no mira el material del curso. No le importa si el alumno tiene razón
sobre punteros: mide **cómo usó al tutor**. Su fuente es la rúbrica.

**De los 6 ítems del Tema 07 estricto, ninguno necesita el RAG.** Por eso el alcance (A-1) es la
pregunta que más cambia el trabajo: decide si construís esa pieza o ni la tocás.

📄 [05](05-seguridad.md)

---

<a name="q-21"></a>
## Q-21 — La salvaguarda anti-fuga, ¿es una capa antes de que llegue al modelo?

**Respuesta:** Va **después**. Y la intuición de "es una capa más" es correcta — hay capas de los dos
lados, la salvaguarda es la de salida.

### Por qué no puede ir antes

> **Lo que la salvaguarda revisa es la respuesta del propio modelo. Antes de llamarlo, esa respuesta
> todavía no existe.**

RF-IA-20: *"**antes de enviar cualquier respuesta del tutor**... verificación de similitud entre el
código propuesto por la IA y el código real esperado... **la respuesta se bloquea y se regenera antes
de mostrarse al alumno**"*.

### Las dos capas

```
alumno → [GUARDARRAÍL ENTRADA] → LLM → [GUARDARRAÍL SALIDA] → alumno
          injection, jailbreak,          ¿se parece a la solución?
          fuera de tema                  → bloquear y regenerar
          RF-IA-05/06/07                 RF-IA-20 ← la salvaguarda
```

### Las dos consecuencias

1. **Obliga a bufferear.** Sin streaming token a token en desafíos prácticos: no podés bloquear algo
   que el alumno ya está leyendo.
2. **Nos pone en el camino de respuesta del tutor.** Es el argumento del alcance: la cátedra nos
   asignó la salvaguarda, y la salvaguarda vive entre el modelo y el alumno.

📄 [05](05-seguridad.md) §4

---

<a name="q-22"></a>
## Q-22 — ¿Hay alguna política de privacidad que nos impida guardar los chats académicos para valorarlos y mostrarlos en una revisión?

**Respuesta:** No. **El PRD obliga a guardarlos y obliga a mostrarlos.**

### Los requerimientos

| | |
|---|---|
| **RF-IA-02** | *"Toda interacción alumno-IA **se registra**"* — es obligación |
| **RF-IA-03** | *"La interacción con la IA **es parte de la evaluación académica**"* |
| **RF-IA-18** | En apelación, *"el profesor **ve la transcripción completa**"* |
| **RF-NFR-01** | No hay borrado físico de producción académica |

El fundamento es textualmente el caso de uso preguntado: *"no se elimina porque constituye **elemento
de juicio sobre su trabajo**... transcripciones con el tutor de IA, scores, apelaciones"*.

Incluso el "derecho al olvido" quedó **fuera de alcance como decisión consciente**.

### Las cinco condiciones

1. Los T&C lo declaran expresamente y en lenguaje llano (RF-NFR-09)
2. **5 años, no indefinido** — "indefinido" sería difícil de defender ante un reclamo
3. La purga nunca es automática; ante el silencio, se conserva
4. La anonimización tiene que poder alcanzar a las transcripciones
5. Al alumno no se le muestra el prompt interno ni las técnicas de gaming (RF-IA-16)

### 🔴 Pero el riesgo está en otro lado

**Guardar es la parte segura. Lo expuesto es enviarlo a un tercero.**

| | Riesgo |
|---|---|
| Guardarlo en nuestra base | 🟢 Bajo — previsto y fundamentado |
| Mostrarlo al docente y al alumno | 🟢 Ninguno — es requisito |
| Enviarlo a un proveedor de LLM | 🟡 RSK-01 lo deja abierto |
| Enviarlo a un **free tier que puede entrenar con ello** | 🔴 **C-2, sin resolver** |

📄 [07](07-datos-y-terminos.md) §6b

---

<a name="q-23"></a>

## Q-23 — ¿No existe ya una librería que resuelva las malas palabras, en español o inglés?

**Respuesta corta: sí, y más de una — pero cubre menos de lo que parece.** El filtrado de lenguaje
ofensivo es un problema resuelto desde mucho antes de que existieran los LLM. Hay algoritmos
publicados, librerías Java maduras y listas de términos con licencia permisiva. El detalle completo de
cada herramienta está en [04](04-funciones-de-ia.md) §2.3.1.

### Por qué SÍ usarlas

- **Cubren cuatro de las seis categorías** de RF-CHT-10 sin tocar un modelo.
- **Latencia:** un match en memoria es < 1 ms contra los 300 ms de presupuesto.
- **Es la red del fail-open** (P-02): si el proveedor externo se cae, esto sigue corriendo. Sin esta
  capa, fail-open significa *sin ninguna moderación*.
- **Menos datos salen del sistema.**

### Por qué NO alcanzan solas (y es lo importante de esta respuesta)

**Tres huecos, y ninguno se tapa con más listas:**

1. **Acoso y amenaza sin léxico explícito.** *"Sé dónde vivís"* no tiene una sola mala palabra. Es
   comprensión de la frase, no búsqueda de un término.
2. **Dos categorías son propias de este producto** y no existen en ninguna herramienta del mundo:
   *compartir soluciones de desafíos* (integridad académica) y *eludir el solo-texto*. La primera la
   resolvemos por **forma** y no por contenido —lo obliga ADR-008—; la segunda con entropía.
3. **Ojo con cuál lista.** Una lista **binaria** —una palabra por línea, sin nivel— no puede expresar
   que *boludo* entre compañeros es afecto y *pelotudo* es agravio, y aplicada tal cual **bloquearía
   media cursada**. LDNOOBW es binaria y peninsular; **el diccionario de ModernMT no**, y esa fue la
   diferencia que decidió cuál usar (ver más abajo).

### El problema técnico que nadie menciona hasta que explota

Se llama [**problema de Scunthorpe**](https://en.wikipedia.org/wiki/Scunthorpe_problem): en 1996 AOL
bloqueó a los habitantes de ese pueblo inglés porque el nombre contiene una mala palabra como
subcadena. En castellano pasa igual: **"cálculo"** contiene *culo*, **"putativo"** contiene *puta*,
**"conchabar"** contiene *concha*.

Y tironea contra el problema opuesto —la evasión: `p3l0tud0`, `p-e-l-o-t-u-d-o`—, porque cuanto más
agresivo el matching, más falsos positivos. **Ese equilibrio es el trabajo real, no conseguir la
lista.** Ver [04](04-funciones-de-ia.md) §2.3.2.

### ✅ La verificación que cambió parte de esta respuesta

Se revisó el repositorio de `com.modernmt.text:profanity-filter` —el README no documenta nada, así que
hubo que mirar los archivos— y **trae más de lo que esperábamos**: `dictionary.es` (429 entradas) y
`dictionary.en` (467), **cada término con un score de 0 a 1 en vez de un booleano**, y con el registro
rioplatense razonablemente calibrado: `pelotudo` 0,74, `boludo` **0,42**, `boludazo` **0,00**,
`concha` **0,06**, `coger` **0,06**. Soporta además frases enteras, no solo palabras sueltas.

Ese `concha` en 0,06 es, literalmente, **la defensa contra el problema de Scunthorpe ya resuelta por
otro**. La conclusión práctica: el "nivel por término" que dábamos por trabajo propio en buena medida
ya existe, y **LDNOOBW pasó de primera opción a plan B**.

Lo que queda nuestro es bastante menos: sumar los términos ausentes (*forro*, *chabón*, *sorete*),
**fijar dónde cortan baja→media y media→alta** —eso no lo da ninguna librería y es decisión de
producto, Parte C de [08](08-decisiones-y-pendientes.md)— y validar la calibración contra los 100
mensajes etiquetados.

### Qué cambiaría la respuesta

Que la validación contra nuestro propio corpus muestre que los scores, que vienen de otro dominio, no
se trasladan a un chat de cursada. En ese caso el diccionario sigue sirviendo como punto de partida,
pero hay que recalibrarlo con datos nuestros.

📄 [04](04-funciones-de-ia.md) §2.3, ADR-012

---

<a name="q-24"></a>

## Q-24 — El clasificador de moderación, ¿es agregar otra IA al proyecto?

**Respuesta corta: no. Al revés — el moderador quedó con menos IA que antes.**

Es la pregunta correcta para un proyecto que ya tiene cinco funciones de IA. Tres cortes:

| | |
|---|---|
| **¿Otra función de IA?** | **No.** RF-IA-23 asigna el modelo *por función*, no globalmente, y el moderador ya tenía el suyo. Cambiar cuál usa no crea una sexta función |
| **¿Otro proveedor?** | **No.** Es un endpoint más de OpenAI: misma API key, mismo adapter del gateway M1 que ADR-001 ya exige. Ni un contenedor ni un secreto de más |
| **¿Otro LLM?** | **No, y esto es lo importante.** Es un **clasificador**: entra texto, salen etiquetas con score |

### Qué elimina, que es el verdadero argumento

El diseño anterior era *pre-filtro + GPT-5 nano con prompt propio*. El nuevo es *capa clásica +
clasificador*. Lo que desapareció:

| Antes estaba planificado | Ahora |
|---|---|
| `prompts/plantillas/moderador.v1.txt` | No existe — no hay prompt que escribir |
| El pendiente E-05 | Cerrado |
| `temperature: 0` + `seed` (A-3 de [14](14-sincronizacion-guia-didactica.md)) | No aplica: un clasificador ya es determinístico |
| Superficie de prompt injection sobre el moderador | Cero: no interpreta instrucciones |

Y como *compartir soluciones* se detecta por **forma**, **el moderador quedó sin ningún LLM
generativo adentro.**

### Por qué NO — el caso en contra, honesto

- **Sigue siendo una dependencia externa.** Menos IA no es cero red: los mensajes del residuo salen
  igual del sistema.
- **Sigue habiendo un proveedor que puede cambiar su modelo debajo nuestro** sin avisar, y la
  clasificación puede moverse. Es el mismo riesgo que RF-IA-11 mitiga con el nombre del modelo en
  tabla editable, no en el código.
- **Un clasificador es una caja más cerrada que un prompt.** Con un prompt podés ajustar el criterio
  escribiendo; con un clasificador solo podés mover el umbral. Menos superficie de ataque, pero
  también menos control.

### Qué cambiaría la respuesta

Que necesitemos criterios de moderación específicos del producto que el clasificador no expone. Ahí
volvería un LLM con prompt — y con él, todo lo que la tabla de arriba eliminó.

📄 ADR-012, [04](04-funciones-de-ia.md) §2.3.5

---

<a name="q-25"></a>

## Q-25 — ¿Conviene resolverlo sin IA para no gastar tokens?

**Respuesta corta: conviene resolverlo sin IA, pero el ahorro de tokens no es la razón — y creerlo
lleva a decidir mal en otros lados.**

### El dato que corrige la premisa

**La Moderation API es gratuita.** No consume tokens del presupuesto. Si el argumento para empujar
trabajo a la capa clásica fuera el costo, **no habría argumento**: en esta función el costo por
consulta es USD 0 de las dos maneras.

### Las tres razones que sí valen

1. **Latencia.** El presupuesto es < 300 ms ([02](02-arquitectura-y-stack.md)) y un roundtrip HTTP se
   lo come casi entero. Un match en memoria es < 1 ms. **En el chat esto se nota; el usuario está
   esperando.**
2. **Resiliencia.** La capa clásica es la red del fail-open de P-02. Sin ella, que el proveedor se
   caiga significa *sin ninguna moderación*.
3. **Datos.** Cuantos menos mensajes de alumnos viajen a un tercero, mejor
   ([07](07-datos-y-terminos.md)).

### Por qué NO llevarlo al extremo

**Resolver todo sin IA no se puede, y el intento tiene un costo.** Cada categoría que se fuerza al
lado determinista con reglas cada vez más finas produce más falsos positivos, y **el falso positivo
del moderador se lo come un alumno** que ve su mensaje bloqueado sin saber por qué (RF-CHT-12 no le
explica el motivo, justamente para no enseñarle a evadir el filtro). Una lista agresiva es peor que
una llamada de red.

El corte está donde la técnica clásica deja de tener información suficiente: acoso y amenaza sin
léxico. Ahí no es que la regla sea difícil — **es que no existe**.

### Entonces, ¿en algún momento hay que meterle IA?

**No, y con lo diseñado alcanza para cumplir RF-CHT-09 a RF-CHT-14.** No hay fase 2 pendiente.

Los pedidos que van a aparecer —*"se escapan malas palabras nuevas"*, *"hay que soportar otro
idioma"*, *"están evadiendo con `p3l0tud0`"*, *"hay spam"*— **ya están resueltos** y no necesitan un
modelo. El único hueco serio es el **acoso acumulativo**, y ahí lo importante es que **no se arregla
cambiando de modelo sino cambiando el contrato**: como `moderar(mensaje)` ve un mensaje por vez, el
LLM más caro del mundo tampoco lo detectaría. La lista completa de escenarios —los cuatro que sí lo
justificarían y los seis que no— está en [04](04-funciones-de-ia.md) §2.10.

### Qué cambiaría la respuesta

Que la Moderation API deje de ser gratuita, o que su free tier no alcance. En ese caso el costo entra
al análisis y hay que revisar ADR-012 — pero la conclusión probablemente no cambie, porque las tres
razones de arriba siguen valiendo igual.

📄 ADR-012, [03](03-modelos-costos-y-contexto.md), [04](04-funciones-de-ia.md) §2.0 y §2.10

---

<a name="q-26"></a>

## Q-26 — Para el moderador, ¿pipeline o cadena de responsabilidad?

**Respuesta corta: pipeline como estructura principal, con un único corte de cadena antes de la
llamada de red.**

Es la decisión estructural del moderador y se presta a confusión porque los dos patrones se parecen:
en los dos hay "etapas en orden". La diferencia real es una sola:

- **Pipeline** → **corren todas** las etapas, cada una **transforma** el dato.
- **Cadena** → **corta** en la primera que **decide**. No corren todas.

### Por qué NO todo pipeline

Sería el código más simple: sin condiciones, sin orden semántico. Pero significa **llamar al
clasificador para todos los mensajes**, y eso rompe dos cosas a la vez: el presupuesto de 300 ms y el
free tier de 5.000 pedidos diarios. Descartada por latencia, no por elegancia.

### Por qué NO todo cadena

Es la lectura intuitiva de "filtros en orden" —y es lo que decía la primera versión de
[04](04-funciones-de-ia.md)—, pero **contradice el contrato que ya escribimos**: `categorias` es un
**array**, porque un mensaje puede ser spam **y** ofensivo. Cortando en el primer detector que
dispara, la segunda categoría se pierde y el profesor ve un incidente incompleto.

Y encima no compra nada: los detectores clásicos cuestan **microsegundos**, así que saltearlos no
ahorra tiempo medible.

### Por qué SÍ el híbrido

Por una asimetría de costo de ~1000×:

| Etapa | Costo |
|---|---|
| Normalizar + todos los detectores clásicos | **< 1 ms** |
| La llamada al clasificador | **200 a 500 ms** |

Ese salto —y solo ese— es lo que justifica una cadena. Todo lo demás es pipeline.

### 🟢 La regla, para reusarla en el resto del proyecto

> **Usá cadena solo donde saltear un eslabón ahorre algo medible. Si todos los eslabones cuestan
> parecido, es un pipeline.**

Y el corolario, que evita el error caro: **en una cadena el orden es semántico.** Si alguien mueve el
clasificador al principio *"para que sea más preciso"*, todos los mensajes pasan a pagar la red y el
diseño se convierte en el que descartamos, sin que nadie lo note. Por eso conviene tener **un solo**
eslabón de cadena y no cinco.

### Qué cambiaría la respuesta

Que algún detector clásico deje de costar microsegundos — por ejemplo, si la detección de integridad
académica pasara a parsear el AST del bloque de código pegado. Ahí ese detector se volvería caro y
merecería su propio eslabón de cadena.

📄 [04](04-funciones-de-ia.md) §2.3.4b, [11](11-glosario-y-metadata.md)

---

## Cómo seguir usando este documento

Cada vez que se tome una decisión nueva o se responda una pregunta, se agrega acá con el mismo
formato. **Lo importante es no saltearse el "por qué no"** — es lo que te va a permitir darte cuenta
cuando una decisión deje de tener sentido, y lo que te van a preguntar en la defensa.

Las decisiones formales están en [08](08-decisiones-y-pendientes.md) como ADR; este documento es el
razonamiento en lenguaje llano detrás de ellas.
