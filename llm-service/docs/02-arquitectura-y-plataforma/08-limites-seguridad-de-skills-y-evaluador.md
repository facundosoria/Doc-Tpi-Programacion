# 08 — Límites, seguridad de skills y modelo evaluador

> **Decisiones:** D-27 a D-38 y D-88 a D-91. D-28 conserva la brecha de cuota como referencia
> futura del punto 2; D-31 a D-38 conservan reglas de dominio, sin adelantar el
> pipeline de sanitización del punto 4, ambos fuera del sprint actual.

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

