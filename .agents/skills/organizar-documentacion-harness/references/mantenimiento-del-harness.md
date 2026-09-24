# Mejora, degradación, métricas y cuándo no hace falta harness pesado (secciones 14, 15, 18, 19, 20 y 20.1 de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.

## 14. Cada fallo debe mejorar el harness

Los equipos débiles corrigen el resultado fallido. Los equipos fuertes también corrigen el sistema que permitió el fallo.

Después de un fallo, hay que preguntar:

```text
¿El contrato de tarea era ambiguo?
¿El contexto importante era invisible?
¿Se expuso la herramienta equivocada?
¿Faltaba una precondición?
¿El resultado no era verificable?
¿La política quedó dentro del prompt?
¿La recuperación era demasiado amplia?
¿La traza era insuficiente?
```

Luego la lección debe convertirse en una mejora reutilizable:

```text
fallo
  -> diagnóstico
      -> nuevo sensor, regla, mapa, prueba o contrato de herramienta
          -> las ejecuciones futuras mejoran automáticamente
```

Este es el **volante del harness**. El sistema se vuelve más fiable porque los fallos dejan infraestructura. Una respuesta corregida ayuda a una ejecución; un harness corregido ayuda a todas las futuras.

### Registro de mejora obligatoria

Cada fallo recurrente debe terminar en uno de estos cambios, o en una justificación explícita de por qué no corresponde:

- contrato más preciso;
- mapa o índice más visible;
- fuente de contexto agregada o marcada como obsoleta;
- precondición de herramienta;
- validador automático;
- prueba de aceptación;
- política de permisos;
- nueva clase de recuperación;
- límite de reintentos;
- evento de traza;
- plantilla de documento;
- decisión o ADR.

---

## 15. Los harnesses también se degradan

Más harness no siempre es mejor. Los modelos mejoran, las herramientas mejoran y las tareas cambian. Las salvaguardas antiguas pueden convertirse en fricción innecesaria.

Una solución temporal creada por una limitación del modelo de ayer puede impedir que el modelo de hoy use una estrategia mejor. El deterioro sigue este patrón:

```text
limitación del modelo antiguo
  -> solución del harness
      -> el modelo mejora
          -> la solución queda
              -> el sistema se vuelve más lento o menos capaz
```

Los componentes del harness deben tratarse como código de producción. Hay que medir si todavía aportan valor.

Para cada router, evaluador, capa de memoria y regla de reintento, preguntar:

- ¿Qué fallo evita?
- ¿Con qué frecuencia sigue ocurriendo ese fallo?
- ¿Qué latencia y complejidad agrega?
- ¿Ahora se puede conseguir el mismo resultado de forma más simple?
- ¿Qué sucede si lo eliminamos?

El mejor harness no es el más grande. Es el sistema más pequeño que cierra de manera fiable la distancia entre intención y evidencia. Hay que construir con la posibilidad de eliminar.

### Aplicación documental

Revisar periódicamente:

- carpetas creadas para un problema que ya no existe;
- validadores redundantes o demasiado lentos;
- reglas duplicadas en varios documentos;
- instrucciones que contradicen el comportamiento actual;
- resúmenes que ya no son necesarios porque existe un índice consultable;
- pasos manuales que podrían automatizarse;
- automatizaciones que impiden cambios válidos.

No conservar una estructura solo porque alguna vez fue útil.

---

## 18. Medir el sistema en el nivel correcto

El conteo de tokens no es la métrica final. Tampoco lo es la cantidad de tareas intentadas.

La unidad útil es el **trabajo aceptado**. Una métrica práctica es:

```text
salidas aceptadas
-------------------------------
minutos de revisión humana + coste de ejecución
```

También deben medirse:

- tasa de aceptación en el primer intento;
- tasa de recuperación después de un fallo de herramienta;
- tasa de fallos repetidos;
- intervenciones humanas por tarea;
- afirmaciones de finalización sin respaldo;
- tiempo desde la solicitud hasta el resultado verificado;
- coste del harness por componente.

Estas métricas evitan una ilusión común: un agente puede parecer muy productivo mientras crea trabajo caro de revisión.

El objetivo no es más actividad del agente. Es más resultados confiables por unidad de atención humana.

### Métricas documentales adicionales

- porcentaje de documentos con propietario y fuente de verdad;
- enlaces rotos por ejecución;
- documentos duplicados o en conflicto;
- tiempo desde cambio de código/decisión hasta actualización documental;
- porcentaje de tareas con recibo completo;
- tasa de rechazo del verificador adversarial;
- cantidad de fallos que generan una mejora permanente;
- tiempo y coste de reconstruir una ejecución desde checkpoint;
- porcentaje de contenido no verificado declarado correctamente.

---

## 19. Cuándo no hace falta un harness pesado

No toda llamada de modelo necesita un sistema operativo.

Se puede usar un prompt simple cuando:

- la tarea es corta;
- el resultado es fácil de inspeccionar;
- el fallo es barato;
- no hay efectos externos;
- la persona usuaria permanece dentro del circuito.

Hay que agregar un harness cuando:

- el trabajo abarca varias herramientas o sesiones;
- el entorno puede cambiar;
- las acciones tienen consecuencias reales;
- es difícil juzgar manualmente si terminó;
- el mismo fallo aparece repetidamente;
- la revisión humana se convierte en cuello de botella.

El propósito de un harness no es hacer que una demo parezca sofisticada. Es hacer que el trabajo real sea confiable.

---

## 20. Cambio conceptual final

La primera generación de productos de IA se construyó alrededor de prompts. La siguiente se está construyendo alrededor de entornos.

La pregunta ya no es solamente:

> ¿Cómo hacemos que el modelo responda mejor?

También es:

> ¿Cómo construimos un sistema donde las buenas acciones sean fáciles, las peligrosas estén controladas, los fallos sean visibles y la finalización pueda probarse?

Ese es el cambio de prompt engineering a harness engineering.

- El modelo aporta inteligencia.
- El harness aporta estructura.
- Juntos producen ejecución confiable.

Si un agente se desarma continuamente, la respuesta no es seguir agregando adjetivos al prompt. Hay que construir el entorno que necesita para tener éxito.

---

## 20.1. Cierre editorial de la fuente

El artículo termina invitando a guardar la guía, seguir a `@LunarResearcher` en X y suscribirse a su Substack. Estas invitaciones no afectan la organización documental ni deben convertirse en acciones automáticas del agente.
