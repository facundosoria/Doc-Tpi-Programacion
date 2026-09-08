---
name: contratos-api-gateway
description: >-
  Referencia para escribir los contratos de un microservicio de forma que
  cumplan con el API Gateway de la plataforma: las reglas del borde (prefijo
  /api/<n>/**, sin reescritura de path, M2M aud=<n>-service + scopes, headers de
  identidad delegada, correlación traceparent + X-Request-Id, Problem Details
  RFC 7807, Idempotency-Key), el estilo de la casa para el OpenAPI/AsyncAPI
  (recursos no RPC, paginación, dato append-only, versionado) y los esqueletos
  a rellenar. Consultá esto ANTES de escribir o modificar cualquier contrato
  (OpenAPI, AsyncAPI, doc inter-equipos, adenda de sprint) de un servicio de la
  plataforma. Las reglas del Gateway son del equipo de plataforma/borde: acá va
  el resumen, la fuente autoritativa es su documentación.
---

# Contratos y API Gateway — referencia

**Esto es una referencia, no un generador.** No transforma una entrada en una salida:
te dice **las reglas fijas** que un contrato de servicio tiene que cumplir para pasar por
el API Gateway de la plataforma, y te da los **esqueletos** a rellenar. El OpenAPI y los
eventos concretos los define el equipo del servicio; esta referencia dice *con qué forma*.

> **De quién es cada cosa.** Las reglas del **Gateway / borde** (secciones 1-5 y 10 de
> `references/convenciones-contrato.md`) las define el **equipo de plataforma/borde**, no
> el equipo del servicio. Acá va el resumen para tenerlo a mano; la fuente autoritativa
> es la documentación de ese equipo (`docs/gateway-y-discovery/`, `docs/00 §3-4`). No se
> modifican desde acá: si algo no cuadra, se habla con plataforma. El resto (recursos vs
> RPC, paginación, append-only, versionado) es estilo de la casa para tus contratos.

## Cuándo consultarla

Antes de:

- escribir o cambiar el **OpenAPI** de un servicio,
- definir un **evento** que se publica o consume (AsyncAPI),
- redactar el **documento inter-equipos** (a quién le pedimos / le damos qué),
- preparar una **adenda de contrato** para un sprint,
- decidir cómo se identifica un llamador, cómo viaja la correlación o qué código de
  error devolver.

## Qué hay acá

| Archivo | Para qué |
|---|---|
| [`references/convenciones-contrato.md`](references/convenciones-contrato.md) | Las 12 reglas: nombres en 3 niveles, ruteo, M2M `aud` + scopes, Problem Details, correlación, idempotencia, recursos vs RPC, paginación, dato académico append-only, los dos canales, versionado, qué NO hacer. Marca cuáles son de plataforma. |
| [`references/plantilla-openapi.yaml`](references/plantilla-openapi.yaml) | Esqueleto OpenAPI 3.1 con los componentes comunes (`IdempotencyKey`, respuesta `Problem`, `serviceJwt`). Rellenar con los recursos del servicio. |
| [`references/plantilla-asyncapi.yaml`](references/plantilla-asyncapi.yaml) | Esqueleto AsyncAPI 3.0 con el envelope común (`eventId`, `version`, `occurredAt`, `producer`, `data`). |
| [`references/plantilla-contratos-inter-equipos.md`](references/plantilla-contratos-inter-equipos.md) | Formato del documento «con quién hablamos, qué pedimos, qué damos, por dónde» (incluye la sección del contrato con el Gateway). |
| [`references/plantilla-adenda-sprint.md`](references/plantilla-adenda-sprint.md) | Formato de una adenda de contrato por sprint. |
| [`references/ejemplo-contrato.md`](references/ejemplo-contrato.md) | Un contrato resuelto (endpoints + evento + adenda + sección Gateway) con notas de por qué queda así. |

## Cómo se usa

1. **Leé `references/convenciones-contrato.md`** entero la primera vez. Es corto.
2. Para el **OpenAPI**: copiá `plantilla-openapi.yaml`, reemplazá `<n>` por el nombre del
   servicio, y agregá **un recurso por operación real** (sustantivo en plural, jerarquía
   por path, `POST /tutor/interactions` no `POST /doTutor`). Toda escritura lleva
   `Idempotency-Key`. Una sola respuesta `Problem` reutilizada. La identidad
   (`courseCohortId`, `learnerId`, …) **sale del token**, no del body.
3. Para los **eventos**: copiá `plantilla-asyncapi.yaml`, un canal por evento con sufijo
   de versión (`intento_cerrado.v1`), `action: send`/`receive`. La correlación va en
   headers, no en el payload.
4. Para el **doc inter-equipos**: copiá `plantilla-contratos-inter-equipos.md`. Listá por
   par qué te llaman, qué te tienen que dar, y los bloqueos 🔴. Incluí la sección de
   autenticación M2M y las reglas generales.
5. Para una **adenda**: `plantilla-adenda-sprint.md`. Las operaciones/campos que el
   sprint agrega, marcados para fusionarse en el OpenAPI **cuando el consumidor apruebe**.
   Cualquier campo que hoy no existe se **congela con el consumidor** antes de publicarlo.
6. Dejá indicado el comando de una línea para levantar un **mock** (Prism sobre el
   OpenAPI, o WireMock). El mock no cierra una historia cuya demo exige integración real.

## Reglas de oro (resumen de la referencia)

- El contrato describe **solo lo implementado o acordado**. Nada «por las dudas».
- **Recursos, no RPC.** Prefijo público `/api/<n>/**`, sin reescritura de path.
- **Identidad y tenancy salen del token**, no del body.
- `Idempotency-Key` en toda escritura; `eventId` como idempotencia en eventos.
- **Correlación en todo:** `traceparent` + `X-Request-Id` en HTTP y en headers Kafka; la
  respuesta devuelve el mismo `X-Request-Id`.
- Errores como **Problem Details** con `codigo` tipado.
- Un campo nuevo que cruza equipos se **congela con el consumidor** antes de publicar.
- Versionado: compatibles suman a `v1`; incompatibles → `v2`; la `v1` no se edita en
  destructivo.
- **No modifiques contratos de otros equipos** (Gateway, bus): documentá lo que
  necesitás de ellos en el doc inter-equipos, con dueño y fecha.

## Checklist de un contrato terminado

- [ ] OpenAPI: rutas por recurso bajo `/api/<n>/**`; `serviceJwt`; `Idempotency-Key` en
      writes; una respuesta `Problem` reutilizada; identidad no viene del body.
- [ ] OpenAPI y AsyncAPI **validan** contra su esquema (linter en CI); sin `<placeholders>`.
- [ ] AsyncAPI: un canal por evento con sufijo de versión; envelope común; correlación en
      headers.
- [ ] Doc inter-equipos: Gateway única puerta, los dos canales, tabla por par, eventos
      publicados/consumidos, autenticación M2M, reglas generales.
- [ ] Pendientes de integración listados con 🔴 y dueño.
- [ ] Adenda (si aplica) marcada para fusión con aprobación del consumidor.
- [ ] Comando de mock indicado.
- [ ] Nada inventado: endpoints, eventos y campos vienen del alcance del servicio.

## Relación con el resto del bundle

- `generar-vision-y-alcance` da las **funciones** del servicio → de ahí salen los recursos
  y los eventos.
- `generar-historias-usuario` **cita** el contrato en *Prototipo / Mock API* y en
  *Dependencias*.
- `generar-backlog-y-recetas` marca en cada receta **qué parte del contrato** se
  implementa en ese sprint.

Para el skill hub, esto iría como `type: reference` (no `convention`), owned por el
equipo de plataforma.
