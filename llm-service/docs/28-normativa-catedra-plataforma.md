# 28 — Normativa de la cátedra para la construcción de la Plataforma

> Estándares mínimos de la cátedra para datos, backend, frontend, flujo de trabajo y
> seguridad, publicados en la Wiki de Taiga (últ. edición de Exequiel Santoro,
> 26 Ago 2026). Incorporado el 2026-09-05 a pedido del equipo.
>
> **Precedencia.** Según [00 · Fuentes de verdad](00-fuentes-de-verdad-y-convenciones.md),
> mandan el PRD e `idea.pptx.pdf`. Esta normativa es una fuente de nivel 3 (aplicación
> de la cátedra). Donde su texto choca con una decisión ya fundamentada de Tema 07, la
> divergencia queda listada en la §6 y **se resuelve en la sesión de integración**, no
> editando esta página.

## 1. Alcance

Aplica a todo el código, scripts, migraciones de base de datos y documentación de los
equipos del curso. Cubre MySQL, Java 21 + Spring Boot, Angular 21 y Git.

## 2. Bases de datos (MySQL)

### 2.1 Motor
- MySQL 8.x.

### 2.2 Nomenclatura y tipos
- Tablas y columnas en **inglés**, `snake_case` (ej.: `patient_address`).
- PK: columna `id BIGINT UNSIGNED AUTO_INCREMENT`.
- FK: `<tabla>_id` (ej.: `user_id`).
- `DATE`: sufijo `_date`.
- `DATETIME`: sufijo `_datetime`.

### 2.3 Campos obligatorios en TODAS las tablas
| Campo | Tipo |
|---|---|
| `created_datetime` | `DATETIME` |
| `created_user` | `BIGINT` |
| `last_updated_datetime` | `DATETIME` |
| `last_updated_user` | `BIGINT` |
| `is_active` | `TINYINT` (0/1) — baja lógica, no se elimina físicamente |

### 2.4 Auditoría
- Tabla espejo `<table>_audit` con los mismos campos + `version BIGINT AUTO_INCREMENT`.
- Versiones insertadas por triggers o por la capa de aplicación.

### 2.5 Índices y claves
- Índices por FK y por campos de búsqueda (`dni`, `protocol_number`, etc.).

### 2.6 Integridad y referencia
- FK con `ON UPDATE RESTRICT` y `ON DELETE RESTRICT`, salvo casos explícitos.
- No almacenar derivados calculables (3FN).

## 3. Backend (Java 21 + Spring Boot)

### 3.1 Estilo y estructura
- Código en inglés. Paquetes por dominio (`patients`, `orders`, `results`).
- Capas: `api` (controllers) · `application` (services / casos de uso) · `domain`
  (entidades) · `infrastructure` (persistencia / adapters).

### 3.2 Fechas y zonas
- No usar `String` para fechas. Usar `LocalDate`, `LocalDateTime` y `ZonedDateTime`.

### 3.3 Validaciones y errores
- Bean Validation (`@NotNull`, `@Email`, …).
- Manejo unificado con `@ControllerAdvice` y códigos HTTP consistentes.

### 3.4 Persistencia
- Spring Data JPA.
- `@Version` para control optimista donde corresponda.

### 3.5 Seguridad
- Spring Security · JWT para APIs · roles por endpoint · logs de auditoría en acciones críticas.

### 3.6 Documentación
- OpenAPI / Swagger habilitado en entorno `dev`.

### 3.7 Compatibilidad entre microservicios
- Contratos en JSON / OpenAPI.
- No acoplarse a nombres de clases de otro servicio.

## 4. Frontend (Angular 21)

### 4.1 Áreas de trabajo por grupo
- Trabajar exclusivamente dentro del área del propio grupo.
- No modificar código de otros grupos, el layout ni `app.routes.ts`, salvo cambios
  transversales acordados.
- Rutas hijas en el `.routes.ts` del grupo. Evitar dependencias cruzadas entre módulos.

### 4.2 Flujo Git/PR
| Rama | Origen | Destino | Uso |
|---|---|---|---|
| `main` | — | — | Producción. Protegida, sin push directo. Solo PR desde `develop` (releases) o `hotfix/*`. |
| `develop` | `main` | `main` | Integración. Recibe PR desde `feature/*` o `fix/*`. Protegida. |
| `feature/*` | `develop` | `develop` | Nuevas funcionalidades. |
| `fix/*` | `develop` | `develop` | Correcciones menores durante el desarrollo. |
| `hotfix/*` | `main` | `main` (+ merge de `main` a `develop`) | Arreglos urgentes en producción. |

**Protección de `main`:** PR obligatorio, checks de GitHub Actions en verde, rama
actualizada con la última versión, sin push directo, solo PR desde `develop` o `hotfix/*`.
**Protección de `develop`:** PR obligatorio, checks (tests/lint/build) posibles, sin push directo.

### 4.3 Dependencias
- No modificar `package.json` ni `package-lock.json` en los PR (bloqueado por CI).
- Nuevas librerías o actualizaciones las gestiona el maintainer.

### 4.4 Estándares de código
- [Guía de estilo de Angular](https://angular.io/guide/styleguide).
- Componentes standalone y rutas lazy por grupo.
- Nombres descriptivos; una responsabilidad por archivo.
- Evitar `console.log`; usar `console.warn` / `console.error` solo cuando sea relevante.

### 4.5 Validaciones de CI en cada PR
- Lint (ESLint) · Build Angular (producción).
- Falla si hay errores de ESLint, si la duplicación de código supera **3%**, o si se
  modifican `package.json` / `package-lock.json`.

```bash
npm run lint
npm run build
```

### 4.6 Seguridad — conceptos y referencias
| Tema | Referencia |
|---|---|
| HTTPS / TLS | https://wiki.mozilla.org/Security/Server_Side_TLS |
| Reverse proxy / API proxy (`/api`, CORS, rate limiting, logs) | https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/ |
| CORS | https://developer.mozilla.org/docs/Web/HTTP/CORS |
| CSP | https://developer.mozilla.org/docs/Web/HTTP/CSP |
| Security headers | https://owasp.org/www-project-secure-headers/ |
| Rate limiting | https://www.npmjs.com/package/express-rate-limit |
| Gestión de secretos | https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html |
| Cookies de sesión (`HttpOnly`, `Secure`, `SameSite=Strict`) | https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html |
| Source maps (deshabilitar en producción) | https://angular.dev/tools/cli/build |
| Autorización / validación backend | https://owasp.org/www-project-application-security-verification-standard/ |

## 5. Objetivo de la normativa

Normativa simple y clara para estudiantes que construyen una Plataforma, con estándares
mínimos de datos, backend, frontend, flujo de trabajo y seguridad.

## 6. Divergencias con las decisiones vigentes de Tema 07

Estos puntos de la normativa **no coinciden** con decisiones ya fundamentadas del
servicio LLM. No se cambian los documentos de Tema 07 por esta página: cada fila se
lleva a la sesión de integración para confirmar excepción o alinear.

| Tema | Normativa (§) | Tema 07 hoy | Dónde se decidió |
|---|---|---|---|
| **Motor de BD** | MySQL 8.x (§2.1) | **PostgreSQL + pgvector** — pgvector guarda los embeddings del RAG en la misma base que su metadata; MySQL no tiene equivalente nativo. | [12 §2](12-almacenamiento-e-ingesta.md) · [ADR-004](08-decisiones-y-pendientes.md) · [26](26-herramientas-y-librerias.md) |
| **Idioma de tablas/columnas** | Inglés `snake_case` (§2.2) | Nombres en español (`mensaje`, `evaluacion`, `golden_set`, `contenido`, `orden`…), marcados como propuesta E-15. | [11 Parte B](11-glosario-y-metadata.md) · [08 Parte C](08-decisiones-y-pendientes.md) |
| **Tipo de PK** | `id BIGINT UNSIGNED AUTO_INCREMENT` (§2.2) | `uuid` en las tablas de conversación y evaluación. | [11 Parte B](11-glosario-y-metadata.md) |
| **Baja lógica `is_active`** | Obligatoria en todas las tablas (§2.3) | Producción académica **inmutable**: los eventos no se editan ni se borran; el override se agrega y nunca pisa el original (RF-IA-18, RF-NFR-01). | [11 "Reglas del esquema"](11-glosario-y-metadata.md) · [00 §7](00-fuentes-de-verdad-y-convenciones.md) |
| **Auditoría por tabla espejo** | `<table>_audit` + `version` por cada tabla (§2.4) | Tablas append-only + `@Version` optimista donde aplica; auditoría LLMOps en `llamadas_llm`. | [24 §Pruebas de infraestructura](24-convenciones-cobertura.md) · [26](26-herramientas-y-librerias.md) |
| **Campos `created_user` / `last_updated_user`** | `BIGINT` (§2.3) | La identidad llega por headers del Gateway (`X-User-Id`) y el modelo de usuario es de otro servicio; falta acordar el tipo. | [00 §3](00-fuentes-de-verdad-y-convenciones.md) |
| **Ramas Git** | `feature/*` y `fix/*` planos; `main` recibe de `develop` (§4.2) | GitFlow con `release/*` y naming estricto `feature/sNN/llm-sNN-hNN-<slug>`; `main` solo recibe de `release/*` o `hotfix/*`. Sin rama `fix/*`. | [GITFLOW.md](GITFLOW.md) |
| **CI de frontend** | ESLint + build; falla si duplicación > 3% (§4.5) | Vitest con cobertura **≥ 95%** de sentencias; formateo con Prettier. Sin gate de duplicación ni ESLint configurado aún. | [24](24-convenciones-cobertura.md) · `llm-workbench/package.json` |
| **`package.json` bloqueado en PR** | Sí, lo gestiona el maintainer (§4.3) | No hay bloqueo; las dependencias del workbench se agregan en el PR de la historia. | `llm-workbench/` |

Coinciden sin fricción: Java 21 + Spring Boot, capas `api`/`application`/`domain`/`infrastructure`,
Bean Validation + `@ControllerAdvice`, Spring Data JPA + `@Version`, Spring Security + JWT + roles,
OpenAPI/Swagger en dev, contratos JSON/OpenAPI sin acoplar clases, Angular 21 standalone + rutas
lazy, y las referencias de seguridad del §4.6.
