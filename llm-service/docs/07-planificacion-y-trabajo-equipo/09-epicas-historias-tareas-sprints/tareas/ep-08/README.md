# Tareas SMART — EP-08 · Moderación integrada (F2)

> Una ficha por historia, igual que [`../../historias/ep-08/`](../../historias/ep-08/README.md).
> Formato del [template de Tarea de Taiga](../../../10-plantillas/tarea-taiga.md); método **SMART**
> ([`../README.md`](../README.md)). Cada tarea cuelga en Taiga de su Historia de Usuario bajo
> EP-08.
>
> **Fuente de verdad:** Épica: [`../../epicas/ep-08.md`](../../epicas/ep-08.md).
> Fichas padre de HU: [`../../historias/ep-08/`](../../historias/ep-08/README.md).
> Contrato de referencia: [`../../../contracts/llm-service-v1-moderacion.openapi.yaml`](../../../../contracts/llm-service-v1-moderacion.openapi.yaml).
> Plan recalibrado: [`../../sprints/sprint-4/README.md`](../../sprints/sprint-4/README.md).

## Índice

| Historia | Archivo | Tareas | h | Estado | Pareja |
|---|---|---:|---:|---|:---:|
| LLM-S11-H01 *(Que un mensaje del chat se permita o bloquee antes de entregarse)* | [`h01.md`](h01.md) | 7 | 34 | ⚪ Borrador | P3+P2+P1 |
| LLM-S11-H02 *(Detectar spam, contenido ofensivo e intentos de ocultar código)* | [`h02.md`](h02.md) | 5 | 28 | ⚪ Borrador | P3 |
| LLM-S12-H01 *(Apelar un mensaje que bloqueó la moderación)* | [`h03.md`](h03.md) | 5 | 26 | ⚪ Borrador | P3 |
| LLM-S12-H02 *(Que el docente revise un incidente de moderación y lo resuelva)* | [`h04.md`](h04.md) | 5 | 30 | ⚪ Borrador | P3 |
| LLM-S13-H01 *(Que la moderación siga funcionando en modo degradado si el clasificador falla)* | [`h05.md`](h05.md) | 5 | 24 | ⚪ Borrador | P3+P2 |
| LLM-S13-H02 *(Que la evidencia de moderación se retenga el tiempo necesario y luego se purgue)* | [`h06.md`](h06.md) | 4 | 18 | ⚪ Borrador | P1 |
| **Total** | | **31** | **160** | | |

## Cómo se generaron

Con el skill [`generar-tareas`](../../../../../../.agents/skills/generar-tareas/SKILL.md): se releyeron
los Criterios de Aceptación (CA) y los escenarios BDD de cada historia padre
([`../../historias/ep-08/`](../../historias/ep-08/README.md)), se agruparon por paso de
construcción (contrato → dominio/migración → caso de uso → adaptadores → seguridad y
resiliencia → observabilidad → prueba E2E → demo y evidencia) y se cuadraron las horas contra
la referencia de cada historia. Los casos negativos (timeout, token inválido, discrepancia de
texto con mismo message_id, apelación duplicada, resolución ajena a curso, fallo del modelo)
tienen cada uno su propia tarea técnica explícita.
