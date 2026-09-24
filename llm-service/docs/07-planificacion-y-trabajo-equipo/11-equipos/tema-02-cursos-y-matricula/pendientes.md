# Tema 02 — Cursos y Matrícula — pendientes

> Fuente completa: 17 §8
> (I-09), [06-operacion-e-ingenieria.md](../../../06-operacion-calidad-y-pruebas/01-operacion-e-ingenieria.md) (runbook de alta de curso).

## 🔴 Cruzado — I-09: de qué clave cuelga el chunk del RAG

`curso_id` / `curso_cohorte_id` / `curso_template_id` — si Tema 02 modela sin esta clave,
después no hay forma de acotar sin migrar datos.

**Nuestra propuesta** (ya recomendada en doc 08 B-7): el material didáctico cuelga del
**template** (se reutiliza entre cohortes); calibración y evaluaciones son de la **cohorte**.
El chunk lleva las dos claves. **Confirmar con Tema 02** el ejemplo textual de la cátedra:
"calibración que se copia pero se reaprueba" al clonar un curso.

**Decide:** sesión de integración — ver [`docs/entregas/sesion-integracion-agenda.md`](../../../01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md) ítem 3.

## 🟡 Nuevo — ¿sigue existiendo `POST /ai/ingesta`?

Hallazgo al armar los ejemplos de `contratos.md`: el endpoint de ingesta de material del curso
**no está en el contrato v1 vigente**, solo existía en el contrato de seis endpoints (retirado).
Puede ser intencional (RAG es Fase 2/3, fuera del MVP), pero si Tema 02 todavía cuenta con
mandarnos material por acá, hay que decirlo explícitamente antes de que alguien lo dé por
hecho.

## 🟡 Riesgo operativo ya observado

El runbook de alta de curso (`06-operacion-e-ingenieria.md`) marca como fallo típico que
"el Tema 02 no implementó la consulta" de calibración — por eso el endpoint se entrega
temprano aunque devuelva un mock, para no quedar bloqueados esperando que el otro lado
integre.

## 🔴 Kafka — contrato de `course-events` a cerrar (2026-09-19)

Pendientes surgidos al revisar [`llm-service.asyncapi.yaml`](../../../contracts/llm-service.asyncapi.yaml) v2.0.0.
El código **todavía no consume** `course-events`; el contrato existe pero es un `Envelope` vacío.
Se agregan acá todos los temas nuevos que salgan de la charla.

- [ ] **`CourseArchived` — campos:** hoy sin campos declarados. Propuesta mínima en
  [`contratos/equipos/tema-02-cursos-y-matricula.md`](../../../contracts/equipos/tema-02-cursos-y-matricula.md)
  (`courseCohortId`); confirmar si les alcanza o falta `courseId` / `courseTemplateId` / `archivedAt`.
- [ ] **Message Key de `course-events`:** la define el productor; figura "Pendiente". Confirmar cuál usan.
- [ ] **Otros eventos de curso que necesitemos:** ¿alta/clonado de curso, cambio de cohorte,
  matrícula/baja de alumno? Definir cuáles publican y si los necesitamos (p. ej. clonado → calibración
  "se copia pero se reaprueba").
- [ ] **Semántica de archivado:** confirmar que al archivar frenamos trabajos pendientes de esa
  cohorte (evaluaciones en cola, diferidos) y qué pasa con datos ya generados.
- [ ] **Productor y nombre:** confirmar `producer` (¿`courses-service`?) y `eventType` en formato
  estándar (`COURSE_ARCHIVED`).
- [ ] _(agregar acá lo que surja)_
