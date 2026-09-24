# 06 — Trazabilidad y estado de implementación

## Cómo leer este documento

El contrato es el objetivo de integración; no prueba que el código lo implemente. **Implementado**
requiere código, pruebas y contrato acordado. Esta matriz evita que la documentación prometa una
capacidad que solo está diseñada.

| Capacidad | Fuente canónica | Dependencia externa | Estado contractual | Estado de código a verificar |
|---|---|---|---|---|
| Tutor con guardarraíl anti-fuga | OpenAPI `/tutor/interactions` | Contexto seguro de Practice | Propuesto | Parcial: verificar payload y no persistencia de solución. |
| Rúbricas editables y versionadas | OpenAPI `/rubrics` | Membresía docente de Courses | Propuesto | Parcial: verificar ciclo DRAFT/PUBLISHED y cinco dimensiones. |
| Golden Set y calibración | OpenAPI `/golden-sets`, `/calibrations` | Membresía docente y Back Office | Propuesto | Parcial: verificar ejecución y activación. |
| Gestión de Gemini, Claude, OpenAI/Groq | OpenAPI `/admin/*` | Admin/Gateway | Propuesto | Parcial: verificar cifrado, enmascaramiento y adaptadores. |
| Práctica publicada e inicio/cierre | Requisitos de entrada | Events de Practice | Pendiente de acuerdo | No declarar implementado hasta contar con contrato de Practice. |
| Score y diferimiento | AsyncAPI score | Consumer Practice | Propuesto | Parcial: verificar outbox, schema y deduplicación del consumidor. |
| Consulta de calibración/pendientes | OpenAPI de Courses | Consumer Courses | Propuesto | Parcial: verificar semántica de bloqueo. |
| Pertenencia docente | Requisitos a Courses | Endpoint/evento de Courses | Pendiente de acuerdo | No declarar implementado hasta acordar la interfaz de Courses. |

## Criterio de actualización

Al cerrar un acuerdo o cambiar código, se modifica primero el contrato o documento de esta carpeta,
se actualiza esta fila con evidencia concreta (prueba, ruta o commit) y se agrega una entrada en
`registro/`. No se conserva una regla anterior activa como adenda.
