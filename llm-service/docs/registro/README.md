# Registro de cambios V2

Cada cambio aprobado debe registrar: fecha, decisión anterior, motivo, regla vigente, fuentes,
documentos V2 corregidos, documentos históricos relacionados, responsable y evidencia de prueba.

El primer registro a migrar será la decisión de rúbricas editables y versionadas aprobada por el
Product Owner.

## Registros

| Fecha | Registro | Qué decidió |
|---|---|---|
| 2026-09-14 | [inicio-docsv2](2026-09-14-inicio-docsv2.md) | Arranque de la documentación V2 |
| 2026-09-15 | [consolidacion-repeticiones](2026-09-15-consolidacion-repeticiones.md) | Consolidación de documentos repetidos |
| 2026-09-20 | [estandar-kafka-del-pdf](2026-09-20-estandar-kafka-del-pdf.md) | Se adopta el envelope Kafka de la cátedra |
| 2026-09-21 | [integracion-main-a-dev](2026-09-21-integracion-main-a-dev.md) | Una sola rama: arquitectura de `main`, funcionalidades de `dev` |
| 2026-09-21 | [revision-ep01-h01](2026-09-21-revision-ep01-h01.md) | ADR-003 supersede a ADR-001; variables de entorno y `ArchitectureTest` al día |
| 2026-09-21 | [revision-ep01-h02](2026-09-21-revision-ep01-h02.md) | `up` real: precondiciones del entorno, smoke autocontenido, 204 s en frío |
| 2026-09-21 | [revision-ep01-h03](2026-09-21-revision-ep01-h03.md) | `404` real para «no existe» (T9) y gate de cobertura recuperado |
| 2026-09-21 | [revision-ep01-h04](2026-09-21-revision-ep01-h04.md) | Esquema: `FlywaySchemaTest` roto y reproducibilidad desde cero probada de verdad |
| 2026-09-21 | [revision-ep01-h05](2026-09-21-revision-ep01-h05.md) | Contrato OpenAPI inválido (3.0 en 3.1) corregido, URL del mock y contrato-vs-código automatizado |
| 2026-09-21 | [revision-ep01-h06](2026-09-21-revision-ep01-h06.md) | Script de reinicio y guía de demo rotos por la integración: corregidos y ejecutados |
| 2026-09-21 | [revision-ep01-h07](2026-09-21-revision-ep01-h07.md) | Kafka: 5 CA verificados; comportamientos de `main` sin disparador y AsyncAPI que declara de más |
| 2026-09-22 | [integracion-material-investigacion](2026-09-22-integracion-material-investigacion.md) | Se disuelve `importado/`: las seis carpetas pasan a ser contenido propio de `04-investigacion-y-material/` |
| 2026-09-22 | [importacion-gestion-y-rubricas-borrador](2026-09-22-importacion-gestion-y-rubricas-borrador.md) | Se importan los documentos de gestión/roadmap y rúbricas-prompts borrador faltantes en `04-investigacion-y-material/` |
| 2026-09-22 | [skills-y-calibracion-jerarquica](2026-09-22-skills-y-calibracion-jerarquica.md) | Se adopta rúbrica jerárquica con subcriterios ponderables y skills adjuntables (166 directivas D-01–D-166), evoluciona el modelo de cinco dimensiones fijas |
| 2026-09-22 | [coverage-matrix-agente-A-skills-calibracion](coverage-matrix-agente-A-skills-calibracion.md) | Trazabilidad de cobertura D-01–D-20, D-39–D-77, D-120–D-161 (skills y calibración jerárquica) |
| 2026-09-22 | [coverage-matrix-agente-B-cuotas-contratos](coverage-matrix-agente-B-cuotas-contratos.md) | Trazabilidad de cobertura D-21–D-38, D-78–D-119, D-162–D-166 (cuotas, contratos y notificaciones) |
| 2026-09-22 | [actualizacion-contratos-equipos-02-y-12](2026-09-22-actualizacion-contratos-equipos-02-y-12.md) | Se corrigen rutas y envelope Kafka retirados en `tema-02` y `tema-12`; se agrega RQ-CS-03 (catálogo de Courses) y los contratos "sin contrato directo" de Temas 06, 08, 09 y 10 |
| 2026-09-22 | [unificacion-demo-en-laboratorio](2026-09-22-unificacion-demo-en-laboratorio.md) | `demo/` se unifica en `lab/`; se corrige el bug de puerto del gateway-mock (502) y se agrega `scripts/up.sh`/`down.sh` |
| 2026-09-22 | [limpieza-historicos-contracts](2026-09-22-limpieza-historicos-contracts.md) | Se retiran `90-*`, `91-*` y `historicos-y-contratos-v1/` de `docs/contracts/`; se rescata la propuesta de streaming SSE del tutor (no era histórica) y se corrigen ~55 citas |
| 2026-09-22 | [reubicacion-plan-skills-calibracion](2026-09-22-reubicacion-plan-skills-calibracion.md) | `Plan_skills_calibracion.md` y su prompt de implementación se mueven de la raíz de `llm-service/` a `golden-set-y-calibracion/` |
| 2026-09-22 | [auditoria-4-agentes-remanentes-docsv2-v3](2026-09-22-auditoria-4-agentes-remanentes-docsv2-v3.md) | 4 agentes en paralelo auditan todo `llm-service/` por remanentes de `docsV2`/`docsV3`/`demo`/históricos; 19 hallazgos corregidos (código Java, scripts, `compose.yaml`, `AGENTS.md`, HTML histórico) |
| 2026-09-24 | [fallos-preexistentes-suite-workbench](2026-09-24-fallos-preexistentes-suite-workbench.md) | 3 tests pre-existentes de `llm-workbench` que fallan (summary-page ×2, evaluator-shell ×1), ajenos a la US #174: reportados y pendientes del equipo dueño |
