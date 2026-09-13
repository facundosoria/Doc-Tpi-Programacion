# 🔑 Guía de API Keys — Setup completo

---

## 1. GROQ (Recomendado para MVP — GRATUITO)

### ¿Por qué Groq?
- ✅ **Gratuito**: 30 requests/minuto (más que suficiente para demo)
- ✅ **Rápido**: 50 tokens/segundo (2-3x más rápido que OpenAI)
- ✅ **Modelo bueno**: Llama-3.3-70b es competente
- ⚠️ **Limitación**: Sólo Llama (no GPT-4, Claude)

### Paso 1: Crear cuenta en Groq

```
1. Ir a https://console.groq.com/keys
2. Click "Sign Up"
3. Email + contraseña (o Google OAuth)
4. Verificar email
```

### Paso 2: Generar API Key

```
1. En consola.groq.com, ir a "API Keys"
2. Click "+ Create New API Key"
3. Copiar la key (empieza con "gsk_")
   Ej: gsk_aBcDeF1234567890AbCdEf1234567890
```

### Paso 3: Guardar en .env.local

```bash
# .env.local (raíz del proyecto Spring Boot)
GROQ_API_KEY=gsk_aBcDeF1234567890AbCdEf1234567890

# O en application.yml
spring:
  ai:
    openai:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai/v1
```

### Paso 4: Usar en Maven

```bash
# Opción 1: Variable de entorno
export GROQ_API_KEY=gsk_...
mvn spring-boot:run

# Opción 2: Flag en línea de comando
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.ai.openai.api-key=gsk_..."

# Opción 3: Archivo .env (si usas spring-cloud-starter-bootstrap)
# En application-local.yml:
spring:
  ai:
    openai:
      api-key: gsk_...
```

### Checkear que funciona

```bash
curl -X POST https://api.groq.com/openai/v1/chat/completions \
  -H "Authorization: Bearer gsk_..." \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama-3.3-70b-versatile",
    "messages": [{"role": "user", "content": "Hola"}]
  }'
```

Si ves respuesta JSON con "choices" → ✅ funciona

---

## 2. OPENAI (Backup/Producción)

### ¿Por qué OpenAI?
- ✅ **Modelos fuertes**: GPT-4, GPT-4-turbo
- ✅ **Más confiable**: Enterprise-grade
- ⚠️ **Costo**: $0.03-0.12 por 1K tokens
- ⚠️ **Más lento**: 5-10 segundos en promedio

### Precio estimado (demo)

```
1000 mensajes de tutor × 500 tokens promedio = 500K tokens
$0.03 (input) × 500 = $15
$0.06 (output) × 250 = $15
TOTAL: ~$30 para demo exhaustiva
```

### Paso 1: Crear cuenta

```
1. Ir a https://platform.openai.com/signup
2. Email + contraseña
3. Verificar email
4. Agregar método de pago (tarjeta crédito)
```

### Paso 2: Generar API Key

```
1. Ir a https://platform.openai.com/account/api-keys
2. Click "+ Create new secret key"
3. Copiar (empieza con "sk-")
   Ej: sk-proj-1234567890abcdefghijklmnopqrstuvwxyz
```

### Paso 3: Configurar en application.yml

```yaml
spring:
  ai:
    openai:
      base-url: https://api.openai.com/v1  # Cambiar de Groq a OpenAI
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4-turbo
        # O más barato: gpt-3.5-turbo
```

### Paso 4: Usar

```bash
export OPENAI_API_KEY=sk-proj-...
mvn spring-boot:run
```

---

## 3. ANTHROPIC Claude (Alternativa premium)

### ¿Por qué Claude?
- ✅ **Mejor reasoning**: Excelente para evaluación
- ✅ **Ventana de contexto**: 200K tokens (vs 8K de OpenAI)
- ✅ **Seguridad**: Mejor en mitigación de jailbreaks
- ⚠️ **Costo**: Similar a OpenAI ($0.003-0.024 por 1K)
- ⚠️ **Spring AI**: Soporte más reciente

### Paso 1: Crear cuenta

```
1. Ir a https://console.anthropic.com
2. Email + contraseña
3. Verificar email
4. Agregar método de pago
```

### Paso 2: Generar API Key

```
1. Ir a https://console.anthropic.com/account/keys
2. Click "Create Key"
3. Copiar (empieza con "sk-ant-")
   Ej: sk-ant-1234567890abcdefghijklmnopqrstuvwxyz
```

### Paso 3: Agregar dependencia (Anthropic aún no está en starter)

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Paso 4: Usar con ChatClient

```java
// En LlmClientConfig.java
@Bean
public ChatClient chatClient(ChatClient.Builder builder) {
    // Spring AI automáticamente usa Anthropic si es el único en classpath
    return builder.build();
}

// O explícito:
@Bean
public AnthropicChatModel anthropicChat() {
    return new AnthropicChatModel(
        new AnthropicApi(System.getenv("ANTHROPIC_API_KEY")),
        AnthropicChatOptions.builder()
            .withModel("claude-3-sonnet-20240229")
            .withMaxTokens(2048)
            .build()
    );
}
```

---

## 4. LOCAL (Sin Internet — Ollama)

### ¿Por qué local?
- ✅ **Gratis**: Sin costos de API
- ✅ **Privado**: Datos no se envían a servidores
- ✅ **Rápido**: En tu GPU/CPU
- ⚠️ **Lento**: Sin GPU es muy lento
- ⚠️ **Modelos chicos**: Llama 2-7B no es tan bueno

### Paso 1: Instalar Ollama

```bash
# Mac/Windows/Linux
https://ollama.ai

# O con Homebrew (Mac):
brew install ollama

# O con apt (Linux):
curl https://ollama.ai/install.sh | sh
```

### Paso 2: Descargar modelo local

```bash
ollama pull llama2  # ~4GB
# O más moderno:
ollama pull neural-chat  # ~5GB
```

### Paso 3: Iniciar servidor

```bash
ollama serve
# Escucha en http://localhost:11434
```

### Paso 4: Configurar Spring Boot

```yaml
spring:
  ai:
    openai:
      base-url: http://localhost:11434/v1  # Cambiar a local
      api-key: ollama  # Dummy, Ollama no necesita auth
      chat:
        model: llama2  # O neural-chat
```

### Checkear

```bash
curl http://localhost:11434/api/generate \
  -X POST \
  -d '{"model": "llama2", "prompt": "Hola"}'
```

---

## 5. Setup Multi-Proveedor (Recomendado para Fase 2)

### Idea: Elegir proveedor por función

```yaml
spring:
  ai:
    # Proveedor por defecto
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4-turbo
    
    # Groq (tutor — rápido)
    groq:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai/v1
      chat:
        model: llama-3.3-70b-versatile
    
    # Anthropic (evaluador — reasoning)
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        model: claude-3-sonnet-20240229

# Configuración de funciones (RF-IA-23)
tutor:
  llm-function-assignment:
    tutor: groq        # Rápido, pedagógico
    evaluador: anthropic  # Reasoning fuerte
    moderador: openai   # General purpose
```

### En código

```java
@Service
public class TutorService {
    
    private final ChatClient groqClient;
    private final ChatClient anthropicClient;
    
    public String responderTutor(String pregunta) {
        // Usar Groq (rápido)
        return groqClient.prompt()
            .user(pregunta)
            .call()
            .getResult()
            .getOutput();
    }
    
    public ScoreIA evaluarRespuesta(String transcripcion) {
        // Usar Anthropic (reasoning)
        return anthropicClient.prompt()
            .user(construirPromptEvaluacion(transcripcion))
            .call()
            .getResult()
            .getOutput();
    }
}
```

---

## 6. Jerarquía de Preferencias para Demo

### MVP Hoy (elegir 1)

```
OPCIÓN A (Recomendada):
├─ Groq (tutor + evaluador)
└─ GRATIS, rápido, sin tarjeta

OPCIÓN B (Si tienes tarjeta OpenAI):
├─ OpenAI GPT-4-turbo (todo)
└─ $30-50 para demo exhaustiva

OPCIÓN C (Sin Internet):
├─ Ollama local (llama2)
└─ Lento, pero privado
```

### Decisión de diseño (PRD RF-IA-11, RF-IA-23)

Para MVP: **Una función ↔ Un modelo**

```
Tutor: Groq Llama-3.3-70b
Evaluador: Groq (mismo, es suficiente)
Moderador: N/A en MVP
```

Para Fase 2: Multi-modelo

```
Tutor: Groq (velocidad)
Evaluador: Claude (reasoning)
Moderador: GPT-3.5-turbo (balance cost/quality)
```

---

## 7. Variables de Entorno — Setup Rápido

### .env.local (Gitignore obligatorio)

```bash
# LLM Providers
GROQ_API_KEY=gsk_...
OPENAI_API_KEY=sk-proj-...
ANTHROPIC_API_KEY=sk-ant-...

# Base de datos (fase 2)
DB_HOST=db.supabase.co
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=...

# Rate limits (PAR-22)
RATE_LIMIT_MSGS_PER_DAY=100
RATE_LIMIT_RPM=10

# Ambiente
ENVIRONMENT=dev
LOG_LEVEL=DEBUG
```

### .gitignore (CRÍTICO)

```gitignore
.env.local
.env.*.local
*.key
secrets/
```

### application-local.yml

```yaml
# Cargado solo en dev (si usas spring profiles)
spring:
  profiles:
    active: local
  ai:
    openai:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai/v1
```

### Cómo cargar en Spring Boot

```java
// Opción 1: application.yml con variables
spring:
  ai:
    openai:
      api-key: ${GROQ_API_KEY}  # Lee de ENV

// Opción 2: application-local.yml + profile
java -Dspring.profiles.active=local -jar app.jar

// Opción 3: spring-cloud-starter-bootstrap + application-local.yml
// (más complejo, para proyecto grande)
```

---

## 8. Testear API Keys

### Test Groq

```java
@Test
void testGroqKey() {
    String key = System.getenv("GROQ_API_KEY");
    assertThat(key).startsWith("gsk_");
    
    ChatClient client = new ChatClient(groqChatModel);
    String respuesta = client.prompt()
        .user("Hola, ¿funciono?")
        .call()
        .getResult()
        .getOutput();
    
    assertThat(respuesta).isNotEmpty();
}
```

### Test OpenAI

```java
@Test
void testOpenAIKey() {
    String key = System.getenv("OPENAI_API_KEY");
    assertThat(key).startsWith("sk-");
    
    // Similar test con OpenAI ChatModel
}
```

### Test local (Ollama)

```bash
curl http://localhost:11434/api/generate \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"model": "llama2", "prompt": "test"}'

# Esperar respuesta JSON
```

---

## 9. Troubleshooting

| Error | Causa | Solución |
|-------|-------|----------|
| `401 Unauthorized` | API key inválida o expirada | Generar nueva key en consola proveedor |
| `429 Too Many Requests` | Rate limit excedido | Esperar 1 min, revisar rate-limit |
| `Connection refused localhost:11434` | Ollama no corriendo | `ollama serve` en otra terminal |
| `Model not found: llama2` | Modelo no descargado | `ollama pull llama2` |
| `GROQ_API_KEY not found` | Variable de entorno no seteada | `export GROQ_API_KEY=gsk_...` |

---

## 10. Resumen — Decisión Final

### Para demo en 1 día: **Groq** ✅
```bash
export GROQ_API_KEY=gsk_...
# Listo, sin pagar nada
```

### Para producción: **Multi-proveedor**
```yaml
tutor: groq (rápido)
evaluador: claude (reasoning)
fallback: openai (general)
```

### Para privacidad: **Ollama local**
```bash
ollama pull neural-chat
ollama serve
```

---

**Siguiente paso**: Voy a explicar la lógica del agente (cómo el tutor piensa y responde).
