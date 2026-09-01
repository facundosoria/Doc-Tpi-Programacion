# 08 — Decisiones tomadas y pendientes

> Registro de decisiones con su condición de revisión, y el estado abierto consolidado. *Consolida los antiguos 09 y 17.*

---

# Parte A — Registro de decisiones (ADR)


Registro vivo. Cuando algo cambie, se actualiza acá y se ajusta el documento correspondiente.

---

## Parte A — Decisiones tomadas (ADR)

Formato: qué se decidió, por qué, y **qué tendría que pasar para revisarla**. Ese último campo es el
importante: una decisión sin condición de revisión es un dogma.

---

### ADR-001 — Un único servicio de IA como puerta a todos los LLM

**Decisión:** todo acceso a un proveedor pasa por `ai-service`. El front y el backend de negocio
nunca llaman a un LLM.

**Por qué:** el PRD exige seis propiedades transversales (RF-IA-11, 23, 24, 02, 22, 25, 27) que no
pertenecen a ninguna función en particular. Centralizarlas es implementarlas una vez en vez de cinco.

**Se revisa si:** aparece una función de IA con requisitos de latencia tan extremos que no tolere el
salto interno. No parece probable.

📄 [02](02-arquitectura-y-stack.md)

---

### ADR-002 — Sin orquestador basado en LLM; ruteo determinístico

**Decisión:** no hay un modelo que decida qué función invocar. La ruta se determina por el endpoint.

**Por qué:** la UI ya sabe la ruta. Un router LLM agrega latencia, costo, un punto de falla y —lo más
grave— **una superficie de prompt injection sobre una decisión de control de flujo**.

**Se revisa si:** aparece una única caja de chat donde el alumno escribe cualquier cosa y el sistema
tiene que inferir la intención. Hoy ese producto no existe.

📄 [06](06-operacion-e-ingenieria.md)

---

### ADR-003 — Sincrónico solo para tutor y moderador; el resto por cola

**Decisión:** evaluador, generador, corrector y calibración corren como trabajos asincrónicos.

**Por qué:** tres beneficios de un mismo corte — Batch API (−50%), RF-IA-27 (score diferido) queda
implementado por construcción, y el pico de carga se absorbe sin escalar nada.

**Se revisa si:** el producto llega a necesitar corrección instantánea visible al alumno.

📄 [06](06-operacion-e-ingenieria.md)

---

### ADR-004 — pgvector en el Postgres existente, sin base vectorial dedicada

**Decisión:** los embeddings viven en el mismo Postgres, con la extensión `pgvector`.

**Por qué:** a 120 usuarios el corpus son miles de chunks, no millones. Una base dedicada sería un
contenedor más, un backup más y una falla más, sin beneficio medible.

**Se revisa si:** el corpus supera ~500.000 chunks o la latencia de búsqueda pasa de 100 ms.

📄 [02](02-arquitectura-y-stack.md), [04](04-funciones-de-ia.md)

---

### ADR-005 — Java Spring Boot para nuestro servicio

> ⚠️ **Revisada.** Antes decía Python FastAPI.

**Decisión:** el `ms-evaluacion-llm` va en **Java Spring Boot**, igual que el resto de la plataforma.

**Por qué:** Programación IV es una materia de Java, y la cátedra impone Service Discovery, API
Gateway y bus de eventos — las tres capas donde un servicio Python paga fricción todas las semanas.
Los dos argumentos que quedaban a favor de Python casi desaparecen: si los desafíos son en Java,
`JavaParser` es mejor que `tree-sitter`, y los embeddings tienen camino con DJL o por API.

**Se revisa si:** los desafíos incluyen otros lenguajes, o hacen falta embeddings locales y DJL
resulta demasiado incómodo. **En ese caso se agrega un componente interno Python — que no es un
microservicio.** Las dos interfaces que lo permiten se construyen desde ahora.

📄 [02](02-arquitectura-y-stack.md)

---

### ADR-006 — Embeddings locales, no por API

**Decisión:** modelo de embeddings multilingüe corriendo en un contenedor propio.

**Por qué:** la indexación es offline (sin presión de latencia), corre bien en CPU, el corpus es
chico, cuesta cero, y **evita mandar el material del profesor a un tercero** — lo que simplifica
RF-NFR-09 y RSK-01.

**Se revisa si:** la calidad de recuperación en español resulta insuficiente en pruebas.

📄 [04](04-funciones-de-ia.md)

---

### ADR-007 — El perímetro temático lo hace cumplir el retrieval, no el prompt

**Decisión:** RF-IA-06 se implementa filtrando por `curso_id` en el servidor y exigiendo un piso de
similitud. Si no hay contexto, el tutor no responde.

**Por qué:** una instrucción en el prompt se puede sortear hablando. Un filtro en el servidor no.
La decisión la toma el código, no el modelo.

**Se revisa si:** nunca. Es la implementación correcta.

📄 [05](05-seguridad.md)

---

### ADR-008 — La solución de referencia nunca entra al contexto del tutor

**Decisión:** el tutor ve enunciado, código del alumno, teoría y reglas. **Jamás** la solución ni los
tests ocultos. La comparación de RF-IA-20 corre en el guardarraíl, fuera del prompt.

**Por qué:** es la defensa más fuerte contra RF-IA-04 y RSK-09. No se puede filtrar lo que no se
tiene. Ningún jailbreak extrae algo que el modelo nunca vio.

**Se revisa si:** nunca.

📄 [05](05-seguridad.md)

---

### ADR-009 — Sin streaming token a token en desafíos prácticos (MVP)

**Decisión:** en prácticos, la respuesta del tutor se muestra completa después de pasar el
guardarraíl anti-fuga. Streaming pleno solo en desafíos de riesgo bajo.

**Por qué:** RF-IA-20 exige poder bloquear y regenerar antes de mostrar. No se puede bloquear algo
que el alumno ya está leyendo.

**Se revisa en Fase 2:** streaming con retención selectiva — transmitir la prosa y retener los
bloques de código hasta validarlos.

📄 [05](05-seguridad.md)

---

### ADR-010 — Escenario B de costos: ~USD 21 por cuatrimestre

**Decisión:** tutor y generador en Gemini 3.5 Flash-Lite, evaluador y corrector en Claude Haiku 4.5
con Batch, moderador en GPT-5 nano con pre-filtro. Contexto del tutor recortado a ~3.000 tokens con
prompt caching.

**Por qué:** entra en el objetivo de USD 15-20, y los ~USD 7 de diferencia contra la opción más
barata compran la única cosa que el PRD marca como bloqueante del arranque del curso (RF-IA-36).

**Se revisa si:** (a) un modelo se retira — hay dos con fecha anunciada; (b) la calibración de PAR-14
falla con Haiku 4.5 → subir a Sonnet 5; (c) GPT-5 nano supera las pruebas de jailbreak → bajar el
tutor y ahorrar USD 7.

📄 [03](03-modelos-costos-y-contexto.md)

---

### ADR-011 — El evaluador nunca corre en un modelo local

**Decisión:** local es aceptable para moderador y como plan B del tutor. Para el evaluador, no.

**Por qué:** PAR-14 exige ±5 de desviación promedio y ±10 por dimensión contra el golden set. Un
modelo abierto de 30B no sostiene esa tolerancia de forma estable, y sin pasarla **el curso no
arranca** (RF-IA-36, sin override).

**Se revisa si:** un modelo abierto pasa la calibración de forma reproducible. Se mide, no se opina.

📄 [03](03-modelos-costos-y-contexto.md)

---

### ADR-012 — El moderador resuelve con técnica clásica; la IA solo cubre el residuo

**Decisión:** el moderador resuelve con **tecnología clásica** todo lo resoluble —listas con nivel por
término, heurísticas de spam, forma de código y detección de base64—, que cubre **cuatro de las seis
categorías** de RF-CHT-10. La base de términos es el `dictionary.es`/`dictionary.en` de
`com.modernmt.text:profanity-filter` (Apache-2.0), **verificado**: trae score por término en vez de
booleano y está calibrado para el registro rioplatense. LDNOOBW queda como plan B. Para el residuo
contextual —acoso y amenaza sin léxico explícito— invoca un **clasificador dedicado**
(`omni-moderation-latest`), **no un LLM con prompt**.

**Por qué:** el filtrado de lenguaje ofensivo es un problema resuelto desde antes de que existieran
los LLM, y hay librerías Java maduras y listas publicadas que lo hacen. Cuatro razones concretas:

1. **Latencia** — el presupuesto es < 300 ms ([02](02-arquitectura-y-stack.md)) y un roundtrip HTTP
   externo se lo come casi entero. Un match en memoria es < 1 ms.
2. **Es la red del fail-open** — sin capa clásica, el fail-open de P-02 significa *sin ninguna
   moderación*.
3. **Datos** — cuantos menos mensajes de alumnos salgan del sistema, mejor ([07](07-datos-y-terminos.md)).
4. **Elimina el prompt del moderador** — y con él la superficie de prompt injection sobre el
   moderador, la necesidad de `temperature: 0` + `seed` (A-3 de
   [14](14-sincronizacion-guia-didactica.md)) y el pendiente E-05. Un clasificador no interpreta
   instrucciones: recibe texto y devuelve etiquetas con score.

> ⚠️ **El argumento no es el ahorro de tokens.** La Moderation API es **gratuita** y no consume
> tokens del presupuesto. Quien justifique esta decisión por costo de tokens la está justificando
> mal: las razones son latencia, resiliencia y datos.

**Para que no quede duda de qué es cada pieza:** la capa clásica **no es IA** —son algoritmos y tablas
de datos, cero tokens, cero red— y el clasificador **no es un LLM** —entra texto, salen etiquetas, sin
prompt y sin tokens de salida—. **El moderador queda sin ningún LLM generativo adentro**, y su costo
en [03](03-modelos-costos-y-contexto.md) es USD 0 porque no hay nada que facturar, no porque se haya
estimado con optimismo. La tabla está en [04](04-funciones-de-ia.md) §2.0.

**Se revisa si:** la medición del campo `origen` muestra que la capa clásica decide una proporción
baja de los mensajes, o si los falsos positivos rioplatenses resultan inmanejables. Si además la
política de datos llega a prohibir que los mensajes salgan del sistema, la alternativa evaluada es un
modelo local —**pysentimiento** (RoBERTuito, entrenado en español rioplatense) o Detoxify
multilingüe—, que ADR-005 contempla como *componente interno Python, no microservicio*.

**¿Y si en algún momento hay que meterle un LLM?** Con esto alcanza para cumplir RF-CHT-09 a
RF-CHT-14; no hay una fase 2 pendiente. Los cuatro escenarios que sí obligarían a revisarlo —y los
seis pedidos habituales que **no** lo justifican— están enumerados en [04](04-funciones-de-ia.md)
§2.10. El único serio es el **acoso acumulativo**, y ahí la observación importante es que **no se
arregla cambiando de modelo sino cambiando el contrato**: el LLM más caro del mundo detrás de
`moderar(mensaje)` tampoco lo detecta, porque el problema es que solo ve un mensaje.

**Los patrones que implementan esta decisión** están mapeados uno a uno en
[04](04-funciones-de-ia.md) §2.3.4b y §2.3.4c. En resumen: **Pipes and Filters** para la
normalización, **Composite** de **Strategy** para los detectores clásicos —que corren **todos** y
fusionan veredictos, porque `categorias` es un array—, y **Chain of Responsibility** de **solo dos
eslabones** para el corte clásico → clasificador, que es el único lugar donde hay trabajo caro que
evitar. El resto —**Adapter**, **Factory Method**, **Circuit Breaker**, **Rate Limiter**,
**Idempotent Receiver**— ya venía de ADR-001 y de [02](02-arquitectura-y-stack.md).

Ahí también está por qué **no** va un orquestador LLM (lo prohíbe ADR-002) ni eventos entre los
detectores (el moderador es sincrónico, ADR-003).

📄 [03](03-modelos-costos-y-contexto.md), [04](04-funciones-de-ia.md), [13](13-rubrica-y-prompts.md)

---

## ⚠️ Decisiones que se revisaron durante el diseño

**Leé esto antes de reabrir una discusión.** Siete decisiones cambiaron mientras se armaba la
documentación, y los documentos ya reflejan la versión final — pero si encontrás una afirmación que
parece contradecir a otra, probablemente sea una de estas.

| # | Antes | Ahora | Por qué cambió |
|---|---|---|---|
| 1 | Monolito modular | **Microservicios** | La cátedra los declara no negociables |
| 2 | pgvector en base compartida | **Base propia y exclusiva** | *"Cada servicio es dueño exclusivo de su base"* |
| 3 | Python FastAPI | **Java Spring Boot** | La materia es de Java + fricción con Spring Cloud |
| 4 | Sin streaming en prácticos | **Buffer Interceptor** | La guía didáctica lo resuelve mejor ([14](14-sincronizacion-guia-didactica.md) C-2) |
| 5 | Redis obligatorio | **Probablemente innecesario** | A 120 usuarios, Postgres alcanza |
| 6 | El corrector no usa el RAG | **Sí lo usa**, por el chunk trazado | La pregunta nació de un fragmento; ese fragmento sirve al corregir |
| 7 | ~USD 125 por cuatrimestre | **USD 5 a 22** | Al optimizar el contexto del tutor |

> **La #4 vino de afuera:** existe otro set de documentación (la *guía didáctica*) que en ese punto
> tenía mejor solución que la nuestra. La comparación completa está en
> [14](14-sincronizacion-guia-didactica.md).


---

## Parte B — Preguntas abiertas para el Product Owner

Van con recomendación, no solo con la pregunta.

---

### ❓ P-01 — ¿El corrector de respuestas abiertas tiene golden set y calibración?

**El hueco:** el PRD construye una maquinaria completa alrededor del **evaluador de uso de IA**
(rúbrica versionada, golden set en dos niveles, calibración bloqueante, deriva, auditoría,
apelación). El **corrector de respuestas abiertas** no está especificado como función propia — y
tiene exactamente el mismo problema: una IA poniendo una nota.

**Recomendación:** aplicarle el mismo aparato. El argumento que justifica el del evaluador es
idéntico. Si el PO decide que no, que sea una decisión consciente y quede registrada.

**Prioridad:** Alta — condiciona el diseño del corrector.

📄 [04](04-funciones-de-ia.md)

---

### ❓ P-02 — ¿Qué pasa si el moderador de chat no está disponible?

**El hueco:** RF-CHT-09 dice que corre sobre **todo** mensaje, **antes** de entregarlo. RF-IA-27
enumera la degradación del tutor y del evaluador, pero **no dice nada del moderador**.

**Opciones:** fail-closed (se detiene el chat) / fail-open (se entrega sin moderar) / fail-open con
red (se entrega, el pre-filtro determinístico sigue corriendo, se marca y se re-modera al volver).

**Recomendación: fail-open con red.** El chat social no es producción académica — es lo único que
RF-NFR-01 permite borrar físicamente. Bloquearlo por una caída externa contradice el principio rector
de RF-IA-27.

**Prioridad:** Media.

📄 [06](06-operacion-e-ingenieria.md)

---

### ❓ P-03 — ¿La corrección automática es definitiva o sugerencia?

**La pregunta:** ¿la nota que pone la IA se aplica sola, o queda como sugerencia hasta que el
profesor confirma?

**Recomendación: sugerencia en el MVP.** Pasar a automática solo cuando haya datos de precisión que
lo respalden — el mismo criterio de evidencia que el PRD aplica al evaluador.

**Prioridad:** Alta — cambia el flujo de la entrega y la UI del profesor.

📄 [04](04-funciones-de-ia.md)

---

### ❓ P-04 — ¿Quién produce el golden set base y para cuándo?

**El problema:** RF-IA-36b lo dice explícitamente — es un **hito de calendario académico, no
técnico**. Docentes tienen que producir y puntuar manualmente transcripciones de referencia. Y es
criterio de release (DoD 7b y 7d).

**Es el único criterio de salida del MVP cuya ejecución no depende del equipo de desarrollo.** Si
nadie lo agenda, el proyecto se termina y el curso igual no arranca.

**Recomendación:** fecha límite propia, con margen real antes del inicio del período lectivo, y un
responsable con nombre y apellido. **Definirlo ahora, no en marzo.**

**Prioridad:** 🔴 **Crítica** — es un bloqueo duro sin override.

📄 [04](04-funciones-de-ia.md)

---

### ❓ P-05 — ¿Cuáles son los umbrales concretos de RF-IA-22?

**El hueco:** RF-IA-22 exige límites de uso por usuario y difiere los números al Low Level Design,
*"dado que dependen de costo y cuota disponible"*. Ese Low Level Design es este documento.

**Recomendación inicial**, a ajustar con datos reales:

| Límite | Valor propuesto | Fundamento |
|---|---|---|
| Mensajes al tutor por desafío | 15 | Acota el peor caso sin molestar el uso normal (promedio esperado: 5-6) |
| Mensajes al tutor por día | 60 | |
| Desafíos personalizados por día (RF-DES-05, Fase 3) | 5 | Sin límite, es la vía obvia de abuso de cuota |
| Regeneraciones de parcial por profesor por día | 20 | |

**Nota:** estos límites son también el control de costo. Con el techo de 15 mensajes, el peor caso
del tutor está acotado por diseño y no depende del comportamiento de los alumnos.

**Prioridad:** Media — se puede empezar con estos y calibrar.

📄 [03](03-modelos-costos-y-contexto.md)

---

### ❓ P-06 — ¿La política de datos del free tier permite usarlo con datos de alumnos?

**El problema:** los free tiers de los proveedores habitualmente permiten **usar los datos enviados
para mejorar sus modelos**. Acá los datos son código de alumnos, transcripciones y PII.

Eso choca con **RF-NFR-09** (declarar en T&C qué proveedores reciben el material del alumno) y con
**RSK-01 / Ley 25.326**.

**Recomendación:** free tier **solo** para demo y desarrollo con datos sintéticos. Cualquier curso
real, sobre cuenta paga con la política de retención verificada por escrito. La verificación es una
tarea legal, no técnica.

**Prioridad:** Alta si se pensaba usar free tier en producción.

📄 [03](03-modelos-costos-y-contexto.md) §4b

---

### ❓ P-07 — ¿Se corre algo local por soberanía de datos?

**El contexto:** RSK-01 (cumplimiento Ley 25.326) quedó marcado como riesgo y RF-IA-11 aclara que los
T&C *"mitigan el riesgo contractual/reputacional pero no reemplazan un análisis formal de
cumplimiento"*.

**El dato:** por costo, local **nunca** conviene — la API cuesta USD 15-20 el cuatrimestre y una GPU
usable son USD 800-2.000. Por **soberanía de datos**, sí puede convenir, y ese es el argumento
honesto y el más defendible frente a un jurado.

**Recomendación:** moderador en local (barato, viable, alto volumen, bajo riesgo). Tutor local
documentado y probado como plan B/C. Evaluador nunca local (ADR-011).

**Prioridad:** Baja para el MVP, Alta para la defensa del TP.

📄 [03](03-modelos-costos-y-contexto.md) §7

---

### ❓ P-08 — ¿Qué desafíos prácticos entran al MVP y en qué lenguajes?

**Por qué importa para la IA:** determina qué parsers de AST hacen falta para RF-IA-20 y qué reglas
de RF-IA-19 hay que escribir primero. "Encuentra el bug" y "completado de bloques" son **riesgo
alto** y son los que más trabajo de guardarraíl exigen.

**Recomendación:** arrancar con **riesgo medio** (algoritmos con tests unitarios), que es el más fácil
de proteger, y sumar los de riesgo alto después con el guardarraíl ya probado.

**Prioridad:** Media.

📄 [05](05-seguridad.md)

---

### ❓ P-09 — ¿La respuesta del propio agente `@mención` también se modera?

**El hueco:** RF-CHT-09 dice que el moderador corre sobre *"todo mensaje... antes de que se entregue
a los demás participantes"*. Leído literal, la respuesta del agente es un mensaje que se entrega a
los demás participantes, así que sí. Pero el PRD nunca lo dice de forma explícita, y es la clase de
cosa que se implementa como se leyó y después no coincide entre equipos.

**Por qué importa:** es la defensa contra un prompt injection plantado en el canal. Si alguien
consigue que el agente escriba algo que no debería, moderar la salida es lo único que lo detiene
antes de que quince personas lo lean.

**Recomendación: sí, se modera igual que cualquier otro mensaje.** Duplica el costo de una mención
—dos llamadas en vez de una— y ese costo es despreciable frente al riesgo.

**Prioridad:** Baja mientras el agente sea Fase 3. Sube a Alta el día que se adelante.

📄 [04](04-funciones-de-ia.md) Parte 4

---

### ❓ P-10 — ¿Las menciones al agente cuentan contra los límites de RF-IA-22?

**El hueco:** RF-IA-22 pide límites de uso de IA por usuario, y P-05 propone umbrales concretos para
el tutor y el generador. **Ninguno contempla el chat.**

**Por qué importa:** si las menciones no cuentan, son la vía libre para agotar la cuota del curso —y
encima desde el canal más informal, que es donde menos se mira.

**Recomendación: sí, el mismo pozo que el tutor.** Un alumno tiene N interacciones de IA por día,
las gaste donde las gaste. Es más simple de explicar y no deja un agujero.

**Prioridad:** Baja hoy; se resuelve junto con P-05.

📄 [04](04-funciones-de-ia.md) Parte 4

---

### ❓ P-11 — ¿A cuántos agentes se puede mencionar, y cómo se llaman?

**El hueco:** RF-CHT-05 dice *"agentes de IA"*, en plural, y `@agente` como ejemplo. No define si hay
uno solo por curso o varios con roles distintos.

**Por qué importa para nosotros:** cambia el ruteo del gateway. Un agente único es una fila más en la
tabla `función → proveedor + modelo`. Varios agentes son varias funciones, cada una con su prompt, su
perímetro y su costo.

**Recomendación: uno solo cuando llegue la Fase 3.** Sumar `@material` o `@tutor` después es barato;
arrancar con tres y descubrir que dos no se usan, no.

**Prioridad:** Baja — no bloquea nada hasta Fase 3.

📄 [04](04-funciones-de-ia.md) Parte 4

---

## Parte C — Cosas por definir cuando llegue el momento

No urgentes, pero anotadas para no redescubrirlas:

- Retención de las transcripciones IA en la base de vectores vs en la base académica (RF-NFR-10,
  PAR-16: 5 años).
- Cómo se versionan los prompts y quién los aprueba (¿código? ¿tabla? ¿ambos con revisión?).
- Formato del artefacto de rúbrica: tiene que ser legible por un docente, no solo por el sistema
  (RF-IA-29 dice "artefacto declarativo").
- Idioma: el MVP es solo español (RF-NFR-07b), pero RF-NFR-08 dice que al sumar un idioma hay que
  **recalibrar el evaluador por idioma**. Diseñar el golden set con `idioma` como campo desde ahora
  cuesta cero y después vale mucho.
- Qué pasa con las evaluaciones en curso cuando el ADMIN cambia el modelo evaluador a mitad de
  cuatrimestre (RF-IA-28 lo permite; RF-IA-33 pide señalizar la cohorte afectada).
- **Purga selectiva del chat social.** RF-CHT-08 borra el canal al archivar el curso, pero RF-CHT-14
  retiene los mensajes con incidente y su contexto inmediato, y RF-CHT-08 retiene además los pares
  mención-respuesta del agente. Son dos excepciones dentro del mismo canal: la purga no puede ser un
  borrado por curso. Ver [04](04-funciones-de-ia.md) Parte 4.
- **Dato personal escrito junto a una mención.** Si un alumno menciona al agente en el mismo mensaje
  en el que escribe algo personal, ese mensaje queda bajo el régimen general de retención aunque el
  resto del canal se borre. Se cruza con RSK-11 y con el mecanismo de supresión diferido.
- **Umbral entre severidad baja y media del moderador.** RF-CHT-11 define las acciones pero no dónde
  corta. Es lo que determina cuántos falsos positivos come el alumno; se afina con datos reales, no
  antes.


---

# Parte B — Qué falta definir


> Estado al **2026-08-30**. Consolidado de todo lo que está abierto, ordenado por qué bloquea.
> Cada punto trae **mi recomendación**, así en la mayoría solo hace falta confirmar, no deliberar.

## Resuelto desde la primera versión

| # | Estaba abierto | Resuelto |
|---|---|---|
| **A-2** | ¿Quién hace el tutor? | 🟢 **De hecho, nosotros.** Hacemos el componente Angular del chat, así que el servicio también es nuestro |
| **A-5** | ¿Cuántos desplegables? | 🟢 **Dos:** servicio + worker, misma imagen |
| **N-1** | ¿Cómo consumen el componente Angular? | 🟢 **El front es un monolito compartido.** Es una carpeta más del repo, sin librería npm |
| **B-2** | ¿Quién guarda la transcripción? | 🟢 **Nosotros**, porque el tutor es nuestro. Y capturamos la metadata de tiempos nosotros mismos |
| — | ¿Quiénes son los "docentes" del golden set? | 🟢 **Personas físicas, nunca un modelo.** En el TP pueden ser 2 del equipo actuando como docentes |
| — | Tamaño del equipo | 🟢 **6 personas.** Reparto en [10](10-entregables-y-plan.md) |
| — | ¿Producto o demo? | 🟢 **Tiene que funcionar, pero primero demo local.** Plan de 4 semanas en [10](10-entregables-y-plan.md) |

**Lo que sigue abierto es lo de abajo.** Los ítems tachados quedan por trazabilidad.

## Cómo leer esta lista

| Marca | Significa |
|---|---|
| 🔴 | Bloquea el diseño o el arranque. No se puede diferir |
| 🟡 | Se puede arrancar con un supuesto, pero hay que confirmarlo antes de implementar |
| 🟢 | Podés decidirlo vos solo. Solo hay que dejarlo escrito |

---

# A. Lo que bloquea el gráfico de arquitectura

**Son cinco. Con estas cinco respondidas, el diagrama se puede dibujar completo.**

### 🔴 A-1 — ¿El alcance del Tema 07 es estricto o amplio?

| Opción | Qué incluye | Consecuencia en el diagrama |
|---|---|---|
| **Estricto** | Los 6 ítems de la cátedra: rúbrica, invocación del modelo, golden set, calibración, bloqueo de activación, salvaguarda anti-fuga | Un servicio chico. Sin RAG. Sin funciones de tutor/generador/corrector |
| **Amplio** *(recomendado)* | Todo lo anterior + **ser el servicio de LLM de la plataforma**: toda invocación a un modelo pasa por acá | El servicio del diagrama publicado, con las 5 funciones y el RAG |

**Recomendación: proponer el amplio.** El argumento decisivo está en [02](02-arquitectura-y-stack.md) §2b: la cátedra ya te asignó la **salvaguarda anti-fuga**, que corre sobre la respuesta del tutor — no se puede ser dueño de eso sin estar en el camino del tutor. Y sin tutor no hay transcripción, así que el Tema 07 se quedaría sin insumo.

**Decide:** sesión de integración.

---

### 🔴 A-2 — ¿Quién construye el tutor?

No está asignado a ningún tema. Pero **RF-IA-01 lo pone en el alcance** y es **criterio de release del MVP (DoD 7)**.

**Recomendación:** que sea del Tema 07, o al menos que su invocación al modelo pase por el Tema 07.

**Decide:** sesión de integración. Si nadie lo toma, hay que escalarlo al Product Owner.

---

### 🔴 A-3 — ¿Quién construye el RAG?

RF-IA-08 lo exige y no está asignado a ningún tema. Lo necesitan el tutor, el generador y el corrector.

**Dato útil para priorizar:** ninguna de las 6 funciones del Tema 07 estricto necesita el RAG. Si el alcance queda estricto, esto no es tuyo.

**Recomendación:** va junto con A-1. Si el alcance es amplio, es tuyo.

---

### 🟡 A-4 — ¿Quién provee el API Gateway, el Service Discovery y el bus de eventos?

El API Gateway figura como **extra asignado al Tema 01** (columna «podría ser»). El contrato de eventos lo define el **Tema 11**. El Service Discovery no aparece asignado a nadie.

**Por qué importa para vos:** son tres dependencias de infraestructura que tu servicio necesita para arrancar y que no controlás.

**Recomendación:** pedir fecha comprometida para los tres. Mientras tanto, desarrollá contra un stub — tu servicio no debería quedar bloqueado esperando infraestructura ajena.

---

### 🟡 A-5 — ¿Cuántos desplegables somos?

| Opción | Cuándo conviene |
|---|---|
| Uno (`ms-evaluacion-llm`) | Si el alcance es estricto |
| **Dos** (`ms-evaluacion-llm` + `worker`) *(recomendado)* | Misma imagen, distinto comando. El worker drena la cola de trabajo diferido de RF-IA-27 |

**Recomendación: dos.** Es gratis (misma imagen) y es lo que hace real el cálculo diferido.

**Decide:** vos.

---

# B. Contratos con otros equipos

Lo que hay que acordar en la sesión de integración. **Cada uno que no cierre es un bloqueo tuyo más adelante.**

### 🔴 B-1 — Tema 05: ¿cómo obtenemos la solución esperada?

La salvaguarda anti-fuga (**tuya sin discusión**) compara la respuesta del tutor contra la solución esperada del desafío. **Esa solución vive en el Tema 05.**

**El problema:** les estás pidiendo que expongan algo que hoy consideran interno y sensible. Va a haber resistencia.

**Recomendación:** que el Tema 05 exponga un endpoint que devuelva la solución esperada **solo al Tema 07** y solo para comparación. Nunca la almacenás; la usás y la descartás. Ofrecer esa garantía por escrito destraba la conversación.

---

### 🔴 B-2 — ¿Quién es dueño de la transcripción alumno-tutor?

**Esta es nueva, y la crea la regla de microservicios.** *"Cada servicio es dueño exclusivo de su base"* — entonces, si el tutor no es tuyo, la transcripción tampoco. Pero **el evaluador la necesita para puntuar**.

| Opción | Cómo |
|---|---|
| **A** — El dueño del tutor la guarda y te la manda en el pedido de evaluación | Vos no la persistís. Más limpio respecto de RF-NFR-10 |
| **B** — Vos la guardás | Te hace dueño de PII con retención de 5 años |

**Recomendación: la A.** El evaluador recibe la transcripción como parámetro, la evalúa y guarda solo el resultado. Menos PII de tu lado, menos obligaciones de retención, y coincide con la frase de la cátedra: *"el evaluador no conoce desafíos, cursos ni alumnos"*.

**Ojo:** la metadata de tiempos entre mensajes y ediciones de código ([07](07-datos-y-terminos.md) §3.1) es **evidencia de la dimensión que más pesa** y no se puede reconstruir después. Si la guarda otro equipo, **hay que pedirles explícitamente que la capturen** — no la van a capturar solos.

---

### 🔴 B-3 — Tema 11: los campos del contrato de eventos

El Tema 11 define el contrato de eventos **para toda la plataforma** y su decisión condiciona a cinco equipos.

**Es urgente por secuencia, no por importancia: una vez cerrado, pedir un campo nuevo es renegociar con todos.**

**Lo que necesitás que incluyan:** `curso_cohorte_id`, `intento_id`, `alumno_id`, `rubric_version`, `model_id`, `model_version`, `score_agregado`, `confianza`, `estado` y `trace_id`.

---

### 🟡 B-4 — Tema 02: el contrato de consulta de calibración

El documento de la cátedra lo da como ejemplo textual: *"El Tema 02 le pregunta al Tema 07 si la calibración del curso está aprobada, y necesita ese sí para poder activar: es sincrónico."*

**Es la dependencia más visible que otros tienen sobre vos.** El Tema 02 no puede activar cursos sin esto.

**Recomendación:** definilo y entregalo temprano, aunque al principio devuelva un valor mockeado. Desbloquea al otro equipo y te saca presión en la integración.

---

### 🟡 B-5 — Tema 03 / Tema 10: quién aplica el XP

Conceptualmente ya está resuelto ([01](01-problema-y-alcance.md) §2c) y la cátedra coincide. **Falta escribirlo en el contrato.**

Los cuatro puntos: vos devolvés score 0-100 y nunca XP; el Tema 10 aplica PAR-05; vos exponés el contador de pendientes; **el backend implementa la degradación de RF-IA-27** (que la entrega se acepte con tu servicio caído). Ese último es el que más se cae entre equipos.

---

### 🟡 B-6 — Tema 12: ¿quién es dueño de la config del proveedor LLM?

Tema 12 dice *"gestión del proveedor LLM, exclusiva de ADMIN"*. Tema 07 tiene *"configuración centralizada"* en «podría ser». **Dos temas se la atribuyen.**

**Recomendación:** Tema 12 dueño de la **pantalla y el dato**; Tema 07 dueño de **aplicarla** en cada llamada. Y acordar el contrato de lectura que el Tema 12 necesita (estado de calibración, deriva, costo por curso) en el sprint 1 — el documento avisa que sin eso el Tema 12 *"no tiene nada demostrable"*.

---

### 🟡 B-7 — ¿El material del curso cuelga del template o de la cohorte?

Solo aplica si el RAG es tuyo (A-3). La cátedra advierte en §1.4 que si modelás sin la clave de cohorte, *"después no hay forma de acotarlas sin migrar datos"*.

**Recomendación:** el material didáctico cuelga del **template** (se reutiliza entre cohortes); la **calibración y las evaluaciones son de la cohorte**. El chunk lleva las dos claves. Lo respalda el Tema 02: *"calibración que se copia pero se reaprueba"* al clonar.

**Confirmalo antes de definir el esquema del chunk.**

---

# C. Decisiones del Product Owner

### 🔴 C-1 — El golden set: quién lo produce y para cuándo

**El punto más urgente de toda la lista.** La cátedra lo confirma: *"el golden set depende de producción de contenido docente, no de desarrollo: es una dependencia externa al equipo"*.

Sin calibración aprobada **ningún curso pasa de borrador a activo**, y no hay override ni de ADMIN (RF-IA-36).

**Lo que hay que pedir, concreto:** ~26 h de trabajo de dos docentes (40 transcripciones puntuadas por dos personas de forma independiente, más resolución de desacuerdos), **terminadas 3 semanas antes del inicio del período lectivo**.

📄 [04](04-funciones-de-ia.md)

---

### 🔴 C-2 — ¿El free tier puede tocar datos de alumnos?

Los free tiers habitualmente permiten al proveedor usar lo enviado para mejorar sus modelos. Acá son código de alumnos, transcripciones y PII. Choca con RF-NFR-09 y RSK-01 / Ley 25.326.

**Es consulta legal y las consultas legales tardan.** Define tu modelo de costos entero.

**Recomendación:** free tier para demo y desarrollo con datos sintéticos, sin objeción. Producción, solo con la política verificada por escrito.

---

### 🟡 C-3 — ¿El corrector lleva golden set y calibración como el evaluador?

El PRD monta toda la maquinaria de calibración para el **evaluador de uso de IA** y no dice nada del **corrector de respuestas** — que tiene el mismo problema: una IA poniendo una nota.

**Recomendación: sí, el mismo aparato.** Si el PO decide que no, que quede registrado como decisión consciente.

---

### 🟡 C-4 — ¿La corrección es definitiva o sugerencia hasta que el profesor confirma?

**Recomendación: sugerencia en el MVP.** Pasar a automática cuando haya datos de precisión que lo respalden — el mismo criterio de evidencia que el PRD aplica al evaluador.

---

### 🟡 C-5 — ¿Qué pasa si el moderador de chat se cae?

Solo aplica si el moderador es tuyo. RF-CHT-09 dice que corre sobre todo mensaje antes de entregarlo; **el PRD no dice qué pasa si no está disponible**.

**Recomendación: fail-open con red.** Se entrega, el pre-filtro determinístico sigue corriendo, se marca y se re-modera al volver. El chat social no es producción académica — es lo único que RF-NFR-01 permite borrar.

---

# D. Decisiones que podés tomar vos

Solo hay que dejarlas escritas.

| # | Decisión | Recomendación | Se revisa si |
|---|---|---|---|
| 🟢 D-1 | Lenguaje del servicio | **Java Spring Boot**, igual que el resto de la plataforma. Ver [02](02-arquitectura-y-stack.md) | Hagan falta otros lenguajes en los desafíos, o embeddings locales |
| 🟢 D-2 | Base de datos | **Postgres propio y exclusivo** (+ pgvector si el RAG es tuyo) | — |
| 🟢 D-3 | Cola interna | **Redis persistente**, workers propios. Es diseño interno, no viola la regla del bus | — |
| 🟢 D-4 | Modelo evaluador | **Claude Haiku 4.5 con Batch** como punto de partida | **La calibración manda.** Si Flash-Lite pasa PAR-14, usalo y ahorrás. Si Haiku no pasa, subí a Sonnet 5 |
| 🟢 D-5 | Tamaño del golden set base | **40 transcripciones**, con cobertura bajo/medio/alto en las 5 dimensiones y casos de frontera | La desviación resulte ruidosa |
| 🟢 D-6 | Docentes por transcripción | **2, puntuando de forma independiente** | — |
| 🟢 D-7 | Acuerdo mínimo entre docentes | **±10 por dimensión**, igual que PAR-14. Si no acuerdan, se arregla la rúbrica antes de calibrar | — |
| 🟢 D-8 | Umbrales de RF-IA-22 | 15 mensajes de tutor por desafío · 60 por día · 20 regeneraciones de parcial por profesor por día | Con datos reales de uso |
| 🟢 D-9 | Formato de la rúbrica | Artefacto declarativo versionado, **legible por un docente** — RF-IA-29 dice "declarativo" | — |
| 🟢 D-10 | Idioma en el golden set | Guardar `idioma` como campo **desde ahora**, aunque el MVP sea solo español | — |

> **D-10 cuesta cero hoy y vale mucho después:** RF-NFR-08 dice que al incorporar un idioma hay que **recalibrar el evaluador por idioma**. Un golden set sin ese campo obliga a migrar datos.

---

# E. Lo que ya está decidido

Para que no se vuelva a discutir. Detalle en [08](08-decisiones-y-pendientes.md) y [09](09-preguntas-y-respuestas.md).

| Decisión | Estado |
|---|---|
| Sin orquestador basado en LLM; ruteo determinístico | ✅ Firme |
| AI Gateway interno que envuelve toda llamada a un modelo | ✅ Firme |
| Sincrónico solo para tutor y moderador; el resto por cola | ✅ Firme, y coincide con §1.3 de la cátedra |
| El evaluador **no tiene fallback de modelo** (RF-IA-25). Su plan B es la cola diferida | ✅ Firme |
| La solución de referencia **nunca** entra al contexto del tutor | ✅ Firme |
| El perímetro temático lo hace cumplir el retrieval, no el prompt | ✅ Firme |
| Sin streaming token a token en desafíos prácticos (RF-IA-20) | ✅ Firme para el MVP |
| Vos das el score; el motor de desafíos aplica el XP | ✅ Firme, y la cátedra coincide |
| El evaluador nunca corre en un modelo local | ✅ Firme, salvo que la calibración demuestre lo contrario |
| Microservicios, API Gateway, base propia por servicio | ✅ Impuesto por la cátedra |
| pgvector en base compartida | ❌ **Anulado** por la cátedra |
| Monolito modular | ❌ **Anulado** por la cátedra |

---

# F. A debatir dentro del equipo: la pantalla del golden set

Siete decisiones de diseño sobre la pantalla de puntuación. **Ninguna está tomada.** El mockup con
los puntos marcados está publicado como artifact.

| # | Decisión | Recomendación |
|---|---|---|
| 🟡 **F-1** | ¿El docente ve de qué alumno es la transcripción? | **Anónima.** La cátedra dice que el evaluador no conoce alumnos; el mismo criterio vale para quien lo calibra |
| 🟢 **F-2** | ¿La banda de "puntuación a ciegas" es visible o el bloqueo es silencioso? | **Visible.** Explica por qué la pantalla se siente incompleta y evita que pidan "ver lo del otro" como función faltante |
| 🔴 **F-3** | ¿Cuánto del desafío ve el docente? | **Consigna sí, solución esperada no.** Con la solución a la vista, tiende a puntuar si el alumno resolvió — que no es lo que la rúbrica mide |
| 🟡 **F-4** | ¿Las anclas de la rúbrica siempre visibles o a pedido? | **Siempre.** Son lo que reduce la varianza entre personas. El costo del espacio es menor que el de la varianza |
| 🟡 **F-5** | ¿La justificación por dimensión es obligatoria? | **Solo en los extremos** (menos de 30, más de 70) y donde haya desacuerdo. Es lo que convierte 4 horas en 26 |
| 🔴 **F-6** | ¿Se muestra el score agregado mientras puntúa? | **Ocultarlo hasta completar las 5 dimensiones.** Si lo ve antes, decide "esto es un 60" y mueve las dimensiones hasta que dé 60 |
| 🟡 **F-7** | ¿Qué diferencia "saltear" de "marcar como ambiguo"? | **Ambiguo es una marca de valor, no un descarte.** Los casos de frontera son los más útiles del golden set. Reservar ~8 de las 40 a propósito |

### Y tres pantallas más del mismo flujo que todavía no existen

| Pantalla | Para qué | Prioridad |
|---|---|---|
| **Carga** de transcripciones al conjunto | Sin esto no hay nada que puntuar | Alta — va primero |
| **Comparación** entre docente A y B, resaltando diferencias mayores a ±10 | **Es la que más valor produce:** cada desacuerdo señala un ancla mal escrita | Alta |
| **Congelado** del conjunto como versión atada a `rubric_version` | Cierra el ciclo y habilita al runner | Media |

> **Recordatorio de secuencia:** la herramienta de carga tiene que existir **antes** de que el
> trabajo docente pueda empezar. Es lo que destraba el ítem de plazo más largo del proyecto.

---

# Para la sesión de integración

Una hoja. Lo que hay que salir con respuesta:

- [ ] **A-1** — ¿Tema 07 estricto o servicio de LLM de la plataforma?
- [ ] **A-2** — ¿Quién hace el tutor? *(hoy: nadie)*
- [ ] **A-3** — ¿Quién hace el RAG? *(hoy: nadie)*
- [ ] **A-4** — Fecha de gateway, discovery y bus
- [ ] **B-1** — Tema 05: cómo accedemos a la solución esperada
- [ ] **B-2** — Quién guarda la transcripción **y quién captura la metadata de tiempos**
- [ ] **B-3** — Tema 11: nuestros campos en el contrato de eventos *(antes de que lo cierren)*
- [ ] **B-4** — Tema 02: contrato de consulta de calibración
- [ ] **B-6** — Tema 12: quién es dueño de la config del proveedor LLM

Y por separado, al Product Owner:

- [ ] **C-1** — Golden set: responsable con nombre y fecha límite 🔴
- [ ] **C-2** — Free tier y datos de alumnos: consulta legal 🔴

---

# Parte C — Contenido de ejemplo pendiente de definir

**Varios documentos traen contenido de ejemplo para que se entienda el mecanismo.** Sirve para
arrancar, pero **nada de esto es definitivo**. Esta tabla es el inventario completo: qué es borrador,
dónde está, quién lo define y cuándo.

## La convención

| Marca | Significa |
|---|---|
| 📝 **Borrador** | Escrito por nosotros para que se entienda. Hay que revisarlo y ajustarlo |
| 🧪 **Ejemplo** | Ilustra el formato. El contenido real es otro |
| 🔢 **Estimación** | Un número supuesto. Se reemplaza midiendo |
| ⬜ **Vacío** | Un campo que hay que completar |

## El inventario

### Rúbrica y prompts

| # | Qué | Dónde | Marca | Quién lo define | Cuándo |
|---|---|---|---|---|---|
| E-01 | **Las anclas de las 5 dimensiones** | [13](13-rubrica-y-prompts.md) §3 | 📝 | 🔴 **Dos docentes**, ajustando sobre nuestro borrador | Paso 6 — al comparar puntajes |
| E-02 | Prompt del evaluador | [13](13-rubrica-y-prompts.md) §6 | 📝 | P3 | Paso 5 |
| E-03 | Prompt del tutor + las 3 variantes de riesgo | [13](13-rubrica-y-prompts.md) §7 | 📝 | P5 + P6 | Paso 11 |
| E-04 | Prompt del generador | [13](13-rubrica-y-prompts.md) §8 | 📝 | P2 | Paso 10 |
| ~~E-05~~ | ~~Prompt del moderador~~ | — | ✅ | **Cerrado por ADR-012** | — |
| E-06 | Los schemas de salida | [13](13-rubrica-y-prompts.md) §6-9 | 📝 | Cada dueño de módulo | Con su función |

> ✅ **E-05 quedó sin objeto.** ADR-012 reemplazó el LLM del moderador por un clasificador dedicado, y
> un clasificador no lleva prompt. **El schema de salida sí sigue existiendo** —`moderacion.json`, bajo
> E-06—: lo que desapareció es la plantilla, no el contrato.

> **E-01 es el más importante de la tabla.** Nuestras anclas son un punto de partida para que los
> docentes no arranquen de cero — **no son la rúbrica**. El proceso que las convierte en definitivas
> está en [04](04-funciones-de-ia.md), Parte 3: dos docentes puntúan por separado, y **donde difieren
> más de ±10 el ancla está mal escrita**.

### Golden set

| # | Qué | Dónde | Marca | Quién lo define | Cuándo |
|---|---|---|---|---|---|
| E-07 | La transcripción de ejemplo (#17) | [04](04-funciones-de-ia.md) Parte 3 | 🧪 | Ilustra el formato. Las reales las produce P4 | Paso 6 |
| E-08 | **Los puntajes de esa transcripción** | [04](04-funciones-de-ia.md) Parte 3 | 🧪 | 🔴 **Inventados por nosotros.** Los reales los ponen personas | Paso 6 |
| E-09 | Tamaño del conjunto: 40 (o 10 reducido) | [04](04-funciones-de-ia.md) Parte 3 | 📝 | PO | 🔴 Esta semana |
| E-10 | La distribución de perfiles (8 pasivos, 6 jailbreak…) | [04](04-funciones-de-ia.md) Parte 3 | 📝 | P4 + docentes | Paso 6 |

> ⚠️ **E-08 merece una aclaración explícita cuando se lo muestren a un docente:** los puntajes del
> ejemplo los escribimos nosotros para ilustrar el formato. **Si un docente los toma como referencia,
> la calibración deja de medir nada.**

### Términos y Condiciones

| # | Qué | Dónde | Marca | Quién lo define | Cuándo |
|---|---|---|---|---|---|
| E-11 | Los campos entre `[corchetes]` | [07](07-datos-y-terminos.md) Parte B | ⬜ | PO + institución | Antes del release |
| E-12 | **Anexo A: los proveedores en uso** | [07](07-datos-y-terminos.md) Parte B | ⬜ | Nosotros — **solo nosotros lo sabemos** | Después del paso 7 |
| E-13 | El porcentaje del modificador de XP | [07](07-datos-y-terminos.md) Parte B §4.2 | ⬜ | ADMIN, es PAR-05 | Configuración |
| E-14 | El texto completo | [07](07-datos-y-terminos.md) Parte B | 📝 | 🔴 **Revisión jurídica** | Antes del release |

### Esquema de datos

| # | Qué | Dónde | Marca | Quién lo define | Cuándo |
|---|---|---|---|---|---|
| E-15 | Nombres de campos y tablas | [11](11-glosario-y-metadata.md) Parte B | 📝 | P5 | 🔴 Paso 2 — esta semana |
| E-16 | **Qué es un "mensaje trivial"** (¿menos de cuántos caracteres?) | [11](11-glosario-y-metadata.md) Parte B | ⬜ | P3, ajustando contra el golden set | Paso 4 |
| E-17 | Las 7 colisiones del glosario | [11](11-glosario-y-metadata.md) Parte A | 📝 | 🔴 **La sesión de integración**, no nosotros solos | Esta semana |
| E-18 | Campos del contrato de eventos | [02](02-arquitectura-y-stack.md) Parte 3 | 📝 | 🔴 **Tema 11**, antes de que lo cierren | Esta semana |

### Números y umbrales

| # | Qué | Dónde | Marca | Cómo se define de verdad |
|---|---|---|---|---|
| E-19 | **Modelo por función** | [03](03-modelos-costos-y-contexto.md) §1 | 📝 | 🔴 **La calibración decide**, no la recomendación. Paso 7 |
| E-20 | 10 mensajes de tutor por desafío | [03](03-modelos-costos-y-contexto.md) §4 | 🔢 | Medir la adopción real en el piloto |
| E-21 | 50% de desafíos prácticos | [03](03-modelos-costos-y-contexto.md) §4 | 🔢 | Depende del roadmap que arme el profesor |
| E-22 | Umbrales de RF-IA-22 (15 / 60 / 20) | [08](08-decisiones-y-pendientes.md) D-8 | 📝 | ADMIN, con datos de uso real |
| E-23 | 3 chunks en el retrieval | [03](03-modelos-costos-y-contexto.md) §6 | 📝 | Las 30 preguntas etiquetadas: comparar 3 vs 5 vs 8 |
| E-24 | Piso de similitud del retrieval | [05](05-seguridad.md) | ⬜ | Mirando la tasa de "no encontrado" |
| E-25 | Tamaño de chunk 500-800 tokens | [04](04-funciones-de-ia.md) Parte 1 | 📝 | Probar con el material real |
| E-26 | 20 min de drenado en el pico | [06](06-operacion-e-ingenieria.md) Parte 2 | 🔢 | La prueba de carga del DoD 10 |

### Diseño de pantallas

| # | Qué | Dónde | Marca | Quién lo define |
|---|---|---|---|---|
| E-27 | Las 7 decisiones de la pantalla del golden set | Mockup publicado | 📝 | El equipo, puertas adentro. **Ninguna está tomada** |
| E-28 | Los 7 estados del componente del tutor | [06](06-operacion-e-ingenieria.md) Parte 4 | 📝 | P6 |
| E-29 | Los mensajes de error al usuario | [06](06-operacion-e-ingenieria.md) Parte 4 | 📝 | P6 + PO |

## Lo que hay que definir esta semana

De los 29 ítems, **cinco no pueden esperar**:

| # | Qué | Por qué ahora |
|---|---|---|
| **E-15** | Nombres de campos del esquema de metadata | Se empieza a capturar ya, y lo que no se captura se pierde |
| **E-17** | Las colisiones del glosario | Va a la sesión de integración |
| **E-18** | Nuestros campos en el contrato de eventos | Después es renegociar con cinco equipos |
| **E-09** | Tamaño del golden set y responsable | Es el plazo más largo del proyecto |
| **E-16** | Qué cuenta como mensaje trivial | Bloquea el cálculo de features |

**Los otros 24 se resuelven a medida que se construye cada pieza.**
