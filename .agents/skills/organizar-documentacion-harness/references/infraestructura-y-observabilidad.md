# Instrucciones convertidas en infraestructura, trazas y recibo de cambios (secciones 11, 12 y 13 de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.

## 11. Las instrucciones deben convertirse en infraestructura

Las instrucciones son útiles cuando explican la realidad local. Pero por sí solas son un mecanismo débil de aplicación.

Si una regla importa repetidamente, debe bajarse por la pila:

```text
"usar el formateador"
  -> ejecutar el formateador automáticamente

"no importar entre capas"
  -> agregar una prueba de arquitectura

"incluir rollback de migración"
  -> exigir el archivo de rollback en CI

"no modificar archivos generados"
  -> bloquear escrituras en rutas generadas

"citar toda afirmación externa"
  -> validar cobertura de citas
```

La escalera de instrucciones es:

```text
explicación
  -> checklist
      -> plantilla
          -> comprobación automática
              -> política impuesta
```

El conocimiento importante debe trasladarse tan abajo en esa escalera como sea práctico. El prompt debe explicar el juicio; el harness debe imponer los invariantes.

### Ejemplos de infraestructura documental

- "Cada documento debe tener fuente de verdad" -> esquema o validador de metadatos.
- "No duplicar contratos" -> detector de documentos normativos duplicados.
- "Actualizar el índice" -> comprobación que compare índice y árbol real.
- "No romper enlaces al mover archivos" -> análisis automático de referencias antes y después.
- "Citar toda afirmación externa" -> verificador de citas o de referencias.
- "No editar documentos protegidos" -> permisos de ruta.
- "Mantener un glosario" -> comprobación de términos prohibidos o inconsistentes.

---

## 12. Observar la ejecución, no solo la respuesta final

Un artefacto final limpio puede ocultar un proceso terrible. El agente pudo:

- acceder a datos equivocados;
- ignorar un comando fallido;
- reintentar dos veces una acción externa;
- consumir diez veces el presupuesto esperado;
- llegar a la respuesta correcta por el motivo incorrecto.

Se necesitan trazas que permitan reconstruir la ejecución. Ejemplo del artículo:

```text
09:14   contrato creado
09:15   fuente de contexto cargada: architecture.md
09:17   archivo editado: checkout.ts
09:18   prueba enfocada falló: duplicate coupon
09:21   implementación reparada
09:22   prueba enfocada aprobada
09:24   prueba de integración aprobada
09:25   despliegue externo bloqueado: requiere aprobación
```

Una traza útil registra:

- transiciones de estado;
- fuentes de contexto;
- entradas y salidas de herramientas;
- cambios del entorno;
- resultados de verificación;
- motivos de reintento;
- decisiones de aprobación;
- coste y latencia.

El objetivo no es vigilancia. Es reparación local. Si un proceso falla en el paso 18, se debe poder reiniciar desde un checkpoint confiable en vez de reproducir toda la tarea.

### Eventos mínimos de una ejecución documental

```text
contract.created
context.map_loaded
context.source_loaded
inventory.completed
ownership.checked
decision.recorded
file.created
file.edited
file.moved
link_check.completed
quality_check.completed
verification.rejected
verification.accepted
approval.requested
approval.granted / approval.denied
checkpoint.created
run.failed
run.stopped
receipt.generated
```

Cada evento debe conservar, cuando corresponda, timestamp, actor, ruta, fuente, entrada, salida, código de resultado, riesgo, costo y relación con el contrato.

---

## 13. Cada ejecución necesita un recibo de cambios

Las transcripciones largas son difíciles de revisar. Al terminar, el harness debe compilar un **recibo pequeño de cambios**.

El artículo muestra este formato:

```text
OBJETIVO
Corregir la aplicación duplicada de cupones durante checkout.

CAMBIADO
- lógica de validación de checkout
- prueba de regresión enfocada

VERIFICADO
- lint aprobado
- pruebas unitarias aprobadas
- prueba de integración de checkout aprobada

NO VERIFICADO
- proveedor de pagos de producción

DECISIONES
- se conservó el orden de prioridad existente de cupones

RIESGOS
- el cliente móvil heredado no estaba disponible localmente

APROBACIÓN NECESARIA
- despliegue a staging
```

El recibo no es un resumen de lo que dijo el modelo. Es un resumen de lo que el sistema puede probar.

Esto da a las personas una superficie compacta de revisión y entrega a la próxima sesión un punto de partida confiable. El mejor handoff no es "acá está la conversación"; es "acá están el estado, la evidencia y el riesgo sin resolver".

### Plantilla de recibo documental

```markdown
# Recibo de cambios

## Objetivo
- [Resultado contratado]

## Cambiado
- [Archivo o estructura]
- [Contenido o relación afectada]

## Verificado
- [Comprobación ejecutada + resultado]
- [Índice actualizado + resultado]
- [Enlaces verificados + resultado]

## No verificado
- [Límites de la ejecución]

## Decisiones
- [Decisión]
- Motivo: [por qué]
- Fuente: [ADR, contrato o documento]

## Riesgos
- [Riesgo residual]

## Aprobación necesaria
- [Acción que no se ejecutó]

## Estado durable entregado
- [Ruta del estado]
- [Ruta de la traza]
```
