# Política de tests — ms-evaluacion-llm

> **Alineación vigente:** al migrar el esqueleto a `llm-service`, los tests de contrato deberán
> partir de `docs/contracts/`, verificar el path `/api/llm/**` y propagar
> `traceparent` + `X-Request-Id`. Los nombres históricos de este documento no se extienden.

## Regla fundamental

> **Los tests que corren en CI nunca llaman a la API del modelo.**
> Si el test toca la API real, gasta plata. Si gasta plata sin querer, nadie lo va a correr.

El LLM se invoca **siempre** detrás de una interfaz (un puerto, un cliente, lo que sea).
Esa interfaz se mockea en los tests unitarios con un valor fijo (hardcodeado).
El test no sabe si hay un modelo real del otro lado. No le importa.

---

## Qué va hardcodeado y qué no

| Parte del código | En el test |
|---|---|
| Lógica de negocio (cálculo de score, validaciones, mapeos) | ✅ Se testea con valores fijos reales |
| Llamada al modelo (LLMClient, ChatClient, lo que uses) | ❌ Se mockea — devuelve un JSON fijo que vos escribís |
| Parseo de la respuesta del modelo | ✅ Se testea pasándole ese mismo JSON fijo |

---

## Cuándo SÍ se llama a la API real

Solo en tests que son **explícitamente para eso**, que:

1. Están en un módulo / carpeta separada del resto de los tests.
2. No corren en CI automático (hay que ejecutarlos a mano o en un pipeline aparte).
3. Están documentados con cuánto cuestan por corrida.

Si no se cumple alguna de las tres, el test va con mock.

---

## Por qué esto importa

- **Costo**: una suite de CI que llama a OpenAI en cada push puede costar USD 5-20 por mes sin que nadie se dé cuenta.
- **Flakiness**: los modelos no devuelven siempre lo mismo. Un test que depende del texto exacto de la respuesta va a fallar al azar.
- **Velocidad**: un test con mock tarda milisegundos. Uno que llama a la API puede tardar 3-8 segundos.

---

## Referencia

La política completa de niveles de prueba (unitarias → evals → guardarraíles) está en
`docs/06-operacion-e-ingenieria.md`, sección **"Los cinco niveles de prueba"** (~línea 690).
