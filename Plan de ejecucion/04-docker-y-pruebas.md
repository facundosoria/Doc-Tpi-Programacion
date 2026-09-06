# Docker y pruebas

Esta guía separa el laboratorio histórico del entorno que se construirá para `llm-service`. No confundas una demo funcional con una integración de producto.

Para aprender desde cero cómo funcionan `.env`, la interpolación de Compose, las credenciales parametrizadas, Dockerfile, redes y perfiles, leer primero los [instructivos APB](08-instructivos-apb-docker-env.md).

## 1. Elegir el entorno correcto

| Necesito… | Usar | Estado |
|---|---|---|
| Explorar tutor y guardarraíles hoy | `demo/docker-compose.yml` | Disponible; usa código histórico. |
| Implementar S1–S10 | Compose del servicio definitivo creado en S1. | Pendiente de construcción. |
| Integrar por Gateway, Eureka y Kafka | Compose compartido de la plataforma. | Depende de equipos de infraestructura. |
| Probar una API LLM real | Perfil manual con clave local. | Nunca en CI. |

## 2. Laboratorio Docker disponible hoy

Desde la raíz del repositorio:

```bash
docker compose -f demo/docker-compose.yml up --build -d
docker compose -f demo/docker-compose.yml ps
```

Abrí `http://localhost:3000`. La demo expone su backend histórico en `http://localhost:8087`.

Para ver logs:

```bash
docker compose -f demo/docker-compose.yml logs -f backend
docker compose -f demo/docker-compose.yml logs -f frontend
```

Para detenerla sin borrar imágenes ni volúmenes:

```bash
docker compose -f demo/docker-compose.yml down
```

La demo utiliza modo mock por defecto. Si se define `GROQ_API_KEY`, puede consumir créditos del proveedor. No agregues claves al compose, al código, a commits, capturas ni logs. Si necesitás una prueba manual con modelo real, configurá la variable solo en tu terminal y detené la demo al terminar.

## 3. Entorno definitivo que debe existir desde S1

S1 crea una composición propia, por ejemplo `docker-compose.yml` dentro del futuro directorio del servicio definitivo. Debe levantar como mínimo:

| Servicio | Rol | Exposición |
|---|---|---|
| `llm-service` | API Spring Boot | Sin puerto público final; accesible por Gateway. |
| `worker` | Misma imagen, procesa trabajos internos. | Sin puerto público. |
| `postgres` | Datos, Flyway y tabla de trabajos. | Solo red interna o puerto local de desarrollo. |

Kafka, Eureka, Gateway, frontend y MinIO se agregan desde los composes/entornos compartidos cuando la fase los requiera. No crear sustitutos locales incompatibles si la plataforma ya provee el componente.

El compose definitivo debe incluir:

- Variables de entorno documentadas en `.env.example`, nunca secretos reales.
- Healthchecks de Postgres y servicios.
- `depends_on` condicionado a healthchecks cuando corresponda.
- Volumen explícito para datos de desarrollo.
- Perfiles o archivos de override para desarrollo, integración y pruebas.
- Un comando documentado para API, worker y migraciones.

El servicio no publica un puerto al host en integración final. Solo Gateway queda expuesto. En desarrollo aislado se puede habilitar un puerto local documentado para diagnóstico, nunca como sustituto de la prueba por Gateway.

## 4. Rutina de arranque del entorno definitivo

Estos comandos se usan cuando exista el compose de S1; reemplazá `<ruta-del-servicio>` por el directorio definido en esa entrega.

```bash
cd <ruta-del-servicio>
docker compose up --build -d
docker compose ps
docker compose logs -f llm-service
```

Antes de probar una historia:

1. Confirmá que Postgres está `healthy`.
2. Confirmá en logs que Flyway aplicó migraciones sin error.
3. Confirmá que API y worker usan el perfil esperado.
4. Verificá health/readiness sin llamar al proveedor LLM.
5. Para integración, verificá registro Eureka, ruteo Gateway y headers de correlación.

Para detener conservando datos:

```bash
docker compose down
```

Para borrar los datos locales de ese entorno, usá solo después de confirmar que no necesitás pruebas, golden sets o evidencia local:

```bash
docker compose down -v
```

`down -v` elimina volúmenes del compose, incluidos datos de Postgres. Nunca lo ejecutes contra un entorno compartido o un compose cuya lista de servicios no hayas revisado.

## 5. Pruebas locales

El esqueleto histórico incluye Maven Wrapper. Cuando el servicio definitivo exista, ejecutá sus comandos desde el directorio de ese servicio:

```bash
./mvnw test
./mvnw -Pcompleto test
```

La primera orden ejecuta pruebas normales sin proveedor real. El perfil completo debe usarse cuando el compose de integración esté levantado y los tests lo requieran.

Una prueba que llame a un LLM real debe estar separada, ejecutarse a mano, requerir una variable de entorno explícita y documentar su costo. No puede formar parte del pipeline normal. La política existente está en [TESTING.md](../codigo-ejemplo/ms-evaluacion-llm/TESTING.md).

| Tipo de prueba | Qué valida | Cuándo correrla |
|---|---|---|
| Unitaria | Regla de negocio, mapeos, guards y errores. | Durante el desarrollo. |
| Contrato | Request/response OpenAPI y eventos AsyncAPI. | Al modificar una interfaz. |
| Integración | Postgres, Flyway, Kafka, Gateway o servicios pares. | Antes de Review. |
| Seguridad | JWT, headers falsificados, autorización, jailbreak y anti-fuga. | En cada cambio que toque el borde. |
| Carga | Sesiones concurrentes y degradación. | S10, S13 y S19; antes si el cambio lo exige. |
| Modelo real | Calibración y calidad. | Solo manual, con presupuesto aprobado. |

## 6. Diagnóstico frecuente

| Síntoma | Primeras verificaciones |
|---|---|
| Un contenedor no inicia | `docker compose ps` y `docker compose logs <servicio>`. |
| API no conecta a Postgres | Healthcheck, variables `SPRING_DATASOURCE_*`, red Docker y migraciones Flyway. |
| Endpoint responde 404 | Ruta debe ser `/api/llm/**`; probar por Gateway y comprobar allowlist. |
| Recibo 401/403 | Diferenciar token inválido de autorización funcional; inspeccionar headers inyectados por Gateway. |
| Evento no se procesa | Tópico, grupo consumidor, `eventId`, outbox, correlación y logs de worker. |
| Un trabajo se repite | Revisar idempotencia, bloqueo de fila y estado del trabajo antes de reintentar. |
| Tutor no responde | Timeout, cuota, breaker, proveedor y estado `unavailable`; nunca bloquear la entrega. |

Cuando reportes un problema, compartí comando ejecutado, salida relevante sin secretos, servicio afectado, `X-Request-Id` y pasos para reproducirlo.
