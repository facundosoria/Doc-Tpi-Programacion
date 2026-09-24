# Backend de negocio — pendientes

> Este documento es la fuente completa de lo pendiente con Backend de negocio. Antecedentes:
> 17 §7.3 (N4, N5),
> [01 §3](../../../01-vision-alcance-y-entrega/01-problema-y-alcance.md#3-lo-que-necesitás-pedirle-a-los-otros-equipos).

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

## 🟡 Cruzado — endpoint para devolver resultados

No escribimos en la base académica (ADR-001) — necesitamos que el Backend exponga el endpoint
donde volcamos el resultado del evaluador (`score_agregado`, desglose, confianza). Sin asignar
todavía; ver [01 §3](../../../01-vision-alcance-y-entrega/01-problema-y-alcance.md#3-lo-que-necesitás-pedirle-a-los-otros-equipos).

## 🟡 Cruzado — identidad y `curso_id` derivados de la sesión, nunca del cliente

Regla de seguridad, no propuesta: si `alumno_id` / `curso_cohorte_id` vienen como parámetro del
cliente en vez de derivarse del token de sesión, el aislamiento entre alumnos y cursos no vale
nada (ver [05-seguridad.md](../../../04-seguridad-datos-y-cumplimiento/01-seguridad-y-guardarrailes.md)). Confirmar que el Backend los deriva siempre
del lado servidor antes de reenviarnos cualquier pedido.

## 🔴 Cruzado — aceptar entregas con el evaluador caído

Igual que el punto equivalente con Tema 03 ([`tema-03-motor-de-desafios/pendientes.md`](../tema-03-motor-de-desafios/pendientes.md)):
si respondemos `503`, la entrega tiene que aceptarse igual con `score_agregado = null`. Doc 17
lo marca explícitamente como **"el que más se cae entre equipos"** — si no se implementa,
"no pueden cerrar un curso".
