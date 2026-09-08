# Método para redactar y estimar Historias de Usuario

Guía de método condensada. Fuentes: Cohn (2004, 2005), Jeffries (2001), Wake (2003),
Schwaber & Sutherland (2020), Beck (1999), North (2006). Es el método que la cátedra de
Metodología de Sistemas II evalúa.

---

## 1. Qué es una historia de usuario

Descripción **breve** de una funcionalidad, en el lenguaje del usuario y desde su punto
de vista, que expresa **qué necesita y por qué**. No es una especificación técnica ni un
caso de uso: es deliberadamente **corta e incompleta**. Es el **recordatorio de una
conversación pendiente** entre el equipo y el cliente. Si no entra en una ficha, abarca
demasiado: dividir.

### Las 3 C (Jeffries, 2001)

| C | Qué es | Dónde vive |
|---|---|---|
| **Ficha** (Card) | El enunciado breve en formato **COMO / QUIERO / PARA**. | Título + descripción |
| **Conversación** (Conversation) | El diálogo con el cliente/experto donde se aclara el alcance real. | Notas |
| **Confirmación** (Confirmation) | Los criterios de aceptación: cómo se sabe que está terminada. | CA / BDD |

---

## 2. Paso a paso

### 2.1 Identificar el/los usuario/s y extraer las historias (método de dos pasos)

Para leer una entrevista, una receta de sprint o un texto de alcance:

1. **Identificar los roles.** Un rol es quien interactúa con el sistema persiguiendo un
   objetivo propio. Pregunta: *¿quién realiza acciones distintas?* Si el mismo actor que
   carga un dato es distinto del que lo aprueba, son roles distintos.
2. **Extraer las historias.** Subrayá los **verbos de acción** de cada rol; cada acción
   que un rol quiere realizar es una historia candidata. Ejemplo (App Taxi): pasajero →
   se loguea, busca taxis, pide un taxi, notifica (4); taxista → se loguea, marca
   ocupado, marca fuera de servicio (3); central → da de alta la central (1).

No todas las candidatas terminan siendo una historia (algunas se agrupan), pero el
barrido asegura no dejar funcionalidad afuera.

El usuario del **COMO** es la persona o grupo que **usará** la funcionalidad — no el
sistema.

### 2.2 Boceto de referencia (herramienta de descubrimiento, no diseño gráfico)

Dibujar la pantalla obliga a preguntas que la conversación deja pasar (qué campos son
obligatorios, qué pasa si una lista viene vacía, cuántos elementos se cargan). Casi
siempre esas preguntas **son escenarios de aceptación esperando ser escritos**.

| Fidelidad | Qué es | Cuándo |
|---|---|---|
| **Baja** (boceto/wireframe) | Trazo a mano, bloques/campos/botones, sin color. | **Recomendado para trabajar HU.** |
| **Media** (mockup) | Estático, proporciones y textos reales, sin comportamiento. | Acordar la organización de la pantalla. |
| **Alta** (prototipo navegable) | Pantallas enlazadas que simulan el flujo. | Validar con usuarios finales o funciones críticas. |

**Del boceto a los criterios:** recorrer el dibujo elemento por elemento y preguntarse
*qué pasa si el usuario lo usa bien* y *qué pasa si lo usa mal*. Cada respuesta distinta
del sistema es un escenario. Si un elemento del boceto no aparece en ningún escenario, o
sobra en el dibujo o falta un criterio. El boceto **no reemplaza** a los criterios y
**no decide** la implementación.

### 2.3 Título

Resume la funcionalidad o meta principal. Corto pero claro; identifica la historia en el
tablero sin leer el detalle. Ej.: «Crear equipo para torneos en línea».

### 2.4 Descripción (COMO / QUIERO / PARA)

- **COMO** [usuario] — quién interactúa con la funcionalidad.
- **QUIERO** [acción] — lo que el usuario quiere hacer (no una solución técnica).
- **PARA** [propósito] — el beneficio; conecta la acción con el valor (no repetir la
  acción con otras palabras).

> *«Como jugador de eSports quiero crear un equipo, para participar en torneos en línea.»*

### 2.5 Criterios de aceptación (BDD)

**¿Cuántos escenarios? Regla del camino feliz + lo que puede fallar:**

1. **Camino feliz:** todo sale bien.
2. **Qué puede fallar:** por cada historia, preguntarse qué pasa si —
   - el estado no es el correcto (pedir un taxi cuando no hay taxis),
   - falta un dato o un recurso (no hay conexión),
   - la acción se repite o es inválida (ocupar un taxi ya ocupado).

Cada respuesta distinta del sistema es un escenario. **Mínimo: 1 feliz + 2 negativos.**

**Estructura de un escenario (5 partes):**

| Parte | Palabra clave | Qué es |
|---|---|---|
| Título | *(TÍTULO)* | Resumen de la funcionalidad específica que se valida. |
| Precondición | **DADO** | Estado del sistema antes de la acción. Si no se cumple, el escenario no corre. |
| Acción / disparador | **CUANDO** | La acción que lleva al sistema a actuar. |
| Resultado esperado | **ENTONCES** | La respuesta del sistema, precisa y verificable. |
| Condición adicional *(opcional)* | **Y** | Encadena un paso extra. Muchos escenarios no la usan. |

**Dos reglas:**

- **Regla 1 — el ENTONCES es observable.** Verificable desde afuera: pantalla, mensaje o
  datos guardados.
  - Mal: «ENTONCES el sistema verifica que la fecha indica mayoría de edad» (interno).
  - Bien: «ENTONCES el sistema agrega al jugador a la lista y muestra sus datos».
- **Regla 2 — el Y hereda el paso que lo precede.** Nunca inicia un escenario. Después
  de DADO suma otra precondición; después de CUANDO, otra acción; después de ENTONCES,
  otro resultado. Se escribe en renglón aparte.

```gherkin
Escenario: Registrar nombre del equipo
  DADO que estoy en el formulario de creación de equipos,
  CUANDO ingreso el nombre del equipo,
  Y el nombre de equipo no está en uso,
  ENTONCES el sistema debe permitir ingresar los nombres completos de los jugadores
  y su fecha de nacimiento.
```

### 2.6 Notas técnicas (registro de la Conversación)

Reglas de negocio, restricciones, datos que intervienen con su obligatoriedad,
dependencias con otras historias, supuestos acordados. **Qué NO va:** diseño de
pantallas paso a paso, nombres de tablas o clases, la solución técnica elegida. La nota
responde a *«qué debe cumplirse»*, nunca a *«cómo se programa»*. Si una nota le quita al
equipo la libertad de decidir la implementación, es una decisión técnica disfrazada de
requisito.

### 2.7 Estimación

Se hace **siempre al final y en equipo**, nunca individualmente, y **no antes de tener
los criterios de aceptación**. Procedimiento en §5.

---

## 3. INVEST — ¿la historia está bien escrita? (Wake, 2003)

| Letra | Criterio | Señal de que falla |
|---|---|---|
| **I** | **Independiente** — se desarrolla y entrega sin depender del orden de las demás. | Dos historias comparten escenarios o una no tiene sentido sin la otra. |
| **N** | **Negociable** — el alcance se conversa hasta que se implementa. | La tarjeta ya trae la solución técnica decidida. |
| **V** | **Valiosa** — entrega valor perceptible para el usuario o el negocio. | «Crear la tabla de jugadores en la BD»: ningún usuario percibe ese resultado → es tarea técnica. |
| **E** | **Estimable** — el equipo puede dimensionar el esfuerzo. | Nadie logra estimarla → falta conversación o es demasiado grande. |
| **S** | **Small** — entra cómodamente en un sprint. | No se puede terminar en una iteración → es una épica, dividir. |
| **T** | **Testeable** — criterios que se responden sí/no sin discusión. | «Que sea rápido» no; «que responda en < 2 s» sí. |

---

## 4. Épica, historia y tarea

| Nivel | Qué es | ¿COMO/QUIERO/PARA? |
|---|---|---|
| **Épica** | Funcionalidad grande que no entra en un sprint; se divide en historias. | Sí (es una HU demasiado grande) — **pero el template oficial de épica no lo usa; ver skill `generar-epicas`.** |
| **Historia de usuario** | Unidad de valor entregable; se estima y se compromete en un sprint. | Sí |
| **Tarea** | Paso técnico interno para completar una historia. La define el equipo. | **No** |

### ¿Separo o dejo junta?

- **Separá si…** hay dos verbos de acción distintos; intervienen dos roles; una parte se
  entrega y usa sin la otra; o los escenarios superan 6–7.
- **Dejala junta si…** por separado ninguna parte entrega valor; los escenarios se
  repetirían casi iguales; o una parte no se puede probar sin la otra.

> **Regla práctica:** una historia = un rol, una acción y un resultado observable. Si el
> título necesita «y», «o», «además» → probablemente son dos.

### Habilitadores técnicos

Cuando el COMO es «el equipo» o «la plataforma» y ningún usuario percibe el resultado,
la historia **falla la V**: es una **tarea de sprint / habilitador**. Se puede redactar
en formato largo para trazar los escenarios de aceptación, pero en el backlog se carga
como **tarea** bajo la épica (o bajo una HU habilitadora), sin puntos de valor.

---

## 5. Estimación con Planning Poker

- **Punto de historia:** esfuerzo **relativo** = complejidad + volumen + incertidumbre.
  No es una hora ni un día. La escala **pertenece al equipo**: no se compara entre
  equipos ni mide productividad.
- **Mazo (Fibonacci):** 0 · 1 · 2 · 3 · 5 · 8 · 13 · 20 · 40 · 100. En esta materia se
  usa el tramo **1 · 2 · 3 · 5 · 8 · 13**. Carta «?» = falta info. Carta «∞» = es una
  épica, dividir.
- **Historia canónica:** historia de referencia, ya conocida y estimada, contra la que
  se comparan todas las demás. El valor es una **convención interna** (p. ej. canónica =
  1 o = 2). Lo único que no puede cambiar es la referencia **dentro de un mismo
  backlog**.
- **Velocidad:** suma de puntos de las historias **efectivamente terminadas** en un
  sprint. Tras 2–3 sprints se estabiliza y permite planificar cuánto comprometer.

**Procedimiento (resumen):** elegir la canónica → el equipo revisa la nueva historia →
el responsable la explica → comparar con la canónica («¿más o menos compleja?») →
discutir complejidad y riesgos → ronda de cartas en privado → revelar a la vez →
los extremos justifican → nueva ronda hasta consenso → valor final por comparación.

---

## 6. Priorización y armado del backlog

Estimar responde **«cuánto cuesta»** (lo hace el equipo de desarrollo); priorizar
responde **«cuánto vale»** (lo fija el Product Owner). Criterios de prioridad:

- **Valor** para negocio/usuario: cuánto se pierde si no está.
- **Riesgo e incertidumbre:** adelantar lo que tiene dudas técnicas.
- **Dependencias:** si es condición para varias otras, sube de prioridad aunque su valor
  aislado sea bajo.
- **Costo de la demora:** cuánto cuesta postergarla un sprint más.

El **backlog de producto** es una lista **ordenada**: lo primero es lo próximo a
construir. Ante empate, primero la más pequeña. Se reordena en cada refinamiento.

> **Puntos vs. horas:** algunos equipos planifican por velocidad en puntos y otros por
> capacidad en horas-persona. Son dos escalas; **no se convierten** una en otra
> automáticamente. El backlog de Taiga puede llevar los puntos Fibonacci y el plan de
> sprints las horas.

---

## 7. Lista de control antes de entregar

**Roles**
- [ ] Cada rol persigue un objetivo propio y realiza acciones distintas.

**Descripción**
- [ ] El COMO nombra un rol real, no «el sistema» ni «el usuario» a secas.
- [ ] El QUIERO expresa una acción del usuario, no una solución técnica.
- [ ] El PARA expresa un beneficio real, no repite la acción.
- [ ] El título alcanza para identificar la historia en el tablero.

**Notas**
- [ ] Reglas de negocio, datos y restricciones — no diseño de pantallas ni tablas.

**Criterios de aceptación**
- [ ] Al menos un camino feliz y dos negativos.
- [ ] Cada escenario tiene título, DADO, CUANDO y ENTONCES.
- [ ] Cada ENTONCES es observable (pantalla, mensaje o datos).
- [ ] Cada Y extiende claramente al paso que lo precede.
- [ ] Ningún escenario se repite casi igual en dos historias.

**Bocetos**
- [ ] Adjuntados los de las pantallas principales.
- [ ] Cada campo y botón del boceto está cubierto por al menos un escenario.
- [ ] Ningún escenario menciona elementos que no aparecen en ningún boceto.

**Conjunto**
- [ ] Cada historia cumple los seis criterios INVEST.
- [ ] Todas las estimaciones se comparan contra la misma canónica.
- [ ] El backlog está ordenado, no es solo una lista.
