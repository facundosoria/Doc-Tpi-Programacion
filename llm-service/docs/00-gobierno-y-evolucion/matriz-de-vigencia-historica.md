# Matriz de vigencia histórica

**Propósito:** clasificar reglas ya documentadas sin borrar, resumir ni editar los documentos de
origen. Esta matriz no reemplaza ninguna fuente: indica dónde consultar el texto completo y qué
documento canónico debe actualizarse cuando una regla cambie.

## Estados

| Estado | Significado |
|---|---|
| Vigente | La regla sigue aplicando y no tiene reemplazo identificado. |
| Vigente con ajuste | La regla se conserva, pero su nombre, alcance o contrato está normalizado en la fuente canónica vigente. |
| Reemplazada | Una decisión posterior identificada cambió esa regla. El antecedente se conserva. |
| Pendiente | Falta decisión de Product Owner, responsable externo, legal o evidencia técnica. |
| Futura / fuera de MVP | Sigue siendo diseño o investigación de una fase posterior; no prometerla como MVP. |
| Histórica | Contexto, auditoría o estimación temporal; no es una instrucción vigente por sí sola. |

## Documento 00 — fuentes y convenciones

| Regla o bloque | Estado | Fuente de consulta / destino canónico |
|---|---|---|
| Precedencia: producto aprobado, arquitectura de plataforma, contratos/ADR y resto de documentación | Vigente con ajuste | [Fuentes y convenciones](01-fuentes-de-verdad-y-convenciones.md) §1 y [Gobierno y fuentes de verdad](../00-gobierno-y-fuentes-de-verdad.md). |
| Identidad `llm-service`, prefijo `/api/llm/**`, Gateway, M2M, headers y Problem Details | Vigente con ajuste | [Fuentes y convenciones](01-fuentes-de-verdad-y-convenciones.md) §§2–4; contratos vigentes. |
| MVP tutor, anti-fuga, rúbrica, Golden Set, calibración, evaluación diferida y bloqueo | Vigente como alcance objetivo; implementación no inferida | [Alcance MVP](../01-alcance-mvp.md) y [estado](../06-trazabilidad-y-estado.md). |
| Chat/moderación y RAG/`@mención` por fases posteriores | Futura / fuera de MVP | [Fuentes y convenciones](01-fuentes-de-verdad-y-convenciones.md) §5 y [Alcance MVP](../01-alcance-mvp.md). |
| Cinco dimensiones, versiones inmutables publicadas y calibración por cohorte | Vigente con ajuste | [Dominio y flujos MVP](../03-dominio-y-flujos-mvp.md). La edición en borrador está regida por ADR-017. |
| Intercambio indirecto con `challenges-service` a través de `practice-service` | Vigente con ajuste | [Fuentes y convenciones](01-fuentes-de-verdad-y-convenciones.md) §6 y [mapa de integración](../contracts/00-mapa-de-integracion.md). |

## ADR del documento 08 — decisiones y pendientes

| ADR | Estado | Regla aplicable vigente |
|---|---|---|
| ADR-001, gateway único de IA | Vigente con ajuste | El servicio se denomina `llm-service`; ningún frontend o micro de negocio llama al proveedor. |
| ADR-002, ruteo determinístico | Vigente | La operación/endpoint determina la función; no hay orquestador LLM. |
| ADR-003, sincronía y trabajos diferidos | Vigente con ajuste | Tutor síncrono; calibración y evaluación diferida asíncronas. Corrector y moderador no pertenecen al MVP. |
| ADR-004, ADR-006 y ADR-007 | Futura / fuera de MVP | Decisiones de pgvector, embeddings y retrieval para RAG; no habilitan RAG MVP. |
| ADR-005, Java/Spring Boot | Vigente con ajuste | Java y `llm-service`; el nombre histórico `ms-evaluacion-llm` no es canónico. |
| ADR-008, solución fuera del prompt | Vigente | `expectedSolution` se usa solo en memoria del guardarraíl y no en prompt, log, persistencia, respuesta ni evento. |
| ADR-009, streaming | Reemplazada | La regla vigente mantiene el MVP sin streaming token a token. La propuesta posterior de Buffer Interceptor quedó como antecedente hasta decisión explícita. |
| ADR-010, asignación y costo de modelos concretos | Histórica | Precios/modelos no son una configuración obligatoria; la selección se rige por calibración y configuración administrativa. |
| ADR-011, prohibición absoluta de modelo local para evaluar | Reemplazada | ADR-018 permite cualquier candidato que supere doble calibración. |
| ADR-012, moderación clásica + clasificador | Futura / fuera de MVP | Aplicable cuando se implemente Fase 2. |
| ADR-013, rolling update; ADR-014, readiness sin proveedor; ADR-015, nginx en el borde | Vigente con ajuste | Operación y plataforma; normalizar nombres/rutas al tocar sus documentos. |
| ADR-016, LangChain4j dentro de adapters | Vigente | Es la implementación acordada del gateway interno de LLM. |
| ADR-017, rúbricas editables/versionadas | Vigente | Decisión de producto aprobada: cinco dimensiones, borrador editable, pesos 100 %, publicación inmutable. |
| ADR-018, modelo por cohorte y doble calibración | Vigente | Admin administra credenciales/modelos; la cohorte solo puede activar un candidato calibrado. |

## Preguntas y pendientes del documento 08

| Grupo | Estado | Tratamiento |
|---|---|---|
| P-01 y P-03, corrector académico fuera del alcance | Vigente | No crear contratos ni funcionalidades de corrección académica LLM. |
| P-04, responsable/fecha del Golden Set base | Pendiente | Requiere responsable externo explícito antes de declarar habilitación operativa. |
| P-05, límites iniciales de uso | Pendiente | Son valores iniciales a validar con cuota, costo y datos reales; no se declaran como contrato acordado. |
| P-06, uso de free tier con datos de alumnos | Pendiente | Decisión legal/producto; solo datos sintéticos en desarrollo hasta resolución escrita. |
| P-07 y P-08, opciones locales y tipo de desafíos | Histórica / pendiente de evidencia | No cambian el alcance MVP sin decisión posterior y pruebas. |
| P-02 y P-09 a P-11, moderación y `@mención` | Futura / fuera de MVP | Mantener como diseño de Fase 2/3. |
| P-12, límites individualizados por alumno | Pendiente | Requiere definición PO/DPO y contrato Back Office. |
| B-1/B-3/B-4 y C-6, contratos y evaluación diferida | Pendiente | Se gestionan en [requisitos a otros micros](../contracts/requisitos-a-otros-micros.md) y [trazabilidad](../06-trazabilidad-y-estado.md). |

## Otros antecedentes de gobierno

| Documento | Estado | Cómo usarlo |
|---|---|---|
| [Glosario y metadata](03-glosario-y-metadata.md) | Vigente con ajuste | Usar términos no contradictorios; nombres de tablas/campos siguen siendo propuesta hasta acordarlos. RAG y moderación conservan estado de fase futura. |
| [Matriz de trazabilidad](04-matriz-trazabilidad.md) | Histórica | Evidencia de preparación; el estado actual se declara en [Trazabilidad y estado](../06-trazabilidad-y-estado.md). |
| [Informe comparativo](05-informe-de-alineacion.md) | Histórica | Explica una alineación anterior; contrastar siempre con la documentación y los contratos vigentes. |
| [Normativa de cátedra](06-normativa-de-catedra.md) | Vigente | Norma externa; divergencias solo mediante una decisión aprobada y registrada. |
| [Control de consolidación](07-control-de-consolidacion-y-entrega.md) | Histórica con acciones pendientes | Sus brechas de contrato se trasladaron a `contracts/`; no usar los OpenAPI v1/v2 históricos como fuente única. |
| [Informe de auditoría](08-auditoria-de-convenciones.md) | Histórica | Auditoría de una presentación. Las correcciones de nombre/ruteo siguen vigentes, pero rutas funcionales se toman del OpenAPI vigente. |

## Regla de mantenimiento

Al resolver una fila marcada **Pendiente** o cambiar una regla **Vigente**, se actualiza primero el
documento canónico correspondiente, después esta matriz y finalmente se agrega una entrada con
antes, motivo, decisión y después en [registro](../registro/README.md). El antecedente histórico
que originó la regla no se reescribe ni se elimina.
