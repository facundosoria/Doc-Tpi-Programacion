# Plantilla — Historia de Usuario (Wiki / backlog de Taiga)

> Template oficial de Historia de Usuario. Uso **obligatorio** para uniformidad del
> backlog.
>
> Copiar este contenido al crear una HU nueva. Reemplazar `GXX` por el número de
> grupo/equipo y borrar los textos de ejemplo antes de publicar. El ID interno del
> equipo sigue el esquema `Sxx-Hyy` y vive dentro de la ficha; el título en Taiga usa
> el formato `GXX — TÍTULO`.
>
> Esta plantilla es el **formato de presentación**. Las compuertas de la historia (DoR
> para comprometerla, DoD para aceptarla) están en `dor-dod.md` y no se renegocian
> acá. Cómo se redacta y estima cada apartado: `guia-metodo.md`.

---

# [GXX] — [TÍTULO DE LA HISTORIA DE USUARIO]

> **ID interno:** `Sxx-Hyy` · **Tipo:** [HU de valor / Tarea de sprint (habilitador)] ·
> **Épica:** [EP-0X] · **Responsable / suplente:** [·] · **Requisito:** [RF-* / PAR-*]

## Descripción (Como / Quiero / Para)

- **Como:** [ROL REAL — no «el sistema» ni «el equipo» a secas]
- **Quiero:** [ACCIÓN DEL USUARIO — no una solución técnica]
- **Para:** [PROPÓSITO / VALOR — no repetir la acción con otras palabras]

## Notas / Observaciones

- **Reglas de negocio:** [DETALLAR]
- **Validaciones:** [DETALLAR]
- **Datos obligatorios:** [LISTAR CAMPOS]
- **Performance (tiempos, volumen, límites):** [DETALLAR o «no aplica»]
- **Seguridad (roles, permisos, datos sensibles):** [DETALLAR]
- **Accesibilidad (WCAG / teclado / lectores):** [DETALLAR o «no aplica» + motivo]
- **Otros:** [DETALLAR]
- **Endpoints (si cruza servicios):** `[MÉTODO /ruta]` → [respuesta]

## Criterios de Aceptación (CA)

- **CA1:** [CONDICIÓN MEDIBLE Y OBJETIVA]
- **CA2:** [CONDICIÓN MEDIBLE Y OBJETIVA]
- **CA3:** [CONDICIÓN MEDIBLE Y OBJETIVA]
- **CA4 (negativo):** [no autorizado / entrada inválida / duplicado / dependencia caída]
- **CA5 (negativo):** [otra condición de error]

> Incluir siempre los criterios **negativos**, no solo el camino feliz.

## BDD (mínimo 3 escenarios — 1 camino feliz + ≥ 2 negativos)

**Característica:** [NOMBRE / OBJETIVO DE LA FUNCIONALIDAD]

### Escenario 1 — [título del camino feliz]

- **Dado:** [contexto inicial / precondiciones]
- **Cuando:** [acción del usuario o del sistema]
- **Entonces:** [resultado observable: pantalla, mensaje o dato guardado]
- **Y:** [opcional — hereda el paso anterior]

### Escenario 2 — [título del negativo]

- **Dado:** [contexto]
- **Cuando:** [acción]
- **Entonces:** [resultado observable]

### Escenario 3 — [título del negativo]

- **Dado:** [contexto]
- **Cuando:** [acción]
- **Entonces:** [resultado observable]

## Prototipo

- **Capturas / Wireframes:** [boceto de baja fidelidad de las pantallas principales, o
  «no aplica» + motivo si es borde técnico]
- **URL Figma:** [https://... o «pendiente»]
- **Storybook:** [https://... o «pendiente»]
- **Mock API / Swagger:** `[GET /api/...]`, `[POST /api/...]`

## Estimación / Prioridad

| Puntos (Fibonacci) | Prioridad (MoSCoW) |
|---|---|
| *(a asignar en Sprint 0 con Planning Poker contra la historia canónica)* | [Must / Should / Could / Won't] |

> Referencia de planificación en horas del plan (si el equipo planifica en horas):
> **[N] h** — dato separado, no se convierte a puntos.

## Dependencias / Impactos

- **Servicios involucrados:** [LISTAR]
- **Módulos afectados:** [LISTAR]
- **Otros equipos / aprobaciones:** [LISTAR]
- **Impacto en datos / migraciones:** [DETALLAR]
- **Riesgos y mitigación:** [DETALLAR]

## Tareas

> Pasos técnicos internos del equipo. **Sin** formato Como/Quiero/Para. En el orden de
> construcción del equipo. Cada tarea ≤ 1 jornada efectiva. Horas orientativas que suman
> la referencia de la historia.

| # | Tarea | Foco / Paso | h |
|---|---|---|--:|
| T1 | [·] | [Contrato y amenaza] | · |
| T2 | [·] | [Dominio y migración] | · |
| T3 | [·] | [Caso de uso] | · |
| T4 | [·] | [Adaptadores] | · |
| T5 | [·] | [Seguridad y resiliencia] | · |
| T6 | [·] | [Observabilidad] | · |
| T7 | [·] | [Prueba E2E / demo] | · |
| | **Total** | | **·** |
