# Ejemplo resuelto — Historia de Usuario en formato Taiga

## Contexto de entrada que aportó el equipo (resumido)

- **Objetivo del sprint (S1):** un docente autorizado carga y consulta casos de
  referencia (*golden set*) y **los datos sobreviven al reinicio** del servicio.
- **Paquete de la receta:** «API golden set v1 — implementar solo operaciones existentes
  del OpenAPI; validar actor/ownership/idempotencia. Persistencia en PostgreSQL con
  volumen que sobrevive a `docker compose restart`.»
- **Rol real:** docente autorizado de una cohorte.
- **Contrato (adenda S1):** `GET /api/llm/golden-sets?rubricVersion={v}&page={n}&size={s}`
  y `GET /api/llm/golden-sets/{goldenSetId}`. Todo pasa por el Gateway (`/api/llm/**`),
  auth M2M + usuario delegado, errores como *Problem Details* (RFC 7807).
- **Historia canónica:** aún sin fijar — esta historia es **candidata a canónica**
  (pequeña, entendida por todos, recorrido completo).
- **Épica:** EP-03 · Golden set y referencia humana.
- **Prefijo de ID:** `LLM-S01-Hyy`. Referencia de plan: 14 h.

Del barrido salieron varias historias del paquete (alta + carga → H05; consulta → H06;
pantalla → H07). Acá se muestra **H06**.

---

## Ficha generada

# [G07] — Consulta del golden set que sobrevive al reinicio

> **ID interno:** `LLM-S01-H06` · **Tipo:** HU de valor · **candidata a historia
> canónica** · **Épica:** EP-03 · **Responsable / suplente:** P5 / *(a nombrar)* ·
> **Requisito:** RF-IA-30 a 36

## Descripción (Como / Quiero / Para)

- **Como:** docente autorizado de una cohorte
- **Quiero:** consultar mi golden set y sus entradas aunque el servicio se reinicie
- **Para:** confiar en que el dato de referencia que cargué persiste y puedo calibrar
  sobre él

## Notas / Observaciones

- **Reglas de negocio:** la consulta se hace por el Gateway con la misma autorización
  que la escritura. El listado es paginado (`page` desde 0, `size` entre 1 y 100) y
  ordenado por fecha de creación descendente. El detalle incluye las entradas. Una
  cohorte ajena **no aparece** en la respuesta.
- **Validaciones:** un `goldenSetId` inexistente devuelve `404` (no `500`); un `size`
  fuera de 1–100 devuelve `400`.
- **Datos obligatorios:** ninguno en el cuerpo (es `GET`); parámetros `rubricVersion`
  (opcional), `page`, `size`.
- **Performance:** los datos se leen de PostgreSQL con volumen persistente; tras
  `docker compose restart` la lectura devuelve exactamente lo cargado antes.
- **Seguridad:** sin scope `llm.golden-set.manage` y usuario delegado → `403`; el filtro
  por cohorte se aplica siempre, no es un parámetro opcional.
- **Accesibilidad:** no aplica en esta historia (la pantalla es otra historia).
- **Endpoints:**
  - `GET /api/llm/golden-sets?rubricVersion={v}&page={n}&size={s}` → lista de
    `{ id, version, rubricVersion, language, createdAt }`.
  - `GET /api/llm/golden-sets/{goldenSetId}` → lo anterior +
    `entries[] { id, transcript, referenceScores, createdAt }`.

## Criterios de Aceptación (CA)

- **CA1:** un golden set creado y con una entrada se puede consultar y devuelve esa
  entrada.
- **CA2:** después de `docker compose restart`, la misma consulta devuelve los mismos
  datos.
- **CA3:** el listado respeta `page`/`size` y el orden por creación descendente.
- **CA4 (negativo):** el golden set de otra cohorte no aparece en el listado y su
  detalle responde `404`.
- **CA5 (negativo):** `goldenSetId` con formato UUID pero inexistente → `404` *Problem
  Details*, nunca `500`.
- **CA6 (negativo):** `size=500` → `400`.

## BDD (mínimo 3 escenarios — 1 camino feliz + ≥ 2 negativos)

**Característica:** lectura del golden set con persistencia garantizada.

### Escenario 1 — La consulta sobrevive al reinicio (camino feliz)

- **Dado:** un docente que creó un golden set y le cargó una entrada
- **Cuando:** se ejecuta `docker compose restart` del servicio y su base
- **Y:** el docente vuelve a consultar el golden set por el Gateway
- **Entonces:** la respuesta `200` contiene el golden set y la entrada tal como se
  cargaron

### Escenario 2 — Aislamiento entre cohortes

- **Dado:** dos golden sets, uno de la cohorte del docente y otro de una cohorte ajena
- **Cuando:** el docente pide el listado
- **Entonces:** solo aparece el golden set de su cohorte
- **Y:** pedir el detalle del ajeno responde `404`

### Escenario 3 — Golden set inexistente

- **Dado:** un `goldenSetId` con formato válido que no corresponde a ningún golden set
- **Cuando:** el docente consulta su detalle
- **Entonces:** el sistema responde `404` con *Problem Details*, no `500`

### Escenario 4 — Paginación fuera de rango

- **Dado:** el endpoint de listado con `size` limitado a 1–100
- **Cuando:** el cliente pide `size=500`
- **Entonces:** el sistema responde `400` y no devuelve datos

## Prototipo

- **Capturas:** boceto del listado de golden sets (versión, rúbrica, idioma, fecha) y
  del detalle con las entradas y sus cinco puntajes.
- **Mock API / Swagger:** adenda S1 del golden set (endpoints de arriba).

## Estimación / Prioridad

| Puntos (Fibonacci) | Prioridad (MoSCoW) |
|---|---|
| *(historia canónica — se estima primera en Sprint 0; su valor fija la referencia del backlog)* | Must |

> Referencia de planificación en horas del plan: **14 h** — dato separado, no se
> convierte a puntos.

## Dependencias / Impactos

- **Servicios involucrados:** Gateway, PostgreSQL, `courses-service` (filtro por
  cohorte).
- **Módulos afectados:** `api`, `application`, `infrastructure`, `security`.
- **Otros equipos / aprobaciones:** contrato de lectura acordado con `admin-service`
  (adenda S1).
- **Impacto en datos / migraciones:** solo lectura; exige que el volumen de PostgreSQL
  persista entre reinicios.
- **Riesgos y mitigación:** si el volumen no persiste, la demo falla; se verifica con la
  prueba automatizada de reinicio.

## Tareas

> Pasos técnicos del equipo (P5). Se cargan en Taiga como tareas hijas de la HU. Horas
> orientativas.

| # | Tarea | Paso | h |
|---|---|---|--:|
| T1 | Caso de uso «listar»: paginado (`page` desde 0, `size` 1–100), orden por creación desc., filtro por cohorte siempre aplicado | Caso de uso | 4 |
| T2 | Caso de uso «detalle»: golden set + `entries[]`; cohorte ajena → `404`; `goldenSetId` inexistente → `404`, nunca `500` | Caso de uso | 4 |
| T3 | Autorización de lectura (mismo scope y usuario delegado que la escritura); `size=500` → `400` | Seguridad y resiliencia | 3 |
| T4 | Prueba de integración de persistencia: `docker compose restart` y la consulta devuelve exactamente lo cargado | Prueba E2E | 3 |
| | **Total** | | **14** |

---

## Por qué queda así

- **COMO = docente autorizado**, un rol real que percibe el resultado → es **HU de
  valor**, no tarea. (Comparar con H01 «Como equipo, quiero un ADR…»: esa falla la V y
  va como tarea.)
- **Un rol, una acción, un resultado observable**: «consultar mi golden set aunque el
  servicio se reinicie». El título no necesita «y/o».
- **BDD = 1 feliz + 3 negativos.** El camino feliz es *la* razón de ser de la historia
  (sobrevive al reinicio). Los negativos salen de recorrer el contrato: aislamiento de
  cohortes, id inexistente (`404` vs `500`), paginación fuera de rango.
- Cada **Entonces es observable**: «la respuesta `200` contiene…», «responde `404`»,
  «solo aparece el golden set de su cohorte». Nada de «el sistema verifica…».
- El **Y hereda**: en el escenario 1, el `Y` después de `Cuando` suma otra acción
  (volver a consultar); en el escenario 2, el `Y` después de `Entonces` suma otro
  resultado (el detalle ajeno da `404`).
- Las **Notas** dicen qué debe cumplirse (rangos de paginación, filtro por cohorte
  siempre aplicado, persistencia) sin decir cómo se implementa el repositorio.
- **Sin puntos asignados**: se deja para Planning Poker en Sprint 0. Como es la
  candidata a canónica, se estima **primera** y su valor ancla el resto del backlog.
- **Prioridad Must**: no por su valor aislado, sino porque la demo del sprint depende de
  ella y es condición para la pantalla (H07).
- **Tareas sin Como/Quiero/Para**, en orden de construcción, cada una ≤ 1 jornada,
  sumando las 14 h de referencia.
