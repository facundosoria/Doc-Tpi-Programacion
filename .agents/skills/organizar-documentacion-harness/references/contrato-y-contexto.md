# Contrato de tarea y mapa de contexto (secciones 2 y 3 de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.

## 2. Empezar con un contrato de tarea

La mayoría de las tareas de agentes comienzan como una intención vaga, por ejemplo: "Mejorar el flujo de onboarding". Eso puede ser suficiente para una conversación, pero no para una ejecución autónoma.

Antes de actuar, el harness debe convertir la solicitud en un **contrato de tarea**. Un contrato útil responde estas preguntas:

1. ¿Qué resultado debe existir?
2. ¿Qué está dentro del alcance?
3. ¿Qué no debe cambiar?
4. ¿Qué evidencia demuestra que terminó?
5. ¿Qué acciones requieren aprobación humana?

El artículo muestra este formato:

```yaml
objective: reduce onboarding drop-off

scope:
  - signup flow
  - onboarding analytics

constraints:
  - do not change authentication
  - preserve existing mobile behavior

acceptance:
  - tests pass
  - analytics event is emitted
  - screenshots cover desktop and mobile

approval_required:
  - production deployment
  - database migration
```

El contrato transforma la pregunta del agente. Sin contrato, el agente optimiza actividad plausible: "¿qué debería hacer después?". Con contrato, optimiza avance hacia una finalización verificable: "¿qué acción mueve el entorno hacia el resultado contratado?".

### Contrato documental obligatorio

Para una tarea de documentación, el agente debe completar como mínimo:

```yaml
objective: "Resultado documental observable que debe quedar disponible"

scope:
  - "Documentos, carpetas y fuentes que se pueden revisar o modificar"

constraints:
  - "Documentos protegidos o fuera de alcance"
  - "No cambiar decisiones técnicas existentes sin ADR o autorización"
  - "Conservar enlaces y referencias válidos"

acceptance:
  - "La estructura resultante coincide con la taxonomía aprobada"
  - "No quedan enlaces internos rotos"
  - "Cada documento tiene propósito, propietario y fuente de verdad"
  - "Los índices y referencias se actualizaron"
  - "Las comprobaciones de formato y consistencia pasan"
  - "La traza y el recibo de cambios fueron generados"

approval_required:
  - "Eliminar o sobrescribir información"
  - "Cambiar una fuente de verdad o una decisión vigente"
  - "Mover documentación que otro equipo posee"
  - "Publicar documentación externamente"
```

El agente nunca debe declarar que una tarea está terminada solo porque creó archivos o porque la estructura "parece razonable".

### Variante: contrato de solo diagnóstico

> Nota: esta variante no proviene del artículo fuente — es una adición operativa de esta
> skill, detectada al probarla contra un caso real donde el pedido era "decime qué pasa
> acá" y no "arreglalo". No aplica la regla de fidelidad de la sección 23; se documenta
> aparte para no mezclarla con el contenido conservado verbatim.

No todo pedido de organización documental pide ejecutar cambios. Cuando el usuario pide
un diagnóstico, una auditoría, o "decime qué encontrás antes de que decida qué hacer", el
contrato de tarea completo (arriba) sobra en su forma "acceptance"/"approval_required"
orientada a ejecución. Usá esta forma reducida en su lugar:

```yaml
objective: "Diagnóstico observable: qué estado real tiene la documentación en el alcance dado"

scope:
  - "Documentos y carpetas a inspeccionar (sin modificarlos)"

constraints:
  - "No mover, editar ni borrar ningún archivo real durante el diagnóstico"

acceptance:
  - "Cada afirmación del diagnóstico está respaldada por una comparación real (diff, grep, git log), no por suposición"
  - "Se identificó explícitamente qué fuente es la vigente y cuál está en duda, con evidencia"
  - "Se listó qué preguntas requieren una decisión del usuario antes de poder ejecutar algo"

next_step_if_approved:
  - "Recién si el usuario aprueba actuar, abrir el contrato documental completo de ejecución (arriba) para la Fase D en adelante"
```

Un diagnóstico que termina proponiendo cambios sin haberlos ejecutado no necesita
`approval_required`: ese campo pertenece al contrato de ejecución que se abre después, una
vez que el usuario decide qué camino tomar.

---

## 3. Dar al agente un mapa, no un manual completo

Volcar en el contexto todo el repositorio, el conjunto entero de documentación y todo el historial de conversación no es buena ingeniería de contexto: es **inundación de contexto**.

El harness debe proporcionar primero un mapa pequeño y permitir que el agente recupere los detalles cuando sean relevantes. El artículo usa este ejemplo:

```text
MAPA DEL PROYECTO

reglas de producto -> docs/product/
arquitectura       -> docs/architecture.md
frontend           -> apps/web/
backend            -> services/api/
tests              -> tests/
comandos           -> docs/commands.md
reglas de release  -> docs/release.md
```

La divulgación progresiva debe seguir esta secuencia:

```text
tarea
  -> mapa del proyecto
      -> subsistema relevante
          -> archivos exactos
              -> instrucciones locales
```

El contexto debe crecer porque la tarea lo exige, no simplemente porque la información existe.

Un buen compilador de contexto decide:

- qué se necesita siempre;
- qué puede recuperarse después;
- qué quedó obsoleto;
- qué puede resumirse;
- qué debe permanecer literalmente sin alteración.

El objetivo no es maximizar el contexto. Es maximizar la señal por token.

### Mapa documental que debe cargar primero

El agente debe comenzar construyendo o consultando un mapa con este tipo de entradas:

```text
fuentes de verdad       -> docs/00-fuentes-de-verdad.md
visión y alcance        -> docs/vision/
arquitectura            -> docs/architecture/
contratos HTTP          -> docs/contracts/http/
contratos de eventos    -> docs/contracts/events/
decisiones ADR          -> docs/adr/
operación y comandos    -> docs/operations/
pruebas y verificaciones -> docs/quality/
historial de cambios    -> docs/changes/
documentación por equipo -> carpetas propietarias correspondientes
```

Esta estructura es orientativa: el agente debe respetar la estructura existente y no crear una taxonomía paralela si ya hay una fuente de verdad. Antes de mover archivos debe descubrir:

- el índice maestro;
- las reglas de precedencia;
- los documentos propietarios de cada tema;
- los directorios protegidos;
- las instrucciones locales;
- los comandos de verificación;
- los enlaces entrantes y salientes de cada documento.
