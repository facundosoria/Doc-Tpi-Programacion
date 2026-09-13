# Adenda S1 — Golden sets

Esta adenda completa el contrato v1 para satisfacer la demo de S1: un docente autorizado crea, carga y consulta un golden set después de reiniciar. Debe fusionarse en `llm-service-v1.openapi.yaml` cuando se apruebe el cambio de contrato con `admin-service`.

## Lectura

`GET /api/llm/golden-sets?rubricVersion={version}&page=0&size=20`

- Requiere el scope M2M `llm.golden-set.manage` y usuario delegado.
- `page` inicia en 0; `size` está entre 1 y 100.
- Devuelve una lista de `{ id, version, rubricVersion, language, createdAt }`, ordenada por creación descendente.

`GET /api/llm/golden-sets/{goldenSetId}`

- Requiere la misma autorización.
- Devuelve los metadatos anteriores y `entries`, donde cada elemento contiene `{ id, transcript, referenceScores, createdAt }`.

## Escritura

Los dos `POST` existentes devuelven `201`, un header `Location` y estos cuerpos:

```json
{ "id": "uuid", "version": 1, "rubricVersion": "1.0", "language": "es", "createdAt": "date-time" }
```

para crear un golden set, y:

```json
{ "id": "uuid", "goldenSetId": "uuid" }
```

para crear una entrada. Se conserva el `Idempotency-Key` obligatorio de OpenAPI v1.
