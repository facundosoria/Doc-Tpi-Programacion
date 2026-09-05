# 01 — El problema, el alcance y el equipo

> **Alineación 2026-09-04.** El alcance operativo se interpreta con [00](00-fuentes-de-verdad-y-convenciones.md): MVP de evaluación/tutor seguro; moderación en Fase 2; RAG, ingesta y personalización en Fase 3. Las listas históricas de este documento no amplían ese alcance.

> Este documento existe porque el equipo se hace cargo **solo del LLM y la parte de IA**. Eso cambia
> qué decisiones son tuyas, cuáles hay que negociar, y —sobre todo— **qué cosas van a quedar en el
> medio y nadie va a construir si no las reclamás ahora.**

## 1. El hallazgo principal, primero

**5 de los 16 criterios de release del MVP dependen de la IA. Ninguno lo puede completar el equipo
de IA solo.**

| Criterio (Sección 19 del PRD) | Qué exige | Qué falta que no es tuyo |
|---|---|---|
| **DoD 7** | El tutor respeta las reglas por nivel de riesgo y el evaluador emite score **con desglose visible y vía de apelación** | Pantalla del desglose (RF-IA-16) y flujo de apelación (RF-IA-18) |
| **DoD 7b** | El golden set base existe y **está puntuado por docentes** | Herramienta para que los docentes carguen y puntúen transcripciones |
| **DoD 7c** | Cada curso tiene calibración aprobada y **el bloqueo draft→activo está verificado** | Integración con el ciclo de vida del curso, que es del otro equipo |
| **DoD 11** | Degradación controlada verificada ante caída del proveedor | Que el backend acepte la entrega y difiera el score |
| **DoD 13** | Auditoría sobre overrides de score académico | Modelo de auditoría del backend |

**Tu servicio puede estar perfecto y el MVP igual no sale.** Esto hay que plantearlo en la próxima
reunión de equipos, no en marzo.

## 2. Qué es tuyo, qué no

```mermaid
flowchart TB
    subgraph tuyo["EQUIPO DE IA - lo que construis y decidis"]
        T1["ai-service completo"]
        T2["AI Gateway: modelos, guardarrailes,<br/>cuotas, fallback, logging"]
        T3["Las 5 funciones de IA"]
        T4["RAG: ingesta, chunking,<br/>embeddings, retrieval"]
        T5["Rubricas y prompts versionados"]
        T6["Runner de calibracion"]
        T7["Workers asincronicos"]
    end

    subgraph negociar["ZONA DE NEGOCIACION - decidir JUNTOS"]
        N1["Contrato de la API interna"]
        N2["Quien es dueño de la cola"]
        N3["Quien persiste los resultados"]
        N4["Formato del trace_id"]
        N5["Quien construye las pantallas de IA"]
    end

    subgraph otros["OTROS EQUIPOS - lo que dependes de ellos"]
        O1["Backend de negocio y su stack"]
        O2["Gamificacion, XP, ranking"]
        O3["Auth, roles, 2FA"]
        O4["TODAS las pantallas"]
        O5["Ciclo de vida del curso"]
        O6["Base de datos academica"]
    end

    tuyo <--> negociar <--> otros
```

## 2b. Alcance confirmado (2026-08-29)

### Lo que construye el equipo de IA

| # | Pieza | Requerimientos | Nota |
|---|---|---|---|
| 0 | **RAG** — ingesta del material de la materia, chunking, embeddings, retrieval | RF-IA-08 | Base de todo. Ver [04](04-funciones-de-ia.md) |
| 1 | **Tutor en desafío** | RF-IA-01/04/06/07/19/20 | El más difícil. Sincrónico |
| 2 | **Generador de evaluaciones** | RF-DES-05 | Asincrónico, con gate humano |
| 3 | **Evaluador de uso de IA** | RF-IA-12 a 18, 25, 28 a 36 | **Emite los números.** No asigna XP. Ver §2c |
| 4 | **Corrector de respuestas** | (no especificado — ver P-01) | Asincrónico |
| 5 | **Moderador de chat** | RF-CHT-09 a 14 | Es función de IA aunque el chat sea de otro equipo. Ver [04](04-funciones-de-ia.md) Parte 4 |
| 6 | **Agente `@mención` en canales de curso** | RF-CHT-05 | 🟡 **Fase 3 en el PRD.** Se diseña, no se construye este cuatrimestre |

> ⚠️ **Las dos piezas de chat no son MVP.** La Tabla 11 del PRD deja el **chat interno en Fase 2** y
> los **agentes de IA en canales grupales en Fase 3**. El moderador no figura como fuera de alcance,
> pero modera un chat que en el MVP no existe: es una inconsistencia del PRD que conviene llevar a
> revisión. Lo que sí hay que entregar ahora es el **contrato** `moderar(mensaje)`, para que el Tema
> 11 pueda diseñar su chat. El desarrollo completo está en [04](04-funciones-de-ia.md) Parte 4.

Más lo transversal que las envuelve a todas: el **AI Gateway**, el **runner de calibración** y los
**workers asincrónicos**.

### Lo que explícitamente NO es del equipo de IA

Usuarios, roles, permisos, autenticación, 2FA, cursos, roadmap, el motor de desafíos, la economía de
gamificación (XP, monedas, vidas, insignias, equipamiento), el ranking, el cierre de curso, las
encuestas y **todas las pantallas**.

## 2c. La frontera del XP — tu instinto es el diseño correcto

> *"yo le doy los números; el desafío o motor de desafíos es el que tendría que asignar la XP"*

**Exacto, y es la decisión de diseño correcta.** Ya estaba en ADR-001, pero conviene dejarla escrita
como contrato porque es la zona gris que más se malinterpreta entre equipos.

```mermaid
flowchart LR
    subgraph ia["ms-evaluacion-llm - NUESTRO"]
        A["Evaluador puntua<br/>5 dimensiones 0-100"]
        B["Score agregado con<br/>pesos fijos RF-IA-15"]
        C["+ confianza<br/>+ justificacion por dimension"]
        A --> B --> C
    end

    subgraph core["motor de desafios - OTRO EQUIPO"]
        D["Recibe el score"]
        E["Calcula el modificador<br/>PAR-05 +-20% sobre XP base"]
        F["Aplica XP + monedas + vidas<br/>en UNA transaccion"]
        G["Recalcula ranking"]
        D --> E --> F --> G
    end

    C -->|"POST /internal/ai-result"| D
```

### Por qué el corte va justo ahí

| Motivo | Detalle |
|---|---|
| **Atomicidad** | Otorgar XP + monedas + vidas + insignia + nivel es un solo acto. Si vos escribieras el XP por tu lado, habría **dos escritores sobre la misma economía** y una condición de carrera esperando |
| **Un solo dueño de la economía** | PAR-01 a PAR-14 son parámetros globales de ADMIN. El que los interpreta es el motor de desafíos, no vos |
| **Vos no sabés el XP base** | El score de IA es un **modificador** (±20%, PAR-05) sobre un XP base que depende de dificultad, obligatoriedad, calidad de la solución y tiempo. Todo eso vive del otro lado |
| **RF-IA-27 es lógica de producto, no técnica** | "Si el evaluador no está: aceptar la entrega, otorgar XP base y monedas **ya**, diferir el score" — eso lo ejecuta el backend, no vos |

### Lo que hay que dejar por escrito en el contrato

Es lo que el usuario intuyó como *"algo a definir"*. Estos cuatro puntos:

1. **Vos devolvés `score_agregado` (0-100), el desglose por dimensión, la confianza y las
   justificaciones.** Nunca un valor de XP.
2. **El motor de desafíos traduce ese score a modificador** aplicando PAR-05, y lo suma a los otros
   criterios (calidad de solución, tiempo — PAR-04). El score de IA **no reemplaza** a los demás.
3. **Vos exponés el contador de scores pendientes por curso**; el backend lo consulta antes de dejar
   cerrar un curso (RF-IA-34).
4. **El backend implementa la degradación de RF-IA-27.** Si tu servicio no responde, la entrega se
   acepta igual y el XP base se otorga en el momento. **Eso no lo podés garantizar vos** — es una
   decisión del que escribe en la base.

> ⚠️ **El punto 4 es el que más se cae entre equipos.** El otro equipo suele asumir que "la
> resiliencia es cosa de la IA". No lo es: aceptar la entrega con el evaluador caído es lógica del
> backend. Si nadie la implementa, la caída de un proveedor externo bloquea a un alumno — que es
> exactamente lo que el principio rector de RF-IA-27 prohíbe.

---

### Lo que sí decidís vos

| Decisión | Documento |
|---|---|
| Arquitectura interna del `ai-service` | [02](02-arquitectura-y-stack.md) |
| Qué modelos, qué proveedores, qué cuesta | [03](03-modelos-costos-y-contexto.md) |
| Sincrónico vs asincrónico, y la cola | [06](06-operacion-e-ingenieria.md) |
| Pipeline del generador | [04](04-funciones-de-ia.md) |
| Rúbricas, calibración, umbrales | [04](04-funciones-de-ia.md) |
| Guardarraíles y defensa contra injection | [05](05-seguridad.md) |
| **El lenguaje del `ai-service`** | [02](02-arquitectura-y-stack.md) |

### Lo que ya NO es tu decisión

**Java Spring Boot vs Python para el backend de negocio no es tu llamada.** Y tampoco lo es para el
`ms-evaluacion-llm`: **ADR-005 cerró esa decisión — el servicio va en Java Spring Boot**, igual que el
resto de la plataforma.

El fundamento completo —ventajas y desventajas de cada uno, herramientas por capa, y por qué Java gana
en este contexto— está en [02 · Arquitectura y stack](02-arquitectura-y-stack.md), Parte 2.

Lo que ADR-005 **sí** deja abierto: si hace falta un componente auxiliar para embeddings locales o AST
multilingüe, puede agregarse como **componente interno — no microservicio**. Las interfaces ya están
preparadas desde el diseño inicial.

## 3. Lo que necesitás pedirle a los otros equipos

Esta es la lista concreta. Reclamala temprano — cada ítem que llegue tarde te bloquea.

### 3.1 Del backend de negocio

| Qué | Por qué lo necesitás | Requerimiento |
|---|---|---|
| **Endpoint de contexto de desafío** | Para tutorear necesitás enunciado, código actual, tipo y nivel de riesgo | RF-IA-19 |
| **Endpoint para devolver resultados** | Vos no escribís en la base académica | ADR-001 |
| **Disparador al cerrar un intento** | Es lo que encola la evaluación | RF-IA-12 |
| **Que la entrega se acepte con el evaluador caído** | La entrega no se bloquea nunca; XP base y monedas se otorgan igual | RF-IA-27 |
| **Que el cierre de curso consulte tus pendientes** | El profesor no puede archivar con scores pendientes | RF-IA-34 |
| **Que el paso draft→activo consulte tu calibración** | Bloqueo duro, sin override | RF-IA-36 |
| **Identidad y `curso_id` derivados de la sesión** | Si vienen del cliente, tu aislamiento no vale nada | [05](05-seguridad.md) |

### 3.2 Del equipo de front

Siete pantallas. **Ninguna la hacés vos, y sin ellas la IA no se puede usar ni verificar.**

| Pantalla | Para qué | Requerimiento |
|---|---|---|
| Chat del tutor dentro del IDE | Es la interfaz del tutor | RF-IA-01 |
| **Desglose del score por dimensión** con justificación | El alumno tiene que ver por qué le puntuaron así | RF-IA-16 |
| **Flujo de apelación** | El alumno pide revisión humana | RF-IA-18 |
| **Revisión del parcial generado**, con el fragmento fuente al lado | Gate humano obligatorio antes de publicar | [04](04-funciones-de-ia.md) §5 |
| **Herramienta de golden set** para docentes | Cargar y puntuar transcripciones de referencia | RF-IA-30, DoD 7b |
| **Dashboard de incidentes** de jailbreak y moderación | El profesor tiene que ver los incidentes | RF-IA-10, RF-CHT-11 |
| **Config de modelos del ADMIN** | Asignación modelo→función | RF-IA-24 |

> ⚠️ **La herramienta de golden set es la más urgente y la que más se subestima.** No es una pantalla
> más: sin ella, los docentes no pueden calibrar, y sin calibración **ningún curso arranca**
> (RF-IA-36, sin override). Y encima el trabajo docente de producirla es un hito de calendario
> académico (RF-IA-36b), no de desarrollo.

### 3.3 Del Product Owner

Las 11 preguntas abiertas de [08](08-decisiones-y-pendientes.md). Las tres que más te bloquean:

| # | Pregunta | Por qué te bloquea |
|---|---|---|
| **P-04** | ¿Quién produce el golden set y para cuándo? | 🔴 Sin fecha, el MVP no sale |
| **P-06** | ¿El free tier puede tocar datos de alumnos? | Define todo tu modelo de costos |
| **P-01** | ¿El corrector lleva calibración como el evaluador? | Cambia el diseño del corrector |

## 4. Las cosas que van a caer en el medio

Los puntos donde cada equipo asume que lo hace el otro. **Estos son los que fallan.**

| Zona gris | El malentendido típico | Cómo resolverlo |
|---|---|---|
| **¿Quién es dueño de la cola?** | IA asume que el backend encola; el backend asume que la IA se encarga | **Recomendación: tuya.** Es tu resiliencia (RF-IA-27) y tu control de cuota. El backend solo llama a un endpoint |
| **¿Quién persiste el score?** | Los dos escriben, o ninguno | **El backend.** Vos devolvés; él lo aplica en su transacción. Un solo dueño de la economía |
| **El contador de pendientes de RF-IA-34** | Nadie lo construye hasta el día del cierre | Es tuyo exponerlo, del backend consumirlo. Definilo en el contrato desde el día uno |
| **Ingesta del material del curso** | ¿Sube el archivo el backend y te avisa, o lo recibís vos? | **Recomendación:** el backend guarda el archivo, te pasa una referencia, vos indexás. Vos no manejás archivos de usuario |
| **Prueba de carga con IA (DoD 10)** | Cada equipo prueba lo suyo y nadie prueba el conjunto | Es conjunta por definición. Agendala explícitamente |
| **Detección de deriva (RF-IA-32)** | Corre mensual... ¿quién lo dispara? | Tuyo, con un scheduler propio dentro del `ai-service`. No dependas de otro equipo para algo periódico |
| **Los T&C (RF-NFR-09)** | Nadie escribe qué proveedores se usan | Vos sos el único que sabe la lista. **Entregala vos, escrita** |

## 5. El contrato: tu artefacto más importante

Cuando la frontera es entre equipos y no solo entre servicios, **el contrato deja de ser
documentación y pasa a ser el acuerdo de trabajo**. Tres reglas:

1. **Escribilo primero, en OpenAPI, antes de implementar nada.** El otro equipo puede empezar contra
   un mock mientras vos construís lo de verdad. Los dos avanzan en paralelo desde el día uno.
2. **Mantenelo chico.** 6 endpoints ([02](02-arquitectura-y-stack.md) §8). Cada endpoint nuevo es
   una negociación nueva.
3. **Versionalo.** Un cambio incompatible sin aviso rompe al otro equipo en medio de su sprint.

**El `ai-service` nunca se expone a internet.** Sin puerto publicado en el Compose, autenticación por
token interno. Solo el backend le habla.

## 6. Qué cambia en la demo

Como el equipo solo hace IA, **la demo debería demostrar el `ai-service` solo, sin depender de que
nadie más tenga nada listo.**

| Paso | Qué mostrar | Sin depender de |
|---|---|---|
| D1 | Ingesta de un PDF → chunks → búsqueda que devuelve el fragmento correcto | Front, backend, auth |
| D2 | Generación de parcial por API, con parámetros | Ídem |
| D3 | Corrección de respuestas con rúbrica y desglose | Ídem |
| D4 | Tutor con RAG y guardarraíles | Ídem |

Una UI mínima propia — dos páginas feas, sin diseño — **solo para poder mostrarlo**. No es la UI del
producto: es tu banco de pruebas. Sirve para tres cosas y las tres valen:

1. Demostrarle al equipo qué hace el servicio.
2. Iterar prompts sin esperar al front.
3. **Ser el prototipo de las 7 pantallas que el otro equipo va a tener que construir** — mostrarles
   una versión fea que funciona vale más que cualquier especificación escrita.

## 7. Los tres riesgos del recorte de alcance

| Riesgo | Por qué pasa | Mitigación |
|---|---|---|
| **La IA queda lista y no se puede usar** | Faltan las 7 pantallas de §3.2 | Reclamalas ahora, con los IDs de requerimiento y los puntos del DoD en la mano |
| **El golden set no existe el día del go-live** | Nadie lo agendó porque no es trabajo de desarrollo | 🔴 Escalá P-04 al PO **esta semana**. Es el único DoD que no depende de ningún equipo técnico |
| **El backend no implementa la degradación de RF-IA-27** | Asume que la resiliencia es "cosa de la IA" | La degradación es **de producto**, no técnica: aceptar la entrega y otorgar XP con el evaluador caído es lógica del backend. Explicitalo en el contrato |
