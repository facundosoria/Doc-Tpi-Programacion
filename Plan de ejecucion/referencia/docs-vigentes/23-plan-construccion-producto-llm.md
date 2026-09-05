# 23 — Plan de construcción del producto LLM

> **Planificación vigente acordada el 2026-09-04.** Equipo de 10 integrantes, capacidad
> efectiva de 5 personas a 5 horas diarias, tres fases y **19 sprints de dos semanas**.
> Cada sprint termina con un incremento funcional. Las reuniones consumen capacidad.

Esta guía permite al equipo organizar el desarrollo de `llm-service` desde el estado actual
hasta el producto completo del alcance acordado. Reemplaza el calendario y reparto anteriores
de [10](10-entregables-y-plan.md) y [20](20-backlog-y-sprints.md), que se conservan como
antecedentes. No reemplaza el PRD, las [convenciones](00-fuentes-de-verdad-y-convenciones.md)
ni los contratos inter-equipos. La cobertura funcional se consulta en
[21](21-matriz-trazabilidad-llm.md).

El calendario de este documento se ejecuta mediante el [playbook para agentes](../Plan%20de%20ejecucion/06-playbook-de-construccion.md)
y el [backlog ejecutable de S1–S19](../Plan%20de%20ejecucion/07-backlog-ejecutable-sprints.md).
Allí están el orden atómico de construcción, los gates de dependencia, el presupuesto por
paquete, las pruebas y la evidencia exigida; las tablas de fases de este documento son el mapa
de producto, no el nivel de detalle de implementación.

**Estado:** plan de trabajo; los sprints no están ejecutados ni sus integraciones aprobadas.
El código de ejemplo requiere adaptación antes de usarse como producto. El comienzo calendario
se fija en la primera Planning: las semanas de este documento son relativas, no fechas comprometidas.

## 1. Alcance y entregas

El alcance es el servicio LLM, sus componentes de interfaz en el frontend compartido y sus
integraciones. Los otros equipos conservan la propiedad de identidad, cursos, desafíos, XP,
chat y notificaciones; este plan no incluye reconstruir esos servicios.

| Fase | Sprints | Semanas relativas | Resultado de salida |
|---|---|---|---|
| **F1 · Tutoría y evaluación académica** | S1–S10 | 1–20 | MVP académico: tutor seguro, evaluación, calibración, revisión y operación integradas. |
| **F2 · Moderación integrada** | S11–S13 | 21–26 | Mensajes moderados, incidentes revisables y retención/recuperación coordinadas con chat. |
| **F3 · RAG y asistencia personalizada** | S14–S19 | 27–38 | Material docente consultable, desafíos personalizados, agente por mención y operación completa. |

Se excluyen **el corrector LLM de respuestas abiertas y el generador de parciales**. El generador
de desafíos personalizados sí pertenece a F3: utiliza formatos que el motor existente puede
corregir. El idioma inicial es español; la comparación estructural inicial de código es Java.

Un incremento funcional permite a su usuario terminar el caso de uso comprometido, con interfaz
necesaria, persistencia, permisos e integración reales. Cada incremento conserva lo entregado
antes. Un contrato, una base vacía, un adaptador aislado o un mock no constituyen por sí solos
el entregable del sprint.

Las entregas previas al cierre de F1 pueden utilizarse en el ambiente compartido de validación.
Su existencia no habilita un curso real: el uso académico exige todos los controles de salida
de F1. Las versiones mayores de producto y las versiones de contrato son conceptos distintos;
completar una fase no obliga a romper o renumerar la API.

## 2. Capacidad y presupuesto de reuniones

### 2.1 Cálculo inicial

La disponibilidad efectiva ya contempla otras responsabilidades. Se cuentan horas-persona,
incluidos fines de semana, sin presuponer presencia simultánea de los diez integrantes.

| Concepto | Cálculo | Horas-persona por sprint |
|---|---|---:|
| Disponibilidad nominal | 5 personas efectivas × 5 h × 14 días | **350** |
| Reuniones programadas | Ver tabla siguiente | **−90** |
| Capacidad después de reuniones | 350 − 90 | **260** |
| Reserva | 20% de 260 | **−52** |
| **Máximo inicial para entregables** | 260 − 52 | **208** |

Las **208 h** incluyen análisis técnico, implementación, pruebas, revisión de código, integración,
documentación y despliegue. Las **52 h** cubren preparación de reuniones/demos, actas, mantenimiento
del tablero, coordinación asincrónica, imprevistos y soporte no previsto. No son capacidad adicional
para comprometer funcionalidades. El soporte conocido se descuenta antes de calcular la reserva.

La reserva genérica anterior de 105 h queda reemplazada por este desglose, no sumada a él.
El horizonte de 19 sprints procede de conservar el presupuesto previo de entregables:

```text
Presupuesto previo: 16 × 245 = 3.920 horas-persona
Horizonte inicial: techo(3.920 / 208) = 19 sprints = 38 semanas
Presupuesto nuevo: 19 × 208 = 3.952 horas-persona para entregables
```

Es una normalización del presupuesto, **no una estimación de abajo hacia arriba de todas las
historias** ni una promesa de fecha. No se convierten automáticamente los puntos del backlog
histórico en horas. Al cerrar S2 se revisa la previsión con capacidad y trabajo terminado reales.

### 2.2 Reuniones programadas

| Reunión | Cantidad por sprint | Duración por reunión | Participantes internos | Horas-persona |
|---|---:|---:|---:|---:|
| Sprint Planning | 1 | 120 min | 10 | **20** |
| Daily / sincronización | 4, dos por semana | 45 min | 10 | **30** |
| Sprint Review con demo | 1 | 90 min | 10 | **15** |
| Retrospectiva | 1 | 60 min | 10 | **10** |
| Refinamiento del backlog | 2, uno por semana | 30 min | 10 | **10** |
| Coordinación de dependencias | 2, una por semana | 30 min | 5 representantes | **5** |
| **Total** | | | | **90** |

Las duraciones, salvo las sincronizaciones solicitadas de 45 minutos, son valores iniciales de
planificación. La demo está incluida en la Review; no se vuelve a descontar como otra reunión.
Los docentes y consumidores externos pueden asistir a la Review, pero sus horas no forman parte
de las 350 h del equipo. Si participa más gente del equipo en coordinación, se registra ese costo.

La cadencia acordada de dos sincronizaciones semanales de 45 minutos es una adaptación del
equipo. La Daily Scrum formal dura 15 minutos por día de trabajo y la Planning inicia el sprint;
el refinamiento es una actividad continua y la coordinación técnica no es un evento formal
adicional de Scrum. Referencia: [Guía Scrum](https://scrumguides.org/scrum-guide.html).

### 2.3 Agenda quincenal

| Momento | Actividad | Salida verificable |
|---|---|---|
| Antes de comenzar la ejecución | Planning | Objetivo, caso de uso, capacidad individual, historias, responsables, dependencias y aceptación. |
| Martes y jueves de cada semana | Sincronización de 45 min | Avance hacia el objetivo, impedimentos con dueño y próximos pasos. |
| Una vez por semana | Refinamiento de 30 min | Historias futuras entendidas, divididas y con criterios de aceptación. |
| Una vez por semana | Coordinación de dependencias de 30 min | Contratos y compromisos externos actualizados con fecha y responsable. |
| Cierre quincenal | Review con demo de 90 min | Incremento probado, feedback de usuarios/consumidores y backlog actualizado. |
| Después de la Review | Retro de 60 min | Una mejora accionable con dueño y capacidad para el siguiente sprint. |

La **Planning se carga al sprint que prepara**. Si se hace el día previo al inicio operativo,
sus 20 horas-persona se descuentan igualmente de ese sprint. No agrega un día gratuito al ciclo,
ni se carga nuevamente al sprint anterior. Esto también aplica a la primera Planning.

En las sincronizaciones se dedica el tiempo a avance, bloqueos y coordinación; una discusión
especializada continúa con los participantes necesarios y su costo se registra. No se espera a
la próxima reunión para comunicar un bloqueo. No hay una tercera reunión semanal implícita.

Reuniones extraordinarias, preparación o soporte que consuman la reserva reducen el margen
restante; si lo superan, se renegocia el alcance antes de comprometer horas inexistentes.

### 2.4 Recalcular en cada Planning

```text
Disponibilidad = suma de horas declaradas por los 10 integrantes
Reuniones = suma(cantidad × duración en horas × participantes internos)
Base = disponibilidad − reuniones − soporte conocido
Capacidad comprometible = máximo(0, Base × 0,80)
```

La referencia inicial por pareja es **41,6 h por sprint** para entregables, no 208 h para cada
pareja. Se ajusta por disponibilidad real. Una persona en una reunión o trabajando en pareja
no puede registrar simultáneamente esas horas en otra tarea. Una ausencia no se compensa
presuponiendo horas extra. Si la base es negativa, se replanifica también la agenda.

## 3. Equipo, responsabilidades y forma de trabajo

Se trabaja como **un equipo con un backlog y un objetivo por sprint**. Las parejas aportan
continuidad y referentes, no son silos ni equipos con objetivos independientes.

| Pareja | Responsabilidad principal | Evolución |
|---|---|---|
| **P1 · Plataforma e integración** | Contratos, infraestructura compartida, migraciones transversales, Kafka, CI/CD y operación. | Acompaña las tres fases. |
| **P2 · Modelos y resiliencia** | AI Gateway, adaptadores, cuotas, validación de salidas, configuración, costos y fallas. | Añade capacidades para moderación, extracción visual y agentes. |
| **P3 · Asistencia y seguridad** | Tutor, guardarraíles e interfaz de asistencia. | Moderación y agente por mención. |
| **P4 · Evaluación y auditoría** | Rúbrica, evaluador, calibración, revisión humana y calidad. | Corpus de moderación y validación de personalización/RAG. |
| **P5 · Contenido y conocimiento** | Golden set e interfaces docentes. | Ingesta, RAG y desafíos personalizados. |

Cada pareja implementa las pruebas, endpoints, migraciones y componentes de interfaz de su
funcionalidad. P1 revisa lo transversal; no es el único autor de todos los controllers o migraciones.
Las revisiones de cambios contractuales incluyen al consumidor afectado. El resto del equipo
puede apoyar a una pareja sin cambiar la responsabilidad del entregable.

En la primera Planning se anotan nombres, suplencias, referente de producto y facilitador de
ceremonias. Los identificadores P1–P5 no suponen personas ya asignadas. Si PO o facilitador son
integrantes, su dedicación consume la capacidad existente; no se añaden recursos ficticios.

Los PR se mantienen pequeños y se integran durante el sprint. Un cambio de contrato se publica
con sus pruebas; un cambio de dato tiene migración versionada y revisión del dueño del módulo.
Los controles de calidad no se posponen al sprint de cierre de fase.

## 4. Base técnica e interfaces

| Área | Base elegida para implementar |
|---|---|
| Aplicación | Java 21, Spring Boot 3.x y Maven; `spring.application.name=llm-service`. |
| HTTP | Spring Web, Bean Validation, Jackson; rutas `/api/llm/**`, Problem Details y `requestId`. |
| Red e identidad | API Gateway, Eureka, tokens M2M con scopes y autorización funcional dentro del servicio. |
| Datos | PostgreSQL propio, Spring Data JPA/JDBC y Flyway. |
| Trabajos internos | Tabla durable en PostgreSQL; toma concurrente con `FOR UPDATE SKIP LOCKED`, recuperación y reintentos; worker de la misma aplicación. |
| Eventos externos | Kafka, outbox transaccional y deduplicación de eventos. |
| Modelos | Adaptadores detrás del AI Gateway interno, cliente `RestClient`, asignación por función y Resilience4j. |
| RAG | pgvector y embeddings locales con ONNX Runtime; PDFBox/Tika, Tess4J y extracción visual mediante AI Gateway. |
| Objetos | MinIO compartido; originales bajo responsabilidad del servicio dueño, referencias y hashes en LLM. |
| Interfaz | Componentes en el frontend Angular compartido, dentro de sus convenciones de versión y navegación. |
| Calidad y operación | JUnit, Mockito, Testcontainers, WireMock, Actuator, Micrometer y Docker. |

La cola interna en PostgreSQL sigue la revisión de [06](06-operacion-e-ingenieria.md), Parte 6.
No se confunde con Kafka ni se exige Redis para comenzar. Versiones exactas y compatibilidad del
BOM se fijan en S1 según la plataforma. El modelo concreto se habilita por evidencia de calidad;
el catálogo histórico de nombres/precios no acredita disponibilidad ni calibración.

### 4.1 Trabajo contractual

Partir de [OpenAPI v1](contracts/llm-service-v1.openapi.yaml) y
[AsyncAPI v1](contracts/llm-service-v1.asyncapi.yaml). Sus respuestas y payloads incompletos se
cierran con los consumidores, no se rellenan silenciosamente en una implementación particular.

| Momento | Operaciones y contenido a completar o acordar |
|---|---|
| S1, antes de los consumidores afectados | Consultas de golden set, respuesta de creación, transcripción estructurada, identidad/ownership, Problem Details y respuestas de calibración. Publicar mocks para integración temprana. |
| S2–S4 | Puntuación docente y publicación de versiones; calibración de plataforma separada de la de curso; consulta de reportes y habilitación de modelo. |
| Antes de S5–S7 | Contexto del tutor y canal separado de solución esperada; metadata; evidencia de indisponibilidad; justificaciones; pendientes; apelaciones/overrides y propagación de revisión. |
| S9–S10 | Contrato de moderación con chat, incidentes, contexto, notificación, revisión y retención. |
| S12–S13 | Ingesta por referencia autorizada, estado de trabajo, reporte de calidad, versiones y fuentes. |
| S15–S17 | Generación personalizada y entrega al motor; mención, respuesta del agente y retención. |

Las nuevas operaciones son **ampliaciones a acordar**, no endpoints ya vigentes. Se conservan
los campos y consumidores válidos; cambios incompatibles requieren versión y coexistencia.
Los enums nuevos también se revisan con consumidores que puedan tratarlos como cerrados.

El body/path contiene referencias a recursos que deben autorizarse; no acredita la identidad.
No se supone que toda cohorte proviene del JWT. Se propagan `traceparent` y `X-Request-Id` por
HTTP y headers Kafka. Se respeta `Idempotency-Key` y la deduplicación por identificadores estables.

### 4.2 Invariantes del producto

- Un único modelo evaluador activo, sin fallback automático ni recálculo histórico al cambiarlo.
- Habilitación de plataforma y calibración por curso separadas; ninguna admite sustituir la otra.
- Pesos: autonomía 30%, claridad 25%, progresión 20%, cumplimiento 15%, eficiencia 10%.
- Rúbrica portable; un cambio conserva evidencia y versiones de resultados anteriores.
- La solución de referencia permanece fuera del prompt; la salida se valida antes de mostrarla.
- Tutor indisponible implica tratamiento neutro de IA; evaluador indisponible implica cálculo diferido.
- La entrega no se bloquea por caída de IA. Negocio aplica XP y monedas, LLM entrega evidencia/resultados.
- El cierre del curso se bloquea mientras existan scores diferidos pendientes.
- La interfaz interna de contexto se define en S5: contexto validado de práctica primero, RAG en S14.
- Apelaciones y overrides son auditables y preservan el original. La retención académica no se mezcla con la rotación de logs operativos.

## 5. Fase 1 — Tutoría y evaluación académica

**S1–S10 · Semanas 1–20 · MVP académico completo.** Los liderazgos de la tabla no excluyen el
apoyo de otras parejas; el total comprometido por sprint sigue limitado a 208 h inicialmente.

| Sprint | Entregable funcional y demo de aceptación | Trabajo incluido | Liderazgo / dependencia |
|---|---|---|---|
| **S1** | Un docente autorizado carga y consulta casos de referencia; los datos sobreviven al reinicio. | Alinear esqueleto, arranque reproducible, Gateway/Eureka, base y migraciones, CI, contratos mínimos, rúbrica e interfaz de golden set. | P1 + P5; P4 prepara rúbrica. Identidad y frontend compartido disponibles en los primeros días. |
| **S2** | Dos docentes puntúan independientemente, resuelven discrepancias y publican una versión trazable que pueden exportar y recuperar. | Puntuaciones independientes, consolidación, versiones y recuperación de golden set; referencias humanas. | P5 + P4; depende de S1 y docentes. |
| **S3** | ADMIN calibra un modelo real y consulta el reporte; no puede habilitarlo si queda fuera de tolerancia. | AI Gateway, evaluador, schema de salida, trabajos persistentes, calibración base, habilitación y registro de costo/versiones. | P2 + P4; depende de S2 y disponibilidad del proveedor. |
| **S4** | Un docente calibra su curso; cursos bloquea activación sin aprobación y emite los avisos acordados. | Set de curso, adaptación al dominio sin alterar pesos, runner reutilizado, consulta de estado e integración con cursos/notificaciones. | P4 + P5 + P1; depende de S3 y material docente del curso. |
| **S5** | El alumno usa el tutor seguro dentro del desafío y puede continuar sin asistencia ante caída, con evidencia de esa condición. | Contexto, historial/metadata, interfaz, riesgo, cuotas, entrada y anti-fuga; canal separado para solución; tratamiento de indisponibilidad. | P3 + P2; contexto, solución y actividad del intento disponibles. |
| **S6** | Cerrar un intento genera un score visible; con el evaluador caído se acepta la entrega y se completa después sin duplicados. | Kafka, workers, resultados/diferidos, recuperación, desglose, consulta de pendientes y bloqueo de cierre con cursos; efectos en negocio. | P4 + P1; depende de S3–S5 y desafíos/cursos. |
| **S7** | El alumno apela y el docente resuelve viendo la evidencia; el original permanece y negocio recibe la revisión. | Apelación, override append-only, bandeja, baja confianza, muestreo y prioridad por impacto en umbrales informado por negocio. | P4 + P5; depende de S6. |
| **S8** | ADMIN cambia a un modelo habilitado y ve cohortes afectadas; la recalibración por período/versión produce reportes y alertas. | Portabilidad, segundo adaptador para comprobarla, cambio auditado, deriva, historial y señalización; sin recálculo histórico. | P2 + P4; depende de S3, S4 y S6. |
| **S9** | ADMIN consulta costos, cuotas y fallas y modifica límites; el cambio se aplica y queda auditado. | Panel y consultas operativas, configuración de límites y trazabilidad. Las cuotas básicas ya funcionan desde S5. | P2 + P5; consume instrumentación de S3–S8. |
| **S10** | Un operador identifica trabajos detenidos y recupera los recuperables sin duplicar resultados; se demuestra el recorrido bajo carga. | Operación de pendientes, alertas, restauración, despliegue/rollback, 120 sesiones concurrentes y guía operativa. | P1 + todas; depende de los incrementos anteriores. |

### Salida de F1

- [ ] Golden sets humanos base y de curso completos para las cohortes que salen a producción.
- [ ] Modelo habilitado a nivel plataforma y calibraciones de curso aprobadas dentro de PAR-14.
- [ ] Activación sin calibración rechazada incluso para ADMIN.
- [ ] Tutor seguro, score desglosado y revisión humana funcionando desde la interfaz real.
- [ ] Caída de tutor y evaluador verificadas con sus tratamientos académicos respectivos.
- [ ] Entrega aceptada y cierre bloqueado con pendientes, comprobados con los servicios dueños.
- [ ] Pruebas de 120 sesiones, restauración y operación completadas.

El golden set docente es un hito de calendario propio desde S1. Un set sintético sirve para
desarrollar y probar, pero no reemplaza la aprobación académica exigida para salir a producción.

## 6. Fase 2 — Moderación integrada

**S11–S13 · Semanas 21–26.** Chat, entrega de mensajes y notificaciones siguen siendo de sus
equipos. La integración real se prepara en S9–S10.

| Sprint | Entregable funcional y demo de aceptación | Trabajo incluido | Liderazgo / dependencia |
|---|---|---|---|
| **S11** | Los mensajes del chat real se permiten o bloquean antes de entregarse; se ven incidentes y se notifican los graves. | Contrato, detectores clásicos, clasificador contextual, categorías/severidad, feedback e incidentes. | P3 + P2 + P1; requiere chat/notificaciones y política acordada. |
| **S12** | El usuario apela un bloqueo y el docente revisa mensaje, contexto y reiteración; la resolución queda auditada y se refleja en chat. | Revisión humana, contexto inmediato autorizado, señales de reiteración, evidencias y pruebas de falsos positivos. | P3 + P4 + P5; depende de S11 y contexto del hilo. |
| **S13** | Ante caída se aplica la política acordada y se recuperan revisiones; archivar preserva las evidencias requeridas. | Capa clásica durante caída, remoderación/retirada cuando corresponda, retención por incidente, purga selectiva y métricas. | P1 + P3 + P4; depende de S11–S12 y archivado del chat. |

**Decisión de producto a ratificar en S10:** ante caída del clasificador, mantener la capa clásica,
permitir solo lo que esta no bloquee y marcarlo para remoderación; ante pérdida total de moderación,
mantener el envío pendiente sin simular aprobación. Se registra la aprobación y el contrato con
chat antes de S11. Esta política propuesta no se presenta como requisito ya cerrado del PRD.

### Salida de F2

- [ ] Moderación previa a entrega, feedback y notificaciones comprobados en chat real.
- [ ] Apelaciones, contexto y decisiones auditables.
- [ ] Corpus de categorías y falsos positivos evaluado; umbrales aprobados por producto.
- [ ] Caída, recuperación y remoderación verificadas bajo la política acordada.
- [ ] Archivado selectivo retiene evidencias necesarias y respeta la propiedad de cada dato.

## 7. Fase 3 — RAG y asistencia personalizada

**S14–S19 · Semanas 27–38.** El primer recorrido RAG completo se entrega en **S14**; S15 amplía
formatos y S16 completa el ciclo de vida del material.

| Sprint | Entregable funcional y demo de aceptación | Trabajo incluido | Liderazgo / dependencia |
|---|---|---|---|
| **S14** | Un docente incorpora un PDF de texto y el alumno consulta con fuente/página; sin respaldo el asistente se abstiene. | Referencia autorizada, ingesta, chunks, embeddings locales, pgvector, filtro de cohorte, citas y conexión al tutor. | P5 + P3; P1/P2 apoyan; material y acceso a objetos preparados. |
| **S15** | El docente incorpora escaneos/diagramas, revisa la extracción y el alumno consulta su contenido con citas; las páginas fallidas se identifican. | OCR, extracción visual por AI Gateway, reporte de calidad y pruebas sobre material real. | P5 + P2 + P4; depende de S14. |
| **S16** | El docente reemplaza o retira material sin mezclar versiones; búsquedas y cachés dejan de usar fuentes retiradas. | Estado de ingesta, reingesta, activación atómica de versión, invalidación de caché y archivado. | P5 + P1; depende de S14–S15. |
| **S17** | El alumno solicita, resuelve y entrega un desafío personalizado; negocio aplica la recompensa sin duplicarla. | Generación asíncrona fundada en RAG, dificultad, cuotas, validación e interfaz; motor existente corrige formatos soportados. | P5 + P4 + P1; depende de S14–S16 y motor de desafíos. |
| **S18** | El alumno menciona a `@agente` y recibe respuesta fundamentada en el canal; sin mención no actúa. | Un agente inicial por curso, contexto autorizado, RAG, cuotas, moderación de salida, anti-fuga, retención y prevención de bucles. | P3 + P5 + P2; depende de F2 y S14–S16. |
| **S19** | ADMIN supervisa las funciones y recupera ingestas/trabajos; el recorrido completo funciona tras restauración y despliegue. | Panel consolidado, reproceso controlado, backup de datos/índices, carga combinada, regresión, rollback y transferencia operativa. | P1 + todas; depende de todas las entregas. |

### Salida de F3 y del producto acordado

- [ ] Material textual, escaneado y visual consultable con fuentes autorizadas y versiones correctas.
- [ ] Sin recuperación entre cohortes y con abstención ante falta de respaldo.
- [ ] Desafíos personalizados resolubles/corregibles por el motor y con efectos idempotentes.
- [ ] Agente invocado explícitamente, moderado y con retención diferenciada.
- [ ] Recorridos de las tres fases pasan sin integraciones simuladas pendientes en el alcance entregado.
- [ ] Recuperación, contratos, manuales, evidencias y responsables de operación disponibles.

## 8. Dependencias y prevención de bloqueos

| ID | Dependencia | Referente LLM | Preparación y disponibilidad requerida | Entrega protegida |
|---|---|---|---|---|
| D01 | Identidad, Gateway, Eureka, ambiente y frontend compartido. | P1 | Primeros días de S1. | S1 |
| D02 | Docentes responsables, set base y referencias de curso. | P5 + P4 | Nombrar en S1; referencias en S2 y antes de calibrar curso en S4. | S3–S4 |
| D03 | Contexto, solución esperada y metadata de práctica. | P3 | Contrato en S1; integración comprobable antes de S5. | S5 |
| D04 | Cierre de intento, efectos académicos y pendientes. | P1 + P4 | Contrato temprano en S1; integración antes de S6. | S6 |
| D05 | Impacto del score en umbrales académicos. | P4 | Preparación S5–S6 con el servicio dueño del cálculo. | S7 |
| D06 | Chat, notificaciones, contexto, revisión y política de moderación. | P3 + P1 | Preparación S9–S10; política ratificada al cerrar S10. | S11–S13 |
| D07 | Material, permisos, referencias a objetos y archivado. | P5 + P1 | Preparación S12–S13 con cursos y almacenamiento. | S14 |
| D08 | Formatos y corrección de desafíos personalizados. | P4 + P5 | Preparación S15–S16 con desafíos. | S17 |
| D09 | Invocación/publicación del agente y retención. | P3 | Preparación S16–S17 con chat. | S18 |

Los referentes registran nombre del contraparte, fecha, contrato y evidencia; la tabla no afirma
que esos acuerdos externos ya existen. La sesión semanal de coordinación revisa este registro.
Las interfaces de fases futuras pueden diseñarse antes de habilitar funcionalidades de esas fases.

Un mock acordado permite trabajo interno independiente. **No acredita una integración finalizada.**
Si una dependencia incumple, se registra el impedimento, su efecto y responsable; se reordena un
incremento independiente completo cuando exista. Si no existe, se ajusta la previsión en vez de
declarar terminado el sprint o cargar trabajo incompleto en el siguiente sin descontarlo.

## 9. Calidad, aceptación y operación del backlog

### 9.1 Pruebas por área

| Área | Escenarios de aceptación |
|---|---|
| Red/seguridad | Path completo por Gateway; JWT inválido; headers falsificados; aislamiento y autorización de cohorte; dos instancias Eureka; correlación HTTP/Kafka. |
| Evaluación | Cinco dimensiones, pesos y justificaciones; transcripción como datos; versiones y originales conservados; idempotencia de resultados y revisiones. |
| Calibración | Referencias humanas base/curso; PAR-14: desvío promedio del score dentro de ±5 y ninguna dimensión fuera de ±10; candidato y curso rechazados si no pasan. |
| Resiliencia | Tutor indisponible con neutralidad; evaluador diferido; worker interrumpido; reenvío de eventos; recuperación sin doble efecto; cierre bloqueado por pendientes. |
| Tutor | Niveles de riesgo; corpus de jailbreak/pedidos de solución; cero fugas observadas en el corpus; validación antes de mostrar. |
| Moderación | Categorías, severidad, falsos positivos, revisión, contexto, caída/recuperación, remoderación y archivado selectivo. |
| RAG | Página/fuente correctas; aislamiento; abstención; escaneos/figuras; versiones retiradas; `recall@3 > 85%` en conjunto etiquetado. |
| Personalización/agente | Desafío válido y resoluble; efectos en negocio una sola vez; cuotas; mención explícita; salida moderada y sin bucles. |
| Operación | 120 sesiones concurrentes, cuotas reales del proveedor, fallas combinadas, backup/restauración, despliegue y rollback con preservación de trabajos. |

Las pruebas ordinarias usan proveedores simulados. Las pruebas de calidad/calibración del modelo
y las pruebas de carga que dependen de él usan proveedores reales con presupuesto controlado.
Pasar el corpus de seguridad es evidencia de aceptación, no garantía universal de ausencia de fugas.

### 9.2 Definition of Ready

Una historia puede comprometerse si tiene usuario, resultado, aceptación comprobable, requisito,
contrato, responsable/suplente y estimación. Debe caber junto con pruebas e integración en la
capacidad real. Sus dependencias de entrega deben tener evidencia o compromiso externo explícito;
un mock solo habilita las tareas internas que no dependen de una integración real.

Se usan identificadores nuevos `LLM-S01-H01` y siguientes. No se reutilizan ni renumeran los IDs
`E...` del documento 20. Una historia que se reprograma mantiene su ID de origen; su sprint objetivo
se actualiza como campo separado. Puede enlazar una historia histórica como antecedente después
de revisar su compatibilidad con el alcance y contrato actuales.

Las historias futuras se refinan progresivamente; las tareas se dividen preferentemente en una
jornada efectiva o menos para facilitar relevo. No se asignan estimaciones ficticias para completar
un tablero. Al seleccionar historias, se cuenta también el trabajo de integración de cada una.

### 9.3 Definition of Done

1. El usuario termina el recorrido comprometido con interfaz, persistencia y permisos reales.
2. Los servicios consumidores incluidos en el compromiso participan realmente.
3. Pruebas de contrato, fallas, idempotencia, seguridad y regresión aplicables pasan.
4. Las comprobaciones de calidad del modelo/RAG/moderación aplicables tienen evidencia.
5. Código revisado, migraciones y contratos coinciden con la versión desplegada.
6. Existe versión identificable, evidencia de demo/aceptación y procedimiento de recuperación.
7. Documentación y estado del backlog reflejan lo efectivamente entregado.

Una funcionalidad parcial no se presenta como terminada. Si excede capacidad se reduce a un caso
de uso menor completo, con acuerdo de producto, o se ajusta el calendario. Nunca se rebajan
seguridad, pruebas o controles académicos para cumplir una fecha.

### 9.4 Ejecución y revisión de capacidad

- **Planning:** sumar disponibilidad real; descontar reuniones y soporte; reservar 20%; elegir
  entregable; validar dependencias y asignar trabajo sin superar capacidad individual ni total.
- **Primera semana:** poner el primer recorrido real en el ambiente compartido y resolver contratos
  e integración; no reservar esa integración para el final.
- **Segunda semana:** completar variantes/fallas, aceptación y regresión; desplegar y preparar demo.
- **Review:** mostrar resultado; registrar aceptación, feedback, defectos y trabajo no terminado.
- **Retro:** comparar horas reales/planificadas, reuniones, reserva y bloqueos; elegir una mejora
  con responsable y capacidad; actualizar previsión desde S2 y después de cada fase.

El tiempo de soporte previsto y las tareas de la retro entran en la siguiente Planning. El trabajo
arrastrado se reestima por lo que falta y consume capacidad; no se suma gratis al nuevo sprint.

## 10. Inicio operativo y plantilla

Usar [la plantilla de sprint](plantillas/sprint-llm.md) para registrar el compromiso y el cierre.
La guía para quienes se incorporan por primera vez —tecnologías, fases, Docker, pruebas y flujo Git—
está en [Plan de ejecución](<../Plan de ejecucion/README.md>).
Antes de la ejecución de S1, la primera Planning debe dejar:

- [ ] Fecha de inicio, participantes y disponibilidad individual para el ciclo completo.
- [ ] Nombres de P1–P5, suplencias, referente de producto y facilitador.
- [ ] Agenda de las reuniones y presupuesto calculado sin duplicar Planning/Review/demo.
- [ ] D01 y D02 con responsables y estado comprobado; D03/D04 solicitadas con fecha.
- [ ] Historias de S1 que cubran el recorrido completo de carga/consulta de golden set.
- [ ] Adaptación del esqueleto, migraciones, contrato, interfaz y aceptación incluidas en la estimación.
- [ ] Criterio de demo: acceso autorizado, carga, consulta y recuperación tras reinicio.
- [ ] Capacidad reservada para soporte, preparación y acuerdos de integración.

Este documento fija planificación y criterios. Crear issues remotas, asignar personas, coordinar
reuniones externas o desplegar software son acciones posteriores; publicar el plan no las realiza.
