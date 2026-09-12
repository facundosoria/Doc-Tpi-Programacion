# 00 — Fuentes de verdad y convenciones obligatorias

> Referencia normativa para `llm-service` y para cualquier documento posterior del Tema 07.

## 1. Orden de precedencia

| Prioridad | Fuente | Decide |
|---|---|---|
| 1 | `PRD-Plataforma-Gamificada-TP.pdf` y adendas de producto expresamente aprobadas | Alcance funcional, fases, reglas académicas y criterios de release. Las adendas solo reemplazan los puntos que identifican de forma explícita. |
| 2 | `idea.pptx.pdf` | Red, Gateway, Eureka, identidad, rutas, observabilidad y pruebas de integración. |
| 3 | Este documento, contratos v1 y ADRs | Aplicación concreta de esas dos fuentes a Tema 07. |
| 4 | Resto de `docs/` | Explicación y detalle; no puede contradecir las fuentes anteriores. |

`docs/importado/` es material histórico y no participa de esta jerarquía. Tampoco se modifica.

Los PDF de origen (`PRD-Plataforma-Gamificada-TP.pdf`, `TUP_PIV_BE_PROPUESTA_ARQ.pdf`,
`TUP_PIV_FE_TEO_U1_ARQUITECTURA_DESPLIEGUE.pdf`) viven en `docs/fuentes/`,
no se versionan (`.gitignore`) y no se modifican: se distribuyen por los canales de la cátedra.

Las decisiones de producto que amplían el PRD para Golden Set y calibración son:

- [ADR-017](08-decisiones-y-pendientes.md#adr-017--rúbricas-editables-y-versionadas-por-curso): el docente puede editar criterios, anclas, prompts y pesos; las cinco dimensiones siguen siendo obligatorias y los pesos deben sumar 100 %.
- [ADR-018](08-decisiones-y-pendientes.md#adr-018--selección-de-modelo-por-curso-sujeta-a-doble-calibración): el docente puede elegir por curso entre modelos habilitados por ADMIN; cada candidato debe superar calibración base y del curso.

Estos dos ADR funcionan como adendas limitadas. No cambian ningún otro requisito del PRD.

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

El tutor MVP recibe contexto pedagógico validado por `practice-service`; no requiere retrieval RAG. El corrector y la generación de parciales quedan fuera del alcance vigente.

El evaluador analiza exclusivamente **cómo el alumno utilizó al tutor de IA**. Recibe la conversación completa, el contexto del desafío y la metadata del intento, y produce cinco puntuaciones según la rúbrica publicada. El Golden Set calibra esta función con conversaciones puntuadas por personas.

La validación académica determina si la respuesta o entrega resuelve el desafío. Esa decisión corresponde a reglas determinísticas del dominio o a revisión docente. No usa el Golden Set, no forma parte de la calibración del evaluador y no debe inferirse a partir del score de uso de IA.

## 5.1 Terminología obligatoria de Golden Set y calibración

| Término | Uso canónico |
|---|---|
| Evaluador | Función LLM que puntúa el uso pedagógico del tutor de IA. |
| Golden Set | Conjunto versionado de conversaciones con contexto, metadata y cinco puntuaciones humanas de referencia. |
| Caso | Una conversación completa incorporada a una versión del Golden Set. |
| Rúbrica | Cinco dimensiones obligatorias con criterios, anclas, prompts y pesos; una versión publicada es inmutable. |
| Calibración | Comparación reproducible entre el evaluador y las referencias humanas. No entrena ni ajusta los pesos del modelo. |
| Calibración activa | Ejecución aprobada que se asigna a nuevos desafíos de un curso. Solo puede existir una por curso. |
| Evaluación | Resultado sobre el uso de IA de un intento real, ligado a la calibración que tenía asignada. |
| Corrección académica | Validación del contenido de una respuesta o entrega. Queda fuera de este módulo. |

No se usan “examen corregido”, “respuesta esperada”, “corrector LLM” ni “nota del examen” como nombres del Golden Set, sus casos o la evaluación del uso de IA.

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
