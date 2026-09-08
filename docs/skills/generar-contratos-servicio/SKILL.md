---
name: generar-contratos-servicio
description: >-
  Genera los contratos de un microservicio y su documento de integración
  inter-equipos: OpenAPI v1 (HTTP por recursos, no RPC), AsyncAPI v1 (eventos
  que publica y consume), el contrato con el API Gateway (única puerta, M2M
  aud=<servicio>, headers de borde, correlación, identidad delegada del token
  no del body, Problem Details RFC 7807, Idempotency-Key), plantilla de adenda
  por sprint y puntero a mock. Úsalo cuando pidan "el contrato del servicio",
  "el OpenAPI", "el contrato con el Gateway", "los eventos de Kafka", "qué le
  pedimos a cada equipo" o "una adenda de contrato para el sprint".
---

# Generar contratos de servicio

Convierte **funciones + integraciones + convenciones de la plataforma** en los
documentos de contrato de un microservicio. El skill es **autónomo**: trae su copia de
las convenciones de contrato y de los esqueletos.

Se **encadena**:

- Aguas arriba con `generar-vision-y-alcance` (funciones) y con la doc de arquitectura.
- Aguas abajo: `generar-historias-usuario` cita estos contratos en el apartado
  *Prototipo / Mock API* y en *Dependencias*; `generar-backlog-y-recetas` marca en cada
  receta qué parte del contrato se implementa.

## Qué produce (todo bajo `docs/`)

```
docs/contracts/
├── <servicio>-v1.openapi.yaml          HTTP: recursos, seguridad, Problem Details
├── <servicio>-v1.asyncapi.yaml         eventos Kafka que publica y consume
├── <servicio>-v1-sNN-<feature>-adenda.md   cambios acordados por sprint (se fusionan al aprobar)
└── NN-contratos-inter-equipos.md       con quién hablamos, qué pedimos, qué damos, por dónde
```

La identidad canónica del servicio (nombre, prefijo `/api/<n>/**`, `aud`) sale de
`docs/00-fuentes-de-verdad-y-convenciones.md` si el scaffold ya corrió.

Más, dentro del doc inter-equipos, el **contrato con el API Gateway** (§ «Cómo leemos los
contratos» + § «Autenticación entre servicios»).

## Entradas que necesito (las pide el equipo al invocar)

Mínimo imprescindible:

1. **Nombre del servicio** (`<n>-service`) y su **prefijo público** (`/api/<n>/**`).
2. **Funciones / recursos** que expone: qué hace cada uno, sync o async, quién lo llama.
3. **Integraciones**: qué eventos **publica** y cuáles **consume**, y con qué equipo.
4. **Convenciones de la plataforma**: cómo identifica el Gateway a los servicios
   (`aud`, scopes), qué headers de borde inyecta, cómo viaja la correlación. Si no las
   tenés, usá las de [`references/convenciones-contrato.md`](references/convenciones-contrato.md)
   y marcá que son a confirmar con el equipo de Gateway.

Opcional:

5. **Rúbrica / esquemas de dominio** ya definidos (dimensiones, enums, versiones).
6. **Requisitos** `RF-*` que cada operación cubre.
7. **Pendientes de integración** conocidos (decisiones sin cerrar, campos en disputa).
8. **Sprint y feature** si lo que se pide es una **adenda**, no el contrato completo.

Si falta 1, 2 o 3, **pídelos antes de generar**. No inventes endpoints, eventos ni
campos «por las dudas»: el contrato describe **solo lo que existe o está acordado**.

## Método (paso a paso)

### A. OpenAPI v1

1. **Recursos, no RPC.** `POST /tutor/interactions`, no `POST /doTutor`. Sustantivos en
   plural, jerarquía por path (`/evaluations/{id}/appeals`).
2. Todas las rutas cuelgan del **prefijo público** y el Gateway **no reescribe** el path.
3. **Seguridad**: `serviceJwt` (bearer JWT M2M). El servicio valida `aud=<n>-service` y
   el **scope** que la operación requiere.
4. **Idempotencia**: toda escritura lleva el header `Idempotency-Key` (UUID) **obligatorio**.
5. **Errores**: una sola respuesta `Problem` (RFC 7807) reutilizada; el `requestId` viaja
   en `X-Request-Id` y como extensión del body.
6. **Identidad**: `courseCohortId`, `learnerId`, etc. **los deriva el servidor del token**,
   no del body. Si el cliente los manda, se ignoran (documentarlo).
7. Componentes comunes en `components` (parámetros `IdempotencyKey`, respuesta `Problem`,
   `securitySchemes`). Esquemas de dominio con `enum`, `minimum`/`maximum`, `format`.
8. Partir del esqueleto de [`references/plantilla-openapi.yaml`](references/plantilla-openapi.yaml).

### B. AsyncAPI v1 (eventos)

1. Un **canal por evento**, address `= <evento>.v<major>` (`intento_cerrado.v1`).
2. `operations` con `action: send` (publica) / `action: receive` (consume).
3. **Envelope común**: `eventId` (UUID, sirve de idempotencia), `version`, `occurredAt`,
   `producer`, `data`. La correlación viaja en **headers** (`traceparent`,
   `X-Request-Id`), no en el payload.
4. Esqueleto en [`references/plantilla-asyncapi.yaml`](references/plantilla-asyncapi.yaml).

### C. Contrato con el API Gateway + inter-equipos

Redactá el documento `NN-contratos-inter-equipos.md` con
[`references/plantilla-contratos-inter-equipos.md`](references/plantilla-contratos-inter-equipos.md):

- **Cómo leemos los contratos**: el Gateway es la única puerta (nadie llega directo, ni
  HTTP ni interno); lo asincrónico va por Kafka; la correlación viaja en headers.
- **Los dos canales** (sincrónico por Gateway / asincrónico por Kafka) con diagrama.
- **Por par**: qué nos llaman, qué nos tienen que dar, bloqueos 🔴.
- **Eventos que publicamos / consumimos** con su estructura mínima.
- **Autenticación entre servicios**: qué header, qué formato, qué claims mínimos; el
  servicio nunca confía en identidad del cliente ni se expone sin Gateway.
- **Reglas generales**: `Idempotency-Key` obligatoria, correlación en todo, errores
  tipados, no escribir en bases ajenas.

### D. Adenda de sprint (si se pide eso)

Con [`references/plantilla-adenda-sprint.md`](references/plantilla-adenda-sprint.md): las
operaciones/campos que un sprint agrega o completa, marcadas para **fusionarse** en el
OpenAPI/AsyncAPI **cuando el consumidor apruebe** el cambio. Congelar cualquier campo aún
inexistente antes de publicarlo.

### E. Mock

Dejá indicado el comando de una línea para levantar un mock del contrato (p. ej. Prism
sobre el OpenAPI, o WireMock con stubs). El mock **no** cierra una historia cuya demo
exige integración real.

## Reglas de oro

- El contrato describe **solo lo implementado o acordado**. Nada «por las dudas».
- Recursos, no RPC. Prefijo público, sin reescritura de path.
- Identidad y tenancy salen del **token**, no del body.
- `Idempotency-Key` en toda escritura; `eventId` como idempotencia en eventos.
- Un campo nuevo que cruza equipos se **congela con el consumidor** antes de publicar.
- Versionado: cambios compatibles suman; incompatibles → `v2`. La `v1` no se edita en
  destructivo.
- No modifiques contratos de **otros** equipos (Gateway, bus): documentá lo que
  necesitás de ellos en el doc inter-equipos.

## Checklist antes de entregar

- [ ] OpenAPI: rutas por recurso bajo el prefijo público; `serviceJwt`; `Idempotency-Key`
      en writes; una respuesta `Problem` reutilizada; identidad no viene del body.
- [ ] El OpenAPI y el AsyncAPI generados **validan** contra su esquema (linter en CI);
      los `<placeholders>` de las plantillas quedaron todos reemplazados.
- [ ] AsyncAPI: un canal por evento con sufijo de versión; envelope común; correlación en
      headers.
- [ ] Doc inter-equipos: Gateway única puerta, dos canales, tabla por par, eventos
      publicados/consumidos, autenticación M2M, reglas generales.
- [ ] Pendientes de integración listados con 🔴 y dueño.
- [ ] Adenda (si aplica) marcada para fusión con aprobación del consumidor.
- [ ] Comando de mock indicado.
- [ ] Nada inventado: endpoints, eventos y campos vienen de las entradas.

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/convenciones-contrato.md`](references/convenciones-contrato.md) | Estilo de la casa: recursos vs RPC, prefijo y ruteo, M2M `aud` + scopes, headers de borde, correlación, Problem Details, idempotencia, paginación, append-only. Incluye el contrato con el Gateway. |
| [`references/plantilla-openapi.yaml`](references/plantilla-openapi.yaml) | Esqueleto OpenAPI 3.1 con componentes comunes. |
| [`references/plantilla-asyncapi.yaml`](references/plantilla-asyncapi.yaml) | Esqueleto AsyncAPI 3.0 con envelope común. |
| [`references/plantilla-contratos-inter-equipos.md`](references/plantilla-contratos-inter-equipos.md) | Formato del documento «con quién hablamos, qué pedimos, qué damos». |
| [`references/plantilla-adenda-sprint.md`](references/plantilla-adenda-sprint.md) | Formato de adenda de contrato por sprint. |
| [`references/ejemplo-contrato.md`](references/ejemplo-contrato.md) | Contrato resuelto (endpoints + evento + adenda + contrato con Gateway) con notas de por qué queda así. |
