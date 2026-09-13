# Backend de negocio — pendientes

> Fuente completa: [17 §7.3](../../17-mapa-de-integracion.md#73-quién-nos-bloquea-y-a-quién-bloqueamos)
> (N4, N5), [18 §4.6](../../18-contratos-inter-equipos.md#46-backend-de-negocio).

## 🟡 Cruzado — endpoint de contexto del desafío

El tutor necesita el enunciado del desafío para armar el prompt. Sin asignar todavía.

**🟡 Propuesta de cuerpo — no acordada:**

```json
// GET (hipotético) {backend}/challenges/{challengeId}/context
{
  "challengeId": "b1e2c3d4-0002-4a00-8000-000000000002",
  "enunciado": "Implementar factorial recursivo con manejo de casos borde",
  "lenguaje": "java",
  "riskLevel": "medium"
}
```

Ni el endpoint ni estos campos están decididos — sirve como punto de partida para que el
Backend de negocio proponga la forma real que ya tienen modelada.

## 🔴 Cruzado — aceptar entregas con el evaluador caído

Igual que el punto equivalente con Tema 03 ([`tema-03-motor-de-desafios/pendientes.md`](../tema-03-motor-de-desafios/pendientes.md)):
si respondemos `503`, la entrega tiene que aceptarse igual con `score_agregado = null`. Doc 17
lo marca explícitamente como **"el que más se cae entre equipos"** — si no se implementa,
"no pueden cerrar un curso".
