# Integración del material de investigación como contenido propio — 2026-09-22

## Decisión anterior

`08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/` guardaba seis
subcarpetas (`especificacion-tecnica/`, `planes-de-ejecucion/`, `investigacion-jailbreak/`,
`fundamentos-defensa-oral/`, `gestion-roadmap-equipo/`, `rubricas-prompts-borrador/`) bajo un
nivel intermedio `importado/`, con un `README.md` y un `CORRECCIONES-SUGERIDAS.md` propios que
documentaban rama de origen, autoría y fecha de importación, y anotaban discrepancias sin
aplicarlas al texto.

## Motivo

`docsV3` pasó a ser la única fuente de documentación del proyecto. Mantener un marco de "material
importado de otra rama" ya no aporta nada a quien recién llega a leer la documentación; lo único
con valor real eran las tablas de equivalencia de nombres y las correcciones detectadas. Se
decidió eliminar el marco histórico e integrar las seis carpetas como contenido propio de
`04-investigacion-y-material/`, en el mismo nivel que `presentaciones/`, `prototipos/`,
`recomendaciones/` y `referencias/`.

## Regla vigente

- Las seis carpetas quedan como hermanas directas dentro de `04-investigacion-y-material/`; el
  nivel `importado/` desaparece.
- Ningún archivo de esas carpetas define contratos ni comportamiento vigente: la fuente de verdad
  sigue siendo `00-gobierno-y-evolucion/`, `contracts/` y `06-operacion-calidad-y-pruebas/`, como
  indica el `README.md` de `04-investigacion-y-material/`.
- Las notas de "lectura histórica" al inicio de los documentos de `fundamentos-defensa-oral/`,
  `especificacion-tecnica/` y `gestion-roadmap-equipo/` se reformularon como notas de terminología
  directas (equivalencia de nombre antiguo → nombre vigente), sin el marco de "versión anterior /
  documento superado".

## Equivalencias de nombres (antiguo → vigente)

| Nombre antiguo | Nombre / referencia vigente |
|---|---|
| `ms-evaluacion-llm` | `llm-service` |
| RabbitMQ / AMQP (bus del Tema 11) | Kafka |
| Motor interno en Python (FastAPI) | Java Spring Boot (ADR-005) |
| `04_GESTION_ROADMAP_Y_EQUIPO/01_ALCANCE_Y_FRONTERAS_INTER_EQUIPOS.md` | `01-vision-alcance-y-entrega/01-problema-y-alcance.md` |
| `02_FUNDAMENTOS_Y_DEFENSA_ORAL/02_DEBATE_ARQUITECTURA_JAVA_VS_PYTHON.md` | `02-arquitectura-y-plataforma/01-arquitectura-y-stack.md` |
| `02_FUNDAMENTOS_Y_DEFENSA_ORAL/03_ANALISIS_ECONOMICO_Y_CONTEXTO_LLM.md` | `03-capacidades-de-ia/01-modelos-costos-y-contexto.md` |
| `03_RUBRICAS_PROMPTS_Y_CALIBRACION/02_ESPECIFICACION_FUNCIONES_Y_JUECES.md` | `03-capacidades-de-ia/02-funciones-de-ia.md` |
| `02_FUNDAMENTOS_Y_DEFENSA_ORAL/08_SEGURIDAD_FRONTERAS_Y_GUARDARRAILES.md` | `04-seguridad-datos-y-cumplimiento/01-seguridad-y-guardarrailes.md` |
| `02_FUNDAMENTOS_Y_DEFENSA_ORAL/04_OPERACION_INGENIERIA_Y_CARGA.md` | `06-operacion-calidad-y-pruebas/01-operacion-e-ingenieria.md` |
| `02_FUNDAMENTOS_Y_DEFENSA_ORAL/05_DATOS_TRAZABILIDAD_Y_TERMINOS_LEGALES.md` | `04-seguridad-datos-y-cumplimiento/02-datos-retencion-y-terminos.md` |
| `04_GESTION_ROADMAP_Y_EQUIPO/02_DECISIONES_ABIERTAS_Y_PENDIENTES.md` | `00-gobierno-y-evolucion/02-decisiones-y-pendientes.md` |
| `02_FUNDAMENTOS_Y_DEFENSA_ORAL/01_PREGUNTAS_Y_RESPUESTAS_DEFENSA.md` | `08-preguntas-investigacion-y-sincronizaciones/01-preguntas-y-respuestas.md` |
| `04_GESTION_ROADMAP_Y_EQUIPO/03_PLAN_DE_TRABAJO_12_PASOS_6_PERSONAS.md` | `01-vision-alcance-y-entrega/02-entregables-y-plan.md` |
| `04_GESTION_ROADMAP_Y_EQUIPO/04_GLOSARIO_Y_METADATA_INTEGRACION.md` | `00-gobierno-y-evolucion/03-glosario-y-metadata.md` |
| `02_FUNDAMENTOS_Y_DEFENSA_ORAL/06_ALMACENAMIENTO_E_INGESTA_MULTIMODAL.md` | `03-capacidades-de-ia/rag-e-ingesta/01-almacenamiento-e-ingesta.md` |
| `03_RUBRICAS_PROMPTS_Y_CALIBRACION/01_RUBRICA_ANCLAS_Y_PROMPTS_COMPLETOS.md` | `03-capacidades-de-ia/03-rubricas-y-prompts.md` |
| `02_FUNDAMENTOS_Y_DEFENSA_ORAL/07_HISTORIAL_DE_SINCRONIZACION_Y_CONFLICTOS.md` | `08-preguntas-investigacion-y-sincronizaciones/02-sincronizacion-con-guia-didactica.md` |

## Contradicciones y correcciones detectadas (no aplicadas al texto de origen)

1. **Bus de eventos:** `especificacion-tecnica/` (ADR, arquitectura híbrida, glosario) especifica
   RabbitMQ / AMQP (`:5672`, `@RabbitListener`, `RabbitTemplate`, "Workers Celery & Listener
   AMQP"). La decisión vigente del equipo es que el bus del Tema 11 corre sobre **Kafka**, y Kafka
   no se reusa para la cola interna de trabajo diferido (esa cola va en Postgres con
   `SKIP LOCKED`, o Redis). Lo mismo aplica a
   `especificacion-tecnica/07_REGISTRO_DE_DECISIONES_ADR.md`, que además propone un motor interno
   en Python (FastAPI) — descartado por ADR-005, el servicio es Java Spring Boot.
2. **Rúbrica y pesos:** `rubricas-prompts-borrador/01_RUBRICA_ANCLAS_Y_PROMPTS_COMPLETOS.md`
   declara los cinco pesos fijos a nivel plataforma, sin edición docente ni por curso. La
   especificación funcional vigente (`03-capacidades-de-ia/golden-set-y-calibracion/`) permite
   editar pesos en borrador siempre que sumen 100 %, con dimensiones/subcriterios para
   subcalibración. Rige `docsV3`; el archivo se mantiene como **BORRADOR**, sin aplicar sus pesos
   ni anclas.
3. **Corrector LLM:** `rubricas-prompts-borrador/02_ESPECIFICACION_FUNCIONES_Y_JUECES.md` propone
   un corrector LLM para respuestas abiertas y extenderle la maquinaria de calibración del
   evaluador. La especificación funcional vigente delimita el Golden Set al uso del tutor y
   reserva la aprobación académica a reglas determinísticas del dominio o revisión docente. Rige
   `docsV3`; la propuesta no amplía alcance ni crea un corrector vigente.
4. **Solapamiento no armonizado:** `especificacion-tecnica/` e `investigacion-jailbreak/` cubren
   temas que también tratan `02-arquitectura-y-plataforma/` y `04-seguridad-datos-y-cumplimiento/`,
   a veces con números o decisiones distintas. Cuando difieren, manda la documentación de trabajo
   vigente (`00-gobierno-y-evolucion/`, `02-arquitectura-y-plataforma/`,
   `04-seguridad-datos-y-cumplimiento/`); el material de esta carpeta queda como profundización y
   apoyo de defensa, no como contrato.

## Fuentes

Contenido y notas de `04-investigacion-y-material/importado/README.md` y
`04-investigacion-y-material/importado/CORRECCIONES-SUGERIDAS.md` (ambos eliminados en esta
migración), más las tablas de equivalencia repetidas en el `README.md` de
`fundamentos-defensa-oral/`.

## Documentos V3 corregidos o agregados

**Movidos** (sin nivel `importado/`, contenido sin reescribir salvo notas de encabezado):
`08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/{especificacion-tecnica,planes-de-ejecucion,investigacion-jailbreak,fundamentos-defensa-oral,gestion-roadmap-equipo,rubricas-prompts-borrador}/`.

**Eliminados:**
`08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/importado/{README.md,CORRECCIONES-SUGERIDAS.md}`.

**Actualizados:** `04-investigacion-y-material/README.md`,
`04-investigacion-y-material/presentaciones/README.md`,
`04-investigacion-y-material/presentaciones/CORRECCIONES-SUGERIDAS.md`,
`04-investigacion-y-material/presentaciones/mapa-conceptual-interactivo.html`,
`04-investigacion-y-material/presentaciones/defensa-39-slides.html`,
`03-capacidades-de-ia/golden-set-y-calibracion/README.md` (corrección de ruta),
encabezados de los documentos de `fundamentos-defensa-oral/`, `especificacion-tecnica/` y
`gestion-roadmap-equipo/` que traían nota de "lectura histórica", y
`registro/2026-09-22-importacion-gestion-y-rubricas-borrador.md` (rutas sin `importado/`).
