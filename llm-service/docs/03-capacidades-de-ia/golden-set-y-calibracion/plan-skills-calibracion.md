# Plan de skills y calibración — registro de decisiones

> Registro incremental de las definiciones acordadas con Producto para diseñar e
> implementar skills, calibración global por curso y subcalibración por desafío.
> Contiene únicamente decisiones confirmadas y sus implicancias vigentes.

## Corte de alcance del sprint actual — obligatorio

Este plan también conserva decisiones para entregas posteriores. Para el sprint
actual, los siguientes bloques técnicos están **pendientes y fuera de alcance**:

| Punto | Bloque pendiente | Instrucción para implementación |
|---:|---|---|
| 2 | Libro de cuota: reserva atómica, vencimiento, conciliación contra uso real y saldo no expuesto por el proveedor. | No diseñar ni implementar reservas, conciliación, almacenamiento de saldo, políticas de cuota ni cambios de contratos/persistencia para este bloque. |
| 4 | Sanitización de Markdown: pipeline, estados de seguridad, evidencia y bloqueo de versiones comprometidas. | No diseñar ni implementar el pipeline, sus estados, evidencia, alarmas, bloqueos ni cambios de contratos/persistencia para este bloque. |
| 9 | Observabilidad y pruebas: correlación, auditoría, métricas y matriz de escenarios críticos. | No diseñar ni implementar la estrategia de observabilidad, auditoría, métricas ni la matriz/suite de pruebas de este bloque. |

Las decisiones ya registradas que se relacionen con esos puntos se mantienen
como **referencia futura**, pero no son requisitos de este sprint y no deben
generar tareas, dependencias, migraciones, endpoints, eventos, workers ni
criterios de aceptación. Tampoco deben bloquear el diseño o la implementación
de los bloques que sí entren explícitamente en el sprint.

Un agente que lea este plan debe ignorar esos tres bloques al planificar o
modificar el alcance actual. Sólo se reabren mediante una decisión explícita de
Producto o de la planificación de un sprint posterior.

## Decisiones acordadas

### D-01 — Formato inicial y evolución de skills

**Decisión de Producto:** las skills se adjuntan en formato Markdown (`.md`) al
momento de calibrar.

**Decisión de diseño:** modelarlas internamente como artefactos
versionados genéricos (`SkillArtifact` y `SkillAttachment`), aunque la primera
versión sólo admita Markdown. Cada corrida conserva un snapshot inmutable,
versión, orden y hash de las skills aplicadas. Esto permitirá incorporar en el
futuro catálogos institucionales, paquetes firmados, metadatos estructurados o
documentos de contexto sin modificar las calibraciones históricas.

**Guardarraíles:** validar tipo, codificación, tamaño y cantidad; no ejecutar
código adjunto; no registrar contenido sensible en logs.

### D-02 — Catálogo y selección de skills

**Decisión de Producto:** además de adjuntar skills al calibrar, debe existir un
catálogo tipo grilla. El docente puede buscar por nombre o función, filtrar por
curso y ordenar por fecha de subida; puede elegir una o más skills existentes
para agregarlas a la calibración.

**Implicancia de diseño:** el catálogo será un recurso de `llm-service`. Cada
skill tendrá como mínimo nombre, función/descripción, cursos asociados, autor,
fecha, versión, hash, estado y contenido Markdown. La calibración puede combinar
skills recién adjuntadas y skills seleccionadas del catálogo, preservando el
orden elegido.

### D-03 — Visibilidad

**Decisión de Producto:** el docente autor decide, al publicar una skill, si es
`PUBLIC` o `PRIVATE`.

**Implicancia de diseño:** las privadas sólo las ve y usa el autor; las públicas
pueden ser descubiertas y usadas por docentes autorizados. Los filtros de curso
ayudan a descubrir contenido, pero no otorgan acceso a cursos no autorizados.

### D-04 — Versionado, impacto y recalibración

**Decisión de Producto:** editar una skill crea una nueva versión. El sistema
debe informar en qué desafíos fue usada y permitir que el docente recalibre
cursos o desafíos para utilizar la versión nueva. Debe ser resiliente: no puede
bloquear al docente por cambios, pero debe avisarlo con feedback claro.

**Implicancia de diseño:** una versión no modifica configuraciones ni intentos
históricos. La ficha de impacto distinguirá uso histórico, calibración activa y
calibración pendiente. No habrá recalibración automática: se conserva la
configuración activa anterior y el docente inicia una nueva corrida si decide
migrar. Fallas de procesamiento o de dependencias deberán ser recuperables y
ofrecer reintento.

### D-05 — Gestión completa y trazabilidad por desafío

**Decisión de Producto:** skills tendrá una vista de gestión completa fuera de
Calibraciones, manteniendo también el selector rápido dentro del flujo de
calibración. Debe incluir un banco de skills y una página con desafíos listados
junto con las skills usadas en cada uno.

**Implicancia de diseño:** se requerirán vistas para catálogo, búsqueda, filtros,
visibilidad, versiones, impacto y favoritos; y una vista por desafío que muestre
la calibración efectiva y las versiones de skills aplicadas.

### D-06 — Favoritos y nuevas versiones

**Decisión de Producto:** los favoritos se mantienen fijados a la versión que el
docente marcó; no deben actualizarse automáticamente. Si existe una versión
nueva, se informa al docente y se le ofrece quitar el favorito anterior y
migrarlo explícitamente a la nueva versión.

**Implicancia de diseño:** los favoritos son personales y deben referenciar
`skillVersionId`, no sólo la familia de la skill. La migración de favorito es una
acción explícita, reversible y no altera calibraciones existentes.

### D-07 — Precondición de subcalibración por desafío

**Decisión de Producto:** un docente sólo puede crear y activar una calibración
específica de desafío si el curso tiene previamente una calibración global activa
y aprobada.

**Implicancia de diseño:** la interfaz debe bloquear y explicar la
subcalibración hasta cumplir esta condición. El backend la valida de forma
autoritativa tanto al crear como al activar una corrida por desafío; no puede
depender sólo del bloqueo visual.

### D-08 — Herencia editable al subcalibrar

**Decisión de Producto:** al iniciar una subcalibración por desafío, el sistema
precarga la rúbrica, el Golden Set y las skills de la calibración global activa.
El docente puede modificar cualquiera de esos valores antes de iniciar la
corrida.

**Implicancia de diseño:** la vista previa debe diferenciar los valores
heredados de los reemplazados. Al confirmar, se crea una configuración inmutable
propia del desafío: cambios posteriores de la calibración global no modifican
esa configuración ni su historial.

### D-09 — Una activa por alcance e historial reactivable

**Decisión de Producto:** cada curso tendrá como máximo una calibración global
activa y cada desafío tendrá como máximo una subcalibración activa. Recalibrar
cualquiera de los dos niveles crea una nueva versión y conserva todas las
anteriores. Una calibración anterior aprobada podrá volver a habilitarse para
reemplazar la activa.

**Implicancia de diseño:** las activaciones son cambios explícitos, auditables y
reversibles hacia una corrida histórica aprobada. Nunca se sobrescriben las
versiones, sus métricas, ni las skills snapshot asociadas.

### D-10 — Recalibración global en cascada

**Decisión de Producto:** al recalibrar un curso deben recalibrarse también los
desafíos vinculados que no estén terminados.

**Pendiente de Producto — desafío parcialmente realizado:** debe definirse qué
ocurre con un desafío que ya fue iniciado por parte del curso cuando se impone
una recalibración global por fuerza mayor. La resolución debe conservar la
configuración histórica de los intentos ya comenzados y evitar cambios opacos o
injustos para alumnos que estén cursando.

### D-11 — Recalibración forzosa con corte por intento

**Decisión de Producto:** ante una recalibración forzosa, la configuración se
congela por intento. Los intentos iniciados —incluidos los que están en curso—
conservan la versión anterior; los intentos que comiencen tras activar la nueva
configuración usan la nueva versión. No se recalculan ni reinterpretan
resultados históricos.

**Implicancia de diseño:** una recalibración global crea las subcalibraciones
necesarias para desafíos no terminados. La configuración anterior continúa
activa hasta que la nueva apruebe. La interfaz muestra el alcance del cambio,
la fecha de efectividad y los intentos alcanzados por cada versión.

### D-12 — Auditoría obligatoria

**Decisión de Producto:** todas las decisiones y cambios de este flujo deben
quedar en un historial de auditoría.

**Alcance mínimo de auditoría:** creación, publicación, cambio de visibilidad,
versionado, archivado y favoritismo de skills; adjuntos y snapshots usados;
creación, ejecución, aprobación, activación, reemplazo y reversión de
calibraciones; recalibraciones en cascada y forzosas; corte por intento;
notificaciones y acciones explícitas del docente. Cada evento debe registrar
actor, rol, fecha, recurso, versiones implicadas, curso/desafío/intentó cuando
corresponda, motivo, `X-Request-Id` y `traceparent`, sin exponer secretos ni
contenido sensible en logs.

### D-13 — Consulta del historial de auditoría

**Decisión de Producto:** los administradores pueden consultar el historial de
auditoría completo. Los docentes sólo pueden consultar los eventos de los cursos
para los que están autorizados.

**Implicancia de diseño:** la autorización se evalúa en el backend para cada
consulta y no sólo mediante filtros de interfaz. Las respuestas omiten datos de
otros cursos y cualquier contenido sensible de skills o prompts.

### D-14 — Retención indefinida

**Decisión de Producto:** el historial de auditoría y las versiones/snapshots de
skills utilizados en calibraciones se conservan eternamente.

**Implicancia de diseño:** el archivado o reemplazo lógico no elimina evidencia
histórica. La solución deberá prever almacenamiento escalable, integridad de
hashes y copias de respaldo para auditoría de largo plazo, aplicando las
obligaciones legales de protección de datos que correspondan.

### D-15 — Skills como instrucciones efectivas del evaluador

**Decisión de Producto:** las skills seleccionadas se incorporan, en el orden
elegido, como instrucciones adicionales del agente. Potencian al agente tanto
durante la calibración como durante la evaluación posterior del uso de IA en
exámenes y desafíos.

**Implicancia de diseño:** la configuración activada debe ligar las versiones
exactas de skills a la corrida de calibración y a cada evaluación posterior. El
AI Gateway construye el contexto efectivo con rúbrica, Golden Set y skills; los
intentos conservan el snapshot que les correspondía al inicio. Las skills no
pueden eludir el AI Gateway ni cambiar resultados históricos.

### D-16 — Skills declarativas, sin ejecución ni red

**Decisión de Producto:** una skill no ejecuta código ni realiza llamadas a APIs
o herramientas externas. Su único efecto es aportar instrucciones y contexto en
Markdown al agente dentro del flujo controlado por el AI Gateway.

**Implicancia de diseño:** se rechaza cualquier formato distinto de Markdown o
contenido que intente declarar ejecución, red, secretos o instrucciones que
contradigan los guardarraíles de plataforma. No se incorpora un runtime de
plugins, intérpretes ni credenciales de terceros.

### D-17 — Publicación inmediata por confianza docente

**Decisión de Producto:** una skill marcada como pública por su docente autor
queda disponible inmediatamente; no requiere revisión ni aprobación previa de
un administrador.

**Implicancia de diseño:** la publicación debe auditar autor, fecha, visibilidad
y versión. Se mantienen validaciones automáticas de formato y seguridad, pero
no existe una cola editorial que bloquee la disponibilidad de la skill.

### D-18 — Archivado administrativo y flujo de seguridad de archivos

**Decisión de Producto:** un administrador puede archivar una skill pública
inapropiada o insegura. El archivado impide usos nuevos, pero conserva el
historial y las calibraciones que ya la utilizaron. Todas las skills pasan por
el mismo flujo de seguridad definido para archivos PDF del RAG en `docs/`.

**Implicancia de diseño:** la carga Markdown se somete al pipeline de validación
de archivos y guardarraíles de contenido vigente en la plataforma antes de
publicarse o quedar disponible para adjuntar. El resultado de validación,
rechazo o archivado se audita. La aplicación concreta reutilizará y alineará las
etapas documentadas de RAG/seguridad y aplica la sanitización síncrona definida
para skills.

### D-19 — Suspensión preventiva y sanitización crítica

**Decisión de Producto:** si un administrador archiva por seguridad una skill
usada por una calibración activa, se suspenden las evaluaciones futuras que la
utilizan y se solicita una recalibración. Los intentos ya iniciados conservan su
snapshot histórico. El archivado es motivo suficiente para recalibrar.

**Decisión de Producto:** toda skill subida debe sanitizarse inmediatamente y
disparar alarmas antes de que pueda adjuntarse, calibrar o evaluar desafíos. Es
un control crítico.

**Implicancia de diseño:** una skill no alcanza los estados seleccionable,
pública o adjuntable hasta que el pipeline síncrono de seguridad la apruebe. Un
rechazo o incidente genera auditoría, alerta y feedback seguro; nunca expone
detalles que permitan eludir los guardarraíles. La suspensión sólo afecta usos
futuros: no sobrescribe evidencia ni resultados previos. Una entrega posterior
se acepta conforme a RF-IA-27 y su cálculo de IA queda diferido hasta contar con
una configuración segura y calibrada.

### D-20 — Destinatarios de alertas críticas

**Decisión de Producto:** ante una skill rechazada, archivada por seguridad o
que suspenda calibraciones activas, se alerta a todos los actores afectados: el
docente autor, los administradores y los docentes autorizados de los cursos
afectados.

### D-21 — Integración asíncrona de notificaciones

**Restricción de integración:** `docs/contracts/01-inventario-y-brechas.md`
no contiene todavía el contrato técnico con `notification-service`. La
implementación exige definir canal, evento, destinatarios, severidad, payload,
reintentos y DLQ.

**Decisión de diseño:** `llm-service` persiste localmente el incidente de seguridad, la
suspensión/recalibración y el evento de auditoría. En la misma transacción guarda
un registro outbox. Un relay publica el aviso por Kafka de manera asíncrona;
`notification-service` es dueño de guardar, entregar y marcar leída/no leída la
notificación de cada destinatario. Kafka nunca es síncrono ni reemplaza la
respuesta HTTP inmediata de la carga.

**Reglas del contrato:** eventos versionados por topic, envelope con
`eventId`, `version`, `occurredAt`, `producer` y `data`; correlación sólo en los
headers `traceparent` y `X-Request-Id`; publicación at-least-once mediante
outbox y consumo idempotente con deduplicación por `eventId`.

### D-22 — Responsabilidades de alertas confirmadas

**Decisión de Producto:** se confirma la separación: `llm-service`
es dueño del incidente, la auditoría, la suspensión/recalibración y el outbox;
`notification-service` es dueño de persistir, entregar y administrar el estado
de lectura de las notificaciones por destinatario.

**Implicancia de diseño:** la integración se implementará sólo tras acordar el
contrato con el equipo de Notificaciones. La respuesta HTTP del flujo crítico no
depende de Kafka ni de la entrega del aviso.

### D-23 — Dueño del catálogo de desafíos publicados

**Decisión de Producto:** `challenges-service` es dueño del catálogo de desafíos
publicados y debe proveer la información necesaria para listarlos y
seleccionarlos durante la subcalibración.

**Implicancia de diseño:** `llm-service` no accede a la base de datos de
Desafíos. El contrato interequipos deberá acordar los campos de catálogo y la
validación de pertenencia a curso/estado publicado antes de implementar el
selector o permitir crear una subcalibración.

### D-24 — Sin proyección de catálogo ni ciclo de vida en `llm-service`

**Decisión de Producto:** `llm-service` no mantiene una proyección del catálogo,
estado ni vencimiento de desafíos. Sólo registra el `challengeId` y `courseId`
que se le asocian al crear, ejecutar o activar una subcalibración.

### D-25 — Elegibilidad de desafíos

**Decisión de Producto:** sólo los desafíos publicados y vigentes pueden
seleccionarse para crear o recalibrar una subcalibración.

**Implicancia de diseño:** el dueño del catálogo limita el selector a desafíos
publicados y vigentes. `llm-service` no valida el ciclo de vida externo; responde
únicamente por el estado de su calibración asociada.

### D-26 — Recursos publicados seleccionables por desafío

**Decisión de Producto:** una subcalibración puede seleccionar cualquier rúbrica
y Golden Set publicados que pertenezcan al curso, aunque no sean los usados por
la calibración global.

**Implicancia de diseño:** el backend valida publicación y pertenencia al curso
para ambos recursos. La interfaz mostrará los valores heredados y permitirá
reemplazarlos por cualquier versión elegible, dejando visible esa diferencia en
la vista previa.

### D-27 — Límites de carga administrados

**Decisión de Producto:** los administradores pueden aumentar o restringir la
cantidad máxima de skills que un docente puede subir/adjuntar y el tamaño máximo
de cada archivo.

**Implicancia de diseño:** estos límites son parámetros administrativos
auditables y se validan tanto al cargar una versión como al armar una
calibración. La interfaz informa el límite vigente antes de que el docente
inicie la operación.

### D-28 — Protección preventiva de cuota y contexto

**Requisito de Producto:** el sistema no debe sobrepasar la cuota de tokens de
una llamada al calibrar. Debe verificar el conjunto de skills y el contexto
antes de enviar una invocación al proveedor.

**Diseño propuesto pendiente de confirmación:** antes de encolar y antes de cada
llamada del worker, construir el prompt efectivo y validar el peor caso:
instrucciones base, rúbrica, skill snapshots, caso del Golden Set, contexto,
esquema de salida y reserva de tokens de respuesta. La suma no puede superar el
límite real del modelo ni el presupuesto administrativo. Si no entra, se
rechaza con feedback accionable sin llamar al proveedor ni recortar contenido
en silencio.

**Brecha técnica actual:** el SPI de modelos sólo expone capacidades funcionales;
no declara aún ventana de contexto ni tokenizer por modelo. El plan deberá
incorporar límites de contexto versionados por deployment y un estimador
conservador/específico por proveedor antes de habilitar skills en producción.

### D-29 — Límites reales definidos por proveedor

**Decisión de Producto:** los límites de contexto, tokens y solicitudes son
propios de cada modelo/proveedor; Administración no los aumenta ni los configura
arbitrariamente. La organización y sus administradores evalúan proveedores y
modelos según sus criterios y sólo habilitan deployments aptos.

**Implicancia de diseño:** el AI Gateway debe tomar los topes duros desde la
metadata certificada del modelo y su revisión, no desde un valor libre ingresado
por el docente. Una política institucional sólo podría imponer un límite menor
por seguridad/costo, nunca elevar el límite real del proveedor. La habilitación
de un deployment requiere conocer sus límites y mantenerlos auditados.

### D-30 — Feedback de límites al docente

**Decisión de Producto:** cuando una calibración no puede iniciarse por exceso
de uso, tokens, ventana de contexto, cuota o límite de solicitudes, el sistema
debe informar claramente al docente la causa.

**Implicancia de diseño:** el error se presenta como `ProblemDetail` tipado y
feedback de interfaz accionable, por ejemplo reducir skills, esperar el período
de cuota o elegir una configuración apta. No se exponen credenciales, prompts
internos ni detalles de seguridad del proveedor. El intento rechazado queda
auditado.

### D-31 — Deshabilitación por administrador o autor

**Decisión de Producto:** un administrador puede deshabilitar una skill ante una
falla de seguridad u otra causa justificada. El docente autor también puede
deshabilitar sus propias skills.

**Implicancia de diseño:** deshabilitar impide nuevos usos y se audita con actor,
motivo, alcance e impacto. No altera snapshots ni resultados históricos. Si la
skill está activa en configuraciones futuras, se aplica la política de
suspensión preventiva y recalibración acordada. La acción afecta sólo la
versión deshabilitada; las versiones relacionadas se tratan según su propio
estado y auditoría.

### D-32 — Deshabilitación granular y revisión de antecedentes

**Decisión de Producto:** la deshabilitación afecta sólo la versión que presenta
la falla, problema de seguridad u otra causa. Las versiones anteriores no se
deshabilitan si no tienen un criterio propio que lo justifique.

**Decisión de Producto:** al detectar un problema de seguridad en una versión,
las versiones anteriores se marcan para auditoría.

**Implicancia de diseño:** las versiones anteriores quedan en estado de revisión
sin retirarse automáticamente; la interfaz muestra su condición y el historial
del incidente. Una auditoría posterior decide explícitamente si cada versión
permanece habilitada o también se deshabilita.

### D-33 — Cierre administrativo de auditorías

**Decisión de Producto:** sólo un administrador puede cerrar la auditoría de una
versión marcada para revisión y decidir explícitamente mantenerla habilitada o
deshabilitarla.

**Implicancia de diseño:** el docente autor puede consultar el estado y recibir
alertas, pero no resuelve su propia revisión de seguridad. La decisión
administrativa, motivo y evidencia quedan en el historial inmutable.

### D-34 — Versiones en revisión no seleccionables

**Decisión de Producto:** una versión marcada en revisión queda indisponible
para nuevas calibraciones hasta que Administración cierre su auditoría.

**Implicancia de diseño:** el selector y el backend la rechazan como adjunto
nuevo y muestran su estado. Una configuración activa no puede usarla mientras
se completa la revisión; sus entregas posteriores se resuelven mediante cálculo
diferido según RF-IA-27.

### D-35 — Suspensión preventiva durante revisión

**Decisión de Producto:** mientras una versión está en revisión, se suspenden
las evaluaciones futuras de configuraciones activas que la utilizan. Los
intentos ya iniciados preservan su evidencia y configuración histórica.

**Implicancia de diseño:** el estado `UNDER_REVIEW` tiene el mismo efecto
preventivo que una deshabilitación sobre los usos futuros, pero conserva una
salida de auditoría para que Administración pueda rehabilitarla explícitamente.
La entrega no se bloquea: sólo se difiere su cálculo de IA hasta disponer de una
configuración segura y calibrada. La suspensión y su impacto se notifican y
auditan.

### D-36 — Corrida afectada por una skill no segura

**Decisión de Producto:** si una skill de una corrida en cola o ejecución pasa a
revisión o se deshabilita, la corrida debe finalizar de manera controlada,
informar el error y su causa, y no puede volver a ejecutarse con esa versión para
proteger cursos y desafíos.

**Implicancia de diseño:** la corrida conserva evidencia parcial y queda en un
estado terminal de fallo de seguridad, con código de error específico. El
backend revisa el estado de todas las skill versions inmediatamente antes de
cada invocación para evitar seguir enviando contexto inseguro al proveedor.

### D-37 — Nueva corrida con skill reemplazada

**Decisión de Producto:** tras un fallo por una skill afectada, el docente puede
reemplazarla por una versión habilitada y crear una nueva corrida de
calibración.

**Implicancia de diseño:** la corrida fallida no se reutiliza ni se altera. La
nueva corrida conserva su propia configuración y deja trazada la relación con
el incidente y la versión sustituida.

### D-38 — Reemplazo explícito, nunca automático

**Decisión de Producto:** el sistema no sustituye automáticamente una skill
afectada por otra versión. El docente elige explícitamente el reemplazo.

**Implicancia de diseño:** la interfaz puede sugerir versiones habilitadas, pero
requiere confirmación del docente y crea una configuración/corrida nueva,
auditada e inmutable.

### D-39 — Asociación obligatoria a cursos para ordenar el catálogo

**Decisión de Producto:** al crear o editar una skill, el docente debe asociarla
a uno o más cursos para permitir un catálogo ordenado visualmente y evitar una
grilla global desorganizada.

**Implicancia de diseño:** el catálogo abre filtrado por el curso actual y las
skills se buscan primero en ese contexto. La asociación es una clasificación de
descubrimiento, no otorga permisos sobre datos de otro curso. Existen skills
potencialmente reutilizables entre asignaturas —por ejemplo guías pedagógicas o
de seguridad—, pero ese uso debe ser deliberado y no la vista predeterminada.

### D-40 — Skills públicas asociables a varios cursos

**Decisión de Producto:** una skill pública puede asociarse a varios cursos,
siempre que su autor esté autorizado en dichos cursos.

**Implicancia de diseño:** el backend valida la autorización del autor sobre
cada curso al modificar sus asociaciones. La lista de cursos asociados sirve
para clasificar y filtrar, sin trasladar propiedad ni acceso a recursos
académicos de otros servicios.

### D-41 — Reutilización directa de skills públicas

**Decisión de Producto:** un docente puede usar directamente una versión
pública de una skill creada en otro curso dentro de una calibración de su curso,
sin modificar la propiedad ni las asociaciones originales.

**Implicancia de diseño:** la reutilización queda auditada como uso en el curso
destino. El catálogo debe ofrecer una búsqueda explícita fuera del filtro del
curso actual para encontrar estas skills, manteniendo claro su autor, origen y
versión.

### D-42 — Orden manual de skills

**Decisión de Producto:** el docente puede reordenar manualmente las skills
seleccionadas antes de iniciar una calibración. La interfaz debe indicar que el
orden afecta el razonamiento del agente.

**Implicancia de diseño:** la posición forma parte del snapshot inmutable y del
hash/contexto efectivo de la corrida. La vista previa muestra el orden final y
la auditoría registra cualquier modificación antes de iniciar la ejecución.

### D-43 — Jerarquía de instrucciones y confianza cero

**Decisión de Producto:** las skills se tratan como contenido no confiable de
usuario. El system prompt y los controles de seguridad prevalecen siempre; una
skill no puede anular guardarraíles, revelar soluciones ni transformar la
evaluación de uso de IA en corrección académica.

**Implicancia de diseño:** el AI Gateway compone las skills debajo de las
instrucciones de sistema y aplica sanitización, aislamiento y validación antes
de cada invocación. Un intento de instrucción conflictiva se trata como evento
de seguridad, no como una orden válida del docente.

### D-44 — Inspección opcional del contenido de skills públicas

**Decisión de Producto:** el docente no está obligado a leer el contenido de
una skill pública antes de seleccionarla, pero debe poder abrir y leer su
Markdown completo, además de sus metadatos y estado de seguridad.

**Implicancia de diseño:** la grilla prioriza nombre, función, autor, versión y
estado; la ficha o vista previa ofrece el contenido completo bajo demanda. La
selección sigue sujeta a los controles de sanitización y no implica confianza
ciega en el contenido visualizado.

### D-45 — Exámenes como tipo de desafío

**Decisión de Producto:** los exámenes son desafíos de tipo `EXAM` publicados
por `challenges-service`.

**Implicancia de diseño:** no se integra un microservicio adicional. La
subcalibración aplica las mismas reglas de seguridad, versiones, skills,
activación y congelamiento por intento tanto a desafíos regulares como a
exámenes. `llm-service` sólo conserva su asociación de calibración y no el
catálogo ni el tipo del desafío.

### D-46 — Filtro de tipo de desafío

**Decisión de Producto:** el selector de subcalibración permite filtrar entre
desafíos regulares y exámenes, mostrando el tipo de manera visible.

**Implicancia de diseño:** el selector obtiene el tipo desde el dueño del
catálogo. El filtro es una ayuda de interfaz y no cambia las reglas de
elegibilidad ni el flujo de calibración de `llm-service`.

### D-47 — Inicio diferido de la recalibración en cascada

**Decisión de Producto:** al recalibrar un curso, las recalibraciones de
desafíos vinculados se inician sólo cuando la nueva calibración global aprueba y
el docente la activa.

**Implicancia de diseño:** si la global falla o no se activa, no se consumen
recursos en subcalibraciones derivadas. `llm-service` marca como requeridas para
recalibración sus subcalibraciones asociadas; el servicio que gobierna el ciclo
de vida del desafío determina cuáles continúan vigentes y solicita las nuevas
corridas correspondientes.

### D-48 — Alcance de la cascada por vigencia del desafío

**Decisión de Producto:** al recalibrar un curso se recalibran los desafíos
vinculados que continúan vigentes. Los desafíos cerrados o expirados no se
recalibran y conservan su historial.

**Implicancia de diseño:** la verificación de vigencia ocurre fuera de
`llm-service`. Este conserva el historial y el estado de recalibración de las
asociaciones que ya conoce, sin consultar ni almacenar fechas de desafío.

**Decisión de Producto:** cada subcalibración nueva en cascada copia la nueva
configuración global como punto de partida. El docente puede modificar esa
configuración antes de utilizarla; no queda atado a los recursos copiados.

### D-49 — Subcalibración híbrida vinculada a la calibración global

**Decisión de Producto:** la subcalibración será híbrida: conserva una base de
curso de mayor peso y puede acoplar una rúbrica, Golden Set y skills específicas
del desafío.

**Decisión de diseño:** la relación se modela en dos capas
inmutables, no concatenando dos calibraciones completas en un prompt:

- `CourseBaseline`: referencia a la versión activa de calibración global y a
  su snapshot inmutable.
- `ChallengeOverlay`: referencias explícitas a los recursos elegidos para el
  desafío y a sus reemplazos respecto de la base.
- `EffectiveEvaluationProfile`: resultado determinista de resolver ambas capas
  antes de invocar al proveedor, con hash, versiones y orden de skills
  auditables.

La rúbrica global conservaría las dimensiones y pesos canónicos del curso; la
rúbrica del desafío aportaría criterios, evidencias y restricciones situadas
sobre esas mismas dimensiones. No se permitirá al modelo decidir qué regla
prevalece ni fusionar pesos contradictorios. El Golden Set global acredita la
base del curso y el Golden Set del desafío valida el perfil híbrido para ese
desafío; no se enviarán ambos conjuntos completos como contexto de una misma
llamada.

Las skills de curso y de desafío se compondrán por capas y orden explícito,
descartando duplicados por hash. Las instrucciones de sistema y seguridad
siempre quedan por encima de ambas capas. El compilador de contexto calculará
de forma determinista el presupuesto total (instrucciones fijas, rúbrica
efectiva, skills, evidencia del alumno y reserva de salida); si excede el
máximo certificado del modelo, fallará antes de llamar al proveedor y explicará
al docente qué componente debe reducir o reemplazar. La salida se exigirá con
esquema estructurado y se validará contra el perfil efectivo para reducir
ambigüedad y alucinación.

### D-50 — Revisión docente antes de ejecutar la cascada

**Decisión de Producto:** cuando una nueva calibración global activa alcance a
desafíos vigentes, el sistema creará una subcalibración derivada en estado de
borrador pendiente de revisión. Copiará la nueva configuración global, pero no
la ejecutará automáticamente: el docente podrá ajustarla y luego iniciará su
recalibración de forma explícita.

**Implicancia de diseño:** la activación global crea tareas durables de revisión
por desafío, con vínculo a la versión global originadora y trazabilidad de los
cambios realizados antes de la ejecución. Mientras tanto, la versión de subcalibración previamente activa
continúa gobernando las evaluaciones futuras, salvo una suspensión de seguridad
u otra regla de excepción ya definida.

### D-51 — Modificación de skills heredadas en la subcalibración

**Decisión de Producto:** el docente puede quitar o reemplazar skills heredadas
de la calibración global al editar la subcalibración de un desafío, además de
agregar skills propias de ese desafío.

**Implicancia de diseño:** el `ChallengeOverlay` registra por cada skill
heredada si se conserva, excluye o reemplaza, y las skills agregadas y su orden.
El perfil efectivo conserva el vínculo a la base global y materializa el
resultado final para auditoría, recalibración y evaluación posterior. La
interfaz debe distinguir con claridad las skills heredadas, excluidas,
reemplazadas y agregadas para que el cambio no sea accidental.

### D-52 — Pesos de rúbrica y rigor específico de un desafío

**Decisión de Producto:** el docente puede ajustar el rigor de un examen o
desafío mediante una rúbrica específica.

**Implicancia de diseño:** el ajuste es una política explícita y versionada del
`ChallengeOverlay`, validada antes de calibrar y visible en la comparación con
la base global. La IA no infiere ni arbitra pesos: recibe una única rúbrica
efectiva cuya suma es 100, junto con reglas de precedencia deterministas. La
calibración global seguirá aportando el marco, las restricciones y las skills
no excluidas, pero la rúbrica efectiva del desafío podrá expresar un rigor
diferente, dentro de las reglas de dimensiones y porcentajes ya acordadas.

### D-53 — Dimensiones heredadas y porcentajes ajustables por desafío

**Decisión de Producto:** la subcalibración debe ajustarse a las dimensiones
definidas en la rúbrica del curso. El docente puede redistribuir los porcentajes
de esas dimensiones según la necesidad del desafío o examen, con validación de
que el total sea exactamente 100 %.

**Implicancia de diseño:** la rúbrica efectiva conserva el mismo conjunto de
dimensiones del curso y materializa una única distribución de pesos para el
desafío. La interfaz compara los porcentajes heredados y los propuestos, y el
backend rechaza una dimensión desconocida, duplicada o un total distinto de
100. Esto da más o menos énfasis a competencias concretas sin pedir a la IA que
fusione rúbricas ni resuelva contradicciones.

### D-54 — Cero permitido; pesos negativos prohibidos

**Decisión de Producto:** una dimensión puede recibir 0 % en una
subcalibración. No se permiten porcentajes negativos.

**Implicancia de diseño:** todas las dimensiones heredadas se conservan en el
snapshot y en la vista comparativa, aun cuando una tenga 0 %, para dejar
explícito que no participa de ese desafío. La validación exige valores numéricos
mayores o iguales a cero y suma exacta de 100 %, antes de crear la calibración o
enviar contexto al proveedor.

### D-55 — Sin criterios globales no desactivables

**Decisión de Producto:** no existirán criterios mínimos globales que operen
como condición adicional de aprobación ni que impidan asignar 0 % a una
dimensión en un desafío.

**Implicancia de diseño:** la calibración de curso aporta la plantilla de
dimensiones, los valores heredados por defecto y el contexto general; la
rúbrica efectiva del desafío determina por completo los pesos que intervienen
en su resultado. La auditoría conserva la comparación con la base para hacer
visible cada diferencia, sin aplicar reglas de puntuación ocultas.

### D-56 — Herencia de configuración para subcalibraciones

**Decisión de Producto:** la subcalibración adopta el concepto de herencia:
parte de la calibración base del curso y, dentro de ese marco, agrega criterios
o modifica los existentes para el desafío.

**Implicancia de diseño:** se utilizará composición de configuraciones
inmutables, no herencia de clases de programación. Cada subcalibración guarda
la referencia y el snapshot de su `CourseBaseline`, más un `ChallengeOverlay`
de diferencias explícitas. El resolvedor construye un único perfil efectivo;
si una propiedad no fue modificada, hereda el valor de la base. Esto permite
comparar, auditar y recalibrar sin duplicar ni mezclar prompts completos.

### D-57 — Extensión de dimensiones en una subcalibración

**Decisión de Producto:** además de heredar y modificar dimensiones, una
subcalibración puede agregar dimensiones nuevas, como una subclase que incorpora
una función propia. Por ejemplo, puede heredar cuatro dimensiones del curso y
agregar una quinta para un examen concreto.

**Implicancia de diseño:** esta decisión reemplaza la restricción de D-53 que
exigía conservar exactamente el mismo conjunto de dimensiones. Una dimensión
agregada debe tener identificador inmutable, nombre, descripción, criterios,
peso mayor o igual a cero y origen `CHALLENGE`. La rúbrica efectiva contendrá
las dimensiones heredadas, modificadas y agregadas, con suma total exactamente
100 %. Las heredadas no eliminadas permanecen trazables; el peso 0 % expresa su
no participación.

**Impacto técnico a planificar:** la implementación actual asume un conjunto
fijo de dimensiones, por lo que esta capacidad exigirá evolucionar el modelo y
sus validadores, contratos y persistencia mediante cambios aprobados
explícitamente antes de desarrollar migraciones o APIs.

### D-58 — Máximo administrable de dimensiones adicionales

**Decisión de Producto:** el administrador puede configurar la cantidad máxima
de dimensiones nuevas que una subcalibración puede agregar a su base heredada.

**Implicancia de diseño:** la creación y edición validan ese máximo antes de
guardar o calibrar. Este límite complementa, pero no reemplaza, la validación
del presupuesto real de contexto y tokens del proveedor. El mensaje de error
indica si se excedió la política administrativa o el límite técnico de la
invocación.

### D-59 — Promoción opcional de una dimensión de desafío al curso

**Decisión de Producto:** una dimensión creada para un desafío podrá, a futuro,
promoverse a la rúbrica base del curso mediante una nueva calibración global.
No es una capacidad bloqueante para la primera entrega.

**Implicancia de diseño:** la primera versión conserva el origen `CHALLENGE` y
la trazabilidad de la dimensión sin requerir promoción. El backlog evolutivo
incorporará una acción explícita de promoción que cree una nueva versión de la
rúbrica/calibración global y aplique las reglas ya acordadas de borradores de
subcalibración, revisión docente e historial.

### D-60 — Reutilización flexible del Golden Set heredado

**Decisión de Producto:** una subcalibración puede reutilizar el Golden Set de
la calibración global y, al mismo tiempo, modificar otros criterios, pesos o
skills según necesite. También puede reemplazarlo por un Golden Set propio.

**Implicancia de diseño:** el Golden Set se hereda como referencia versionada
por defecto, no como una obligación de copiar contenido. Toda ejecución
calibra el perfil efectivo completo —base, overrides, dimensiones y skills—
contra el Golden Set finalmente seleccionado. El historial indica si el recurso
fue heredado o reemplazado y conserva su snapshot para reproducibilidad.

### D-61 — Desactivación mediante peso cero

**Decisión de Producto:** desactivar un criterio equivale a asignarle 0 %. No
se utiliza una regla de exclusión separada. Los elementos restantes deben
redistribuirse para que la ponderación efectiva vuelva a totalizar 100 %.

**Implicancia de diseño:** la edición debe recalcular o solicitar al docente el
reparto restante; nunca completa porcentajes de forma implícita. El snapshot
conserva el elemento con peso cero y su motivo de modificación para auditoría.
El modelo recibe únicamente la ponderación efectiva resuelta, sin instrucciones
ambiguas de evaluar y a la vez ignorar un mismo criterio.

### D-62 — Terminología vigente de documentación v2: dimensión y criterio

**Hallazgo documental:** docs (entonces `docsV2`) distingue ambos conceptos. `RubricDimension` es
el elemento ponderado y contiene `key`, `criterio`, anclas, prompt y `weight`.
La rúbrica vigente tiene cinco dimensiones fijas; cada dimensión tiene un único
campo de criterio y no existe una colección de criterios con peso propio.

**Fuente:** `docs/03-capacidades-de-ia/golden-set-y-calibracion/02-especificacion-funcional.md`
§3 y `03-modelo-de-dominio-y-transiciones.md`.

**Impacto para este alcance:** permitir dimensiones adicionales en una
subcalibración contradice la regla v2 de cinco keys fijas y requiere una ADR y
evolución aprobada de dominio, contrato, persistencia, Golden Set, métricas
PAR-14 y validadores. La regla D-61 de desactivar "criterios" debe precisarse:
con el modelo v2 sólo una dimensión posee peso; si se desean subcriterios
ponderables, se necesita un modelo nuevo de criterios dentro de cada dimensión.

### D-63 — Qué significa que un criterio no tenga peso propio en v2

**Aclaración:** en el modelo vigente el criterio sí se usa: describe qué debe
observar el evaluador para asignar el puntaje de una dimensión. Las anclas y el
prompt de esa misma dimensión completan esa guía. Lo que no existe es un
porcentaje independiente para el criterio.

Por ejemplo, si la dimensión `AUTONOMY` pesa 30 %, el modelo produce un puntaje
0–100 para autonomía siguiendo su criterio; el resultado final incorpora ese
puntaje multiplicado por 30 %. El criterio no agrega ni descuenta otro
porcentaje aparte. Esta estructura permite que Golden Set, calibración y MAE
compararen las mismas cinco puntuaciones de manera estable.

**Consecuencia:** en v2 no se puede "desactivar sólo un criterio" con 0 %,
porque es texto de la dimensión. El nuevo alcance incorpora subcriterios
identificables y ponderables dentro de una dimensión, con reglas claras de suma
y de agregación a su puntaje.

### D-64 — Subcriterios ponderables dentro de una dimensión

**Decisión de Producto:** el nuevo alcance incorpora una lista de subcriterios
identificables y ponderables dentro de cada dimensión. La dimensión conserva su
peso en el total de la rúbrica; sus subcriterios distribuyen internamente el
100 % de esa dimensión.

**Reglas de cálculo:** el puntaje de una dimensión es la suma de los
puntajes de sus subcriterios multiplicados por sus pesos internos. El puntaje
final suma los puntajes de dimensión multiplicados por sus pesos globales. Los
pesos de dimensiones totalizan 100 % y, dentro de cada dimensión, sus
subcriterios también totalizan 100 %. Un subcriterio en 0 % queda trazable y no
contribuye; los restantes se redistribuyen dentro de su dimensión.

**Impacto técnico:** esta es una evolución material de la especificación (entonces `docsV2`) y del modelo
actual. Requiere ADR, nuevo contrato y migración aprobados, más adaptación de
Golden Set, resultados por caso, calibración, métricas PAR-14, reportes y UI.

### D-66 — Subcriterios referenciados integran la aprobación de calibración

**Decisión de Producto:** el desvío de cada subcriterio activo con referencia
humana participa de la aprobación o rechazo de la calibración con las mismas
reglas de MAE y error individual aplicadas a las dimensiones.

**Implicancia de diseño:** la métrica conserva el MAE final ponderado y amplía
el conjunto de errores individuales a cada par caso/subcriterio activo. Un
desvío que exceda el umbral vigente hace fallar la corrida. El reporte diferencia
claramente las métricas por dimensión y por subcriterio, sin ocultar cuál causó
el fallo.

### D-67 — Referencia humana obligatoria y reporte explicable de calibración

**Decisión de Producto:** el Golden Set siempre cuenta con referencia humana.
Cada caso tiene puntuación humana por dimensión y por cada subcriterio activo.

**Decisión de Producto:** la calibración global y la subcalibración de desafío
deben mostrar su puntaje o métricas, su estado `PASSED`/`FAILED` y dónde y por
qué aprobó o falló. El docente debe poder identificar el caso, dimensión o
subcriterio, la referencia humana, el puntaje del modelo, el desvío, el umbral
aplicable y la configuración efectiva que originó el resultado.

**Implicancia de diseño:** el reporte no se limita a un indicador global. Debe
incluir MAE final ponderado, máximo error individual, desglose por dimensión y,
cuando aplique, por subcriterio, además de causas técnicas controladas como
presupuesto de contexto, skill no habilitada o error de proveedor. La vista y la
auditoría conservan el snapshot y hashes de rúbrica, Golden Set, skills y modelo
para explicar una corrida histórica.

### D-68 — Referencia humana obligatoria para cada subcriterio activo

**Decisión de Producto:** todo subcriterio activo debe tener puntuación humana
en cada caso del Golden Set.

**Implicancia de diseño:** la validación de publicación y de inicio de
calibración rechaza un Golden Set incompleto para los subcriterios ponderados.
El modelo debe devolver el mismo conjunto de puntajes por subcriterio y la
calibración aplica las métricas acordadas sobre todos ellos. Un subcriterio con
peso 0 % permanece trazable, pero no requiere participar del cálculo ni de la
referencia activa.

**Consecuencia sobre D-60:** el Golden Set heredado puede reutilizarse sin
cambios si el desafío no altera su conjunto de subcriterios activos. Si agrega
o activa uno nuevo, debe completarse la referencia humana correspondiente antes
de calibrar y conservar su procedencia versionada.

### D-69 — Golden Set derivado al extender subcriterios

**Decisión de Producto:** si una subcalibración agrega o activa subcriterios que
no existen en el Golden Set heredado, el sistema crea una copia derivada del
Golden Set, con sus casos y referencias existentes precompletados. El docente
completa las nuevas referencias humanas allí, sin modificar el Golden Set
original.

**Implicancia de diseño:** la copia conserva `baseVersionId`, autor, fecha y
trazabilidad de procedencia; nace como borrador y sólo puede seleccionarse al
publicarse completa. El original permanece inmutable y apto para sus
calibraciones históricas. La subcalibración referencia la versión derivada
publicada y el reporte permite navegar a su origen.

### D-70 — Golden Set derivado al extender dimensiones

**Decisión de Producto:** se aplica la misma regla de copia derivada cuando la
subcalibración agrega una dimensión nueva. No se modifica ningún Golden Set
publicado ni histórico para adaptarlo.

**Implicancia de diseño:** el Golden Set derivado hereda sus casos y referencias
de dimensión existentes, añade el espacio obligatorio para referencias humanas
de la nueva dimensión en cada caso y se publica sólo al completarlas. Así, la
rúbrica efectiva, las referencias humanas y las métricas de calibración siempre
describen el mismo conjunto de dimensiones y subcriterios.

### D-71 — Máximo administrable de subcriterios por dimensión

**Decisión de Producto:** el administrador puede configurar la cantidad máxima
de subcriterios que puede contener una dimensión.

**Implicancia de diseño:** el límite se valida al editar la rúbrica base o su
overlay de desafío y se muestra antes de llegar al máximo. Complementa los
límites de skills y dimensiones adicionales, y protege la legibilidad del
Golden Set, del reporte de calibración y el presupuesto real de contexto del
modelo. No sustituye el cálculo preventivo de tokens.

### D-73 — Rebase de personalizaciones ante una nueva base global

**Decisión de Producto:** al activarse una nueva calibración global, cada
borrador derivado de desafío intenta conservar sus personalizaciones previas
sobre la nueva base. El sistema debe señalar los conflictos para revisión, sin
descartar cambios ni aplicar decisiones silenciosas.

**Implicancia de diseño:** el rebase compara `CourseBaseline` anterior, overlay
anterior y nueva base, y genera diferencias clasificadas: aplicadas sin
conflicto, heredadas nuevamente, inválidas o en conflicto. El docente recibe una
vista de comparación de dimensiones, subcriterios, pesos, Golden Set y skills;
su resolución queda versionada y auditada antes de iniciar la recalibración.

### D-74 — Conflictos de rebase bloquean la nueva corrida

**Decisión de Producto:** un borrador de subcalibración con conflictos de
rebase sin resolver no puede iniciar una recalibración.

**Implicancia de diseño:** el estado `CONFLICTED` comunica el motivo y las
acciones pendientes, sin invocar al proveedor ni consumir cuota. La
subcalibración activa anterior continúa rigiendo las evaluaciones futuras hasta
que el docente resuelva los conflictos, complete el borrador, lo calibre y lo
active; se mantienen las excepciones de seguridad ya acordadas.

### D-76 — Resolución de conflicto volviendo a la nueva base

**Decisión de Producto:** un docente puede resolver un conflicto de rebase en
un borrador derivado descartando explícitamente las personalizaciones del
desafío y adoptando la nueva configuración base global.

**Implicancia de diseño:** la acción sólo modifica el borrador actual y requiere
confirmación con una vista de los cambios que se perderán. El overlay previo,
la subcalibración activa y todas las versiones históricas se conservan. La
auditoría registra actor, momento, conflicto y decisión de volver a la base.

### D-77 — Vencimiento durante la revisión de un borrador derivado

**Decisión de Producto:** si un desafío expira mientras su borrador derivado
espera revisión, el sistema lo cancela y no ejecuta su recalibración.

**Implicancia de diseño:** el servicio dueño del vencimiento determina la
cancelación y evita solicitar una nueva corrida. `llm-service` no ejecuta
controles de calendario; conserva el borrador y el historial cuando recibe la
cancelación por el mecanismo de integración que se acuerde.

### D-78 — Recordatorios de borradores próximos a vencer

**Decisión de Producto:** el docente recibe recordatorios sobre borradores de
subcalibración pendientes antes del vencimiento del desafío asociado.

### D-79 — Alcance de conocimiento de `llm-service`

**Decisión de Producto:** `llm-service` no conoce el catálogo completo de
cursos o desafíos ni sus vencimientos. Mantiene un registro acotado de cursos
activos y de desafíos marcados como `requiresCalibration`, junto con el estado
de las calibraciones que efectivamente existan: en ejecución, aprobada, activa,
fallida, requerida nuevamente o bloqueada por seguridad. No infiere un estado
pendiente si un recurso requerido aún no tiene corrida, ni persiste el resto del
catálogo.

### D-80 — `challenges-service` no gobierna la habilitación

**Decisión de Producto:** `challenges-service` no tiene la potestad de habilitar
ni deshabilitar un desafío o examen. La compuerta de aprobación/publicación debe
ubicarse en el servicio que realmente gobierna esa decisión.

### D-81 — Contrato agnóstico de estado de calibración

**Decisión de Producto:** `llm-service` expone un contrato HTTP de consulta que
cualquier servicio puede invocar mediante el API Gateway; ningún consumidor
concreto queda codificado en el contrato.

**Recursos acordados:**

```http
GET /api/llm/courses/{courseId}/calibration-status
GET /api/llm/courses/{courseId}/challenges/{challengeId}/calibration-status
```

La respuesta informa exclusivamente `registered`, `calibrationState` y, si
existe, el identificador de calibración efectiva. Si el recurso no fue marcado
como `requiresCalibration`, responde `registered: false` y no crea ni persiste
ningún registro por la consulta.

El contrato no decide elegibilidad, no publica ni habilita cursos o desafíos, y
no valida existencia, catálogo o vencimiento. Tampoco devuelve rúbricas, Golden
Set, skills ni contenido académico. La consulta protege el endpoint mediante el
JWT de servicio y propaga `traceparent` y `X-Request-Id`. Cada consumidor aplica
su propia política: por ejemplo, un curso requerido puede bloquearse sin estado
activo y un desafío opcional puede publicarse sin calibración.

### D-82 — Registro acotado de recursos que requieren calibración

**Decisión de Producto:** `courses-service` registra en `llm-service` los
cursos activos. Cuando quien crea un desafío indica que requiere calibración,
Cursos informa a LLM únicamente ese desafío y su relación con el curso. Los
recursos requeridos aparecen en la lista de recursos que requieren calibración
aun antes de que exista una corrida; el resto del catálogo de desafíos no se
almacena ni se lista en LLM. LLM no los clasifica ni gestiona como pendientes por
la ausencia de una calibración.

### D-83 — Nombre visible en el registro de calibración requerida

**Decisión de Producto:** al registrar un curso o desafío con
`requiresCalibration`, se guarda también un nombre visible como snapshot para
mostrarlo en las listas de LLM.

**Implicancia de diseño:** el snapshot se asocia al identificador externo y, en
el caso de un desafío, a su `courseId`. Es sólo información de presentación; no
convierte a `llm-service` en dueño del catálogo, del ciclo de vida ni del
vencimiento del recurso.

### D-84 — Versionado de cambios de nombre del snapshot

**Decisión de Producto:** un cambio de nombre de un curso o desafío registrado
crea una versión nueva del snapshot de presentación. Las versiones anteriores
se conservan para auditoría.

**Implicancia de diseño:** el historial de calibraciones mantiene el nombre que
era visible cuando se creó o ejecutó. Actualizar el snapshot no modifica
calibraciones, subcalibraciones ni evaluaciones históricas; sólo actualiza la
versión vigente mostrada en las listas futuras.

### D-85 — Sincronización por eventos de cursos activos y requisitos

**Decisión de Producto:** `courses-service` informa a `llm-service` por eventos
Kafka los cursos activos, sus cambios de nombre y los cambios de
`requiresCalibration` de cursos o desafíos.

**Implicancia de diseño:** los eventos entregan únicamente el tipo e
identificador del recurso, `courseId` cuando corresponda, el estado activo o de
requisito y la nueva versión del nombre visible. Se publican mediante outbox y
se consumen idempotentemente por `eventId`, con `traceparent` y
`X-Request-Id` en headers. `llm-service` actualiza su registro acotado sin
adquirir el catálogo, fechas de vencimiento ni la potestad de publicación.

### D-86 — Recalibración por pérdida periódica de aprobación

**Decisión de Producto:** un cambio de nombre no exige recalibración. Las
calibraciones activas se verifican mediante un job mensual o programado contra
el proveedor; si una calibración deja de aprobar, por ejemplo porque el
proveedor ajustó parámetros del modelo, el sistema solicita una nueva
recalibración.

**Implicancia de diseño:** el resultado de cada verificación conserva proveedor,
deployment, configuración efectiva, fecha, métricas y causa de pérdida de
aprobación. La solicitud de recalibración es auditable y no reescribe la
calibración histórica que previamente había aprobado.

### D-87 — Pérdida de vigencia y diferimiento del evaluador

**Decisión de Producto:** si la calibración periódica falla, el modelo deja de
ser apto para evaluar desafíos del curso de inmediato. El evaluador no conmuta a
un proveedor de respaldo.

**Implicancia de diseño:** el estado de calibración pasa a no vigente para las
nuevas ejecuciones del evaluador y la consulta de estado lo refleja. Las
entregas se aceptan y su cálculo de IA queda diferido hasta que el único modelo
evaluador activo vuelva a ser utilizable y vigente; la calibración fallida y los
intentos históricos quedan preservados para auditoría.

### D-88 — Un único modelo activo para el evaluador

**Decisión de Producto:** el evaluador opera con un único deployment activo y
no admite fallback, pool ni enrutamiento entre modelos.

**Implicancia de diseño:** todos los scores comparables se producen con el mismo
modelo evaluador. Si éste no está disponible o pierde vigencia, no se sustituye
por otro: las nuevas entregas se aceptan y su evaluación de IA se difiere,
conforme a RF-IA-25 y RF-IA-27.

### D-89 — Prioridad administrativa de fallback fuera del evaluador

**Decisión de Producto:** el administrador define el orden de prioridad de los
proveedores fallback para las funciones de IA que admiten más de un modelo.

**Implicancia de diseño:** ni el docente ni el modelo seleccionan libremente el
proveedor de respaldo. La selección recorre la prioridad administrativa y sólo
considera deployments habilitados para la función solicitada. Esta prioridad no
se aplica al evaluador ni altera su modelo activo único.

### D-90 — Conmutación automática en funciones no evaluadoras

**Decisión de Producto:** ante indisponibilidad de una función de IA distinta
del evaluador, el sistema puede conmutar automáticamente al primer fallback
válido según la prioridad administrativa.

**Implicancia de diseño:** la conmutación preserva resiliencia sin usar un
modelo no habilitado. Se audita proveedor saliente, proveedor entrante, función,
motivo y fecha; se notifican los actores acordados. Esta conmutación no se usa
para calcular scores del evaluador ni modifica intentos o evaluaciones cerradas.

### D-91 — Recuperación de proveedor en funciones no evaluadoras

**Decisión de Producto:** cuando el proveedor principal de una función no
evaluadora vuelve a estar disponible, recupera automáticamente su condición de
proveedor activo.

**Implicancia de diseño:** el cambio de retorno se audita y notifica con el
mismo nivel de trazabilidad que una conmutación de contingencia. No aplica a la
selección del modelo evaluador, que requiere su propia calibración y activación.

### D-92 — Configuración y deshabilitación de verificaciones periódicas

**Decisión de Producto:** el administrador puede configurar la frecuencia o
deshabilitar las verificaciones periódicas de calibración.

**Implicancia de diseño:** el backend aplica la política vigente de forma
durable y auditable. Deshabilitar la verificación no elimina ni modifica
calibraciones existentes, sus resultados ni su historial; sólo evita nuevas
ejecuciones programadas hasta que la política vuelva a habilitarse.

### D-93 — Motivo obligatorio sin notificación al deshabilitar verificaciones

**Decisión de Producto:** deshabilitar verificaciones periódicas exige un motivo
administrativo obligatorio, pero no notifica a los docentes.

**Implicancia de diseño:** la auditoría conserva administrador, fecha, política
anterior, motivo y eventual reactivación. La decisión no genera eventos de
notificación a docentes.

### D-94 — Ejecución manual de verificación periódica

**Decisión de Producto:** el administrador puede iniciar manualmente una
verificación de calibración fuera del calendario configurado.

**Implicancia de diseño:** la corrida manual aplica las mismas reglas,
proveedores, métricas, auditoría y efectos de validez que una ejecución
programada; se registra que su disparador fue administrativo.

### D-95 — Desactivación del requisito de calibración

**Decisión de Producto:** cuando el dueño informa
`requiresCalibration: false`, LLM conserva el registro como inactivo para
auditoría y lo oculta de la lista vigente de recursos requeridos.

**Implicancia de diseño:** el registro `requiresCalibration` existe sólo para
que LLM pueda obtener y mostrar los datos de cursos o desafíos requeridos. No
crea una tarea, pendiente ni proceso autónomo por falta de calibración; las
calibraciones ya existentes mantienen su historial independiente.

### D-96 — Agrupación de desafíos requeridos por curso

**Decisión de Producto:** la lista de LLM agrupa los desafíos que requieren
calibración debajo de su curso relacionado.

**Implicancia de diseño:** el registro acotado conserva la relación externa
`challengeId → courseId` y los snapshots vigentes de nombres para presentar la
jerarquía. No replica el catálogo completo ni el estado de publicación de los
desafíos.

### D-97 — Cursos activos provistos por `courses-service`

**Decisión de Producto:** los cursos son inmutables bajo responsabilidad de
`courses-service`, que provee a LLM la lista de cursos activos. Cuando un curso
deja de estar activo, LLM oculta de su lista vigente el curso y sus desafíos
requeridos, conservando su historial.

**Implicancia de diseño:** LLM no decide ni modifica el ciclo de vida del curso.
Usa la información de actividad provista por Cursos para filtrar su registro
acotado de requisitos y snapshots de presentación.

### D-98 — Sincronización inicial e incremental de cursos activos

**Decisión de Producto:** `courses-service` ofrece una lista inicial de cursos
activos para sincronizar LLM y luego emite eventos Kafka para sus altas o
cambios.

**Implicancia de diseño:** LLM puede reconstruir su registro acotado después de
una caída o desincronización y aplicar los cambios incrementales de manera
idempotente. La lista inicial no transfiere la propiedad del catálogo ni del
ciclo de vida de Cursos.

### D-99 — Lista persistente y filtro por existencia de calibración

**Decisión de Producto:** un curso o desafío requerido permanece en la lista de
LLM después de calibrarse, para poder seleccionarlo y recalibrarlo. La lista
ofrece un filtro explícito entre recursos con calibración existente y recursos
sin calibración.

**Implicancia de diseño:** LLM conoce la existencia de sus propias
calibraciones y usa ese dato sólo para filtrar y presentar la lista. El filtro
no crea una pendiente, no solicita una calibración ni aplica una compuerta de
publicación.

### D-100 — Filtro de recursos que requieren recalibración

**Decisión de Producto:** la lista de LLM incluye un filtro específico para
recursos cuya calibración existente requiere recalibración.

**Implicancia de diseño:** el filtro utiliza el estado de calibración ya
registrado por LLM, por ejemplo tras una verificación periódica fallida o un
cambio de seguridad. Sólo mejora la visualización y selección del docente; no
genera una solicitud ni decide bloqueos de publicación.

### D-101 — Desactivación del requisito excluye verificaciones periódicas

**Decisión de Producto:** cuando `requiresCalibration` se desactiva, dejan de
ejecutarse verificaciones periódicas sobre sus calibraciones.

**Implicancia de diseño:** LLM conserva el historial, pero excluye el recurso
inactivo de la planificación de verificaciones hasta que el dueño vuelva a
marcarlo como requerido.

### D-102 — Reactivación según historial de recalibración del modelo

**Decisión de Producto:** al reactivarse `requiresCalibration`, una calibración
histórica puede continuar siendo válida si el modelo usado no fue recalibrado
durante el período inactivo. Si ese modelo fue recalibrado en dicho intervalo,
LLM informa el recurso como sin calibración válida.

**Implicancia de diseño:** LLM cruza la fecha de desactivación/reactivación con
la línea de tiempo de recalibraciones del deployment que usó cada configuración.
La respuesta de estado muestra el motivo de invalidez sin generar una corrida ni
una compuerta automática; el docente puede seleccionarlo para recalibrar.

### D-103 — Validez al reactivar contra el deployment efectivo

**Decisión de Producto:** al reactivarse un requisito, la validez de la
calibración se comprueba contra el único deployment evaluador activo en ese
momento, aunque la calibración histórica haya utilizado otro deployment.

**Implicancia de diseño:** LLM resuelve el deployment evaluador activo y exige
una calibración vigente para esa configuración. Una calibración histórica de
otro deployment queda preservada para auditoría, pero no habilita el estado de
calibración vigente del recurso reactivado.

### D-104 — Estado por falta de proveedor elegible calibrado

**Decisión de Producto:** si no existe ningún proveedor o deployment elegible
con calibración vigente, LLM responde el estado específico
`NO_CALIBRATED_PROVIDER_UNAVAILABLE`.

**Implicancia de diseño:** la respuesta identifica de forma segura el
deployment o la calibración faltante y su causa, para que el docente y el
responsable externo puedan diferenciar este caso de una calibración inexistente
del curso o desafío. No expone secretos, prompts ni detalles internos del
proveedor.

### D-105 — Ausencia de verificación no infiere necesidad de recalibración

**Decisión de Producto:** LLM no exige una calibración ni una verificación
inmediata al reactivar un requisito. Si durante su inactividad no existe una
recalibración registrada del deployment relevante, LLM no puede inferir que la
calibración histórica necesite recalibrarse.

**Implicancia de diseño:** la respuesta de estado refleja exclusivamente hechos
registrados —calibraciones, recalibraciones, seguridad y disponibilidad del
deployment—. La ausencia de verificaciones periódicas no convierte por sí sola
una calibración en inválida ni crea una obligación para el docente.

### D-106 — Calibración aislada por configuración de modelo

**Decisión de Producto:** un cambio explícito de proveedor, modelo o versión
de modelo crea un deployment distinto. Sus calibraciones no se heredan desde el
deployment anterior, porque modelos diferentes pueden evaluar de manera
distinta.

**Implicancia de diseño:** cada calibración y verificación se asocia a un
identificador inmutable de deployment. Al cambiar esa identidad, el estado del
recurso sólo puede ser válido con una calibración propia del nuevo deployment;
el historial anterior se conserva exclusivamente para auditoría.

### D-107 — Detección de cambios silenciosos del proveedor

**Decisión de Producto:** cuando un proveedor cambia silenciosamente el
comportamiento de un modelo sin informar una nueva versión, la verificación
periódica de calibración es el mecanismo para detectar la pérdida de vigencia.

**Implicancia de diseño:** LLM no presume cambios ni invalida una calibración
por especulación. Ejecuta la verificación configurada y, si ésta falla, registra
la recalibración necesaria y aplica las reglas ya definidas de estado,
diferimiento, auditoría y notificación.

### D-108 — Fallo operativo de una verificación periódica

**Decisión de Producto:** si una verificación periódica del modelo evaluador no
puede ejecutarse por cuota agotada del proveedor o por un error interno de
backend, queda pendiente de verificación; no se verifica con un fallback.

**Implicancia de diseño:** el agotamiento de cuota y el error de backend no
invalidan por sí mismos el último resultado de calibración. Se registra la
causa, el estado `verificationPending` y se reintenta según la política
operativa, con trazabilidad y auditoría.

### D-109 — Indisponibilidad del modelo evaluador único

**Decisión de Producto:** si se agota la cuota o el deployment evaluador activo
no está disponible, LLM marca el evaluador como indisponible. No se utiliza
fallback para sustituirlo.

**Implicancia de diseño:** la indisponibilidad no altera ni borra el historial
de calibraciones aprobadas; expresa que no hay un modelo utilizable para una
nueva evaluación. La entrega del desafío no se bloquea ni se posterga: LLM
informa la indisponibilidad y deja el cálculo de IA diferido, mientras conserva
evidencia de la causa.

### D-110 — Alerta administrativa por falta total de modelo

**Decisión de Producto:** la indisponibilidad del modelo evaluador único genera
una alerta inmediata al administrador.

**Implicancia de diseño:** LLM registra el incidente y publica la solicitud de
notificación de manera confiable mediante el mecanismo asíncrono ya definido,
con correlación y causa. La alerta permite intervenir sobre la disponibilidad de
proveedores sin que LLM asuma el rol de gobernar cursos, desafíos o bloqueos.

### D-111 — Aviso a docentes ante indisponibilidad de evaluación

**Decisión de Producto:** ante falta total de un modelo utilizable, también se
notifica a los docentes cuyos cursos o desafíos puedan resultar afectados.

**Implicancia de diseño:** LLM determina los destinatarios a partir de las
asociaciones de calibración y la autorización de cursos que recibe de sus
dueños, sin replicar su catálogo completo. La notificación identifica el
recurso afectado y la indisponibilidad, pero no divulga información sensible de
proveedores ni de otros cursos.

### D-112 — Notificación de recuperación operativa

**Decisión de Producto:** cuando vuelve a existir un modelo utilizable, se
notifica la recuperación al administrador y a los docentes afectados por la
indisponibilidad.

**Implicancia de diseño:** el incidente conserva una transición explícita de
abierto a recuperado y enlaza ambos avisos para evitar ambigüedad. La
recuperación informa el restablecimiento de capacidad; no modifica evaluaciones
históricas ni fuerza recalibraciones fuera de las reglas ya establecidas.

### D-113 — Resiliencia de entregas ante indisponibilidad de IA (RF-IA-27)

**Decisión de Producto:** una indisponibilidad, degradación o cuota agotada de
un modelo o proveedor nunca bloquea, invalida ni posterga la entrega de un
desafío. Si el tutor de IA no está disponible, el alumno resuelve y entrega sin
asistencia y su score de uso de IA es neutro, sin bonus ni penalidad. Si el
evaluador no está disponible, la entrega se acepta, se otorgan en ese momento
el XP base y las monedas, y el score de uso de IA queda pendiente de cálculo
diferido para aplicar su modificador al restablecerse el servicio.

**Implicancia de diseño:** la aceptación de la entrega, el XP base y las
monedas pertenecen a sus servicios dueños; LLM expone o comunica que el cálculo
de IA está diferido, sin gobernar la entrega. La indisponibilidad del evaluador
se registra, notifica y se resuelve posteriormente
sin penalizar al alumno por la dependencia externa.

### D-114 — Reserva preventiva de cuota del evaluador

**Decisión de Producto:** antes de iniciar una evaluación, LLM reserva de forma
preventiva el presupuesto máximo estimado de tokens más un margen de seguridad.
Si la cuota disponible no cubre esa reserva, no inicia una nueva evaluación y la
deja en cálculo diferido hasta recuperar capacidad.

**Implicancia de diseño:** esta admisión protege la cuota y evita cortar una
evaluación en ejecución. Si aun con la reserva el proveedor responde por cuota
agotada durante una llamada, LLM no confirma un score parcial ni cambia de
modelo: descarta el resultado incompleto, reencola el cálculo íntegro y lo
ejecuta nuevamente con el mismo modelo evaluador activo cuando haya capacidad.
La entrega, el XP base y las monedas no se alteran.

### D-115 — Cierre de curso condicionado a scores de IA pendientes

**Decisión de Producto:** un curso no puede cerrarse mientras tenga scores de
uso de IA pendientes de cálculo diferido.

**Implicancia de diseño:** LLM conserva una cola persistente e idempotente de
evaluaciones diferidas y expone el contador de pendientes por curso. El servicio
dueño del cierre consulta ese estado y aplica el bloqueo de RF-IA-34; LLM no
cierra cursos ni modifica la economía del alumno. La cola debe monitorearse por
antigüedad además de cantidad para permitir intervención operativa antes de un
cierre.

### D-116 — Sin excepción administrativa al cierre con pendientes

**Decisión de Producto:** ningún rol, incluido Administración, puede forzar el
cierre de un curso que tenga scores de IA pendientes.

**Implicancia de diseño:** la consulta de pendientes es una precondición dura
del cierre en el servicio que lo gobierna. LLM mantiene el conteo verificable y
la trazabilidad de cada trabajo pendiente, sin exponer una operación de omisión,
vaciado manual ni cierre forzado.

### D-117 — Límite configurable de reintentos diferidos

**Decisión de Producto:** los cálculos de IA diferidos tienen un límite de
reintentos configurable, con cinco intentos como valor predeterminado. Al
agotarlo, el sistema emite un aviso.

**Implicancia de diseño:** cada trabajo conserva el número de intento, las
causas y los momentos de error. La política de reintentos se configura y audita
sin reescribir trabajos históricos; al llegar al límite, LLM detiene los
reintentos automáticos y genera la notificación correspondiente, manteniendo el
trabajo y su estado para trazabilidad.

### D-118 — Reanudación administrativa tras agotar reintentos

**Decisión de Producto:** tras alcanzar el límite de reintentos, sólo un
administrador puede reanudar manualmente el cálculo diferido una vez resuelta
la causa.

**Implicancia de diseño:** la reanudación crea una transición explícita y
auditable con administrador, motivo y momento. Conserva el historial de
intentos previo y vuelve a encolar el trabajo sin habilitar a docentes ni a
otros servicios a omitir el pendiente o forzar el cierre del curso.

### D-119 — Destinatarios del aviso por reintentos agotados

**Decisión de Producto:** al agotarse los reintentos de un cálculo diferido, se
notifica al administrador y a los docentes afectados.

**Implicancia de diseño:** la notificación identifica el recurso y que la
intervención administrativa es necesaria, sin revelar la entrega del alumno ni
detalles sensibles del proveedor. Se vincula al trabajo pendiente y al incidente
para que el seguimiento y la posterior reanudación sean auditables.

### D-120 — Clonado de skills públicas

**Decisión de Producto:** un docente puede clonar una skill pública de otro
docente para adaptarla como skill propia, conservando la referencia a su origen.

**Implicancia de diseño:** el clonado crea una nueva skill independiente, con
su propia versión inicial, autor, visibilidad, favoritos, versiones y ciclo de
vida. La referencia de procedencia es sólo trazable: cambios, archivado o
incidentes de la skill original no modifican automáticamente la copia.

### D-121 — Visibilidad inicial de una skill clonada

**Decisión de Producto:** una skill clonada se crea privada por defecto. Su
nuevo autor decide explícitamente si la publica.

**Implicancia de diseño:** el clonado no amplía la visibilidad de contenido
adaptado sin decisión del docente. La copia se muestra en su banco personal y
mantiene los controles habituales de sanitización, autorización y publicación.

### D-122 — Auditoría de clones ante incidente de seguridad del origen

**Decisión de Producto:** si una versión origen se deshabilita por seguridad,
sus skills clonadas quedan marcadas para auditoría, con trazabilidad hacia el
origen, pero no se deshabilitan automáticamente por ser independientes.

**Implicancia de diseño:** el incidente conserva la relación entre versión de
origen y copias potencialmente afectadas, sus autores y usos. Administración
puede revisar cada clon con evidencia suficiente y aplicar una acción individual
si corresponde; los cambios a la copia no se atribuyen erróneamente al autor de
la original.

### D-123 — Aviso al autor de un clon auditado

**Decisión de Producto:** cuando una skill clonada queda marcada para auditoría
por un incidente de seguridad en su origen, se notifica a su autor.

**Implicancia de diseño:** el aviso identifica la copia y la relación con el
incidente origen, sin exponer contenido sensible ni asumir que la copia es
insegura. Queda ligado a la auditoría para informar posteriormente su resolución
y permitir al docente tomar decisiones sobre sus calibraciones.

### D-124 — Metadatos funcionales para skills

**Decisión de Producto:** cada skill requiere una descripción funcional breve y
puede incluir etiquetas opcionales.

**Implicancia de diseño:** el catálogo indexa nombre, descripción funcional,
etiquetas, curso relacionado, autor, visibilidad y fechas; la búsqueda no
depende de inspeccionar el cuerpo Markdown. La interfaz puede usar esos
metadatos para buscar por función y escalar futuros filtros sin alterar las
versiones inmutables de contenido.

### D-125 — Etiquetas de catálogo y personalizadas

**Decisión de Producto:** una skill puede usar etiquetas provistas por un
catálogo central y etiquetas personalizadas.

**Implicancia de diseño:** el catálogo central aporta vocabulario consistente
para filtros transversales, mientras que las etiquetas personalizadas permiten
expresar necesidades disciplinares nuevas. Ambos tipos se distinguen en los
metadatos para conservar calidad de búsqueda, trazabilidad y una posible
gobernanza futura sin reinterpretar el Markdown.

### D-126 — Descubrimiento global de etiquetas personalizadas públicas

**Decisión de Producto:** las etiquetas personalizadas de una skill pública
aparecen en la búsqueda global del catálogo.

**Implicancia de diseño:** el índice global incorpora esas etiquetas sólo para
contenido cuya visibilidad permita descubrirlo. Las etiquetas de skills privadas
permanecen restringidas a su autor y usuarios autorizados, evitando revelar
temas, funciones o relaciones de contenido no público.

### D-127 — Promoción administrativa de etiquetas

**Decisión de Producto:** un administrador puede promover una etiqueta
personalizada frecuente al catálogo central, sin modificar retrospectivamente
las skills que ya la usan.

**Implicancia de diseño:** la promoción crea una nueva entrada gobernada y
auditada del catálogo. Las asociaciones históricas de etiquetas conservan su
origen y texto; el sistema puede sugerir la etiqueta central para usos futuros
sin reversionar, editar ni cambiar la visibilidad de skills existentes.

### D-128 — Sanitización síncrona antes de persistir una skill

**Decisión de Producto:** toda versión de skill se sanitiza de forma síncrona
durante la carga o el clonado. Sólo se guarda, publica y vuelve seleccionable si
supera los controles de seguridad.

**Implicancia de diseño:** antes de persistir se validan tamaño, codificación,
estructura Markdown y patrones o instrucciones prohibidos. No existe un estado
inicial de skill publicable “en análisis”. Ante rechazo, no se almacena una
versión utilizable; se conserva sólo la evidencia mínima, segura y auditable
necesaria para alarmas e investigación.

### D-129 — Markdown sin imágenes ni enlaces externos

**Decisión de Producto:** las skills Markdown no admiten imágenes ni enlaces
externos.

**Implicancia de diseño:** la sanitización rechaza esas construcciones antes de
persistir. Las skills permanecen como instrucciones textuales autocontenidas,
sin recursos remotos que aumenten tokens, agreguen latencia, filtren datos o
amplíen la superficie de seguridad del visor.

### D-130 — Markdown sin HTML embebido

**Decisión de Producto:** las skills Markdown no admiten HTML embebido.

**Implicancia de diseño:** el parser y sanitizador aceptan sólo el subconjunto
textual de Markdown definido por la plataforma. El HTML se rechaza antes de
persistir para evitar contenido oculto, comportamientos de renderizado no
necesarios y consumo de contexto que no mejora el razonamiento del modelo.

### D-131 — Bloques de código como contenido literal

**Decisión de Producto:** las skills pueden contener bloques de código sólo
como ejemplos o instrucciones literales; nunca se ejecutan.

**Implicancia de diseño:** el visor los representa como texto y el AI Gateway
los envía al modelo únicamente como parte del contexto declarado. Ningún
controller, worker ni proveedor interpreta esos bloques como comandos, scripts,
herramientas o llamadas externas.

### D-132 — Reproceso seguro de entregas diferidas por incidente de skill

**Decisión de Producto:** si una entrega queda diferida porque su skill se
volvió insegura, tras reemplazarla y recalibrar, se evalúa con la nueva
configuración segura y calibrada.

**Implicancia de diseño:** el trabajo diferido conserva el snapshot original y
el incidente para auditoría, pero referencia explícitamente la configuración
segura que produjo su resultado final. Nunca se invoca la versión deshabilitada;
la actualización permite resolver el pendiente sin bloquear la entrega ni el
cierre del curso.

### D-133 — Aviso al alumno al resolver un score diferido

**Decisión de Producto:** cuando se calcula y aplica posteriormente el
modificador de IA de una entrega diferida, se notifica al alumno y se explica
que su score estaba pendiente.

**Implicancia de diseño:** la notificación enlaza al resultado y su desglose
permitido, sin revelar prompts, skills, detalles de seguridad ni información del
proveedor. El servicio dueño de la economía aplica el modificador y solicita el
aviso; LLM aporta el resultado trazable del cálculo diferido.

### D-134 — Cambio de visibilidad de pública a privada

**Decisión de Producto:** al cambiar una skill pública a privada, las
calibraciones existentes de otros docentes conservan su versión fijada, pero se
impiden nuevas selecciones por terceros.

**Implicancia de diseño:** el cambio de visibilidad afecta el índice y los
permisos de descubrimiento, no los snapshots inmutables de calibraciones ni
evaluaciones ya asociadas. El historial mantiene que la versión fue pública al
momento de su uso, mientras que la nueva selección queda limitada al autor y a
Administración.

### D-135 — Favorito externo sin clon tras privatizar una skill

**Decisión de Producto:** si otro docente tenía como favorita una skill que se
vuelve privada y no creó un clon, su favorito permanece visible como referencia
no disponible y no puede adjuntarse.

**Implicancia de diseño:** el banco de skills explica que la versión dejó de
estar disponible para ese usuario y conserva la trazabilidad de su favorito.
No se elimina ni se redirige automáticamente a otra skill, evitando cambios
silenciosos en las preferencias del docente.

### D-136 — Un favorito no migra automáticamente a un clon

**Decisión de Producto:** aunque un docente haya creado un clon de la skill
original, su favorito no migra automáticamente a ese clon.

**Implicancia de diseño:** un clon puede haber sido modificado y no equivale a
la versión que el docente marcó originalmente. La interfaz conserva el favorito
existente y permite al docente elegir explícitamente si marca su clon como
favorito, manteniendo ambas referencias auditables.

### D-137 — Concurrencia en la activación de calibración global

**Decisión de Producto:** si dos docentes autorizados recalibran el mismo
curso, quien intente activar una configuración basada en una versión global ya
superada debe revisarla y rebasarla sobre la versión activa actual antes de
activarla.

**Implicancia de diseño:** la activación aplica control de concurrencia
optimista sobre la versión base y rechaza una activación desactualizada con una
explicación de conflicto. Las corridas, borradores y evidencias de ambos
docentes se conservan; el rebase explícito produce una nueva configuración
auditable y evita sobrescribir una calibración activa en silencio.

### D-138 — Concurrencia en la activación de subcalibración

**Decisión de Producto:** la misma regla de control de concurrencia y rebase se
aplica cuando dos docentes editan o recalibran simultáneamente un mismo desafío.

**Implicancia de diseño:** una subcalibración sólo se activa si su baseline de
curso y la versión activa del desafío siguen siendo las esperadas. Ante cambios
concurrentes, el backend conserva el borrador, muestra el conflicto y exige una
resolución explícita antes de generar un nuevo perfil efectivo y activarlo.

### D-139 — Lectura compartida del historial por curso autorizado

**Decisión de Producto:** todo docente autorizado en un curso puede consultar
el historial completo de sus calibraciones globales y subcalibraciones, aunque
otra persona autorizada haya creado una versión.

**Implicancia de diseño:** la lectura se autoriza por pertenencia vigente al
curso y no por autoría de la corrida. El historial muestra creador, fechas,
versiones, resultados y auditoría, sin ampliar el acceso a cursos ajenos ni a
contenido privado no asociado al curso.

### D-140 — Reactivación autorizada de versiones históricas aprobadas

**Decisión de Producto:** cualquier docente actualmente autorizado en el curso
puede reactivar una versión histórica que haya aprobado la calibración y no
haya sido descartada por un fallo de calibración.

**Implicancia de diseño:** la reactivación valida el estado aprobado y vigente
de la versión antes de cambiar la referencia activa. Versiones fallidas o
descartadas permanecen consultables para auditoría, pero no son activables; la
transición conserva actor, motivo, momento y configuración efectiva.

### D-141 — Precondiciones de seguridad y vigencia al reactivar

**Decisión de Producto:** no puede reactivarse una versión histórica aprobada
si usa una skill deshabilitada por seguridad o un modelo cuya calibración perdió
vigencia.

**Implicancia de diseño:** la reactivación verifica las versiones de skills y
el estado actual del deployment evaluador antes de aplicarse. Si alguna
precondición falla, el sistema explica la causa y exige reemplazo seguro y/o
recalibración; el historial aprobado queda intacto, pero no se reutiliza de
forma insegura.

### D-142 — Módulo autónomo de gestión de skills

**Decisión de Producto:** Skills tiene una vista y catálogo propios, separados
de las pantallas de desafíos y de la selección durante una calibración.

**Implicancia de diseño:** el módulo centraliza catálogo, búsqueda, filtros,
favoritos, carga, versiones, visibilidad, clonado, seguridad y vistas de uso.
Los selectores de calibración reutilizan ese catálogo, pero no duplican ni se
convierten en el lugar principal para gestionar skills.

### D-143 — Historial de uso de skills por desafío

**Decisión de Producto:** dentro del módulo Skills, la vista de uso muestra
tanto los desafíos con configuración efectiva actual como el historial de
calibraciones anteriores que utilizaron cada skill.

**Implicancia de diseño:** la consulta de usos distingue asociación activa e
histórica, versión exacta de skill, curso, desafío y versión de calibración. El
acceso se limita a los cursos autorizados del docente o a Administración, y la
información se vincula a los snapshots para auditoría.

### D-144 — Selección unificada de recursos dentro de cada calibración

**Decisión de Producto:** cada formulario de calibración reúne su recurso
objetivo, el Golden Set, el paquete de rúbricas y una o más skills. El global
elige un curso; el específico elige un curso ya calibrado y luego su desafío.

**Implicancia de diseño:** la interfaz compone la configuración mediante listas
o modales desplegables según corresponda y muestra el perfil resultante antes
de iniciar la corrida. El backend recibe una única solicitud tipada con las
versiones exactas seleccionadas, valida permisos, seguridad, compatibilidad y
presupuesto de contexto, y materializa su snapshot inmutable.

### D-145 — Flujos separados para curso y desafío

**Decisión de Producto:** la calibración global de curso y la subcalibración de
desafío son flujos separados. Para acceder a las calibraciones de desafíos, el
docente primero selecciona un curso que ya esté calibrado.

**Implicancia de diseño:** la interfaz presenta entradas separadas para ambos
flujos. La subcalibración filtra y muestra sólo los desafíos asociados al curso
global activo y aprobado que estén registrados en LLM como requeridos de
calibración, evitando seleccionar desafíos de otro curso o iniciar una
calibración específica sin su baseline válida.

### D-146 — Una corrida de subcalibración por desafío

**Decisión de Producto:** cada corrida de subcalibración corresponde a un único
desafío; no se calibran varios desafíos en una misma corrida.

**Implicancia de diseño:** la configuración efectiva, Golden Set, rúbrica,
skills, métricas y resultado quedan vinculados inequívocamente a un solo
`challengeId`. Esto evita mezclar contextos académicos y conserva explicabilidad
por desafío, aun cuando se ejecuten corridas independientes en paralelo.

### D-147 — Borradores persistentes de calibración

**Decisión de Producto:** el docente puede guardar un borrador incompleto de
calibración global o de subcalibración y retomarlo más adelante sin iniciar una
corrida.

**Implicancia de diseño:** el borrador conserva sus selecciones y validaciones
parciales, pero no invoca al proveedor ni altera la calibración activa. Sólo al
iniciar una corrida se exigen todos los recursos obligatorios, permisos,
seguridad, compatibilidad y presupuesto de contexto; la corrida resultante crea
su snapshot inmutable.

### D-148 — Privacidad de borradores de calibración

**Decisión de Producto:** los docentes autorizados distintos del creador no
pueden ver ni editar sus borradores de calibración.

**Implicancia de diseño:** el borrador se autoriza por su creador y permanece
fuera de las vistas compartidas del curso. Una vez iniciada la corrida, su
resultado y posterior versión entran en el historial compartido según las
permisiones del curso; no se exponen configuraciones incompletas a otros
docentes.

### D-149 — Consulta administrativa de borradores privados

**Decisión de Producto:** Administración puede consultar borradores privados
para fines de auditoría y soporte.

**Implicancia de diseño:** este acceso es de solo lectura, queda auditado con
actor y motivo, y no habilita a Administración a editar, iniciar, publicar o
exponer el borrador a otros docentes. La privacidad entre docentes del curso se
mantiene intacta.

### D-150 — Autorización de curso gobernada por Cursos

**Decisión de Producto:** Cursos gobierna las autorizaciones de docentes por
curso. Si un docente pierde esa autorización, pierde el acceso a borradores,
historial y operaciones de calibración de ese curso.

**Implicancia de diseño:** LLM no administra ni replica membresías. En cada
consulta o mutación protegida, aplica la identidad y autorización vigente que
llegan por el API Gateway y los contratos de Cursos. El historial se conserva
para auditoría bajo acceso administrativo, pero no queda accesible para quien
ya no esté autorizado en el curso.

### D-151 — Skills privadas restringidas por autorización de curso

**Decisión de Producto:** al perder autorización sobre un curso, un docente no
puede gestionar sus skills privadas asociadas a ese curso; permanecen sólo para
auditoría administrativa.

**Implicancia de diseño:** LLM aplica la autorización vigente provista por
Cursos al listar, abrir o modificar una skill privada vinculada al curso. No
administra el alcance ni la membresía; sólo protege sus propios datos y
operaciones frente a la autorización delegada.

### D-152 — Carga de skill desde el formulario de calibración

**Decisión de Producto:** si el docente carga una nueva skill desde un
formulario de calibración y ésta supera la sanitización, se agrega
automáticamente a la selección actual.

**Implicancia de diseño:** la carga conserva el flujo síncrono de seguridad y
crea la versión de skill antes de incorporarla al borrador de calibración. Si la
validación falla, no se añade ni se inicia la corrida; el docente recibe el
feedback seguro correspondiente y puede elegir otra skill o corregir la carga.

### D-153 — Estimación visible de presupuesto de contexto

**Decisión de Producto:** el formulario de calibración muestra en tiempo real
la estimación de tokens del perfil seleccionado y advierte antes de iniciar si
supera el límite.

**Implicancia de diseño:** cada cambio de rúbrica, Golden Set o skills actualiza
la estimación y explica los componentes de mayor consumo. La interfaz orienta al
docente, pero el backend y el AI Gateway conservan la validación autoritativa
final inmediatamente antes de la invocación.

### D-154 — Metadatos de skill sin nueva versión de contenido

**Decisión de Producto:** cambiar sólo el nombre, descripción funcional,
etiquetas o visibilidad de una skill no crea una nueva versión si su contenido
Markdown no cambia.

**Implicancia de diseño:** las versiones inmutables representan instrucciones
efectivas y su hash de contenido. Los metadatos de catálogo se actualizan y
auditan por separado, no invalidan calibraciones ni exigen recalibración; los
snapshots conservan el nombre visible que correspondía a su momento histórico.

### D-155 — Corridas de calibración sin cancelación manual

**Decisión de Producto:** un docente no puede cancelar una corrida de
calibración después de iniciarla.

**Implicancia de diseño:** una corrida iniciada termina con su resultado o por
los controles automáticos de seguridad y fallos ya definidos. La interfaz no
ofrece cancelación manual, evita estados parciales ambiguos y conserva la
evidencia y trazabilidad completas de toda corrida iniciada.

### D-156 — Falla transitoria de calibración sin reintento automático

**Decisión de Producto:** si una corrida de calibración falla por cuota agotada
o un error temporal de proveedor, finaliza e informa la causa al docente; no se
reintenta automáticamente.

**Implicancia de diseño:** la corrida queda en un estado terminal explicable y
auditable, sin activar resultados parciales ni consumir cuota adicional sin
decisión docente. El docente puede corregir la configuración, esperar capacidad
o iniciar una nueva calibración cuando corresponda.

### D-157 — Indicadores de scores diferidos por curso

**Decisión de Producto:** la consulta de pendientes por curso expone el total
de scores diferidos, la antigüedad del más antiguo y si alguno agotó sus
reintentos.

**Implicancia de diseño:** el servicio dueño del cierre dispone de una
precondición verificable, y Administración puede distinguir acumulación normal
de una cola que requiere intervención. Los indicadores no exponen entregas ni
datos del alumno; LLM conserva el detalle interno para reintento y auditoría.

### D-158 — Concurrencia de calibraciones con reserva agregada de cuota

**Decisión de Producto:** docentes distintos pueden ejecutar calibraciones en
paralelo; cada docente ejecuta una sola corrida a la vez. Antes de cada inicio,
LLM valida que el saldo de cuota alcance también para las reservas ya tomadas
por otras corridas concurrentes.

**Implicancia de diseño:** la admisión reserva de manera atómica el presupuesto
estimado de toda corrida antes de invocar al proveedor, evitando que dos
docentes sobreasignen la misma cuota. Si no hay saldo suficiente, la nueva
corrida no inicia y el docente recibe una causa clara; al finalizar o fallar, la
reserva se concilia con el uso real y libera el remanente.

### D-159 — Modelo lógico propio y trazable de LLM para skills y calibraciones

**Decisión técnica:** LLM persiste en su base exclusiva los agregados de
skill, versión inmutable de contenido, metadato de catálogo, favorito, linaje
de clon, incidente de seguridad, perfil de calibración, corrida, activación,
verificación periódica, reserva de cuota, score diferido, auditoría y outbox.
Los cursos y desafíos se identifican sólo por sus IDs, snapshots autorizados y
estado de registro para calibración; no se replica su catálogo ni ciclo de
vida.

**Implicancia de diseño:** el contenido Markdown, sus hashes y los perfiles
efectivos quedan inmutables una vez usados. Los metadatos modificables y los
estados operativos se conservan separados de esos snapshots. Las transiciones
que deben sobrevivir a reintentos o producir eventos se registran junto con su
auditoría y outbox en la misma transacción, para que una falla no deje una
calibración, una reserva o una notificación en un estado contradictorio.

### D-160 — Compilador determinista del perfil efectivo de desafío

**Decisión técnica:** la evaluación y la subcalibración no reciben la
calibración de curso y la de desafío como contextos independientes. Un
compilador puro construye un único `EffectiveEvaluationProfile` a partir del
snapshot inmutable de `CourseBaseline`, el `ChallengeOverlay` y las versiones
exactas de sus recursos. Con los mismos datos de entrada, versión de plantilla
y versión de política, debe producir el mismo perfil canónico y el mismo hash.

**Reglas de compilación:**

- La rúbrica parte de las dimensiones heredadas en su orden canónico. El
  overlay modifica sólo los campos declarados, desactiva mediante peso 0 % y
  agrega dimensiones al final en el orden explícito del docente. Se validan
  IDs sin duplicados, pesos no negativos, suma global de 100 % y pesos internos
  de subcriterios activos de 100 % por dimensión.
- Se resuelve un único Golden Set: el del desafío si fue reemplazado; de lo
  contrario, el heredado. Nunca se envían ambos conjuntos completos en la misma
  invocación. Antes de ejecutar se valida que sus referencias humanas cubran
  cada dimensión y subcriterio activo del perfil resultante.
- Las skills se resuelven por versión y hash: se parte del orden de la base,
  una sustitución conserva el lugar de la skill reemplazada, las exclusiones la
  retiran y las nuevas se insertan en la posición explícita elegida por el
  docente. Si dos entradas tienen el mismo hash de contenido, sólo queda la
  primera posición efectiva y se conserva el motivo de la deduplicación en el
  snapshot.
- El contexto enviado al AI Gateway se genera únicamente desde ese perfil
  materializado: política y esquema de salida controlados por LLM, rúbrica
  efectiva, Golden Set resuelto y skills delimitadas como contenido de usuario.
  No incluye prompts originales de base u overlay ni permite que una skill
  altere las instrucciones de sistema, seguridad o el esquema estructurado de
  respuesta.
- Se calcula el presupuesto sobre el contexto ya materializado —instrucciones
  fijas, rúbrica, Golden Set, skills, evidencia y reserva de salida—. No se
  trunca ningún componente para hacerlo entrar: si supera el límite certificado
  del despliegue, la corrida se rechaza antes de invocar y explica qué recurso
  debe revisarse.

**Implicancia de diseño:** el perfil, su hash, sus recursos ordenados y las
versiones de plantilla, política, esquema de salida y despliegue de modelo se
guardan como snapshot de la corrida. La validación de la respuesta estructurada
usa exactamente los IDs de ese perfil. Así se evita que el modelo arbitre reglas
contradictorias, se reproduce cualquier resultado histórico y la interfaz puede
mostrar una comparación explícita entre base, overlay y resultado efectivo.

### D-161 — Máquina de estados de corridas, activaciones y verificaciones

**Decisión de diseño confirmada:** la corrida de calibración, la activación de
una configuración y su verificación periódica son agregados con máquinas de
estado distintas. Una corrida aprobada no cambia de estado al activarse ni se
reescribe si posteriormente pierde vigencia. La activación es un puntero
versionado por alcance; la verificación comprueba su aptitud operativa.

#### 1. Corrida de calibración (`CalibrationRun`)

La corrida guarda su snapshot inmutable de perfil efectivo, recursos y
deployment antes de entrar a la cola. Sus estados son:

```text
QUEUED  -> RUNNING
RUNNING -> PASSED | FAILED
QUEUED  -> FAILED   (control automático antes de invocar)
RUNNING -> FAILED   (control automático durante la ejecución)
```

- `PASSED` y `FAILED` son terminales. `PASSED` requiere cumplir las reglas de
  aprobación vigentes para el Golden Set; `FAILED` conserva un código de causa
  controlado, por ejemplo resultado fuera de tolerancia, error de proveedor,
  respuesta inválida o política de seguridad.
- No existe cancelación manual de una corrida iniciada, ni para docente ni para
  Administración. Ante un error transitorio tampoco hay reintento automático:
  el docente inicia otra corrida, con nuevo `runId` y nuevo snapshot.
- Si un control externo de seguridad invalida una skill antes o durante la
  ejecución, la corrida termina en `FAILED` con causa `SECURITY_POLICY`. Nunca
  se completa ni se vuelve a ejecutar con esa versión afectada.
- El estado histórico `CANCELLED` que pudiera aparecer en documentos o
  contratos anteriores no tiene transición habilitada en este flujo. Se
  deprecará al congelar el contrato del bloque 6; no debe implementarse una
  acción de cancelación para producirlo.

Solo una corrida `PASSED` puede ser elegida como origen de una activación.
Reintentar, recalibrar o sustituir recursos siempre crea una corrida nueva; no
revive ni modifica una corrida terminal.

#### 2. Activación por alcance (`CalibrationActivation`)

Una activación referencia una corrida `PASSED` y se identifica por su alcance:
`COURSE(courseId)` para la base global o `CHALLENGE(challengeId)` para el perfil
específico. Hay como máximo una activación vigente por cada clave de alcance.

```text
ACTIVE -> SUPERSEDED
ACTIVE -> SUSPENDED_RECALIBRATION_REQUIRED
ACTIVE -> SUSPENDED_SECURITY
SUSPENDED_RECALIBRATION_REQUIRED -> SUPERSEDED
SUSPENDED_SECURITY                -> SUPERSEDED
```

- Activar una corrida aprobada crea una nueva activación y pasa la activación
  anterior del mismo alcance a `SUPERSEDED`. Reactivar una corrida histórica
  aprobada crea otra activación; nunca altera la activación ni la corrida
  anteriores.
- `SUSPENDED_RECALIBRATION_REQUIRED` representa que una verificación terminó
  fuera de tolerancia. Desde ese momento la configuración ya no es apta para
  nuevos scores; se necesita una nueva corrida `PASSED` y una activación
  explícita para recuperarla.
- `SUSPENDED_SECURITY` representa una suspensión preventiva disparada por un
  control de seguridad externo. La causa y su pipeline se resuelven en el
  bloque pendiente de sanitización; esta máquina sólo consume su resultado y
  bloquea futuros usos de la configuración afectada.
- La recuperación de disponibilidad del proveedor no rehabilita por sí sola
  una activación suspendida por tolerancia o seguridad. Tampoco hay sustitución
  automática de modelo, de skill ni de perfil.
- La activación global nueva conserva las activaciones específicas existentes
  hasta que cada desafío tenga su recalibración correspondiente. Las nuevas
  subcalibraciones deben partir de la base global vigente; si su base cambió,
  deben rebasing y revisión antes de ejecutarse o activarse.

#### 3. Verificación periódica (`CalibrationVerification`)

La verificación es una ejecución separada sobre el snapshot de la activación y
el deployment que ésta referencia. No crea una calibración activable ni
reemplaza su corrida original.

```text
QUEUED  -> RUNNING
RUNNING -> PASSED | FAILED_TOLERANCE | FAILED_OPERATIONAL
QUEUED/RUNNING -> CANCELLED  (la activación fue sustituida antes de terminar)
```

- La planificación crea como máximo una verificación abierta por activación y
  ventana programada. Una petición manual administrativa usa la misma máquina,
  pero conserva su origen manual.
- `PASSED` mantiene la activación en `ACTIVE` y actualiza su última
  verificación satisfactoria.
- `FAILED_TOLERANCE` mueve la activación a
  `SUSPENDED_RECALIBRATION_REQUIRED`; no se puntúan nuevas evaluaciones con
  ella.
- `FAILED_OPERATIONAL` —por indisponibilidad del proveedor, cuota agotada o
  error interno— no invalida por sí mismo la última calibración aprobada. La
  verificación queda pendiente para reintento según la política operativa y
  no se usa un modelo de fallback para el evaluador.
- `CANCELLED` sólo evita aplicar el resultado de una verificación cuya
  activación ya fue reemplazada. No cancela, revierte ni modifica ninguna
  corrida de calibración.

#### 4. Estado efectivo y manejo de concurrencia

El estado consultable se compone internamente a partir de dos dimensiones, sin
mezclar la validez de la calibración con la disponibilidad momentánea del
deployment:

```text
calibrationValidity  = NO_ACTIVE | VALID | VERIFICATION_PENDING
                     | RECALIBRATION_REQUIRED | SECURITY_SUSPENDED

evaluatorAvailability = AVAILABLE | UNAVAILABLE
```

Los nombres, campos y códigos HTTP definitivos quedan fuera de esta decisión y
se congelarán exclusivamente en el bloque 6 de contratos.

Toda operación mutante aplica estas reglas de concurrencia:

- Una misma `Idempotency-Key`, actor, operación y payload devuelve el resultado
  original. Reutilizarla con un payload diferente se rechaza como conflicto de
  idempotencia.
- Las transiciones condicionan el estado y la revisión actual del agregado. Si
  otro comando ya lo cambió, el comando tardío falla por estado obsoleto; no se
  fuerza ni se repite una transición terminal.
- El preview de activación fija la corrida, las revisiones de las activaciones
  involucradas y las asociaciones migrables. La confirmación debe comprobar
  ese token completo: si cambió cualquier elemento, rechaza toda la operación
  como preview obsoleto, sin migraciones parciales.
- Un worker toma una corrida o verificación mediante un lease. Sólo el mismo
  lease puede completar la transición `RUNNING`; si vence, otro worker puede
  retomarla sin que ambos apliquen resultados.
- El primer intento mantiene el bloqueo condicional ya definido sobre la
  asociación desafío-calibración. Una asociación bloqueada nunca se migra ni
  cambia por una activación posterior.

Esta decisión define únicamente el comportamiento de dominio y de
concurrencia del punto 1. No adelanta el libro de cuota (punto 2), el pipeline
de sanitización (punto 4) ni la observabilidad, auditoría, métricas o pruebas
(punto 9), que siguen fuera del sprint conforme al corte de alcance vigente.

## Propuestas técnicas pendientes de confirmación

Las entradas D-162 a D-166 desarrollan los puntos que sí continúan en
discusión durante este sprint. Son **propuestas**, no decisiones de Producto ni
contratos publicados: no autorizan por sí solas cambios de OpenAPI, AsyncAPI,
persistencia o implementación. El lector debe usarlas para revisar y cerrar
decisiones, no para anticipar desarrollo.

### D-162 — Propuesta: cola durable de evaluaciones diferidas

**Alcance:** punto 3. Esta propuesta define la cola y su idempotencia; no
define el libro de cuota, que permanece fuera de alcance.

**Propuesta de modelo:** una `DeferredEvaluation` representa la obligación de
calcular una única vez el score de uso de IA de un `attemptId`. Conserva la
asignación y el perfil efectivo que correspondían al intento, su razón de
diferimiento, número de intentos, próximo intento y el historial de fallos. No
almacena ni modifica el resultado académico, XP o monedas.

```text
DEFERRED -> QUEUED -> RUNNING -> COMPLETED
    ^          ^         |
    |          |         +-> RETRY_WAIT -> QUEUED
    |          |                              |
    |          +------------------------------+
    |
    +-- condición aún no apta

RETRY_WAIT -> RETRY_EXHAUSTED
RETRY_EXHAUSTED --reanudación administrativa--> QUEUED
```

- `DEFERRED` significa que no puede comenzar todavía por una causa tipada:
  `CALIBRATION_UNAVAILABLE`, `EVALUATOR_UNAVAILABLE`,
  `CALIBRATION_RECALIBRATION_REQUIRED` o `PROFILE_SECURITY_SUSPENDED`. Sólo
  una condición apta la mueve a `QUEUED`.
- `QUEUED` es apta para ser tomada por un worker; `RUNNING` posee un lease de
  ejecución. `RETRY_WAIT` conserva un `nextAttemptAt` calculado por la política
  vigente. `RETRY_EXHAUSTED` no se considera resuelta: sigue contando como
  score pendiente y bloquea el cierre del curso.
- `COMPLETED` es el único estado que quita la obligación pendiente. No existe
  una transición de omisión, vaciado, cancelación ni cierre forzado, incluso
  para Administración.
- La reanudación desde `RETRY_EXHAUSTED` requiere rol administrativo y motivo.
  Conserva todos los intentos previos, incrementa una época de reanudación y
  vuelve a `QUEUED`; no crea otra obligación para el mismo intento.
- El límite de reintentos se lee de la política vigente al programar el
  siguiente intento. El valor de inicio propuesto es cinco, conforme a D-117;
  un cambio de política no reescribe intentos ya registrados.

**Idempotencia de negocio propuesta:** existe una única obligación abierta o
completada por `(attemptId, evaluationKind=AI_USAGE)`. El intento conserva el
`evaluationProfileSnapshotId` que le correspondía al inicio. Al completar, la
misma transacción registra el resultado inmutable, marca la obligación
`COMPLETED` y prepara la comunicación posterior. Un índice único sobre esa
identidad impide que una repetición, un lease vencido o una respuesta tardía
creen dos scores.

El AI Gateway recibe además una clave estable derivada del `evaluationId` para
las invocaciones que el proveedor pueda hacer idempotentes. Si el proveedor no
confirma el resultado de una llamada que expiró, la cola puede reintentar la
invocación, pero nunca publica ni persiste un segundo score: sólo la primera
transacción que adquiere la unicidad de `evaluationId` puede completar.

**Reanudación y perfil histórico:** la cola nunca sustituye silenciosamente el
perfil del intento por la calibración activa posterior. Si el perfil histórico
está suspendido por seguridad o ya no es apto, la obligación queda diferida con
la causa correspondiente. La regla de negocio para habilitar una evaluación
histórica contra un perfil nuevo debe resolverse expresamente antes de
implementarse; mientras no exista esa decisión, no se remapea ni se puntúa el
intento con otra configuración.

**Concurrencia propuesta:** el worker reclama trabajos vencidos mediante lease
y transición condicional `QUEUED -> RUNNING`. Sólo quien conserva el mismo
`leaseId` puede terminar o programar el reintento. La recepción repetida de un
cierre de intento devuelve la misma obligación por su clave de negocio; no
duplica filas ni invocaciones simultáneas.

**Pendientes para confirmar:** el conjunto final de causas de diferimiento y
la política específica para un perfil histórico que queda suspendido de forma
permanente. Ninguno de esos pendientes habilita una excepción al bloqueo de
cierre.

### D-163 — Propuesta: ejecución de verificaciones periódicas y degradación

**Alcance:** punto 5. Complementa la máquina de estados D-161 y define cómo se
planifica y coordina una verificación; no diseña la reserva ni conciliación de
cuota del punto 2.

**Propuesta de planificación:** una `VerificationPolicy` administrada contiene
`enabled`, frecuencia, próxima ejecución y fecha de última modificación. Al
deshabilitarla se exige motivo administrativo y se detienen únicamente nuevas
ejecuciones programadas; no se modifica ninguna calibración ni resultado
histórico. Una solicitud administrativa manual crea una verificación con
origen `MANUAL`, pero aplica exactamente las mismas métricas y efectos que una
programada.

El planificador considera sólo activaciones de recursos que continúan
registrados con `requiresCalibration=true`. Por cada `(activationId,
verificationWindow)` crea como máximo una verificación. Una restricción única
evita que dos instancias del scheduler creen la misma ventana; si ya existe una
en `QUEUED` o `RUNNING`, un disparo posterior la reutiliza en lugar de abrir
otra.

**Propuesta de concurrencia:** las verificaciones se ejecutan con un pool
separado y con límite de concurrencia por deployment evaluador. Un lease evita
dos workers sobre la misma verificación. Las verificaciones manuales no
preemptan ni duplican una corrida abierta: se asocian a ella o se programan a
continuación según la política de prioridad que se confirme.

**Resultado y degradación:**

| Resultado de la verificación | Efecto sobre activación | Efecto sobre nuevas evaluaciones |
|---|---|---|
| `PASSED` | Permanece `ACTIVE`; actualiza última comprobación válida. | Pueden ejecutarse si el deployment está disponible. |
| `FAILED_TOLERANCE` | Pasa a `SUSPENDED_RECALIBRATION_REQUIRED`. | Se difieren hasta nueva corrida aprobada y activada. |
| `FAILED_OPERATIONAL` | Mantiene su última validez; marca `VERIFICATION_PENDING`. | Sólo se difieren si el evaluador también está `UNAVAILABLE`. |
| Activación sustituida durante la corrida | La verificación queda `CANCELLED`; no aplica su resultado. | No altera la nueva activación. |

No existe fallback para el evaluador. La recuperación operativa del deployment
restaura `evaluatorAvailability=AVAILABLE`, pero no recupera una activación que
falló por tolerancia o fue suspendida por seguridad: para eso se requiere una
nueva corrida `PASSED` y activación explícita, como fija D-161.

**Reintento propuesto:** sólo `FAILED_OPERATIONAL` se reprograma. La próxima
ejecución se decide por una política de backoff separada de los reintentos de
scores diferidos. El fallo operativo no se convierte en fallo de tolerancia ni
induce una recalibración por especulación.

**Pendientes para confirmar:** representación exacta de la frecuencia
(intervalo o calendario), límite de concurrencia por deployment y prioridad
entre una solicitud manual y una ventana programada ya abierta.

### D-164 — Propuesta: superficie HTTP y congelamiento de campos

**Alcance:** punto 6. Esta es una propuesta de contrato para revisión previa;
no modifica `llm-service.openapi.yaml` ni acuerda aún consumidores. Todas las
rutas públicas quedan bajo `/api/llm/**`, pasan por API Gateway, requieren JWT
M2M con `aud=llm-service`, scopes mínimos por operación y propagan
`traceparent`, `X-Request-Id` e `Idempotency-Key` en cada escritura. Los errores
se expresarán como RFC 7807 con un `codigo` estable.

#### Consulta de estado de calibración

Se propone conservar los recursos ya acordados:

```http
GET /api/llm/courses/{courseId}/calibration-status
GET /api/llm/courses/{courseId}/challenges/{challengeId}/calibration-status
```

Respuesta propuesta, sin rúbricas, Golden Set, skills, prompts ni contenido
académico:

```json
{
  "registered": true,
  "calibrationValidity": "VALID",
  "evaluatorAvailability": "AVAILABLE",
  "effectiveCalibrationRunId": "uuid | null"
}
```

- `registered=false` indica que LLM no posee un registro de requisito para el
  recurso y no crea ninguno por leerlo.
- `calibrationValidity` propone los valores `NO_ACTIVE`, `VALID`,
  `VERIFICATION_PENDING`, `RECALIBRATION_REQUIRED` y `SECURITY_SUSPENDED`.
- `evaluatorAvailability` propone `AVAILABLE` o `UNAVAILABLE`, separado de la
  validez para distinguir una calibración vigente de un proveedor temporalmente
  indisponible.
- `effectiveCalibrationRunId` es nulo cuando no existe configuración efectiva.
  No se expone una causa técnica del proveedor, secretos ni estados de otros
  cursos.

Esta propuesta ajusta D-81: allí `calibrationState` era un único campo. Se
recomienda sustituirlo por las dos dimensiones anteriores, ya que un solo enum
no puede expresar sin ambigüedad validez y disponibilidad. Esta modificación
queda pendiente de confirmación antes de cualquier OpenAPI.

#### Indicadores de scores diferidos

Se propone un recurso de sólo lectura para el servicio que gobierna el cierre y
para las vistas autorizadas:

```http
GET /api/llm/courses/{courseId}/deferred-evaluation-summary
```

Respuesta propuesta:

```json
{
  "pendingCount": 12,
  "oldestPendingAt": "2026-09-21T18:20:00Z | null",
  "retryExhaustedCount": 1,
  "hasRetryExhausted": true,
  "updatedAt": "2026-09-21T18:25:00Z"
}
```

El recurso no devuelve `attemptId`, alumno, transcripciones ni detalle de una
entrega. `pendingCount` incluye `DEFERRED`, `QUEUED`, `RUNNING`, `RETRY_WAIT` y
`RETRY_EXHAUSTED`; sólo `COMPLETED` queda fuera. `oldestPendingAt` es nulo con
contador cero. El dueño del cierre usa el contador, no una operación de
excepción de LLM.

#### Comandos de calibración propuestos

Para evitar que la API replique todo el perfil en cada inicio, la corrida debe
partir de un borrador persistente y privado. Se proponen estos recursos:

| Operación propuesta | Cuerpo final propuesto | Resultado |
|---|---|---|
| `POST /api/llm/courses/{courseId}/calibration-drafts/{draftId}/runs` | Sin body; el servidor materializa el snapshot del borrador. | `202`, corrida y `Location` de consulta. |
| `POST /api/llm/courses/{courseId}/calibration-runs/{runId}/activation-previews` | Sin body. | `201`, `previewId`, `expiresAt`, corridas/asignaciones esperadas y desafíos migrables o bloqueados. |
| `POST /api/llm/courses/{courseId}/calibration-activations` | `calibrationRunId`, `previewId`, `migrateChallengeIds` y `reason` opcional sólo para reactivación histórica. | `201`, activación creada. |
| `POST /api/llm/admin/calibration-activations/{activationId}/verifications` | `reason` obligatorio para origen manual. | `202`, verificación y `Location`. |

El `draftId` identifica de manera suficiente el objetivo (`COURSE` o un único
`CHALLENGE`), recursos seleccionados y baseline esperado; por eso no se aceptan
IDs redundantes ni identidad de curso/desafío en el body. Todas las escrituras
requieren `Idempotency-Key`; el servidor rechaza `draftId` ajeno al actor,
`runId` fuera de curso, preview vencido u obsoleto, baseline cambiado y una
lista de migración que contenga desafíos bloqueados.

**Decisiones que deben congelarse antes de editar OpenAPI:** nombres definitivos
de recursos, scopes por operación, las dos dimensiones del estado, enum de
validez, forma exacta del preview, semántica de `reason`, paginación del futuro
detalle administrativo y consumidores autorizados del resumen de pendientes.

### D-165 — Propuesta: eventos propios, outbox y mensajes a Notificaciones

**Alcance:** punto 7. Esta propuesta no modifica el AsyncAPI actual ni el
contrato de `notification-service`; enumera eventos que requieren acuerdo de
consumidores antes de publicarse.

**Reglas propuestas de publicación:** todo hecho se inserta en una outbox local
en la misma transacción que su cambio de estado. Un relay lo publica a Kafka de
forma at-least-once. Cada mensaje usa el envelope de plataforma:

```text
eventId, version, occurredAt, producer="llm-service", data
```

`traceparent` y `X-Request-Id` viajan exclusivamente en headers Kafka. El
relay no reconstruye ni altera la correlación original. Un consumidor deduplica
por `eventId`; LLM aplica la misma regla al consumir eventos ajenos. No se
asume orden entre topics: cada evento se particiona por el identificador de su
agregado (`courseId`, `challengeId`, `attemptId` o `incidentId` según el caso).

**Eventos de LLM propuestos para acuerdo:**

| Topic propuesto | Hecho | `data` mínima | Consumidor previsto |
|---|---|---|---|
| `calibracion_activada.v1` | Una activación fue creada o reemplazó la previa. | `activationId`, `scope`, `courseId`, `challengeId` opcional, `calibrationRunId`, `occurredAt`. | Servicios que consultan o proyectan disponibilidad; por confirmar. |
| `calibracion_suspendida.v1` | Una activación deja de ser apta. | `activationId`, `scope`, `courseId`, `challengeId` opcional, `reasonCode`, `occurredAt`. | Consumidores de estado y Notificaciones; por confirmar. |
| `score_de_ia_calculado.v1` | Se completó el único score de un intento. | Se conserva la propuesta existente: `evaluationId`, `attemptId`, `challengeId`, `courseId`, score agregado, scores por dimensión, versión de rúbrica/perfil y fecha. | `practice-service`. |
| `score_pendiente_diferido.v1` | Un score pasa a diferido o cambia su causa. | `evaluationId`, `attemptId`, `challengeId`, `courseId`, `reasonCode`, `deferredAt`, `nextRetryAt` opcional. | `practice-service` y quien controla el cierre; por confirmar. |
| `score_diferido_reintentos_agotados.v1` | Un pendiente alcanza el límite de reintentos. | `evaluationId`, `courseId`, `challengeId`, `attemptId`, `attemptCount`, `occurredAt`. | Notificaciones y operación; por confirmar. |
| `evaluador_indisponible.v1` / `evaluador_recuperado.v1` | Se abre o recupera una indisponibilidad total del evaluador. | `incidentId`, `deploymentId`, `reasonCode`, `affectedCourseIds` sólo si se acuerda, `occurredAt`. | Notificaciones; por confirmar. |

Los eventos existentes `calibracion_aprobada.v1` y
`calibracion_fuera_de_tolerancia.v1` se revisarán junto a esta tabla para evitar
duplicar semánticas. Una aprobación de corrida no equivale a una activación y
una suspensión no equivale a una falla técnica de corrida; los topics deben
mantener esa diferencia.

**Propuesta de mensaje para `notification-service`:** LLM publica una solicitud
de notificación, no persiste estados de lectura ni intenta entregar mensajes.
El topic candidato es `notificacion_solicitada.v1`; requiere aprobación del
dueño de Notificaciones. Su `data` no incluye Markdown de skills,
transcripciones, prompts, secretos ni resultados de alumnos y propone:

```json
{
  "notificationRequestId": "uuid",
  "incidentId": "uuid | null",
  "type": "CALIBRATION_SUSPENDED",
  "severity": "HIGH",
  "recipientScope": {
    "kind": "ADMIN_ROLE | COURSE_AUTHORIZED_TEACHERS | USER",
    "courseId": "uuid | null",
    "userId": "uuid | null"
  },
  "resource": {
    "courseId": "uuid | null",
    "challengeId": "uuid | null"
  },
  "reasonCode": "string",
  "occurredAt": "date-time"
}
```

La deduplicación técnica del consumidor usa `eventId`. Para evitar avisos de
negocio repetidos, LLM propone una unicidad de outbox por `(incidentId, type,
recipientScope, phase)`; `phase` distingue `OPENED`, `RETRY_EXHAUSTED` y
`RECOVERED`. Notificaciones conserva la autoridad sobre destinatarios finales,
entrega y leído/no leído. Falta acordar con ese servicio el topic, scopes de
destinatarios, severidades y tratamiento de DLQ.

### D-166 — Propuesta: validación de respuesta estructurada del modelo

**Alcance:** punto 8. Esta propuesta regula la aceptación de una respuesta del
modelo y la evidencia mínima de una corrida; no crea el pipeline de
sanitización de skills ni una estrategia general de observabilidad.

**Propuesta de salida canónica:** el AI Gateway solicita modo de respuesta
estructurada estricto. Un deployment que no soporte un esquema JSON estricto no
es elegible para el evaluador. La respuesta transporta puntajes de unidades de
evaluación, no el puntaje final ponderado:

```json
{
  "schemaVersion": "1.0",
  "profileHash": "sha256-hex",
  "scores": [
    { "unitId": "AUTONOMY", "score": 74 },
    { "unitId": "AUTONOMY/PLANIFICATION", "score": 70 }
  ]
}
```

`unitId` identifica cada dimensión o subcriterio activo definido por el
`EffectiveEvaluationProfile` materializado. El servidor calcula el puntaje de
dimensión y el final exclusivamente a partir de esas puntuaciones y pesos; no
acepta que el modelo imponga un total, pesos, aprobación académica ni reglas de
composición.

**Pipeline de aceptación propuesto:**

1. El AI Gateway verifica transporte, tamaño máximo y que el proveedor haya
   declarado salida JSON estructurada.
2. Un parser estricto valida JSON, `schemaVersion`, propiedades permitidas y
   tipos. No repara JSON, extrae números desde texto libre ni ignora campos
   inesperados.
3. El validador compara `profileHash` con el snapshot de la invocación y exige
   que el conjunto de `unitId` sea exactamente el conjunto de unidades activas:
   sin faltantes, duplicados ni unidades desconocidas.
4. Cada `score` debe ser entero entre 0 y 100. Sólo entonces el servidor
   calcula agregados, desvíos de calibración y decisión PAR-14.
5. El resultado se persiste o se usa para completar una evaluación sólo si todo
   el pipeline aprobó. No existe score parcial, redondeo implícito ni publicación
   de un evento de score ante un fallo de validación.

Los códigos de fallo propuestos son `MODEL_RESPONSE_MALFORMED`,
`MODEL_RESPONSE_SCHEMA_MISMATCH`, `MODEL_RESPONSE_PROFILE_MISMATCH`,
`MODEL_RESPONSE_SCORE_OUT_OF_RANGE`, `MODEL_RESPONSE_SCORE_MISSING`,
`MODEL_RESPONSE_SCORE_DUPLICATED` y `MODEL_RESPONSE_UNIT_UNKNOWN`. Una corrida
de calibración termina con el código correspondiente, sin reintento automático
según D-156. Una evaluación diferida aplica su política de reintentos de D-162,
sin confirmar un resultado parcial.

**Evidencia mínima propuesta por invocación:** se conserva una referencia
inmutable con `invocationId`, `evaluationId` o `calibrationRunId`, `caseId`
cuando aplique, `profileHash`, versión de esquema, deployment, marcas de tiempo,
hash de request, respuesta normalizada, resultado de validación, código de
fallo y metadatos de proveedor disponibles. La respuesta cruda, si se conserva
para diagnóstico, queda en almacenamiento protegido y acotado; no se escribe en
logs ni se expone por contratos docentes. La transcript o prompt completo no se
duplica en esta evidencia: se referencia el snapshot que ya le corresponde.

**Pendientes para confirmar:** versión inicial exacta del esquema, si la
respuesta debe incluir justificación visible al docente, límite de tamaño de
salida y política de retención/acceso de la respuesta cruda. Hasta cerrarlos no
se modifica el contrato del AI Gateway ni se implementa un parser de producción.
