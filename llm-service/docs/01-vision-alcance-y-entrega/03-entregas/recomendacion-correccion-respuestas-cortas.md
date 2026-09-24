# Recomendación técnica: corrección de respuestas cortas con clave única

> **Para:** equipo de Desafío Teórico (Tema 04 — "corregir", según
> [`11-glosario-y-metadata.md`](../../00-gobierno-y-evolucion/03-glosario-y-metadata.md#las-ocho-colisiones)).
> **De:** Tema 07 — Evaluación LLM (Grupo G03, `llm-service`).
> **Qué es esto:** una recomendación técnica, **no un contrato de integración**. No hay endpoint
> nuevo, no hay llamada a `llm-service`, no hay nada que nosotros vayamos a operar. Es la respuesta
> a lo que nos plantearon: que la corrección de respuestas cortas de tipo "clave única" (ej.
> pregunta espera `API`, el alumno escribe `appi` o `aplicacion`) hoy se resuelve con comparación
> exacta de string y eso genera falsos negativos.

---

## 0. Por qué esto no es un pedido de LLM (y no lo va a ser)

Antes de la parte técnica, la aclaración que evita una expectativa equivocada: **ya existe una
decisión resuelta que excluye un corrector académico basado en LLM.**

- [`08-decisiones-y-pendientes.md` — P-01](../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md#-p-01--corrector-de-respuestas-abiertas-fuera-de-alcance),
  resuelta el 2026-09-06: *"el PRD calibra al evaluador del uso de IA. No especifica un corrector
  académico basado en LLM y no se lo incorporará por analogía. [...] Incorporar un corrector en el
  futuro exige un cambio de alcance con requisitos, riesgos, responsable y validación propios."*
- [`04-funciones-de-ia.md` §4](../../03-capacidades-de-ia/02-funciones-de-ia.md#4-validación-académica-fuera-del-evaluador):
  *"`llm-service` no expone un corrector de respuestas abiertas y el Golden Set no recibe
  soluciones esperadas."*
- [`05-seguridad.md`](../../04-seguridad-datos-y-cumplimiento/01-seguridad-y-guardarrailes.md#para-qué-sirve-tener-el-chunk-a-mano) ya analizó el caso
  exacto que nos trajeron — *"el alumno da una respuesta válida que la rúbrica no previó"* — y la
  conclusión escrita fue **mantenerlo bajo revisión docente**, no automatizarlo con un LLM.

**Consecuencia práctica:** lo que sigue resuelve el caso de *typos y variantes de formato* (`appi`,
`API `, `Api.`) sin IA. Lo que **no** resuelve es una respuesta parcial o una paráfrasis genuina
(`aplicacion` en el sentido de "es una aplicación", conceptualmente incompleta) — eso sigue siendo
zona de revisión docente hasta que alguien reabra P-01 con el Product Owner. No lo asuman resuelto
por esta vía.

## 1. La técnica: dos capas, ninguna es un modelo

Es la misma idea que el propio Tema 07 ya adoptó para el moderador de chat (atrapar `pelotdo` como
variante de `pelotudo`), solo que acá el objetivo es aceptar en vez de bloquear. Referencia:
[`09-preguntas-y-respuestas.md`](../../08-preguntas-investigacion-y-sincronizaciones/01-preguntas-y-respuestas.md#q-23) y
[`04-funciones-de-ia.md` §2.3.1](../../03-capacidades-de-ia/02-funciones-de-ia.md).

### Capa 0 — Normalización (gratis, sin librerías nuevas)

Antes de comparar nada:

1. `trim()` + colapsar espacios múltiples.
2. Pasar a minúsculas.
3. Quitar tildes/diacríticos (en Java, `java.text.Normalizer.normalize(s, Form.NFD)` + regex que
   saca los caracteres combinantes; en otros stacks hay equivalentes directos).
4. Quitar puntuación final (`.`, `!`, `?`).

Con esto solo, `"Api."`, `" api "` y `"API"` ya empatan contra `"api"`.

### Capa 1 — Distancia de edición (typos)

Si la Capa 0 no empata, calcular distancia de edición entre la respuesta normalizada y **cada**
respuesta aceptada de la pregunta. Si Tema 04 ya está en el ecosistema Java/Spring, la librería que
el propio proyecto ya evaluó y adoptó es
[`commons-text` (`org.apache.commons.text.similarity.LevenshteinDistance`)](../../08-preguntas-investigacion-y-sincronizaciones/01-preguntas-y-respuestas.md#q-23) —
mide inserciones/borrados/sustituciones entre dos strings.

**El único punto delicado es el umbral**, y el proyecto ya dejó escrita la advertencia que aplica
igual acá: *"subir el umbral a 2 empieza a matchear palabras legítimas"*. Para respuestas cortas
(una palabra o pocas), lo razonable es:

| Longitud de la respuesta esperada | Umbral sugerido |
|---|---|
| ≤ 4 caracteres (`API`, `SQL`) | 1 |
| 5–8 caracteres | 1–2 |
| > 8 caracteres / más de una palabra | evaluar caso a caso — a esta longitud, la distancia de edición empieza a aceptar cosas que no debería |

No hay un número universal correcto — hay que calibrarlo contra ejemplos reales de la cátedra
(respuestas de alumnos ya corregidas a mano), igual que se calibra cualquier umbral en este
proyecto.

## 2. El cambio que más impacto tiene y no es un algoritmo

**Aceptar una lista de respuestas válidas por pregunta, no un único string.** Antes de invertir en
distancia de edición, esto solo ya elimina buena parte de los falsos negativos: sinónimos
conocidos, mayúsculas/minúsculas del enunciado, abreviaturas esperables. Es una decisión de modelo
de datos en el motor de Tema 04 (`respuestasAceptadas: string[]` en vez de `respuestaEsperada: string`),
no algo que dependa de nosotros ni de ningún otro equipo.

## 3. Qué sigue sin resolver (a propósito)

| Caso | Con esta técnica | Sin ella |
|---|---|---|
| `appi`, `Api.`, ` api ` | ✅ Resuelto | ❌ Falso negativo |
| `aplicacion` (como sinónimo válido ya anticipado) | ✅ Si está en la lista de aceptadas | ❌ |
| `aplicacion` (respuesta parcial/incompleta, no anticipada) | ❌ Sigue mal marcada | ❌ |
| Paráfrasis larga o justificación en prosa | ❌ Es "respuesta abierta", va a revisión docente | ❌ |

La última fila es la que quedaría pendiente si en algún momento la cátedra decide que vale la pena
un corrector semántico real. Esa conversación implica reabrir P-01 con el Product Owner —
requisitos, riesgos y validación propios — no es algo que se resuelva ampliando este documento.

---

*Esta nota no crea una integración entre `llm-service` y el motor de Tema 04: no hay endpoint,
evento ni scope M2M involucrado. Si en el futuro se decide construir el corrector semántico y eso
sí requiere a Tema 07, ese sería un documento nuevo, con la misma estructura que
[`alcance-y-contrato-para-desafios-practicos.md`](alcance-y-contrato-para-desafios-practicos.md),
no una extensión de este.*
