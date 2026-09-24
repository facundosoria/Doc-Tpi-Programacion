# Fundamentos del harness (secciones 0, 0.1 y 1 de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.
> No resumir ni parafrasear: esta referencia existe para leerse completa cuando el agente
> necesite justificar por qué el harness importa antes de organizar documentación.

## 0. Cómo debe leer y aplicar esta guía el agente

El agente debe tratar esta guía como una especificación de trabajo, no como una colección de consejos opcionales.

Antes de modificar documentación debe:

1. identificar el contrato de la tarea;
2. cargar el mapa del repositorio y solo después recuperar el contexto pertinente;
3. usar herramientas con contratos claros y límites explícitos;
4. guardar el estado durable, no depender de la conversación;
5. separar razonamiento, ejecución e historial;
6. comprobar el resultado con evidencia observable;
7. someter el resultado a una verificación que pueda rechazarlo;
8. respetar políticas y aprobaciones fuera del razonamiento libre;
9. clasificar cada fallo antes de reintentar;
10. dejar una traza reconstruible y un recibo de cambios;
11. convertir cada fallo recurrente en una mejora del sistema documental.

No debe interpretar "organizar" como "mover archivos sin más". Organizar significa cerrar la distancia entre la intención del proyecto y la evidencia documental que demuestra que esa intención está comprendida, vigente, localizada y verificable.

---

## 0.1. Planteo inicial del artículo

El artículo parte de una observación: muchas personas intentan mejorar los agentes en la capa equivocada.

Cuando un agente falla, suelen reescribir el prompt. Cuando vuelve a fallar, agregan más instrucciones. Después cambian el modelo, agregan más herramientas, aumentan la ventana de contexto y esperan que la siguiente ejecución se comporte de otra manera.

Pero muchos fallos de agentes no son fallos de razonamiento. Son fallos del entorno:

- el agente no supo qué archivos importaban;
- utilizó la herramienta correcta en el lugar equivocado;
- perdió las decisiones tomadas en la sesión anterior;
- declaró éxito sin ejecutar las comprobaciones;
- repitió una acción después de un fallo parcial;
- tenía permiso para hacer algo que debía requerir aprobación.

El modelo no necesariamente era el problema. El sistema alrededor del modelo estaba incompleto. Ese sistema es el harness, y diseñarlo se está convirtiendo en una disciplina de ingeniería propia.

La tesis inicial puede expresarse así: un prompt cambia un intento; un harness cambia todos los intentos. El artículo presenta la ingeniería del harness como la práctica de construir el entorno que convierte la inteligencia del modelo en trabajo fiable.

El artículo también presenta su publicación complementaria como un canal para recibir novedades, flujos de trabajo de agentes y guías paso a paso antes de su publicación en X. Ese elemento es editorial y no constituye una regla del harness; se conserva aquí para no confundir el material promocional con una instrucción operativa.

Referencia editorial incluida en la fuente: `https://substack.com/@lunarresearcher`.

---

## 1. El modelo no es el agente

Un modelo puede razonar, generar, comparar y elegir. Un agente, en cambio, debe interactuar con un entorno real. Para trabajar de manera fiable debe poder:

- comprender la tarea;
- encontrar el contexto relevante;
- seleccionar y utilizar herramientas;
- conservar el estado;
- respetar permisos;
- inspeccionar el resultado;
- recuperarse de fallos;
- demostrar que el trabajo terminó.

El modelo es el motor de razonamiento dentro del sistema. El **harness** es todo lo que vuelve operativo ese razonamiento.

La relación debe entenderse así:

```text
solicitud del usuario
        |
        v
     HARNESS
 contrato | contexto | política
 herramientas | estado | comprobaciones
 trazas | recuperación
        |
        v
      modelo
        |
        v
 entorno real del repositorio
```

Un modelo poderoso dentro de un harness débil sigue produciendo un agente débil. Puede generar respuestas individuales impresionantes, pero se comportará de forma inconsistente durante tareas largas, cuando cambie el entorno o cuando ocurra un fallo parcial.

El objetivo del harness engineering no es eliminar la incertidumbre del modelo. Es contener esa incertidumbre dentro de un sistema capaz de observar, verificar y recuperarse.

### Aplicación a la documentación

El agente no debe recibir solamente el pedido "ordená la documentación" y producir una estructura según su intuición. Debe recibir un entorno documental con:

- contrato de la tarea;
- mapa de carpetas y fuentes de verdad;
- reglas de alcance y propiedad;
- herramientas de lectura y edición limitadas;
- estado de decisiones y progreso;
- comprobaciones de enlaces, índices, formato y consistencia;
- trazas y recibo de cambios.
