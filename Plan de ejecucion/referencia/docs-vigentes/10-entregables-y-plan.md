# 10 — Qué entregamos y cómo lo construimos

> **Calendario histórico.** El reparto y la secuencia de este documento se conservan como
> antecedente. El plan vigente es [23 · Construcción del producto LLM](23-plan-construccion-producto-llm.md):
> 10 integrantes, tres fases, 19 sprints quincenales y 208 horas-persona iniciales para entregables
> por sprint, después de reuniones y reserva. Las funciones de F2/F3 entran únicamente en sus fases.

> **Punto de partida actualizado.** Antes de estimar o repartir pasos, usar la matriz [21](21-matriz-trazabilidad-llm.md) y los contratos v1. Esta planificación previa mantiene información de contexto, pero no habilita trabajo de Fase 2/Fase 3 ni endpoints `/ai/*`.

> El inventario completo del aporte del equipo, y el plan de 14 pasos para seis personas.

---

# Parte 1 — Qué tenemos que entregar


> El inventario completo de lo que el equipo produce. Todo lo demás de esta carpeta explica **cómo**;
> este documento dice **qué**.
>
> Estado al **2026-08-30**, con correcciones puntuales posteriores. **Lo que está decidido lo dice
> la lista de ADR** de [08](08-decisiones-y-pendientes.md), Parte A — no este documento.

## 1. En una frase

**Somos el Tema 07 — Evaluación LLM. Construimos el servicio que le pone nota a *cómo* un alumno usó
la IA, y demostramos que esa nota es confiable.**

Y como consecuencia de tener la salvaguarda anti-fuga, quedamos también en el camino del tutor — y
con él, del RAG y del resto de las funciones de IA.

## 2. Lo que entregamos: siete bloques

### Bloque 1 — El servicio

| Entregable | Qué es | Confirmado |
|---|---|---|
| **`ms-evaluacion-llm`** | El microservicio: se registra en el discovery, habla por el API Gateway, publica en el bus | ✅ |
| **`worker`** | Misma imagen, distinto comando. Drena la cola de trabajo diferido | ✅ |
| **Base propia** | Postgres + pgvector, exclusiva nuestra | ✅ |
| **`py-tools`** *(si hace falta)* | Sidecar interno para embeddings y AST. No es un microservicio | 🟡 Incremental |

### Bloque 2 — El AI Gateway

**Toda llamada a un modelo de toda la plataforma pasa por acá.**

| Entregable | Requerimiento |
|---|---|
| Registro `función → proveedor + modelo`, editable por ADMIN, **nunca en el código** | RF-IA-23/24/35 |
| Adapters de proveedor (Anthropic, Gemini, OpenAI, local) | RF-IA-11/26 |
| Cuotas por usuario, por desafío y por día | RF-IA-22 |
| Escalera de degradación y fallback entre proveedores | RF-IA-27 |
| Validación de salida contra schema | RF-IA-13/16 |
| Log de cada llamada: modelo, versión, tokens, costo, latencia, incidentes | RF-IA-02/25/33 |

### Bloque 3 — Las funciones de IA

| Función | Estado | Nota |
|---|---|---|
| **Evaluador de uso de IA** | ✅ Confirmado | El núcleo del tema |
| **Salvaguarda anti-fuga** | ✅ Confirmado | Nos pone en el camino del tutor |
| **Tutor** | 🟢 De hecho nuestro | Hacemos el componente Angular |
| **RAG** | 🟡 A confirmar | Nadie más lo tiene, y el tutor lo necesita |
| **Generador de evaluaciones** | 🟡 A confirmar | Figura en Tema 04/03 como opcional |
| **Corrector de respuestas abiertas** | 🟡 A confirmar | **Solo abiertas** — el resto se corrige con código |
| **Moderador de chat** | 🟡 A confirmar | Figura en Tema 11. **El chat es Fase 2**: se diseña ahora, se construye cuando el chat exista |
| **Agente `@mención`** | 🔵 Fase 3 | RF-CHT-05. Diseñado en [04](04-funciones-de-ia.md) Parte 4, fuera de este cuatrimestre |

### Bloque 4 — Los artefactos académicos

**No son código, y son lo que más cuesta.**

| Entregable | Quién lo produce | Costo |
|---|---|---|
| **Rúbrica versionada** — 5 dimensiones, pesos 30/25/20/15/10, anclas de bajo/medio/alto. **Declarativa y legible por un docente** | Nosotros | Días |
| **Golden set** — transcripciones puntuadas **por personas** | 🔴 **Docentes, no nosotros** | ~26 h · o ~4 h en versión reducida |
| **Runner de calibración** — puntúa a ciegas, calcula desviación contra PAR-14 | Nosotros | Días |
| **Detección de deriva** — recalibración mensual y ante cambio de versión | Nosotros | RF-IA-32 |

### Bloque 5 — Las pantallas

**Van en el monolito Angular compartido. Son nuestras porque nadie más entiende para qué existen.**

| Pantalla | Por qué es nuestra |
|---|---|
| **Chat del tutor** (componente reutilizable) | Lo consumen los otros equipos |
| **Carga y puntuación del golden set** | 🔴 **Destraba el ítem de plazo más largo del proyecto** |
| **Comparación entre docente A y B** | Resalta diferencias > ±10. Es donde se mejora la rúbrica |
| **Desglose del score para el alumno** | RF-IA-16 |
| **Flujo de apelación** | RF-IA-18 |
| **Revisión del parcial generado**, con el fragmento fuente al lado | Gate humano obligatorio |
| **Dashboard de incidentes** de jailbreak y moderación | RF-IA-10 |

### Bloque 6 — Los conjuntos de prueba

**Fáciles de postergar hasta que es tarde. Sin ellos no podés cambiar un prompt sin miedo — y vas a
cambiar prompts cincuenta veces.**

| Conjunto | Tamaño | Métrica |
|---|---|---|
| Golden set | 40 (o 10 en demo) | Desviación PAR-14: ±5 / ±10 |
| Corpus de ataques al tutor | 30 jailbreak + 20 pedidos de solución | **Cero fugas** |
| Mensajes etiquetados para el moderador | 100 | > 90% en severidad media/alta |
| Preguntas etiquetadas del RAG | 30 | recall@3 > 85% |
| Preguntas generadas revisadas | 20 | > 70% usables |
| Respuestas corregidas a mano | 30 | Coincidencia con el corrector |

> **El corpus de ataques tiene una fuente gratis:** cada incidente real detectado en producción
> (RF-IA-10) es un caso de test. Conectalos desde el principio.

### Bloque 7 — Los contratos y la documentación

| Entregable | Para quién |
|---|---|
| **OpenAPI de los endpoints**, escrito antes de implementar | Otros equipos, para arrancar contra un mock |
| **Los campos que necesitamos en el contrato de eventos** | 🔴 Tema 11, **antes de que lo cierren** |
| **Endpoint de estado de calibración** | Tema 02 — sin esto no puede activar cursos |
| **Contrato de lectura** (calibración, deriva, costo por curso) | Tema 12 — Backoffice |
| **El glosario** | 🔴 Todos. *"Evaluación"* significa cosas distintas en tres temas |
| **La lista de proveedores de LLM en uso** | Para el Anexo A de los T&C (RF-NFR-09). **Solo nosotros la sabemos** |

## 3. Lo que NO entregamos

Vale tanto como lo anterior, para que nadie asuma que lo hacemos.

| No es nuestro | De quién |
|---|---|
| Usuarios, roles, permisos, auth, 2FA | Tema 01 |
| Cursos, matrícula, ciclo de vida del curso | Tema 02 |
| Motor de desafíos, entregas, estados | Tema 03 |
| **Aplicar el XP** — nosotros damos el score, ellos lo aplican | Tema 03 / 10 |
| Gamificación: monedas, vidas, insignias, ranking | Tema 08 / 09 / 10 |
| Sandbox de ejecución de código | Tema 06 |
| Control de originalidad entre alumnos (el *otro* 70%) | Tema 05 |
| API Gateway, Service Discovery, bus de eventos | Infraestructura compartida |
| **Producir el contenido del golden set** | 🔴 Docentes |

## 4. El compromiso con cada equipo

Lo que otros no pueden terminar sin nosotros:

| Equipo | Qué les debemos | Si no lo entregamos |
|---|---|---|
| **Tema 02** | Endpoint de estado de calibración | 🔴 **No pueden activar ningún curso** |
| **Tema 03 / 10** | El score de uso de IA con su desglose | No pueden aplicar el modificador de XP |
| **Tema 11** | Nuestros campos en el contrato de eventos | Hay que renegociar con cinco equipos |
| **Tema 12** | Contrato de lectura: calibración, deriva, costo | *"No tienen nada demostrable"* |
| **Front** | El componente del chat del tutor | Los desafíos prácticos no tienen asistencia |

Y lo que necesitamos de ellos:

| Equipo | Qué necesitamos | Estado |
|---|---|---|
| **Tema 05** | La solución esperada de cada desafío | 🔴 Abierto |
| **Tema 03** | El evento `intento_cerrado` que dispara la evaluación | Por acordar |
| **Tema 02** | El evento `curso_archivado` | Por acordar |
| **Tema 12** | La configuración del proveedor LLM | Por acordar quién es dueño |
| **Product Owner** | Golden set con responsable y fecha · consulta legal del free tier | 🔴 Abierto |

## 5. Cinco criterios de release dependen de nosotros

Y **ninguno lo podemos completar solos.**

| DoD | Qué exige | Qué falta que no es nuestro |
|---|---|---|
| **7** | Tutor respeta las reglas y el evaluador emite score **con desglose visible y apelación** | Las pantallas |
| **7b** | Golden set existe y **está puntuado por docentes** | Las horas docentes |
| **7c** | Cada curso tiene calibración aprobada y **el bloqueo draft→activo verificado** | Integración con Tema 02 |
| **11** | Degradación verificada ante caída del proveedor | Que el backend acepte la entrega igual |
| **13** | Auditoría sobre overrides de score | Modelo de auditoría del backend |

## 6. Los números del compromiso

| | |
|---|---|
| Personas | **6** |
| Módulos | **8** |
| Pasos hasta el núcleo funcionando | **14** (Paso 0 a Paso 13) |
| Semanas de la demo local | **4** |
| Costo de la demo | **USD 0** — free tier con datos sintéticos |
| Costo de un cuatrimestre real | **USD 5 a 22** |
| Trabajo docente necesario | **~26 h** (o ~4 h reducido) |
| Escala objetivo | 120 usuarios, 120 sesiones concurrentes |

## 7. Lo que hay que hacer esta semana

| # | Qué | Por qué ahora |
|---|---|---|
| 1 | **El glosario** — medio día, todo el equipo | Sin él, la integración acuerda cosas distintas creyendo que acordó lo mismo |
| 2 | **Definir el esquema de metadata y empezar a capturarla** | 🔴 **Es lo único que se pierde para siempre si se posterga.** Sin tiempos entre mensajes y ediciones de código, la dimensión que pesa 30% queda inevaluable |
| 3 | **Pedir al PO responsable y fecha para el golden set** | Es el plazo más largo del proyecto y no depende de ningún equipo técnico |
| 4 | **Escalar la consulta legal del free tier** | Las consultas legales tardan, y define el modelo de costos y los T&C |
| 5 | **Pedirle al Tema 11 nuestros campos en el contrato de eventos** | Después es renegociar con cinco equipos |
| 6 | ~~**Preguntar si la cátedra permite Python**~~ — **cerrado por ADR-005: el servicio va en Java Spring Boot** | Queda el caso del borde: si hacen falta embeddings locales, ADR-005 admite un **componente interno** Python que no es un microservicio |

**Cinco siguen abiertos y son de esta semana; el sexto ya está cerrado. Los cinco que quedan son
conversaciones, no código.**

## 8. La frase para la defensa

Si hay que resumir el aporte del equipo en una oración:

> **Construimos la capa de IA de la plataforma: el servicio por el que pasa toda llamada a un modelo,
> el evaluador que puntúa cómo cada alumno usó al tutor, y el mecanismo de calibración que demuestra
> —con un número, contra transcripciones puntuadas por docentes— que ese puntaje se parece al que
> pondría una persona.**

Y si preguntan qué fue lo difícil:

> **Que una nota puesta por una IA tiene que ser defendible.** Por eso la rúbrica es un artefacto
> versionado y no un prompt; por eso la mitad del puntaje se calcula con código determinístico en vez
> de con el modelo; por eso cada evaluación guarda con qué modelo y qué versión de rúbrica se hizo; y
> por eso ningún curso arranca hasta que el modelo demuestre, sobre un conjunto puntuado por humanos,
> que evalúa como un humano.


---

# Parte 2 — Plan de trabajo


> Cómo repartir el Tema 07 entre 6 personas, qué construir primero, y el plan de la demo local.
> Estado: **2026-08-30**.

## 1. El principio del reparto

**Cada persona arranca con una pieza que no depende de nadie, y su segunda tarea reusa lo que
construyó en la primera.**

Eso evita las dos cosas que matan un equipo de 6 en un TP: que cinco esperen a que uno termine, y
que nadie pueda tocar el código de otro porque no lo entiende.

## 2. Los módulos

El servicio se parte en 8 módulos con fronteras claras. **Cada uno es una carpeta con una interfaz
explícita**, no un microservicio.

| Módulo | Qué hace | ¿Depende de algo? |
|---|---|---|
| **M1 · Gateway** | Registro modelo→función, adapters de proveedor, reintentos, fallback, cuotas, log | No |
| **M2 · RAG** | Ingesta de PDF, chunking, embeddings, retrieval | No |
| **M3 · Evaluador** | Rúbrica versionada, prompt, scoring de la transcripción | M1 |
| **M4 · Calibración** | Golden set, runner, comparación con PAR-14, deriva | M1 + M3 |
| **M5 · Guardarraíles y moderación** | Filtro de entrada, salvaguarda anti-fuga con AST, y el moderador de chat (capa clásica + clasificador, ADR-012) | M1 |
| **M6 · Tutor** | Servicio del tutor + componente Angular | M1 + M2 + M5 |
| **M7 · Generador y corrector** | Blueprint, generación por slot, validación, corrección | M1 + M2 |
| **M8 · Plataforma** | Docker, API, contratos, cola, base, eventos | No |

> **El moderador de chat vive en M5 y no es un módulo aparte.** Es un clasificador de texto corto sin
> contexto: comparte la capa clásica con el filtro de entrada y es del mismo
> responsable. **No entra en las cuatro semanas de la demo** —el chat es Fase 2 del PRD— pero el
> contrato sí conviene entregarlo temprano. El agente `@mención` (RF-CHT-05) es Fase 3 y, cuando
> llegue, va en M6 junto al tutor: es un agente conversacional, no un filtro. Ver
> [04](04-funciones-de-ia.md) Parte 4.

## 3. El reparto

| Persona | Primera tarea | Segunda tarea | Por qué encaja |
|---|---|---|---|
| **P1** | **M1 · Gateway** | Observabilidad: costo, latencia, aciertos de caché | Es la base de la que todos dependen. **Tiene que arrancar el día 1** |
| **P2** | **M2 · RAG** | **M7 · Generador** | El generador es el mayor consumidor del RAG. Quien lo construyó sabe cómo consultarlo |
| **P3** | **M3 · Evaluador** | **M7 · Corrector** | El corrector es el mismo patrón de juez con otra rúbrica. Se reusa el 80% |
| **P4** | **M4 · Calibración y golden set** | Perseguir a los docentes 😅 | Es el de **mayor riesgo de calendario**. Necesita a alguien dedicado y que empuje afuera del equipo |
| **P5** | **M5 · Guardarraíles** | Comparación por AST multi-lenguaje, y el moderador de chat cuando el chat exista | Es la parte más técnica y la más aislada. Se puede probar sin el resto |
| **P6** | **M8 · Plataforma** | **M6 · Tutor** + componente Angular | Arranca armando el esqueleto que todos usan, y sigue con lo que necesita Angular |

### Por qué P1 y P6 arrancan juntos y primero

**P1 (gateway) y P6 (plataforma)** son las dos piezas que todos los demás necesitan para poder
correr algo. En la primera semana esos dos tienen que entregar:

- Un `docker compose up` que levante el servicio, Postgres y Redis.
- Una función `llamar_modelo(funcion, prompt) -> respuesta` que ande contra un proveedor real.

**Con eso, los otros cuatro pueden empezar en paralelo.** Sin eso, todos esperan.

### La regla que evita el caos

> **Nadie llama a un proveedor de LLM directamente. Todos pasan por la función de P1.**

Si cada uno arma su propia llamada al proveedor, terminás con 6 formas distintas de manejar errores,
6 formatos de log y ninguna forma de cambiar de modelo. **Es el mismo argumento del AI Gateway, pero
adentro del equipo.**

## 4. La demo local

**Objetivo: entender el mecanismo y tener algo que mostrar. No es el producto.**

### Qué NO va en la demo

Sacarlo del alcance ahorra semanas:

| Fuera de la demo | Por qué |
|---|---|
| API Gateway, Service Discovery, bus de eventos | Es infraestructura de plataforma, de otros equipos |
| Autenticación, roles, 2FA | No es tuyo |
| Cola y workers | Que bloquee y tarde. Agregala cuando un timeout te moleste |
| El componente Angular | Usá el Swagger UI que genera springdoc |
| Fallback entre proveedores | Un proveedor alcanza para demostrar |
| Prompt caching y Batch | Optimización. La demo no tiene volumen |
| Postgres | Para la demo alcanza SQLite o incluso archivos. Pero si vas a usar pgvector, arrancá con Postgres |

### Las cuatro semanas

| Semana | Entregable | Quién | Cómo se demuestra |
|---|---|---|---|
| **1** | `docker compose up` levanta el servicio + una llamada real a un modelo responde | P1, P6 | Un endpoint `/ping-modelo` que devuelve texto generado |
| **2** | Ingesta de un PDF real → chunks con metadata → embeddings → búsqueda | P2 | Preguntás "¿qué dice sobre punteros?" y devuelve el fragmento **con número de página** |
| **3** | Generar 5 preguntas desde ese PDF, con salida estructurada y validación | P2, P3 | JSON con 5 preguntas, cada una **con su fragmento fuente al lado** |
| **4** | Corregir una respuesta + evaluar una transcripción de ejemplo | P3, P5 | Desglose por dimensión con justificación y confianza |

**En paralelo, desde la semana 1:** P4 arma 10 transcripciones sintéticas y consigue que **una sola
persona** las puntúe. Es el ensayo del golden set en chico.

### El costo de la demo

**USD 0 con el free tier de Gemini**, y sin ninguna objeción legal porque son datos sintéticos.

### La prueba que hace entender la demo

Cuando la muestres, la pantalla que convence es **la pregunta generada al lado del fragmento del
apunte del que salió**. En tres segundos, quien la mira entiende qué es RAG y deja de preguntar si
"se entrenó" el modelo.

## 5. Qué falta definir — actualizado

Cambios respecto de [08](08-decisiones-y-pendientes.md) con la información nueva:

| # | Estado | Detalle |
|---|---|---|
| **A-2 — ¿quién hace el tutor?** | 🟢 **Se resuelve solo** | Si hacen el componente Angular del chat, el tutor es de ustedes. Nadie más va a servirlo |
| **A-1 — alcance** | 🟡 **Se inclina al amplio** | El componente Angular + la salvaguarda anti-fuga los pone en el camino del tutor. Falta confirmarlo formalmente |
| **A-3 — ¿quién hace el RAG?** | 🔴 **Sigue abierto** | Si el tutor es suyo, el RAG casi seguro también. Pero hay que decirlo |

### Nuevas preguntas que abre el componente Angular

| # | Pregunta | Recomendación |
|---|---|---|
| ~~**N-1**~~ | ~~¿Cómo consumen los otros equipos el componente?~~ | ✅ **Resuelto: el front es un monolito compartido.** El componente es una carpeta más del repo. Sin librería npm, sin versionado propio. Lo que sí conviene saber: **quién es dueño de ese repo y cómo se resuelven los conflictos** cuando 12 equipos mergean en la misma app |
| **N-2** | ¿El componente llama al gateway o directo a su servicio? | **Al API Gateway.** Es regla no negociable de la cátedra, y además es la única forma de que el token se valide en un lugar |
| **N-3** | ¿Quién mantiene el componente cuando cambie? | Ustedes. Es un costo recurrente que conviene declarar ahora |
| **N-4** | ¿El componente maneja el buffer de RF-IA-20? | Sí. Como no puede haber streaming en prácticos, el componente muestra "pensando..." hasta que la respuesta pasa el guardarraíl |

## 6. Lo que sigue sin respuesta y bloquea

| # | Qué | Quién decide |
|---|---|---|
| 🔴 **C-1** | Golden set: responsable con nombre y fecha | Product Owner |
| 🔴 **B-1** | Cómo accedemos a la solución esperada del Tema 05 | Sesión de integración |
| 🔴 **B-3** | Nuestros campos en el contrato de eventos del Tema 11 | Sesión de integración, **antes de que lo cierren** |
| 🟡 **B-2** | Quién guarda la transcripción y **quién captura la metadata de tiempos** | Sesión de integración |

> **Sobre B-2 con la información nueva:** si el tutor es de ustedes, la transcripción también, y
> **capturan la metadata de tiempos ustedes mismos**. El problema desaparece. Es un argumento más
> para pelear el alcance amplio: **es más fácil hacer el tutor que coordinar con quien lo haga.**

## 7. Una advertencia sobre el tamaño del equipo

Son 6 personas y el alcance amplio son 8 módulos. **Es factible, pero solo si el reparto se respeta
y la interfaz de cada módulo se acuerda antes de escribir código adentro.**

El modo de falla clásico de un equipo de 6 en un TP: tres personas terminan tocando el mismo archivo,
dos esperan a que alguien termine, y una hace el 60% del trabajo.

Tres reglas que lo evitan:

1. **La interfaz de cada módulo se define primero**, aunque el cuerpo devuelva un valor fijo. Así los
   demás pueden avanzar contra algo.
2. **Un dueño por módulo.** Se puede ayudar, pero hay un responsable.
3. **P1 entrega la función de llamar al modelo en la semana 1, sí o sí.** Es la dependencia de todos.

## 8. El paso a paso concreto

Catorce pasos, del 0 al 13, en orden. Los primeros cuatro no requieren que nadie externo defina
nada, y el último no entra en la demo — está para que la frontera exista.

> 💰 **Con qué modelo probar cada uno de estos pasos sin pagar nada** está en
> [03](03-modelos-costos-y-contexto.md) §8. Resumen: solo los pasos 1, 5, 10 y 12 llaman a un modelo,
> y los cuatro entran en el free tier de Gemini. El paso 7 es el único donde conviene gastar
> centavos a propósito.

### Paso 0 — El glosario (medio día, y evita semanas de confusión)

Antes de escribir código, **acordar qué significa cada palabra**. Y hay una urgencia real: la palabra
*"evaluación"* significa cosas distintas en tres temas.

| Término | Qué es acá | Con qué se confunde |
|---|---|---|
| **Corrección** | Decidir si una respuesta está bien | Con "evaluación" |
| **Score de uso de IA** | Puntaje 0-100 sobre **cómo** el alumno usó al tutor | Con la nota del desafío |
| **Calibración** | Verificar que un modelo puntúa como un docente | Con "entrenamiento" |
| **Golden set** | Transcripciones puntuadas **por personas**, usadas como vara | Con datos de entrenamiento |
| **Rúbrica** | Las 5 dimensiones con pesos y anclas | Con el prompt |
| **Transcripción** | La conversación completa + metadata de tiempos y ediciones | Con "el chat" |

**Llevalo a la sesión de integración.** Si el Tema 03 dice "evaluación" pensando en el cierre del
desafío y ustedes piensan en el score de IA, van a acordar cosas distintas creyendo que acordaron lo
mismo.

### Paso 1 — El esqueleto *(P1 + P6, semana 1)*

- `docker compose up` levanta el servicio, Postgres y Redis.
- Una función `llamar_modelo(funcion, prompt) → respuesta` que anda contra Gemini free tier.
- La **tabla `funcion → proveedor + modelo`**, aunque tenga una sola fila.

**Criterio de terminado:** un endpoint devuelve texto generado por un modelo real, y cambiar de
modelo es editar una fila.

### Paso 2 — 🔴 Capturar la metadata *(urgente, en paralelo)*

**Es lo único que se pierde para siempre si no se hace desde el principio.**

Por cada mensaje: `timestamp`, `tiempo_desde_el_anterior`, `ediciones_de_codigo_desde_el_anterior`,
`rol`, `contenido`.

Sin eso, la dimensión que pesa 30% —autonomía: *¿intentó antes de preguntar?*— **queda inevaluable**.
No hay forma de reconstruir después cuánto tardó alguien entre dos mensajes.

> Si el tutor termina siendo de otro equipo, **pediles esto explícitamente**. No lo van a capturar
> solos.

### Paso 3 — La rúbrica como artefacto *(P3)*

Un archivo declarativo versionado —no un prompt— con las 5 dimensiones, sus pesos y sus anclas de
bajo/medio/alto. **Legible por un docente**, porque RF-IA-29 dice "artefacto declarativo".

### Paso 4 — Los features determinísticos *(P3 + P5)*

Calcular sin LLM: cantidad de mensajes, mensajes triviales, tiempo antes del primer mensaje,
ediciones previas, incidentes del guardarraíl.

**Guardarlos con cada transcripción.** Sirven para tres cosas: se los pasás al modelo como evidencia,
después reemplazan dimensiones enteras, y son la prueba en una apelación. Ver
[04](04-funciones-de-ia.md) §1b.

### Paso 5 — Evaluar una transcripción *(P3, semana 2)*

Rúbrica + transcripción + features → el modelo devuelve las 5 dimensiones con justificación y
confianza, validado contra schema.

**Criterio de terminado:** le pasás una conversación y devuelve un desglose que a un humano le parece
razonable.

### Paso 6 — El golden set chico *(P4, semana 2-3)*

**10 transcripciones sintéticas**, puntuadas por **dos integrantes del equipo por separado**.

Y el paso que la gente saltea: **comparar los dos puntajes y discutir dónde difieren más de ±10**.
Ahí se arregla la rúbrica, no el puntaje. Es la parte que más mejora el sistema.

### Paso 7 — El runner de calibración *(P4)*

El modelo puntúa las 10 a ciegas → desviación por dimensión → ¿entra en PAR-14?

**Corré los tres modelos candidatos** (Flash-Lite, Haiku, Sonnet) en la misma pasada. Cuesta centavos
y te dice cuál es el más barato que pasa.

### Paso 8 — El endpoint de estado *(P6)*

`GET /ai/calibracion/{curso_cohorte_id}`

```
{ aprobada: bool, desviacion_promedio, desviacion_maxima_dimension,
  model_id, model_version, rubric_version, golden_set_version, fecha }
```

**Son los ocho campos del contrato, no cuatro.** La forma exacta la fija
[02](02-arquitectura-y-stack.md), Parte 3, endpoint 5: implementarla distinta acá es publicar dos
contratos para el mismo endpoint.

**Entregalo temprano aunque devuelva un mock:** el Tema 02 no puede activar cursos sin esto y es la
dependencia más visible que otros tienen sobre ustedes.

### Paso 9 — El RAG *(P2, semanas 2-3)*

PDF → chunks con metadata (`curso_cohorte_id`, unidad, tema, página, tipo) → embeddings locales →
pgvector → búsqueda que devuelve el fragmento **con su página**.

**La prueba que casi nadie hace:** preguntar algo que **no está** en el apunte y verificar que
devuelve baja similitud en vez de inventar. Eso es lo que después implementa el perímetro temático.

### Paso 10 — El generador *(P2, semana 3)*

Blueprint determinístico → retrieval por cobertura → una llamada por pregunta → validación → JSON.

**La pantalla que hace entender la demo:** la pregunta al lado del fragmento del que salió.

### Paso 11 — Guardarraíles y tutor *(P5 + P6, semana 4+)*

Filtro de entrada, salvaguarda anti-fuga por AST, y recién después el tutor —que es el más difícil
porque junta latencia, RF-IA-04, los tres niveles de RF-IA-19 y el buffer de RF-IA-20.

### Paso 12 — El corrector *(P3)*

**Solo para respuestas abiertas.** Multiple choice, V/F, ordenar, emparejar y tests se corrigen con
código. Ver [04](04-funciones-de-ia.md) §1c.

### Paso 13 — El moderador *(P5, cuando exista el chat)*

**No va en las cuatro semanas de la demo.** El chat es Fase 2 del PRD; el moderador no tiene qué
moderar todavía. Pero hay una parte que sí conviene entregar temprano y una parte que no:

| Ahora | Cuando exista el chat |
|---|---|
| **El contrato**: `moderar(mensaje)` devuelve `categorias`, `severidad`, `confianza` y `origen` | La calibración contra mensajes reales, y cuánto resuelve cada capa según `origen` |
| La capa clásica, que es código puro y se prueba sin proveedor | El registro de incidentes y el evento de severidad alta |
| Los 100 mensajes etiquetados a mano — es una tarde, no un hito de calendario | La marca de retención de RF-CHT-14 |

**Por qué el contrato ahora:** el Tema 11 está diseñando el chat. Si no sabe qué puede pedirnos ni
cuánto tarda, lo va a diseñar asumiendo algo, y esa asunción va a estar mal. Es la misma lógica del
Paso 8 con el endpoint de calibración.

**⚠️ El moderador ya no sirve como primer ejercicio con un modelo:** ADR-012 lo dejó sin LLM. Sigue
siendo la función más simple del proyecto —sin contexto, sin historial, sin RAG, sin rúbrica, y su
salida es un JSON de cuatro campos—, pero por eso mismo dejó de ser práctica sobre M1: la capa
clásica es código puro y el clasificador no lleva prompt. Como ejercicio de calentamiento sobre M1
vale mucho; como prioridad de entrega, no compite con el evaluador.

**Criterio de terminado:** le pasás 100 mensajes etiquetados y acierta más del 90% en severidad media
y alta, con la capa clásica resolviendo la mayoría sin salir a la red.

> El agente `@mención` (RF-CHT-05) **no tiene paso**. Es Fase 3, está diseñado en
> [04](04-funciones-de-ia.md) Parte 4 y ahí se queda hasta que alguien lo priorice.

## 9. ¿Hace falta DDD?

**Lo que veníamos haciendo es modelado de dominio, pero no DDD formal.** Y para 6 personas en un
cuatrimestre, el DDD completo —agregados, repositorios, fábricas, la ceremonia entera— es más costo
que beneficio.

**Pero tres piezas ya las están usando sin nombrarlas, y conviene hacerlas explícitas:**

| Pieza de DDD | Ya la tienen | Qué hacer |
|---|---|---|
| **Contextos delimitados** | **Los temas de la cátedra son exactamente eso.** Y *"cada servicio dueño exclusivo de su base"* es el patrón textual | Nada. Ya está impuesto |
| **Eventos de dominio** | El bus de eventos del Tema 11 | Nada. Ya está |
| **Lenguaje ubicuo** | ❌ **No lo tienen** | 🔴 **El glosario del paso 0.** Es lo único de DDD que urge |
| **Agregados** | Implícito | Vale la pena modelar uno: la **evaluación con sus overrides** |

### El único agregado que vale modelar bien

```
Evaluación  (raíz del agregado)
├── dimensiones[]           ← puntaje + justificación, no existen sin la evaluación
├── overrides[]             ← append-only, nunca pisan el original
└── metadata de versión     ← rubric_version, model_id, model_version
```

**La regla que protege:** nadie modifica una dimensión por afuera de su evaluación, y **un override
agrega un registro en vez de pisar el anterior**. Eso es lo que hace que una apelación de hace ocho
meses siga siendo auditable — y es exactamente lo que un agregado bien definido garantiza.

### Veredicto

**DDD ligero: glosario + ese agregado + los eventos que ya existen.** Saltear el resto.

Y una razón práctica: en la defensa del TP, poder explicar *"modelamos la evaluación como agregado
con overrides append-only para que una apelación sea auditable"* demuestra más criterio que haber
aplicado la ceremonia completa.

## 10. Lo que hay que definir antes de empezar

De los catorce pasos, **solo tres dependen de definiciones externas:**

| Paso | Depende de | Estado |
|---|---|---|
| 9 · RAG | ¿Es nuestro? (alcance A-1) | 🟡 Se inclina a sí |
| 11 · Salvaguarda | Cómo accedemos a la solución esperada del Tema 05 | 🔴 Abierto |
| 11 · Tutor | ¿Es nuestro? | 🟢 De hecho sí, hacemos el componente |

**Los pasos 0 a 8 no dependen de nadie.** Son ~5 semanas de trabajo para 6 personas y cubren el
núcleo entero del Tema 07.

> **Con qué empezar, en una línea:** el **paso 0** (glosario, medio día) y el **paso 2** (capturar la
> metadata), porque es lo único que se pierde para siempre si se posterga.
