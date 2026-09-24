# Laboratorio de integración local

Este entorno prueba contratos reales de `llm-service` sin exigir todavía el Gateway, Courses ni el
frontend oficiales. Es la única variante mantenida: la variante empaquetada que existía en
`demo/` se unificó acá (ver [`registro/`](../registro/README.md) por la fecha de la fusión) —
`gateway-mock` y `courses-mock` comparten exactamente la misma configuración, en
[`lab/gateway/nginx.conf`](../../lab/gateway/nginx.conf) y
[`lab/courses/expectations.json`](../../lab/courses/expectations.json).

```text
Workbench Angular -- /api/** --> gateway-mock -- /api/llm/** --> llm-service --> PostgreSQL
                                             \-- /api/courses/** --> courses-mock
```

## Qué es real y qué se simula

- **Real:** controllers, autorización funcional, casos de uso, JDBC, Flyway, PostgreSQL, adaptadores
  HTTP de `llm-service`, contratos OpenAPI y el broker Kafka local (`kafka-local`, ver
  `compose.yaml`) — `llm-service` depende de él para arrancar sano, aunque no sea el bus real de la
  plataforma (`event-bus`).
- **Simulado por red:** autenticación/borde del Gateway y `courses-service` (MockServer).
- **Fuera del laboratorio:** frontend oficial, IdP, Eureka y proveedores LLM con credenciales de
  producción.

Antes de la primera vez, crear la red externa que usa `compose.yaml` para integrarse con la
plataforma (solo hace falta una vez por máquina):

```bash
docker network create tpi-platform
```

Desde `llm-service/`:

```bash
docker compose -f compose.yaml -f compose.workbench.yaml up --build
```

Abrir `http://localhost:4200/docente`. El navegador sólo usa rutas relativas; nunca conoce hosts
internos, tokens M2M ni headers de identidad. Para detenerlo:

```bash
docker compose -f compose.yaml -f compose.workbench.yaml down
```

El Gateway mock descarta la identidad enviada por el navegador, agrega identidad delegada,
`traceparent` y `X-Request-Id`, y conserva el path completo. `llm-service` consulta membresías
mediante su cliente HTTP real usando un Bearer M2M; los casos de Cursos están en
[`lab/courses/expectations.json`](../../lab/courses/expectations.json).

El Workbench no es el frontend final. Sirve como consumidor plug-and-play de referencia para el
equipo Angular: las rutas, esquemas, paginación, idempotencia, `If-Match`, SSE y Problem Details se
obtienen exclusivamente de [`../../contracts/llm-service.openapi.yaml`](../contracts/llm-service.openapi.yaml).

## Escenarios de prueba (UUIDs mágicos)

Para probar cómo reacciona la interfaz y el backend a distintas respuestas del servicio de cursos
(fallas, permisos insuficientes o demoras), `courses-mock` responde según el UUID del curso.
Navegá a `http://localhost:4200/courses/<UUID>` con el UUID correspondiente:

| UUID del curso | Rol simulado | Escenario | Código HTTP del mock |
| :--- | :--- | :--- | :--- |
| `22222222-2222-2222-2222-222222222222` | **TEACHER** | Flujo exitoso (camino feliz) | `200 OK` |
| `33333333-3333-3333-3333-333333333333` | **STUDENT** | Acceso denegado (solo docentes) | `200 OK` del mock, `403` lo decide `llm-service` por rol |
| `44444444-4444-4444-4444-444444444444` | *N/A* | Curso inexistente | `404 Not Found` |
| `55555555-5555-5555-5555-555555555555` | *N/A* | Falla interna en el servicio de cursos | `500 Internal Server Error` |
| `66666666-6666-6666-6666-666666666666` | *N/A* | Latencia extrema (demora 5 s) | `200 OK` con `delay` de 5 s |

> **Nota:** la identidad de desarrollo vive en el Gateway mock, no en el workbench ni en
> `llm-service`. Cambiarla debe hacerse como un escenario explícito de gateway, nunca enviando
> headers desde el navegador.
