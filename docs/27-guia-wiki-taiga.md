# 27 — Guía obligatoria de documentación en la Wiki de Taiga

> Regla de la cátedra para publicar el desarrollo del proyecto en la Wiki de Taiga.
> Aplica a toda página que el equipo cree o actualice en esa Wiki. No sustituye a
> `docs/`: la Wiki es la vista de entrega trazable; `docs/` sigue siendo la fuente de
> diseño. Última incorporación: 2026-09-05 (fuente: home de la Wiki, guías de
> documentación por grupo, de producto y de diagramas, y templates oficiales, últ.
> edición de Exequiel Santoro, 26 Ago 2026).
>
> Complementos: [28 · Normativa de la cátedra](28-normativa-catedra-plataforma.md) ·
> [`plantillas/pagina-wiki-grupo.md`](plantillas/pagina-wiki-grupo.md).

## 1. Estructura y ubicación

- La documentación del proyecto se registra dentro de la Wiki de Taiga.
- **Una página por tema o funcionalidad** desarrollada.
- Cada página conecta con las relacionadas mediante enlaces internos de la Wiki.

## 2. Regla de nombrado

Formato obligatorio del título de la página:

```
GXX - TEMA
```

| Parte | Significado |
|---|---|
| `GXX` | Número de grupo asignado por la cátedra (`G01`, `G02`, …). Reemplazar `XX` por el número real del equipo. |
| `TEMA` | Título o funcionalidad principal que documenta la página. |

Ejemplos: `G03 - Gestión de Clientes`, `G07 - Módulo de Pedidos`.

## 3. Contenido obligatorio de cada página

Cada página incluye estos apartados, en este orden:

| # | Apartado | Contenido |
|---|---|---|
| a | **Título del tema** | Idéntico al nombre de la página (`GXX - TEMA`). |
| b | **Descripción del tema u objeto** | Explicación breve del propósito y el alcance de lo documentado. |
| c | **Referencia a Historia(s) de Usuario** | La o las HU del backlog vinculadas, para mantener la trazabilidad requerimiento ↔ implementación. Verificar que cada HU exista y esté documentada en el backlog antes de referenciarla. |
| d | **Diagramas requeridos** | En esta secuencia: **DER → BPMN → Flujograma (opcional) → Clases → Estados → Secuencias → Microservicios**. El checklist de cada uno está en la §4. |
| e | **Enlace(s) al diagrama en Draw.io** | Enlace directo a cada diagrama en Draw.io, **con permiso editable**, en la carpeta compartida del grupo. |
| f | **Explicación de los diagramas** | Por diagrama: qué muestra, qué decisiones o relaciones se destacan y cómo se vincula con el resto del sistema. |
| g | **Observaciones y notas técnicas** | *(Opcional pero recomendable.)* Decisiones técnicas, dependencias y particularidades del módulo. |
| h | **Documentación de endpoints** | Por cada endpoint: Controller, acción (nombre del método), método HTTP, URL, descripción, *path variables* si las hay, y ejemplos de *request* y *response* en JSON. |

La plantilla lista para copiar está en
[`plantillas/pagina-wiki-grupo.md`](plantillas/pagina-wiki-grupo.md).

### Formato sugerido de la referencia a HU

```
HU-03: Como empleado, quiero registrar un nuevo cliente para poder asociarle pedidos.
HU-07: Como usuario, quiero modificar los datos de un cliente existente para mantener la información actualizada.
```

Cada HU referenciada se **redacta en el backlog con el template oficial**
([`plantillas/historia-de-usuario-taiga.md`](plantillas/historia-de-usuario-taiga.md)):
descripción Como/Quiero/Para, notas y observaciones, criterios de aceptación, mínimo
tres escenarios BDD, prototipo, estimación/prioridad y dependencias. La página de la
Wiki solo la enlaza por su permalink; no reescribe su contenido.

## 4. Checklists de diagramas

Cada diagrama se acompaña de una breve descripción y de un **enlace a Draw.io con
permiso editable**. Se mantiene la coherencia entre niveles (datos, lógica, procesos y
arquitectura) para asegurar la trazabilidad del diseño.

### Secuencia de entrega

```
1. DER
2. BPMN
3. Flujograma (opcional, si aplica)
4. Clases
5. Estados
6. Secuencias
7. Microservicios
```

El **BPMN** aporta el contexto de negocio que orienta a los demás diagramas. El
**flujograma** puede complementar, pero no reemplaza a los modelos principales.

### 4.1 Diagrama DER (Entidad–Relación)

**Propósito:** representar la estructura lógica de la base de datos.

- [ ] Definir todas las entidades / tablas.
- [ ] Especificar atributos con sus tipos de datos.
- [ ] Indicar relaciones (1:1, 1:N, N:M).
- [ ] Definir claves primarias y foráneas.
- [ ] Incluir restricciones, índices y normalización básica.
- [ ] Adjuntar enlace a Draw.io (editable).

### 4.2 Diagrama BPMN (proceso de negocio)

**Propósito:** modelar el flujo de negocio extremo a extremo: actores, eventos,
decisiones y orquestación entre sistemas o áreas.

**Cuándo usar BPMN:**
- El flujo involucra más de un rol, área o sistema.
- Hay eventos temporales (deadlines, expiraciones) o de mensaje (integraciones).
- Se busca estandarizar o automatizar el proceso, o dejar trazabilidad del *happy path*
  y sus alternativas.

**Checklist:**
- [ ] Definir el *pool* del proceso y los *lanes* por rol o área.
- [ ] Incluir eventos de inicio, intermedios y de finalización.
- [ ] Modelar tareas y subprocesos con nombres verbales y resultados claros.
- [ ] Usar *gateways* adecuados (XOR, OR, AND).
- [ ] Representar *message flows* entre sistemas (no mezclar con *sequence flows*).
- [ ] Incorporar *boundary events* (errores, timeouts, reintentos).
- [ ] Añadir *data objects* relevantes y anotaciones para reglas clave.
- [ ] Validar conformidad BPMN 2.0 y numerar versiones.
- [ ] Adjuntar enlace a Draw.io (editable).

### 4.3 Flujograma (flowchart) — opcional

**Propósito:** aclarar la lógica interna de un método, servicio o algoritmo cuando el
texto o el pseudocódigo no alcanzan. **No reemplaza al BPMN:** solo para procesos
locales o internos de una función.

**Checklist:**
- [ ] Incluir Start / End y mantener el flujo unidireccional.
- [ ] Decisiones binarizadas (sí/no) y bucles identificados.
- [ ] Señalar manejo de errores y condiciones límite.
- [ ] No exceder la complejidad visual (si crece, dividir o pasar a BPMN).
- [ ] Mantener sincronía con el código o pseudocódigo correspondiente.
- [ ] Adjuntar enlace a Draw.io (editable).

**Decisión rápida BPMN vs. flujograma:**

| Situación | BPMN | Flujograma |
|---|:---:|:---:|
| Múltiples roles / áreas / sistemas | ✅ | ❌ |
| Eventos (tiempo, mensaje, error) | ✅ | ❌ |
| Vista de negocio extremo a extremo | ✅ | ❌ |
| Explicar un algoritmo interno | ❌ | ✅ |
| Documentar una función / clase | ❌ | ✅ |
| Preparar automatización / workflow | ✅ | ❌ |

Regla práctica: si el proceso cruza límites organizacionales o tiene eventos → BPMN.
Si es lógica interna o un algoritmo puntual → flujograma.

### 4.4 Diagrama de clases

**Propósito:** mostrar la estructura estática del sistema a nivel de objetos y sus relaciones.

- [ ] Definir las clases principales.
- [ ] Especificar atributos y métodos clave.
- [ ] Representar relaciones (herencia, asociación, composición).
- [ ] Incluir interfaces y clases abstractas.
- [ ] Indicar visibilidad (public, private, protected).
- [ ] Adjuntar enlace a Draw.io (editable).

### 4.5 Diagrama de estados (máquina de estados)

**Propósito:** describir los estados de un objeto o entidad y las transiciones entre ellos.

- [ ] Identificar las entidades u objetos con comportamiento dependiente de estado.
- [ ] Enumerar todos los estados posibles.
- [ ] Definir los eventos o condiciones que generan las transiciones.
- [ ] Incluir estado inicial y final.
- [ ] Representar acciones internas o de entrada/salida en cada transición.
- [ ] Adjuntar enlace a Draw.io (editable).

### 4.6 Diagrama de secuencias

**Propósito:** detallar la interacción temporal entre objetos, servicios o componentes
durante la ejecución de un proceso.

- [ ] Identificar los casos de uso principales.
- [ ] Representar la comunicación entre microservicios u objetos.
- [ ] Modelar los flujos complejos de negocio.
- [ ] Incluir manejo de errores y excepciones.
- [ ] Incorporar integraciones externas cuando corresponda.
- [ ] Adjuntar enlace a Draw.io (editable).

### 4.7 Diagrama de microservicios

**Propósito:** describir la arquitectura general y la comunicación entre los
microservicios del sistema.

- [ ] Identificar todos los microservicios.
- [ ] Especificar las APIs y endpoints principales.
- [ ] Representar la comunicación síncrona y/o asíncrona.
- [ ] Asociar las bases de datos por microservicio.
- [ ] Incluir gateways, service discovery y mecanismos de balanceo.
- [ ] Adjuntar enlace a Draw.io (editable).

### 4.8 Material de Tema 07 que ya cubre parte de estos diagramas

No se parte de cero: varios diagramas ya existen en `docs/` como Mermaid o como
artefactos, y se enlazan desde la página de la Wiki en lugar de rehacerse.

| Diagrama | Insumo en este repo |
|---|---|
| DER / esquema de datos | [11 Parte B](11-glosario-y-metadata.md), [12 §12](12-almacenamiento-e-ingesta.md) |
| BPMN / flujo de negocio | [04 · Funciones de IA](04-funciones-de-ia.md), [17 · Mapa de integración](17-mapa-de-integracion.md) |
| Clases / patrones | [11 "Patrones de diseño"](11-glosario-y-metadata.md), [04 §2.3](04-funciones-de-ia.md) |
| Estados | ciclo de vida de `evaluacion` / `curso-cohorte` en [04](04-funciones-de-ia.md) y [08](08-decisiones-y-pendientes.md) |
| Secuencias | Mermaid de [17 · Mapa de integración](17-mapa-de-integracion.md), [18 · Contratos inter-equipos](18-contratos-inter-equipos.md) |
| Microservicios | [02 · Arquitectura y stack](02-arquitectura-y-stack.md), [17](17-mapa-de-integracion.md), [gateway y discovery](gateway-y-discovery/README.md) |

> El DER se dibuja sobre el esquema real que se migre con Flyway. Recordar que las
> divergencias con la normativa de la cátedra (motor, idioma de tablas, `is_active`)
> están en [28 §6](28-normativa-catedra-plataforma.md).

## 5. Buenas prácticas

- Redacción clara, técnica y uniforme entre páginas.
- No duplicar información: si un contenido ya vive en otra página, enlazarlo.
- Usar enlaces internos de la Wiki para conectar temas relacionados.
- Actualizar la página cada vez que haya un cambio significativo en el sistema.
- Diagramas legibles, actualizados y consistentes entre sí.
- Antes de referenciar una HU, confirmar que existe y está bien documentada en el backlog.

## 6. Objetivo

Construir una documentación colaborativa, trazable y progresiva del proyecto, donde
cada grupo aporte de forma ordenada su parte del sistema y se facilite la integración
final y la revisión global.

## 7. La Wiki del proyecto: guías y templates oficiales

La Wiki de Taiga «Plataforma de Aprendizaje Gamificado de Programación» es el repositorio
documental único del proyecto: consolida requerimientos, diseño, arquitectura, decisiones,
implementación y evolución. Cada equipo crea y mantiene las páginas de los componentes,
servicios o funcionalidades que tiene asignados, y documenta cómo se integran con el resto.

**Documentos guía** (reglas de redacción y formato):

- Guía para la Documentación del Proyecto por Grupo → base de esta página (doc 27).
- Guía para la Documentación del Producto.
- Guía para la Documentación de Diagramas → §4 de esta página.

**Templates oficiales** (uso obligatorio para uniformidad):

- Template de Historia de Usuario (HU) → [`plantillas/historia-de-usuario-taiga.md`](plantillas/historia-de-usuario-taiga.md). Cómo se completa cada apartado: [29 · Guía de cátedra: Historias de Usuario](29-guia-catedra-historias-de-usuario.md).
- Template de Épicas → [`plantillas/epica-taiga.md`](plantillas/epica-taiga.md).
- Template de Tarea → [`plantillas/tarea-taiga.md`](plantillas/tarea-taiga.md). Método SMART para el desglose: [29 · §4](29-guia-catedra-historias-de-usuario.md) y [23 · §9.2](23-plan-construccion-producto-llm.md).
- Template de Proyecto por Grupo → [`plantillas/pagina-wiki-grupo.md`](plantillas/pagina-wiki-grupo.md).

**Cadena de coherencia** que se revisa de forma permanente:

```
Requerimientos → HU → Diseño → Diagramas → Arquitectura → Implementación
```

Además: nombrar páginas como `GXX - Nombre del componente, servicio o tema`, registrar
las decisiones técnicas o funcionales que puedan afectar a otros equipos, y documentar
explícitamente dependencias e integraciones entre componentes.

## 8. Relación con `docs/` de este repositorio

| En la Wiki de Taiga | En `docs/` |
|---|---|
| Página `GXX - TEMA` por funcionalidad, con HU y diagramas en la secuencia fija. | Diseño, convenciones y decisiones (`00`, `01`, `13`, `17`, `18`, ADRs). |
| Trazabilidad requerimiento ↔ implementación a nivel de HU. | [21 · Matriz de trazabilidad LLM](21-matriz-trazabilidad-llm.md) a nivel de requisito y contrato. |
| Enlace a Draw.io por diagrama. | Diagramas Mermaid embebidos y artefactos enlazados. |

Cuando un diagrama o una decisión ya estén en `docs/`, la página de la Wiki los enlaza
en lugar de reescribirlos.
