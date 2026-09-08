# Convenciones de contrato (estilo de la casa)

Material fijo. Reglas del **canal sincrónico (HTTP por el API Gateway)**. Destiladas de
los contratos v1 vigentes y de la guía de Gateway/Discovery.

> Para el **canal asíncrono** (eventos por Kafka) — nombre de topic, envelope, correlación
> en headers de Kafka, at-least-once, outbox, versionado de eventos — ver el skill
> **`contratos-kafka`**. Las secciones 5 (correlación), 6 (idempotencia) y 10 (los dos
> canales) de acá tienen su contraparte allá.

---

## 1. Contrato de nombres en tres niveles

| Nivel | Forma | Ejemplo | Para qué |
|---|---|---|---|
| Repositorio Git | `tpi-<nombre>` | `tpi-evaluations` | organización entre equipos; **no** participa del ruteo |
| Eureka / `spring.application.name` | `<nombre>-service` | `evaluations-service` | identidad lógica |
| Prefijo público en el Gateway | `/api/<nombre>/**` | `/api/evaluations/**` | API pública |

El Gateway deriva el prefijo del `spring.application.name`: minúsculas y sin el sufijo
`-service`. El repo Git no cuenta.

## 2. Ruteo

- El **API Gateway es la única puerta**. Nadie llega directo al servicio: ni HTTP, ni
  gRPC, ni llamada interna. Todo pasa por el Gateway, que valida el token.
- El Gateway **no reescribe el path**: reenvía `/api/<n>/me` tal cual y el backend mapea
  el prefijo completo (`@RequestMapping("${app.api.private-path}")`). Sin reescrituras
  mágicas.
- **Público vs. protegido**:
  - `/api/<n>/public/**` → abierto (login, registro, JWKS); `permitAll()` solo para
    rutas documentadas.
  - `/api/<n>/**` → exige JWT válido en el Gateway; el servicio autoriza por rol/scope.

## 3. Seguridad M2M

- Esquema `serviceJwt`: `type: http`, `scheme: bearer`, `bearerFormat: JWT`.
- El servicio valida **`aud=<n>-service`** y el **scope** que la operación requiere
  (p. ej. `llm.golden-set.manage`).
- **Identidad y tenancy salen del token**, nunca del body. `courseCohortId`,
  `learnerId`, `alumno_id`… los deriva el servidor del JWT que propaga el Gateway. Si el
  cliente los manda en el body, **se ignoran** (documentarlo en la operación).
- Headers de borde que inyecta el Gateway (según la plataforma): identidad del servicio
  (`X-Service-Id` / `X-Service-Scopes`), usuario delegado (`X-Delegated-User` /
  `X-User-Id`), y la correlación (§5). Una identidad delegada ausente o falsificada →
  `403`, sin ejecutar la acción.

## 4. Errores — Problem Details (RFC 7807)

- Una sola respuesta reutilizable `Problem` en `components/responses`.
- Cuerpo con `type`, `title`, `status`, `detail`. El `requestId` viaja en el header
  `X-Request-Id` **y** como extensión del body.
- **Errores tipados**: campo `codigo` estable (`cuota_agotada`, `calibracion_pendiente`,
  `payload_invalido`, …), nunca un string libre.
- `404` para recurso inexistente (nunca `500`). `400`/`422` para entrada inválida.
  `401` token inválido / sin scope. `403` ownership o identidad delegada. `409`
  conflicto de estado. `429` cuota. `503` proveedor no disponible.

## 5. Correlación

- Cada request y cada evento propaga `traceparent` y `X-Request-Id`.
- Los **cuerpos no llevan** `trace_id`/`traceId`: la correlación es de headers.
- La respuesta devuelve el mismo `X-Request-Id` que llegó.

## 6. Idempotencia

- Toda **escritura HTTP** lleva el header `Idempotency-Key` (UUID) **obligatorio**.
- El reintento con la misma clave devuelve el mismo resultado sin crear un segundo
  recurso. Índice único por `(operación, servicio, usuario delegado, Idempotency-Key)`.
- En **eventos**, `eventId` (UUID) cumple ese rol: reprocesar no duplica efectos.

## 7. Recursos, no RPC

| Bien (recursos) | Mal (RPC) |
|---|---|
| `POST /tutor/interactions` | `POST /doTutor` |
| `GET /evaluations/{id}` | `POST /getEvaluation` |
| `POST /evaluations/{id}/appeals` | `POST /appealEvaluation` |
| `POST /golden-sets/{id}/entries` | `POST /addGoldenSetEntry` |

Sustantivos en plural, jerarquía por path, el verbo lo pone el método HTTP. Async: la
operación devuelve `202` + `Location` a `/jobs/{jobId}`; el estado se consulta ahí.

## 8. Paginación y listados

- `page` desde `0`; `size` entre `1` y `100` (fuera de rango → `400`).
- Orden explícito (p. ej. creación descendente).
- El filtro por tenancy (cohorte, curso) **se aplica siempre**, no es un parámetro
  opcional. Un recurso de otra tenancy no aparece y su detalle da `404`.

## 9. Dato académico append-only

Las tablas de dato de referencia (golden set, entradas, auditoría) son **append-only**:
un `UPDATE`/`DELETE` destructivo se rechaza a nivel base. Cambiar una rúbrica exige una
**versión nueva**, no editar la vigente.

## 10. Los dos canales

| Canal | Transporte | Contrato | Garantías |
|---|---|---|---|
| Sincrónico | HTTP por el Gateway | OpenAPI v1 | request/response, `200/202/4xx/5xx` |
| Asincrónico | Kafka | AsyncAPI v1 | publicar un evento **no** es un POST a otro micro |

## 11. Versionado

- Cambios **compatibles** (agregar campo opcional, nuevo endpoint) suman a `v1`.
- Cambios **incompatibles** → `v2`. La `v1` no se edita en destructivo.
- Un campo nuevo que cruza equipos se **congela con el consumidor** antes de publicarlo;
  no se agrega al contrato «por las dudas».
- Los canales de eventos llevan el sufijo de versión mayor en el address:
  `intento_cerrado.v1`.

## 12. Qué NO hacer

- No exponer el servicio sin el Gateway.
- No confiar en identidad que venga del cliente.
- No escribir en bases de otros equipos: se devuelve, el dueño persiste.
- No modificar los contratos de otros equipos (Gateway, bus): lo que necesitás de ellos
  va documentado en el doc inter-equipos, marcado 🔴 con dueño y fecha.
- No documentar endpoints o eventos que no existen ni están acordados.
