# Demo Interactiva: Pipeline de Ciberseguridad LLM Paso a Paso
> **Cátedra:** Programación IV — Back End · UTN FRC  
> **Stack Oficial:** Java 21 · Spring Boot 3.4 · Spring AI 1.0.0 · JavaParser · PostgreSQL/pgvector (ADR-005)  
> **Especificación Central:** [`docs/05-seguridad.md`](../../docs/05-seguridad.md) (§3 Defensa en capas contra prompt injection)

---

## 🎯 Enfoque Didáctico: Comparador Lado a Lado (Vulnerable vs Protegido)

Esta demo interactiva permite recorrer **paso a paso (desde la entrada del alumno hasta la respuesta final)** cómo reacciona una arquitectura ingenua sin protecciones frente a la implementación en backend Java con defensa en profundidad:

* **🔴 Panel Izquierdo — Sin Protección (Vulnerable):** Muestra el prompt desprotegido, la concatenación simple de strings, la ausencia de filtros y cómo el atacante logra vulnerar o sobrecargar el sistema.
* **🟢 Panel Derecho — Con Protección Backend (Java Spring Boot):** Muestra el código Java de la clase correspondiente (`HarmlessnessFilter`, `PromptBuilderService`, `RetrievalBoundaryEnforcer`, `AsyncIntentClassifier`, `ContextMinimizerService`, `AstStreamingGuardrail`) y el mecanismo de mitigación en tiempo real.

---

## 🛤️ Recorrido del Pipeline (8 Pasos)

1. **Paso 0: Entrada del Alumno** — Payload crudo vs DTO inmutable con validación Jakarta `@Size(max=4000)`.
2. **Paso 1: Capa 1 — Filtros Determinísticos (`HarmlessnessFilter.java`)** — Scanner léxico, regex compilado y desofuscador Base64 en ~1ms ($0).
3. **Paso 2: Capa 2 — Separación Estructural (`PromptBuilderService.java`)** — `SystemMessage` inmutable + delimitación `<untrusted_student_input>` XML para evitar el secuestro de rol.
4. **Paso 3: Capa 3 — Perímetro por Retrieval (`RetrievalBoundaryEnforcer.java`)** — Filtrado estricto por `curso_id` en pgvector impuesto por el servidor (RF-IA-06).
5. **Paso 4: Capa 4 — Clasificador de Intención (`AsyncIntentClassifier.java`)** — Modelo auxiliar concurrente con `CompletableFuture` que detecta peticiones de solución y fuerza la respuesta socrática.
6. **Paso 5: Capa 5 — Minimización de Contexto (`ContextMinimizerService.java`)** — Principio Zero-Leaks: la solución oficial NUNCA ingresa al prompt del LLM (RF-IA-04).
7. **Paso 6: Capa 6 — Guardarraíl con AST (`AstStreamingGuardrail.java`)** — Buffer interceptor en RAM y `JavaParser` calculando similitud sintáctica $\ge 70\%$ (PAR-11 / RF-IA-20).
8. **Paso 7: Veredicto Final** — Resumen comparativo de mitigación OWASP LLM01, consumo de presupuesto y preservación académica.

---

## 🚀 Cómo Iniciar la Demo

Abre con doble clic en cualquier navegador (100% offline, 0 dependencias externas):
👉 **[`index.html`](index.html)**

### Controles:
* Botones **`Paso Anterior`** / **`Siguiente Paso`** en la barra inferior.
* Teclas **`Flecha Derecha [→]`** y **`Flecha Izquierda [←]`**.
* Selector de **Vectores de Ataque** en la barra superior.
