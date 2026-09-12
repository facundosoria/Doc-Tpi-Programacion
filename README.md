# Tema 07 — Evaluación LLM

Documentación de diseño de la capa de inteligencia artificial de la **Plataforma de Aprendizaje
Gamificado**.

> ## Estado de alineación — 2026-09-04
>
> Para planificar o implementar Tema 07, mandan primero el PRD y
> [`idea.pptx.pdf`](idea.pptx.pdf). La aplicación concreta para este repositorio está en
> [`docs/00-fuentes-de-verdad-y-convenciones.md`](docs/00-fuentes-de-verdad-y-convenciones.md):
> el servicio es **`llm-service`**, sus rutas privadas viven bajo **`/api/llm/**`** y la
> correlación usa **`traceparent` + `X-Request-Id`**. Los ejemplos que aún mencionan
> `ms-evaluacion-llm`, `/ai/*` o `trace_id` se consideran antecedentes hasta que se reescriban.

**UTN FRC · Tecnicatura Universitaria en Programación · Programación IV — Back End · 2.º año, 4.º cuatrimestre**

---

## El problema

**Construimos el servicio que le pone nota a *cómo* un alumno usó la IA, y tenemos que demostrar que
esa nota es confiable.**

Lo que lo vuelve difícil no es integrar un modelo de lenguaje:

1. **Es una IA evaluando a otra IA**, y el resultado modifica el XP — que define si un alumno
   promociona. Una nota mal puesta no es un bug: es un resultado académico que no se deshace.
2. **Hay que demostrar que evalúa como un humano.** Y para eso primero hay que lograr que **dos
   humanos se pongan de acuerdo entre ellos**, que es más difícil que el problema técnico.
3. **La restricción es asimétrica:** un único modelo activo, sin fallback (RF-IA-25) — pero su caída
   **no puede bloquear al alumno** (RF-IA-27). Un solo camino, y prohibido cortarlo.
4. **Hay que evitar que la IA filtre la solución, sin que la IA vea la solución.**
5. **El texto del alumno es a la vez el dato evaluado y un vector de ataque.**
6. **Dependemos de cinco equipos y de docentes que no controlamos.**

## 👉 Si te sumás al proyecto

Para comenzar a trabajar, leer en este orden:

1. **[00 · Fuentes de verdad y convenciones](docs/00-fuentes-de-verdad-y-convenciones.md)** y
   **[01 · Problema y alcance](docs/01-problema-y-alcance.md)** — reglas vigentes y límites del servicio.
2. **[23 · Plan de construcción del producto LLM](docs/23-plan-construccion-producto-llm.md)** —
   planificación vigente: **12 integrantes, 3 fases, 19 sprints de dos semanas y un entregable
   funcional por sprint**. Del nominal del sprint (816 horas-persona) se descuentan 102 de
   reuniones y 143 de reserva: **~571 horas de capacidad**; el trabajo estimado de las recetas
   es ~208 h/sprint (piso, no tope). RAG se entrega por primera vez en S14.
3. **[35 · Backlog ejecutable](docs/35-backlog-ejecutable.md)** y
   **[36 · Playbook de construcción](docs/36-playbook-de-construccion.md)** — el catálogo de las diez
   épicas, el checklist de Sprint 0 y las recetas S1–S19, más las reglas de ejecución. Flujo Git y
   rutina diaria: **[GITFLOW](docs/GITFLOW.md)** y **[WORKFLOW_DIARIO](docs/WORKFLOW_DIARIO.md)**.
4. **[21 · Matriz de trazabilidad](docs/21-matriz-trazabilidad-llm.md)** y
   **[08 · Decisiones y pendientes](docs/08-decisiones-y-pendientes.md)** — qué está decidido, qué
   bloquea, y las decisiones que ya se revisaron una vez — conviene mirarlas antes de reabrir una discusión.

Para la Planning y el cierre, usar la [plantilla de sprint](docs/plantillas/sprint-llm.md).
El [plan de 14 pasos para seis personas](docs/10-entregables-y-plan.md), sus cuatro semanas de demo
y el [backlog de siete sprints para doce](docs/20-backlog-y-sprints.md) se conservan como antecedentes.
Los ejemplos de modelos por paso siguen en [03](docs/03-modelos-costos-y-contexto.md) §8; no sustituyen
la verificación de disponibilidad y calibración del modelo que se implemente.

## Documentación

| # | Documento | Qué responde |
|---|---|---|
| 00 | [Fuentes de verdad y convenciones](docs/00-fuentes-de-verdad-y-convenciones.md) | 🔴 Punto de partida: precedencia, identidad, red, fases y pares canónicos. |
| 01 | [Problema, alcance y equipo](docs/01-problema-y-alcance.md) | Qué es nuestro, qué no, y qué reclamarle a los otros equipos |
| 02 | [Arquitectura y stack](docs/02-arquitectura-y-stack.md) | Reglas de la cátedra, diagrama de sistema, los 8 módulos, el AI Gateway, y el fundamento completo de Java vs Python |
| 03 | [Modelos, costos y contexto](docs/03-modelos-costos-y-contexto.md) | Qué modelo para cada función, costo por consulta, cuánto contexto meter, qué puede ser gratis |
| 04 | [Las funciones de IA](docs/04-funciones-de-ia.md) | El generador, los dos jueces, el golden set, y las dos funciones del chat |
| 05 | [Seguridad](docs/05-seguridad.md) | Prompt injection, fuga de solución, dónde corre cada guardarraíl, de dónde sale cada nota |
| 06 | [Operación e ingeniería](docs/06-operacion-e-ingenieria.md) | Colas y prioridades, pico, degradación, caché — y cómo se prueba algo no determinístico |
| 07 | [Datos y T&C](docs/07-datos-y-terminos.md) | Qué se guarda, quién lo ve, cuánto dura, y el borrador de Términos y Condiciones |
| 08 | [Decisiones y pendientes](docs/08-decisiones-y-pendientes.md) | Registro de decisiones (ADR), las que ya se revisaron, y **lo que falta definir** |
| 09 | [Preguntas y respuestas](docs/09-preguntas-y-respuestas.md) | El porqué de cada decisión, **con el caso a favor y el caso en contra** |
| 10 | [Qué entregamos y cómo](docs/10-entregables-y-plan.md) | Inventario y plan histórico de 14 pasos para 6 personas; calendario vigente en 23. |
| 11 | [Glosario y metadata](docs/11-glosario-y-metadata.md) | El vocabulario para la integración y las tres tablas que hay que crear ya |
| 12 | [Almacenamiento e ingesta](docs/12-almacenamiento-e-ingesta.md) | Qué base de datos y cuántas, MinIO, y cómo se baja a texto un PDF con imágenes |
| 13 | [La rúbrica y los prompts](docs/13-rubrica-y-prompts.md) | 📝 El artefacto central del equipo: las 5 dimensiones con sus anclas y los prompts de cada función |
| 14 | [Sincronización con la guía didáctica](docs/14-sincronizacion-guia-didactica.md) | 🔄 **Comparación con el otro set de documentación**: los 6 conflictos, lo que hay que adoptar y lo que aportamos |
| 15 | [Sincronización con la U1 de Front End](docs/15-sincronizacion-arquitectura-y-despliegue.md) | 🔄 **Infraestructura y despliegue**: los tres «gateway», qué adoptamos de las seis estrategias de release y qué descartamos con fundamento |
| 17 | [El mapa de integración](docs/17-mapa-de-integracion.md) | 🔌 **Cómo se comunica el servicio, en una sola vista**: quién nos habla y por dónde, el verbo de cada endpoint, cuánto tarda cada camino, qué modelo resuelve cada función y con qué costo, y qué le debemos a cada equipo |
| 18 | [Contratos inter-equipos](docs/18-contratos-inter-equipos.md) | 🤝 **El punto de entrada para la sesión de integración**: los 6 endpoints con schemas, los 4 eventos que publicamos, los 3 que consumimos, lo que necesitamos de cada equipo, el mapa de dependencias y la agenda de 8 ítems para acordar antes de codear |
| 19 | [Modernización, seguridad y rate limit LLM](docs/19-modernizacion-seguridad-y-ratelimit-llm.md) | 🛡️ **Protección y sostenibilidad**: Rate limiting en 3 capas (Bucket4j, cuota por desafío, Resilience4j), mitigación de Denial of Wallet y checklist de modernización |
| 20 | [Backlog general y plan de sprints](docs/20-backlog-y-sprints.md) | Antecedente: 12 épicas, 100 historias, 12 personas y siete sprints; no usar sus estimaciones sin revisar alcance y contratos. Plan vigente en 23. |
| 21 | [Matriz de trazabilidad LLM](docs/21-matriz-trazabilidad-llm.md) | Requisitos, fase, contratos, dependencias y pruebas para desarrollo. |
| 22 | [Informe comparativo de alineación](docs/22-informe-comparativo-alineacion-llm.md) | Diferencias entre la documentación anterior y la vigente. |
| 23 | [Plan de construcción del producto LLM](docs/23-plan-construccion-producto-llm.md) | Plan vigente desde cero hasta el producto completo: 3 fases, 19 sprints, capacidad con reuniones (§2), dependencias, entregables, DoR/DoD (§9.2) y aceptación. |
| 24 | [Convención de cobertura](docs/24-convenciones-cobertura.md) | Cobertura mínima 95% back/front, alcance de la medición y pruebas de infraestructura por fase; requisito de cada PR y Review. |
| 25 | [Matriz de pruebas de infraestructura](docs/25-matriz-pruebas-infraestructura.md) | Suites que verifican integraciones reales (Postgres, Compose, Gateway, Eureka, Kafka), su estado en S1 y el comando/evidencia de cada una. |
| 26 | [Herramientas y librerías](docs/26-herramientas-y-librerias.md) | Índice único del stack: qué es cada herramienta, de qué librería/binario/servicio viene, para qué, cómo y dónde se decidió. |
| 27 | [Guía de la Wiki de Taiga](docs/27-guia-wiki-taiga.md) | Regla de la cátedra para documentar en la Wiki: nombrado `GXX - TEMA`, apartados obligatorios (a–h), **checklists de cada diagrama** y secuencia DER → BPMN → Clases → Estados → Secuencias → Microservicios, guías y templates oficiales. |
| 28 | [Normativa de la cátedra para la Plataforma](docs/28-normativa-catedra-plataforma.md) | Estándares mínimos de la cátedra (MySQL, Java 21 + Spring Boot, Angular 21, Git, seguridad) y **las 9 divergencias con las decisiones vigentes de Tema 07** a resolver en integración. |
| 29 | [Guía de cátedra: Historias de Usuario (MSII, U1)](docs/29-guia-catedra-historias-de-usuario.md) | Referencia de **cómo se redactan y estiman las HU**: 3C, formato COMO/QUIERO/PARA, criterios de aceptación BDD, INVEST, épica/historia/tarea, Planning Poker y armado del backlog, con los ejemplos resueltos de la guía. |
| 30 | [Arranque ágil: Sprint 0, DoD, capacidad, épicas e HU](docs/30-arranque-agil-y-sprint-0.md) | **Vista de entrega** que reúne las cinco piezas del arranque (Sprint 0, DoR/DoD, cálculo de capacidad, primeras épicas, historias de S1) con el formato de la guía de cátedra. No es fuente de verdad: apunta a 23 §2/§9.2 y al backlog ejecutable. |
| 31 | [Plan de revisión de Golden Set y calibración](docs/31-plan-revision-golden-set-calibracion.md) | Revisión y checklist de la calibración del golden set. |
| 32 | [Especificación funcional de Golden Set y calibración](docs/32-especificacion-funcional-golden-set-calibracion.md) | Comportamiento funcional del golden set y de la calibración. |
| 33 | [Modelo de dominio y transiciones — Golden Set](docs/33-modelo-dominio-y-transiciones-golden-set.md) | Entidades, estados y transiciones del golden set. |
| 34 | Plan de purga V1 y transición exclusiva a V2 *(en elaboración)* | Purga de la V1 y pase a V2 del golden set. |
| 35 | [Backlog ejecutable S0–S19](docs/35-backlog-ejecutable.md) | Catálogo de las diez épicas (EP-01…EP-10), checklist de Sprint 0 y las recetas atómicas S1–S19 con horas, gates y aceptación. Fuente de ID, épica, pareja, dependencias y horas. Antes vivía en `Plan de ejecucion/07`. |
| 36 | [Playbook de construcción](docs/36-playbook-de-construccion.md) | Reglas de ejecución: autoridad y precedencia, arquitectura y fronteras, secuencia obligatoria para una capacidad nueva (§4), patrones que no se negocian y pruebas mínimas. Antes vivía en `Plan de ejecucion/06`. |
| 37 | [Estructura de carpetas del backend](docs/37-estructura-carpetas-backend.md) | El árbol real de `llm-service/` mapeado a las capas de 36, guía de en qué carpeta va cada cosa nueva, y el hueco conocido (controllers que saltan a persistencia directo). Sólo backend. |
| — | [Épicas del `llm-service` (formato Taiga)](docs/epicas/README.md) | Las diez épicas (EP-01…EP-10), una por archivo, en el template oficial de épica: objetivo, suposiciones y restricciones, criterios de aceptación a nivel épico y dependencias. Fuente de verdad del catálogo: `docs/35`. |
| — | [Historias de usuario (formato Taiga), por épica](docs/historias/README.md) | Las HU en el template oficial, agrupadas por épica ([EP-01](docs/historias/ep-01/README.md), [EP-03](docs/historias/ep-03/README.md)): Como/Quiero/Para, notas, criterios de aceptación con negativos, BDD (≥3 escenarios), prototipo, estimación y dependencias. Versión detallada de la tabla de `docs/35`. |
| — | [Tareas SMART por historia](docs/tareas/README.md) | El desglose de cada historia en tareas técnicas ([EP-01](docs/tareas/ep-01/README.md), [EP-03](docs/tareas/ep-03/README.md)), método SMART, en el template de Tarea de Taiga: objetivo SMART, pasos, criterio de terminado, estimación y trazabilidad al CA/escenario. |
| — | [Sprints — registro y vista de historias](docs/sprints/README.md) | Empieza en el **Sprint 0** ([acta](docs/sprints/sprint-0.md)): arranque sin incremento, capacidad de S1, historia canónica, dependencias y ambiente. Vista de S1: [índice y demo](docs/sprints/s1-historias.md) · [explicado sin jerga](docs/sprints/s1-explicado.md). S1+ se registran con la plantilla de sprint. |
| — | [Wireframes de LLM-S01-H07](docs/prototipos/wireframes-h07-golden-set.md) | Bocetos de baja fidelidad de la pantalla docente del golden set, con trazabilidad a los escenarios BDD de H05–H07. |
| — | [Plantilla de sprint LLM](docs/plantillas/sprint-llm.md) | Registro de Planning, disponibilidad individual, reuniones, historias, dependencias, Review/demo y retro. |
| — | [Plantilla de página de Wiki por grupo](docs/plantillas/pagina-wiki-grupo.md) | Estructura lista para copiar a la Wiki de Taiga: `GXX - TEMA`, descripción, HU, diagramas, Draw.io, explicación, notas técnicas y documentación de endpoints. |
| — | [Plantilla de Historia de Usuario (Taiga)](docs/plantillas/historia-de-usuario-taiga.md) | Template oficial de HU para el backlog de Taiga: COMO/QUIERO/PARA, notas, criterios de aceptación, BDD (≥3 escenarios), prototipo, estimación y dependencias. Cómo se completan: [29](docs/29-guia-catedra-historias-de-usuario.md). |
| — | [Plantilla de Épica (Taiga)](docs/plantillas/epica-taiga.md) | Template oficial de épica para el backlog de Taiga: objetivo, suposiciones y restricciones, criterios de aceptación a nivel épico y dependencias/impactos. |
| — | [Plantilla de Tarea (Taiga)](docs/plantillas/tarea-taiga.md) | Template oficial de Tarea para el backlog de Taiga: objetivo **SMART**, pasos/alcance, criterio de terminado, estimación en horas y trazabilidad al CA o escenario BDD de la historia. |
| — | [Contratos v1](docs/contracts/) | OpenAPI de HTTP y AsyncAPI de eventos Kafka. |
| — | [Suite API Gateway y Service Discovery](docs/gateway-y-discovery/README.md) | 🚪 **Borde y ruteo (Tema 01)**: Guía de 7 documentos sobre arquitectura de red, Eureka, M2M con tokens técnicos, filtros WebFlux RS256 y resiliencia |

> **El número 16 queda reservado y el índice salta del 15 al 17 a propósito.** Renumerar
> el 17, el 18 y el 19 para tapar el hueco haría que el mismo número signifique documentos
> distintos en el historial.

## Lo demás que hay en el repositorio

| Carpeta | Quién lo hizo | Qué hay adentro |
|---|---|---|
| **[`docs/importado/`](docs/importado/)** | `Brf93` (`421562`) | Anidado adentro de `docs/`, para que la raíz tenga una sola carpeta de documentación. 25 documentos: especificación técnica, nueve planes de ejecución e investigación sobre jailbreak. Material de profundización, no de trabajo diario |
| **[`llm-workbench/`](llm-workbench/)** | el equipo | Frontend docente en Angular 21 (evaluador de uso de IA, rúbricas, golden set y calibración). Se levanta con Docker Compose (`llm-service/compose.workbench.yaml`) o localmente vía Angular CLI |
| **[`docs/presentaciones/`](docs/presentaciones/)** | base de `Brf93`, más `412181-HerediaLara` y equipo | [`defensa-39-slides.html`](docs/presentaciones/defensa-39-slides.html), el deck de defensa (39 slides) · [`presentacion-integracion-servicios.html`](docs/presentaciones/presentacion-integracion-servicios.html), deck interactivo de integración y contratos (17 slides) · [`prd-wiki-consulta.html`](docs/presentaciones/prd-wiki-consulta.html), el PRD como wiki de consulta. Más tres documentos HTML armados sobre `doc-tpi-unificada` —mapa de requerimientos, guía del golden set e informe de gestión de modelos—: venían desalineados con `docs/` en ocho puntos y **ya están corregidos**, con el registro de qué se cambió en [`CORRECCIONES-SUGERIDAS.md`](docs/presentaciones/CORRECCIONES-SUGERIDAS.md). Las seis abren sin internet |
| **[`docs/fuentes/`](docs/fuentes/)** | cátedra | PDFs oficiales del PRD y propuesta de arquitectura del TPI (excluidos de Git) |

> ### Sobre el material que vino de otras ramas
>
> `docs/importado/` se importó **sin tocar una sola línea**: cada archivo es byte a byte idéntico
> a su rama de origen, y lo que había que corregir está anotado, no aplicado, en su
> `CORRECCIONES-SUGERIDAS.md`.
>
> **🟢 2026-09-12 — `codigo-ejemplo/` se consolidó dentro de `llm-service/` y se eliminó.** Los
> dos proyectos de referencia (`ms-evaluacion-llm/`, `lara-heredia-demo-llm-spring-ai/`) ya
> cumplieron su función: lo que servía se portó a `llm-service/domain/ai/` e
> `infrastructure/ai/` (puerto de invocación de modelos + guardarraíles del tutor — ver
> [`docs/estado-implementacion/ep-02/h10.md`](docs/estado-implementacion/ep-02/h10.md) y
> [`ep-05/interactions.md`](docs/estado-implementacion/ep-05/interactions.md)). El análisis
> original y los artefactos no-código que otros docs seguían citando (`ESTRUCTURA.md`,
> `TESTING.md`, `pom.xml`, `pmd-ruleset.xml`) quedaron preservados, sin editar, en
> [`docs/estado-implementacion/codigo-ejemplo/fuentes/`](docs/estado-implementacion/codigo-ejemplo/fuentes/).
>
> **Los tres HTML nuevos de `docs/presentaciones/` son la excepción:** venían desalineados con `docs/` en
> ocho puntos —ADR, ADR-005 con FastAPI, endpoints, deriva, costos, catálogo de modelos, enlaces y
> dependencia del CDN— y se corrigieron. El detalle está en
> [`docs/presentaciones/CORRECCIONES-SUGERIDAS.md`](docs/presentaciones/CORRECCIONES-SUGERIDAS.md). **Había una API key de Groq versionada en la demo
> (`lara-heredia-demo-llm-spring-ai`) que seguía necesitando rotarse en Groq** — el archivo ya no
> está en el working tree, pero la clave puede seguir en el historial de git, así que la rotación
> en el proveedor sigue siendo necesaria. Detalle preservado en
> [`docs/estado-implementacion/codigo-ejemplo/fuentes/CORRECCIONES-SUGERIDAS.md`](docs/estado-implementacion/codigo-ejemplo/fuentes/CORRECCIONES-SUGERIDAS.md).

> ### 📝 Sobre el contenido de ejemplo
>
> Varios documentos traen anclas, prompts, transcripciones y números **de ejemplo**, para que el
> mecanismo se entienda y para no arrancar de cero. **Nada de eso es definitivo.**
> El inventario completo —los 31 ítems, qué marca tiene cada uno, quién lo define y cuándo— está en
> [08 · Decisiones y pendientes](docs/08-decisiones-y-pendientes.md), **Parte C**.

### Por dónde entrar

- **Para entender antes que implementar** → [09 · Preguntas y respuestas](docs/09-preguntas-y-respuestas.md).
  Es el razonamiento en lenguaje llano y el mejor material para la defensa.
- **Para empezar a trabajar** → [23 · Plan vigente](docs/23-plan-construccion-producto-llm.md)
  (capacidad en §2, DoR/DoD en §9.2), [35 · backlog ejecutable](docs/35-backlog-ejecutable.md)
  (épicas, Sprint 0 e historias S1), [36 · playbook de construcción](docs/36-playbook-de-construccion.md),
  [plantilla de sprint](docs/plantillas/sprint-llm.md) y [11 · Glosario y metadata](docs/11-glosario-y-metadata.md).
- **Para la sesión de integración** → [08 · Decisiones y pendientes](docs/08-decisiones-y-pendientes.md), parte B.
- **Para decidir modelos** → [03 · Modelos y costos](docs/03-modelos-costos-y-contexto.md).

## Los hallazgos que ordenan todo

1. **Nuestro alcance oficial es más angosto de lo que asumíamos.** El Tema 07 son seis cosas:
   rúbrica, invocación del modelo, golden set, calibración, bloqueo de activación y salvaguarda
   anti-fuga. **El RAG, el tutor y el generador no están asignados a ningún equipo.**

2. **Pero la salvaguarda anti-fuga nos pone en el camino del tutor.** Corre sobre la respuesta del
   tutor justo antes de que el alumno la vea: **no se puede ser dueño de ese guardarraíl sin estar
   ahí.** Y sin tutor no hay transcripción, así que el evaluador se queda sin insumo.

3. **El costo es de USD 5 a 22 por cuatrimestre.** Y la palanca principal **no es qué modelo elegís,
   es cuántos tokens le mandás**: recortar el contexto del tutor de 6.000 a 3.000 ahorra más que
   cambiar de modelo, sin costar calidad.

4. **Buena parte de la rúbrica se puede calcular con código**, no con un modelo: entre el 45% y el
   60% del score. Y no por ahorro — por **reproducibilidad, inmunidad a injection, auditabilidad y
   ausencia de deriva**.

5. **RF-IA-20 mata el streaming token a token** en desafíos prácticos: no se puede bloquear una
   respuesta que el alumno ya está leyendo.

6. **El golden set es el ítem de plazo más largo y no depende de nadie técnico.** Sin calibración
   aprobada, **ningún curso pasa de borrador a activo** — sin override, ni de ADMIN.

7. **La herramienta de carga del golden set tiene que existir antes** de que ese trabajo docente
   pueda empezar. Parece "una pantalla de admin más" y en realidad destraba el camino crítico.

## Diagramas

- [Arquitectura interna](https://claude.ai/code/artifact/3289c8d8-1ecb-45b6-8ec8-f064b70089c5) —
  los 8 módulos, cómo se conectan los modelos, el reparto entre 6
- [Construcción del Tema 07](https://claude.ai/code/artifact/ddc1b820-b1aa-4dc2-ad20-aad24af54ca9) —
  los pasos en 4 semanas, con dependencias
- [Java o Python](https://claude.ai/code/artifact/6bda78ed-698b-48dd-aa10-3268c107be13) —
  comparación capa por capa de los dos stacks
- [Pantalla del golden set](https://claude.ai/code/artifact/0854689d-1746-486e-b30f-a9cdced0a2d2) —
  mockup con las 7 decisiones a debatir

Los documentos incluyen además diagramas Mermaid, que GitHub renderiza directamente. **Los nueve
que cruzan endpoints, latencias, modelos y dependencias están juntos en
[17 · El mapa de integración](docs/17-mapa-de-integracion.md)** — es el documento para llevar a la
sesión de integración.

## Lo que bloquea hoy

| Qué | Quién decide |
|---|---|
| 🔴 Golden set: responsable con nombre y fecha | Product Owner |
| 🔴 Free tier y datos de alumnos | Consulta legal |
| 🔴 Tema 05: cómo accedemos a la solución esperada | Sesión de integración |
| 🔴 Tema 11: nuestros campos en el contrato de eventos, **antes de que lo cierren** | Sesión de integración |
| 🟡 Alcance: ¿RAG, tutor, generador y corrector son nuestros? | Sesión de integración |

Detalle y recomendación de cada uno en [08 · Decisiones y pendientes](docs/08-decisiones-y-pendientes.md).

## Decisiones cerradas

| Decisión | Fundamento |
|---|---|
| **Java Spring Boot** para el servicio | La materia es de Java y la integración con Spring Cloud pesa más que el ecosistema de IA de Python. [Detalle](docs/02-arquitectura-y-stack.md) |
| **Un microservicio, no cinco** | Las cinco funciones comparten gateway, guardarraíles, cuotas y log. Separarlas multiplica la maquinaria transversal |
| **Sin orquestador basado en LLM** | La ruta la sabe la UI. Un router agrega latencia, costo, un punto de falla y una superficie de injection |
| **Sincrónico solo para tutor y moderador** | El resto va por cola: Batch al 50%, RF-IA-27 implementado por construcción, y el pico absorbido |
| **langchain4j en los adapters de LLM** | API uniforme por proveedor y salida estructurada, sin atar el servicio a un SDK. La capa de agentes de la librería no se usa ([ADR-016](docs/08-decisiones-y-pendientes.md)) |
| **Bus de eventos del Tema 11: Kafka** | La cola interna es aparte (Postgres/Redis): Kafka no tiene prioridades por mensaje ni DLQ, y las dos hacen falta |
| **La solución de referencia nunca entra al contexto del tutor** | No se puede filtrar lo que no se tiene |
| **El perímetro temático lo hace cumplir el retrieval, no el prompt** | Una instrucción se sortea hablando; un filtro en el servidor no |

## Fuentes

- `fuentes/PRD-Plataforma-Gamificada-TP.pdf` (v2.1) — definición funcional del producto
- `idea.pptx.pdf` — convenciones obligatorias de Gateway, Eureka, seguridad, ruteo y pruebas
- `fuentes/TUP_PIV_BE_PROPUESTA_ARQ.pdf` — propuesta de arquitectura de la cátedra
- `fuentes/TUP_PIV_FE_TEO_U1_ARQUITECTURA_DESPLIEGUE.pdf` — teórico de Front End, Unidad 1: arquitectura y
  despliegue. Sincronizado en [15](docs/15-sincronizacion-arquitectura-y-despliegue.md)

> Los PDF de origen no se versionan en este repositorio (carpeta `fuentes/`, gitignored). Se
> distribuyen por los canales de la cátedra.

## Cómo se armó este repositorio

Cuatro ramas consolidadas en una sola estructura:

| Rama | Autor | Qué aportó |
|---|---|---|
| `main` | — | La base de los quince documentos originales |
| `feat/qa-gate` | `facundosoria` | Los documentos evolucionados, el esqueleto del microservicio y la demo interactiva |
| `lara` | `412181-HerediaLara` | La demo funcional de Spring AI, y dos de las presentaciones |
| `doc-tpi-unificada` | `Brf93` (`421562`) | Los 25 documentos de `docs/importado/` y la presentación que quedó como base del deck |

La autoría de cualquier archivo sale del historial, que es la fuente que no se
desactualiza:

```bash
git log --format='%an  %ad  %s' --date=short -- <ruta>
```

Donde el mismo documento existía dos veces, **quedó la versión más nueva**. Quince
archivos de `doc-tpi-unificada` eran copias byte a byte de versiones anteriores de
`docs/01` a `docs/15` y se descartaron; la tabla de equivalencias, por si alguien busca
uno por el nombre viejo, está en
[`docs/importado/CORRECCIONES-SUGERIDAS.md`](docs/importado/CORRECCIONES-SUGERIDAS.md).

---

*Documentación viva. Última actualización: 2026-09-02.*
