# Historias de usuario — EP-04 · Calibración y gobernanza del modelo

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../../10-plantillas/historia-de-usuario-taiga.md). **Carpeta nueva**, creada al
> documentar a posteriori lo que el commit `605f381` ya construyó — ver
> [decision-605f381.md](../../../../01-vision-alcance-y-entrega/03-entregas/decision-605f381.md) y
> [verificacion-v2-golden-set-calibracion.md](../../../../01-vision-alcance-y-entrega/03-entregas/verificacion-v2-golden-set-calibracion.md).
>
> **`LLM-S03-H01` tiene un hueco de implementación real** (falta conectar el puerto de
> invocación de modelos, `LLM-S01-H10`) — está marcado explícitamente en la ficha, no asumir
> que está terminada por tener CA escritas.
>
> **Fuente de verdad.** Épica: [`../../epicas/ep-04.md`](../../epicas/ep-04.md). DoR/DoD:
> [23 · §9.2](../../../03-plan-de-construccion-del-producto.md).

## Índice

| ID | Título | Tipo | Pareja | Dep. | Estado |
|---|---|---|---|---|---|
| [LLM-EP04-H01](h01.md) | Correr y activar una calibración de curso con métrica PAR-14 *(ex-S03-H01)* | **HU** de valor | P4 | EP-02·H01 (✅), EP-03·H04/H05 (S2) | 🟢 Completa — T7 (conectar el modelo) cerrada el 2026-09-13, ver [sprint de cierre](../../sprints/sprint-1/s1-cierre.md) |
| [LLM-EP04-H02](h02.md) | Correr una calibración de plataforma y ver su reporte por dimensión *(ex-S04-H01)* | **HU** de valor | P4 | EP-02·H01 (✅), EP-03·H04/H05 (🟢), H01 (motor, 🟢 — se reutiliza) | ⚪ Borrador |
| [LLM-EP04-H03](h03.md) | Que una calibración vigente venza y deje de confiarse ciegamente *(ex-S04-H02)* | **HU** de valor | P4 | H01, H02 (EP-04) | ⚪ Borrador |
| [LLM-EP04-H04](h04.md) | Enterarme de que mi curso tiene evaluaciones frenadas por falta de calibración *(ex-S04-H03)* | **HU** de valor | P4 | compuerta `pending_evaluations` (🟢) | ⚪ Borrador |

> **Diferencia de alcance con la épica.** EP-04 describe calibración de **plataforma** (ADMIN) y
> de **curso** (docente); el código de S3 solo implementa la segunda. `LLM-EP04-H02` es la
> propuesta para cerrar ese hueco — falta pasarla por Refinamiento (estimación, prioridad y
> confirmar el diseño de datos) antes de comprometerla en una Planning.
