# Tema 11 — Chat — contratos

> Fuente completa: [18 §4.4](../../18-contratos-inter-equipos.md#44-tema-11--chat),
> [17 §4](../../17-mapa-de-integracion.md#4-camino-sincrónico-b--el-moderador) (diagrama completo),
> [04-funciones-de-ia.md](../../04-funciones-de-ia.md) líneas 1658-1679 ("qué construimos y qué no").

## Qué nos llama

- `POST /ai/moderador` — siempre sincrónico, siempre antes de entregar el mensaje al hilo
  (presupuesto **< 300 ms**). **No está en el contrato v1 vigente** — vive solo en el borrador
  [`llm-service-v1-moderacion-borrador.yaml`](../../contracts/llm-service-v1-moderacion-borrador.yaml)
  (EP-08, sin implementar).

### Cuerpo de la solicitud 🟡 (borrador — sin implementar, pero con schema detallado)

```json
{
  "contexto": {
    "curso_cohorte_id": "b1e2c3d4-0003-4a00-8000-000000000003",
    "usuario_ref": "ref-opaca-alumno-001"
  },
  "payload": {
    "mensaje": "che alguien tiene el codigo de la practica 3??",
    "canal_ref": "canal-comision-2b",
    "autor_ref": "ref-opaca-alumno-001",
    "desafio_activo": true
  },
  "modo": "sync",
  "idempotency_key": "idem-mod-0001"
}
```

`desafio_activo` lo tiene que mandar Tema 11 — nosotros no podemos deducir si el emisor tiene un
desafío en curso, y la heurística de integridad académica (bloque de código pegado en el chat)
lo necesita.

## Qué nos da

- Define el contrato de eventos del bus **para toda la plataforma** — su decisión condiciona a
  cinco equipos. Ver [`pendientes.md`](pendientes.md) sobre por qué esto es urgente por
  secuencia.

## Qué le damos

✅ **Forma única y canónica, fijada el 2026-09-12** (ver
[`pendientes.md`](pendientes.md) para el porqué): la de
[`llm-service-v1-moderacion-borrador.yaml`](../../contracts/llm-service-v1-moderacion-borrador.yaml)
(`RespuestaModeracion`) — reemplaza el bosquejo más viejo que tenía doc 18 §4.4.

```json
{
  "resultado": {
    "categorias": ["integridad_academica"],
    "severidad": "media",
    "confianza": 0.94,
    "origen": "heuristica",
    "version_lista": "2025-05-v3"
  },
  "trace_id": "6d1f7a10-0000-4000-8000-000000000010",
  "metadata": {
    "model_id": null,
    "model_version": null,
    "latencia_ms": 45
  }
}
```

`categorias` es un **array de enum** (`ofensivo_discriminatorio`, `acoso`,
`sexual_violencia`, `spam_no_academico`, `integridad_academica`, `elusion_solo_texto` — las
seis de RF-CHT-10), no un objeto de booleanos, y puede traer más de una a la vez porque los
detectores clásicos corren todos y fusionan veredictos. **No hay campo `veredicto`**: el
bloqueo se infiere de `severidad` (`media`/`alta` bloquean, `baja` no).

**Tema 11 no entrega el mensaje al hilo hasta recibir una respuesta con `severidad: baja`.**

## Qué pasa si esto falla

Técnica común en
[transversales del README](../README.md#resiliencia-y-manejo-de-errores-técnica-común-a-todos-los-endpoints).
El moderador tiene su propio par de errores tipados, ya en el schema del borrador — **no son
propuesta, son schema real** (`ErrorCuota` y `ErrorProveedor`):

**Cuota agotada:**

```json
{ "error": "cuota_agotada", "limite": 15, "reinicia_en": "2026-09-13T00:00:00Z" }
```

**Proveedor (clasificador externo) no disponible — dos variantes de degradación:**

```json
{ "error": "proveedor_no_disponible", "degradacion": "prefiltro_solamente" }
```

```json
{ "error": "proveedor_no_disponible", "degradacion": "diferido" }
```

- **`prefiltro_solamente`** (Circuit Breaker abierto): la capa clásica sigue decidiendo sola —
  es la red que hace tolerable el fail-open. Sin esta capa, fail-open significaría *sin ninguna
  moderación*.
- **`diferido`**: el mensaje se entrega marcado y se re-modera cuando el clasificador vuelve;
  si en la re-moderación resulta `media`/`alta`, se retira y se genera el incidente
  retroactivamente.

**Decisión de producto ya resuelta (P-02, recomendación fail-open con red):** ante cualquier
falla, **el mensaje se entrega** — el chat social no es producción académica, y RF-IA-27
prohíbe bloquear al alumno por una caída externa. Fail-closed (detener el chat) quedó
descartado.

## Deslinde de alcance ya acordado

**"El chat es del Tema 11. Nosotros aportamos una función, no una funcionalidad"** (doc 04).
De ellos: el chat completo (canales, hilos, citas, entrega), la pantalla del dashboard de
incidentes, el envío de notificaciones, la ejecución de la purga al archivar el curso. De
nosotros: la capa clásica + clasificador, el registro de incidentes, el evento de severidad
alta, la marca de retención.
