# Harness mínimo viable y especificación reutilizable (secciones 16 y 17 de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.

## 16. Harness mínimo viable

No hace falta una plataforma de orquestación para comenzar. El harness debe construirse por capas.

### Nivel 1: tarea acotada

- objetivo;
- alcance;
- restricciones;
- comprobaciones de aceptación.

### Nivel 2: entorno legible

- mapa del proyecto;
- comandos;
- instrucciones locales;
- dependencias conocidas.

### Nivel 3: acciones controladas

- herramientas tipadas;
- validación de argumentos;
- límites de rutas y permisos;
- resultados estructurados.

### Nivel 4: ejecución durable

- estado explícito de ejecución;
- checkpoints;
- decisiones;
- lecciones.

### Nivel 5: evidencia

- comprobaciones deterministas;
- verificación adversarial;
- recibo de cambios.

### Nivel 6: recuperación y aprendizaje

- clasificación de fallos;
- reintentos acotados;
- escalamiento;
- actualizaciones del harness a partir de fallos recurrentes.

Debe construirse la capa más pequeña que elimine el fallo real que se tiene. No hay que comenzar con una arquitectura multiagente porque un prompt aislado ocasionalmente necesita aclaración. La complejidad debe ganarse mediante fallos observados.

### Orden mínimo para este repositorio

1. Contrato e inventario.
2. Mapa de documentación y fuentes de verdad.
3. Convenciones de nombres, metadatos y enlaces.
4. Edición con diff y límites.
5. Comprobaciones automáticas.
6. Estado durable y recibos.
7. Verificación adversarial.
8. Recuperación y mejora del sistema.

---

## 17. Especificación reutilizable del harness

Antes de dar autonomía significativa a un agente, debe definirse lo siguiente:

```text
AGENT HARNESS SPEC

1. CONTRACT
   objective:
   scope:
   constraints:
   acceptance evidence:

2. CONTEXT
   always-loaded map:
   retrieval sources:
   local instructions:
   freshness rules:

3. TOOLS
   allowed tools:
   preconditions:
   side effects:
   success evidence:
   timeout and retry policy:

4. STATE
   facts:
   decisions:
   progress:
   lessons:
   checkpoint format:

5. POLICY
   automatic actions:
   approval-required actions:
   prohibited actions:
   budget limits:

6. VERIFICATION
   deterministic checks:
   adversarial checks:
   acceptance rule:

7. RECOVERY
   failure classes:
   retry limits:
   escalation conditions:
   safe rollback:

8. OBSERVABILITY
   trace events:
   metrics:
   final change receipt:
```

Si estos campos no están definidos, el agente no es autónomo: está improvisando.

### Versión lista para una tarea documental

```yaml
contract:
  objective: "Organizar la documentación sin pérdida de información"
  scope:
    - "Archivos explícitamente incluidos por la tarea"
    - "Índices y referencias afectadas"
  constraints:
    - "No eliminar contenido sin aprobación"
    - "No cambiar decisiones normativas sin autoridad"
  acceptance_evidence:
    - "Inventario comparativo antes/después"
    - "Enlaces válidos"
    - "Índices consistentes"
    - "Verificación adversarial aprobada"
    - "Recibo de cambios"

context:
  always_loaded_map: "docs/00-fuentes-de-verdad.md"
  retrieval_sources:
    - "Índices"
    - "ADRs"
    - "Contratos"
    - "Instrucciones locales"
  local_instructions: "AGENTS.md y reglas del directorio"
  freshness_rules: "Preferir fuentes vigentes; marcar obsoletas"

tools:
  allowed_tools:
    - "list"
    - "read"
    - "search"
    - "edit"
    - "move"
    - "check"
  preconditions: "Rutas permitidas, alcance cargado, propiedad comprobada"
  side_effects: "Cambios de archivos y enlaces"
  success_evidence: "Diff, resultados y código de salida"
  timeout_and_retry_policy: "Por clase de fallo, con límites"

state:
  facts: []
  decisions: []
  progress: {}
  lessons: []
  checkpoint_format: "YAML o Markdown estructurado"

policy:
  automatic_actions: "Lectura, búsqueda y cambios reversibles dentro del alcance"
  approval_required_actions: "Borrado, publicación, cambio de fuente de verdad"
  prohibited_actions: "Secretos, rutas fuera del workspace"
  budget_limits: "Intentos, tiempo y alcance"

verification:
  deterministic_checks:
    - "Markdown"
    - "Enlaces"
    - "Índices"
    - "Metadatos"
  adversarial_checks:
    - "Búsqueda de omisiones"
    - "Búsqueda de duplicados normativos"
    - "Contradicciones"
  acceptance_rule: "Todas las comprobaciones obligatorias pasan"

recovery:
  failure_classes: []
  retry_limits: "Máximo definido por tarea"
  escalation_conditions: "Contradicción, propiedad o fallo repetido"
  safe_rollback: "Restaurar desde diff/checkpoint"

observability:
  trace_events: []
  metrics: []
  final_change_receipt: "docs/changes/..."
```
