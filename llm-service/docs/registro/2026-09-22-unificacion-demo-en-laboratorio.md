# Unificación de `demo/` en el laboratorio de integración (`lab/`) — 2026-09-22

## Decisión anterior

Existían dos variantes de laboratorio local: `demo/docker-compose.yml` (stack empaquetado,
standalone, sin hot reload) y `compose.yaml` + `compose.workbench.yaml` (workbench de desarrollo
diario, con hot reload de Angular), que reusaban por volumen los mismos archivos
`demo/gateway/nginx.conf` y `demo/courses/expectations.json`.

`demo/gateway/nginx.conf` apuntaba al upstream `llm-service:8080`, pero `compose.yaml` corre el
backend con `SERVER_PORT: "8086"` desde hace tiempo. El laboratorio documentado
(`compose.yaml` + `compose.workbench.yaml`) devolvía **502 Bad Gateway** en cualquier request a
`/api/llm/**`; solo `demo/docker-compose.yml` "andaba", porque no fijaba `SERVER_PORT` y la app
caía en un valor que coincidía por accidente con el hardcodeo de nginx.

## Motivo

El usuario pidió evaluar si convenía borrar `demo/` reemplazándolo por el workbench, dado que el
propio `demo/README.md` ya recomendaba el workbench para uso diario. Antes de unificar se detectó
el bug de puerto real (verificado levantando el stack con Docker y viendo el 502), y se decidió
mantener separados los perfiles de despliegue en la mesh Tailscale (`compose.mesh.yaml` /
`compose.server.yaml`) porque registran la misma instancia en Eureka si corren juntos — no
aplicaba la misma lógica de unificación.

## Regla vigente

- `demo/gateway/nginx.conf` y `demo/courses/expectations.json` se movieron (con `git mv`) a
  `llm-service/lab/gateway/nginx.conf` y `llm-service/lab/courses/expectations.json`. El upstream
  de nginx se corrigió a `llm-service:8086`.
- `demo/docker-compose.yml`, `demo/.env` y `demo/README.md` se eliminaron: el workbench cubre el
  mismo caso de uso con mejor DX (hot reload). La carpeta `demo/` ya no existe.
- `compose.workbench.yaml` apunta a `./lab/gateway/nginx.conf` y `./lab/courses/expectations.json`.
- [`docs/02-arquitectura-y-plataforma/07-laboratorio-integracion-local.md`](../02-arquitectura-y-plataforma/07-laboratorio-integracion-local.md)
  es ahora la única guía del laboratorio: agrega la tabla de escenarios de UUIDs mágicos (antes
  solo en el `README.md` de `demo/`, que se borró), corrige que Kafka **sí** es parte del
  laboratorio (el documento decía lo contrario) y documenta el paso de crear la red externa
  `tpi-platform`.
- Se creó `llm-service/scripts/up.sh` y `down.sh`: dispatcher de perfiles de `docker compose`
  (`local`, `workbench`, `debug`, `groq`, `mesh`, `server`, `shadow`) que valida variables de
  entorno, crea la red `tpi-platform` si falta, y bloquea combinaciones inválidas.

## Evidencia de prueba

Regresión completa contra el stack levantado con Docker, antes y después de mover `demo/` a
`lab/`: proxy al backend real (`401` en vez de `502`), y los 5 escenarios de UUID mágicos de
`courses-mock` (200 feliz, 200 con rol distinto, 404, 500, 200 con delay de 5 s) más el endpoint
sin auth — mismos resultados en ambas corridas.

## Documentos corregidos o agregados

**Movidos:** `demo/gateway/` → `lab/gateway/`, `demo/courses/` → `lab/courses/` (con historial de
git preservado).

**Eliminados:** `demo/docker-compose.yml`, `demo/.env`, `demo/README.md`.

**Agregados:** `scripts/up.sh`, `scripts/down.sh`.

**Actualizados:** `compose.workbench.yaml`,
[`docs/02-arquitectura-y-plataforma/07-laboratorio-integracion-local.md`](../02-arquitectura-y-plataforma/07-laboratorio-integracion-local.md).
