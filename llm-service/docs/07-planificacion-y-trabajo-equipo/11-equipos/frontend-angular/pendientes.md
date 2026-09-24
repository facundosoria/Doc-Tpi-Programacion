# Front End — Angular — pendientes

> Este documento es la fuente completa de lo pendiente con Front End. Antecedentes:
> 17 §7.3 (N7),
> [01 §3](../../../01-vision-alcance-y-entrega/01-problema-y-alcance.md#3-lo-que-necesitás-pedirle-a-los-otros-equipos).

## 🔴 Cruzado — las pantallas que necesitamos

> ⚠️ **No son 7.** `01-problema-y-alcance.md` §3.2 y esta lista traían dos catálogos de "7
> pantallas" distintos, con solo una en común (el chat del tutor) — quedó así porque se
> escribieron en momentos distintos sin cruzarse. Esta es la lista fusionada y deduplicada; si
> aparece una pantalla nueva, se agrega acá, no se vuelve a inventar un conteo redondo.

| # | Pantalla | Para qué | Requerimiento |
|---|---|---|---|
| 1 | Chat del tutor en el IDE | Sin esto la función principal no tiene UI | RF-IA-01 |
| 2 | Estado del evaluador por intento, con desglose por dimensión y justificación | Para que el alumno vea el feedback | RF-IA-16 |
| 3 | Flujo de apelación | El alumno pide revisión humana | RF-IA-18 |
| 4 | Revisión del parcial generado, con el fragmento fuente al lado | Gate humano obligatorio antes de publicar | [04](../../../03-capacidades-de-ia/02-funciones-de-ia.md) §5 |
| 5 | Panel de calibración (docente) | Para aprobar el golden set | RF-IA-36 |
| 6 | Panel/dashboard de moderación e incidentes (admin) | Ver incidentes de jailbreak y moderación, e historial | RF-IA-10, RF-CHT-11 |
| 7 | Dashboard de costos y uso (admin) | Visualización del Tema 12 | — |
| 8 | Config de modelos del ADMIN | Asignación modelo→función | RF-IA-24 |
| 9 | **Pantalla del golden set** | La más urgente — destraba el plazo más largo | RF-IA-30, DoD 7b |

Si no llegan: **"la IA queda lista y no se puede usar ni verificar"** (17 §7.3).

La pantalla 9 está cruzada con [`tema-12-backoffice-admin/pendientes.md`](../tema-12-backoffice-admin/pendientes.md)
(I-15, dueño sin definir) y con [`product-owner/pendientes.md`](../product-owner/pendientes.md).
