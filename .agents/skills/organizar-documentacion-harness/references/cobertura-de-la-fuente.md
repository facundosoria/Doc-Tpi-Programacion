# Cobertura de la fuente (sección 23 y cita final de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.
> Esta lista es el control de fidelidad de todo el skill: si algún punto deja de estar
> cubierto en `SKILL.md` o en alguna de las referencias de `references/`, se considera una
> reducción de cobertura y requiere una decisión explícita, no una simplificación automática.

## 23. Cobertura de la fuente

Para evitar que la adaptación se convierta en un resumen, esta guía conserva explícitamente:

- la distinción modelo/agente/harness;
- la lista de capacidades que debe tener un agente;
- el diagrama de contrato, contexto, política, herramientas, estado, checks, trazas y recuperación;
- las cinco preguntas del contrato y el ejemplo YAML;
- el mapa de proyecto y la divulgación progresiva;
- los criterios del compilador de contexto;
- el contrato de herramienta `edit_file`;
- las capacidades de la puerta de herramientas;
- la separación modelo/gateway/tool/sensor;
- la separación cerebro/manos/historial;
- las cuatro categorías de memoria durable y su ejemplo YAML;
- la regla de que la afirmación "done" no es evidencia;
- la tabla afirmación/evidencia y la cadena de comprobaciones;
- la distinción entre modelos para ambigüedad y código para mecánica;
- la verificación asimétrica trabajador/evaluador;
- la rúbrica, contexto, herramientas independientes y permiso de rechazo;
- las políticas que no deben depender del prompt;
- los cuatro niveles de riesgo y sus barreras;
- las clases de fallo y sus recuperaciones;
- el requisito de cambiar una condición al reintentar;
- el bucle observar-decidir-actuar-medir;
- los cinco presupuestos del bucle;
- la escalera explicación-checklist-plantilla-check automático-política;
- el principio de mover invariantes a infraestructura;
- la observabilidad de proceso, sus eventos y métricas;
- el recibo de cambios y el handoff por estado/evidencia/riesgo;
- el análisis de fallos para mejorar el harness;
- el volante del harness;
- la degradación del harness y las preguntas de mantenimiento;
- los seis niveles del harness mínimo viable;
- la especificación reutilizable de ocho bloques;
- las métricas de trabajo aceptado y revisión humana;
- los casos donde no hace falta un harness pesado;
- el cambio de prompt engineering a harness engineering;
- el procedimiento y checklist específicos para organizar documentación.

Si una futura edición elimina alguno de estos puntos, debe considerarse una reducción de cobertura y requerir una decisión explícita, no una simplificación automática.

## Mapa de cobertura por archivo

| Punto de la lista | Dónde vive |
|---|---|
| Distinción modelo/agente/harness, capacidades del agente, diagrama, planteo inicial | `references/fundamentos-harness.md` |
| Cinco preguntas del contrato, YAML de contrato, mapa de proyecto, divulgación progresiva, compilador de contexto | `references/contrato-y-contexto.md` |
| Contrato `edit_file`, puerta de herramientas, modelo/gateway/tool/sensor, cerebro/manos/historial, memoria durable (FACTS/DECISIONS/PROGRESS/LESSONS) | `references/herramientas-y-arquitectura.md` |
| "done" no es evidencia, tabla afirmación/evidencia, cadena de comprobaciones, modelos vs. código, verificación asimétrica, rúbrica de rechazo | `references/evidencia-y-verificacion.md` |
| Políticas fuera del prompt, cuatro niveles de riesgo, clases de fallo, condición de cambio al reintentar, bucle observar-decidir-actuar-medir, presupuestos | `references/politica-y-recuperacion.md` |
| Escalera explicación→política, invariantes como infraestructura, observabilidad, eventos de traza, recibo de cambios y handoff | `references/infraestructura-y-observabilidad.md` |
| Volante del harness, degradación, preguntas de mantenimiento, métricas de trabajo aceptado, casos sin harness pesado, cambio prompt→harness engineering | `references/mantenimiento-del-harness.md` |
| Seis niveles del harness mínimo viable, especificación reutilizable de ocho bloques | `references/spec-reutilizable.md` |
| Procedimiento (Fases A-G) y checklist específicos para organizar documentación | `SKILL.md` |

## Fuente

- Lunar, "Harness Engineering: The Complete Guide to Building AI Agents That Don't Fall Apart", PDF proporcionado por el usuario: `~/Downloads/Lunar en X: "Harness Engineering: The Complete Guide to Building AI Agents That Don't Fall Apart" : X.pdf`.
- La guía se elaboró a partir del contenido completo extraído del PDF y se orientó a la organización de documentación del repositorio.
- Documento origen de este skill: `docs/17-guia-organizacion-documentacion-harness-engineering.md`.
