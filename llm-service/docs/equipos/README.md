# Equipos — vista de trabajo por contraparte

> **Qué es esta carpeta.** Una subcarpeta por cada equipo de la cátedra con el que
> `llm-service` (Tema 07) se integra, con dos archivos cortos por equipo:
>
> - **`contratos.md`** — lo que ya sabemos o acordamos con ese equipo: qué nos da, qué le
>   damos, con el endpoint/evento/JSON concreto cuando ya existe.
> - **`pendientes.md`** — lo que falta definir para ese equipo, sea trabajo **interno** nuestro
>   (ej. algo que todavía no implementamos) o un acuerdo **cruzado** que depende de conversar
>   con ellos.
>
> **Qué NO es.** No reemplaza a los documentos completos — sigue siendo necesario ir a ellos
> para el detalle largo (diagramas de secuencia, JSON completos, el registro de decisiones).
> Esta carpeta es la vista corta por destinatario: para no tener que rastrear ocho documentos
> antes de hablar con un equipo puntual. Todo el contenido de acá **viene de**:
>
> | Fuente | Qué aporta |
> |---|---|
> | [18 — Contratos inter-equipos](../18-contratos-inter-equipos.md) | El resumen por equipo (§4.1–4.8), los JSON de request/response y de eventos |
> | [17 — Mapa de integración](../17-mapa-de-integracion.md) | Diagramas de secuencia y la tabla de pendientes I-01 a I-16 |
> | [08 — Decisiones y pendientes](../08-decisiones-y-pendientes.md) | ADR relevantes y las preguntas abiertas al Product Owner (Parte B) |
> | [04 — Funciones de IA](../04-funciones-de-ia.md) | Qué construimos/no construimos nosotros por función, decisiones de producto pendientes |
> | [06 — Operación e ingeniería](../06-operacion-e-ingenieria.md) | Runbooks donde aparece la dependencia de otro equipo |
> | [11 — Glosario y metadata](../11-glosario-y-metadata.md) | Colisiones de vocabulario entre equipos |
> | [docs/entregas/](../entregas/) | Cartas y agendas ya escritas dirigidas a un equipo puntual |
> | [docs/contracts/](../contracts/) | Los YAML ejecutables (OpenAPI/AsyncAPI) — la fuente de verdad técnica |
>
> **Cómo se actualiza.** Cuando algo se acuerde (por ejemplo en la sesión de integración), se
> mueve de `pendientes.md` a `contratos.md` de ese equipo, y se actualiza también el documento
> de origen (18, 17, o el YAML correspondiente) — la sección "Checklist de cierre" de
> [`docs/entregas/sesion-integracion-agenda.md`](../entregas/sesion-integracion-agenda.md) ya
> cubre ese paso para los ítems que se deciden en sesión.

---

## Equipos

| Equipo | Carpeta | Fuente principal |
|---|---|---|
| Tema 02 — Cursos y Matrícula | [`tema-02-cursos-y-matricula/`](tema-02-cursos-y-matricula/) | 18 §4.1 |
| Tema 03 — Motor de Desafíos | [`tema-03-motor-de-desafios/`](tema-03-motor-de-desafios/) | 18 §4.2 |
| Tema 05 — Desafíos Prácticos | [`tema-05-desafios-practicos/`](tema-05-desafios-practicos/) | 18 §4.3, `docs/entregas/alcance-y-contrato-para-desafios-practicos.md` |
| Tema 11 — Chat | [`tema-11-chat/`](tema-11-chat/) | 18 §4.4 |
| Tema 12 — Backoffice / ADMIN | [`tema-12-backoffice-admin/`](tema-12-backoffice-admin/) | 18 §4.5 |
| Backend de negocio | [`backend-de-negocio/`](backend-de-negocio/) | 18 §4.6 |
| Front End — Angular | [`frontend-angular/`](frontend-angular/) | 18 §4.7 |
| Product Owner | [`product-owner/`](product-owner/) | 18 §4.8, 08 Parte B |

---

## Transversales (no son de un solo equipo)

Estos ítems afectan a más de un equipo a la vez. Para no repetirlos en cada `pendientes.md`,
viven acá una sola vez; cada equipo afectado los nombra y linkea a esta sección.

### Autenticación entre servicios (I-14)

Formato del token M2M, header y claims mínimos — todavía sin definir. Afecta a **todos** los
equipos que llaman a `llm-service` (Tema 02, 03, 05, 11, 12). Reglas ya fijas y no negociables:
nadie llega directo al servicio, todo pasa por el API Gateway, y el servicio nunca confía en
parámetros del cliente para identidad (`18` §0 y §7, ADR-001/ADR-015).

**Decide:** sesión de integración — ver [`docs/entregas/sesion-integracion-agenda.md`](../entregas/sesion-integracion-agenda.md) ítem 5.

### Resiliencia y manejo de errores (técnica común a todos los endpoints)

Cómo reaccionamos a una falla es el mismo mecanismo para todos los equipos — cada
`contratos.md` de equipo solo agrega el caso puntual ("qué le llega a este equipo cuando pasa
esto"), linkeando acá para la técnica.

- **Formato de error:** RFC 7807 Problem Details (respuesta `Problem` del
  [`llm-service-v1.openapi.yaml`](../contracts/llm-service-v1.openapi.yaml)), con
  `X-Request-Id` propagado en el header.
- **Escalera de degradación (RF-IA-27)**, diagrama completo en
  [06 §5](../06-operacion-e-ingenieria.md#5-plan-b-la-escalera-de-degradación-rf-ia-27): Nivel 1
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
[`docs/entregas/sesion-integracion-agenda.md`](../entregas/sesion-integracion-agenda.md). Los
`pendientes.md` de equipo linkean ahí en vez de repetir el timebox y el orden de la reunión.
