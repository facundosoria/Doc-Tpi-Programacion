# User Stories Applied — notas de Mike Cohn (2004)

> Resumen propio, no transcripción. Fuente: *User Stories Applied: For Agile
> Software Development*, Mike Cohn, Addison-Wesley, 2004 (Addison-Wesley
> Signature Series). Ver [`README.md`](README.md) para cómo se relaciona con
> las plantillas del repo. Muchas ideas de
> [`historias-de-usuario-scrum-manager.md`](historias-de-usuario-scrum-manager.md)
> (INVEST, SPIDR) se originan acá; este archivo cubre lo que Scrum Manager no
> desarrolla en tanto detalle.

## 1. Las 3 C y qué es (y no es) una historia

Una HU son tres cosas, no una tarjeta: **Card** (descripción escrita, para
planificar y recordar), **Conversation** (donde se resuelven los detalles) y
**Confirmation** (los tests que dicen cuándo está terminada). La tarjeta
"representa" el requisito, no lo "documenta" (Rachel Davies).

Una buena HU describe funcionalidad que el usuario o comprador *valora* — no
"el software estará escrito en Java" (salvo que el usuario sea un programador
consumiendo una API). Cuando una historia se vuelve demasiado grande para
codificarla y probarla en poco tiempo (Cohn sugiere medio día a dos semanas
por par de programadores como referencia, no una regla dura) se la llama
**épica**.

No hace falta bajar una historia a cada detalle posible: los detalles que
faltan se resuelven en la conversación, no acumulando sub-historias hasta
cubrir "cada último caso" (ese nivel de descomposición ya es estilo
requisito formal, no historia).

## 2. INVEST en detalle

- **Independent:** las dependencias entre historias complican estimar y
  priorizar (si A depende de B y B es baja prioridad, ¿qué hago?). Dos
  salidas: combinar las historias dependientes en una sola, o buscar otro eje
  de corte. Si no se puede, poner dos estimaciones en la tarjeta (con/sin la
  otra historia ya hecha).
- **Negotiable:** la tarjeta no es un contrato. El desafío es incluir "la
  cantidad justa" de detalle: frases/notas que recuerden qué conversar,
  *no* la especificación completa. El exceso de detalle en la tarjeta genera
  la falsa sensación de que "ya está todo dicho" y de que no hace falta
  seguir conversando con el cliente.
- **Valuable:** debe ser valiosa para el usuario o el comprador — nunca solo
  para el desarrollador. "Todas las conexiones a la BD van por un connection
  pool" no es una buena historia; reformulada en términos de valor sería algo
  como "hasta 50 usuarios pueden usar la app con una licencia de 5 usuarios
  de BD". La mejor garantía de que una historia es valiosa es que la escriba
  el cliente.
- **Estimable:** si el equipo no puede estimar una historia es por: falta de
  conocimiento de dominio (→ hablar con el cliente), falta de conocimiento
  técnico (→ un *spike*, actividad time-boxed para investigar), o porque es
  demasiado grande (→ desagregarla).
- **Small:** ni tan grande que no se pueda estimar/planificar, ni tan chica
  que no valga la pena escribirla aparte (bugs chicos y cambios triviales de
  UI se agrupan en una historia combinada en vez de listarse uno por uno).
- **Testable:** si el cliente no sabe cómo probarla, la funcionalidad no está
  clara o no es valiosa; si el equipo no puede probarla, no puede saber
  cuándo terminó.

## 3. Historia compuesta vs. historia compleja

Las épicas caen en dos categorías con soluciones distintas:

- **Compound story** (compuesta): en realidad son varias historias más chicas
  metidas en una (ej. "un usuario puede publicar su currículum" esconde
  crear/editar/borrar/activar/tener-múltiples-currículums). Se desagrega por
  create/edit/delete, o por los componentes de datos involucrados — cuidado
  con pasarse de fino y terminar con historias demasiado chicas para valer la
  pena (ej. "editar la fecha de un ítem del currículum" por separado).
- **Complex story** (compleja): es grande por incertidumbre, no porque
  esconda varias historias — no se puede desagregar en partes funcionales.
  Se separa en una historia de investigación (**spike**, siempre
  time-boxed) + la historia real, e idealmente el spike va en una iteración
  anterior a la historia que depende de su resultado (si van en la misma
  iteración, toda la iteración hereda la incertidumbre del spike).

## 4. User Role Modeling (cap. 3)

Antes de escribir historias, identificar los roles de usuario evita que todo
se escriba desde la perspectiva de un único "usuario genérico" (y se pierdan
historias de roles minoritarios pero importantes). Pasos:

1. **Brainstorm** de roles en tarjetas, sin discutir ni evaluar mientras se
   generan (sesión corta, ~15 min).
2. **Organizar**: se acomodan las tarjetas superpuestas según cuánto se
   solapan los roles entre sí.
3. **Consolidar**: los roles que se superponen totalmente se funden en uno
   (o se elimina el redundante); los que no aportan al éxito del sistema se
   descartan.
4. **Refinar**: se agregan atributos a cada rol — frecuencia de uso, nivel de
   experiencia en el dominio, proficiencia general con software, objetivo
   general (conveniencia vs. experiencia rica, etc.).

Un rol representa **una persona física**, nunca una organización completa
("una empresa puede publicar un aviso" está mal; el rol es la persona que lo
hace). Evitar roles no-humanos (otro sistema) salvo excepción justificada —
el objetivo es forzar empatía con quien puede hacer fracasar el proyecto si
no se lo satisface.

## 5. Guías de redacción (cap. 7)

- **Start with Goal Stories:** para no perderse en un proyecto grande, partir
  de los objetivos de alto nivel de cada rol (ej. el objetivo de un
  buscador de empleo es simplemente "conseguir trabajo"; de ahí salen las
  historias).
- **Slice the Cake:** al dividir una historia grande, cortar **verticalmente**
  (cada historia resultante atraviesa todas las capas y es usable por sí
  sola), nunca horizontalmente por capa técnica (ej. "llenar el formulario"
  separado de "guardar en la base" — ninguna de las dos sirve sola).
- **Write Closed Stories:** una historia "cerrada" termina con el logro de
  un objetivo con sentido, dejando al usuario con sensación de haber
  completado algo. "Un reclutador puede administrar sus avisos" no es
  cerrada (es una actividad continua); "puede cambiar la fecha de
  vencimiento de un aviso" sí lo es.
- **Put Constraints on Cards:** los requisitos que deben *obedecerse* pero no
  se implementan como una funcionalidad puntual (NFR, restricciones legales
  o técnicas) se anotan como tarjetas marcadas explícitamente
  "**Constraint**" — no se estiman ni planifican como una historia normal,
  pero conviene escribirles un test automatizado que corra en cada
  iteración para asegurar que no se violan.
- **Size the Story to the Horizon:** las historias de las próximas
  iteraciones deben tener el tamaño justo para planificarlas; las lejanas
  pueden quedar grandes e imprecisas — no vale la pena refinarlas antes de
  tiempo.
- **Keep the UI Out as Long as Possible:** evitar que la historia implique
  una solución de interfaz específica antes de que haga falta — mezclar
  requisito con diseño de solución limita opciones de UI innecesariamente.
- Otras: incluir el rol en la redacción ("Un Job Seeker puede…" en vez de
  "Un usuario puede…"), escribir para **un solo usuario** (evita ambigüedad
  de alcance), voz activa, que el cliente sea quien escribe la historia, no
  numerar las tarjetas (usar un título corto en vez de un ID como referencia
  conversacional).

## 6. Desagregar una historia en tareas (cap. 10)

La HU ya es chica (1-5 días ideales); igual conviene bajarla a tareas más
finas porque: (a) rara vez la hace un solo desarrollador de punta a punta,
(b) el ejercicio grupal de listar tareas ayuda a no olvidar pasos (ej.
actualizar el manual de usuario), (c) es el "diseño mínimo" que reemplaza
la fase de diseño previo de un proceso en cascada.

**No hay un tamaño obligatorio de tarea.** Guías para decidir cuándo separar
una tarea:

- Si una parte de la historia es particularmente difícil de estimar
  (depende de un tercero lento en responder, por ejemplo), separarla del
  resto.
- Si dos tareas las puede hacer gente distinta en paralelo, separarlas (esto
  es lo que permite que más de un desarrollador trabaje la misma historia a
  la vez, típicamente hacia el final de la iteración cuando escasea tiempo).
- Si sirve saber que una parte específica ya está terminada (para que otra
  tarea que depende de ella pueda arrancar antes), separarla como tarea
  propia.

Una vez listadas, cada desarrollador **acepta la responsabilidad** de una
tarea (no dos personas en la misma tarea salvo pair programming) y la estima
en tiempo ideal — recién ahí se sabe si el compromiso de la iteración es
realista o si hay que devolver trabajo al equipo o a negociar con el cliente
qué historia sale de la iteración.

## 7. En qué se diferencia una HU de… (cap. 12)

| | Requisito IEEE 830 | Caso de uso (UML) | Escenario de interacción |
|---|---|---|---|
| Formato típico | "El sistema deberá…" | Actor, pre/postcondición, flujo principal + extensiones | Narrativa detallada de una interacción concreta |
| Alcance vs. HU | Mucho más detallado y permanente | Mayor alcance (≈ varias HU + sus tests) | Mayor alcance aún (≈ varios casos de uso) |
| Foco | Lista de atributos del sistema | El *cómo* de la interacción | El contexto y el "por qué" de una interacción real |
| Vida útil | Documento formal persistente | Artefacto persistente | Insumo de diseño, no persistente |
| Cuándo preferirlo sobre HU | Certificación/auditoría obligatoria (IEEE 830/29148) | Sistemas regulados, legacy, contratos de alcance cerrado | Diseño de UX antes de escribir historias |

Idea central: una lista de requisitos ("the system shall…") empuja a
imaginar una solución prematuramente y esconde el costo de cada ítem hasta
tener todo escrito; una historia expone el costo (vía estimación) desde el
principio y prioriza objetivos de usuario en vez de atributos de un sistema
ya imaginado.

## 8. Catálogo de "story smells" (cap. 14)

Sospechar de estos síntomas en el backlog:

- **Historias demasiado chicas:** hay que revisar la estimación
  constantemente porque se solapan entre sí (ej. "exportar a XML" y
  "exportar a HTML" comparten trabajo) → combinarlas hasta que sea necesario
  partirlas para una iteración concreta.
- **Historias interdependientes:** cuesta planificar porque meter una obliga
  a meter otra → o están mal separadas (revisar "slice the cake") o son
  demasiado chicas (combinarlas).
- **Goldplating:** el desarrollador agrega funcionalidad no pedida — se
  combate con visibilidad diaria del trabajo (daily) y demo de fin de
  iteración donde se nota lo no pedido.
- **Demasiado detalle en la tarjeta:** delata que se está valorando más la
  documentación que la conversación — "si no entra en la tarjeta, achicá la
  tarjeta" (Tom Poppendieck).
- **Detalle de interfaz demasiado pronto:** asumir una pantalla que todavía
  no existe limita el diseño sin necesidad.
- **Pensar demasiado adelante:** síntoma de venir de procesos con
  "ingeniería de requisitos" pesada — pedir estimaciones en horas en vez de
  días, o un template de historia que capture "todo", son señales de esto.
- **Dividir demasiadas historias durante la planificación:** no es grave
  ocasionalmente, pero si pasa todo el tiempo hay que revisar el backlog
  antes de la reunión, no durante.
- **El cliente no puede priorizar:** o las historias son muy pocas y muy
  grandes ("dame un poco de cada una"), o no transmiten valor de negocio
  claro (reescribirlas en términos que el cliente entienda, no en
  términos técnicos).
- **El cliente no quiere escribir ni priorizar:** típico de culturas donde
  nadie quiere quedar "responsable" de una decisión — mitigarlo dándole al
  cliente un canal no amenazante para opinar (a veces una conversación
  privada) mientras alguien más asume la responsabilidad final de la
  decisión.

## 9. NFR, papel vs. software y bugs (cap. 16)

- **NFR como constraints:** rendimiento, precisión, portabilidad,
  reusabilidad, mantenibilidad, interoperabilidad, disponibilidad,
  usabilidad, seguridad, capacidad — se escriben como tarjeta "Constraint"
  (ver §5) con, cuando sea posible, un test automatizado que la verifique
  periódicamente (ej. "80 % de las búsquedas devuelven resultado en <2s").
- **Retener las historias** una vez implementadas (no romper la tarjeta):
  sirve para auditorías, para reescrituras futuras del producto, o para
  entregar como "documentación de requisitos" si un cliente/comprador lo
  pide formalmente.
- **Bugs como historias:** un bug que tomaría tanto tiempo como una historia
  normal, se trata como una historia más. Los bugs chicos se agrupan en una
  sola historia "paraguas" para no ensuciar el backlog con ítems triviales
  uno por uno.

## 10. Scrum + historias (cap. 15, resumen)

Scrum es iterativo (refinamiento sucesivo) e incremental (cada incremento es
funcionalidad completa y entregable) a la vez. Historias entran al *product
backlog* priorizado; el subconjunto que el equipo se compromete a construir
en el sprint pasa al *sprint backlog*. La velocidad (story points completados
por sprint) es el dato que conecta estimación de historias con planificación
de release — no debe incluir historias parcialmente terminadas (evita la
trampa de "todo al 90 %, nada al 100 %").
