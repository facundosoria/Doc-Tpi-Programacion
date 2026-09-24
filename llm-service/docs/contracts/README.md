# 05 — Contratos de integración

Esta carpeta es la única entrada para la comunicación entre `llm-service` y otros microservicios.
Usala antes de exponer un endpoint, consumir datos, publicar un evento o asumir una responsabilidad
de otra contraparte.

## Qué vas a encontrar

1. [Mapa de integración](00-mapa-de-integracion.md): actores, canales, responsables y acuerdos
   pendientes.
2. [Inventario y brechas](01-inventario-y-brechas.md): contratos definidos, parciales y faltantes,
   con el orden recomendado para cerrarlos, y
   [contratos de skills y calibración](02-contratos-skills-y-calibracion.md) (superficie de API
   propuesta) junto con
   [sincronización de calibración y avisos de operación](03-sincronizacion-calibracion-y-notificaciones.md)
   (detalle íntegro por decisión de Producto de las fronteras con `courses-service`,
   `challenges-service` y `notification-service`).
3. [Requisitos a otros micros](requisitos-a-otros-micros.md): datos, eventos y consultas que
   Tema 07 necesita recibir para responder, evaluar o cerrar un intento.
4. [OpenAPI actual](llm-service.openapi.yaml): contrato HTTP que Tema 07 expone. Para moderación,
   ver también [`llm-service-v1-moderacion.openapi.yaml`](llm-service-v1-moderacion.openapi.yaml) y
   los pendientes con `chat-service` en
   [`moderacion-pendientes-chat-service.md`](moderacion-pendientes-chat-service.md). El tutor en
   streaming (SSE) está propuesto, no fusionado, en
   [`llm-service-tutor-interactions-stream-propuesta.md`](llm-service-tutor-interactions-stream-propuesta.md).
5. [AsyncAPI actual](llm-service.asyncapi.yaml) (v3.0.0): eventos Kafka que Tema 07 publica y consume, con los
   nombres de tópico marcados como provisorios. Su estándar es el del PDF `KAFKA.pdf`, transcrito en
   [`KAFKA_EVENT_STANDARD.md`](KAFKA_EVENT_STANDARD.md) ([ADR-020](../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md)).
6. [Contratos por equipo](equipos/README.md): explicación completa por contraparte, incluyendo
   responsabilidades, secuencias, errores y pendientes.
7. [Simulador y Mock](MOCK.md): comando de una línea (Prism) y simulación local con Docker Workbench para habilitar integración desacoplada (CA2).
8. [Skillhub](skillhub/README.md): contratos y revisiones pendientes de aceptación con Skillhub
   (skills adjuntables); estado en curso, no cerrado.

## Cómo leerla

Partí del mapa. Luego elegí la contraparte y leé sus requisitos o su contrato narrativo. Por último
abrí el schema ejecutable que corresponda: **OpenAPI para HTTP** y **AsyncAPI para Kafka**. No copies
JSON desde una explicación narrativa; el YAML es la definición técnica única de campos y tipos.

Cada contrato declara estado: **Propuesto** requiere revisión de pares; **Acordado** tiene aceptación
explícita de productor y consumidor; **Implementado** además cuenta con evidencia en código y
pruebas. No infieras un estado por el solo hecho de encontrar un endpoint o evento documentado.

## Cómo actualizar un contrato

Un cambio de contrato modifica en el mismo trabajo la vista por equipo, el schema afectado y la
evidencia/prueba que lo valida. Si cambia una regla, registralo en [`registro/`](../registro/README.md).
Los archivos `11-equipos/*/contratos.md` son punteros de navegación, no una segunda fuente editable.
