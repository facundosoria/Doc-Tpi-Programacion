# 🚀 Guía Paso a Paso: Configurar Supabase + pgvector para DEMOLLM

Esta guía explica detalladamente cómo crear desde cero una base de datos **PostgreSQL gratuita en la nube con Supabase**, habilitar la extensión vectorial **`pgvector`**, ejecutar el script de tablas para el RAG Multi-Documento y conectar el backend de **Spring Boot**.

---

## 📌 ¿Por qué Supabase?
- **100% Gratuito:** El Free Tier incluye 500 MB de almacenamiento (suficiente para miles de páginas de documentos).
- **No requiere tarjeta de crédito.**
- **`pgvector` nativo:** Ya viene preinstalado en el motor de PostgreSQL, solo requiere una línea SQL para activarlo.
- **Panel visual interactivo:** Posee un visor web de tablas (*Table Editor*) donde podrás ver en tiempo real los documentos subidos y sus fragmentos vectorizados.

---

## Paso 1: Crear la cuenta y el proyecto en Supabase

1. Ingresa a [https://supabase.com](https://supabase.com) y haz clic en **"Start your project"** o **"Sign in"**.
2. Inicia sesión con tu cuenta de **GitHub**.
3. En el panel principal (*Dashboard*), haz clic en el botón verde **"New Project"**.
4. Completa el formulario de creación:
   * **Organization:** Selecciona tu organización personal predeterminada.
   * **Name:** `demollm-rag-db` (o el nombre que prefieras).
   * **Database Password:** Escribe una contraseña segura o genera una.  
     ⚠️ **¡MUY IMPORTANTE!** Guarda esta contraseña en un bloc de notas; la necesitarás para el archivo de configuración de Spring Boot y no se vuelve a mostrar.
   * **Region:** Elige una región cercana para menor latencia:
     * Recomendada: `South America (São Paulo)` o `East US (North Virginia)`.
   * **Pricing Plan:** Asegúrate de que esté seleccionado **Free Plan ($0/month)**.
5. Haz clic en **"Create new project"**.
6. Espera entre 1 y 2 minutos mientras Supabase aprovisiona el servidor PostgreSQL.

---

## Paso 2: Habilitar `pgvector` y crear las tablas

Una vez que el proyecto esté en estado verde (*Active*):

1. En el menú lateral izquierdo de Supabase, haz clic en el ícono de **SQL Editor** (parece una consola o `>_`).
2. Haz clic en el botón **"+ New query"** (arriba a la izquierda).
3. Pega el siguiente script SQL completo en el editor:

```sql
-- ==============================================================================
-- 1. HABILITAR EXTENSIÓN VECTORIAL (pgvector)
-- ==============================================================================
CREATE EXTENSION IF NOT EXISTS vector;

-- ==============================================================================
-- 2. TABLA DE DOCUMENTOS (FUENTES NOTEBOOKLM)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS rag_documentos (
    document_id VARCHAR(64) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    page_count INT NOT NULL,
    chunk_count INT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    preview_text TEXT
);

-- ==============================================================================
-- 3. TABLA DE FRAGMENTOS VECTORIZADOS (CHUNKS)
-- Dimensión 768 correspondiente a Google text-embedding-004
-- ==============================================================================
CREATE TABLE IF NOT EXISTS rag_chunks (
    id VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL REFERENCES rag_documentos(document_id) ON DELETE CASCADE,
    document_name VARCHAR(255) NOT NULL,
    page_number INT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768)
);

-- ==============================================================================
-- 4. ÍNDICES DE RENDIMIENTO
-- Acelera el filtrado por documentos seleccionados y la búsqueda semántica
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_rag_chunks_document_id ON rag_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_rag_chunks_hnsw ON rag_chunks USING hnsw (embedding vector_cosine_ops);

-- ==============================================================================
-- 5. TABLAS DE HISTORIAL DE CONVERSACIÓN (JPA)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS conversacion (
    id UUID PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    estado VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS mensaje (
    id UUID PRIMARY KEY,
    conversacion_id UUID NOT NULL REFERENCES conversacion(id) ON DELETE CASCADE,
    rol VARCHAR(50) NOT NULL,
    contenido TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL
);
```

4. Haz clic en el botón verde **"Run"** (o presiona `Ctrl + Enter` / `Cmd + Enter`).
5. En la parte inferior deberías ver el mensaje:  
   `Success. No rows returned`.

6. **Verificación rápida:** Puedes confirmar que la extensión está activa ejecutando:
   ```sql
   SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
   ```
   Te responderá con `vector | 0.8.0` (o la versión vigente instalada).

---

## Paso 3: Obtener los datos de conexión (JDBC Connection String)

Para que Spring Boot se comunique con Supabase:

1. En el menú lateral izquierdo, haz clic en **Project Settings** (el ícono de engranaje ⚙️ abajo del todo).
2. Selecciona la pestaña **Database**.
3. Baja hasta la sección **Connection parameters** y **Connection string**.
4. Haz clic en la pestaña **URI** o **JDBC**.
5. Verás los datos con el siguiente formato:

| Parámetro | Dónde encontrarlo | Ejemplo típico |
| :--- | :--- | :--- |
| **Host** | En Connection parameters $\to$ Host | `db.xxxxxxxxxxxxxxxxxxxx.supabase.co` |
| **Port** | En Connection parameters $\to$ Port | `5432` |
| **Database** | Por defecto en Supabase | `postgres` |
| **User** | Por defecto en Supabase | `postgres` |
| **Password** | La contraseña que creaste en el **Paso 1** | *(Tu contraseña)* |

---

## Paso 4: Configurar Spring Boot en `demoLLMSpringAi`

### Opción A: Configurar directamente en `application.properties`

Abre el archivo `BE/src/main/resources/application.properties` y reemplaza la sección de base de datos con:

```properties
# ============================================================
# SUPABASE POSTGRESQL + PGVECTOR (Cloud)
# ============================================================
spring.datasource.url=jdbc:postgresql://db.TU_PROJECT_REF.supabase.co:5432/postgres?sslmode=require
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD_AQUI
spring.datasource.driver-class-name=org.postgresql.Driver

# Configuración Hibernate / JPA
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# ============================================================
# GOOGLE GEMINI (AI Studio)
# ============================================================
spring.ai.openai.base-url=${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta/openai}
spring.ai.openai.api-key=${GEMINI_API_KEY}
spring.ai.openai.chat.model=${GEMINI_MODEL:gemini-flash-lite-latest}
spring.ai.openai.chat.options.temperature=0.3
spring.ai.openai.chat.options.max-tokens=1500

# Modelo de Embeddings (768 dimensiones)
gemini.embedding.model=text-embedding-004
```

> 💡 **Nota sobre caracteres especiales en la contraseña:**  
> Si tu contraseña contiene caracteres como `@`, `:`, `/`, `%`, o espacios, en una URL JDBC común no suele haber problemas en la propiedad `spring.datasource.password=...`. Sin embargo, si usas una cadena de conexión completa tipo `postgresql://user:pass@host...`, esos caracteres deben ir URL-encodeados (por ejemplo `@` se escribe `%40`).

---

### Opción B: Usar Variables de Entorno (Recomendado para seguridad)

Si prefieres no dejar tu contraseña escrita en el archivo `.properties`:

En tu terminal de PowerShell / Bash o en la configuración de ejecución de IntelliJ/VS Code:

```powershell
# En Windows PowerShell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://db.TU_PROJECT_REF.supabase.co:5432/postgres?sslmode=require"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="TU_PASSWORD"
$env:GEMINI_API_KEY="TU_API_KEY_DE_GOOGLE"

# Ejecutar el backend
cd demoLLMSpringAi/BE
./mvnw spring-boot:run
```

---

## Paso 5: Cómo inspeccionar los datos visualmente en Supabase

Una de las grandes ventajas de Supabase es su interfaz gráfica:

1. En el menú lateral izquierdo, haz clic en **Table Editor** (el ícono de hoja de cálculo / tabla 🗄️).
2. Selecciona la tabla **`rag_documentos`**:
   * Cada vez que subas un PDF desde el frontend de Angular, verás aparecer una fila con el nombre del archivo, cantidad de páginas y fragmentos generados.
3. Selecciona la tabla **`rag_chunks`**:
   * Podrás ver el contenido extraído de cada página y la columna **`embedding`**, que mostrará el array numérico de 768 dimensiones calculado para cada fragmento.
4. Si eliminas un documento desde el panel del frontend, gracias a la regla `ON DELETE CASCADE`, todos sus fragmentos vectorizados se limpiarán automáticamente de la base de datos sin dejar registros huérfanos.

---

## 🛠️ Solución de Problemas Frecuentes (Troubleshooting)

### 1. `FATAL: password authentication failed for user "postgres"`
* **Causa:** La contraseña ingresada no coincide con la definida al crear el proyecto.
* **Solución:** Ve a **Project Settings** $\to$ **Database**, busca la sección **Database Password** y haz clic en **"Reset database password"** para colocar una nueva.

### 2. `Connection timed out` o problemas de resolución DNS
* **Causa:** Algunos proveedores de internet locales tienen problemas para resolver direcciones IPv6 directas de Supabase.
* **Solución:** En Supabase ve a **Database Settings** y activa el **Connection Pooling (Session Mode)** en el puerto `5432` o `6543`. La URL pasará a ser algo como:  
  `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres?sslmode=require`  
  con usuario `postgres.TU_PROJECT_REF`.

### 3. `type "vector" does not exist`
* **Causa:** No se ejecutó el comando `CREATE EXTENSION IF NOT EXISTS vector;`.
* **Solución:** Abre el **SQL Editor** en Supabase y ejecuta esa instrucción manualmente.
