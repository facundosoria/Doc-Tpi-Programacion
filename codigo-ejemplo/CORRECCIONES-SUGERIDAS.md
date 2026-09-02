# Correcciones sugeridas — `lara-heredia-demo-llm-spring-ai`

El código de esta demo llegó desde la rama `lara` y **se importó sin tocar una sola
línea**: cada archivo es byte a byte idéntico al original. Lo que sigue son
correcciones que conviene aplicar, anotadas acá en vez de aplicadas, para que las
haga quien es dueño del código.

---

## 🔴 1. Hay una API key de Groq hardcodeada en el repositorio

**Archivo:** `lara-heredia-demo-llm-spring-ai/BE/src/main/resources/application.properties`, línea 9.

```properties
spring.ai.openai.api-key=gsk_ffHA4AZs...   # key real, completa, en texto plano
```

**Por qué es urgente:** la key ya está publicada en el historial de la rama `lara` en
GitHub. Borrarla del archivo no la saca del historial: cualquiera que clone el
repositorio la sigue teniendo con un `git log -p`. **Rotarla en el panel de Groq es
obligatorio, no opcional** — es el único paso que la invalida de verdad.

**Los dos pasos, en orden:**

1. **Rotar la key en Groq.** Revocar la actual y generar una nueva.
2. **Dejar de versionarla.** Que el archivo lea la variable de entorno:

   ```properties
   spring.ai.openai.api-key=${GROQ_API_KEY}
   ```

   Y para correr la demo:

   ```bash
   export GROQ_API_KEY="la-nueva-key"
   cd codigo-ejemplo/lara-heredia-demo-llm-spring-ai/BE && ./mvnw spring-boot:run
   ```

   El comentario que ya está arriba de esa línea explica justamente esto, así que el
   cambio es coherente con lo que el archivo dice de sí mismo.

**Mientras tanto:** el gate de calidad tiene la etapa `secretos` en `bloquea`, pero
esta carpeta queda fuera de `owned-paths.txt`, así que el hallazgo se reporta como
informativo y no frena las corridas de nadie. Esa es la razón por la que no se rompió
nada al importar la demo — no que el problema no exista.

---

## 🟡 2. Las carpetas `.idea/` no llegaron a esta rama

Al importar se dejaron afuera `demoLLMSpringAi/.idea/` y `demoLLMSpringAi/FE/.idea/`,
que eran configuración de IntelliJ. Es lo único del árbol original que no está acá.

Como `FE/` no contenía nada más que su `.idea/`, la carpeta directamente no aparece.
Si el front tenía código sin commitear, quedó afuera y hay que subirlo aparte.

El `.gitignore` de la raíz ahora ignora `.idea/` y `*.iml`, así que no vuelven a
entrar solas.

---

## 🟢 3. La demo apunta a H2 en memoria, el microservicio real a PostgreSQL

No es un error: son proyectos distintos con propósitos distintos y está bien que la
demo arranque sin base instalada. Se anota solamente para que nadie tome la
configuración de la demo como referencia de la del servicio real. La decisión de
PostgreSQL 16 + pgvector está en
[`docs/12 · Almacenamiento e ingesta`](../docs/12-almacenamiento-e-ingesta.md).
