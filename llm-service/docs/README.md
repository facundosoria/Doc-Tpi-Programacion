# docs — guía de entrada a `llm-service`

Esta es la documentación de trabajo de Tema 07 y la única fuente vigente: no hace falta recorrer
otra carpeta ni buscar documentos por nombre. Empezá acá, seguí el recorrido de abajo y usá el
[catálogo documental](catalogo-documental.md) para llegar a cualquier bloque sin explorar carpetas al
azar. Toda corrección o evolución de una regla queda anotada en [`registro/`](registro/README.md).

## Primeros 45 minutos

Seguí este orden antes de tomar una tarea:

1. [00 — Gobierno y evolución](00-gobierno-y-evolucion/README.md): qué fuente manda, vocabulario
   y decisiones ya tomadas.
2. [01 — Visión, alcance y entrega](01-vision-alcance-y-entrega/README.md): problema, MVP,
   exclusiones y entregables.
3. [02 — Arquitectura y plataforma](02-arquitectura-y-plataforma/README.md): fronteras del
   servicio, Gateway, stack, Docker y estructura del backend.
4. [03 — Capacidades de IA](03-capacidades-de-ia/README.md) y
   [04 — Seguridad, datos y cumplimiento](04-seguridad-datos-y-cumplimiento/README.md): qué debe
   hacer la funcionalidad y qué límites no puede romper.
5. [Contratos](contracts/README.md): obligatorio antes de integrar, exponer un endpoint o publicar
   un evento.
6. [06 — Operación, calidad y pruebas](06-operacion-calidad-y-pruebas/README.md): obligatorio antes
   de dar una tarea por terminada.

Después elegí tu recorrido según el trabajo que vas a hacer:

| Si vas a… | Leé primero | Después seguí con |
|---|---|---|
| Implementar una funcionalidad del backend | Arquitectura y la capacidad de IA correspondiente | Seguridad, contratos, operación y la historia/tarea asignada. |
| Integrar otro microservicio | [Mapa de integración](contracts/00-mapa-de-integracion.md) | Requisitos de entrada, vista del equipo y OpenAPI/AsyncAPI. |
| Cambiar un endpoint o evento | [Contratos](contracts/README.md) | El schema ejecutable y los consumidores afectados. |
| Trabajar Golden Set, rúbricas o calibración | [Golden Set y calibración](03-capacidades-de-ia/golden-set-y-calibracion/README.md) | Dominio MVP, contratos de Backoffice y pruebas. |
| Tomar una historia, planificar o cerrar un sprint | [07 — Planificación y trabajo](07-planificacion-y-trabajo-equipo/README.md) | Épica, historia, tarea, sprint y flujo Git. |
| Investigar un antecedente o una decisión antigua | [08 — Preguntas e investigación](08-preguntas-investigacion-y-sincronizaciones/README.md) | Volvé al documento vigente de 00–06 antes de implementar. |
| Abrir rama, PR o cerrar una tarea | [09 — Flujo de trabajo](09-flujo-de-trabajo-del-equipo/README.md) | Planificación, pruebas y evidencia de la tarea. |

## Cómo reconocer qué documento manda

- **Regla vigente:** empezá por `00 — Gobierno`. Si hay conflicto, su orden de precedencia decide.
- **Contrato vigente:** está bajo [`contracts/`](contracts/README.md). Los YAML de raíz son los
  schemas ejecutables; `contracts/equipos/` explica cada contraparte.
- **Diseño, operación y planificación:** las carpetas numeradas explican el porqué y el cómo. Leé
  primero su `README.md` y después los archivos numerados.
- **Histórico o evidencia:** los nombres `historico`, `registro`, `estado-de-implementacion` e
  `investigacion-y-material` conservan contexto. No se editan para cambiar una regla vigente.

No copies una regla en otro archivo para corregirla: actualizá el documento canónico correspondiente,
el contrato/schema afectado si corresponde, y registrá el cambio en [`registro/`](registro/README.md).

## Mapa de la documentación

El [índice global](catalogo-documental.md) permite llegar a cualquier bloque documental sin
explorar carpetas al azar. Las categorías tienen prefijo numérico para indicar el orden de lectura;
las subcarpetas contienen su propio README cuando requieren un recorrido particular.

Los siete archivos breves de la raíz (`00-gobierno-y-fuentes-de-verdad.md` a
`06-trazabilidad-y-estado.md`) son el núcleo de referencia del MVP. No sustituyen el detalle de las
carpetas: dan una entrada rápida y remiten a las fuentes temáticas.