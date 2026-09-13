# Historias de usuario — EP-04 · Calibración y gobernanza del modelo

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../plantillas/historia-de-usuario-taiga.md). **Carpeta nueva**, creada al
> documentar a posteriori lo que el commit `605f381` ya construyó — ver
> [decision-605f381.md](../../entregas/decision-605f381.md) y
> [verificacion-v2-golden-set-calibracion.md](../../entregas/verificacion-v2-golden-set-calibracion.md).
>
> **`LLM-S03-H01` tiene un hueco de implementación real** (falta conectar el puerto de
> invocación de modelos, `LLM-S01-H10`) — está marcado explícitamente en la ficha, no asumir
> que está terminada por tener CA escritas.
>
> **Fuente de verdad.** Épica: [`../../epicas/ep-04.md`](../../epicas/ep-04.md). DoR/DoD:
> [23 · §9.2](../../23-plan-construccion-producto-llm.md).

## Índice

| ID | Título | Tipo | Pareja | Dep. | Estado |
|---|---|---|---|---|---|
| [LLM-S03-H01](s03-h01.md) | Correr y activar una calibración de curso con métrica PAR-14 | **HU** de valor | P4 | H10 (EP-02, ✅), H01/H02 (EP-03, S2) | 🟡 Esqueleto construido; falta conectar el modelo (T7, **14 h, programada en [sprint de cierre](../../sprints/s1-cierre.md)**) |

> **Diferencia de alcance con la épica**, marcada en la ficha: EP-04 describe calibración de
> **plataforma** (ADMIN) y de **curso** (docente); el código solo implementa la segunda. A
> confirmar con quien mantiene el módulo.
