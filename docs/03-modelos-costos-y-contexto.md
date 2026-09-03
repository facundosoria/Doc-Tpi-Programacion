# 03 — Modelos, costos y contexto

> 🔢 **Los supuestos de volumen son estimaciones** (10 mensajes por desafío, 50% de prácticos) y
> **la asignación de modelos es una recomendación: la calibración es la que decide**. Ver
> [08](08-decisiones-y-pendientes.md), Parte C, ítems E-19 a E-26.

> Qué modelo para cada función, cuánto cuesta cada consulta, cuánto contexto meter y qué puede ser
> gratis. Precios verificados el **2026-08-30**; son una foto, no una constante. Fuentes al final.
>
> *Consolida los antiguos 02 (modelos y costos), 21 (catálogo), 22 (el mínimo que funciona) y 23
> (cuánto contexto).*

## 1. Conclusión y método

**El rango real es USD 5 a 22 por cuatrimestre** para 120 alumnos. La incógnita que lo define es una
sola: **si el modelo barato pasa la calibración del evaluador o hay que subir uno.**

El método no es elegir el modelo más barato:

> **Definí qué significa "funciona" para cada función, y probá de abajo hacia arriba hasta que pase.**

Y antes de tocar un solo modelo, aplicá las palancas gratis de §5 — **valen más que cualquier cambio
de modelo y varias además mejoran la calidad.**

### La tabla de decisión

| Función | Modelo | Costo por consulta | Por qué |
|---|---|---|---|
| **Tutor** | Gemini 3.5 Flash-Lite | USD 0,00052 | Rápido y sigue instrucciones negativas razonablemente bien |
| **Moderador** | Capa clásica + `omni-moderation-latest` | **USD 0** | No es un LLM: listas y heurísticas resuelven 4 de 6 categorías, y el clasificador —que es gratuito— cubre el resto. Ver ADR-012 |
| **Evaluador** | Claude Haiku 4.5 + Batch | USD 0,006 | Consistencia de criterio. **Acá no se ahorra** |
| **Generador** | Gemini 3.5 Flash-Lite + Batch | USD 0,00083 | Hay revisión humana obligatoria |
| **Corrector** | Claude Haiku 4.5 + Batch | USD 0,002 | Es una nota, sin gate de calibración |

### El supuesto que más mueve este número: cuántos tokens pesa un prompt

Todo el rango de arriba sale de contar el texto del alumno. **Pero el prompt del alumno nunca se
envía solo.** El informe de gestión de modelos —[`presentaciones/informe-gestion-modelos.html`](../presentaciones/informe-gestion-modelos.html)—
desarma el payload real de una consulta al tutor y le encuentra cuatro capas:

| Capa | Tokens |
|---|---|
| Consulta cruda del alumno + su código | ~350 |
| System prompt pedagógico y directivas anti-fuga (RF-IA-04) | +450 |
| Delimitadores XML y guardarraíles anti-jailbreak (`<untrusted_student_input>`) | +150 |
| Contexto del desafío, rúbrica 5D y RAG | +600 |
| **Total de entrada por llamada** | **~1.550** |

Son **4,4 veces** el texto crudo. Con esa hipótesis y el mismo volumen —5 consultas de tutor por
alumno por semana, 120 alumnos—, el informe ubica el escenario realista en **USD 62 a 118 al año**,
contra los **USD 5 a 22 por cuatrimestre** de acá arriba.

**Las dos cuentas no se contradicen: parten de payloads distintos.** Y el informe agrega el dato que
salva la diferencia: el sobrecosto neto de la seguridad no es del 400 % sino del **20 al 25 %**,
porque el 80 % de ese payload es idéntico entre alumnos de la misma cohorte y entra por **prompt
caching** con 75-90 % de descuento, y porque el filtro local de la capa 1 descarta los ataques
burdos sin llamar a la API.

> **Qué falta para cerrarlo:** medir el payload real cuando el servicio corra. Si son 1.550 tokens y
> no 350, el rango de esta sección queda corto y hay que rehacer el ADR-010 con el número medido en
> vez del estimado. Está anotado en [08](08-decisiones-y-pendientes.md), en el propio ADR-010.

## 2. Catálogo de modelos

### Precios (USD por millón de tokens)

| Modelo | Input | Output | Contexto | Nota |
|---|---|---|---|---|
| **GPT-5 nano** | 0,05 | 0,40 | — | El más barato del mercado |
| **Gemini 3.5 Flash-Lite** | 0,15 | 1,25 | 1M | El caballo de batalla. ⚠️ Dos fuentes externas discrepan: `0,15 / 1,25` contra `0,30 / 2,50`. Confirmar en la consola de Google antes de fijarlo |
| **DeepSeek V4-Flash** | 0,22 | 0,66 | — | Off-peak. Cache hit: 0,007 |
| **Gemini 3.1 Flash-Lite** | 0,25 | 1,50 | 1M | |
| **DeepSeek V4-Pro** | 0,66 | 1,98 | — | Off-peak |
| **Claude Haiku 4.5** | 1,00 | 5,00 | 200K | Recomendado para el evaluador |
| **GPT-5** | 1,25 | 10,00 | — | |
| **Claude Sonnet 5** | 2,00 | 10,00 | 1M | El techo razonable |
| Claude Opus 5 | 5,00 | 25,00 | 1M | No hace falta acá |
| Claude Fable 5 | 10,00 | 50,00 | 1M | No hace falta acá |

### ⛔ Los dos que NO adoptar

| Modelo | Problema |
|---|---|
| **Gemini 2.5 Flash-Lite** (0,10 / 0,40) | **Se retira el 16-oct-2026** |
| **GPT-5 mini** (0,25 / 2,00) | **Se apaga el 11-dic-2026** |

Son los más baratos de sus familias y por eso tentadores. **Ambos se apagan dentro de la vida de este
proyecto.** Es la mejor prueba de por qué el nombre del modelo va en una tabla editable por ADMIN y
no en el código (RF-IA-11).

### Fichas

**GPT-5 nano** — precio imbatible, muy rápido, excelente para clasificación. **Flojo siguiendo
instrucciones negativas complejas.** Era el candidato del moderador hasta ADR-012; para el tutor hay
que probarlo antes.

**`omni-moderation-latest`** — el clasificador de moderación de OpenAI. **No es un LLM:** recibe texto
y devuelve 13 categorías con score, sin prompt. **Es gratuito** (250 req/min, 5.000 req/día en el free
tier) y su rendimiento en español supera al del inglés del modelo anterior. Sus categorías mapean casi
uno a uno con RF-CHT-10, salvo las dos propias de este producto —integridad académica y elusión del
solo-texto—, que resuelve la capa clásica.

**Gemini 3.5 Flash-Lite** — free tier real (~1.500 req/día sin vencimiento), muy rápido, contexto de
1M, multimodal para PDF escaneado. **Su salida es cara en proporción** (1,25 contra 0,15 de entrada):
por eso importa tanto que las respuestas del tutor sean cortas.

**Claude Haiku 4.5** — muy bueno con restricciones negativas, que es literalmente RF-IA-04 y RF-IA-19.
Consistente entre corridas, que para juzgar importa más que la inteligencia bruta. Sin free tier.

**Claude Sonnet 5** — mejor consistencia de la gama. Usalo **solo si Haiku 4.5 no pasa PAR-14**. No lo
adoptes por las dudas: medí primero.

**DeepSeek** — precio agresivo y cache hit casi gratis, pero precios peak/off-peak variables (subieron
50-1100% en agosto 2026) y **proveedor fuera de Argentina y la UE**, lo que cambia el análisis de Ley
25.326 (RSK-01). Plan C.

**Gama alta** — ninguna función de este proyecto la justifica. Puntuar contra una rúbrica de 5
dimensiones no es un problema difícil de razonamiento: es de **consistencia**, y eso se compra con
rúbrica bien escrita y anclas, no con un modelo más caro.

### Modelos locales

| Modelo | VRAM | Para qué |
|---|---|---|
| Qwen3 4B / Phi-4-mini | 8 GB | **Moderador. Viable de verdad** |
| Qwen3 30B-A3B (MoE) | 24 GB | Tutor como plan B |
| Qwen3 32B Q4 | 24 GB | Generador, lento pero funcional |

**Nunca para el evaluador:** PAR-14 exige ±5 de desviación promedio, y un modelo abierto de 30B no
sostiene esa tolerancia de forma estable. Sin pasarla, el curso no arranca.

**Por costo, local nunca cierra:** la API cuesta ~USD 20 el cuatrimestre y una RTX 4090 son USD 2.000.
El repago son décadas. **Por soberanía de datos sí puede cerrar**, y ese es el argumento honesto y el
más defendible frente a un jurado: RSK-01 deja abierto el cumplimiento de la Ley 25.326, y un modelo
local no envía nada a nadie.

## 3. Costo por consulta

Más útil que el precio por millón, porque es lo que realmente pasa.

### Una consulta al tutor
*3.000 tokens de entrada (60% cacheado → 1.380 equivalentes), 250 de salida.*

| Modelo | Por consulta | Por 1.000 |
|---|---|---|
| GPT-5 nano | 0,000169 | **USD 0,17** |
| DeepSeek V4-Flash | 0,00047 | USD 0,47 |
| **Gemini 3.5 Flash-Lite** | 0,00052 | USD 0,52 |
| Claude Haiku 4.5 | 0,0026 | USD 2,60 |
| Claude Sonnet 5 | 0,0053 | USD 5,30 |

**El rango es 31x**, pero incluso el más caro son 5 dólares cada mil consultas.

### Una evaluación de uso de IA
*8.000 tokens de transcripción, 800 de salida, con Batch (−50%).*

| Modelo | Por evaluación | Las 2.300 del cuatrimestre |
|---|---|---|
| GPT-5 nano | 0,00036 | USD 0,83 |
| Gemini 3.5 Flash-Lite | 0,0011 | USD 2,53 |
| **Claude Haiku 4.5** | **0,006** | **USD 13,80** |
| Claude Sonnet 5 | 0,012 | USD 27,60 |

### Lo demás

| | Modelo | Por unidad |
|---|---|---|
| Una pregunta generada | Flash-Lite + Batch | USD 0,00083 → **un parcial de 15: 1,2 centavos** |
| Corregir una respuesta | Haiku + Batch | USD 0,002 |
| Moderar un mensaje | Capa clásica + `omni-moderation-latest` | **USD 0** — no hay tokens que facturar (ADR-012) |

## 4. Qué significa "que funcione" y cómo probarlo

Cada barra es concreta y verificable. **Ninguna es una opinión.**

| Función | "Funciona" significa | Cómo se prueba | ¿Barra dura? |
|---|---|---|---|
| **Tutor** | **Nunca emite la solución**, ni bajo presión | 30 intentos de jailbreak + 20 pedidos directos. **Cero fugas** | 🔴 Sí — RSK-09 |
| **Moderador** | Detecta las 6 categorías de RF-CHT-10 **sin comerse los falsos positivos rioplatenses** | 100 mensajes etiquetados a mano. >90% en severidad media/alta. **El set debe incluir *boludo* afectuoso y "cálculo"** | 🟡 Medible |
| **Evaluador** | **Pasa PAR-14**: ±5 promedio, ±10 por dimensión | El golden set. **Es la prueba, literalmente** | 🔴 **Sí — sin esto el curso no arranca** |
| **Generador** | El profesor usa las preguntas sin reescribirlas todas | 20 preguntas, un docente marca cuáles usaría. >70% | 🟢 Blanda — hay gate humano |
| **Corrector** | Coincide con corrección humana | 30 respuestas corregidas a mano vs el modelo | 🟡 Medible |

**Fijate la asimetría:** el evaluador tiene barra dura con número exacto y consecuencia catastrófica;
el generador tiene barra blanda **porque un humano revisa todo antes de publicar**. Esa diferencia es
la que decide dónde ahorrar.

### La escalera: probá de abajo hacia arriba y pará en el primero que pase

**Tutor**

| Orden | Modelo | Costo / 1.000 | Probabilidad |
|---|---|---|---|
| 1º | GPT-5 nano | USD 0,17 | ⚠️ Baja |
| 2º | Gemini 3.5 Flash-Lite | USD 0,52 | ✅ Alta |
| 3º | Claude Haiku 4.5 | USD 2,60 | ✅ Muy alta |

**Evaluador** — la escalera que más importa

| Orden | Modelo | Cuatrimestre | Probabilidad de pasar PAR-14 |
|---|---|---|---|
| 1º | GPT-5 nano | USD 0,83 | ❌ Muy baja |
| 2º | **Gemini 3.5 Flash-Lite** | **USD 2,53** | ⚠️ Media — **vale probarlo** |
| 3º | Claude Haiku 4.5 | USD 13,80 | ✅ Alta |
| 4º | Claude Sonnet 5 | USD 27,60 | ✅ Muy alta |

**La diferencia entre el 2º y el 3º son USD 11.** No lo decidas por intuición: el golden set te lo
dice con un número. Y como la calibración hay que correrla igual, probar tres modelos cuesta casi
nada extra.

**Generador** — arrancá por nano de verdad. Hay revisión humana: si las preguntas salen flojas, el
profesor las descarta. Es el lugar más seguro para probar lo barato.

> **La regla general: elegí el más barato que pase la prueba de su función.** No el mejor, no el más
> caro por las dudas.

## 5. Las palancas gratis — aplicalas antes de elegir modelo

**Ninguna negocia calidad. Varias la mejoran.**

| # | Palanca | Efecto | ¿Cuesta calidad? |
|---|---|---|---|
| 1 | Chunks del RAG: 8 → **3** | −50% del input del tutor | **No. Mejora** — sobre-recuperar mete ruido |
| 2 | Historial: completo → **ventana de 4** + resumen | −27% del input | No |
| 3 | **Prompt caching** en tutor y evaluador | −60% del input efectivo | No |
| 4 | Salida del tutor: 400 → **250 tokens** | −38% del costo de salida y **−40% de latencia** | **Mejora.** RF-IA-04 pide pistas, no ensayos |
| 5 | **Batch** en evaluador, generador y corrector | −50% | No. Es la latencia que RF-IA-27 ya tolera |
| 6 | **Capa clásica** antes del clasificador | −70% de las llamadas o más — se mide con `origen` | No. **Además es la red del fail-open** |

### El caching del evaluador es la más subestimada

La rúbrica, las anclas y las instrucciones son **idénticas en todas las evaluaciones**: ~3.000 de los
8.000 tokens de entrada.

| | Input equivalente | Con Haiku + Batch |
|---|---|---|
| Sin caching | 8.000 | USD 13,80 |
| **Con caching del prefijo** | **5.300** | **≈ USD 10,70** |

> ⚠️ **El caching se rompe solo y en silencio.** Basta un `datetime.now()` en el system prompt o un
> JSON sin ordenar las claves para que el prefijo deje de coincidir y pagues todo a precio completo
> **sin ningún error visible**. Verificalo mirando los tokens de lectura de caché: si dan cero
> llamada tras llamada, hay un invalidador escondido.

### Las dos palancas estructurales que no son técnicas

**RF-IA-22 — el techo es obligatorio igual.** No es una optimización, es un requerimiento. Y define
el peor caso:

| Techo de mensajes por desafío | Volumen del tutor | Con Flash-Lite |
|---|---|---|
| 15 | 19.200 llamadas | USD 10,00 |
| **8** | ~11.000 llamadas | **USD 5,70** |

**La adopción real probablemente sea menor.** El PRD duda de que el tutor se use: al descartar un KPI
candidato dice que la adopción es *"el indicador que diría si el componente más caro y más riesgoso
del producto realmente se usa"*. Con 5-6 mensajes de promedio, el tutor baja a la mitad.
**Medilo en el piloto.**

## 6. Cuánto contexto meter

### Cuánto entra

| Modelo | Ventana | Equivale a |
|---|---|---|
| Gemini Flash-Lite, Sonnet 5, Opus 5 | **1.000.000 tokens** | ~750.000 palabras · **~1.500 páginas** |
| Claude Haiku 4.5 | 200.000 tokens | ~300 páginas |

En español, 1 token ≈ 0,75 palabras; una página de apunte ≈ 670 tokens.

> **El apunte completo de una materia entra sin problema. La capacidad no es la restricción.**

### La pregunta natural: ¿por qué no meter el PDF entero?

Un apunte de 200 páginas ≈ 133.000 tokens.

| Enfoque | Por consulta | Las 19.200 del cuatrimestre |
|---|---|---|
| **Apunte entero** | USD 0,020 | **USD 384** |
| Apunte entero **con caching** | USD 0,002 | **USD 38** |
| **RAG con 3 chunks** | USD 0,0001 | **USD 2** |

**Aun con caching cuesta 20 veces más.** Y hay dos motivos que no son de plata:

- **Calidad.** Los modelos atienden mejor al principio y al final de una ventana larga. Si la
  definición está en la página 94 de 200, compite con 199 páginas de ruido. Con RAG es 1 de 3
  fragmentos.
- **Trazabilidad — el decisivo.** El generador tiene que guardar de qué fragmento salió cada pregunta
  (`chunk_fuente_id`) para que el profesor lo vea al lado. **Con el apunte entero no sabés de dónde
  salió nada**, y perdés la pantalla que hace entender todo el sistema.

Es una alternativa legítima —para un corpus de 10 páginas sería lo correcto por simplicidad— pero a
la escala de una materia la trazabilidad sola ya lo decide.

### Qué se mete en cada función

**Tutor — 3.000 tokens**

| Componente | Tokens | ¿Recortable? |
|---|---|---|
| System prompt + reglas RF-IA-04/19 | 500 | Poco. Es lo que evita la fuga |
| **Chunks del RAG** | 900 (3 × 300) | **Sí — la palanca principal** |
| Enunciado del desafío | 400 | No |
| **Código del alumno** | 800 | **No. Es el objeto de la consulta** |
| **Historial** | 500 (ventana de 4 + resumen) | **Sí — la segunda palanca** |

**Evaluador — 8.000 tokens:** rúbrica + anclas (3.000, cacheable) · **transcripción completa** (4.500,
🔴 no truncar) · metadata de tiempos y ediciones (500).

**Generador — 6.000 por pregunta:** instrucciones + schema (1.500) · chunk fuente + contexto (3.500) ·
preguntas ya generadas para no repetir (1.000).

**Moderador — 300 tokens:** instrucciones + las 6 categorías + el mensaje. Nada más.

### La curva: más contexto no es mejor

```
calidad
   ▲          ╭──────────╮
   │       ╭──╯          ╰────────╮
   │    ╭──╯                      ╰──────── ruido
   │  ╭─╯ falta información
   └──┴──────┴───────────┴───────────────▶
      1-2    3-5         10       20+  chunks
             └─ zona útil ─┘
```

**El punto óptimo está más abajo de lo que uno cree.** Para consultas puntuales —que es lo que hace un
alumno— **3 chunks suele ser mejor que 8**, no solo más barato: cada chunk irrelevante obliga al
modelo a decidir qué es pertinente antes de responder.

> **Es la única palanca que ahorra plata y mejora la calidad al mismo tiempo.**

### Ventajas y desventajas de limitar

| A favor | En contra | Cómo se mitiga |
|---|---|---|
| Costo lineal: el input es el 90% del volumen | Puede faltar la teoría relevante | Piso de similitud: si nada supera el umbral, ampliar una vez |
| Latencia | **El tutor "olvida" lo ya intentado** | **Resumen rodante** de 2-3 líneas con lo descartado |
| Mejor señal/ruido | Respuestas cortadas a mitad de frase | Tope en el parámetro **y** en el prompt, coherentes |
| Caching más efectivo | Se pierde matiz en la evaluación | 🔴 Nunca truncar. Ver abajo |
| Peor caso acotado | | |

**El modo de falla más subestimado: el tutor que repite sugerencias.** Con ventana chica y sin
resumen, el alumno escucha por tercera vez "¿probaste agregar un print?" y pierde la confianza. Un
resumen rodante cuesta ~100 tokens y lo evita.

### 🔴 La transcripción del evaluador NO se trunca. Nunca.

RF-IA-13 dice que puntúa **"la transcripción completa"**. Las razones son de fondo:

- **Justicia:** evaluar media conversación es poner nota sobre un expediente incompleto.
- **La dimensión que más pesa está en la progresión:** autonomía (30%) mide si intentó antes de
  preguntar. Cortando el principio o el final no se puede ver.
- **Es apelable:** RF-IA-18 dice que el profesor ve la transcripción completa. Si el modelo vio menos
  que el profesor, la apelación es indefendible.

Si una transcripción no entrara en la ventana, la solución es cambiar de modelo, no truncar. Con los
200K de Haiku entra ~40 veces: **no es un problema real**, pero conviene tenerlo escrito antes de que
alguien "optimice".

### Límites recomendados

| Función | Contexto | Salida máx. | Regla especial |
|---|---|---|---|
| Tutor | 3.000 tok · 3 chunks · ventana de 4 + resumen | **250 tok ≈ 180 palabras** | Nunca la solución de referencia |
| Moderador | 300 tok · solo el mensaje | 30 tok | Sin historial |
| Evaluador | 8.000 tok · **transcripción completa** | 800 tok | 🔴 Nunca truncar |
| Generador | 6.000 tok por pregunta | 600 tok | Incluir las ya generadas |
| Corrector | 2.000 tok | 400 tok | Sin identidad del alumno |

**Entrada del alumno:** ~500 palabras por mensaje · 8-15 mensajes por desafío (RF-IA-22) · 60 por día.

### Cómo saber si estás en el punto justo

| Señal | Qué significa |
|---|---|
| **Aciertos de caché en cero** | Hay un invalidador en el prefijo. Estás pagando de más sin error visible |
| **Chunks efectivamente citados** | Si de 3 usa 1, probá con 2. Si querés 4, subilo |
| **Respuestas cortadas** | Más del 5% llegando al tope = el tope está bajo |

**La prueba cualitativa que más convence:** juntá 20 consultas reales y correlas con 3, 5 y 8 chunks.
Compará a mano. Casi siempre gana 3.

## 7. Velocidad

| Función | Objetivo | ¿Manda la velocidad? |
|---|---|---|
| **Moderador** | < 300 ms — está en el camino de entrega del mensaje | 🔴 **Es lo único que importa** |
| **Tutor** | < 2 s hasta la respuesta completa | 🔴 Sí |
| Corrector, evaluador, generador | Minutos | 🟢 No |

**Latencia medida:** Gemini Flash ~280 ms hasta el primer token (mejor relación velocidad/precio);
Claude Haiku 4.5 ~597 ms. Los dos sirven para el tutor.

> ### ⚠️ En el tutor no podés usar el primer token
>
> RF-IA-20 obliga a comparar la respuesta contra la solución esperada **antes de mostrarla**, así que
> **no hay streaming en desafíos prácticos**. Lo que importa no es el TTFT sino el tiempo hasta la
> respuesta **completa**.
>
> **De ahí sale lo práctico: recortar la salida mejora la latencia percibida más que cambiar de
> modelo.** De 400 a 250 tokens son ~40% menos de espera.

## 8. Qué puede ser gratis

**Solo Gemini tiene un free tier realmente usable.**

| Proveedor | Free tier | Sirve para |
|---|---|---|
| **Google Gemini** | **~1.500 req/día, 15 req/min, sin vencimiento, sin tarjeta** | ✅ Toda la demo y el desarrollo |
| OpenAI | USD 5 una vez, **vencen a los 3 meses** | Probar |
| Anthropic | USD 5 una vez, límites similares | Probar |
| DeepSeek | Sin free tier | — |

### El volumen entra holgado

| | Valor |
|---|---|
| Llamadas del cuatrimestre | ~35.900 |
| **Promedio diario** | **321/día** contra un techo de 1.500 |
| **Utilización** | **21%** |

Lo que rompe el free tier no es el volumen, son dos cosas:

1. **15 req/min.** El pico de un examen necesita ~225. Se cae justo el día que más importa.
2. 🔴 **La política de datos.** Los free tiers suelen permitir al proveedor usar lo enviado para
   mejorar sus modelos. Acá enviás código de alumnos, transcripciones y PII — choca con RF-NFR-09 y
   RSK-01.

### La estrategia "free tier primero, pago por desborde"

Funciona, **y la clave es que un pico solo es problema si alguien está esperando**:

- **Funciones asincrónicas** (evaluador, generador, corrector): si se acaba la cuota, la cola
  **espera**. Sin desborde, sin costo, sin cambio de modelo. Son 47 llamadas por día entre las tres.
- **Funciones sincrónicas** (tutor, moderador): ahí sí desbordás a pago, y **solo pagás el desborde**.

> ⚠️ **El evaluador NO puede desbordar.** RF-IA-25: *"un único modelo activo, no admite pool ni
> enrutamiento"*. Una cascada por cuota **es** enrutamiento. Pero no hace falta: son 21 llamadas por
> día, la cola las drena a 15 RPM sin despeinarse. Marcalo con una bandera `admite_desborde: false`
> para evaluador y corrector.

**Costo de implementarlo: bajo.** Es prácticamente el mismo código que la escalera de degradación de
RF-IA-27 que hay que construir igual — solo cambia el disparador: en vez de "el proveedor falló", es
"se acabó la cuota".

### Qué usar para probar cada paso, sin pagar nada

Los pasos son los de [10](10-entregables-y-plan.md) Parte 2, §8. **La mitad no necesita ningún
modelo** — conviene verlo antes de preocuparse por cuotas.

| Paso | Qué probás | Con qué | Costo |
|---|---|---|---|
| 0 · Glosario | — | Nada | — |
| 1 · Esqueleto | Que `llamar_modelo()` ande contra un proveedor real | **Gemini free tier** | USD 0 |
| 2 · Metadata | Captura de tiempos y ediciones | **Nada** — es código puro | USD 0 |
| 3 · Rúbrica | Que el artefacto declarativo cargue y valide | **Nada** | USD 0 |
| 4 · Features determinísticos | Conteos, tiempos, ediciones | **Nada** | USD 0 |
| 5 · Evaluar una transcripción | Que devuelva 5 dimensiones válidas contra schema | **Gemini free tier** | USD 0 |
| 6 · Golden set chico | Acuerdo entre dos personas | **Nada** — son humanos puntuando | USD 0 |
| 7 · Runner de calibración | Comparar 3 modelos candidatos contra el golden set | **Free + los USD 5 de Anthropic** | ~USD 0,10 |
| 8 · Endpoint de estado | Un JSON | **Nada** — puede ser mock | USD 0 |
| 9 · RAG | Ingesta, chunking, embeddings, retrieval | **Nada** — embeddings locales (ADR-006) | USD 0 |
| 10 · Generador | 5 preguntas desde un PDF, con fuente | **Gemini free tier** | USD 0 |
| 11 · Guardarraíles | AST, comparación, filtro de entrada | **Nada** — es análisis estático | USD 0 |
| 11b · Tutor | Latencia, RF-IA-04, los tres niveles de RF-IA-19 | **Gemini free tier** | USD 0 |
| 12 · Corrector | Nota + justificación sobre respuesta abierta | **Gemini free tier** | USD 0 |
| 13 · Moderador | 100 mensajes etiquetados, acierto en media/alta | **Nada** — capa clásica sin modelo + Moderation API gratuita (ADR-012) | USD 0 |

**Los cuatro pasos que sí llaman a un modelo son 1, 5, 10 y 12** — más el 7, que es el único donde
conviene gastar unos centavos a propósito.

### El único proveedor con free tier realmente usable sigue siendo Gemini

Ver la tabla de §8. **OpenAI y Anthropic dan USD 5 por única vez y vencen**: no sirven como motor de
desarrollo, sí como **munición para el Paso 7**, que es exactamente donde hace falta comparar contra
un modelo que no es Gemini. Guardá esos créditos para eso y no los quemes en el Paso 1.

### El límite que te va a molestar no es el diario, son los 15 req/min

1.500 requests por día alcanzan de sobra para seis personas desarrollando. **15 por minuto no**, si
seis personas comparten una sola clave y alguien corre un lote.

**La solución es trivial y hay que decirla el día 1: cada persona saca su propia clave del free
tier.** Son gratis, no piden tarjeta, y el registro `función → proveedor + modelo` del Paso 1 ya
soporta que la credencial venga de una variable de entorno distinta por máquina.

> 🔴 **Lo que NO se hace con el free tier: datos reales de alumnos.** Los free tiers suelen permitir
> al proveedor entrenar con lo enviado, y eso choca con RF-NFR-09 y RSK-01. **Para desarrollo y demo
> todo es sintético**, así que no hay problema; el día que entre un curso real, la respuesta la da la
> consulta legal de P-06 en [08](08-decisiones-y-pendientes.md), no nosotros.

> ⚠️ **Probar con free no es elegir el modelo.** El evaluador se decide contra el golden set en el
> Paso 7, y RF-IA-25 le prohíbe pool y enrutamiento. Que Flash-Lite ande bien en el Paso 5 no
> significa que pase PAR-14 — eso lo dice la calibración, y **es la única forma legítima de decidirlo**.

## 9. Escenarios y presupuesto

### 🎯 Piso — si todo lo barato pasa sus pruebas

| Función | Modelo | Costo |
|---|---|---|
| Tutor | GPT-5 nano | USD 3,24 |
| Moderador | Capa clásica + clasificador | **USD 0** |
| Evaluador | Flash-Lite + Batch | USD 2,53 |
| Generador | nano + Batch | USD 0,24 |
| Corrector | Flash-Lite + Batch | USD 0,80 |
| | **Total** | **≈ USD 7** |

### ✅ Realista — con el evaluador en terreno seguro

| Función | Modelo | Costo |
|---|---|---|
| Tutor | Flash-Lite | USD 10,00 |
| Moderador | Capa clásica + clasificador | **USD 0** |
| **Evaluador** | **Haiku 4.5 + Batch + caching** | **USD 10,70** |
| Generador | Flash-Lite + Batch | USD 0,70 |
| Corrector | Flash-Lite + Batch | USD 0,80 |
| | **Total** | **≈ USD 22** |

### Con el techo de 8 mensajes de RF-IA-22

| Escenario | Total |
|---|---|
| Piso | **≈ USD 5** |
| Realista | **≈ USD 17** |

### La etapa actual

| Etapa | Costo |
|---|---|
| **Demo local, 4 semanas, datos sintéticos** | **USD 0** — free tier de Gemini |
| Pruebas de la escalera (§4) | < USD 2 |
| Primer piloto real | USD 5 a 22 el cuatrimestre |

## 10. Dónde NO bajar

| Idea | Ahorra | Cuesta |
|---|---|---|
| **Evaluador en un modelo que no calibró** | USD 11 | 🔴 **El curso no arranca.** RF-IA-36, sin override |
| Evaluar solo una muestra de desafíos | ~USD 5 | 🔴 Desarma el mecanismo académico central (RF-IA-15) |
| Sacar el moderador | **USD 0** — ya no cuesta nada | 🔴 Viola RF-CHT-09 |
| Contexto del tutor bajo 2.500 tokens | centavos | 🔴 El tutor deja de ver el código del alumno |
| Cachear respuestas del tutor entre alumnos | poco | 🔴 Contamina la evaluación de RF-IA-09 |

**La primera fila es la única que importa de verdad:** es la diferencia entre ahorrar once dólares y
no poder arrancar el cuatrimestre.

## 11. El orden de trabajo

1. **Aplicá las seis palancas gratis** desde el primer día. Son diseño, no optimización tardía.
2. **Escribí las pruebas de §4 antes de elegir modelos.** Sobre todo el set de jailbreak y el golden set.
3. **Probá la escalera de abajo hacia arriba.**
4. **Dejá el modelo en la tabla de ADMIN**, nunca en el código.
5. **Medí la adopción real en el piloto** y recalibrá con datos.

> **La conclusión honesta:** la diferencia entre la config más barata y la más cara son ~USD 17 por
> cuatrimestre. Vale la pena buscar el mínimo **porque el método te obliga a escribir las pruebas** —
> y esas pruebas valen mucho más que el dinero que ahorrás.

## Fuentes

- Precios de Claude: catálogo oficial de la API de Anthropic, corte 2026-06-24.
- [Gemini API Pricing (agosto 2026) — BenchLM](https://benchlm.ai/google/api-pricing)
- [OpenAI API Pricing (agosto 2026) — BenchLM](https://benchlm.ai/openai/api-pricing)
- [DeepSeek API pricing (agosto 2026) — BenchLM](https://benchlm.ai/deepseek/api-pricing)
- [LLM API Latency Benchmarks 2026 — EdenAI](https://www.edenai.co/post/llm-api-latency-benchmarks-speed-comparison-across-providers)
- [Every AI API with a Free Tier in 2026 — Grizzly Peak](https://www.grizzlypeaksoftware.com/articles/p/every-ai-api-with-a-free-tier-in-2026-the-developers-cheat-sheet-jl33ach0)
- [Gemini API Free Tier limits — TokenMix](https://tokenmix.ai/blog/gemini-api-free-tier-limits)
- [Local LLM hardware requirements 2026](https://www.kunalganglani.com/blog/local-llm-hardware-requirements-2026)
