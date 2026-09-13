# Tema 11 — Chat — pendientes

> Fuente completa: [17 §4](../../17-mapa-de-integracion.md#4-camino-sincrónico-b--el-moderador)
> y [17 §8](../../17-mapa-de-integracion.md#8-lo-que-estos-diagramas-dejaron-a-la-vista) (I-01,
> I-05, I-08), [18 §3](../../18-contratos-inter-equipos.md#3-eventos-que-consumimos).

## 🔴 Cruzado — I-05: qué enum viaja en `estado`

Compartido con Tema 03 — ver [`docs/entregas/sesion-integracion-agenda.md`](../../entregas/sesion-integracion-agenda.md) ítem 2.

## 🔴 Cruzado — I-08: esquema de la tabla `mensaje`

Escrito de tres formas incompatibles en distintos documentos. Es lo único de toda la lista que
se pierde para siempre si se posterga — un dato no capturado hoy no se recupera después.
P5 lo trae preparado a la sesión (asignado esta semana). Lo que nosotros necesitamos que quede
adentro: `trace_id`, timestamp, autor, y espacio para el veredicto del moderador — sin que el
moderador termine dueño de la tabla.

**Decide:** sesión de integración — ver [`docs/entregas/sesion-integracion-agenda.md`](../../entregas/sesion-integracion-agenda.md) ítem 4.

## 🟡 Cruzado — I-01: cuándo se invoca el clasificador

¿Cuando la capa clásica "no decidió", o cuando "no llegó a media o alta"? Cambia los 300 ms de
presupuesto, el ahorro de −70% de invocaciones al clasificador, y el tope diario del free tier.
Es "nuestro" en el sentido de que lo resolvemos leyendo el código y eligiendo — pero conviene
que Tema 11 lo conozca porque cambia la latencia observable del chat.

## ✅ Resuelto (2026-09-12) — dos versiones del contrato del moderador

Al armar los ejemplos de `contratos.md` apareció que **18 §4.4** y el borrador
[`llm-service-v1-moderacion-borrador.yaml`](../../contracts/llm-service-v1-moderacion-borrador.yaml)
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
[`mañanav2.html`](../../presentaciones/mañanav2.html) y
[`presentacion-proyector.html`](../../presentaciones/presentacion-proyector.html) mostraban el
JSON viejo (`veredicto` + `categorias` como booleanos) y ya se corrigieron a la forma canónica.
`presentacion-integracion-servicios.html` usa la palabra "veredicto" en prosa suelta, sin
afirmar un campo — no hacía falta tocarla.

## 🟡 Interno — EP-08 (moderación) sin código todavía

El contrato está escrito ([`llm-service-v1-moderacion-borrador.yaml`](../../contracts/llm-service-v1-moderacion-borrador.yaml))
pero **ningún controller de `llm-service` lo implementa hoy**. Es contexto para la
conversación, no algo que dependa de Tema 11 resolver.

## Colisión de vocabulario

Ver [transversales del README](../README.md#glosario-de-colisiones-de-vocabulario).
