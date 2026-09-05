# 00 — Fuentes de verdad y convenciones obligatorias

> Referencia normativa para `llm-service` y para cualquier documento posterior del Tema 07.

## 1. Orden de precedencia

| Prioridad | Fuente | Decide |
|---|---|---|
| 1 | `PRD-Plataforma-Gamificada-TP.pdf` | Alcance funcional, fases, reglas académicas y criterios de release. |
| 2 | `idea.pptx.pdf` | Red, Gateway, Eureka, identidad, rutas, observabilidad y pruebas de integración. |
| 3 | Este documento, contratos v1 y ADRs | Aplicación concreta de esas dos fuentes a Tema 07. |
| 4 | Resto de `docs/` | Explicación y detalle; no puede contradecir las fuentes anteriores. |

`docs/importado/` es material histórico y no participa de esta jerarquía. Tampoco se modifica.

## 2. Identidad canónica de Tema 07

| Ámbito | Valor obligatorio |
|---|---|
| Repositorio | `tpi-llm` |
| Eureka / `spring.application.name` | `llm-service` |
| Prefijo privado | `/api/llm/**` |
| Prefijo público reservado | `/api/llm/public/**` |
| Audience de token técnico | `llm-service` |

No existe un endpoint público de LLM en el MVP. El prefijo público queda reservado para que no se invente otra convención. La forma `/api/llm/public/**` resuelve la discrepancia de ejemplos en `idea.pptx.pdf` y preserva la regla de derivación `{nombre}-service -> /api/{nombre}/**`.

## 3. Red, ruteo e identidad

- Internet y frontend llegan únicamente al API Gateway. Los puertos de `llm-service` son privados.
- Las llamadas síncronas entre servicios vuelven al Gateway; no hay host, IP ni llamada HTTP directa entre micros.
- Eureka descubre instancias, pero el Gateway expone solo servicios admitidos mediante allowlist. El path se reenvía completo: `/api/llm/...` llega sin reescrituras al controlador.
- El Gateway valida JWT y reemplaza headers de identidad. `llm-service` autoriza rol, pertenencia y ownership de su dominio.
- El micro puede confiar solo en `X-Principal-Type`, `X-User-Id` / `X-User-Roles`, `X-Service-Id` / `X-Service-Scopes`, `X-Delegated-User`, `traceparent` y `X-Request-Id` provenientes de la red privada del Gateway.
- Los tokens M2M usan `type=service`, `aud=llm-service`, scopes mínimos y duración corta. Los usuarios nunca usan un token técnico.

## 4. Correlación y errores

La correlación estándar es `traceparent` más `X-Request-Id`, tanto en HTTP como en headers Kafka. Los cuerpos JSON no exponen el campo heredado `trace_id`.

Los errores HTTP usan RFC 7807 Problem Details e incluyen `requestId` como extensión. El Gateway devuelve 401, 429, 503 o 504 por políticas técnicas; `llm-service` devuelve 403 por autorización funcional y errores de dominio tipados.

## 5. Alcance de IA por fase

| Fase | Tema 07 |
|---|---|
| MVP | Tutor sin streaming token a token, registro de interacción, anti-fuga, evaluador asíncrono, rúbrica, golden set, calibración, apelación, score diferido y bloqueo de cierre. |
| Fase 2 | Chat interno y moderador de chat. |
| Fase 3 | RAG pedagógico, ingesta de material, desafíos personalizados por LLM y agentes `@mención`. |

El tutor MVP recibe contexto pedagógico validado por `practice-service`; no requiere retrieval RAG. El corrector y la generación de parciales no entran hasta contar con un RF del PRD y dueño explícito.

## 6. Pares y comunicación

Los service IDs canónicos son `practice-service`, `challenges-service`, `courses-service` y `admin-service`.

| Llamador | Scope M2M mínimo hacia `llm-service` |
|---|---|
| `practice-service` | `llm.tutor.invoke` |
| `challenges-service` | `llm.evaluation.read` |
| `courses-service` | `llm.calibration.read`, `llm.pending.read` |
| `admin-service` | `llm.golden-set.manage`, `llm.calibration.manage`, `llm.model-assignment.manage`, `llm.evaluation.override` |

- `practice-service` invoca tutoría por M2M.
- `challenges-service` publica el cierre de intento y aplica el modificador de XP; LLM nunca asigna XP.
- `courses-service` consulta calibración y evaluaciones pendientes antes de cambiar el estado del curso.
- `admin-service` administra modelos, golden set y calibraciones mediante M2M delegado.
- Kafka es el bus compartido para eventos asíncronos. No sustituye al Gateway para solicitudes HTTP.

## 7. Prueba mínima de integración

Antes de integrar se verifican las cuatro familias de `idea.pptx.pdf`: ruteo/path final, JWT y headers falsificados, Eureka con dos instancias, y resiliencia (429, timeout, retry idempotente, breaker y trazabilidad).

Los contratos vigentes están en [`contracts/llm-service-v1.openapi.yaml`](contracts/llm-service-v1.openapi.yaml) y [`contracts/llm-service-v1.asyncapi.yaml`](contracts/llm-service-v1.asyncapi.yaml). La cobertura de requisitos vive en [21](21-matriz-trazabilidad-llm.md).
