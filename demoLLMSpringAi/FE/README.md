# Frontend — Tutor IA Pedagógico (Angular 21 + Node.JS)

Frontend moderno desarrollado en **Angular 21** y **Node.JS**, diseñado con arquitectura Standalone Components, Signals reactivos y layout Split-Screen interactivo para el sistema RAG (Retrieval-Augmented Generation) de Spring AI.

---

## 🛠️ Tecnologías Utilizadas

- **Node.JS**: `v24.15.0` (LTS compatible)
- **Angular**: `v21.2.x` (Standalone, Signals, Control Flow moderno `@if`/`@for`)
- **Angular CLI**: `v21.2.23`
- **TypeScript**: `~5.9.2`
- **Marked**: Parseo y renderizado seguro de Markdown pedagógico
- **FontAwesome 6 & Google Fonts (Plus Jakarta Sans, JetBrains Mono)**

---

## 🎯 Características y Funcionalidades

### 1. Panel Izquierdo (RAG & Material Educativo):
- **Zona Drag & Drop**: Arrastra cualquier archivo PDF educativo (hasta 25 MB) o haz clic para seleccionarlo.
- **Carga Rápida de Muestra**: Botón *"Cargar PDF de Prueba (PRD Gamificado)"* para probar el sistema de inmediato con el documento de 46 páginas incluido en el proyecto.
- **Métricas de Indexación en Memoria**: Muestra nombre, tamaño, total de páginas, cantidad de fragmentos (chunks) y extracto del material.
- **Explorador de Fragmentos**: Acordeón interactivo para auditar cada chunk de texto y su página de origen.
- **Panel de Guardrails**: Información visual de los filtros de seguridad y ahorro de tokens activos.

### 2. Panel Derecho (Chatbot Tutor Pedagógico):
- **Tutor con Adaptación de Rol**: Adopta el rol docente específico según el tema del documento (ej. Profesor de Programación / Proyectos de Software).
- **Respuestas Cortas y Concisas**: Explicaciones directas a la duda puntual en 2 a 3 párrafos breves, sin divagaciones.
- **Fuentes Citadas del PDF**: Cada respuesta del tutor incluye un acordeón desplegable que muestra la página exacta y el porcentaje de relevancia del fragmento utilizado.
- **Múltiples Validaciones de Uso (Pre-LLM / 0 Tokens)**:
  - Documento obligatorio antes de consultar.
  - Filtro de malas palabras y lenguaje inapropiado (Español e Inglés).
  - Escudo contra inyecciones de prompt y alteración de rol (DAN, bypass).
  - Validación de longitud (4 a 600 caracteres) con contador dinámico.
  - Cooldown anti-flood (1.2 s).
  - Detección de spam y caracteres repetitivos.
- **Contador de Tokens Ahorrados**: Métrica en tiempo real que contabiliza los tokens no gastados gracias a las validaciones y al caché en memoria.

---

## 🚀 Cómo Ejecutar

### Opción A: Servidor de Desarrollo Angular (Puerto 4200)

1. En la carpeta `FE/`, asegúrate de tener las dependencias instaladas:
   ```powershell
   cd d:\Users\Usuario\Desktop\demoLLMSpringAi\FE
   npm install
   ```

2. Inicia el servidor de desarrollo de Angular:
   ```powershell
   npm start
   # o: npx ng serve
   ```

3. Abre en tu navegador:
   ```
   http://localhost:4200/
   ```
   *(El frontend se comunicará automáticamente con la API en `http://localhost:8080`)*

---

### Opción B: Ejecución Unificada con Spring Boot (Puerto 8080)

Los bundles compilados de producción de Angular 21 ya están integrados en `BE/src/main/resources/static/`.

1. En la carpeta `BE/`, arranca Spring Boot:
   ```powershell
   cd d:\Users\Usuario\Desktop\demoLLMSpringAi\BE
   $env:JAVA_HOME = "C:\Users\Usuario\.jdks\ms-21.0.11"
   .\mvnw.cmd spring-boot:run
   ```

2. Abre en tu navegador:
   ```
   http://localhost:8080/
   ```
