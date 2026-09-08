# Ejemplo resuelto — Contrato de un servicio (slice de un sprint)

> El contenido de abajo es **ilustración**, no algo a copiar. El skill genera la misma
> **estructura** para cualquier servicio a partir de las entradas del equipo.

## Contexto de entrada que aportó el equipo (resumido)

- **Servicio:** `llm-service` · prefijo `/api/llm/**`.
- **Recurso del sprint:** golden set — un docente autorizado crea un golden set, carga
  entradas con puntaje de referencia y las consulta tras reiniciar.
- **Integración:** consume `intento_cerrado.v1` (equipo de desafíos); publica
  `score_de_ia_calculado.v1`. Para este sprint solo el recurso golden set.
- **Convenciones de plataforma:** Gateway única puerta, M2M `aud=llm-service`, scope
  `llm.golden-set.manage`, correlación `traceparent` + `X-Request-Id`.
- **Sprint / feature:** S1 / golden set → se pide una **adenda**, no el contrato entero.

## Salida A — fragmento de OpenAPI v1

```yaml
paths:
  /golden-sets:
    post:
      summary: Crea una versión de golden set
      operationId: createGoldenSet
      parameters: [ { $ref: '#/components/parameters/IdempotencyKey' } ]
      requestBody:
        required: true
        content:
          application/json:
            schema: { type: object, required: [rubricVersion, language],
                      properties: { rubricVersion: { type: string }, language: { type: string, const: es } } }
      responses:
        '201': { description: Golden set creado, headers: { Location: { schema: { type: string } } } }
        '403': { $ref: '#/components/responses/Problem' }
  /golden-sets/{goldenSetId}/entries:
    post:
      summary: Incorpora una transcripción con puntaje docente de referencia
      operationId: createGoldenSetEntry
      parameters: [ { $ref: '#/components/parameters/GoldenSetId' }, { $ref: '#/components/parameters/IdempotencyKey' } ]
      requestBody:
        required: true
        content:
          application/json:
            schema: { type: object, required: [transcript, referenceScores],
                      properties: { transcript: { type: array, items: { type: object } },
                                    referenceScores: { $ref: '#/components/schemas/Dimensions' } } }
      responses:
        '201': { description: Entrada creada }
        '422': { $ref: '#/components/responses/Problem' }
components:
  parameters:
    IdempotencyKey: { name: Idempotency-Key, in: header, required: true, schema: { type: string, format: uuid } }
    GoldenSetId:    { name: goldenSetId, in: path, required: true, schema: { type: string, format: uuid } }
  responses:
    Problem: { description: RFC 7807 Problem Details; requestId en X-Request-Id y extensión del body. }
  schemas:
    Dimensions:
      type: object
      required: [clarity, autonomy, progression, compliance, efficiency]
      properties:
        clarity:     { type: integer, minimum: 0, maximum: 100 }
        autonomy:    { type: integer, minimum: 0, maximum: 100 }
        progression: { type: integer, minimum: 0, maximum: 100 }
        compliance:  { type: integer, minimum: 0, maximum: 100 }
        efficiency:  { type: integer, minimum: 0, maximum: 100 }
```

## Salida B — adenda de sprint

```markdown
# Adenda S1 — Golden sets

Completa el contrato v1 para la demo de S1: un docente crea, carga y consulta un golden
set tras reiniciar. Se fusiona en `llm-service-v1.openapi.yaml` al aprobar el cambio con
`admin-service`.

## Lectura
GET /api/llm/golden-sets?rubricVersion={v}&page=0&size=20
- Scope M2M `llm.golden-set.manage` + usuario delegado. page desde 0; size 1–100.
- Devuelve [{ id, version, rubricVersion, language, createdAt }], creación descendente.

GET /api/llm/golden-sets/{goldenSetId}
- Misma autorización. Agrega `entries[] { id, transcript, referenceScores, createdAt }`.

## Escritura
POST /api/llm/golden-sets  → 201 + Location, { id, version, rubricVersion, language, createdAt }
POST /api/llm/golden-sets/{id}/entries → 201 + Location, { id, goldenSetId }
Idempotency-Key obligatoria (de OpenAPI v1).
```

## Salida C — sección «contrato con el Gateway» del doc inter-equipos

- El Gateway rutea `/api/llm/**` sin reescritura de path; `llm-service` registrado en
  Eureka.
- M2M: valida `aud=llm-service` y scope `llm.golden-set.manage`. Token sin scope → `401`.
- Identidad delegada (`X-Delegated-User`) ausente o falsificada → `403`, sin ejecutar.
- `courseCohortId` / ownership de cohorte se derivan del token; el body no los trae.
- Errores como Problem Details; `X-Request-Id` de vuelta en cada respuesta.

---

## Por qué queda así

- **Recursos, no RPC**: `POST /golden-sets/{id}/entries`, no `POST /addGoldenSetEntry`.
- El contrato trae **solo** las 2 operaciones del sprint; el resto del OpenAPI llega
  cuando esas funciones existan.
- **Idempotency-Key** en las dos escrituras; identidad **fuera del body**.
- Una sola respuesta `Problem` reutilizada para todos los errores.
- Se entrega como **adenda** marcada para fusión con aprobación de `admin-service`: no se
  toca el `v1` publicado hasta el acuerdo.
- El contrato con el Gateway va en el doc inter-equipos, no reescribe nada del equipo de
  Gateway: solo dice qué se necesita de él (ruta, `aud`, scopes, headers).
