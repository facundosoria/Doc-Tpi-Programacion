# Cálculo de Capacidad de Sprints — `llm-service`

> **Documento normativo de planificación ágil.**  
> **Ámbito:** `llm-service` (Tema 07 — Plataforma de Aprendizaje Gamificado).  
> **Duración del ciclo:** 14 días corridos (2 semanas por sprint).  
> **Estado:** Vigente y auditado matemáticamente contra la planilla oficial del equipo.

---

## 1. Reglas Fundamentales de Capacidad

1. **Segregación Estricta de Roles:**  
   - **Solo los Desarrolladores (Dev / Programadores)** integran la capacidad comprometible para el Backlog de Desarrollo (historias de usuario, tareas técnicas de software, endpoints, migraciones y pruebas).
   - **Soporte y QA NO computan como capacidad de desarrollo.** Sus horas se reservan exclusivamente para tareas operativas, guardias, infraestructura, coordinación de incidentes y aseguramiento de calidad.
2. **Reserva y Factor de Foco (80% de dedicación):**  
   - El 20% de reserva (`Factor 0,80`) **no es tiempo libre** ni capacidad adicional para inflar el alcance; cubre imprevistos, coordinación técnica asincrónica, desvíos en estimación y revisiones de PR.
3. **Escalas Independientes:**  
   - La capacidad se planifica y controla en **horas-persona**.  
   - Los **puntos Fibonacci** en Taiga representan esfuerzo relativo a la historia canónica (`LLM-S01-H06`) y **jamás se convierten matemáticamente a horas** (regla de cátedra).
4. **Capacidad ≠ Presupuesto a Llenar:**  
   - La suma de paquetes de las recetas de sprint representa un **piso verificable**, no un techo. La diferencia entre los paquetes estimados (~208 h) y la capacidad comprometible de desarrollo es **margen explícito** para absorber la complejidad del trabajo en parejas.

---

## 2. Auditoría Matemática de la Planilla Original (Equipo Completo: 12 Integrantes)

A continuación se transcribe la planilla del equipo tal como fue registrada originalmente con los 12 integrantes para un sprint quincenal (14 días).

| Integrante | Rol | Horas/día | Días Sprint | Ausencias | Horas Teóricas | Ceremonias | Horas Efectivas | % Dedicación | Capacidad Ajustada |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Lázaro Nieto** | Dev | 5,86 | 14 | 0 | 82,00 | 5,00 | 77,00 | 80% | 61,60 |
| **Ignacio Cantero** | Dev | 2,86 | 14 | 0 | 40,00 | 5,00 | 35,00 | 80% | 28,00 |
| **Fran Tejerina** | Dev | 4,29 | 14 | 0 | 60,00 | 5,00 | 55,00 | 80% | 44,00 |
| **Lucas Quevedo** | Dev | 5,29 | 14 | 0 | 74,00 | 5,00 | 69,00 | 80% | 55,20 |
| **Agostina Chapeta** | Dev | 4,71 | 14 | 0 | 66,00 | 5,00 | 61,00 | 80% | 48,80 |
| **Franco Chachagua** | Dev (SM rotativo) | 6,57 | 14 | 0 | 92,00 | 6,00 | 86,00 | 80% | 68,80 |
| **Facundo Soria** | Dev | 8,00 | 14 | 0 | 112,00 | 5,00 | 107,00 | 80% | 85,60 |
| **Constantino Picco** | Dev | 4,43 | 14 | 0 | 62,00 | 5,00 | 57,00 | 80% | 45,60 |
| **Franco Brizzio** | Dev | 6,71 | 14 | 0 | 94,00 | 5,00 | 89,00 | 80% | 71,20 |
| **Lara Heredia** | Dev | 6,86 | 14 | 0 | 96,00 | 5,00 | 91,00 | 80% | 72,80 |
| **Heber Vela** | Soporte | 3,43 | 14 | 0 | 48,00 | 5,00 | 43,00 | 80% | 34,40 |
| **Emma Knubel** | Soporte | 4,00 | 14 | 0 | 56,00 | 5,00 | 51,00 | 80% | 40,80 |
| **TOTALES** | — | — | **14** | **0** | **882,00** | **61,00** | **821,00** | **80%** | **656,80** |

### Control de consistencia de los cálculos de la planilla:

1. **Horas Teóricas Brutas ($882,00\text{ h}$):**
   - La columna "Horas/día" corresponde a la división de las horas teóricas declaradas sobre los 14 días del sprint (ej. $82 / 14 \approx 5,857 \to 5,86$). La suma de horas teóricas de los 12 integrantes suma exactamente **$882,00\text{ h}$**.
2. **Ceremonias ($61,00\text{ h}$):**
   - 11 integrantes computan $5,00\text{ h}$ de ceremonias ($11 \times 5 = 55\text{ h}$).
   - Franco Chachagua computa $6,00\text{ h}$ debido a su rol adicional de Scrum Master rotativo (+1 h de facilitación/preparación).
   - Total ceremonias: $55 + 6 = \mathbf{61,00\text{ h}}$.
3. **Horas Efectivas ($821,00\text{ h}$):**
   $$\text{Horas Efectivas} = \text{Horas Teóricas} - \text{Ausencias} - \text{Ceremonias} = 882,00 - 0 - 61,00 = \mathbf{821,00\text{ h}}$$
   La suma vertical de las horas efectivas individuales da exactamente **$821,00\text{ h}$**.
4. **Capacidad Ajustada ($656,80\text{ h}$):**
   $$\text{Capacidad Ajustada} = \text{Horas Efectivas} \times 0,80 = 821,00 \times 0,80 = \mathbf{656,80\text{ h}}$$
   La suma de las capacidades ajustadas individuales da exactamente **$656,80\text{ h}$**.
5. **Indicador "Capacidad respecto de un Sprint normal = 74,47%":**
   $$\frac{\text{Capacidad Ajustada}}{\text{Capacidad Teórica Bruta}} = \frac{656,80}{882,00} = 0,744671... \approx \mathbf{74,47\%}$$
   El cálculo del indicador en la segunda captura es exacto: expresa el factor neto de entrega del sprint tras descontar ceremonias (6,92%) y aplicar el 20% de reserva.

---

## 3. Capacidad Oficial de Desarrollo (Solo Devs / Programadores)

> [!IMPORTANT]
> **Directiva de Planificación:** Soporte y QA **no** se incluyen en la capacidad de programación.  
> Los compromisos de Sprint Backlog (Sprint 1 a 19) se calculan **única y exclusivamente** sobre los **10 desarrolladores**.

### 3.1 Tabla de Capacidad del Equipo Dev (10 Desarrolladores)

| Integrante | Rol | Horas Teóricas | Ceremonias | Horas Efectivas | Factor Foco | Capacidad Comprometible |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Lázaro Nieto** | Dev | 82,00 | 5,00 | 77,00 | 80% | **61,60 h** |
| **Ignacio Cantero** | Dev | 40,00 | 5,00 | 35,00 | 80% | **28,00 h** |
| **Fran Tejerina** | Dev | 60,00 | 5,00 | 55,00 | 80% | **44,00 h** |
| **Lucas Quevedo** | Dev | 74,00 | 5,00 | 69,00 | 80% | **55,20 h** |
| **Agostina Chapeta** | Dev | 66,00 | 5,00 | 61,00 | 80% | **48,80 h** |
| **Franco Chachagua** | Dev (SM rotativo) | 92,00 | 6,00 | 86,00 | 80% | **68,80 h** |
| **Facundo Soria** | Dev | 112,00 | 5,00 | 107,00 | 80% | **85,60 h** |
| **Constantino Picco** | Dev | 62,00 | 5,00 | 57,00 | 80% | **45,60 h** |
| **Franco Brizzio** | Dev | 94,00 | 5,00 | 89,00 | 80% | **71,20 h** |
| **Lara Heredia** | Dev | 96,00 | 5,00 | 91,00 | 80% | **72,80 h** |
| **TOTALES DEV** | **10 Desarrolladores** | **778,00 h** | **51,00 h** | **727,00 h** | **80%** | **581,60 h** |

### 3.2 Indicadores Clave del Equipo de Desarrollo

| Indicador Dev | Valor | Detalle de Cálculo |
| :--- | :---: | :--- |
| **Integrantes Dev** | **10** | Desarrolladores asignados a parejas P1 a P5 |
| **Capacidad Teórica Dev** | **778,00 h** | $882,00\text{ h (total)} - 104,00\text{ h (soporte)}$ |
| **Ceremonias Dev** | **51,00 h** | $(9 \times 5,00\text{ h}) + (1 \times 6,00\text{ h})$ |
| **Capacidad Efectiva Dev** | **727,00 h** | $778,00\text{ h} - 51,00\text{ h}$ |
| **Reserva Dev (20%)** | **145,40 h** | $727,00\text{ h} \times 0,20$ (imprevistos y coordinación) |
| **Capacidad Comprometible Dev** | **581,60 h** | **Techo máximo de horas de desarrollo por sprint** |
| **Rendimiento Neto Dev** | **74,76%** | $581,60 / 778,00$ |
| **Promedio por Desarrollador** | **58,16 h** | $581,60 / 10$ por sprint (quincenal) |
| **Promedio por Pareja (5 parejas P1..P5)** | **116,32 h** | $581,60 / 5$ por sprint (~58,16 h/semana por pareja) |

> [!NOTE]
> **Alineación con el Plan de Construcción (`docs/23` y `docs/30`):**  
> El modelo de arranque estimaba una capacidad teórica de ~571 h (~114 h por pareja). Con la planilla empírica real de 10 programadores, la capacidad efectiva comprometible es de **581,60 h** (**116,32 h por pareja**), demostrando consistencia directa entre la teoría de cátedra y los números reales declarados por los integrantes.

---

## 4. Capacidad de Soporte y Aseguramiento (Soporte / QA)

Las horas del personal de Soporte y QA no se destinan a cerrar historias de usuario del backlog de software, sino a estabilización, soporte a infraestructura y tareas transversales.

| Integrante | Rol | Horas Teóricas | Ceremonias | Horas Efectivas | Factor Foco | Capacidad Asignada |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Heber Vela** | Soporte | 48,00 | 5,00 | 43,00 | 80% | **34,40 h** |
| **Emma Knubel** | Soporte | 56,00 | 5,00 | 51,00 | 80% | **40,80 h** |
| **TOTALES SOPORTE** | **2 Integrantes** | **104,00 h** | **10,00 h** | **94,00 h** | **80%** | **75,20 h** |

### Destino de las Horas de Soporte (75,20 h):
- Guardia y mesa de ayuda interna a otros equipos consumidores del API Gateway.
- Verificación cruzada y smoke testing de ambientes locales (`docker compose`).
- Mantenimiento de bases de datos de prueba y limpieza de datos sintéticos.
- Seguimiento de incidencias de red, Docker, Kafka y dependencias bloqueantes (D01–D04).

---

## 5. Cuadro Comparativo Consolidado

| Dimensión | Desarrolladores (Dev) | Soporte / QA | Total Equipo (12 p) |
| :--- | :---: | :---: | :---: |
| **Cantidad de Personas** | 10 | 2 | 12 |
| **Horas Teóricas Brutas** | 778,00 h (88,21%) | 104,00 h (11,79%) | 882,00 h (100%) |
| **Ceremonias de Sprint** | 51,00 h | 10,00 h | 61,00 h |
| **Horas Efectivas Base** | 727,00 h | 94,00 h | 821,00 h |
| **Reserva de Contingencia (20%)** | 145,40 h | 18,80 h | 164,20 h |
| **Capacidad Comprometible / Ajustada** | **581,60 h** | **75,20 h** | **656,80 h** |
| **Aplicación en Sprint Planning** | **Compromiso de Software (HU/Tareas)** | **Operación y Calidad** | **Control General de Equipo** |

---

## 6. Procedimiento para Recalcular en Cada Sprint Planning

En cada reunión de Sprint Planning se debe abrir la planilla y aplicar la fórmula estándar:

```text
1. Disponibilidad Nominal Dev  = Suma de horas declaradas para el sprint por los 10 Devs
2. Ceremonias Dev             = Horas de ceremonias programadas para los Devs
3. Ausencias Dev              = Días feriados, exámenes o licencias de los Devs
4. Base Dev                   = Nominal Dev − Ceremonias Dev − Ausencias Dev
5. Reserva (20%)              = Base Dev × 0,20
6. Capacidad Comprometible    = Base Dev × 0,80
```

Si en un sprint un desarrollador se ausenta por exámenes o enfermedad, su base individual se reduce y **no debe trasladarse a otro integrante**. La capacidad comprometible total del sprint se ajusta hacia abajo en consecuencia antes de comprometer las historias.
