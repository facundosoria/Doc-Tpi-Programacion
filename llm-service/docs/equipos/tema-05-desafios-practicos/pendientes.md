# Tema 05 — Desafíos Prácticos — pendientes

> Fuente completa: [17 §7.3](../../17-mapa-de-integracion.md#73-quién-nos-bloquea-y-a-quién-bloqueamos)
> (N1, N3), [18 §4.3](../../18-contratos-inter-equipos.md#43-tema-05--desafíos-prácticos),
> [08 B-1](../../08-decisiones-y-pendientes.md), [20-backlog-y-sprints.md](../../20-backlog-y-sprints.md) E12-02.

## 🔴 Cruzado — la solución esperada del desafío

Sin esto el guardarraíl anti-fuga (RF-IA-20) no tiene contra qué comparar. Falta definir
endpoint, verbo y payload. Hay resistencia esperable: le estamos pidiendo a Tema 05 que exponga
algo que hoy consideran interno y sensible.

**Nuestra propuesta ya escrita** (doc 08 B-1): que Tema 05 exponga un endpoint que devuelva la
solución esperada **solo a nosotros**, solo para comparación — nunca la almacenamos, la usamos
y la descartamos. Ofrecer esa garantía por escrito destraba la conversación.

**Estado a la fecha de `20-backlog-y-sprints.md`:** sin resolver — la salvaguarda corre hoy
contra una solución mock, y la integración real queda anotada como deuda.

**🟡 Propuesta de cuerpo — no acordado, solo para arrancar la conversación:**

```json
// GET (hipotético) {tema-05}/challenges/{challengeId}/expected-solution
// Llamado por nosotros, con nuestro JWT M2M — nunca al revés.
{
  "challengeId": "b1e2c3d4-0002-4a00-8000-000000000002",
  "language": "java",
  "expectedSolution": "public int factorial(int n) { ... }",
  "hiddenTestsSummary": "3 casos borde: n=0, n=1, overflow"
}
```

Ningún campo de este ejemplo está cerrado — ni el verbo, ni si viaja por HTTP o por evento, ni
si incluye `hiddenTestsSummary` o solo el código de referencia. Se pone acá para que la sesión
de integración tenga algo concreto para tachar o corregir, no para presentarlo como decidido.

## 🔴 Cruzado — evento de ediciones y ejecuciones de tests del IDE

Alimenta el 30% del score (dimensión autonomía). Si no se pide ahora, no va a existir.

**Sin decidir:** si la fuente es Tema 05 (IDE) o Tema 06 (sandbox de ejecución) —
`11-glosario-y-metadata.md` deja explícito que puede ser cualquiera de los dos y no está
resuelto.

## 🟡 Interno, en desarrollo — streaming SSE del tutor

**Hoy NO implementado.** El código actual (EP-05) solo tiene el camino síncrono completo con
guardarraíles; streaming quedó fuera de esta pasada
(`docs/estado-implementacion/ep-05/README.md`).

El contrato ya está escrito y listo para fusionar:
[`llm-service-v1-tutor-sse-adenda.md`](../../contracts/llm-service-v1-tutor-sse-adenda.md)
(patrón Buffer Interceptor, eventos `token`/`hold`/`segment`/`blocked`/`done`/`error`). Pero
**no se fusiona al contrato vigente hasta que se cierre I-10** (streaming: propagar o revertir
la decisión en el resto de los documentos) **y Tema 05 acuerde consumir SSE**.

Por eso, aunque sea trabajo nuestro, hay que avisarles antes de construirlo — no es un cambio
que se pueda lanzar sin que el otro lado sepa que va a dejar de recibir una respuesta completa
de una sola vez.

**Nota aparte, sin bloquear esta conversación:** tampoco existe todavía una ficha de historia
formal para EP-05 (`docs/historias/ep-05/` no existe hoy) — el código se adelantó a la ficha,
igual que pasó con golden set/calibración. Se anota acá como contexto, no como algo que
dependa de Tema 05.

## 🟡 Nuevo — UX del tutor `unavailable`

Sin acordar: qué le mostramos al alumno cuando el tutor agota la escalera de degradación y
responde `state: unavailable`. No hay copy, ni pantalla, ni reintento sugerido definidos. Surge
al documentar resiliencia en `contratos.md` — no bloquea código, pero sí a Front End para armar
la pantalla 1.

## Colisión de vocabulario

"Umbral 70%" significa cosas distintas para cada equipo: para nosotros es el piso de similitud
del guardarraíl anti-fuga; para Tema 05 es el umbral de originalidad entre entregas de
alumnos. Ver [transversales del README](../README.md#glosario-de-colisiones-de-vocabulario) y
[`11-glosario-y-metadata.md`](../../11-glosario-y-metadata.md).
