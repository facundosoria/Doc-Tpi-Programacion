# Equipos — vista de trabajo por contraparte

> **Qué es esta carpeta.** Una subcarpeta por cada equipo de la cátedra con el que
> `llm-service` (Tema 07) se integra, con los pendientes operativos y punteros de navegación. La
> versión canónica y completa de cada contrato está en [`contracts/equipos/`](../../contracts/equipos/README.md).
>
> - **`contratos.md`** — puntero al contrato canónico en `contracts/equipos/`; no se edita como
>   una copia independiente.
> - **`pendientes.md`** — lo que falta definir para ese equipo, sea trabajo **interno** nuestro
>   (ej. algo que todavía no implementamos) o un acuerdo **cruzado** que depende de conversar
>   con ellos.
>
> **Qué contiene esta carpeta.** Cada contrato completo vive en
> `contracts/equipos/` y cada `pendientes.md` conserva el trabajo abierto de la carpeta de equipo.
> Los documentos generales de arriba quedaron como:
>
> | Documento | Qué le queda |
> |---|---|
> | 18 — Contratos inter-equipos | §0 (cómo leemos los contratos, reglas de todos), el anexo histórico y un índice corto hacia cada carpeta — el detalle por equipo (§4.1-4.8 viejas) ya se movió acá |
> | 17 — Mapa de integración | Los 3 diagramas de secuencia siguen también acá (vista cruzada para comparar los tres presupuestos de latencia a la vez) — copiados además en `tema-05` y `tema-11`. La tabla I-01 a I-16: las filas "decide: Nosotros" quedan internas, las de Product Owner y las transversales ya están repartidas |
> | [08 — Decisiones y pendientes](../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md) | Los 18 ADR (registro de decisión) y las preguntas/decisiones ya resueltas (✅). Lo abierto de la Parte B/C ya está repartido en los `pendientes.md` de equipo |
> | [04 — Funciones de IA](../../03-capacidades-de-ia/02-funciones-de-ia.md) | Qué construimos/no construimos nosotros por función — diseño interno, no contrato de equipo |
> | [06 — Operación e ingeniería](../../06-operacion-calidad-y-pruebas/01-operacion-e-ingenieria.md) | La ingeniería propia (colas, caché, despliegue); la vista contractual de UI está centralizada en [`contracts/equipos/frontend-angular.md`](../../contracts/equipos/frontend-angular.md) |
> | [11 — Glosario y metadata](../../00-gobierno-y-evolucion/03-glosario-y-metadata.md) | Colisiones de vocabulario — se queda transversal, ver más abajo |
>
> **Cómo se actualiza.** Cuando algo se acuerde (por ejemplo en la sesión de integración), se
> mueve de `pendientes.md` a la vista canónica de `contracts/equipos/` de ese equipo. Solo se
> actualizan `17`/`18` cuando el ítem es transversal a varios equipos a la vez
> (ver "Transversales" abajo). La sección "Checklist de cierre" de
> [`docs/entregas/sesion-integracion-agenda.md`](../../01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md) ya
> cubre ese paso para los ítems que se deciden en sesión.

---

## Equipos

| Equipo | Carpeta | Antecedente (ya migrado a la carpeta) |
|---|---|---|
| Tema 02 — Cursos y Matrícula | [Guía de la contraparte](tema-02-cursos-y-matricula/README.md) | [`contracts/equipos/tema-02-cursos-y-matricula.md`](../../contracts/equipos/tema-02-cursos-y-matricula.md) |
| Tema 03 — Motor de Desafíos | [Guía de la contraparte](tema-03-motor-de-desafios/README.md) | [`contracts/equipos/tema-03-motor-de-desafios.md`](../../contracts/equipos/tema-03-motor-de-desafios.md) |
| Tema 04 — Desafío Teórico ("corregir") | [Guía de la contraparte](tema-04-desafios-teoricos/README.md) | [`contracts/equipos/tema-04-desafios-teoricos.md`](../../contracts/equipos/tema-04-desafios-teoricos.md) |
| Tema 05 — Desafíos Prácticos | [Guía de la contraparte](tema-05-desafios-practicos/README.md) | [`contracts/equipos/tema-05-desafios-practicos.md`](../../contracts/equipos/tema-05-desafios-practicos.md) |
| Tema 11 — Chat | [Guía de la contraparte](tema-11-chat/README.md) | [`contracts/equipos/tema-11-chat.md`](../../contracts/equipos/tema-11-chat.md) |
| Tema 12 — Backoffice / ADMIN | [Guía de la contraparte](tema-12-backoffice-admin/README.md) | [`contracts/equipos/tema-12-backoffice-admin.md`](../../contracts/equipos/tema-12-backoffice-admin.md) |
| Backend de negocio | [Guía de la contraparte](backend-de-negocio/README.md) | [`contracts/equipos/backend-de-negocio.md`](../../contracts/equipos/backend-de-negocio.md) |
| Front End — Angular | [Guía de la contraparte](frontend-angular/README.md) | [`contracts/equipos/frontend-angular.md`](../../contracts/equipos/frontend-angular.md) |
| Product Owner | [Guía de la contraparte](product-owner/README.md) | [`contracts/equipos/product-owner.md`](../../contracts/equipos/product-owner.md) |
| Notifications Service | [`notifications-service/pendientes.md`](notifications-service/pendientes.md) | Sin contrato canónico propio todavía en `contracts/equipos/`; carpeta abierta solo para los pendientes. |

> **Tema 03 pasa a ser integración indirecta.** Desde la decisión de diseño del 2026-09-13,
> `llm-service` no vuelve a hablar directo con el Motor de Desafíos: el intercambio del
> evaluador (cierre de intento → score) queda intermediado por Tema 05 (`practice-service`).
> El contrato vigente vive en
> [`contracts/equipos/tema-05-desafios-practicos.md`](../../contracts/equipos/tema-05-desafios-practicos.md); la
> carpeta de Tema 03 conserva el contrato directo anterior marcado como retirado, para que quede
> registro de qué cambió.

> **Tema 04 no es un socio de integración técnica, todavía.** No hay endpoint, evento ni scope M2M
> entre `llm-service` y Tema 04 — solo una recomendación de código (normalización + distancia de
> edición) para que la implementen en su propio motor. Se le abrió carpeta igual porque es donde
> vive "qué falta hablar con cada equipo", y con Tema 04 sí hay algo pendiente (ver su
> `pendientes.md`), aunque hoy no sea un contrato.

---

## Transversales (no son de un solo equipo)

Estos ítems afectan a más de un equipo a la vez. Para no repetirlos en cada `pendientes.md`,
viven acá una sola vez; cada equipo afectado los nombra y linkea a esta sección.

### Autenticación entre servicios (I-14)

Formato del token M2M, header y claims mínimos — todavía sin definir. Afecta a **todos** los
equipos que llaman a `llm-service` (Tema 02, 03, 05, 11, 12). Reglas ya fijas y no negociables:
nadie llega directo al servicio, todo pasa por el API Gateway, y el servicio nunca confía en
parámetros del cliente para identidad (`18` §0 y §7, ADR-001/ADR-015).

**Decide:** sesión de integración — ver [`docs/entregas/sesion-integracion-agenda.md`](../../01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md) ítem 5.

### Resiliencia y manejo de errores (técnica común a todos los endpoints)

Cómo reaccionamos a una falla es el mismo mecanismo para todos los equipos — cada
`contratos.md` de equipo solo agrega el caso puntual ("qué le llega a este equipo cuando pasa
esto"), linkeando acá para la técnica.

- **Formato de error:** RFC 7807 Problem Details (respuesta `Problem` del
  [`llm-service-v1.openapi.yaml`](../../contracts/llm-service.openapi.yaml)), con
  `X-Request-Id` propagado en el header.
- **Escalera de degradación (RF-IA-27)**, diagrama completo en
  [06 §5](../../06-operacion-calidad-y-pruebas/01-operacion-e-ingenieria.md#5-plan-b-la-escalera-de-degradación-rf-ia-27): Nivel 1
  modelo primario → (`timeout`/`429`/`5xx`) → Nivel 2 otro proveedor → Nivel 3 modelo local →
  Nivel 4 degradación funcional. **El evaluador es la excepción:** su única degradación válida
  es la cola diferida — no tiene "modelo local" ni "funcional", porque el score tiene que ser
  real o no existir (04 línea 619).
- **Patrones ya elegidos — Resilience4j** (04 §2.3.4b/c, ADR-016):

  | Qué pasa | Patrón | Dónde |
  |---|---|---|
  | El proveedor no responde | **Circuit Breaker** | Por proveedor, en el adapter |
  | Un usuario abusa de la cuota | **Rate Limiter** (token bucket) | Gateway interno |
  | El llamador reintenta por timeout | **Idempotent Receiver** | Vía header `Idempotency-Key` — nunca duplica el efecto |
  | Un job falla repetido | **Backoff con tope → dead letter queue → alerta** | Nunca reintento infinito contra un proveedor caído |

- **El proveedor nunca entra al readiness** (ADR-014): su caída es degradación, no caída del
  servicio — la sonda solo chequea Postgres y Redis.
- **Códigos de error típicos** (contenido de doc 18 §1.5 — del contrato retirado, pero sigue
  siendo la referencia que usa el resto de los documentos; el schema `Problem` del v1 no fija
  todavía un enum cerrado de `codigo`):

  | HTTP | `codigo` | Significa |
  |---|---|---|
  | `429` | `cuota_agotada` | El alumno agotó su cuota diaria (RF-IA-22) |
  | `503` | `proveedor_no_disponible` | El proveedor LLM no responde |
  | `409` | `calibracion_pendiente` | El curso no tiene calibración aprobada |
  | `422` | `payload_invalido` | El body no pasa la validación |
  | `401` | `token_invalido` | El JWT no es válido o expiró |

### Glosario de colisiones de vocabulario

`11-glosario-y-metadata.md` documenta ocho palabras que significan cosas distintas según el
equipo que las usa (ej. "evaluación", "umbral 70%", "curso", "gateway"). Cada `pendientes.md`
de equipo que tenga una colisión puntual la nombra con una línea y linkea acá — el detalle
completo de las ocho no se duplica equipo por equipo.

### Agenda de la sesión de integración

Los ítems 🔴 que necesitan sala con varios equipos a la vez (I-04, I-05, I-08, I-09, I-14) ya
tienen agenda armada, con timebox y propuesta de apertura, en
[`docs/entregas/sesion-integracion-agenda.md`](../../01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md). Los
`pendientes.md` de equipo linkean ahí en vez de repetir el timebox y el orden de la reunión.
