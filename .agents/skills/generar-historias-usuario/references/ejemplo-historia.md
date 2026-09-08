# Ejemplo resuelto — Historia de Usuario

> El contenido es **ilustración**, no algo a copiar. El skill genera la misma
> **estructura** para cualquier proyecto a partir de las entradas del equipo.
> Estilo por defecto: **lenguaje simple**.

## Contexto de entrada que aportó el equipo (resumido)

- **Objetivo del sprint (S1):** un profesor autorizado carga y consulta casos de
  referencia (la «colección de referencia» / *golden set*) y **los datos sobreviven al
  reinicio** del servicio.
- **Paquete de la receta:** «API golden set v1 — implementar solo operaciones existentes;
  validar identidad, dueño del curso y antiduplicado; persistencia que sobrevive a apagar
  y prender».
- **Rol real:** profesor autorizado de un curso.
- **Historia patrón (canónica):** esta misma, H06.
- **Grupo / pareja:** EP-03 / P5. Referencia de plan: 14 h.

Del barrido salieron varias historias del paquete (crear + cargar → H05; consultar →
H06; pantalla → H07). Acá se muestra **H06**.

---

## Salida — estilo por defecto (lenguaje simple)

# LLM-S01-H06 — Consultar la colección aunque el sistema se reinicie

> Título en Taiga: `G07 — Consultar la colección aunque el sistema se reinicie`.

| | |
|---|---|
| **Grupo de trabajo** | Colección de referencia y calibración humana (EP-03) |
| **Pareja a cargo** | P5 |
| **Depende de** | H04 |
| **Trabajo estimado** | 14 horas |
| **Tipo** | Historia de valor · **historia patrón** (la vara para medir las demás) |
| **Responsable del producto** | Se nombra en el Sprint 0 |

## Descripción (Como / Quiero / Para)

- **Como:** profesor autorizado de un curso.
- **Quiero:** consultar mi colección y sus ejemplos **aunque el sistema se haya
  reiniciado**.
- **Para:** confiar en que lo que cargué no se pierde y puedo seguir trabajando sobre
  eso.

## Notas / Observaciones

- **Reglas de trabajo:** la consulta se hace por la recepción central, con el mismo
  permiso que la carga. El listado viene **de a páginas** y ordenado del más nuevo al más
  viejo. El detalle incluye los ejemplos de la colección. La colección de **otro** curso
  **no aparece** nunca.
- **Cómo se controla:** pedir una colección que no existe devuelve «no encontrado» (no un
  error de servidor). Pedir una página con un tamaño imposible devuelve «pedido
  inválido».
- **Qué tiene que incluir sí o sí:** nada en el cuerpo (es una consulta); como opciones,
  la versión de la plantilla, el número de página y el tamaño de página.
- **Tiempos / volumen:** los datos se leen del «cajón» persistente de la base; después de
  apagar y prender, la lectura devuelve exactamente lo que se había cargado.
- **Seguridad:** sin el permiso necesario y sin identificación de la persona →
  «prohibido». El filtro por curso se aplica siempre, no es opcional.
- **Accesibilidad:** no aplica en esta historia (la pantalla es H07).
- **Otros:** es la **historia patrón**: chiquita, la entienden todos y tiene el recorrido
  completo (permiso + lectura + que los datos sobrevivan). Su tamaño en puntos se fija
  primero en el Sprint 0 y queda como referencia de las demás.
- **Operaciones nuevas:** «listar mis colecciones» y «ver el detalle de una colección con
  sus ejemplos».

## Criterios de Aceptación (CA)

- **CA1:** una colección creada y con un ejemplo se puede consultar y devuelve ese
  ejemplo.
- **CA2:** después de apagar y volver a prender, la misma consulta devuelve **lo mismo**.
- **CA3:** el listado respeta el número y el tamaño de página, y el orden del más nuevo
  al más viejo.
- **CA4 (caso que debe fallar):** la colección de otro curso no aparece en el listado y
  pedir su detalle responde «no encontrado».
- **CA5 (caso que debe fallar):** pedir una colección con un identificador bien formado
  pero inexistente → «no encontrado», nunca un error de servidor.
- **CA6 (caso que debe fallar):** pedir una página de tamaño 500 → «pedido inválido».

## BDD (mínimo 3 escenarios)

**Qué se prueba:** la lectura de la colección con datos que sobreviven.

### Escenario 1 — La consulta sobrevive al reinicio (camino esperado)

- **Dado:** un profesor que creó una colección y le cargó un ejemplo.
- **Cuando:** se apaga y se vuelve a prender el servicio y su base.
- **Y:** el profesor vuelve a consultar la colección por la recepción central.
- **Entonces:** la respuesta contiene la colección y el ejemplo tal como se cargaron.

### Escenario 2 — Cada profesor ve solo lo suyo

- **Dado:** dos colecciones, una del curso del profesor y otra de un curso ajeno.
- **Cuando:** el profesor pide el listado.
- **Entonces:** solo aparece la colección de su curso.
- **Y:** pedir el detalle de la ajena responde «no encontrado».

### Escenario 3 — Colección inexistente

- **Dado:** un identificador bien formado que no corresponde a ninguna colección.
- **Cuando:** el profesor consulta su detalle.
- **Entonces:** el servicio responde «no encontrado», no un error de servidor.

### Escenario 4 — Página de tamaño fuera de rango

- **Dado:** el listado, cuyo tamaño de página va de 1 a 100.
- **Cuando:** se pide una página de tamaño 500.
- **Entonces:** el servicio responde «pedido inválido» y no devuelve datos.

## Prototipo

- **Capturas / bocetos:** boceto del listado de colecciones (versión, plantilla, idioma,
  fecha) y del detalle con los ejemplos y sus cinco puntajes.
- **Maqueta / documentación:** el agregado de este tramo al acuerdo con el otro equipo.

## Estimación / Prioridad

- **Puntos de esfuerzo:** es la **historia patrón**, se estima primera en el Sprint 0.
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** la recepción central, la base de datos y el servicio de cursos
  (para el filtro por curso).
- **Otros equipos / aprobaciones:** el contrato de lectura acordado con el servicio de
  administración.
- **Impacto en los datos:** solo lectura; exige que el «cajón» de la base sobreviva a los
  reinicios.
- **Riesgos:** si el «cajón» no sobrevive, la demo falla; se verifica con la prueba de
  reinicio de H09.

## Tareas (los pasos técnicos)

| # | Tarea | h |
|---|---|--:|
| T1 | «Listar mis colecciones»: de a páginas, de la más nueva a la más vieja, siempre filtrado por curso | 4 |
| T2 | «Ver el detalle» con sus ejemplos; colección ajena o inexistente → «no encontrado», nunca error de servidor | 4 |
| T3 | Permiso de consulta igual al de carga; pedir una página de tamaño imposible → «pedido inválido» | 3 |
| T4 | Prueba de apagar y prender que confirma que la consulta devuelve exactamente lo mismo | 3 |
| | **Total** | **14** |

---

## La misma historia en estilo técnico (bajo pedido)

Aplicando [`referencia-estilo-tecnico.md`](referencia-estilo-tecnico.md), la misma ficha
queda como `docs/historias/s01.md` H06: metadatos con «Épica / Requisito», Notas con
«Reglas de negocio / Validaciones / Endpoints» (`GET /api/llm/golden-sets?page&size`,
`GET /api/llm/golden-sets/{goldenSetId}`), CA «(negativo)», BDD «Característica:»,
Estimación como tabla Fibonacci + MoSCoW, «Servicios involucrados: Gateway, PostgreSQL,
courses-service». Mismos 4 escenarios, mismas 4 tareas, mismas 14 h.

---

## Por qué queda así

- **COMO = profesor autorizado**, un rol real que percibe el resultado → **Historia de
  valor**, no tarea. (Comparar con H01 «Como equipo…»: esa es «Tarea interna».)
- **Un rol, una acción, un resultado observable**: «consultar mi colección aunque el
  sistema se reinicie». El título no necesita «y/o».
- **BDD = 1 camino esperado + 3 que deben fallar.** El camino esperado es *la* razón de
  ser de la historia (sobrevive al reinicio). Los que fallan salen de recorrer la
  operación: aislamiento entre cursos, id inexistente («no encontrado» vs «error de
  servidor»), página fuera de rango.
- Cada **Entonces es observable**: «la respuesta contiene la colección…», «responde no
  encontrado», «solo aparece la colección de su curso». Nada de «el sistema verifica…».
- El **Y hereda**: escenario 1, el `Y` tras `Cuando` suma otra acción (volver a
  consultar); escenario 2, el `Y` tras `Entonces` suma otro resultado.
- Las **Notas** dicen qué debe cumplirse (rangos de página, filtro por curso siempre,
  persistencia) sin decir cómo se implementa.
- **Sin puntos asignados**: se deja para el Sprint 0. Como es la historia patrón, se
  estima primera y su valor ancla el resto.
- **Prioridad imprescindible**: no por su valor aislado, sino porque la demo del sprint
  depende de ella y es condición para la pantalla (H07).
- **Tareas sin Como/Quiero/Para**, en orden de construcción, cada una ≤ 1 jornada,
  sumando las 14 h.
