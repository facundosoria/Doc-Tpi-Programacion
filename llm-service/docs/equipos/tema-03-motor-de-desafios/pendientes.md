# Tema 03 — Motor de Desafíos — pendientes

> Fuente completa: [17 §5](../../17-mapa-de-integracion.md#5-camino-asincrónico--el-evaluador)
> líneas 353-377, [17 §8](../../17-mapa-de-integracion.md#8-lo-que-estos-diagramas-dejaron-a-la-vista)
> I-04/I-05, [08 B-5](../../08-decisiones-y-pendientes.md).

## 🔴 Cruzado — I-04: cómo llega el score al motor de desafíos

**El hallazgo más grande del mapa de integración.** Cuatro documentos describen cuatro
mecanismos distintos y ninguno tiene payload definido: el evento `score_de_ia_calculado` (sin
tópico ni versión fijados en algún documento), un `POST /internal/ai-result` que aparece una
sola vez (HTTP directo entre microservicios — justo lo que el corpus llama no negociable),
polling a `GET /ai/jobs/:id`, y un `POST` del worker al backend Spring sin contraparte descrita.

**El Tema 03 no puede empezar su lado hasta que se elija uno.**

**Nuestra propuesta de apertura:** el evento Kafka `score_de_ia_calculado.v1`, que ya es
contrato ejecutable en [`llm-service-v1.asyncapi.yaml`](../../contracts/llm-service-v1.asyncapi.yaml).
Un solo mecanismo, no cuatro.

**Decide:** sesión de integración, primer ítem de la agenda — ver
[`docs/entregas/sesion-integracion-agenda.md`](../../entregas/sesion-integracion-agenda.md) ítem 1.

## 🔴 Cruzado — I-05: qué enum viaja en `estado`

Compartido con Tema 11 — ver [`docs/entregas/sesion-integracion-agenda.md`](../../entregas/sesion-integracion-agenda.md) ítem 2.

## 🟡 Ya resuelto conceptualmente, falta escribirlo en el contrato (B-5)

Cuatro puntos, ya coincididos con la cátedra pero sin volcar al YAML todavía: nosotros
devolvemos score 0-100 y nunca XP; Tema 03/10 aplica PAR-05; nosotros exponemos el contador de
pendientes (`GET /ai/pendientes/{curso_cohorte_id}`); **el backend implementa la degradación de
RF-IA-27** (aceptar la entrega con nuestro servicio caído) — ese último es el que más se cae
entre equipos en la práctica.
