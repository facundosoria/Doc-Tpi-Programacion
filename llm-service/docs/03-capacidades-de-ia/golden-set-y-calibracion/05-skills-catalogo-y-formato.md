# Skills — formato, catálogo, visibilidad, versionado y clonado

> **Estado:** vigente para diseño e implementación (directivas de Producto).
> **Decisiones:** D-01 a D-06, D-39 a D-44, D-120 a D-127. Donde una decisión posterior (D-39 en
> adelante) precisa o amplía una decisión anterior (D-01 a D-06), se indica explícitamente.

## 1. Qué es una skill y en qué formato se adjunta (D-01)

Una skill es contenido en **Markdown (`.md`)** que el docente adjunta al calibrar un curso o un
desafío. Aunque la primera versión sólo admite Markdown, el modelo interno se diseña genérico desde
el inicio: `SkillArtifact` (la familia/identidad de la skill) y `SkillAttachment` (la instancia
adjuntada a una calibración concreta), ambos versionados.

Cada corrida de calibración conserva:

- un **snapshot inmutable** del contenido de cada skill aplicada;
- su **versión**, su **orden** dentro de la selección y su **hash** de contenido.

Este diseño permite incorporar en el futuro catálogos institucionales, paquetes firmados, metadatos
estructurados adicionales o documentos de contexto de otro tipo, sin modificar las calibraciones
históricas ya ejecutadas: una corrida vieja siempre puede reproducirse con el snapshot que usó.

**Guardarraíles de carga (D-01):** se valida tipo de archivo, codificación, tamaño y cantidad antes
de aceptar el adjunto; el contenido adjunto nunca se ejecuta como código; el contenido de la skill no
se registra en logs de aplicación ni de infraestructura (ver también
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md)
para el resto de las reglas de contenido y sanitización).

## 2. Catálogo tipo grilla y selección (D-02)

Además de adjuntar una skill nueva al calibrar, existe un **catálogo** con vista de grilla:

- el docente busca por **nombre** o por **función/descripción**;
- filtra por **curso**;
- ordena por **fecha de subida**;
- selecciona una o más skills existentes para incorporarlas a la calibración en curso.

Cada entrada del catálogo expone, como mínimo: nombre, función/descripción, cursos asociados, autor,
fecha, versión, hash, estado (de seguridad y de ciclo de vida) y el contenido Markdown completo.

Una calibración puede combinar libremente skills recién adjuntadas en el mismo flujo y skills
seleccionadas del catálogo, **preservando el orden elegido por el docente** (ver también D-42 en
[06 — Subcalibración jerárquica](06-subcalibracion-jerarquica.md#orden-manual-de-skills-d-42), que
detalla el efecto del orden sobre el razonamiento del agente).

## 3. Visibilidad pública/privada (D-03)

El docente autor decide, **al publicar** una skill, si su visibilidad es `PUBLIC` o `PRIVATE`.

| Visibilidad | Quién la ve/usa |
|---|---|
| `PRIVATE` | Sólo el autor. |
| `PUBLIC` | Cualquier docente autorizado puede descubrirla y usarla. |

Los filtros de curso (D-02, D-39) ayudan a **descubrir** contenido dentro de un catálogo ordenado,
pero nunca otorgan por sí solos acceso a cursos para los que el docente no está autorizado. La
autorización de curso la gobierna `courses-service` (ver D-150 en
[09 — Flujos de UI](09-flujos-ui-calibracion-y-gestion-skills.md)).

### 3.1 Cambio de visibilidad de pública a privada (D-134)

Cuando una skill pública pasa a privada:

- las **calibraciones existentes de otros docentes** que ya la usaban conservan su versión fijada
  (snapshot inmutable), sin verse afectadas;
- se impiden **nuevas selecciones** de esa skill por parte de terceros a partir de ese momento.

El cambio de visibilidad afecta únicamente el índice de descubrimiento y los permisos de selección
futura; nunca los snapshots inmutables de calibraciones o evaluaciones ya asociadas. El historial
conserva el hecho de que la versión era pública en el momento de su uso. La nueva selección queda
limitada al autor y a Administración.

### 3.2 Favorito externo sin clon tras privatizar (D-135)

Si otro docente tenía como favorita una skill que se vuelve privada y **no creó un clon propio**, su
favorito permanece visible en su banco de skills como una **referencia no disponible**: no puede
adjuntarse a una nueva calibración. No se elimina el favorito ni se redirige automáticamente a otra
skill — evitar cambios silenciosos en las preferencias del docente es la razón de esta regla.

### 3.3 Un favorito no migra automáticamente a un clon (D-136)

Aunque el docente haya clonado la skill original (D-120), su favorito **no migra automáticamente**
al clon: un clon puede haber sido modificado y no equivale a la versión exacta que el docente marcó
como favorita. La interfaz conserva el favorito original (ahora no disponible, ver D-135) y permite
al docente elegir explícitamente si marca también su clon como favorito. Ambas referencias quedan
auditables por separado.

## 4. Versionado, impacto y recalibración (D-04)

Editar una skill **crea una nueva versión**; no se sobrescribe la anterior.

El sistema debe poder informar, para cualquier versión de una skill, **en qué desafíos fue usada**
(ficha de impacto), y permitir que el docente **recalibre** cursos o desafíos concretos para adoptar
la versión nueva.

La ficha de impacto distingue tres situaciones:

1. **uso histórico** — corridas o intentos pasados que usaron esa versión y no cambian;
2. **calibración activa** — la configuración vigente hoy, que sigue usando la versión anterior hasta
   que el docente decida migrar;
3. **calibración pendiente** — una recalibración ya solicitada pero todavía no aprobada/activada.

**No hay recalibración automática.** Editar una skill nunca dispara por sí sola una nueva corrida: se
conserva la configuración activa anterior tal cual estaba, y es el docente quien inicia
explícitamente una nueva corrida si decide migrar. El sistema debe ser resiliente ante estos cambios
—nunca bloquear al docente— pero sí avisarlo con feedback claro sobre el desfasaje de versión.
Fallas de procesamiento o de dependencias durante ese flujo deben ser recuperables y ofrecer
reintento (a nivel de flujo de usuario; la corrida de calibración en sí no reintenta automáticamente,
ver D-156 en [08 — Máquina de estados](08-maquina-de-estados-corridas-y-activaciones.md)).

### 4.1 Metadatos de skill sin nueva versión de contenido (D-154)

Cambiar sólo el **nombre**, la **descripción funcional**, las **etiquetas** o la **visibilidad** de
una skill **no crea una nueva versión** si el contenido Markdown no cambió. Las versiones inmutables
representan instrucciones efectivas y su hash de contenido; los metadatos de catálogo se actualizan y
auditan por separado, sin invalidar calibraciones ni exigir recalibración. Los snapshots de corridas
ya ejecutadas conservan el nombre visible que correspondía a su momento histórico (coherente con
D-84, sobre snapshots de nombre de recursos externos).

## 5. Gestión completa y trazabilidad por desafío (D-05)

Skills tiene una **vista de gestión completa** fuera del flujo de Calibraciones, además de mantener
el selector rápido dentro del propio flujo de calibración (ver D-142 en
[09 — Flujos de UI](09-flujos-ui-calibracion-y-gestion-skills.md) para el detalle del módulo
autónomo). Esa vista incluye como mínimo:

- un **banco de skills** (propias, favoritas, clonadas, descubiertas);
- una **página de desafíos** que lista, para cada uno, las skills efectivamente usadas.

Se requieren vistas para: catálogo, búsqueda, filtros, visibilidad, versiones, impacto y favoritos; y
una vista por desafío que muestre la calibración efectiva y las versiones exactas de skills
aplicadas.

## 6. Favoritos fijados a versión (D-06)

Los favoritos se fijan a la **versión exacta** (`skillVersionId`) que el docente marcó — no a la
familia de la skill. No se actualizan automáticamente ante una versión nueva.

Si existe una versión nueva de una skill favorita:

- se informa al docente;
- se le ofrece una acción explícita para **quitar el favorito anterior y migrarlo** a la nueva
  versión.

Esa migración de favorito es una acción explícita, reversible y **no altera ninguna calibración
existente** (las calibraciones ya construidas siguen usando su snapshot inmutable).

## 7. Asociación a cursos, reutilización y orden (D-39 a D-42)

### 7.1 Asociación obligatoria a cursos (D-39)

Al crear o editar una skill, el docente **debe asociarla a uno o más cursos**. Esto permite un
catálogo ordenado visualmente y evita una grilla global desorganizada: el catálogo abre filtrado por
el curso actual y la búsqueda ocurre primero en ese contexto.

La asociación a curso es una **clasificación de descubrimiento**, no otorga permisos sobre datos de
otro curso. Existen skills potencialmente reutilizables entre asignaturas (por ejemplo, guías
pedagógicas o de seguridad), pero ese uso cruzado debe ser **deliberado** — nunca la vista
predeterminada.

### 7.2 Skills públicas asociables a varios cursos (D-40)

Una skill pública puede asociarse a varios cursos, siempre que **su autor esté autorizado** en cada
uno de esos cursos. El backend valida esa autorización al modificar las asociaciones. La lista de
cursos asociados sirve para clasificar y filtrar; no traslada propiedad ni acceso a recursos
académicos de otros servicios.

### 7.3 Reutilización directa de skills públicas (D-41)

Un docente puede usar **directamente** una versión pública de una skill creada en otro curso, dentro
de una calibración de su propio curso, sin modificar la propiedad ni las asociaciones originales de
la skill. Esa reutilización queda auditada como **uso en el curso destino**. El catálogo debe ofrecer
una búsqueda explícita **fuera** del filtro del curso actual para encontrar estas skills, mostrando
con claridad su autor, curso de origen y versión.

### 7.4 Orden manual de skills (D-42)

El docente puede **reordenar manualmente** las skills seleccionadas antes de iniciar una calibración.
La interfaz debe indicar explícitamente que **el orden afecta el razonamiento del agente** (las
skills se incorporan en el orden elegido como instrucciones adicionales, ver D-15 en
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md#skills-como-instrucciones-efectivas-del-evaluador-d-15)).

La posición de cada skill forma parte del **snapshot inmutable** y del hash del contexto efectivo de
la corrida. La vista previa muestra el orden final antes de confirmar, y la auditoría registra
cualquier reordenamiento realizado antes de iniciar la ejecución.

## 8. Inspección opcional del contenido (D-44)

El docente **no está obligado** a leer el contenido completo de una skill pública antes de
seleccionarla, pero **debe poder** abrir y leer su Markdown completo junto con sus metadatos y su
estado de seguridad. La grilla del catálogo prioriza nombre, función, autor, versión y estado; la
ficha o vista previa ofrece el contenido íntegro bajo demanda. Esto no implica confianza ciega: la
selección sigue sujeta a los controles de sanitización descriptos en
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md).

## 9. Clonado de skills públicas (D-120 a D-123)

### 9.1 Clonado con referencia de origen (D-120)

Un docente puede **clonar** una skill pública de otro docente para adaptarla como skill propia,
conservando la **referencia trazable a su origen**. El clonado crea una skill **nueva e
independiente**: propia versión inicial, propio autor, propia visibilidad, propios favoritos, propio
historial de versiones y propio ciclo de vida. La referencia de procedencia es sólo informativa/
trazable — cambios, archivado o incidentes de seguridad de la skill original **no modifican
automáticamente** la copia (ver D-122 para la excepción de auditoría).

### 9.2 Visibilidad inicial privada (D-121)

Una skill clonada se crea **privada por defecto**. Su nuevo autor decide explícitamente si la
publica. El clonado nunca amplía por sí solo la visibilidad de contenido adaptado. La copia aparece
en el banco personal del nuevo autor y queda sujeta a los mismos controles habituales de
sanitización, autorización y publicación que cualquier skill nueva.

### 9.3 Auditoría de clones ante incidente de seguridad del origen (D-122)

Si una versión origen se deshabilita por seguridad, sus skills clonadas quedan **marcadas para
auditoría**, con trazabilidad explícita hacia el origen — pero **no se deshabilitan
automáticamente**, por ser copias independientes.

El incidente conserva la relación entre la versión de origen y las copias potencialmente afectadas,
sus autores y sus usos. Administración puede revisar cada clon con evidencia suficiente y aplicar una
acción individual si corresponde. Los cambios realizados sobre la copia no se atribuyen erróneamente
al autor de la skill original.

### 9.4 Aviso al autor del clon auditado (D-123)

Cuando una skill clonada queda marcada para auditoría por un incidente de seguridad de su origen, se
**notifica a su autor**. El aviso identifica la copia y su relación con el incidente origen, sin
exponer contenido sensible ni asumir que la copia en sí es insegura. Queda ligado a la auditoría para
informar posteriormente su resolución y permitir al docente decidir sobre sus propias calibraciones
que la usan.

## 10. Metadatos funcionales y etiquetas (D-124 a D-127)

### 10.1 Metadatos funcionales obligatorios (D-124)

Cada skill requiere una **descripción funcional breve** y puede incluir **etiquetas opcionales**. El
catálogo indexa nombre, descripción funcional, etiquetas, curso relacionado, autor, visibilidad y
fechas. La búsqueda no depende de inspeccionar el cuerpo Markdown completo: estos metadatos permiten
buscar por función y escalar futuros filtros sin alterar las versiones inmutables de contenido.

### 10.2 Etiquetas de catálogo y personalizadas (D-125)

Una skill puede usar dos tipos de etiquetas:

- **etiquetas de catálogo central** — vocabulario consistente para filtros transversales;
- **etiquetas personalizadas** — permiten expresar necesidades disciplinares nuevas no cubiertas aún
  por el catálogo central.

Ambos tipos se distinguen explícitamente en los metadatos, para conservar calidad de búsqueda,
trazabilidad y una posible gobernanza futura, sin necesidad de reinterpretar el Markdown de la skill.

### 10.3 Descubrimiento global de etiquetas personalizadas públicas (D-126)

Las etiquetas personalizadas de una skill **pública** aparecen en la búsqueda global del catálogo. El
índice global sólo incorpora etiquetas de contenido cuya visibilidad permite descubrirlo; las
etiquetas de skills **privadas** permanecen restringidas a su autor y a los usuarios autorizados,
para no revelar temas, funciones o relaciones de contenido no público.

### 10.4 Promoción administrativa de etiquetas (D-127)

Un administrador puede **promover** una etiqueta personalizada frecuente al catálogo central, **sin
modificar retrospectivamente** las skills que ya la usaban. La promoción crea una nueva entrada
gobernada y auditada del catálogo central. Las asociaciones históricas de etiquetas conservan su
origen y su texto original; el sistema puede sugerir la etiqueta central para usos futuros, sin
reversionar, editar ni cambiar la visibilidad de skills existentes.

## Trazabilidad de esta sección

| Decisión | Tema |
|---|---|
| D-01 | Formato Markdown y modelo genérico versionado de skills. |
| D-02 | Catálogo tipo grilla, búsqueda, filtro y orden. |
| D-03 | Visibilidad `PUBLIC`/`PRIVATE` decidida al publicar. |
| D-04 | Versionado por edición, ficha de impacto, sin recalibración automática. |
| D-05 | Vista de gestión completa y trazabilidad por desafío. |
| D-06 | Favoritos fijados a versión, migración explícita. |
| D-39 | Asociación obligatoria a uno o más cursos. |
| D-40 | Skills públicas asociables a varios cursos autorizados. |
| D-41 | Reutilización directa de skills públicas de otro curso. |
| D-42 | Orden manual de skills, parte del snapshot. |
| D-44 | Inspección opcional del contenido completo. |
| D-120 | Clonado con referencia de origen trazable. |
| D-121 | Visibilidad inicial privada del clon. |
| D-122 | Auditoría de clones ante incidente de seguridad del origen. |
| D-123 | Aviso al autor del clon auditado. |
| D-124 | Metadatos funcionales obligatorios (descripción, etiquetas). |
| D-125 | Etiquetas de catálogo central y personalizadas. |
| D-126 | Descubrimiento global de etiquetas personalizadas públicas. |
| D-127 | Promoción administrativa de etiquetas. |
| D-134 | Cambio de visibilidad pública a privada. |
| D-135 | Favorito externo sin clon tras privatizar. |
| D-136 | Un favorito no migra automáticamente a un clon. |
| D-154 | Metadatos sin nueva versión de contenido. |
