# Tema 10 — XP / economía — contratos

> La denominación oficial de este tema está por confirmar; se usa "XP / economía" como referencia
> descriptiva, no como nombre de servicio acordado.

## Estado: sin contrato directo

No existe contrato directo entre `llm-service` y el servicio de XP/economía.

| Dirección | HTTP | Kafka |
|---|---|---|
| Tema 10 → Tema 07 | Tema 07 no recibe límites de XP, monedas ni logros. | Tema 07 no consume eventos de economía. |
| Tema 07 → Tema 10 | Tema 07 no concede ni ajusta XP/monedas. | Tema 07 no publica hechos de economía. |

El resultado técnico de IA llega a Tema 05 (`practice-service`) vía `SCORE_CALCULATED` /
`SCORE_DEFERRED` (ver [`tema-05-desafios-practicos.md`](tema-05-desafios-practicos.md)); el flujo
posterior hacia el motor de desafíos/economía es responsabilidad de sus dueños. Tema 07 no
interpreta ese flujo ni puede convertir un score en recompensa — ver también
[`tema-03-motor-de-desafios.md`](tema-03-motor-de-desafios.md).

## Futuro

Cualquier integración futura con Tema 10 parte como **faltante por definir**, no como contrato
existente. La denominación y el dueño deben confirmarse antes de acordar nada.
