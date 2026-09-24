# Tareas SMART — EP-05 · Tutor seguro y guardarraíles

> Una ficha por historia, igual que [`../../historias/ep-05/`](../../historias/ep-05/README.md).
> Formato del [template de Tarea de Taiga](../../../10-plantillas/tarea-taiga.md); método **SMART**
> ([`../README.md`](../README.md)). Cada tarea cuelga en Taiga de su Historia de Usuario bajo
> EP-05.
>
> **Horas.** H01 y H02 son referencia retroactiva (código ya construido, ver
> [`../../historias/ep-05/h01.md`](../../historias/ep-05/h01.md) y
> [`h02.md`](../../historias/ep-05/h02.md)); las tareas quedan marcadas ✅ hecho / ⚠️ pendiente
> según lo que documentan sus fichas de estado de implementación. H03 está en borrador, sin
> código: sus 23 h caen dentro del rango 20–25 h que ya trae la propia historia.

## Índice

| Historia | Archivo | Tareas | h | Estado |
|---|---|---:|---:|---|
| LLM-EP05-H01 *(LLM-S05-H01)* | [`h01.md`](h01.md) | 9 | 28 | 🟢 8 hechas · 1 pendiente (CA6: prueba dedicada) |
| LLM-EP05-H02 *(LLM-S05-H02)* | [`h02.md`](h02.md) | 8 | 26 | 🟢 7 hechas · 1 pendiente (integración real con Postgres, bloqueada por Docker) |
| LLM-EP05-H03 *(sin alias histórico)* | [`h03.md`](h03.md) | 8 | 23 | ⚪ 0 hechas — historia en borrador |
| **Total** | | **25** | **77** | |

## Cómo se generaron

Con el skill [`generar-tareas`](../../../../../../.agents/skills/generar-tareas/SKILL.md): se releyeron
los CA y los escenarios BDD de cada historia padre
([`../../historias/ep-05/`](../../historias/ep-05/README.md)), se agruparon por paso de
construcción (contrato → dominio/migración → caso de uso → adaptadores → seguridad y
resiliencia → observabilidad → prueba E2E → demo y evidencia) y se cuadraron las horas contra
la referencia de cada historia. Los casos negativos de H01/H02/H03 (jailbreak, fuga, acceso
ajeno, desafío cerrado/inexistente) tienen cada uno su propia tarea explícita, no se dan por
incluidos en el camino feliz.
