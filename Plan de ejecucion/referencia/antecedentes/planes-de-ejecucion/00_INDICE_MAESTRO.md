# 📋 Índice Maestro de Planes Técnicos: Sección 15 (RF-IA-XX)

> [!IMPORTANT]
> **Metodología y Reglas de Trabajo**:
> Este directorio (`planes_seccion_15/`) es el repositorio central de diseño y mitigación técnica para la Sección 15 del PRD.
> - Cada punto normativo se planifica en su propio documento técnico numerado (`01_` a `08_`).
> - A medida que se elabora cada plan individual, se actualiza su casilla de estado a `[x] Planificado` y se vincula el archivo correspondiente.
> - **Regla Estricta:** Ninguna modificación en el código del microservicio o en la guía didáctica se llevará a cabo sin que su plan previo haya sido redactado, revisado y validado.

---

## 📊 Matriz de Estado de Planificación

| # | Tema / Requerimiento | RF-IA / PRD | Estado | Documento de Plan Asociado |
|---|----------------------|-------------|--------|----------------------------|
| **1** | Clasificación de Desafíos por Riesgo de Fuga | RF-IA-19 / Tabla 8 | `[x] Implementado (Tests OK)` | [`01_PLAN_CLASIFICACION_RIESGO_FUGA.md`](01_PLAN_CLASIFICACION_RIESGO_FUGA.md) |
| **2** | Golden Set en Doble Nivel (Global y Curso Bloqueante) | RF-IA-30, 30b, 36, 36b / PAR-14 | `[x] Implementado (Tests OK)` | [`02_PLAN_GOLDEN_SET_DOBLE_NIVEL.md`](02_PLAN_GOLDEN_SET_DOBLE_NIVEL.md) |
| **3** | Resiliencia ante Caídas: Score Neutro y Cálculo Diferido | RF-IA-27 / PAR-05 | `[x] Implementado (Tests OK)` | [`03_PLAN_RESILIENCIA_Y_CALCULO_DIFERIDO.md`](03_PLAN_RESILIENCIA_Y_CALCULO_DIFERIDO.md) |
| **4** | Bloqueo de Cierre/Archivado de Cursos por Diferidos | RF-IA-34 / RF-RNK-10 | `[x] Implementado (Tests OK)` | [`04_PLAN_BLOQUEO_CIERRE_CURSOS.md`](04_PLAN_BLOQUEO_CIERRE_CURSOS.md) |
| **5** | Trazabilidad de Cohortes y Cambio de Modelo Activo | RF-IA-33 / RF-IA-28 | `[x] Implementado (Tests OK)` | [`05_PLAN_TRAZABILIDAD_CAMBIO_MODELO.md`](05_PLAN_TRAZABILIDAD_CAMBIO_MODELO.md) |
| **6** | Auditoría Humana Obligatoria en Umbrales P90 / Regularidad | RF-IA-17 / PAR-10 | `[x] Implementado (Tests OK)` | [`06_PLAN_AUDITORIA_UMBRALES_P90.md`](06_PLAN_AUDITORIA_UMBRALES_P90.md) |
| **7** | Registro y Telemetría de Jailbreak sin Tolerancia | RF-IA-10 / RF-IA-13 | `[x] Implementado (Tests OK)` | [`07_PLAN_REGISTRO_JAILBREAK_INCIDENTES.md`](07_PLAN_REGISTRO_JAILBREAK_INCIDENTES.md) |
| **8** | Calibración Multilingüe e Invarianza de Rúbrica | RF-IA-31 / Sección 17 | `[x] Implementado (Tests OK)` | [`08_PLAN_CALIBRACION_MULTILINGUE.md`](08_PLAN_CALIBRACION_MULTILINGUE.md) |
| **9** | Frontend, Vistas de Usuario y Workbench de Pruebas | RF-IA-04, 13, 17, 30 | `[x] Implementado (Tests OK)` | [`09_PLAN_FRONTEND_Y_WORKBENCH_TESTING.md`](09_PLAN_FRONTEND_Y_WORKBENCH_TESTING.md) |

---

## 🔍 Detalle Exhaustivo de Requerimientos por Punto

---

### Punto 1: Clasificación de Desafíos por Nivel de Riesgo de Fuga
* **Normativa PRD:** RF-IA-19, Tabla 8 ("Matriz de Riesgo de Fuga según Tipo de Ejercicio"), RF-IA-04, PAR-11.
* **Problema a Mitigar:** Evitar que el Tutor entregue código o snippets en ejercicios donde una sola línea resuelve el desafío.
* **Estado:** ✅ **Planificado** en [`01_PLAN_CLASIFICACION_RIESGO_FUGA.md`](01_PLAN_CLASIFICACION_RIESGO_FUGA.md).

---

### Punto 2: El "Golden Set" en Doble Nivel (Global y Bloqueante por Curso)
* **Normativa PRD:** RF-IA-30, RF-IA-30b, RF-IA-31, RF-IA-36, RF-IA-36b, PAR-14 ($\pm 5$ puntos de tolerancia).
* **Problema a Mitigar:** Garantizar que la IA evaluadora esté calibrada tanto a nivel plataforma (ADMIN) como temáticamente por cada cátedra (Docente), bloqueando la activación del curso si no se cumple el umbral.
* **Estado:** ✅ **Planificado** en [`02_PLAN_GOLDEN_SET_DOBLE_NIVEL.md`](02_PLAN_GOLDEN_SET_DOBLE_NIVEL.md).

---

### Punto 3: Resiliencia ante Caídas: Score Neutro y Cálculo Diferido
* **Normativa PRD:** RF-IA-27, PAR-05.
* **Problema a Mitigar:** Asegurar que ninguna caída de LLM bloquee el progreso o entrega de ejercicios del alumno.
* **Estado:** ✅ **Planificado** en [`03_PLAN_RESILIENCIA_Y_CALCULO_DIFERIDO.md`](03_PLAN_RESILIENCIA_Y_CALCULO_DIFERIDO.md).

---

### Punto 4: Bloqueo de Cierre / Archivado de Cursos por Evaluaciones Diferidas
* **Normativa PRD:** RF-IA-34, RF-RNK-10.
* **Problema a Mitigar:** Prevenir que un docente cierre o archive un curso si existen evaluaciones de IA diferidas pendientes que podrían alterar rankings y actas selladas.
* **Estado:** ✅ **Planificado** en [`04_PLAN_BLOQUEO_CIERRE_CURSOS.md`](04_PLAN_BLOQUEO_CIERRE_CURSOS.md).

---

### Punto 5: Trazabilidad de Cohortes Afectadas por Cambio de Modelo Activo
* **Normativa PRD:** RF-IA-33, RF-IA-28, RF-IA-17.
* **Problema a Mitigar:** Controlar el impacto evaluativo y los sesgos al actualizar o cambiar de proveedor/versión de LLM en medio de un período lectivo.
* **Estado:** ✅ **Planificado** en [`05_PLAN_TRAZABILIDAD_CAMBIO_MODELO.md`](05_PLAN_TRAZABILIDAD_CAMBIO_MODELO.md).

---

### Punto 6: Revisión Humana Obligatoria en Umbrales de Promoción P90 y Regularidad
* **Normativa PRD:** RF-IA-17, PAR-10, P90.
* **Problema a Mitigar:** Garantizar que ninguna decisión académica de alto impacto (Promoción Directa P90 o Regularidad) dependa exclusivamente del modificador de IA sin supervisión docente.
* **Estado:** ✅ **Planificado** en [`06_PLAN_AUDITORIA_UMBRALES_P90.md`](06_PLAN_AUDITORIA_UMBRALES_P90.md).

---

### Punto 7: Registro y Telemetría de Incidentes de Jailbreak sin Tolerancia
* **Normativa PRD:** RF-IA-10, RF-IA-13 (Dimensión 4), Q11.
* **Problema a Mitigar:** Registrar de forma inmutable cada intento de manipulación del LLM y aplicar penalizaciones automáticas directas.
* **Estado:** ✅ **Planificado** en [`07_PLAN_REGISTRO_JAILBREAK_INCIDENTES.md`](07_PLAN_REGISTRO_JAILBREAK_INCIDENTES.md).

---

### Punto 8: Calibración Multilingüe e Invarianza de Rúbrica
* **Normativa PRD:** RF-IA-31, Sección 17.
* **Problema a Mitigar:** Eliminar sesgos de severidad o laxitud del evaluador al calificar en diferentes idiomas (Español vs Inglés).
* **Estado:** ✅ **Planificado** en [`08_PLAN_CALIBRACION_MULTILINGUE.md`](08_PLAN_CALIBRACION_MULTILINGUE.md).

---

### Punto 9: Arquitectura de Frontend, Vistas de Usuario y Workbench de Pruebas
* **Normativa PRD:** RF-IA-04, RF-IA-10/13, RF-IA-17/18, RF-IA-19, RF-IA-22, RF-IA-27, RF-IA-30/30b/36, RF-IA-31, RF-IA-33/34.
* **Problema a Mitigar:** Diseñar los componentes visuales e interactivos de cara al alumno, docente y admin sin invadir otros microservicios, proporcionando un *Developer Workbench / Testbed* para pruebas E2E inmediatas del backend de IA.
* **Estado:** ✅ **Planificado** en [`09_PLAN_FRONTEND_Y_WORKBENCH_TESTING.md`](09_PLAN_FRONTEND_Y_WORKBENCH_TESTING.md).

