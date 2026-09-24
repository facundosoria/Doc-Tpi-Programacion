# Puerta de herramientas, arquitectura y memoria durable (secciones 4, 5 y 6 de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.

## 4. Construir una puerta de herramientas, no una pila de herramientas

Dar al agente veinte herramientas no lo vuelve capaz: le da veinte formas de equivocarse.

Cada herramienta debe tener un contrato claro. El artículo usa este ejemplo:

```text
TOOL: edit_file

inputs:
  path
  patch

preconditions:
  path exists
  path is inside allowed workspace

success evidence:
  patch applied
  resulting diff returned

failure behavior:
  no partial overwrite
  structured error returned

risk class:
  reversible
```

El harness debe controlar cómo se exponen y utilizan las herramientas. Puede:

- ocultar herramientas irrelevantes;
- validar argumentos;
- restringir rutas y dominios;
- agregar timeouts;
- hacer idempotentes los reintentos;
- normalizar resultados;
- exigir confirmación para acciones riesgosas;
- devolver evidencia, no solamente "éxito".

La separación esencial es:

```text
el modelo decide la intención
la puerta valida la acción
la herramienta cambia el entorno
el sensor observa el resultado
```

El modelo puede proponer una edición. La puerta de herramientas decide si esa edición es suficientemente válida para ejecutarse.

### Contratos mínimos para herramientas documentales

El agente debe operar con herramientas equivalentes a estas, aunque sus nombres concretos sean distintos:

| Herramienta | Precondiciones | Evidencia de éxito | Riesgo |
|---|---|---|---|
| `list_files` | directorio dentro del workspace permitido | listado devuelto con ruta y tipo | bajo |
| `read_file` | archivo permitido y existente | contenido y metadatos de lectura | bajo |
| `search_text` | patrón válido y alcance acotado | coincidencias con archivo y línea | bajo |
| `edit_file` | archivo permitido, instrucciones cargadas, parche aplicable | diff resultante y archivo verificable | reversible |
| `move_file` | destino dentro de alcance, enlaces analizados, aprobación si corresponde | origen, destino y enlaces actualizados | reversible con riesgo |
| `delete_file` | autorización explícita y registro de dependencias | confirmación de eliminación y sustitución | irreversible |
| `check_links` | conjunto de documentos definido | reporte de enlaces válidos y rotos | bajo |
| `run_quality_checks` | comandos conocidos y entorno disponible | salida completa, código de salida y artefactos | bajo/reversible |
| `publish` | revisión y aprobación explícitas | URL o identificador de publicación | externo |

Nunca se debe exponer una operación destructiva como si fuera una edición común.

---

## 5. Separar cerebro, manos e historial

Muchos agentes frágiles mezclan todo en una transcripción que crece sin límite: razonamiento, llamadas a herramientas, archivos, decisiones, errores y observaciones antiguas compiten por la misma ventana de contexto.

Un sistema más fuerte separa tres responsabilidades:

```text
CEREBRO
  planifica, razona y elige

MANOS
  ejecutan herramientas dentro de un entorno controlado

HISTORIAL
  almacena hechos durables, decisiones y estado de ejecución
```

El modelo no necesita todos los eventos sin procesar en el contexto activo: necesita el estado actual correcto. El sandbox no necesita entender el objetivo completo: necesita ejecutar de manera segura una acción acotada. El registro de sesión no necesita razonar: necesita conservar lo ocurrido cuando el contexto actual desaparezca.

Esta separación facilita reanudar, inspeccionar y reparar agentes de larga duración. También permite reemplazar una parte sin reconstruir todo el sistema.

### Aplicación documental

- **Cerebro:** interpreta el contrato, decide qué información falta y propone la organización.
- **Manos:** leen, buscan, editan, mueven y ejecutan comprobaciones bajo límites.
- **Historial:** conserva contratos, decisiones, fuentes consultadas, diffs, resultados, riesgos y pendientes.

El agente no debe usar el transcript como base de conocimiento. Debe compilar los datos relevantes en estado durable.

---

## 6. La memoria debe convertirse en estado durable

El historial de conversación no es memoria confiable: es un flujo de eventos. La memoria útil debe convertirse en estado explícito.

Como mínimo, deben conservarse cuatro categorías:

```text
FACTS       información estable descubierta sobre el entorno
DECISIONS   elecciones realizadas y motivo de cada una
PROGRESS    trabajo completado, activo, bloqueado y pendiente
LESSONS     fallos que deben modificar el comportamiento futuro
```

El formato de ejemplo del artículo es:

```yaml
facts:
  - checkout validation lives in services/orders

decisions:
  - reuse the existing validation pipeline
  - reason: avoids a second source of truth

progress:
  completed:
    - added server-side rule
  remaining:
    - update integration test

lessons:
  - local test command requires TEST_DB_URL
```

Esto es mucho más útil que reproducir cincuenta páginas de transcripción esperando que el modelo encuentre la línea importante.

Debe conservarse el historial crudo para auditoría, pero debe compilarse un estado durable para la ejecución.

### Estado documental recomendado

```yaml
facts:
  - "La fuente de verdad de la arquitectura está en ..."
  - "El índice maestro enlaza ..."
  - "El equipo X es propietario de ..."

decisions:
  - decision: "Separar contratos HTTP de explicación arquitectónica"
    reason: "Evita mezclar referencia normativa con explicación"
    source: "ADR-..."

progress:
  completed:
    - "Inventario de documentos"
    - "Detección de enlaces entrantes"
  active:
    - "Normalización de nombres"
  blocked: []
  remaining:
    - "Actualizar índice"

lessons:
  - "La búsqueda por título no detecta referencias por nombre antiguo"

freshness:
  - document: "..."
    last_checked: "YYYY-MM-DD"
    source_version: "..."
```
