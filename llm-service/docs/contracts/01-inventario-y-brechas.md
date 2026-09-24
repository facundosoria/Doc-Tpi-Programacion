# Inventario de contratos y brechas de integración

> Estado relevado contra los schemas canónicos y la implementación actual de
> `llm-service`. Este documento es un inventario de trabajo: no reemplaza a OpenAPI ni AsyncAPI.

## Resumen ejecutivo

La superficie principal ya está definida, pero el conjunto no está cerrado para una integración
productiva. Hay contratos ejecutables, contratos propuestos en la documentación narrativa y
dependencias que todavía no tienen schema.

Los estados significan:

- **Definido:** existe en OpenAPI/AsyncAPI canónico.
- **Parcialmente implementado:** existe el schema, pero el código todavía no cubre todo el flujo.
- **Propuesto:** está documentado, pero requiere aprobación del dueño productor/consumidor.
- **Faltante:** no existe todavía un contrato técnico acordado.

## Contratos definidos

### HTTP — `llm-service.openapi.yaml`

El contrato expone, bajo el server `/api/llm`:

- tutor síncrono para `practice-service`:
  `POST /tutor/interactions`;
- plantillas y versiones de rúbricas institucionales;
- rúbricas por curso, versionado y publicación;
- Golden Sets, casos, importaciones y propuestas de actualización;
- interacciones elegibles, anonimización y casos sintéticos;
- corridas de calibración, grupos de estabilidad, preview y activación;
- calibración activa, asignaciones por desafío y evaluaciones pendientes;
- despliegues de modelos por curso;
- proveedores, credenciales, modelos evaluadores y asignaciones de modelos;
- operaciones de administración consumidas por `admin-service` y el Workbench.

Consumidores principales: `practice-service`, `courses-service` y `admin-service`.

### Eventos Kafka — `llm-service.asyncapi.yaml`

El schema define estos eventos publicados por `llm-service`:

- `score_de_ia_calculado.v1` para `practice-service`;
- `score_pendiente_diferido.v1` para `practice-service` y seguimiento de Courses;
- `calibracion_aprobada.v1` para `courses-service` y `admin-service`;
- `calibracion_fuera_de_tolerancia.v1` para `courses-service` y `admin-service`;
- `incidente_de_jailbreak.v1` para `admin-service` y seguridad.

Todos usan envelope común, correlación en headers Kafka, entrega al menos una vez, outbox y
deduplicación por `eventId`.

## Contratos parciales o pendientes de aprobación

### Entrada desde `practice-service`

`llm-service` necesita recibir:

- `practica_publicada.v1` para asignar la calibración vigente;
- `intento_iniciado.v1` para inmovilizar esa calibración;
- `intento_cerrado.v1` para encolar la evaluación del tutor.

Están descriptos en `requisitos-a-otros-micros.md`, pero todavía no forman parte del AsyncAPI
canónico. El código ya tiene listener para `intento_cerrado.v1`; los otros dos eventos aún no
están implementados.

### Resultado del evaluador

`score_de_ia_calculado.v1` y `score_pendiente_diferido.v1` están definidos en AsyncAPI, pero la
implementación actual sólo evidencia el publisher del diferimiento. Falta completar y probar el
camino de score calculado.

### Integración con `courses-service`

Falta formalizar el contrato que `llm-service` consume para validar pertenencia docente:

```http
GET /api/courses/{courseCohortId}/members/{userId}
```

Debe acordarse su scope, respuesta, rol, estado de matrícula y semántica `403`/`404`.

También falta el evento de cierre o archivo de cohorte, propuesto como:

```text
curso_archivado.v1
```

### Notificaciones

No existe todavía un contrato técnico con `notification-service`. La documentación sólo establece
que ciertos incidentes o resultados deben notificarse.

Hay que definir el canal, evento o endpoint, destinatario, severidad, payload, reintentos y DLQ.
`incidente_de_jailbreak.v1` informa a administración/seguridad, pero no es todavía un contrato de
envío de notificaciones a docentes o alumnos.

## Límites de responsabilidad

- No hay comunicación directa `llm-service` ↔ `challenges-service`.
- `practice-service` recibe el score y lo reenvía al motor de desafíos.
- `llm-service` no otorga XP ni nota académica.
- El antiguo `POST /ai/ingesta` está retirado y no es contrato vigente.
- Las rutas `/ai/**` de documentos históricos no deben publicarse como contratos nuevos.

## Orden recomendado para cerrar las brechas

1. Acordar con `practice-service` el envelope y payload de `intento_cerrado.v1`.
2. Formalizar `practica_publicada.v1` e `intento_iniciado.v1` en AsyncAPI.
3. Completar y probar `score_de_ia_calculado.v1`.
4. Acordar con `courses-service` la consulta de membresía docente.
5. Definir `curso_archivado.v1` y su tratamiento de pendientes.
6. Relevar con `notification-service` el contrato de avisos e incidentes.
7. Revalidar OpenAPI/AsyncAPI y recién entonces publicarlos en SkillHub.
