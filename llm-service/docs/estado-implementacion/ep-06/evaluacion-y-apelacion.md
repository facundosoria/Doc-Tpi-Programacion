# EP-06 — Evaluación real, score y apelación: auditoría 2026-09-12

> **Búsqueda realizada:** `grep -rliE "appeal|apelacion|override|score|dispute"` sobre todo
> `llm-service/src/main/java` + revisión manual de los seis archivos que aparecían bajo nombres
> relacionados con "evaluation". Ningún archivo de dominio, aplicación ni infraestructura calcula,
> persiste o expone un puntaje de un intento real, ni existe ningún endpoint, entidad o tabla de
> apelación/override. Esta no es una lectura parcial — es la búsqueda completa del repo.

## Veredicto: 🔴 No iniciado

Lo único que el código tiene con nombres relacionados a "evaluación de curso" —
`CourseEvaluationStatusController`, `EvaluationAvailabilityService`, `PendingEvaluationWorker`—
resultó ser, tras leerlo, una pieza de **EP-04** (la compuerta que retrasa una evaluación mientras
el desafío no tenga calibración activa), no de EP-06. Se migró y quedó documentada en
[`ep-04/pending-evaluations-gate.md`](../ep-04/pending-evaluations-gate.md). Una vez separado eso,
**no queda nada de EP-06 en el código real.**

## Qué pide la épica, contra qué hay

Objetivo de [`epicas/ep-06.md`](../../epicas/ep-06.md): *"Que cerrar un intento produzca un
puntaje explicado y apelable en las cinco dimensiones de la rúbrica, sin que una caída del
evaluador frene la entrega, y que toda corrección docente quede auditada sin borrar el resultado
original."*

| Pieza que pide la épica | Existe en el código | Estado |
|---|---|---|
| Consumir el aviso de cierre de intento (`intento_cerrado`) | No hay ningún `@KafkaListener` ni consumidor de eventos en todo el repo | 🔴 |
| Calcular las 5 dimensiones para un intento real (evaluador) | El único "evaluador" que existe es `CalibrationMetrics`, y compara **casos del golden set contra referencia humana** — no evalúa intentos de alumnos reales | 🔴 |
| Persistir la evaluación (dimensiones, justificación, versión de modelo/rúbrica) | No existe ninguna tabla ni entidad `evaluation`/`evaluacion` fuera del contexto de calibración | 🔴 |
| Publicar `score_de_ia_calculado.v1` (outbox) | No existe ningún productor de eventos ni patrón outbox en el repo | 🔴 |
| Diferir el cálculo si el evaluador está caído, sin perderlo ni duplicarlo | Existe la mitad: la compuerta de EP-04 encola si **falta calibración** — no si el **modelo/proveedor** está caído, que es el caso que pide esta épica | 🟡 (cubre un caso distinto y más fácil) |
| Bloquear el cierre de un curso con puntajes pendientes | No hay ningún endpoint ni regla de este tipo | 🔴 |
| Apelación (alumno apela, docente ve evidencia, resuelve) | No existe ningún controller, servicio ni tabla de apelación | 🔴 |
| Override append-only (corrección docente sin borrar el original) | No existe ningún mecanismo de override en ningún dominio del repo (ni siquiera para golden set/calibración, que sí son *append-only* pero no tienen "override", tienen versión) | 🔴 |

**Ninguna fila está en verde.** Lo más cerca que hay es la compuerta de EP-04, que resuelve una
precondición ("no evaluar sin calibración"), no la evaluación en sí.

## Contra las recetas de sprint (para dimensionar lo que falta)

[`35-backlog-ejecutable.md`](../../35-backlog-ejecutable.md) reserva **S6 (~208 h)** y **S7
(~208 h)** completos para esto — **416 h**, nada de lo cual está construido:

| Sprint | Paquetes | h | Construido hoy |
|---|---|---:|---|
| S6 — Evaluación asíncrona y diferida | Consumidor/dedupe, evaluador+evidencia, outbox/resultados, diferido/recuperación, consulta pendientes+cursos, UI/observabilidad, pruebas/demo | 208 | Solo el paquete 4 (diferido/recuperación) tiene un antecedente parcial — la compuerta de EP-04, que cubre menos de lo que pide el paquete |
| S7 — Apelación y override auditables | Datos append-only, API/ownership, bandeja docente, propagación a negocio, política/muestreo, pruebas/demo | 208 | Nada |

## El hallazgo que más importa: hay una contradicción de alcance entre dos fuentes del propio proyecto

- [`sprints/README.md` · "Reprogramación a 8 semanas"](../../sprints/README.md) define el
  **alcance estricto de la cátedra** como **6 ítems**: rúbrica, golden set, invocación del modelo,
  calibración, bloqueo de activación y salvaguarda anti-fuga. **EP-06 no aparece en esa lista.**
  Ese mismo documento se autodefine como *"nota de planificación, no una fuente de verdad nueva"*.
- [`00-fuentes-de-verdad-y-convenciones.md` · §5](../../00-fuentes-de-verdad-y-convenciones.md),
  que **sí** es fuente de verdad (prioridad 3, por encima de "resto de `docs/`"), define el
  **alcance de IA en el MVP** como: *"Tutor sin streaming token a token, registro de interacción,
  anti-fuga, **evaluador asíncrono**, rúbrica, golden set, calibración, **apelación**, **score
  diferido** y bloqueo de cierre."* Acá **EP-06 sí está**, tres veces (evaluador, apelación, score
  diferido).

**No puedo resolver esta contradicción yo — es una decisión de producto, no de auditoría.** Pero
sí puedo decir con certeza qué cambia según cómo se resuelva:

- **Si manda [00 · §5]** (que por jerarquía documental debería mandar): EP-06 es parte del MVP
  actual y hoy está en 0% — faltan realistamente **2 sprints más** (S6 + S7, ~416 h) además de lo
  que ya está planificado en el sprint de cierre.
- **Si manda la reprogramación de `sprints/README.md`**: EP-06 queda fuera del corte de 4
  sprints/8 semanas de la cátedra, y el proyecto puede darse por "completo" (a nivel del alcance
  acordado con la cátedra) sin construir nada de esto ahora.

## Recomendación

Llevar esta contradicción puntual a la próxima reunión de equipo (o a la sesión con el Product
Owner) **antes** de comprometer un número de sprints restantes hacia la cátedra — es una pregunta
de 5 minutos con una respuesta que cambia el plan en ~416 h. No es algo que convenga dejar
implícito hasta que alguien pregunte en la defensa por qué no hay apelación si el propio doc 00
la lista como parte del MVP.

**Ya redactada como decisión lista para llevar al PO:**
[`08-decisiones-y-pendientes.md` · C-6](../../08-decisiones-y-pendientes.md) — con la tabla de
contradicción, la consecuencia en horas de cada opción, y qué información necesita el PO para
resolverla en la reunión (no solo la pregunta).
