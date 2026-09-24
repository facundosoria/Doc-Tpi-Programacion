# Tema 03 — Motor de Desafíos — contratos

## Estado vigente: sin contrato directo

**`llm-service` no se comunica directamente con el Motor de Desafíos.** Desde la decisión de
diseño del 2026-09-13, todo el intercambio del evaluador pasa por Tema 05 (`practice-service`):

- Tema 05 nos notifica el cierre del intento (con transcripción) — antes lo hacía Tema 03
  directo.
- Nosotros le entregamos el score a Tema 05 por evento Kafka (`SCORE_CALCULATED` /
  `SCORE_DEFERRED`) — antes se lo dábamos a Tema 03.
- **Tema 05 es quien le reenvía el resultado a Tema 03** para que aplique el modificador de XP.
  Eso no cambia: **nosotros nunca otorgamos XP**, solo se movió quién nos habla.

El contrato vigente del evaluador (eventos, payloads, degradación) vive en
[`tema-05-desafios-practicos.md`](tema-05-desafios-practicos.md). El detalle del contrato
directo anterior (endpoints `/ai/**`, payloads `snake_case`) y por qué se retiró está en
[`registro/2026-09-22-limpieza-historicos-contracts.md`](../../registro/2026-09-22-limpieza-historicos-contracts.md).

| Dirección | HTTP | Kafka |
|---|---|---|
| Tema 03 → Tema 07 | No hay endpoint directo. | Tema 07 no consume eventos de Tema 03. |
| Tema 07 → Tema 03 | Tema 07 no llama a Tema 03. | Tema 07 no publica scores a Tema 03 (va a Tema 05). |

## Futuro

Si Tema 07 necesitara catálogo de desafíos para una selección de calibración, Tema 03 debe
acordar un contrato nuevo. Canal, recurso, campos y autorización son **faltantes por definir**;
Tema 07 no puede suplirlos leyendo la base de Tema 03.
