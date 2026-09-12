# Propuestas de actualización de golden set — sin ficha de historia

- **Estado:** ⚪ No auditado en profundidad
- **Épica tentativa:** EP-03 (mismo dominio que el golden set por curso)
- **Código:** `GoldenSetUpdateProposalController` + `GoldenSetUpdateProposalRepository`
- **Evidencia:** [`CORRECCIONES-SUGERIDAS.md` ítem 15](../../../llm-service/CORRECCIONES-SUGERIDAS.md)

## Qué se sabe

- El controller devuelve el tipo anidado del repository (`GoldenSetUpdateProposalRepository.
  GoldenSetUpdateProposal`) directo como respuesta HTTP, sin DTO intermedio — mismo patrón que
  otros 6 controllers (ver [`ep-01/h03.md`](../ep-01/h03.md) y
  [`CORRECCIONES-SUGERIDAS.md` ítem 1](../../../llm-service/CORRECCIONES-SUGERIDAS.md)).
- El schema `GoldenSetUpdateProposal` del contrato v2 exige `baseVersion` y `baseCaseCount`; no
  se verificó si el registro real del repository trae esos dos campos poblados.

## Qué falta para auditar esto en serio

Ninguna revisión previa (ni `verificacion-v2-golden-set-calibracion.md` ni este tablero) leyó el
servicio completo ni sus tests. No hay veredicto de estado (🟢/🟡/🔴) todavía — solo la
observación puntual de arriba. Pendiente de una pasada dedicada.
