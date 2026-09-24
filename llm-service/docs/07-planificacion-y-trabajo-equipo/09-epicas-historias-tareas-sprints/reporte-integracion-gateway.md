# Reporte de Integración al API Gateway y Eureka — `llm-service` (Topic 01)

## Tabla de integración `llm-service`

| Item | Estado | Detalle |
|---|:---:|---|
| Nombre único (`serviceId`, path `/api/<seg>`) | ✅ | `llm-service`, prefijo `/api/llm/**` |
| Eureka (register, no fetch, healthcheck) | ✅ | `register-with-eureka: true`, `fetch-registry: false`, `healthcheck: true`, `prefer-ip-address: true` |
| Puertos sin colisión + `expose` sin `ports` | ✅ | App `8086`, Management `8087` (DEC-28). `expose: ["8086", "8087"]` sin publicar puertos al host |
| Rutas por `app.api.*` (sin literales) — bloqueante | ✅ | 20 controladores migrados a `${app.api.private-path}`; 0 literales detectados en grep |
| Filtro identidad (no valida JWT, lee X-*) | ✅ | `GatewayIdentityFilter` procesa `X-Principal-Type`, `X-User-*` y `X-Service-*` |
| `@PreAuthorize` capa 1 + regla negocio capa 2 | ✅ | `SecurityConfig` stateless con RFC 7807 problem+json; autorización fina en capas |
| Trazabilidad (`traceId`/`requestId` en logs) | ✅ | `RequestCorrelationFilter` propaga W3C traceparent y devuelve `X-Request-Id` con MDC format |
| Micro-a-micro hacia Gateway (`aud`) | ✅ | `HttpClientConfig` inyecta `RestClient` con base URL `http://api-gateway:8080` y propaga trazas |
| Sincronización JWKS (canario de firma) | ✅ | `JwksRefreshJob` y endpoint `GET /api/llm/jwks-estado` operativos |
| Alta de scopes | ✅ | Scopes registrados: `llm.golden-set.manage`, `llm.rubric-template.manage`, `llm.tutor.interact`, `llm.rag.query` |

---

## Datos para el equipo Gateway (Identity & Users)

- **`serviceId` exacto:** `llm-service`
- **Segmento asignado:** `llm` (ruta base `/api/llm/**`)
- **Ruta pública reservada:** `/api/llm/public/**`
- **Puertos internos:** `8086` (aplicación) / `8087` (management)
- **Red Docker:** `tpi-platform`
- **`clientId`:** `llm-service`
- **Onboarding propio:** No (sin prefijo exempt requerido)
- **Scopes que expone el microservicio:**
  - `llm.golden-set.manage`: Gestión de rúbricas y casos de prueba del Golden Set (Admin / Docente)
  - `llm.rubric-template.manage`: Gestión de plantillas globales institucionales (Admin)
  - `llm.tutor.interact`: Interacción con el tutor inteligente de práctica (Service `practice-service`)
  - `llm.rag.query`: Consultas semánticas con recuperación aumentada (Service `practice-service`)
- **Acción requerida en Gateway:**
  - Agregar `llm-service` a `GATEWAY_ALLOWLIST`
  - Recrear contenedor: `docker compose up -d api-gateway`