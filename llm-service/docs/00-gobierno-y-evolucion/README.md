# 00 — Gobierno y evolución

Esta carpeta responde una pregunta antes que cualquier otra: **¿qué documento manda cuando dos
fuentes parecen decir cosas distintas?** Leela al incorporarte al proyecto y cada vez que una
decisión afecte alcance, seguridad, contratos o planificación.

## Qué vas a encontrar

1. [Fuentes de verdad y convenciones](01-fuentes-de-verdad-y-convenciones.md): orden de
   precedencia, identidad del servicio, Gateway, correlación y reglas transversales.
2. [Decisiones y pendientes](02-decisiones-y-pendientes.md): ADR narrados, decisiones aprobadas,
   dueños y preguntas que aún requieren acuerdo (incluye ADR-004, pgvector en el Postgres existente).
   Los ADR con archivo formal propio viven en [`adr/`](adr/):
   [ADR-001](adr/ADR-001-arquitectura-y-convenciones-llm-service.md) (arquitectura y convenciones),
   [ADR-002](adr/ADR-002-mensajeria-kafka-outbox-y-dedup.md) (mensajería Kafka, outbox y dedup),
   [ADR-003](adr/ADR-003-estructura-multimodulo-y-variables-de-entorno.md) (estructura multi-módulo y
   variables de entorno vigentes; supersede parcialmente a ADR-001) y
   [ADR-021](adr/ADR-021-rubrica-jerarquica-dimensiones-extendidas-y-subcriterios-ponderables.md)
   (rúbrica jerárquica, dimensiones extendidas y subcriterios ponderables; **propuesto, pendiente de
   aprobación** — numerado 021 para no colisionar con el ADR-004 narrado en `02-decisiones-y-pendientes.md`,
   ya que este documento usa una numeración continua de ADR-001 a ADR-020 antes de que existiera el
   primer archivo formal separado).
3. [Glosario y metadata](03-glosario-y-metadata.md): términos que distintos equipos pueden usar
   con sentidos diferentes.
4. [Matriz de trazabilidad](04-matriz-trazabilidad.md): relación entre requisitos, contratos,
   historias, pruebas y evidencia.
5. [Informe de alineación](05-informe-de-alineacion.md) y
   [normativa de cátedra](06-normativa-de-catedra.md): contraste con las fuentes de plataforma y
   reglas externas que debemos respetar.
6. [Control de consolidación](07-control-de-consolidacion-y-entrega.md),
   [auditoría de convenciones](08-auditoria-de-convenciones.md),
   [matriz de vigencia histórica](matriz-de-vigencia-historica.md) y
   [detección de repeticiones](09-deteccion-de-repeticiones.md): control de calidad documental y
   trazabilidad de los cambios de esta documentación.

## Cómo leerla

Para una incorporación nueva, leé `01`, `02` y `03` en ese orden. Consultá `04` cuando necesites
saber cómo se prueba o evidencia una condición. Los demás documentos se leen por necesidad: no son
un tutorial de implementación, sino respaldo para decidir con consistencia.

Si encontrás una contradicción, no la resuelvas por intuición ni copies una regla a otro archivo:
aplicá el orden de precedencia de `01`, corregí la fuente canónica y registrá el cambio en
[`registro/`](../registro/README.md).

## Qué no vas a encontrar aquí

No contiene el detalle de bodies HTTP, eventos Kafka ni código. Para integrar leé
[`contracts/`](../contracts/README.md); para implementar una funcionalidad, continuá con alcance,
arquitectura, capacidad de IA, seguridad y operación.
