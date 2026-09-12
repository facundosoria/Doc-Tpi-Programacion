# Contexto de curso (`GET /api/llm/courses`) — sin ficha de historia

- **Estado:** ⚪ No auditado en profundidad
- **Épica tentativa:** ninguna — parece un endpoint utilitario/transversal, no una función de
  negocio de una épica específica
- **Código:** `CourseContextController.list`
- **Evidencia:** [`CORRECCIONES-SUGERIDAS.md` ítem 11](../../../CORRECCIONES-SUGERIDAS.md)
  (nota que `GET /api/llm/courses` existe en el código pero no aparece en ningún contrato
  OpenAPI publicado)

## Qué se sabe

Ninguna auditoría previa (`verificacion-v2-golden-set-calibracion.md` incluida) leyó este
controller a fondo — solo se lo detectó al listar todos los controllers del servicio y al cruzar
rutas reales contra el contrato v2. Sin veredicto de estado todavía.
