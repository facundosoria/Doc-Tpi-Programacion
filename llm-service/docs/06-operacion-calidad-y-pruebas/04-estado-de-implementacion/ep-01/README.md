# EP-01 · Plataforma, contratos e integración — estado

> Fichas fuente: [`docs/historias/ep-01/`](../../../07-planificacion-y-trabajo-equipo/09-epicas-historias-tareas-sprints/historias/ep-01/README.md). Auditoría de
> código: [`llm-service/CORRECCIONES-SUGERIDAS.md`](../codigo-ejemplo/fuentes/CORRECCIONES-SUGERIDAS.md)
> ítems 16–22 (primera vez que estos habilitadores se contrastan contra código; hasta el
> 2026-09-12 ninguna ficha de EP-01 tenía marcador de estado).

## Índice

| ID | Título | Estado | Nota en una línea |
|---|---|---|---|
| [H01](h01.md) | ADR de arquitectura y convenciones técnicas | 🟢 | ADR-001 y ADR-002 aceptados el 2026-09-19 (aprobación directa, sin PR formal); `ArchitectureTest` en verde |
| [H02](h02.md) | Entorno reproducible con un comando | 🟢 | CA1–CA4 y T7 verificados con evidencia real 2026-09-16; se agregó volumen persistente de Postgres (gap real) |
| [H03](h03.md) | Esqueleto transversal del servicio | 🟡 | Re-verificada 2026-09-21: `404` real (T9) cerrado, gate de cobertura recuperado; CA1 (Eureka/Gateway compartidos) y CA6 (CI) siguen abiertos |
| [H04](h04.md) | Esquema inicial versionado con auditoría | 🟢 | `V1` cumple; re-verificada 2026-09-21: `FlywaySchemaTest` estaba roto (corregido) y CA2 ahora se prueba con dos bases desde cero |
| [H08](h08.md) *(hoy H05)* | Contrato OpenAPI y mock del golden set publicados | 🟡 | Re-verificada 2026-09-21: contrato estaba inválido (3.0 en 3.1) y la URL del mock mal — corregidos; `OpenApiContractTest` nuevo; falta evidencia de la aprobación de `admin-service` (CA3) |
| [H07](h07.md) *(propuesta)* | Esqueleto de mensajería Kafka con deduplicación | 🟢 | Re-verificada 2026-09-21: 5 CA con evidencia (4 ITs con EmbeddedKafka + reinicio de Compose); el consumidor perdió 2 comportamientos de `main` sin disparador hasta que Tema 05 defina `challengeId`; el AsyncAPI declara eventos aún no construidos |
| [H09](h09.md) *(hoy H06)* | Suite de pruebas y guía de demo de S1 | 🟡 | Re-verificada 2026-09-21: `mvn verify` 688 + 99 en verde con gate cumplido; script de reinicio y guía de demo estaban rotos por la integración (corregidos y ejecutados); sin CI |

## Pendiente para cerrar EP-01 al 100 %

- **CI:** el workflow `llm-service-ci.yml` se retiró el 2026-09-19 (`28a8b5c1`). Sin él, H03·CA6, H06·CA1 y H06·CA5 solo se cumplen localmente con `mvn verify`.
- **H03·CA1:** registro en el Eureka y ruteo por el Gateway compartidos sin verificar de punta a punta (infraestructura de la plataforma).
- **H07:** integración con el broker Kafka real y nombres de tópico definitivos.
