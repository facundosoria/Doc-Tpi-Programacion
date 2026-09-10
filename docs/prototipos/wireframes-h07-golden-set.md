# Prototipos y Wireframes — LLM-S01-H07 (Golden Set)

> **Propósito:** Bocetos de baja fidelidad (wireframes) requeridos por la cátedra ([29 · §2.2 y §10](../29-guia-catedra-historias-de-usuario.md)) para la historia de usuario **`LLM-S01-H07`** (*Pantalla docente mínima del golden set*).
>
> **Trazabilidad BDD:** Cada botón, campo y estado visible en estos diagramas corresponde a un escenario de aceptación de `LLM-S01-H07` y de las operaciones backend de `LLM-S01-H05` y `LLM-S01-H06`.
>
> **Demo interactiva funcional:** El comportamiento dinámico de estos wireframes, la rúbrica 5D y el evaluador pueden probarse en la demo web interactiva ubicada en [`Demos/Golden Set/index.html`](../../Demos/Golden%20Set/index.html).

---

## 1. Vista 1 — Listado de Golden Sets por Cohorte

Permite al docente visualizar los conjuntos de casos de referencia de su cohorte, ordenados cronológicamente, con soporte de paginación y acceso a la creación de un nuevo lote.

```
+--------------------------------------------------------------------------------------------------+
|  UTN - Plataforma Gamificada | Módulo LLM                                  [👤 Prof. García (P5)]|
+--------------------------------------------------------------------------------------------------+
|  Cohorte: [ Programación III - 2026 2C ▼ ]              Estado: 🟢 Gateway Conectado             |
|                                                                                                  |
|  Gestión de Casos de Referencia (Golden Set)                                                     |
|  Referencia humana base para la calibración del evaluador analítico (PAR-14)                     |
|                                                                                                  |
|  [ + Nuevo Golden Set ]                                                   [ 🔍 Buscar / Filtro ] |
|                                                                                                  |
|  +--------------------------------------------------------------------------------------------+  |
|  | Versión   | Rúbrica | Idioma | Fecha Creación   | Entradas | Estado       | Acciones       |  |
|  +-----------+---------+--------+------------------+----------+--------------+----------------+  |
|  | GS-v1.0.2 | 1.0     | es     | 2026-09-04 18:20 | 12       | 🔒 Publicado | [ Ver Detalle ]|  |
|  | GS-v1.0.1 | 1.0     | es     | 2026-09-01 10:15 | 8        | 📝 En Carga  | [ Ver Detalle ]|  |
|  +--------------------------------------------------------------------------------------------+  |
|  Página 1 de 1  (Mostrando 2 resultados)                                [ Anterior ] [ Siguiente]|
+--------------------------------------------------------------------------------------------------+
|  ⚠️ Nota: Las llamadas se canalizan exclusivamente por el Gateway (/api/llm/**).                  |
+--------------------------------------------------------------------------------------------------+
```

---

## 2. Vista 2 — Modal / Formulario de Alta de Golden Set (Paso 1)

Inicia un nuevo contenedor de casos de referencia inmutable. Las variables de rúbrica e idioma están fijadas para garantizar el estándar transversal de la cátedra.

```
+--------------------------------------------------------------------------------------------------+
|  Modal: Alta de Golden Set                                                                   [X] |
+--------------------------------------------------------------------------------------------------+
|                                                                                                  |
|  Cohorte Asignada: Programación III - 2026 2C (Titular: Prof. García)                            |
|                                                                                                  |
|  Rúbrica de Plataforma:                                                                          |
|  [ 1.0 (5 Dimensiones Oficiales: Autonomía 30%, Claridad 25%, Progresión 20%, ...) 🔒 ]         |
|                                                                                                  |
|  Idioma de Evaluación:                                                                           |
|  [ Español (es) 🔒 ]                                                                             |
|                                                                                                  |
|  🛡️ Política de Inmutabilidad:                                                                   |
|  Una vez creado el Golden Set, el esquema es append-only. Las entradas agregadas no podrán ser   |
|  editadas destructivamente ni eliminadas de la base de datos de auditoría.                       |
|                                                                                                  |
|  [ Cancelar ]                                               [ Crear Golden Set (Idempotente) ]   |
|                                                                                                  |
+--------------------------------------------------------------------------------------------------+
```

---

## 3. Vista 3 — Detalle y Carga de Entradas con Rúbrica 5D (Paso 2)

Permite al docente cargar una transcripción de interacción alumno-tutor e ingresar las cinco calificaciones de referencia humana (0 a 100) con sus ponderaciones oficiales.

```
+--------------------------------------------------------------------------------------------------+
|  ← [ Volver al Listado ]                  Golden Set: GS-v1.0.1 (En Carga)       [👤 Prof. García]|
+--------------------------------------------------------------------------------------------------+
|  Cohorte: Programación III - 2026 2C | Rúbrica: v1.0 | Idioma: es | Total Casos: 8                |
|                                                                                                  |
|  +-- NUEVA ENTRADA DE REFERENCIA (HUMAN GROUND-TRUTH) ----------------------------------------+  |
|  |                                                                                            |  |
|  |  🛡️ Transcripción de Interacción Alumno-Tutor (JSON Pasivo - No Interpretable):             |  |
|  |  +--------------------------------------------------------------------------------------+  |  |
|  |  | [                                                                                    |  |  |
|  |  |   {"role": "user", "content": "Tengo un problema con el puntero en el nodo AVL..."}, |  |  |
|  |  |   {"role": "assistant", "content": "¿Qué ocurre cuando ejecutas la rotación simple?"} |  |  |
|  |  | ]                                                                                    |  |  |
|  |  +--------------------------------------------------------------------------------------+  |  |
|  |  ℹ️ Formato: Array JSON de mensajes. El sistema procesa el contenido como dato inerte.       |  |
|  |                                                                                            |  |
|  |  Puntuaciones Humanas de Referencia (Rúbrica Oficial RF-IA-15):                            |  |
|  |  +--------------------------------------------------------------------------------------+  |  |
|  |  | 1. Autonomía y Crítica (30%):    [ 85 ] pts  (0 - 100)                               |  |  |
|  |  | 2. Claridad de Prompts (25%):    [ 70 ] pts  (0 - 100)                               |  |  |
|  |  | 3. Progresión Lógica (20%):      [ 90 ] pts  (0 - 100)                               |  |  |
|  |  | 4. Cumplimiento Límites (15%):   [ 95 ] pts  (0 - 100)                               |  |  |
|  |  | 5. Eficiencia Diálogo (10%):     [ 80 ] pts  (0 - 100)                               |  |  |
|  |  +--------------------------------------------------------------------------------------+  |  |
|  |  Puntaje Ponderado Calculado: 83.0 / 100 pts                                               |  |
|  |                                                                                            |  |
|  |  [ Limpiar ]                                                    [ Guardar Entrada (POST) ] |  |
|  +--------------------------------------------------------------------------------------------+  |
|                                                                                                  |
|  Casos Registrados en este Golden Set (8):                                                       |
|  • Entrada #8: Score 83.0 pts | 2 mensajes | Creado: 2026-09-05 21:10 [ Ver Transcripción ]      |
|  • Entrada #7: Score 91.5 pts | 4 mensajes | Creado: 2026-09-05 20:45 [ Ver Transcripción ]      |
|  • ...                                                                                           |
+--------------------------------------------------------------------------------------------------+
```

---

## 4. Estados de Error y Accesibilidad (WCAG 2.1 AA)

### Estado: Backend no disponible o Gateway caído (Escenario 2 de H07)
```
+--------------------------------------------------------------------------------------------------+
|  ⚠️ [ALERTA: role="alert"]                                                                        |
|  No se pudo contactar al backend a través del Gateway.                                           |
|  Verifique que los servicios de Docker estén levantados o intente nuevamente en unos instantes.  |
|  [ Reintentar Conexión ]                                                                         |
+--------------------------------------------------------------------------------------------------+
```

### Estado: Transcripción no válida (Escenario 3 de H07)
```
+--------------------------------------------------------------------------------------------------+
|  ❌ [ERROR DE VALIDACIÓN: role="alert"]                                                           |
|  La transcripción ingresada no es un arreglo JSON válido. Asegúrese de incluir al menos un       |
|  mensaje con formato {"role": "...", "content": "..."}.                                          |
+--------------------------------------------------------------------------------------------------+
```

### Estado: Acceso denegado / Cohorte ajena (Escenario 4 de H07 / CA3 de H05)
```
+--------------------------------------------------------------------------------------------------+
|  ⛔ [403 FORBIDDEN: role="alert"]                                                                |
|  No autorizado. Su usuario no posee permisos de edición sobre la cohorte seleccionada.           |
+--------------------------------------------------------------------------------------------------+
```

---

## 5. Matriz de Trazabilidad: Elementos del Boceto ↔ Escenarios BDD

| Elemento del Wireframe | Vista | Escenario BDD que lo valida | Comportamiento esperado |
|---|---|---|---|
| Botón `[ + Nuevo Golden Set ]` y `[ Crear ]` | V1, V2 | **H05 · Escenario 1** (Camino feliz) | Envía `POST /api/llm/golden-sets` con `Idempotency-Key` y retorna HTTP 201 + `Location`. |
| Reintento con misma clave | V2, V3 | **H05 · Escenario 2** (Idempotencia) | No duplica el registro en la base de datos ni altera la lista. |
| Selector de Cohorte no asignada | V1, V2 | **H05 · Escenario 3** y **H07 · Escenario 4** | Muestra banner accesible `403 Forbidden` y bloquea la acción. |
| Formulario de 5 Dimensiones (0-100) | V3 | **H05 · Escenario 4** | Si falta una dimensión o es $<0$ o $>100$, el botón se deshabilita y marca error `400`. |
| Tabla de Casos y Persistencia | V1, V3 | **H06 · Escenario 1** (Reinicio) | La lista muestra los casos tras `docker compose restart` sin pérdida de datos. |
| Detección de caída de servicio | Global | **H07 · Escenario 2** (Backend no disp.) | Mensaje en `role="alert"` sin romper la aplicación ni dejar pantalla blanca. |
| Área de Transcripción JSON | V3 | **H07 · Escenario 3** (JSON inválido) | Valida sintaxis JSON en cliente antes del envío y bloquea si no es array no vacío. |
