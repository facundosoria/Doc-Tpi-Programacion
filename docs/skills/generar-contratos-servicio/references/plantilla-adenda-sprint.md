# Adenda S<NN> — <feature>

Esta adenda completa el contrato v1 para satisfacer la demo de S<NN>: [una frase con el
recorrido que habilita]. Debe **fusionarse** en `<servicio>-v1.openapi.yaml`
(y/o `.asyncapi.yaml`) cuando se apruebe el cambio con `<equipo consumidor>`.

> Regla: cualquier campo que hoy **no exista** se **congela** con el consumidor antes de
> publicarlo. No se agrega al contrato «por las dudas».

## Lectura

`GET /api/<n>/<recurso>?<param>={valor}&page=0&size=20`

- Requiere el scope M2M `<scope>` y usuario delegado.
- `page` inicia en 0; `size` entre 1 y 100.
- Devuelve `[{ <campos> }]`, ordenado por [criterio].

`GET /api/<n>/<recurso>/{<recurso>Id}`

- Misma autorización. Devuelve los metadatos anteriores y `<sub-colección>`, donde cada
  elemento contiene `{ <campos> }`.

## Escritura

`POST /api/<n>/<recurso>` → `201` + header `Location`, cuerpo:

```json
{ "id": "uuid", "<campo>": "..." }
```

`POST /api/<n>/<recurso>/{id}/<sub>` → `201` + `Location`, cuerpo:

```json
{ "id": "uuid", "<fkId>": "uuid" }
```

Se conserva el `Idempotency-Key` obligatorio de OpenAPI v1.

## Negativos que esta adenda debe cubrir

- Sin scope / identidad no delegada → `403`.
- Entrada inválida (campo faltante o fuera de rango) → `400`/`422` Problem Details.
- Recurso de otra tenancy → no aparece en el listado; su detalle → `404` (nunca `500`).
- `size` fuera de 1–100 → `400`.
- Reintento con la misma `Idempotency-Key` → mismo resultado, sin duplicar.
