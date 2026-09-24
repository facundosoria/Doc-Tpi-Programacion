# 09 — Detección de repeticiones documentales

## Propósito

Este informe identifica solapamientos entre documentos de esta documentación y deja constancia de las
consolidaciones realizadas. No se elimina, resume ni acorta información única: cuando un bloque es
la misma regla, contrato o decisión que otro, se conserva una fuente canónica y las demás copias se
reemplazan por una referencia explícita. Una repetición puede ser intencional (por ejemplo, una
plantilla y su instancia) o puede ser una regla que conviene mantener una sola vez.

## Método

Se compararon los documentos Markdown por fragmentos consecutivos de cinco palabras y luego se
revisaron manualmente los candidatos principales mediante títulos y secciones. También se ejecutó
una comparación SHA-256 de cada archivo Markdown completo, incluyendo README, y una comprobación de
todos los enlaces Markdown relativos. La métrica detecta texto compartido; no decide por sí sola
que dos documentos sean duplicados. Los contratos YAML se verifican por separado como schemas
ejecutables, no como prosa duplicable.

## Hallazgos que no son duplicados

| Documentos | Por qué se parecen | Decisión |
|---|---|---|
| `06/01-operacion-e-ingenieria.md` y `contracts/equipos/frontend-angular.md` | El segundo necesita la vista de estados que el consumidor Angular debe representar, mientras el primero explica la operación del sistema completo. | Mantener ambos por audiencia. La tabla común de fallos/UX quedó una sola vez en operación; el contrato conserva su mapeo de estados y enlaza a la fuente. |
| `07/10-plantillas/sprint-llm.md` y `07/.../s1-propuesto.md` | La propuesta es una instancia rellenada de la plantilla. | Mantener ambos. La plantilla es reusable; la propuesta es evidencia histórica. |
| `07/02-arranque-agil-y-sprint-0.md` y `07/04-backlog-ejecutable.md` | Ambos describen Sprint 0, DoR/DoD y capacidad. | Mantener ambos por ahora: uno explica el arranque y el otro contiene el backlog S0–S19. Marcar referencias cruzadas y no volver a copiar reglas al actualizarlos. |
| `contracts/90-mapa-de-integracion-historico.md` y contratos de equipos | Comparían actores, rutas y dependencias. | **Actualizado 2026-09-22:** el mapa histórico se retiró (ver `registro/2026-09-22-limpieza-historicos-contracts.md`); el detalle vigente por contraparte queda centralizado en [`contracts/equipos/`](../contracts/equipos/README.md), sin duplicado que mantener. |
| Historias dentro de una misma épica (`ep-03`, `ep-06`, `ep-09`, etc.) | Repiten estructura de HU, criterios y vocabulario del mismo flujo. | Mantener: son unidades ejecutables distintas, no versiones del mismo documento. |
| `07/.../CORRECCIONES-SUGERIDAS.md` y `08/.../CORRECCIONES-SUGERIDAS.md` | Comparten formato y algunas observaciones, pero tratan código y documentación diferentes. | Mantener separados y distinguir en los README qué objeto corrige cada uno. |

## Candidatos de consolidación por referencia

Estos casos sí repiten reglas o decisiones que deberían tener una fuente primaria. Los que figuran
en la sección de consolidación ya fueron revisados y reemplazados únicamente por referencias; los
restantes siguen pendientes porque requieren confirmar alcance, acuerdos externos o diferencias de
nivel. En todos los casos se conserva cualquier detalle exclusivo.

| Tema repetido | Documentos afectados | Fuente primaria propuesta | Estado |
|---|---|---|---|
| Alcance y límites del MVP | `00/01-fuentes...`, `01/01-problema...`, `01/02-entregables...`, `03/02-funciones...`, `06/07-control...` | [`01-alcance-mvp.md`](../01-alcance-mvp.md) + [`00-gobierno-y-fuentes-de-verdad.md`](../00-gobierno-y-fuentes-de-verdad.md) | Revisar antes de consolidar: evaluación, apelación y bloqueo tienen alcance que debe confirmar PO. |
| Identidad, Gateway, rutas y headers | `00/01-fuentes...`, `02/01-arquitectura...`, `contracts/90...`, `contracts/91...`, carpetas Gateway y contratos de equipos | [`contracts/`](../contracts/README.md) y reglas de Gateway | Consolidar después de verificar contra la arquitectura de plataforma. |
| Rúbrica, cinco dimensiones, pesos y publicación | `00/02-decisiones...` ADR-017, `00/03-glosario...`, `03/03-rubricas...`, `03/golden-set/02-especificacion...`, `03-dominio...` | [`03-dominio-y-flujos-mvp.md`](../03-dominio-y-flujos-mvp.md) + OpenAPI | La regla de rúbricas editables está aprobada; retirar solo contradicciones antiguas de pesos fijos. |
| Golden Set y calibración | `00/02-decisiones...`, `03/golden-set/01`, `02`, `03`, `03/02-funciones...`, estados de implementación | `03/capacidades-de-ia/golden-set-y-calibracion/` y contratos | Mantener plan, especificación y dominio; quitar repeticiones normativas cuando se encuentre cada párrafo. |
| Anti-fuga y solución esperada | `00/01-fuentes...`, `00/02-decisiones...` ADR-008, `03/02-funciones...`, `04/01-seguridad...`, contratos Practice | [`04-seguridad-y-datos-sensibles.md`](../04-seguridad-y-datos-sensibles.md) + `contracts/requisitos-a-otros-micros.md` | Pendiente del contrato real de Practice; no eliminar ningún detalle antes de cerrarlo. |
| Sincronía, colas, outbox, reintentos y resiliencia | `02/01-arquitectura...`, `06/01-operacion...`, `04/03-rate-limit...`, `07/06-playbook...`, Gateway | [`05-operacion-y-pruebas.md`](../05-operacion-y-pruebas.md) y ADRs vigentes | Consolidar solo reglas técnicas; conservar ejemplos y decisiones históricas. |
| Planificación, capacidad, DoR/DoD y sprints | `01/02-entregables...`, `07/01`, `07/02`, `07/03`, `07/04`, `07/05`, `07/06` | El documento correspondiente a cada nivel: marco, plan vigente o backlog ejecutable | No fusionar: tienen distintos niveles temporales. La regla de aceptación queda canónica en `07/03`; `07/02` ahora la referencia. Enlazar para evitar repetirla en nuevas páginas. |

## Consolidación aplicada — 2026-09-15

Cada fila conserva el contenido exclusivo del original. Cuando se indica “puntero”, el archivo
original sigue existiendo para mantener su ruta y conducir al documento canónico; no es una segunda
versión editable.

| Repetición detectada | Fuente canónica | Qué quedó en la otra ubicación | Motivo y control de pérdida |
|---|---|---|---|
| Guía completa de tareas repetida en el índice `09-epicas-historias-tareas-sprints/README.md` | `09-epicas-historias-tareas-sprints/tareas/README.md` | El README de la raíz quedó como índice de epics, historias, tareas y sprints. | La guía extensa se conserva completa en `tareas/README.md`; el índice ya no compite con ella. |
| `historias/PREGUNTAS-ABIERTAS.md` duplicaba las preguntas canónicas | `07.../PREGUNTAS-ABIERTAS.md` | El archivo bajo `historias/` es un puntero. | Las 292 líneas y sus respuestas siguen en la fuente canónica; se preservó la ruta histórica. |
| Tabla de estados de error/UX repetida en el contrato de Angular | `06-operacion-calidad-y-pruebas/01-operacion-e-ingenieria.md`, §6 | `contracts/equipos/frontend-angular.md` conserva el mapeo propio de pantallas y enlaza a §6. | La operación y la vista de consumidor siguen completas para sus audiencias, sin dos tablas normativas. |
| Diagramas de secuencia copiados en el mapa histórico y contratos | `contracts/equipos/tema-05-desafios-practicos.md` y `contracts/equipos/tema-11-chat.md` | **Actualizado 2026-09-22:** `90-mapa-de-integracion-historico.md` se retiró; el diagrama y la decisión I-04 quedan solo en los contratos de equipo, que ya los tenían completos. | El flujo vigente tiene una sola descripción detallada por equipo, sin copia paralela. |
| Schema JSON del evaluador repetido en el contrato T05 | `contracts/llm-service.asyncapi.yaml`, mensaje `ScoreCalculated` | **Actualizado 2026-09-22:** `91-contratos-inter-equipos-historicos.md` se retiró; ya no queda el ejemplo histórico `snake_case` en ningún lado. | El contrato ejecutable define los campos una sola vez. |
| Nueve contratos completos fuera de `contracts/` | `contracts/equipos/*.md` | Cada `07.../11-equipos/*/contratos.md` es un puntero de navegación. | Se mantuvo todo el contenido completo, cambió solo su ubicación canónica para evitar dos copias editables. |
| Regla de funcionalidad parcial repetida entre Sprint 0 y el plan | `07-planificacion-y-trabajo-equipo/03-plan-de-construccion-del-producto.md`, §9 | Sprint 0 explica que aplica la regla y enlaza a §9. | La política se modifica en un solo sitio; no se perdió su aplicación al ingreso al sprint. |
| Estimación/prioridad del paquete EP-09 repetida en H01 y H02 | `historias/ep-09/h01.md`, §Estimación / Prioridad | H02 conserva la sección y referencia H01 con los valores (~208 h, Should y condición de adelanto). | La información sigue visible desde H02, pero los números no pueden divergir entre historias del mismo paquete. |
| Explicación de la relación épica–historia repetida entre Sprint 0 y el backlog | `07-planificacion-y-trabajo-equipo/04-backlog-ejecutable.md`, sección «Épicas» | Sprint 0 conserva su tabla de entrega y referencia esa explicación. | Se mantiene la vista específica de Sprint 0 sin dos párrafos normativos iguales. |
| Rutas internas que apuntaban a las copias antiguas | Documento canónico correspondiente | Los punteros mantienen compatibilidad; las guías e índices principales enlazan al destino canónico. | Se ejecutó una comprobación de enlaces relativos después de mover y consolidar. |

## Resultado actual

- No hay archivos Markdown idénticos por contenido: la comparación SHA-256 dio **0
  grupos duplicados**.
- La comprobación de enlaces Markdown relativos dio **0 destinos faltantes**.
- Las repeticiones semánticas que permanecen son intencionales: plantillas frente a instancias
  rellenadas, tarjetas de historias distintas que deben ejecutarse por separado y documentos con
  diferente audiencia o nivel temporal.
- No se elimina texto basándose solo en una métrica de similitud. Antes de cada consolidación se
  extrajeron los detalles exclusivos, se registró la fuente primaria y se comprobó que ningún
  criterio, excepción, fecha o dependencia desapareciera.

## Próximo procedimiento seguro

1. Revisar un tema de esta tabla por vez.
2. Comparar párrafos completos, no solo títulos o porcentaje de similitud.
3. Declarar la fuente primaria en el documento de gobierno.
4. Cambiar las demás apariciones a una referencia, preservando excepciones y contexto exclusivo.
5. Registrar en `registro/` el antes, motivo, destino y verificación de que no se perdió información.
6. Volver a ejecutar SHA-256 y el verificador de enlaces antes de cerrar el cambio.
