# Contexto de curso (`GET /api/llm/courses`) — resuelto: el endpoint ya no existe

- **Estado:** 🕓 Histórica — el código que describe esta ficha se borró
- **Épica tentativa:** ninguna — era un endpoint utilitario del workbench, no una función de
  negocio de una épica específica
- **Código:** `CourseContextController.list` (borrado) + `WorkbenchDemoCatalog` (borrado)
- **Evidencia:** [`CORRECCIONES-SUGERIDAS.md` ítem 11](../codigo-ejemplo/fuentes/CORRECCIONES-SUGERIDAS.md)
  (notaba que `GET /api/llm/courses` existía en el código pero no aparecía en ningún contrato
  OpenAPI publicado)

## Qué pasó

La duda que abrió esta ficha —un endpoint sin contrato ni épica— quedó resuelta por eliminación.
El commit `495f148a` de `main` borró `CourseContextController` y `WorkbenchDemoCatalog`, y la
[integración del 2026-09-21](../../../registro/2026-09-21-integracion-main-a-dev.md) adoptó esa
decisión: **la pertenencia a un curso ya no se responde desde un catálogo en memoria**, se resuelve
contra courses-service real vía `GatewayCoursesMembershipClient` (y, en el laboratorio local,
contra el `courses-mock` de MockServer).

Que el endpoint no estuviera en ningún contrato publicado resultó ser el dato correcto: nadie
externo dependía de él. No hay nada que auditar ni que migrar.
