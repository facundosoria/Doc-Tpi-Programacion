# 20 — Backlog general y plan de sprints

> **Todo lo que hay que hacer, en un solo lugar**, y cómo se reparte entre **12 personas** en
> **sprints de dos semanas**.
>
> Es la traducción a trabajo planificable de lo que ya está decidido: los ocho módulos de
> [02](02-arquitectura-y-stack.md), los siete bloques de entregables y los catorce pasos de
> [10](10-entregables-y-plan.md), los seis endpoints y los eventos de
> [18](18-contratos-inter-equipos.md), y los bloqueantes de [08](08-decisiones-y-pendientes.md).
>
> **Este documento no decide nada nuevo.** Si algo acá contradice a un ADR de
> [08](08-decisiones-y-pendientes.md) Parte A, manda el ADR.

---

## Cómo leer este documento

| Parte | Qué contiene |
|---|---|
| **1** | El equipo de 12: cómo se parte en seis células y qué regla evita el caos |
| **2** | **El backlog general**: 12 épicas, 100 historias, con criterio de aceptación y estimación |
| **3** | **Los siete sprints**, con objetivo, reparto por célula y criterio de cierre |
| **4** | Lo que queda afuera del cuatrimestre, y por qué |
| **5** | Ceremonias, métricas y las dos definiciones (*ready* y *done*) |

### Las convenciones

**IDs.** `E<épica>-<historia>`. El ID no se reutiliza ni se renumera: si una historia se cancela, el
ID queda muerto. Es lo que hace que un ID escrito en un commit siga significando algo en diciembre.

**Estimación.** Puntos de historia en Fibonacci (1, 2, 3, 5, 8, 13). La referencia del equipo:

| SP | Equivale a | Ejemplo |
|---|---|---|
| **1** | Una tarde sin sorpresas | Rotar una API key, sumar un chequeo al pipeline de calidad |
| **2** | Un día de una persona | Un endpoint que devuelve un mock con los campos del contrato |
| **3** | Dos días de una persona | Un adapter de proveedor sobre una interfaz que ya existe |
| **5** | Una semana de una persona, o dos días de a dos | El registro `función → modelo` con su tabla y su ABM |
| **8** | Casi un sprint de una persona | El runner de calibración, la salvaguarda anti-fuga |
| **13** | 🚫 **No se planifica.** Se parte antes de entrar a un sprint | — |

**Prioridad (MoSCoW).**

| Marca | Significa |
|---|---|
| 🔴 **Must** | Sin esto no hay entrega, o hay otro equipo bloqueado |
| 🟡 **Should** | Duele no tenerlo, pero la entrega existe igual |
| 🟢 **Could** | Mejora real, primera en salir si el sprint se aprieta |
| ⬜ **Won't** | Decidido que **no** entra en este cuatrimestre. Está en la Parte 4 |

**Dependencias.** Se anota solo la dependencia *dura* — la que impide empezar, no la que incomoda.
Una historia sin dependencias se puede tomar el día 1 del sprint.

---

# Parte 1 — El equipo de 12

## 1. Seis células de dos

El reparto de [10](10-entregables-y-plan.md) Parte 2 es de seis personas, una por módulo. Con doce,
**la unidad no es la persona: es la célula de dos**, y cada célula hereda el módulo de su P
correspondiente más lo que antes quedaba sin dueño: las pantallas, los contratos y el pipeline
de calidad.

| Célula | Módulos | Épicas que posee | Equivale a |
|---|---|---|---|
| **C1 · Plataforma y contratos** | M8 | E01, E03 (parcial), E10-07, E11 | P6 (mitad) |
| **C2 · AI Gateway** | M1 | E02, E03-07, E03-09, E11-05 | P1 |
| **C3 · RAG y generador** | M2, M7 (generación) | E04, E09-01…04, E10-06 | P2 |
| **C4 · Evaluador y corrector** | M3, M7 (corrección) | E05, E09-05, E10-04 | P3 |
| **C5 · Calibración y gobernanza** | M4 | E06, E10-02, E10-03, E10-05 | P4 |
| **C6 · Guardarraíles y tutor** | M5, M6 | E07, E08, E10-01 | P5 + P6 (mitad) |

### Por qué de a dos y no doce individuos

Con seis módulos y doce personas la tentación es partir cada módulo en dos mitades. **Es la peor
opción:** duplica la cantidad de interfaces internas justo donde el documento de arquitectura las
quiso pocas y explícitas.

La célula de dos resuelve las tres cosas que rompen un equipo grande en un TP:

1. **Sigue habiendo un dueño por módulo** — la célula, no la persona. La pregunta *«¿a quién le
   pregunto por esta carpeta?»* sigue teniendo una respuesta.
2. **Nadie queda bloqueado por una ausencia.** Un parcial, una guardia o una gripe no congelan un
   módulo entero, que es lo que pasa con un dueño único.
3. **La revisión es interna y ocurre el mismo día.** Cada PR lo revisa el par antes de salir de la
   célula. Con doce personas y revisión cruzada al azar, un PR espera dos días.

> **Dentro de la célula, roles asimétricos, no clonados.** Una persona lleva el backend del módulo y
> la otra lleva su pantalla y sus pruebas. Se rotan en el sprint 4, para que ninguna de las dos
> quede sin saber la mitad de lo suyo.

## 2. Las tres reglas que no se negocian

Son las de [10](10-entregables-y-plan.md) Parte 2 §7, con la corrección que impone ser doce:

1. **La interfaz del módulo se define primero**, aunque el cuerpo devuelva un valor fijo. Con doce
   personas, un módulo sin interfaz publicada bloquea a diez.
2. **Nadie llama a un proveedor de LLM directamente.** Todos pasan por la función de C2. Con seis
   células, seis formas de manejar un timeout es garantía de que ninguna anda.
3. **`repository/` y `entity/` son territorio compartido y el punto de conflicto más probable**
   ([ESTRUCTURA.md](../codigo-ejemplo/ms-evaluacion-llm/ESTRUCTURA.md)). Podés leer la entidad de
   otra célula; para cambiarle un campo, hablás con su dueña. Un `ALTER` sobre una tabla ajena rompe
   la migración de otro y se descubre en la máquina de él, no en la tuya.

## 3. La capacidad

| | |
|---|---|
| Personas | **12** |
| Células | **6** de 2 |
| Duración del sprint | **2 semanas** |
| Dedicación estimada | ~7 h por persona y por semana |
| **Capacidad por célula y sprint** | **~10 SP** (rango aceptado: 8 a 13) |
| **Capacidad del equipo por sprint** | **~60 SP** |
| Sprints planificados | **7** (S1 a S7) |

> **S1 y S2 aparecen por encima de la capacidad.** No es un error de planificación: cargan las
> conversaciones que destraban todo lo demás —glosario, alcance, contrato de eventos, golden set—.
> **Son reuniones y decisiones, no código**, y se pagan una sola vez.

---

# Parte 2 — El backlog general

## Resumen por épica

| Épica | Título | Célula | Historias | SP |
|---|---|---|---|---|
| **E01** | Plataforma y esqueleto del servicio (M8) | C1 | 10 | 41 |
| **E02** | AI Gateway (M1) | C2 | 11 | 52 |
| **E03** | Contratos y API pública | C1 · C2 · C5 | 9 | 38 |
| **E04** | RAG: ingesta, embeddings y retrieval (M2) | C3 | 8 | 39 |
| **E05** | Evaluador y rúbrica (M3) | C4 | 9 | 42 |
| **E06** | Calibración y golden set (M4) | C5 | 8 | 40 |
| **E07** | Guardarraíles y moderación (M5) | C6 | 8 | 39 |
| **E08** | Tutor y captura de metadata (M6) | C6 | 6 | 28 |
| **E09** | Generador y corrector (M7) | C3 · C4 | 6 | 32 |
| **E10** | Las pantallas del monolito Angular | cada dueña | 8 | 44 |
| **E11** | Calidad, CI/CD y observabilidad | C1 | 7 | 24 |
| **E12** | Coordinación, decisiones y documentación | transversal | 10 | 32 |
| | **Total** | | **100** | **451** |

---

## E01 · Plataforma y esqueleto del servicio

**Célula C1.** Módulo M8. Es lo que las otras cinco necesitan para poder correr algo, y por eso
arranca el día 1 junto con E02.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E01-01 | `docker compose up` levanta servicio + Postgres + Redis | Con el repo recién clonado y sin más instalado que Docker, un solo comando deja el servicio respondiendo. Documentado en el README del micro | 5 | — | 🔴 |
| E01-02 | Perfiles de configuración `local`, `demo` e `integracion` | Cambiar de proveedor o de base es cambiar un perfil, no editar código. **Ninguna credencial versionada**: todo por variable de entorno | 3 | E01-01 | 🔴 |
| E01-03 | Migraciones versionadas y esquema inicial | Flyway crea `conversacion`, `mensaje`, `evaluacion`, `dimension`, `override`, `llamada_llm` y `funcion_modelo`. Cada tabla con su dueña declarada | 5 | E01-01 | 🔴 |
| E01-04 | OpenAPI servido por springdoc, con Swagger UI | La UI muestra los seis endpoints. En la demo, el Swagger reemplaza al front | 2 | E01-01 | 🔴 |
| E01-05 | Errores tipados según el contrato | Los cinco códigos de [18](18-contratos-inter-equipos.md) §1.5 —`429`, `503`, `409`, `422`, `401`— salen con su `codigo` y su `trace_id`. Un test por código | 3 | E01-04 | 🔴 |
| E01-06 | Cola Redis: productor y worker | Misma imagen, distinto comando. Un trabajo encolado sobrevive al reinicio del servicio y lo drena el worker | 8 | E01-03 | 🔴 |
| E01-07 | `GET /ai/jobs/{job_id}` | Devuelve `en_proceso`, `completado` con resultado, o `fallido` con causa. Un job inexistente da `404`, no `500` | 3 | E01-06 | 🔴 |
| E01-08 | Publicación de eventos al bus | Los cuatro eventos de [18](18-contratos-inter-equipos.md) §2 salen con el schema acordado. Sin bus disponible se publican contra un stub local y quedan en la tabla de salida | 5 | E01-03 | 🔴 |
| E01-09 | Consumo de `intento_cerrado` | El evento del Tema 03 encola una evaluación. Reprocesar el mismo evento **no** genera dos evaluaciones: idempotencia por `intento_id` | 5 | E01-06 · E12-03 | 🔴 |
| E01-10 | Sondas de salud sin el proveedor LLM | La sonda de readiness no consulta al proveedor (ADR-014). Con el proveedor caído el servicio sigue *ready*, y la degradación la maneja E02-08 | 2 | E01-01 | 🟡 |

---

## E02 · AI Gateway

**Célula C2.** Módulo M1. **Toda llamada a un modelo de toda la plataforma pasa por acá.** Es la
dependencia de las otras cinco células: E02-01 tiene que estar en el primer sprint, sí o sí.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E02-01 | `llamar_modelo(funcion, prompt) → respuesta` y un endpoint de prueba | Devuelve texto generado por un modelo real contra el free tier. Es el entregable de la semana 1 de la demo | 5 | E01-01 | 🔴 |
| E02-02 | Registro `función → proveedor + modelo` en tabla | Cambiar de modelo es editar una fila, **nunca tocar código ni redesplegar** (RF-IA-23/24/35). Con una sola fila ya sirve | 5 | E01-03 | 🔴 |
| E02-03 | Adapter del proveedor primario | Un proveedor real anda punta a punta, con timeouts y manejo de error propios | 3 | E02-01 | 🔴 |
| E02-04 | Segundo adapter de proveedor | La misma función corre contra otro proveedor cambiando la fila de E02-02, sin tocar el llamador | 3 | E02-02 | 🟡 |
| E02-05 | Validación de la salida contra JSON schema | Una respuesta que no valida **no llega al llamador**: se reintenta y, si vuelve a fallar, se devuelve error tipado (RF-IA-13/16) | 5 | E02-01 | 🔴 |
| E02-06 | Log de cada llamada | Modelo, versión, tokens de entrada y salida, costo, latencia e incidentes, por llamada y consultable (RF-IA-02/25/33) | 5 | E01-03 | 🔴 |
| E02-07 | Cuotas por usuario, por desafío y por día | Al agotarse, `429` con `cuota_agotada` (RF-IA-22). Los umbrales van en tabla porque P-05 sigue abierta | 5 | E02-06 | 🔴 |
| E02-08 | Reintentos, timeouts y escalera de degradación | Con el proveedor caído, la entrega del alumno **se acepta igual** y la evaluación queda pendiente (RF-IA-27). Se demuestra apagando el proveedor a mano | 8 | E02-03 · E01-06 | 🔴 |
| E02-09 | Caché de respuestas y métrica de aciertos | Dos llamadas idénticas en la misma ventana consumen tokens una sola vez, y el porcentaje de aciertos es visible | 3 | E02-06 | 🟢 |
| E02-10 | Observabilidad de costo y latencia por función | Un endpoint responde cuánto costó y cuánto tardó cada función en un rango de fechas. Es el insumo del Tema 12 | 5 | E02-06 | 🟡 |
| E02-11 | Rate limiting en tres capas | Bucket4j en el borde, cuota por desafío y Resilience4j sobre el proveedor, según [19](19-modernizacion-seguridad-y-ratelimit-llm.md). Un test dispara *Denial of Wallet* y el costo queda acotado | 5 | E02-07 | 🔴 |

---

## E03 · Contratos y API pública

**Células C1 (dueña del contrato), C2 (autenticación) y C5 (endpoints de calibración).**
Se escribe **antes** de implementar: los otros equipos arrancan contra el mock.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E03-01 | OpenAPI de los seis endpoints, publicado antes de implementar | El archivo está en el repo, cubre los seis de [18](18-contratos-inter-equipos.md) §1.1 y **ningún endpoint más**. Cualquier otro es diseño interno | 5 | — | 🔴 |
| E03-02 | `POST /ai/{funcion}` con `X-Mode` e idempotencia | `sync` responde `200`; `async` responde `202` con `job_id`. Repetir la misma `idempotency_key` devuelve el resultado original, no uno nuevo | 8 | E01-06 · E03-01 | 🔴 |
| E03-03 | `GET /ai/calibracion/{curso_cohorte_id}` — mock temprano | **Los ocho campos** del contrato, no cuatro. Se entrega con datos falsos en el sprint 2: sin esto el Tema 02 no puede activar cursos | 3 | E03-01 | 🔴 |
| E03-04 | `GET /ai/pendientes/{curso_cohorte_id}` | Lista las evaluaciones sin score de la cohorte. Bloquea el cierre de curso del backend | 3 | E03-01 | 🔴 |
| E03-05 | `POST /ai/ingesta` | Siempre `202` con `job_id`. Encola la ingesta de material del Tema 02 | 3 | E01-06 · E04-01 | 🟡 |
| E03-06 | `POST /ai/calibracion` | Siempre `202`. Dispara una corrida del runner sobre el golden set de la cohorte | 3 | E06-03 | 🟡 |
| E03-07 | `curso_cohorte_id` y `alumno_id` derivados del JWT | Si vienen en el body **se ignoran**. Un test manda valores falsos en el body y verifica que se usan los del token | 5 | E03-01 | 🔴 |
| E03-08 | Mock server publicado para los otros equipos | Los cinco equipos integran contra respuestas de ejemplo sin que el servicio real exista. Una línea en el README para levantarlo | 3 | E03-01 | 🟡 |
| E03-09 | Autenticación servicio a servicio | Token técnico y validación RS256 según [gateway-y-discovery/04](gateway-y-discovery/04-seguridad-y-pipeline-de-filtros.md) y [/05](gateway-y-discovery/05-comunicacion-micro-a-micro.md). Una llamada sin token técnico da `401` | 5 | E03-07 | 🟡 |

---

## E04 · RAG: ingesta, embeddings y retrieval

**Célula C3.** Módulo M2. No depende de ninguna definición externa salvo la confirmación de alcance
(A-3) — y aun con A-3 abierta, el tutor lo necesita.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E04-01 | Ingesta de PDF a texto, incluidos los que traen imágenes | Un apunte real de la cátedra queda en texto, con el camino de OCR resuelto para las páginas escaneadas, según [12](12-almacenamiento-e-ingesta.md) | 8 | — | 🔴 |
| E04-02 | Chunking con metadata | Cada chunk lleva `curso_cohorte_id`, unidad, tema, **página** y tipo. Sin la página no se puede citar la fuente | 5 | E04-01 | 🔴 |
| E04-03 | Embeddings locales | Corren sin llamar a una API (ADR-006). Reindexar el apunte entero no cuesta un centavo | 5 | E04-02 | 🔴 |
| E04-04 | pgvector y búsqueda top-k con página | *«¿Qué dice sobre punteros?»* devuelve el fragmento **con su número de página**. Es el entregable de la semana 2 de la demo | 5 | E04-03 · E01-03 | 🔴 |
| E04-05 | Perímetro temático por filtro de retrieval | El filtro se aplica en el servidor sobre la metadata, **no en el prompt** (ADR-007). Un pedido fuera del curso no recupera nada, aunque el texto lo pida amablemente | 5 | E04-04 | 🔴 |
| E04-06 | Umbral de baja similitud | Preguntar algo que **no está** en el apunte devuelve baja similitud y una respuesta de *no lo sé*, en vez de inventar. Es la prueba que casi nadie hace | 3 | E04-04 | 🔴 |
| E04-07 | Conjunto de 30 preguntas etiquetadas | `recall@3 > 85%`, corrido en el CI. Sin esto no se puede tocar el chunking sin miedo | 5 | E04-04 | 🟡 |
| E04-08 | Reingesta versionada y borrado por `curso_archivado` | Subir la versión 2 de un apunte no deja huérfanos de la versión 1, y archivar una cohorte borra su material | 3 | E04-02 | 🟡 |

---

## E05 · Evaluador y rúbrica

**Célula C4.** Módulo M3. Es el núcleo del tema: todo lo demás existe para que este número sea
defendible.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E05-01 | La rúbrica como artefacto declarativo versionado | Archivo versionado con las 5 dimensiones, pesos 30/25/20/15/10 y anclas de bajo, medio y alto. **Legible por un docente**, no un prompt (RF-IA-29) | 5 | — | 🔴 |
| E05-02 | Features determinísticos de la transcripción | Cantidad de mensajes, mensajes triviales, tiempo hasta el primer mensaje, ediciones previas e incidentes del guardarraíl. Se guardan **con** la transcripción | 5 | — | 🔴 |
| E05-03 | Evaluar una transcripción contra la rúbrica | Rúbrica, transcripción y features devuelven las 5 dimensiones con justificación y confianza, validadas contra schema. A un humano le tiene que parecer razonable | 8 | E05-01 · E05-02 · E02-05 | 🔴 |
| E05-04 | El agregado `Evaluación` con overrides *append-only* | Nadie modifica una dimensión por afuera de su evaluación, y un override **agrega** un registro sin pisar el anterior. Una apelación de hace ocho meses sigue siendo auditable | 5 | E01-03 | 🔴 |
| E05-05 | Score agregado con desglose y confianza | `score_agregado` de 0 a 100 más las cinco dimensiones (RF-IA-16). **Nunca se devuelve XP** | 3 | E05-03 | 🔴 |
| E05-06 | Flujo de apelación, lado backend | El alumno apela, queda registrado con su motivo, y la resolución entra como override auditable (RF-IA-18) | 5 | E05-04 | 🟡 |
| E05-07 | Trazabilidad de versiones en cada evaluación | Cada evaluación guarda `rubric_version`, `prompt_version`, `model_id` y `model_version`. Sin esto, la deriva no se puede explicar | 3 | E05-03 | 🔴 |
| E05-08 | Publicar `score_de_ia_calculado` y `score_pendiente_diferido` | Con el schema de [18](18-contratos-inter-equipos.md) §2.1 y §2.2. El segundo se emite cuando el proveedor está caído | 3 | E01-08 · E05-05 | 🔴 |
| E05-09 | Pruebas de algo no determinístico | Snapshots con tolerancia por dimensión, según [06](06-operacion-e-ingenieria.md). Cambiar un prompt sin correr esto queda prohibido por el pipeline de calidad | 5 | E05-03 | 🟡 |

---

## E06 · Calibración y golden set

**Célula C5.** Módulo M4. **Es el ítem de mayor riesgo de calendario del proyecto**, y el único cuyo
camino crítico no es técnico: depende de horas docentes que no controlamos.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E06-01 | 10 transcripciones sintéticas | Cubren los tres perfiles: quien no intentó, quien intentó y preguntó bien, y quien pidió la solución. Datos sintéticos, sin objeción legal | 3 | — | 🔴 |
| E06-02 | Doble puntuación humana y sesión de discrepancias | Dos personas puntúan por separado y **se discute cada diferencia mayor a ±10**. De ahí sale una corrección a la rúbrica, no un promedio | 5 | E06-01 · E05-01 | 🔴 |
| E06-03 | Runner de calibración a ciegas | El modelo puntúa las 10 sin ver la nota humana, y sale la desviación por dimensión contra PAR-14 | 8 | E06-02 · E02-01 | 🔴 |
| E06-04 | Comparar los tres modelos candidatos en una pasada | La corrida dice cuál es el **más barato que pasa**, con su costo por evaluación al lado | 3 | E06-03 | 🟡 |
| E06-05 | Estado de calibración persistido y bloqueo `draft → activo` | Una cohorte sin calibración aprobada **no pasa a activo, ni con ADMIN**. El endpoint E03-03 deja de ser mock | 5 | E06-03 · E03-03 | 🔴 |
| E06-06 | Detección de deriva y recalibración mensual | Se recalibra por calendario y ante cambio de versión de modelo o de rúbrica (RF-IA-32). Fuera de tolerancia dispara evento | 5 | E06-05 | 🟡 |
| E06-07 | Golden set completo de 40 casos puntuados por docentes | Las 40 transcripciones puntuadas **por personas de la cátedra**. Son ~26 h docentes, o ~4 h en la versión reducida | 8 | E10-02 · E12-04 | 🔴 |
| E06-08 | Eventos `calibracion_aprobada` y `calibracion_fuera_de_tolerancia` | Con el schema de [18](18-contratos-inter-equipos.md) §2.3, consumidos por el Tema 02 y el Tema 12 | 3 | E01-08 · E06-05 | 🟡 |

---

## E07 · Guardarraíles y moderación

**Célula C6.** Módulo M5. La parte más técnica y la más aislada: se prueba sin el resto del sistema.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E07-01 | Filtro de entrada y corpus de 30 jailbreaks | Los 30 ataques del corpus no cambian el comportamiento del tutor. El corpus corre en el CI | 5 | E02-01 | 🔴 |
| E07-02 | Salvaguarda anti-fuga sobre la respuesta del tutor | Corre **antes de que el alumno vea el texto**. Compara por similitud y por AST contra la solución esperada, sin que esa solución entre nunca al contexto del modelo (ADR-008) | 8 | E07-01 · E12-02 | 🔴 |
| E07-03 | Corpus de 20 pedidos de solución | **Cero fugas.** Una sola fuga es un fallo del sprint, no un bug menor | 3 | E07-02 | 🔴 |
| E07-04 | Registro de incidentes y evento de jailbreak | Cada incidente queda registrado y publica `incidente_de_jailbreak` (RF-IA-10) con el schema de [18](18-contratos-inter-equipos.md) §2.4 | 5 | E01-08 | 🟡 |
| E07-05 | Los incidentes reales se vuelven casos de test | Un incidente detectado en producción entra al corpus sin trabajo manual. Es la fuente gratis de casos de prueba | 3 | E07-04 | 🟡 |
| E07-06 | Contrato del moderador, entregado temprano | `moderar(mensaje)` devuelve `categorias`, `severidad`, `confianza` y `origen`, publicado aunque el chat no exista. El Tema 11 lo está diseñando ahora | 5 | E03-01 | 🟡 |
| E07-07 | 100 mensajes etiquetados y capa clásica calibrada | Más del 90% de acierto en severidad media y alta, con la capa clásica resolviendo la mayoría sin salir a la red (ADR-012) | 5 | E07-06 | ⬜ |
| E07-08 | Comparación por AST multi-lenguaje | La salvaguarda anti-fuga funciona en los lenguajes de los desafíos prácticos, no en uno solo | 5 | E07-02 · E12-06 | ⬜ |

---

## E08 · Tutor y captura de metadata

**Célula C6.** Módulo M6. El tutor es el más difícil de todos: junta latencia, RF-IA-04, los tres
niveles de RF-IA-19 y el buffer de RF-IA-20. **Y sin tutor no hay transcripción, así que el
evaluador se queda sin insumo.**

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E08-01 | Servicio del tutor con contexto acotado | Responde en menos de 2 s con el contexto recortado a ~3.000 tokens. **Recortar el contexto ahorra más que cambiar de modelo** | 8 | E02-01 · E04-05 · E07-02 | 🔴 |
| E08-02 | 🔴 Captura de metadata por mensaje | Por cada mensaje: `timestamp`, tiempo desde el anterior, ediciones de código desde el anterior, rol y contenido. **Es lo único que se pierde para siempre si se posterga**: sin esto, la dimensión que pesa 30% queda inevaluable | 5 | E01-03 | 🔴 |
| E08-03 | Los tres niveles de ayuda de RF-IA-19 | El tutor sube de nivel según lo que el alumno ya intentó, y el nivel queda registrado en la transcripción | 5 | E08-01 | 🟡 |
| E08-04 | Buffer anti-streaming de RF-IA-20 | En desafíos prácticos no hay streaming token a token: se muestra *«pensando…»* hasta que la respuesta pasa el guardarraíl (ADR-009) | 3 | E08-01 | 🔴 |
| E08-05 | Transcripción completa persistida y entregable | La conversación más su metadata queda disponible para el evaluador con una sola consulta | 5 | E08-02 | 🔴 |
| E08-06 | 🔴 Rotar la API key de Groq versionada en la demo | La key se rota en el proveedor y queda solo como variable de entorno. Está anotada en [codigo-ejemplo/CORRECCIONES-SUGERIDAS.md](../codigo-ejemplo/CORRECCIONES-SUGERIDAS.md) | 2 | — | 🔴 |

---

## E09 · Generador y corrector

**Células C3 (generación) y C4 (corrección).** Módulo M7. Ambas piezas reusan lo que ya construyó su
célula: el generador es el mayor consumidor del RAG, y el corrector es el mismo patrón de juez del
evaluador con otra rúbrica.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E09-01 | Blueprint determinístico del parcial | La estructura del examen —cuántas preguntas, de qué unidad, de qué tipo— la decide código, no el modelo | 5 | — | 🟡 |
| E09-02 | Retrieval por cobertura y una llamada por pregunta | Cada pregunta sale de un fragmento distinto: el parcial cubre el temario en vez de repetir el mismo tema cinco veces | 8 | E09-01 · E04-04 | 🟡 |
| E09-03 | Cinco preguntas validadas, con su fragmento fuente | JSON validado con las 5 preguntas y, junto a cada una, el fragmento del apunte del que salió. Es la pantalla que hace entender la demo | 5 | E09-02 · E02-05 | 🟡 |
| E09-04 | 20 preguntas generadas y revisadas | Más del 70% usables según revisión humana. Debajo de eso, el generador no se entrega | 3 | E09-03 | 🟢 |
| E09-05 | Corrector de respuestas abiertas | **Solo abiertas.** Multiple choice, verdadero o falso, ordenar, emparejar y tests se corrigen con código, no con un modelo | 8 | E05-03 | 🟡 |
| E09-06 | 30 respuestas corregidas a mano como vara | La coincidencia con el corrector se mide igual que la calibración del evaluador | 3 | E09-05 | 🟢 |

---

## E10 · Las pantallas del monolito Angular

**Cada pantalla la hace la célula dueña de la función.** Van en el monolito Angular compartido, y
son nuestras porque nadie más entiende para qué existen.

| ID | Historia | Célula | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|---|
| E10-01 | Componente reutilizable del chat del tutor | C6 | Otros equipos lo consumen como una carpeta del monolito, no como librería npm. Llama **al API Gateway**, nunca directo al servicio | 8 | E08-01 · E08-04 | 🔴 |
| E10-02 | 🔴 Carga y puntuación del golden set | C5 | Un docente sube transcripciones y las puntúa dimensión por dimensión. **Destraba el ítem de plazo más largo del proyecto**, y por eso va antes que pantallas más vistosas | 8 | E06-01 | 🔴 |
| E10-03 | Comparación entre docente A y docente B | C5 | Resalta toda diferencia mayor a ±10. Es la pantalla donde se mejora la rúbrica | 5 | E10-02 · E06-02 | 🟡 |
| E10-04 | Desglose del score para el alumno | C4 | Las cinco dimensiones con su justificación y la evidencia determinística al lado (RF-IA-16) | 5 | E05-05 | 🔴 |
| E10-05 | Flujo de apelación | C5 | El alumno apela desde el desglose y ve en qué estado quedó su apelación (RF-IA-18) | 5 | E05-06 | 🟡 |
| E10-06 | Revisión del parcial generado | C3 | Cada pregunta con su fragmento fuente al lado. **Gate humano obligatorio** antes de publicar un parcial | 5 | E09-03 | 🟡 |
| E10-07 | Dashboard de incidentes | C1 | Jailbreak y moderación, filtrables por cohorte y severidad (RF-IA-10) | 5 | E07-04 | 🟢 |
| E10-08 | Convenciones del monolito compartido | C6 | Acordado y escrito: dónde vive nuestra carpeta, cómo se rutea y **cómo se resuelve un conflicto cuando doce equipos mergean en la misma app** | 3 | — | 🔴 |

---

## E11 · Calidad, CI/CD y observabilidad

**Célula C1.** El pipeline de calidad ya existe —vive en la rama `feat/qa-gate` y no se publica
acá—; esta épica es hacerlo cumplir sobre el código nuevo y sumarle lo que hoy no mira.

| ID | Historia | Criterio de aceptación | SP | Dep. | Prio |
|---|---|---|---|---|---|
| E11-01 | El pipeline en verde sobre el micro, en cada PR | El pipeline de calidad pasa sobre `ms-evaluacion-llm` y ningún PR mergea en rojo | 3 | — | 🔴 |
| E11-02 | Umbral de cobertura por módulo | Cada paquete de `service/` tiene su mínimo y el CI lo hace cumplir. Un módulo nuevo entra con su umbral declarado | 3 | E11-01 | 🟡 |
| E11-03 | Las corridas visibles en el panel del CI | El panel del CI muestra las corridas del micro, no solo las de documentación | 3 | E11-01 | 🟢 |
| E11-04 | Tests de contrato contra el OpenAPI | Un cambio de respuesta que rompe el contrato publicado falla en el CI, no en la integración | 5 | E03-01 | 🟡 |
| E11-05 | Escenario de carga: 120 sesiones concurrentes | La escala objetivo del PRD se sostiene, y el pico se absorbe por cola sin perder trabajos | 5 | E01-06 | 🟡 |
| E11-06 | Despliegue *rolling update* verificado | Un despliegue no corta sesiones de tutor en curso (ADR-013), apoyado en la sonda de E01-10 | 3 | E01-10 | 🟡 |
| E11-07 | Ninguna credencial en el repositorio, verificado | Gitleaks corre en el CI y falla ante una key. El caso conocido lo cierra E08-06 | 2 | — | 🔴 |

---

## E12 · Coordinación, decisiones y documentación

**Transversal.** Son conversaciones, no código — **y son las que bloquean**. Cada una tiene un dueño
nombrado y una fecha, porque una decisión sin dueño no se toma.

| ID | Historia | Criterio de aceptación | SP | Prio |
|---|---|---|---|---|
| E12-01 | El glosario cerrado y publicado | Los seis términos del paso 0 acordados y llevados a la sesión de integración. Medio día de todo el equipo, y evita semanas de confusión: *«evaluación»* significa cosas distintas en tres temas | 3 | 🔴 |
| E12-02 | Cerrar B-1: cómo accedemos a la solución esperada del Tema 05 | Acordado el mecanismo. **Sin esto no hay salvaguarda anti-fuga**, y la salvaguarda es alcance confirmado | 3 | 🔴 |
| E12-03 | Cerrar B-3: nuestros campos en el contrato de eventos del Tema 11 | Los campos entran **antes de que cierren el contrato**. Después es renegociar con cinco equipos | 3 | 🔴 |
| E12-04 | Cerrar C-1: golden set con responsable y fecha | El Product Owner nombra una persona y pone una fecha. Es el plazo más largo del proyecto | 2 | 🔴 |
| E12-05 | Cerrar C-2: consulta legal del free tier | Respuesta escrita sobre si el free tier puede tocar datos de alumnos. Define el modelo de costos y los T&C | 2 | 🔴 |
| E12-06 | Cerrar A-1 y A-3: alcance de RAG, generador y corrector | Confirmado por escrito qué construimos. Hoy se inclina al alcance amplio, pero nadie lo dijo formalmente | 3 | 🔴 |
| E12-07 | Anexo A de los T&C con los proveedores en uso | La lista de proveedores de LLM entregada a quien redacta los T&C (RF-NFR-09). **Solo nosotros la sabemos** | 2 | 🟡 |
| E12-08 | Sesión de integración con la agenda de ocho ítems | Los ocho ítems de [18](18-contratos-inter-equipos.md) §6 tratados, con acuerdo escrito por cada uno | 3 | 🔴 |
| E12-09 | Los 31 ítems de contenido de ejemplo pasan a definitivos | El inventario de [08](08-decisiones-y-pendientes.md) Parte C queda sin marcas de *ejemplo*: cada célula define los suyos | 8 | 🟡 |
| E12-10 | Deck de defensa actualizado con lo construido | Las 43 slides dicen lo que existe, y la demo queda grabada por si falla la red | 3 | 🔴 |

---

# Parte 3 — El plan de siete sprints

## Calendario

| Sprint | Fechas | Objetivo en una frase |
|---|---|---|
| **S1** | 07/09 – 18/09 | **Levanta y contesta un modelo real**, y las cinco conversaciones que bloquean quedan abiertas con dueño |
| **S2** | 21/09 – 02/10 | **Los contratos están publicados** y el evaluador puntúa su primera transcripción |
| **S3** | 05/10 – 16/10 | **El RAG cita la página** y la salvaguarda anti-fuga bloquea |
| **S4** | 19/10 – 30/10 | **Una evaluación punta a punta**, asíncrona y con eventos |
| **S5** | 02/11 – 13/11 | **El tutor conversa** y el alumno ve su score |
| **S6** | 16/11 – 27/11 | **Generador y corrector**, deriva y el componente Angular |
| **S7** | 30/11 – 11/12 | **Integración, cierre y defensa** |
| — | 14/12 – 18/12 | Semana de defensa. **No se planifica trabajo** |

---

## Sprint 1 · «Levanta y contesta»

> **Objetivo:** al final del sprint, cualquier persona del equipo clona el repo, corre un comando y
> obtiene texto generado por un modelo real. **Y las cinco preguntas que bloquean el proyecto están
> hechas, con nombre y fecha de respuesta.**

| Célula | Historias | SP |
|---|---|---|
| **C1 · Plataforma** | E01-01 (5) · E01-03 (5) | 10 |
| **C2 · Gateway** | E02-01 (5) · E02-03 (3) | 8 |
| **C3 · RAG** | E04-01 (8) | 8 |
| **C4 · Evaluador** | E05-01 (5) · E05-02 (5) | 10 |
| **C5 · Calibración** | E06-01 (3) · E06-02 (5) | 8 |
| **C6 · Guardarraíles** | E07-01 (5) · E08-06 (2) | 7 |
| **Transversal** | E12-01 (3) · E12-02 (3) · E12-03 (3) · E12-04 (2) · E12-05 (2) | 13 |
| | **Total** | **64** |

**Criterio de cierre del sprint:**

- `docker compose up` levanta el servicio con su base, y el endpoint de prueba devuelve texto de un
  modelo real.
- La rúbrica existe como archivo versionado y un docente la puede leer sin que se la expliquen.
- La API key versionada **ya no sirve**: fue rotada.
- Las cinco preguntas bloqueantes están formuladas por escrito, con destinatario y fecha de respuesta.

> ⚠️ **El riesgo del sprint 1 no es técnico.** Es que las cinco conversaciones se posterguen «para
> cuando tengamos algo que mostrar». Para diciembre, cuatro de ellas ya no se pueden ganar.

---

## Sprint 2 · «Contratos publicados y el primer score»

> **Objetivo:** los otros equipos pueden integrar contra algo, y el evaluador puntúa una
> transcripción de verdad.

| Célula | Historias | SP |
|---|---|---|
| **C1 · Plataforma** | E03-01 (5) · E01-02 (3) · E01-04 (2) | 10 |
| **C2 · Gateway** | E02-02 (5) · E03-07 (5) | 10 |
| **C3 · RAG** | E04-02 (5) · E04-03 (5) | 10 |
| **C4 · Evaluador** | E05-03 (8) | 8 |
| **C5 · Calibración** | E03-03 (3) · E03-06 (3) · E10-02 *(primera mitad: la carga)* (3) | 9 |
| **C6 · Guardarraíles** | E08-02 (5) · E08-04 (3) · E07-06 (5) | 13 |
| **Transversal** | E12-06 (3) · E12-08 (3) | 6 |
| | **Total** | **66** |

**Criterio de cierre del sprint:**

- El OpenAPI de los seis endpoints está publicado y hay un mock corriendo.
- `GET /ai/calibracion/{id}` responde **los ocho campos**, aunque sean falsos: el Tema 02 se
  desbloquea.
- Cambiar de modelo es editar una fila de la tabla, y hay un test que lo demuestra.
- **Se está capturando metadata de tiempos.** A partir de acá, ya no se pierde.
- El contrato del moderador está entregado al Tema 11 antes de que cierren el diseño del chat.

---

## Sprint 3 · «Cita la página y bloquea la fuga»

> **Objetivo:** las dos piezas que se demuestran solas — el RAG que devuelve el fragmento con su
> página, y el guardarraíl que no deja pasar la solución.

| Célula | Historias | SP |
|---|---|---|
| **C1 · Plataforma** | E03-02 (8) · E01-05 (3) | 11 |
| **C2 · Gateway** | E02-06 (5) · E02-05 (5) | 10 |
| **C3 · RAG** | E04-04 (5) · E04-07 (5) | 10 |
| **C4 · Evaluador** | E05-04 (5) · E05-05 (3) | 8 |
| **C5 · Calibración** | E06-03 (8) | 8 |
| **C6 · Guardarraíles** | E07-02 (8) | 8 |
| | **Total** | **55** |

**Criterio de cierre del sprint:**

- Preguntar por un tema del apunte devuelve el fragmento **con número de página**; preguntar por algo
  que no está devuelve *no lo sé* en vez de inventar.
- La salvaguarda anti-fuga bloquea una respuesta del tutor **antes** de que el alumno la vea, sin que
  la solución esperada haya entrado nunca al contexto del modelo.
- El runner de calibración devuelve una desviación por dimensión sobre las 10 transcripciones.
- `POST /ai/{funcion}` acepta `sync` y `async`, y repetir una `idempotency_key` no duplica trabajo.

> ⚠️ **E07-02 depende de E12-02**, que es una conversación de S1. Si el Tema 05 no contestó cómo
> accedemos a la solución esperada, la historia entra igual **contra una solución mock**, y la
> integración real queda como deuda anotada.

---

## Sprint 4 · «Una evaluación punta a punta»

> **Objetivo:** un intento cerrado entra por el bus, se encola, se evalúa, y el score sale como
> evento. Con el proveedor apagado a propósito, **el alumno no se entera**.

| Célula | Historias | SP |
|---|---|---|
| **C1 · Plataforma** | E01-06 (8) · E11-01 (3) | 11 |
| **C2 · Gateway** | E02-08 (8) · E02-04 (3) | 11 |
| **C3 · RAG** | E04-05 (5) · E04-06 (3) | 8 |
| **C4 · Evaluador** | E05-07 (3) · E01-08 (5) · E03-04 (3) | 11 |
| **C5 · Calibración** | E06-04 (3) · E10-02 *(segunda mitad: la puntuación)* (5) | 8 |
| **C6 · Guardarraíles** | E07-03 (3) · E07-04 (5) | 8 |
| | **Total** | **57** |

**Criterio de cierre del sprint:**

- Se apaga el proveedor a mano y **la entrega del alumno se acepta igual** (RF-IA-27), con el score
  pendiente y su evento emitido.
- La pantalla de carga y puntuación del golden set existe y se le puede pasar a un docente. **A
  partir de acá, E06-07 puede arrancar.**
- Cero fugas sobre el corpus de 20 pedidos de solución.
- El pipeline de calidad corre sobre el micro en cada PR.

> **Es el sprint de la rotación de roles dentro de cada célula.** Quien llevaba el backend toma la
> pantalla y las pruebas, y al revés.

---

## Sprint 5 · «El tutor conversa, el alumno ve su nota»

> **Objetivo:** las dos caras visibles del sistema. Y la calibración deja de ser un número interno
> para convertirse en el bloqueo real de activación de cursos.

| Célula | Historias | SP |
|---|---|---|
| **C1 · Plataforma** | E01-07 (3) · E03-08 (3) · E11-03 (3) | 9 |
| **C2 · Gateway** | E02-07 (5) · E03-09 (5) | 10 |
| **C3 · RAG** | E09-01 (5) · E03-05 (3) | 8 |
| **C4 · Evaluador** | E05-08 (3) · E05-06 (5) | 8 |
| **C5 · Calibración** | E06-05 (5) · E06-08 (3) | 8 |
| **C6 · Tutor** | E08-01 (8) · E07-05 (3) | 11 |
| **Transversal** | E12-07 (2) | 2 |
| | **Total** | **56** |

**Criterio de cierre del sprint:**

- El tutor responde en menos de 2 s con contexto acotado, y su respuesta pasa por el guardarraíl.
- Una cohorte sin calibración aprobada **no pasa a activo, ni con ADMIN**.
- El alumno recibe su score con desglose y puede apelar.
- Las cuotas por usuario y por día devuelven `429` con el código tipado.

---

## Sprint 6 · «Generador, corrector y el componente»

> **Objetivo:** cerrar las funciones que faltan y entregarle al front el componente de chat que los
> otros equipos esperan.

| Célula | Historias | SP |
|---|---|---|
| **C1 · Plataforma** | E01-09 (5) · E11-04 (5) | 10 |
| **C2 · Gateway** | E02-11 (5) · E02-09 (3) | 8 |
| **C3 · Generador** | E09-02 (8) · E04-08 (3) | 11 |
| **C4 · Corrector** | E09-05 (8) | 8 |
| **C5 · Calibración** | E06-06 (5) · E10-03 (5) | 10 |
| **C6 · Tutor** | E08-03 (5) · E08-05 (5) | 10 |
| **Transversal** | E12-09 (8) | 8 |
| | **Total** | **65** |

**Criterio de cierre del sprint:**

- El evento `intento_cerrado` del Tema 03 dispara una evaluación, y reprocesarlo no la duplica.
- El generador arma un parcial que cubre el temario en vez de repetir el mismo tema.
- El corrector puntúa respuestas abiertas reusando el patrón de juez del evaluador.
- La deriva se detecta y dispara recalibración.
- **Ningún documento del repositorio dice ya «de ejemplo»** sobre un valor que se usa de verdad.

---

## Sprint 7 · «Integración, cierre y defensa»

> **Objetivo:** que lo construido esté integrado, medido y contado. **No se toma trabajo nuevo**: lo
> que no entró acá, no entra.

| Célula | Historias | SP |
|---|---|---|
| **C1 · Plataforma** | E01-10 (2) · E11-02 (3) · E11-06 (3) · E11-07 (2) | 10 |
| **C2 · Gateway** | E02-10 (5) · E11-05 (5) | 10 |
| **C3 · Generador** | E09-03 (5) · E09-04 (3) · E10-06 (5) | 13 |
| **C4 · Evaluador** | E05-09 (5) · E10-04 (5) | 10 |
| **C5 · Calibración** | E10-05 (5) · E09-06 (3) | 8 |
| **C6 · Tutor** | E10-01 (8) · E10-08 (3) | 11 |
| **Transversal** | E12-10 (3) | 3 |
| | **Total** | **65** |

**Criterio de cierre del sprint:**

- El componente de chat está en el monolito y otro equipo lo usó al menos una vez.
- 120 sesiones concurrentes sostenidas, con el pico absorbido por la cola.
- Un despliegue no corta una sesión de tutor en curso.
- El deck de defensa dice lo que existe, **y la demo está grabada por si falla la red**.

> ⚠️ **C3 entra a S7 con 13 SP, por encima del tope.** Es el único caso, y la válvula está declarada:
> si el sprint se aprieta, **E09-04 sale** — es 🟢 y no bloquea a nadie.

---

## Distribución del esfuerzo

| Célula | S1 | S2 | S3 | S4 | S5 | S6 | S7 | Total |
|---|---|---|---|---|---|---|---|---|
| **C1 · Plataforma** | 10 | 10 | 11 | 11 | 9 | 10 | 10 | **71** |
| **C2 · Gateway** | 8 | 10 | 10 | 11 | 10 | 8 | 10 | **67** |
| **C3 · RAG y generador** | 8 | 10 | 10 | 8 | 8 | 11 | 13 | **68** |
| **C4 · Evaluador y corrector** | 10 | 8 | 8 | 11 | 8 | 8 | 10 | **63** |
| **C5 · Calibración** | 8 | 9 | 8 | 8 | 8 | 10 | 8 | **59** |
| **C6 · Guardarraíles y tutor** | 7 | 13 | 8 | 8 | 11 | 10 | 11 | **68** |
| **Transversal** | 13 | 6 | — | — | 2 | 8 | 3 | **32** |
| **Total del sprint** | **64** | **66** | **55** | **57** | **56** | **65** | **65** | **428** |

**428 SP planificados** de los 451 del backlog. Los 23 restantes están en la Parte 4.

---

# Parte 4 — Lo que queda afuera, y por qué

Vale tanto como el plan: es lo que nadie debería asumir que va a estar en diciembre.

| ID | Qué | Por qué queda afuera | Cuándo |
|---|---|---|---|
| **E06-07** | Golden set completo de 40 casos puntuados por docentes | **No es trabajo nuestro.** Son ~26 h docentes que dependen de C-1, que hoy no tiene responsable ni fecha. La herramienta (E10-02) sí se entrega en S4 | Cuando el PO nombre responsable |
| **E07-07** | Los 100 mensajes etiquetados y la capa clásica calibrada | **El chat es Fase 2 del PRD**: el moderador no tiene qué moderar todavía. El contrato (E07-06) sí se entrega en S2, que es lo que el Tema 11 necesita ahora | Fase 2 |
| **E07-08** | Comparación por AST multi-lenguaje | Depende de P-08: qué desafíos prácticos entran al MVP y en qué lenguajes. Con un lenguaje alcanza para demostrar la salvaguarda | Cuando cierre P-08 |
| **E10-07** | Dashboard de incidentes | Los incidentes se registran y se publican igual (E07-04). La pantalla es visualización, y ninguna decisión depende de ella este cuatrimestre | Cuando haya incidentes reales |
| — | Agente `@mención` (RF-CHT-05) | Fase 3. Está diseñado en [04](04-funciones-de-ia.md) Parte 4 y ahí se queda hasta que alguien lo priorice | Fase 3 |
| — | Prompt caching y API Batch | Optimización de volumen. **La demo no tiene volumen**, y el costo del cuatrimestre es de USD 5 a 22 | Cuando haya volumen |
| — | Sidecar `py-tools` | Solo si los embeddings locales en Java no alcanzan. ADR-005 lo admite como componente interno, no como microservicio | Si aparece la necesidad |

> **La regla del alcance:** si algo de esta tabla se quiere adentro, **algo de la Parte 3 sale**. No
> se agrega sin sacar, y la decisión la toma el equipo en la planificación del sprint, no a mitad de
> camino.

---

# Parte 5 — Cómo se opera

## Las dos definiciones

### Definition of Ready — una historia puede entrar a un sprint si…

1. Tiene criterio de aceptación **verificable por alguien que no la escribió**.
2. Está estimada, y la estimación es **≤ 8**. Un 13 se parte antes de entrar.
3. Sus dependencias duras están cerradas, **o** hay un mock acordado que las reemplaza.
4. Se sabe qué documento de `docs/` la respalda. Una historia sin respaldo es una decisión nueva
   disfrazada de tarea, y las decisiones se toman en [08](08-decisiones-y-pendientes.md), no en el
   tablero.

### Definition of Done — una historia está terminada si…

1. **El pipeline de calidad pasa en verde**: formato, cobertura, PMD, enlaces, ortografía y
   secretos.
2. Tiene prueba automatizada. Si es no determinística, snapshot con tolerancia (E05-09).
3. La revisó el par de la célula, y si toca `repository/` o `entity/`, también la célula dueña de
   esa tabla.
4. Si cambia el contrato, **el OpenAPI cambió en el mismo PR**.
5. Si cambia una decisión, quedó anotada en [08](08-decisiones-y-pendientes.md).
6. Se puede demostrar en la review **sin explicar nada antes de mostrarlo**.

## Ceremonias

| Ceremonia | Cuándo | Duración | Quiénes |
|---|---|---|---|
| **Planificación** | Lunes de inicio del sprint | 90 min | Los 12 |
| **Daily** | Diario, asincrónico y por escrito | — | Cada célula publica: hecho, próximo, bloqueo |
| **Sincronización de células** | Miércoles | 20 min | Una persona por célula. **Solo dependencias cruzadas** |
| **Review** | Viernes de cierre | 60 min | Los 12. Se muestra funcionando, no se cuenta |
| **Retro** | Viernes de cierre, después de la review | 30 min | Los 12 |
| **Refinamiento** | Miércoles de la segunda semana | 45 min | Los 12. Se deja el sprint siguiente listo (DoR) |

> **El daily es escrito y asincrónico a propósito.** Doce personas con horarios de cursada no
> coinciden a diario, y una reunión que la mitad se pierde deja de ser el lugar donde se levantan
> los bloqueos.

## Métricas que se miran en la retro

| Métrica | Para qué | Alarma |
|---|---|---|
| **SP completados contra comprometidos** | Calibrar la capacidad real, no la deseada | Dos sprints seguidos por debajo del 75% |
| **Historias que cruzan el sprint** | Detectar historias mal partidas | Más de dos por sprint |
| **Bloqueos externos abiertos** | Los 🔴 de [08](08-decisiones-y-pendientes.md) que siguen sin dueño | Cualquiera que sobreviva a dos sprints |
| **Costo acumulado en USD** | Que el free tier alcance, y detectar *Denial of Wallet* | Cualquier salto que no se explique por más uso |
| **Desviación de calibración** | Es el número que define si el sistema sirve | Fuera de PAR-14 |
| **Fugas de solución** | RF-IA-19 | **Una sola. Es cero o falla** |

## Riesgos, con su disparador y su plan

| Riesgo | Probabilidad | Disparador | Plan |
|---|---|---|---|
| **El golden set no llega** | Alta | S4 cerrado sin responsable nombrado | Se calibra con el set reducido de 10 casos puntuados por dos integrantes, y se declara como limitación en la defensa |
| **El Tema 05 no da la solución esperada** | Media | S3 sin acuerdo | La salvaguarda corre contra una solución mock, y la integración real queda como deuda anotada |
| **El Tema 11 cierra el contrato de eventos sin nuestros campos** | Media | Aviso de cierre del contrato | Se consumen los eventos con un adaptador propio y se documenta el costo de la renegociación |
| **Una célula pierde a una persona** | Media | Ausencia mayor a un sprint | La célula pasa a una persona y **entrega la mitad de sus SP**: el resto vuelve al backlog. No se reparte a otras células |
| **El free tier no admite datos de alumnos** | Media | Respuesta legal negativa (C-2) | Se pasa al escenario B de costos (ADR-010, ~USD 21 por cuatrimestre) y se pide presupuesto |
| **La rúbrica cambia después de calibrar** | Alta | Cualquier cambio en E05-01 | Toda calibración guarda su `rubric_version` (E05-07): cambiar la rúbrica **invalida la calibración** y se recalibra. Es por diseño, no un accidente |

---

*Backlog vivo. Los IDs no se renumeran. Última actualización: 2026-09-03.*
