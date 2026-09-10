# Plantilla — Historia de Usuario (Wiki / backlog de Taiga)

> Template oficial de Historia de Usuario de la Wiki de Taiga «Plataforma de
> Aprendizaje Gamificado de Programación». Uso **obligatorio** para uniformidad
> (ver [27 · Guía de la Wiki §7](../27-guia-wiki-taiga.md)).
>
> Copiar este contenido al crear una HU nueva en el backlog. Reemplazar `GXX` por el
> número de grupo asignado por la cátedra y borrar los textos de ejemplo antes de
> publicar. Cada HU se referencia desde la página `GXX - TEMA` por su **enlace
> permanente** del backlog ([27 §3.c](../27-guia-wiki-taiga.md)).
>
> Esta plantilla es el **formato de presentación**; las compuertas de la historia
> (DoR para comprometerla, DoD para aceptarla) son las de
> [23 · §9.2](../23-plan-construccion-producto-llm.md) y no se renegocian acá. El ID
> interno del equipo sigue el esquema `LLM-Sxx-Hyy`; el título de la HU en Taiga usa
> el formato de abajo.
>
> Cómo se redacta y estima cada apartado (3C, COMO/QUIERO/PARA, escenarios BDD,
> INVEST, Planning Poker): [29 · Guía de cátedra: Historias de Usuario](../29-guia-catedra-historias-de-usuario.md).
>
> **Formato fijo de la ficha:**
> - Encabezado `# GXX — <título>` (sin corchetes, sin el ID `LLM-Sxx-Hyy`; el ID interno
>   se usa como ancla en el documento de historias del sprint, no en el título de Taiga).
> - Un separador `---` en línea propia **entre cada sección** `##` (también entre el
>   encabezado y la primera sección). Los `### Escenario N` del BDD **no** se separan.
> - Los Criterios de Aceptación como lista de tildar `- [ ]`.
> - **Al pegar en Taiga:** los criterios van una línea por ítem y sin `code` inline — el
>   renderer de Taiga en modo lectura descoloca las tildas si el ítem trae `code` o listas
>   anidadas.

---

# GXX — [TÍTULO DE LA HISTORIA DE USUARIO]

---

## Descripción (Como / Quiero / Para)

- **Como:** [ROL]
- **Quiero:** [FUNCIONALIDAD DESEADA]
- **Para:** [PROPÓSITO / VALOR ENTREGADO]

---

## Notas / Observaciones

- **Reglas de negocio:** [DETALLAR]
- **Validaciones:** [DETALLAR]
- **Datos obligatorios:** [LISTAR CAMPOS]
- **Performance (tiempos, volumen, límites):** [DETALLAR]
- **Seguridad (roles, permisos, datos sensibles):** [DETALLAR]
- **Accesibilidad (WCAG / teclado / lectores):** [DETALLAR]
- **Otros:** [DETALLAR]

---

## Criterios de Aceptación (CA)

- [ ] **CA1:** [CONDICIÓN MEDIBLE Y OBJETIVA]
- [ ] **CA2:** [CONDICIÓN MEDIBLE Y OBJETIVA]
- [ ] **CA3:** [CONDICIÓN MEDIBLE Y OBJETIVA]
- [ ] **Extras (opcional):** [CONDICIÓN MEDIBLE Y OBJETIVA]

> Incluir siempre los criterios **negativos** (no autorizado, entrada inválida,
> duplicado, dependencia caída), no solo el camino feliz.

---

## BDD

**Característica:** [NOMBRE / OBJETIVO DE LA FUNCIONALIDAD]

### Escenario 1

- **Dado:** [contexto inicial / precondiciones]
- **Cuando:** [acción del usuario o del sistema]
- **Entonces:** [resultado observable y verificable]

### Escenario 2

- **Dado:** [contexto]
- **Cuando:** [acción]
- **Entonces:** [resultado]

### Escenario 3

- **Dado:** [contexto]
- **Cuando:** [acción]
- **Entonces:** [resultado]

---

## Prototipo

- **Capturas:** [PEGAR AQUÍ]
- **URL Figma:** [https://...]
- **Storybook:** [https://...]
- **Mock API / Swagger:** `[GET /api/...]`, `[POST /api/...]`

---

## Estimación / Prioridad

**Formato rápido**

- **Puntos (Fibonacci):** [1 / 2 / 3 / 5 / 8 / 13]
- **Prioridad (MoSCoW / Numérica):** [Must / Should / Could / Won't] o [1..5]

**Formato tabla (opcional)**

| Puntos (Fibonacci) | Prioridad (MoSCoW / Numérica) |
|---|---|
| [1 / 2 / 3 / 5 / 8 / 13] | [Must / Should / Could / Won't] o [1..5] |

---

## Dependencias / Impactos

- **Servicios involucrados:** [LISTAR]
- **Módulos afectados:** [LISTAR]
- **Otros equipos / aprobaciones:** [LISTAR]
- **Impacto en datos / migraciones:** [DETALLAR]
- **Riesgos y mitigación (opcional):** [DETALLAR]
