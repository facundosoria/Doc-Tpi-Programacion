# Plantilla — Épica (Wiki / backlog de Taiga)

> Template oficial de Épica. Uso **obligatorio** para uniformidad del backlog.
>
> Copiar este contenido al crear una épica nueva. Reemplazar `GXX` por el número de
> grupo/equipo y borrar los textos de ejemplo antes de publicar.
>
> Las épicas **no se estiman** ni se comprometen a un sprint: se cierran cuando todas
> sus historias pasan la Definition of Done. El template de épica **no** usa
> `Como / Quiero / Para` ni BDD.

---

# Épica EP-0X — [NOMBRE]

> Ficha en el formato del template oficial de Épica de la Wiki de Taiga. Copiar desde
> `# [GXX] — EP-0X: …` al crear la épica en el módulo **Epics** de Taiga; reemplazar
> `GXX` por el número de grupo. Catálogo y fuente de verdad: `backlog/backlog-ejecutable.md`.
>
> | Campo | Valor |
> |---|---|
> | Pareja / equipo líder | **[P_ · nombre]** |
> | Sprints donde aporta | **[S1, S3, …]** |
> | Requisitos (orientativo) | [RF-* / PAR-*] |
> | Historias iniciales | [`Sxx-H01` … o «a desglosar en el Refinamiento»] |
> | Se cierra cuando | Todas sus historias cumplen la DoD; las épicas no se estiman ni se comprometen. |

---

# [GXX] — EP-0X: [TÍTULO DEL ÉPICO]

## Objetivo

[1–2 líneas: qué valor de negocio / usuario entrega la épica. En términos observables,
no de implementación.]

## Suposiciones y Restricciones

- **Suposiciones:** [lo que se da por cierto para que la épica tenga sentido]
- **Restricciones (legales / técnicas / académicas):** [límites que acotan cómo puede
  resolverse]

## Criterios de Aceptación a nivel Épico

- El conjunto mínimo de historias permite [flujo extremo a extremo, nombrado].
- KPIs iniciales alcanzan: [valores] *(o «a definir» si el equipo aún no los fijó)*.
- Sin regresiones críticas en [áreas / sistemas que la épica toca].
- Observabilidad y alertas configuradas (logs, métricas, trazas) donde aplique.
- Documentación de uso y operación publicada.

> Ajustar la lista a la épica concreta: borrar lo que no aplique, no dejar ítems de
> relleno.

## Dependencias / Impactos

- **Servicios / APIs:** [listar]
- **Módulos afectados:** [listar]
- **Otros equipos:** [listar]
- **Impacto en datos / migraciones:** [detallar]
- **Feature toggles / flags:** [sí / no, plan de retiro]
