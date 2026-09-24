# Tema 11 — Chat — pendientes

> Este documento es la fuente completa de lo pendiente con Tema 11. Antecedentes:
> 17 §8 (I-01,
> I-05, I-08), 18 §3,
> [08 B-3](../../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md).

## 🔴 Cruzado — B-3: los campos que Tema 11 tiene que incluir en el contrato de eventos

Tema 11 define el contrato de eventos **para toda la plataforma** y su decisión condiciona a
cinco equipos — es urgente por secuencia, no por importancia: una vez cerrado, pedir un campo
nuevo es renegociar con todos.

**Lo que necesitamos que incluyan:** `curso_cohorte_id`, `intento_id`, `alumno_id`,
`rubric_version`, `model_id`, `model_version`, `score_agregado`, `confianza`, `estado` y
`trace_id`. Ver [08 B-3](../../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md#-b-3--tema-11-los-campos-del-contrato-de-eventos).

## 🔴 Cruzado — I-05: qué enum viaja en `estado`

Compartido con Tema 03 — ver [`docs/entregas/sesion-integracion-agenda.md`](../../../01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md) ítem 2.

## 🔴 Cruzado — I-08: esquema de la tabla `mensaje`

Escrito de tres formas incompatibles en distintos documentos. Es lo único de toda la lista que
se pierde para siempre si se posterga — un dato no capturado hoy no se recupera después.
P5 lo trae preparado a la sesión (asignado esta semana). Lo que nosotros necesitamos que quede
adentro: `trace_id`, timestamp, autor, y espacio para el veredicto del moderador — sin que el
moderador termine dueño de la tabla.

**Decide:** sesión de integración — ver [`docs/entregas/sesion-integracion-agenda.md`](../../../01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md) ítem 4.

## 🟡 Cruzado — I-01: cuándo se invoca el clasificador

¿Cuando la capa clásica "no decidió", o cuando "no llegó a media o alta"? Cambia los 300 ms de
presupuesto, el ahorro de −70% de invocaciones al clasificador, y el tope diario del free tier.
Es "nuestro" en el sentido de que lo resolvemos leyendo el código y eligiendo — pero conviene
que Tema 11 lo conozca porque cambia la latencia observable del chat.

## ✅ Resuelto (2026-09-12) — dos versiones del contrato del moderador

Al armar los ejemplos de `contratos.md` apareció que **18 §4.4** y el borrador
[`llm-service-v1-moderacion-borrador.yaml`](../../../contracts/llm-service-v1-moderacion.openapi.yaml)
describían formas distintas del mismo resultado: doc 18 usaba `veredicto` + `categorias` como
objeto de booleanos (6 claves inventadas); el borrador usa `resultado.categorias` como array
del enum real de RF-CHT-10, sin `veredicto` explícito (se infiere de `severidad`).

**Se fijó el borrador como forma única y canónica** — es la más nueva, la más detallada, y la
única que usa las seis categorías exactas de la cátedra en vez de nombres inventados. Doc 18
§4.4 y [`contratos.md`](contratos.md) de esta carpeta ya se actualizaron para coincidir. Como
EP-08 todavía no tiene código, no hubo costo de migración — era solo reconciliar el papel antes
de que Tema 11 maquete el panel de moderación (pantalla 5 de
[`frontend-angular/pendientes.md`](../frontend-angular/pendientes.md)).

**Presentaciones actualizadas (2026-09-12):**
[`mañanav2.html`](../../../08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/presentaciones/README.md) y
[`presentacion-proyector.html`](../../../08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/presentaciones/presentacion-proyector.html) mostraban el
JSON viejo (`veredicto` + `categorias` como booleanos) y ya se corrigieron a la forma canónica.
`presentacion-integracion-servicios.html` usa la palabra "veredicto" en prosa suelta, sin
afirmar un campo — no hacía falta tocarla.

## 🟡 EP-08 (moderación) implementada — pendientes para Tema 11 (2026-09-19)

`llm-service` ya implementa el contrato v1.1.0 (`POST /moderation/v1/decisions`, reemplaza al
borrador `POST /ai/moderador`, que quedó obsoleto). Lo que **Tema 11 tiene que cerrar** está en
[`moderacion-pendientes-chat-service.md`](../../../contracts/moderacion-pendientes-chat-service.md):

- **C1:** enviar `sender_id` real (obligatorio, breaking; hoy probamos con un valor mockeado).
- **C2/C3/C4:** manejo en UI de `PENDING`/`PENDING_REVIEW`, timeout propio y política de reintentos (siempre con el mismo `message_id`).
- **C5:** si consumen el evento Kafka `MESSAGE_UNBLOCKED` para publicar tras una reversión docente.
- **C6:** si necesitan retirar mensajes ya publicados (`DELETE /moderation/v1/decisions/{message_id}`; un `ALLOW` responde `409`).

## Colisión de vocabulario

Ver [transversales del README](../README.md#glosario-de-colisiones-de-vocabulario).
