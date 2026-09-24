# Contratos por equipo

Esta carpeta reúne las vistas narrativas completas de los contratos de integración de `llm-service`
(Tema 07), una por cada contraparte. El contenido específico de cada relación se conserva aquí:
responsabilidades, datos que recibimos, datos que devolvemos, invocación HTTP, eventos asíncronos,
errores, dependencias, secuencias y pendientes. No es un resumen.

Los schemas ejecutables se definen una sola vez en [`../llm-service.openapi.yaml`](../llm-service.openapi.yaml)
y [`../llm-service.asyncapi.yaml`](../llm-service.asyncapi.yaml). Las vistas de equipo los enlazan
en lugar de copiar sus objetos JSON, de manera que una corrección de campos se haga en un único
lugar y se registre en [`registro/`](../../registro/README.md).

## Orden recomendado de lectura

1. [`../00-mapa-de-integracion.md`](../00-mapa-de-integracion.md) — mapa de actores, canales y
   estado de cada acuerdo.
2. [`../requisitos-a-otros-micros.md`](../requisitos-a-otros-micros.md) — entradas que Tema 07
   necesita recibir para poder responder o cerrar una evaluación.
3. La vista del equipo que origina o consume el flujo que se está implementando.
4. El schema ejecutable correspondiente (OpenAPI para HTTP o AsyncAPI para Kafka).
5. Los `pendientes.md` de la carpeta de trabajo del equipo, cuando se necesite revisar preguntas
   aún abiertas o tareas internas.

## Índice de contrapartes

| Archivo canónico | Contraparte | Qué cubre |
|---|---|---|
| [`backend-de-negocio.md`](backend-de-negocio.md) | Backend de negocio | Persistencia, orquestación interna y coordinación de casos de uso. |
| [`frontend-angular.md`](frontend-angular.md) | Front End Angular / Workbench | Estados visibles, invocaciones y respuesta que el workbench debe representar. |
| [`product-owner.md`](product-owner.md) | Product Owner | Decisiones de alcance, criterios y acuerdos que requieren validación de producto. |
| [`tema-02-cursos-y-matricula.md`](tema-02-cursos-y-matricula.md) | Tema 02 — Cursos y Matrícula | Contexto de curso, cohorte y matrícula usado por el servicio. |
| [`tema-03-motor-de-desafios.md`](tema-03-motor-de-desafios.md) | Tema 03 — Motor de Desafíos | Sin contrato directo vigente; el flujo de práctica se media por Tema 05. |
| [`tema-04-desafios-teoricos.md`](tema-04-desafios-teoricos.md) | Tema 04 — Desafíos Teóricos | Recomendación y puntos de coordinación todavía sin contrato técnico. |
| [`tema-05-desafios-practicos.md`](tema-05-desafios-practicos.md) | Tema 05 — Desafíos Prácticos | Tutor socrático síncrono, cierre de intento y score asíncrono. |
| [`tema-05-mensaje-de-integracion.md`](tema-05-mensaje-de-integracion.md) | Tema 05 — mensaje para integrar | Texto listo para pegar al equipo; remite al Skill Hub (skill v4, contratos v5) y a la guía v2. |
| [`tema-06-sandbox-ejecucion.md`](tema-06-sandbox-ejecucion.md) | Tema 06 — Sandbox de ejecución | Sin contrato directo; artefactos y secretos de ejecución quedan en Tema 06. |
| [`tema-08-microservicio-por-confirmar.md`](tema-08-microservicio-por-confirmar.md) | Tema 08 — por confirmar | Sin fuentes disponibles para identificar la contraparte. |
| [`tema-09-microservicio-por-confirmar.md`](tema-09-microservicio-por-confirmar.md) | Tema 09 — por confirmar | Sin fuentes disponibles para identificar la contraparte. |
| [`tema-10-xp-economia.md`](tema-10-xp-economia.md) | Tema 10 — XP / economía (denominación por confirmar) | Sin contrato directo; el score llega a Tema 05, que media hacia economía. |
| [`tema-11-chat.md`](tema-11-chat.md) | Tema 11 — Chat | Invocación del tutor desde chats de alumnos y moderación. |
| [`tema-12-backoffice-admin.md`](tema-12-backoffice-admin.md) | Tema 12 — Backoffice / ADMIN | Calibración, golden set, rúbricas editables y operaciones administrativas. |
| [`llm-service-contrato-para-desafios-practicos.md`](llm-service-contrato-para-desafios-practicos.md) | Tema 05 — Desafíos Prácticos | Contrato detallado adicional citado desde [`../skillhub/README.md`](../skillhub/README.md). |
| [`tema-05-skill-integrar-llm-service/SKILL.md`](tema-05-skill-integrar-llm-service/SKILL.md) | Tema 05 — Skill de integración | Skill ejecutable para integrar `llm-service` desde Tema 05. |

## Fuente y actualización

Cuando se acuerda un cambio, se modifica la vista canónica del equipo y el schema afectado en el
mismo cambio, si corresponde. La entrada de [`registro/`](../../registro/README.md) debe indicar qué decía antes, por qué se
modificó y qué dice ahora. No se agrega una adenda para reemplazar una regla vigente.

Los archivos `contratos.md` que siguen visibles bajo
`07-planificacion-y-trabajo-equipo/11-equipos/` son punteros deliberados para conservar URLs y el
recorrido de lectura previo. Su única función es llevar aquí; no contienen información contractual
duplicada.
