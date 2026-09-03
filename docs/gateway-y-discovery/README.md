# Guía Maestra: API Gateway y Service Discovery

> **Tema 01: Componente de Borde · UTN FRC**  
> Documentación técnica exhaustiva sobre el funcionamiento de la puerta única de entrada, registro dinámico y contratos de integración para el Trabajo Práctico Integrador.

---

## 📑 Estructura de Documentación Modular

Esta suite de documentación desglosa con exactitud matemática y técnica todos los aspectos arquitectónicos del borde:

| Archivo | Título | Temas Clave |
|---|---|---|
| [**`01-principios-y-reglas-de-red.md`**](file:///d:/Demo%20Tpi/docs/gateway-y-discovery/01-principios-y-reglas-de-red.md) | Principios de Arquitectura y Red | Puerta única, aislamiento de red (Docker/K8s), qué hace y qué NO hace el Gateway, los 4 escenarios de tráfico. |
| [**`02-service-discovery-eureka.md`**](file:///d:/Demo%20Tpi/docs/gateway-y-discovery/02-service-discovery-eureka.md) | Service Discovery con Eureka | Ciclo de vida (alta, heartbeat 30s, expiración 90s), sincronización por caché, `lb://` y configuración mínima de Spring Boot. |
| [**`03-convenciones-nombres-y-ruteo.md`**](file:///d:/Demo%20Tpi/docs/gateway-y-discovery/03-convenciones-nombres-y-ruteo.md) | Convenciones de Nombres y Ruteo | Contrato en 3 niveles (`tpi-*`, `*-service`, `/api/*/**`), ruteo dinámico gobernado, base paths en properties y controladores. |
| [**`04-seguridad-y-pipeline-de-filtros.md`**](file:///d:/Demo%20Tpi/docs/gateway-y-discovery/04-seguridad-y-pipeline-de-filtros.md) | Seguridad y Pipeline de Filtros | Orden de filtros WebFlux, validación JWT (RS256/JWKS), prevención de header spoofing, inyección de `X-User-Id` / `X-Principal-Type`. |
| [**`05-comunicacion-micro-a-micro.md`**](file:///d:/Demo%20Tpi/docs/gateway-y-discovery/05-comunicacion-micro-a-micro.md) | Comunicación Micro a Micro (M2M) | Por qué toda llamada pasa por el Gateway, tokens técnicos (Client Credentials, exp=60s), identidad delegada (`on_behalf_of`). |
| [**`06-resiliencia-observabilidad-y-operacion.md`**](file:///d:/Demo%20Tpi/docs/gateway-y-discovery/06-resiliencia-observabilidad-y-operacion.md) | Resiliencia y Observabilidad | Cadena en 6 capas: Rate Limit, Bulkhead, Timeouts, Retries idempotentes, Circuit Breakers Resilience4j, Fallback RFC 7807 y Actuator. |
| [**`07-pruebas-obligatorias-y-dod.md`**](file:///d:/Demo%20Tpi/docs/gateway-y-discovery/07-pruebas-obligatorias-y-dod.md) | Pruebas Obligatorias y DoD | Las 4 familias de tests obligatorios (Ruteo, Seguridad, Discovery, Resiliencia) y lista de control de Definition of Done. |

---

## 🚀 Cheat Sheet de Integración Rápida para Equipos

Si estás desarrollando o integrando un nuevo microservicio al proyecto, sigue esta lista de convenciones obligatorias:

### 1. Convención de Nombres
* **Repositorio Git:** `tpi-{nombre}` (ej. `tpi-evaluations`).
* **Service Name (Eureka):** `{nombre}-service` (ej. `evaluations-service`).
* **Prefijo Público de URL:** `/api/{nombre}/**` (ej. `/api/evaluations/**`).

### 2. Configuración Mínima (`application.yml`)
```yaml
spring:
  application:
    name: evaluations-service

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: false
    healthcheck:
      enabled: true

app:
  api:
    public-path: /api/evaluations/public
    private-path: /api/evaluations
```

### 3. Invocación Hacia Otros Microservicios
* ❌ **NUNCA** invoques a otro microservicio directamente por su IP o nombre de host privado (`http://users-service:8080`).
*  **SIEMPRE** llama a través del API Gateway (`http://api-gateway:8080/api/users/...`).
*  Obtén previamente un **Token Técnico de Servicio** (corta duración, `type=service`, con el `aud` y `scope` correspondiente).

### 4. Recepción de Identidad en los Controladores
El Gateway valida la identidad en el borde y garantiza los siguientes headers de confianza:
* `@RequestHeader("X-Principal-Type") String principalType` (`user` o `service`).
* `@RequestHeader(value = "X-User-Id", required = false) String userId`
* `@RequestHeader(value = "X-User-Roles", required = false) String userRoles`
* `@RequestHeader(value = "X-Service-Id", required = false) String serviceId`
* `@RequestHeader("X-Request-Id") String requestId`
