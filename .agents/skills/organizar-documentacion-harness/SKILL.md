---
name: organizar-documentacion-harness
description: >-
  Aplica harness engineering (contrato de tarea, mapa de contexto, herramientas con
  contrato, estado durable, evidencia, verificación adversarial, política de riesgo,
  recuperación por clase de fallo, trazas y recibo de cambios) a tareas de organizar,
  reestructurar, auditar, consolidar o limpiar la documentación de este repositorio.
  Úsala cuando pidan "organizar la documentación", "poner orden en los docs",
  "reestructurar/consolidar/limpiar la documentación", "auditar la documentación",
  "detectar documentos duplicados o desactualizados", "actualizar el índice de docs",
  "chequear enlaces rotos en la documentación", o cualquier tarea que mueva, fusione o
  reescriba varios documentos a la vez. No la uses para escribir un documento nuevo desde
  cero sobre un tema puntual (para eso ya existen skills dedicadas como
  generar-vision-y-alcance, generar-epicas, etc.) ni para ediciones triviales de una
  línea en un solo archivo.
---

# Organizar documentación con harness engineering

Esta skill trata cada tarea de organización documental como una tarea de agente que
necesita un harness, no solo una buena intuición. Está basada en
`docs/17-guia-organizacion-documentacion-harness-engineering.md`, que a su vez adapta el
artículo de Lunar "Harness Engineering: The Complete Guide to Building AI Agents That
Don't Fall Apart". Ese documento tiene una **regla de fidelidad explícita**: no debe
tratarse como un resumen sustitutivo del artículo, y esta skill respeta esa regla
repartiendo el contenido completo en `references/` en vez de recortarlo.

**Por qué importa acá:** organizar documentación es exactamente el tipo de tarea donde un
agente sin harness falla en silencio — mueve archivos "que parecen redundantes" y rompe
enlaces, declara "listo" sin haber revisado el índice, o reescribe una decisión vigente
sin darse cuenta de que era normativa. El framework de abajo existe para que esos fallos
se vuelvan visibles antes de que el agente diga que terminó.

## Cómo usar esta skill

El cuerpo de este archivo es la capa que se aplica siempre: el procedimiento (Fases A–G)
y el checklist de aceptación. Cuando necesites entender el **porqué** de un paso, o vayas
a justificar una decisión no obvia frente al usuario, cargá la referencia correspondiente
— no hace falta leerlas todas para ejecutar una tarea chica.

| Si necesitás... | Leé |
|---|---|
| Justificar por qué la tarea necesita un harness y no solo intuición | `references/fundamentos-harness.md` |
| Redactar el contrato de tarea o decidir qué mapa de contexto cargar primero | `references/contrato-y-contexto.md` |
| Definir con qué herramientas y qué contratos vas a leer/editar/mover archivos, o cómo separar razonamiento/ejecución/historial | `references/herramientas-y-arquitectura.md` |
| Decidir qué evidencia exigir antes de declarar algo "hecho", o armar una verificación adversarial | `references/evidencia-y-verificacion.md` |
| Decidir qué acciones requieren aprobación, o clasificar un fallo antes de reintentar | `references/politica-y-recuperacion.md` |
| Definir qué trazas dejar y cómo armar el recibo de cambios final | `references/infraestructura-y-observabilidad.md` |
| Decidir si conviene simplificar el harness, medir el proceso, o evaluar si esta tarea amerita todo esto | `references/mantenimiento-del-harness.md` |
| Ver el harness mínimo por capas o la especificación completa de 8 bloques | `references/spec-reutilizable.md` |
| Verificar que ninguna parte de la fuente se perdió al dividir esta skill | `references/cobertura-de-la-fuente.md` |

## Antes de empezar: ¿esto necesita el procedimiento completo?

No todo pedido de "ordená esto" justifica las siete fases. Ver
`references/mantenimiento-del-harness.md` (sección 19) para el criterio completo, pero en
resumen: si la tarea toca un solo documento, no tiene enlaces entrantes de otros lados, y
un error es fácil de detectar y corregir a mano, alcanza con leer, editar y confirmar el
resultado — no hace falta abrir un contrato formal. Si la tarea toca varios documentos,
mueve o fusiona archivos, o vos mismo/a no podrías verificar de un vistazo que nada se
rompió, seguí el procedimiento completo.

## El procedimiento, en fases

### Fase A — Preparación

1. Leer el contrato recibido (o construirlo si el pedido no lo trae explícito — ver
   `references/contrato-y-contexto.md` para el formato mínimo: objective, scope,
   constraints, acceptance, approval_required).
2. Confirmar objetivo, alcance, restricciones, evidencia y aprobaciones con quien pidió
   la tarea si algo no está claro.
3. Cargar el mapa mínimo del repositorio (fuentes de verdad, índices, ADRs) antes de leer
   documentos individuales.
4. Identificar instrucciones locales (AGENTS.md, READMEs de carpeta), fuentes de verdad y
   documentos protegidos.
5. Crear el estado inicial con `facts`, `decisions`, `progress` y `lessons` (formato en
   `references/herramientas-y-arquitectura.md`).
6. Crear un checkpoint (commit, snapshot o nota de estado) antes de modificar nada.

### Fase B — Descubrimiento progresivo

1. Inventariar archivos y carpetas dentro del alcance.
2. Localizar índices, ADRs, contratos, glosarios y referencias relevantes.
3. Recuperar solo el contexto necesario para cada zona — evitar la inundación de
   contexto (ver `references/contrato-y-contexto.md`).
4. Registrar qué fuentes están vigentes, obsoletas o en conflicto.
5. Detectar dependencias: enlaces entrantes, enlaces salientes y documentos que citan
   cada archivo que vas a tocar.
6. Detectar quién apunta a la carpeta o documento **desde afuera del alcance**: puntos de
   entrada como el README raíz del componente, AGENTS.md, CI/CD, otros repos, u otras
   carpetas de docs que la mencionen como fuente canónica. Esta comprobación es distinta
   de "enlaces internos" del paso anterior — un árbol de documentación puede ser internamente
   consistente y aun así estar desconectado de lo que el resto del proyecto realmente usa
   como referencia vigente. No asumas que la carpeta "más nueva" o "más completa" es la
   fuente de verdad si nada externo la señala como tal todavía.

### Fase C — Diseño de la organización

1. Proponer la estructura antes de editar.
2. Separar tutorial, guía práctica, referencia y explicación según el propósito de cada
   documento (no mezclarlos sin justificación).
3. Definir propietario, fuente de verdad, estado y criterio de vigencia de cada pieza.
4. Mantener las decisiones normativas separadas de las explicaciones.
5. Identificar información que debe conservarse literalmente (citas, decisiones, datos
   normativos) y no resumirse.
6. Registrar decisiones y sus motivos en el estado durable.

### Fase D — Ejecución controlada

1. Exponer/usar solo las herramientas necesarias para el paso actual.
2. Validar rutas, permisos y precondiciones antes de cada edición (ver la tabla de
   contratos de herramientas en `references/herramientas-y-arquitectura.md`).
3. Preferir cambios reversibles y pequeños por sobre reescrituras grandes.
4. Revisar el diff y la evidencia después de cada modificación.
5. Actualizar enlaces e índices como parte de la misma unidad de trabajo que mueve o
   renombra un documento — nunca en un paso separado que se puede olvidar.
6. Registrar cada transición relevante y crear checkpoints intermedios en tareas largas.

### Fase E — Verificación

1. Ejecutar comprobaciones de sintaxis y formato (Markdown, front matter).
2. Ejecutar comprobaciones de enlaces y rutas.
3. Comparar inventario antes/después para detectar pérdidas de contenido.
4. Verificar índices, fuentes de verdad, glosario y metadatos.
5. Hacer una pasada de revisión adversarial (ver la rúbrica de rechazo en
   `references/evidencia-y-verificacion.md`): buscar activamente motivos para rechazar el
   resultado, no solo confirmar que "se ve bien".
6. Rechazar (y corregir) cualquier afirmación sin evidencia.
7. Solicitar aprobación explícita si alguna acción la requiere (eliminar contenido,
   cambiar una fuente de verdad, mover documentación de otro equipo, publicar afuera).

### Fase F — Recuperación

1. Clasificar cada fallo según las clases de `references/politica-y-recuperacion.md`
   (timeout, argumentos inválidos, contexto faltante, referencia rota, fuentes en
   conflicto, propiedad denegada, fallo de verificación, fallo repetido sin cambios).
2. Cambiar al menos una condición relevante antes de reintentar — repetir la misma
   acción sin cambios no es recuperación.
3. Respetar límites de intentos, tiempo, costo y alcance destructivo.
4. Escalar (pedir a un humano) ante contradicciones, problemas de propiedad de
   documentos, o fallos que se repiten sin cambiar.
5. Restaurar desde el checkpoint si una modificación produjo daño.
6. Registrar la lección aprendida y, si el fallo es recurrente, proponer una mejora
   concreta del proceso (ver el registro de mejora en
   `references/mantenimiento-del-harness.md`).

### Fase G — Entrega

1. Generar el recibo de cambios (plantilla completa en
   `references/infraestructura-y-observabilidad.md`).
2. Separar explícitamente lo verificado de lo no verificado.
3. Declarar riesgos residuales y aprobaciones pendientes.
4. Entregar el estado durable (facts/decisions/progress/lessons) para que una sesión
   futura pueda continuar sin releer todo el historial.
5. Actualizar los índices o el mapa del proyecto si la estructura cambió.
6. Reportar, si corresponde, métricas de aceptación, revisión y recuperación (ver
   `references/mantenimiento-del-harness.md`).

## Checklist de aceptación

Solo se puede cerrar la tarea si se puede responder afirmativamente a todos los puntos
obligatorios (adaptar los que no apliquen a una tarea chica, pero no omitirlos sin decirlo):

**Contrato**
- [ ] El objetivo está expresado como resultado observable.
- [ ] El alcance está delimitado.
- [ ] Las restricciones están registradas.
- [ ] La evidencia de finalización está definida.
- [ ] Las aprobaciones necesarias están identificadas.

**Contexto**
- [ ] Se consultó el mapa antes del detalle.
- [ ] Las fuentes relevantes fueron recuperadas.
- [ ] Las fuentes obsoletas o conflictivas están marcadas.
- [ ] Se preservó literalmente lo que no podía resumirse o alterarse.

**Organización**
- [ ] Cada documento tiene un propósito claro.
- [ ] Los tipos de documentación no se mezclan sin justificación.
- [ ] La fuente de verdad de cada tema es identificable.
- [ ] Hay propietario, estado y vigencia cuando corresponde.
- [ ] No quedaron duplicados normativos sin regla de precedencia.

**Herramientas y política**
- [ ] Todas las modificaciones usaron herramientas permitidas.
- [ ] Se validaron rutas, permisos y precondiciones.
- [ ] Las acciones riesgosas fueron aprobadas o bloqueadas.
- [ ] No se declararon comprobaciones que no se ejecutaron.

**Evidencia**
- [ ] Existe diff o inventario antes/después.
- [ ] Los enlaces y anclas pasan.
- [ ] Los índices coinciden con el árbol real.
- [ ] La revisión adversarial intentó rechazar el resultado.
- [ ] Se documentaron límites y riesgos residuales.

**Recuperación y trazabilidad**
- [ ] La ejecución puede reconstruirse desde la traza.
- [ ] Existe al menos un checkpoint confiable.
- [ ] Los fallos fueron clasificados.
- [ ] Los reintentos tuvieron una razón y cambiaron una condición relevante.
- [ ] Los fallos recurrentes produjeron, cuando correspondía, una mejora del proceso.

**Entrega**
- [ ] Se generó el recibo de cambios.
- [ ] El estado durable quedó disponible.
- [ ] Se separó lo verificado de lo no verificado.
- [ ] Se declararon aprobaciones pendientes.
