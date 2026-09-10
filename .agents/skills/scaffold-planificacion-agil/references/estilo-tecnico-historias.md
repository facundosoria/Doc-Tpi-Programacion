# Estilo simple (por defecto) ↔ estilo técnico

El skill genera **lenguaje simple** por defecto. Si el equipo pide la versión con jerga
precisa (endpoints, códigos HTTP, nombres exactos), aplicá los cambios de abajo. **El
contenido y los escenarios son los mismos**: cambia el idioma y algunos rótulos. En un
proyecto que agrupa por épica, el estilo técnico vive en `docs/historias/ep-0X.md` y la
versión en lenguaje simple, en un doc narrativo del sprint (`docs/sprints/sN-explicado.md`).

## 1. Rótulos de sección

| Estilo simple (por defecto) | Estilo técnico |
|---|---|
| Lista de metadatos (viñetas `- **Rótulo:** valor`): **Grupo de trabajo · Pareja a cargo · Depende de · Trabajo estimado · Tipo · Responsable del producto** | **Épica · Pareja · Dependencias · Estimación (plan) · Tipo · Requisito · Referente de producto** |
| Notas → **Reglas de trabajo** | **Reglas de negocio** |
| Notas → **Cómo se controla** | **Validaciones** |
| Notas → **Qué tiene que incluir sí o sí** | **Datos obligatorios** |
| Notas → **Tiempos / volumen** | **Performance (tiempos, volumen, límites)** |
| Notas → **Operaciones nuevas** (en palabras) | **Endpoints** (`MÉTODO /ruta` → respuesta) |
| CA → **(caso que debe fallar)** | **(negativo)** |
| BDD → **Qué se prueba:** | **Característica:** |
| BDD → escenario «camino esperado» | «camino feliz» |
| Estimación en **bullets** («Puntos de esfuerzo: se asignan en el Sprint 0» / «Prioridad: imprescindible») | **tabla** `\| Puntos (Fibonacci) \| Prioridad (MoSCoW) \|` |
| Dependencias → **Partes involucradas** | **Servicios involucrados** + **Módulos afectados** |
| Dependencias → **Impacto en los datos** | **Impacto en datos / migraciones** |
| Tipo: **Tarea interna** / **Historia de valor** | **Tarea de sprint (habilitador)** / **HU de valor** |
| Prioridad: **imprescindible / deseable / se puede posponer** | **Must / Should / Could / Won't** |

Documento: el simple agrega glosario de términos repetidos + sección «Antes de las
fichas» + «En resumen»; el técnico agrega la tabla de «fuente única» y la nota de
«Título en Taiga».

## 2. Glosario de traducción (dominio de plataforma)

| Palabra simple | Término técnico |
|---|---|
| recepción central | API Gateway |
| se anota en el directorio / directorio de servicios | registro en Eureka / service discovery |
| credencial | token JWT / M2M |
| permiso | scope |
| en nombre de qué persona / identificación de la persona | usuario delegado (`X-Delegated-User`) |
| número de seguimiento | correlación (`traceparent` / `X-Request-Id`) |
| formato ordenado y siempre igual (para los errores) | Problem Details (RFC 7807) |
| «prohibido» | `403` |
| «no autorizado» | `401` |
| «no encontrado» | `404` |
| «pedido inválido» | `400` / `422` |
| «error de servidor» | `500` |
| «confirma» / «crea» (una respuesta) | `201` + `Location` |
| marca antiduplicado | `Idempotency-Key` (UUID) |
| colección de referencia | golden set |
| plantilla de corrección | rúbrica |
| las cinco cosas que se evalúan | dimensiones de la rúbrica |
| curso | cohorte |
| la herramienta base | Docker / `docker compose` |
| enchufe (ocupado) | puerto |
| proceso automático de armado | migración Flyway (`V1`) desde base vacía |
| solo se puede agregar (una tabla) | append-only |
| chequeo de salud | healthcheck / `/actuator/health` |
| control de calidad automático / control automático | pipeline de CI |
| documento con las reglas de cómo se arma el programa | ADR |
| el «corazón» del programa | el paquete `domain` |
| de a páginas | paginado (`page` / `size`) |
| apagar y prender | `docker compose restart` |
| simulador (de la base / de la recepción central) | Testcontainers / WireMock |

Ampliá la tabla con los términos propios del dominio del proyecto que se esté
documentando. Regla: en la prosa se usa la palabra simple; el término técnico puede ir
una vez entre paréntesis la primera vez que aparece, si ayuda.

## 3. Qué NO se traduce

Los códigos internos se dejan tal cual en las dos versiones, porque son parte del formato
de Taiga: `EP-01`, `P1`, `Sxx-Hyy`, `RF-*`, `PAR-*`, nombres de sprint. En la versión
simple se aclara una vez, al inicio, que son etiquetas de trazabilidad y no hace falta
entenderlas.
