# Guía de Adaptación: De la Demo de Lara al Microservicio `ms-evaluacion-llm`

> 📄 **Documento de transferencia técnica** para explicar los cambios, motivos arquitectónicos y cómo se integró el trabajo de Lara (`lara-heredia-demo-llm-spring-ai`) dentro del microservicio oficial del Tema 07 ([`ms-evaluacion-llm`](./ESTRUCTURA.md)).

---

## 1. Resumen Ejecutivo

El trabajo realizado por Lara fue **fundamental como Prueba de Concepto (PoC)**:
* Demostró la viabilidad de usar el starter OpenAI de Spring AI para conectarse a la API de **Groq** (`llama-3.3-70b-versatile`).
* Diseñó las **reglas pedagógicas socráticas** del tutor (guiar con preguntas reflexivas, nunca dar código resuelto).
* Creó el primer filtro de **detección de jailbreak**.
* Validó los flujos con una colección completa de **Postman**.

Para que este código pueda convivir en el ecosistema de 12 microservicios del Trabajo Práctico Integrador (TPI) y cumplir con las normas de la cátedra, adaptamos su estructura al **diseño por capas, contratos OpenAPI y testing con mocks** establecido en la arquitectura.

---

## 2. Tabla Comparativa de Archivos

| Componente en Demo de Lara | Ubicación y Nombre en `ms-evaluacion-llm` | Qué se modificó / adaptó |
| :--- | :--- | :--- |
| `com.example.demo.DemoLlmSpringAiApplication` | `ar.edu.utn.frc.tup.piv.evaluacionllm.Application` | Paquete oficial de la UTN FRC. |
| `com.example.demo.config.GroqChatClientConfig` | `service.gateway.adapter.GroqAdapter` + `application.yml` | Configuración desacoplada en el AI Gateway M1; lee `${GROQ_API_KEY}` por variable de entorno. |
| `com.example.demo.controller.ChatController` | `controller.AiController` + `controller.TutorChatController` | Se agregó el endpoint oficial `POST /ai/tutor` del contrato inter-equipos y se mantuvieron los endpoints `/api/conversaciones` para soporte directo. |
| `com.example.demo.service.TutorSocraticoService` | `service.tutor.TutorService` + `service.tutor.TutorServiceImpl` | Separación en interfaz e implementación; delega la llamada a `LlmGateway` en vez de llamar directo al cliente del LLM. |
| Prompt String en `TutorSocraticoService` | `src/main/resources/prompts/tutor/system-v1.txt` | Externalización del prompt en archivo de texto (RF-IA-29) para editarlo sin recompilar Java. |
| `JAILBREAK_KEYWORDS` en `TutorSocraticoService` | `service.gateway.guard.InputGuard` | Guardarraíl centralizado con normalización de tildes, mayúsculas y caracteres especiales. |
| `com.example.demo.model.Conversacion` | `entity.ConversacionEntity` | Se agregaron los campos multitenancy obligatorios: `curso_cohorte_id`, `usuario_ref` y `desafio_id`. |
| `com.example.demo.model.Mensaje` | `entity.MensajeEntity` | Entidad JPA mapeada con auditoría y timestamps automáticos. |
| `com.example.demo.repository.*` | `repository.ConversacionRepository`, `repository.MensajeRepository` | Repositorios Spring Data JPA con queries por cohorte y usuario. |
| `com.example.demo.dto.*` | `dto.request.*` y `dto.response.*` | DTOs organizados por dirección de flujo, integrando el sobre común `AiRequest` y `AiResponse` con `trace_id`. |
| `DemoLlmSpringAiApplicationTests` (vacío) | `src/test/java/.../*Test.java` (7 clases de test) | Suite completa de **tests unitarios con Mockito** (>85% de cobertura) sin invocar APIs externas ni gastar tokens. |

---

## 3. Los 7 Cambios Arquitectónicos Clave Explicados

### 🏛️ 1. Paquetes y Estructura por Capas
* **Antes:** Todo convivía bajo `com.example.demo`.
* **Ahora:** Organización estricta bajo `ar.edu.utn.frc.tup.piv.evaluacionllm`:
  ```
  controller/   → Presentación (recibe HTTP, valida DTOs, devuelve respuestas)
  service/      → Negocio (orquesta el flujo, guarda en base, delega al gateway)
    ├── gateway/   → M1 AI Gateway (adapters, cuotas, guardarraíles)
    └── tutor/     → M6 Tutor Socrático
  repository/   → Acceso a datos (Spring Data JPA)
  entity/       → Entidades de base de datos
  dto/          → Transferencia de datos (request/ y response/)
  ```

---

### 🤖 2. El AI Gateway (M1) vs Llamada Directa a `ChatClient`
* **Antes:** `TutorSocraticoService` inyectaba directamente `ChatClient` y llamaba a Groq.
* **Ahora:** `TutorServiceImpl` inyecta la interfaz `LlmGateway`.
* **Por qué:**
  1. Si mañana cambiamos Groq por OpenAI o Claude, solo se cambia el adapter sin tocar una sola línea del tutor.
  2. Permite aplicar **cuotas de consumo por alumno/día** y **modo degradado** si la API de IA se cae.

---

### 📝 3. Externalización de Prompts (`resources/prompts/`)
* **Antes:** El texto del system prompt estaba hardcodeado dentro del método `construirPrompt(...)` en Java.
* **Ahora:** Vive en `src/main/resources/prompts/tutor/system-v1.txt` y `user-v1.txt`.
* **Por qué:**
  * **RF-IA-29:** Las directivas de los prompts deben poder versionarse y modificarse sin recompilar ni redeployar el código Java.
  * Facilita que docentes o integrantes del equipo ajusten la pedagogía del tutor directamente en el `.txt`.

---

### 🛡️ 4. Guardarraíles Centralizados (`InputGuard` y `OutputAntiLeakGuard`)
* **Antes:** La validación de jailbreak era un método privado en el service.
* **Ahora:** Se separó en dos componentes en `service.gateway.guard`:
  1. **`InputGuard`:** Normaliza el texto (elimina tildes y pasa a minúsculas) y busca patrones de ataque antes de gastar un token de IA.
  2. **`OutputAntiLeakGuard`:** Verifica que la respuesta generada no contenga bloques de código resuelto o soluciones directas antes de enviarla al alumno.

---

### 🏢 5. Soporte Multitenancy en Entidades (`curso_cohorte_id`)
* **Antes:** Las conversaciones no sabían a qué curso ni a qué alumno pertenecían.
* **Ahora:** `ConversacionEntity` incluye `curso_cohorte_id`, `usuario_ref` y `desafio_id`.
* **Por qué:** En la plataforma conviven múltiples cursos y cohortes. Sin estas claves, las conversaciones de un alumno se mezclarían con las de otros cursos.

---

### 📦 6. Contratos OpenAPI y `trace_id`
* **Antes:** Respuestas personalizadas sin metadata de trazabilidad.
* **Ahora:** El endpoint `POST /ai/tutor` responde con el sobre estándar:
  ```json
  {
    "resultado": {
      "respuesta": "¿Qué estructura de datos te permitiría buscar en tiempo constante?",
      "estado": "OK",
      "modelo": "llama-3.3-70b-versatile",
      "conversacion_id": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
    },
    "trace_id": "8a3c4e21-...",
    "metadata": {
      "funcion": "tutor",
      "modo": "sync"
    }
  }
  ```
* **Por qué:** El API Gateway de la cátedra propaga el header `X-Trace-Id` para depurar problemas entre microservicios.

---

### 🧪 7. Estrategia de Tests (>85% Cobertura con Mocks)
* **Antes:** Un solo test vacío `contextLoads()`.
* **Ahora:** 7 clases de tests con **JUnit 5 + Mockito + MockMvc**:
  1. `InputGuardTest`: 18 casos de prueba de jailbreak y preguntas válidas.
  2. `OutputAntiLeakGuardTest`: verificación de detección de fugas.
  3. `LlmGatewayImplTest`: verificación del ruteo del gateway.
  4. `TutorServiceImplTest`: prueba unitaria de todo el flujo pedagógico (bloqueo, respuestas, histórico, errores).
  5. `AiControllerTest`: validación HTTP de `POST /ai/tutor` con `MockMvc`.
  6. `TutorChatControllerTest`: validación de endpoints CRUD `/api/conversaciones`.
  7. `GlobalExceptionHandlerTest`: respuestas 400, 404 y 500 tipadas.

> 💡 **Por qué usamos Mocks en los tests:**
> En CI **nunca se llama a la API real de Groq**:
> 1. Ahorra dinero (cero costo de API en los builds).
> 2. Los tests corren en milisegundos.
> 3. No fallan por problemas de red o cambios sutiles en la respuesta del modelo (*cero flakiness*).

---

## 4. Guía Rápida para Lara

### ¿Cómo correr los tests del microservicio?
Desde la carpeta `codigo-ejemplo/ms-evaluacion-llm`:
```bash
./mvnw test
```

### ¿Cómo levantar el servicio localmente con Groq real?
1. Configurar la variable de entorno con la API key de Groq:
   ```bash
   # En Windows PowerShell:
   $env:GROQ_API_KEY="tu-clave-aqui"

   # En Linux / Mac / Git Bash:
   export GROQ_API_KEY="tu-clave-aqui"
   ```
2. Ejecutar la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```

### ¿Cómo ajustar las instrucciones del Tutor?
Solo tenés que abrir:
```
src/main/resources/prompts/tutor/system-v1.txt
```
Modificar las directivas o agregar nuevos ejemplos de diálogo y reiniciar la app. ¡No hace falta cambiar código Java!
