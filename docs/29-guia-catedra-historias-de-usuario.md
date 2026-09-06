# 29 — Guía de cátedra: Historias de Usuario (Metodología de Sistemas II, U1)

> Transcripción a Markdown de la guía práctica **«Metodología de Sistemas II — Unidad
> Temática N.º 1: Historias de Usuario»** (TUP, 2.º año, 4.º cuatrimestre). Material
> de cátedra, se conserva como **referencia de cómo se redactan y estiman las HU**.
> No es una decisión de diseño de Tema 07: es el método que la cátedra evalúa.
>
> Relación con el resto del repo:
> - El **formato de tarjeta** que se usa para cargar HU en Taiga está en
>   [`plantillas/historia-de-usuario-taiga.md`](plantillas/historia-de-usuario-taiga.md)
>   y el de épicas en [`plantillas/epica-taiga.md`](plantillas/epica-taiga.md).
> - La **DoR/DoD** del equipo (distinta de los criterios de aceptación, que son
>   propios de cada HU) está en [23 · §9.2](23-plan-construccion-producto-llm.md).
> - La **estimación en horas** del plan vigente ([23 · §2](23-plan-construccion-producto-llm.md))
>   convive con los **puntos de historia** de esta guía: son escalas distintas; ver §6.
>
> Fuentes citadas por la guía: Cohn (2004, 2005), Jeffries (2001), Wake (2003),
> Schwaber & Sutherland (2020), Palacio (2026), Beck (1999), North (2006), Snyder (2003).

---

## 1. Qué es una historia de usuario

Descripción **breve** de una funcionalidad, redactada en el lenguaje del usuario y
desde su punto de vista, que expresa **qué necesita y por qué**. No es una
especificación técnica, ni un caso de uso, ni un documento de requisitos: es
deliberadamente **corta e incompleta**.

Esa incompletitud es el rasgo central del enfoque: la historia es el **recordatorio
de una conversación pendiente** entre el equipo y el cliente. Lo que no entra en la
tarjeta se resuelve hablando, no escribiendo más. De ahí la costumbre de escribirlas
en fichas de cartulina: si una historia no entra en una ficha, abarca demasiado y hay
que dividirla.

### Las 3 C (Ron Jeffries, 2001)

| C | Qué es | Dónde vive en la tarjeta |
|---|---|---|
| **Ficha** (Card) | El enunciado breve, formato **COMO / QUIERO / PARA**. Lo que se ve en el tablero. | Título + descripción |
| **Conversación** (Conversation) | El diálogo con el cliente / experto de dominio donde se aclara el alcance real. | Notas |
| **Confirmación** (Confirmation) | Los criterios de aceptación: cómo se sabe que la historia está terminada. | Criterios de aceptación (BDD) |

> Sin ficha no hay de qué hablar; sin conversación se implementa lo que el
> programador imaginó; sin confirmación nadie puede afirmar que la historia está lista.

---

## 2. Cómo se elabora una historia — paso a paso

Método aplicado en la guía sobre el caso **«Equipos de eSports»** (una plataforma
donde un jugador crea un equipo para torneos en línea; todos los jugadores deben ser
mayores de edad y el nombre del equipo debe ser único).

### 2.1 Identificación de usuario/s

Identificar a la persona o grupo que **usará** la funcionalidad — no el sistema. Si el
mismo jugador registra el equipo, el usuario es **Jugador**; si lo hiciera un
Administrador de torneos, sería otro rol y otra historia.

### 2.2 Boceto de referencia

El boceto **no es diseño gráfico: es una herramienta de descubrimiento**. Dibujar la
pantalla obliga a preguntas que la conversación deja pasar (qué campos son
obligatorios, qué pasa si una lista viene vacía, cuántos elementos se pueden cargar).
Casi siempre esas preguntas son escenarios de criterios de aceptación esperando ser
escritos.

Tres niveles de fidelidad:

| Nivel | Qué es | Cuándo |
|---|---|---|
| **Baja** (boceto / wireframe) | Trazo a mano, bloques/campos/botones, sin color ni tipografía. Se hace en minutos y se descarta sin costo. | **Recomendado para trabajar HU.** |
| **Media** (mockup) | Estático, con proporciones, disposición y textos reales, sin comportamiento. | Para acordar la organización de la pantalla. |
| **Alta** (prototipo navegable) | Pantallas enlazadas que simulan el flujo completo. | Validar con usuarios finales o funcionalidades críticas. |

**Del boceto a los criterios:** recorrer el dibujo elemento por elemento y
preguntarse *qué pasa si el usuario lo usa bien* y *qué pasa si lo usa mal*. Cada
respuesta distinta del sistema es un escenario. Comprobación final: si un elemento del
boceto no aparece en ningún escenario, o sobra en el dibujo o falta un criterio.

Dos advertencias: el boceto **no reemplaza** a los criterios de aceptación (muestra
qué se ve, no cómo se comporta) y **no decide la implementación** (una lista
desplegable en el dibujo no obliga a resolverlo así).

### 2.3 Título de la historia

Resume la funcionalidad o meta principal. Corto pero claro, para identificar la
historia en el backlog sin leer el detalle.
Ejemplo: **«Crear equipo para torneos en línea»**.

### 2.4 Descripción (COMO / QUIERO / PARA)

La parte más importante: enmarca en una frase el **valor** para el usuario.

- **COMO** [usuario] — quién interactúa con la funcionalidad.
- **QUIERO** [acción] — lo que el usuario quiere hacer.
- **PARA** [propósito] — el beneficio; conecta la acción con el valor.

> *«Como jugador de eSports quiero crear un equipo, para participar en torneos en
> línea.»*

### 2.5 Criterios de aceptación (BDD)

Conjunto de **escenarios** a los que se somete la funcionalidad para considerarse
lista. En esta materia se usa **BDD** (Behavior-Driven Development).

**¿Cuántos escenarios? La regla del camino feliz + lo que puede fallar:**

1. **Camino feliz:** el escenario donde todo sale bien.
2. **Qué puede fallar:** por cada historia, preguntarse qué pasa si —
   - el estado no es el correcto (pedir un taxi cuando no hay taxis),
   - falta un dato o un recurso (no hay conexión a internet),
   - la acción se repite o es inválida (ocupar un taxi ya ocupado).

Cada respuesta distinta del sistema es un escenario. Así «Ocupar taxi» no tiene uno
sino **tres** escenarios: taxi libre (feliz), sin viaje pendiente, y taxi ya ocupado.

**Estructura de un escenario (5 partes):**

| Parte | Palabra clave | Qué es |
|---|---|---|
| Título | *(TÍTULO)* | Resumen de la funcionalidad específica que se valida. |
| Precondición / estado inicial | **DADO** | Estado del sistema antes de la acción. Si no se cumple, el escenario no corre. |
| Acción / disparador | **CUANDO** | La acción que lleva al sistema a actuar; simula el comportamiento real del usuario. |
| Resultado esperado | **ENTONCES** | La respuesta del sistema, precisa y acorde a los requisitos. |
| Condición adicional *(opcional)* | **Y** | Encadena un paso extra a Dado, Cuando o Entonces. Muchos escenarios no la usan. |

**Dos reglas para no equivocarse:**

- **Regla 1 — el ENTONCES tiene que ser observable.** Debe poder verificarse desde
  afuera: en la pantalla, en un mensaje o en los datos guardados.
  - Mal: *«ENTONCES el sistema debe verificar que la fecha de nacimiento indica que
    el jugador es mayor de edad»* (verificar es interno).
  - Bien: *«ENTONCES el sistema debe agregar al jugador a la lista y mostrar sus
    datos cargados»* (se ve en la pantalla).
- **Regla 2 — el Y hereda el paso que lo precede.** Nunca inicia un escenario.
  Después de DADO suma otra precondición; después de CUANDO, otra acción o condición;
  después de ENTONCES, otro resultado. Conviene escribirlo en renglón aparte.

Ejemplo:

```gherkin
Escenario: Registrar nombre del equipo
  DADO que estoy en el formulario de creación de equipos,
  CUANDO ingreso el nombre del equipo,
  Y el nombre de equipo no está en uso,
  ENTONCES el sistema debe permitir ingresar los nombres completos de los jugadores
  y su fecha de nacimiento.
```

### 2.6 Notas técnicas

Registro escrito de la **Conversación**: reglas de negocio, restricciones, datos que
intervienen con su obligatoriedad, dependencias con otras historias, supuestos
acordados. Todo lo que el cliente aclaró y no entra en el COMO/QUIERO/PARA ni en un
escenario.

**Qué NO va en las notas:** diseño de pantallas paso a paso, nombres de tablas o
clases, la solución técnica elegida. La nota responde a *«qué debe cumplirse»*, nunca
a *«cómo se programa»*. Si una nota le quita al equipo la libertad de decidir la
implementación, es una decisión técnica disfrazada de requisito.

> Las tres notas del caso eSports — validaciones de entrada, verificación automática
> de mayoría de edad, unicidad del nombre — son **reglas de negocio**, no soluciones
> técnicas: ninguna dice cómo implementarlas.

### 2.7 Estimación

La tarjeta se cierra con un valor en **puntos de historia**. Se hace **siempre al
final y en equipo**, nunca individualmente, y **no antes de tener los criterios de
aceptación**: hasta saber cuántos escenarios abarca, no se sabe cuánto trabajo
implica. Procedimiento en §5.

---

## 3. INVEST — cómo saber si una historia está bien escrita (Bill Wake, 2003)

Respetar el formato no garantiza que la historia esté bien planteada. Seis criterios
como lista de control:

| Letra | Criterio | Señal de que falla |
|---|---|---|
| **I** | **Independiente** — se puede desarrollar y entregar sin depender del orden de las demás. | Dos historias comparten escenarios o una no tiene sentido sin la otra → mal cortadas o son una sola. |
| **N** | **Negociable** — el alcance se conversa hasta que se implementa; no es un contrato cerrado. | La tarjeta ya trae la solución técnica decidida. |
| **V** | **Valiosa** — entrega valor perceptible para el usuario o el negocio. | «Crear la tabla de jugadores en la BD» — ningún usuario percibe ese resultado: es una tarea técnica. |
| **E** | **Estimable** — el equipo puede dimensionar el esfuerzo. | Nadie logra estimarla → falta conversación o la historia es demasiado grande. |
| **S** | **Small (pequeña)** — entra cómodamente en un sprint. | No se puede terminar en una iteración → es una épica, hay que dividirla. |
| **T** | **Testeable** — hay criterios de aceptación que se responden sí/no sin discusión. | «Que el sistema sea rápido» no es testeable; «que la búsqueda responda en < 2 s» sí. |

> Ejemplo App Taxi: «Pedir taxi» y «Buscar taxis cercanos» comparten el escenario «no
> hay taxis disponibles» → falla la **I**. Se corrige decidiendo cuál se queda con la
> visualización en el mapa y cuál solo con la selección.

---

## 4. Épica, historia y tarea

| Nivel | Qué es | Ejemplo | ¿Formato COMO/QUIERO/PARA? |
|---|---|---|---|
| **Épica** | Funcionalidad grande que no entra en un sprint; se divide en historias a medida que se acerca su desarrollo. | «Gestionar torneos» | Sí (es una HU demasiado grande) |
| **Historia de usuario** | Unidad de valor entregable; se estima y se compromete en un sprint. | «Crear equipo para torneos en línea» | Sí |
| **Tarea** | Paso técnico interno necesario para completar una historia. La define el equipo de desarrollo. | «Maquetar el formulario de alta», «crear el servicio de validación de edad» | **No** |

### ¿Cuándo separo una historia y cuándo la dejo junta?

- **Separá si…** tiene dos verbos de acción distintos; intervienen dos roles
  diferentes; una parte se puede entregar y usar sin la otra; o los escenarios superan
  los seis o siete.
- **Dejala junta si…** por separado ninguna parte entrega valor; los escenarios se
  repetirían casi iguales; o una parte no se puede probar sin la otra.

> **Regla práctica:** una historia = un rol, una acción y un resultado observable. Si
> el título necesita una conjunción («y», «o», «además»), probablemente son dos
> historias.

---

## 5. Estimación con Planning Poker

### 5.1 Qué mide un punto de historia

Unidad de **esfuerzo relativo** que combina en un solo número: **complejidad**,
**volumen** de trabajo e **incertidumbre / riesgo**. No es una hora ni un día. Una
historia sencilla pero muy repetitiva puede valer lo mismo que una corta llena de
dudas.

La escala **pertenece al equipo**: dos equipos pueden estimar la misma historia en 3
y en 8 y ambos estar en lo correcto, porque cada uno la comparó contra su propia
historia canónica. **Los puntos no se comparan entre equipos ni miden productividad.**

### 5.2 El mazo (Fibonacci)

- Valores: 0 · 1 · 2 · 3 · 5 · 8 · 13 · 20 · 40 · 100. **En esta materia se usa el
  tramo 1 · 2 · 3 · 5 · 8 · 13.**
- Los saltos crecen a propósito: cuanto más grande la historia, menos precisión tiene
  sentido pedirle a la estimación.
- **Carta «?»:** no hay información suficiente → volver a conversar con el PO.
- **Carta «∞»:** demasiado grande → es una épica, hay que dividirla.
- **Carta «café»:** necesito un descanso (la fatiga degrada las estimaciones).
- **0:** historias sin esfuerzo apreciable; usar con mucha prudencia.

### 5.3 Historia canónica

Historia de referencia, ya conocida y estimada, contra la cual se comparan las demás.
El valor asignado es una **convención interna**: en «App Taxi» la canónica es «Loguear
pasajero» = 1; en el ejercicio del hospital es «Alta Especialidad» = 2. Lo único que
no puede cambiar es **la referencia dentro de un mismo backlog**: todas las historias
se miden contra la misma canónica o los números dejan de ser comparables.

### 5.4 Velocidad

Suma de los puntos de las historias **efectivamente terminadas** en un sprint.
Después de dos o tres sprints se estabiliza y permite responder cuánto trabajo
comprometer en la próxima iteración. Ese es el propósito de estimar: planificar con
datos propios, no controlar personas.

### 5.5 Procedimiento (10 pasos)

1. **Seleccionar la historia canónica** — representativa, comprendida por todos, con
   estimación ya acordada.
2. **Preparación del equipo** — todos revisan la nueva historia (requisitos, criterios
   de aceptación, riesgos).
3. **Explicación de la historia** — el PO o responsable del backlog la explica en
   detalle.
4. **Comparación con la canónica** — «¿es más o menos compleja que la canónica?».
5. **Discusión de complejidad y riesgos** — validaciones, manejo de errores,
   interacción con la interfaz, etc.
6. **Asignación de cartas** — cada integrante recibe su mazo Fibonacci.
7. **Primera ronda** — cada uno elige una carta en privado; se revelan a la vez.
8. **Discusión de diferencias** — los valores más alto y más bajo explican su
   razonamiento.
9. **Segunda ronda** — nueva votación con la información discutida; se repite hasta
   consenso.
10. **Asignación final** — se fija el valor en función de complejidad, esfuerzo e
    incertidumbre relativos a la canónica.

> Ejemplo: «Pedir taxi» con canónica «Loguear pasajero = 1». Loguearse es tomar datos
> y validarlos; «Pedir taxi» activa el posicionamiento, dibuja un mapa, muestra varios
> taxis, deja seleccionar y maneja errores (sin taxis, sin GPS). Más pasos, más
> incertidumbre → **5**: varias veces más grande que la canónica, pero no tanto como
> para saltar a 8. El número sale de la comparación, no de contar horas.

---

## 6. Priorización y armado del backlog

Estimar responde **«cuánto cuesta»**; priorizar responde **«cuánto vale»**. Son
decisiones distintas y de personas distintas: la **estimación la hace el equipo de
desarrollo**; la **prioridad la fija el Product Owner**. Una historia cara puede ser
prioritaria y una barata puede esperar.

**Cuatro criterios para asignar prioridad:**

- **Valor** para el negocio o el usuario: cuánto se pierde si no está.
- **Riesgo e incertidumbre:** adelantar lo que tiene dudas técnicas para descubrir
  problemas temprano.
- **Dependencias:** si una historia es condición para varias otras, sube de prioridad
  aunque su valor aislado sea bajo (no hay turnos sin especialidades).
- **Costo de la demora:** cuánto cuesta postergarla un sprint más.

El **backlog de producto** es una lista **ordenada**: lo primero es lo próximo a
construir. Ante empate de prioridad, primero la más pequeña (entrega valor antes y
libera capacidad). No es un documento cerrado: se reordena en cada refinamiento.

> **Puntos vs. horas en este repo:** esta guía planifica por velocidad en puntos; el
> [plan vigente (23 · §2)](23-plan-construccion-producto-llm.md) planifica por
> capacidad en horas-persona (~571 h de capacidad por sprint; ~208 h de trabajo estimado). Son
> dos formas de dimensionar; el backlog de Taiga puede llevar los puntos Fibonacci y el plan de
> sprints las horas. No se convierten unos en otras automáticamente.

---

## 7. Ejemplo completo — «Crear equipo para torneos en línea» (eSports)

```
Título: Crear equipo para torneos en línea

COMO jugador de eSports QUIERO crear un equipo, PARA participar en torneos en línea.
                                                                     Estimación: 5

Notas:
  - El formulario debe incluir validaciones de entrada para evitar datos incorrectos.
  - La fecha de nacimiento debe ser verificada automáticamente para garantizar la
    mayoría de edad.
  - El nombre del equipo debe ser único dentro de la plataforma.

Criterios de aceptación:

  Escenario: Registrar nombre del equipo
    DADO que estoy en el formulario de creación de equipos,
    CUANDO ingreso el nombre del equipo,
    Y el nombre de equipo no está en uso,
    ENTONCES el sistema debe permitir ingresar los nombres completos de los jugadores
    y su fecha de nacimiento.

  Escenario: Añadir jugadores al equipo
    DADO que estoy creando un equipo,
    CUANDO agrego el nombre completo de un jugador y su fecha de nacimiento,
    ENTONCES el sistema debe agregar al jugador a la lista del equipo y mostrar sus
    datos cargados.

  Escenario: Verificar mayoría de edad
    DADO que he ingresado la fecha de nacimiento de un jugador,
    CUANDO el sistema valida la fecha,
    ENTONCES debe mostrar un mensaje de error si el jugador es menor de 18 años,
    Y no permitir registrar al equipo hasta que todos los jugadores sean mayores de
    edad.

  Escenario: Registro exitoso del equipo
    DADO que he completado todos los campos requeridos correctamente,
    CUANDO hago clic en el botón "Registrar equipo",
    ENTONCES el sistema debe registrar el equipo para competir en torneos en línea
    Y mostrar un mensaje de confirmación de registro exitoso.

  Escenario: Nombre de equipo único
    DADO que estoy registrando un equipo,
    CUANDO ingreso un nombre de equipo que ya está en uso,
    ENTONCES el sistema muestra un mensaje de error indicando que el nombre ya está
    en uso.
```

---

## 8. Caso «App Taxi» — de la entrevista a las historias

**Objetivo:** app para smartphones donde los pasajeros solicitan el taxi más cercano
y ven su ubicación y demora en todo momento.

### 8.1 Método de dos pasos para leer una entrevista

1. **Identificar los roles.** Un rol es quien interactúa con el sistema persiguiendo
   un objetivo propio. *¿Quién realiza acciones distintas?* → pasajero (pide taxi),
   taxista (responde pedidos), central (supervisa) = **3 roles**.
2. **Extraer las historias.** Subrayar los verbos de acción de cada rol; cada acción
   que un rol quiere realizar es una historia candidata:
   - Pasajero: se loguea, busca taxis, pide un taxi, notifica → 4.
   - Taxista: se loguea, marca ocupado, marca fuera de servicio → 3.
   - Administrador de la central: da de alta la central → 1.

   No todas terminan siendo una historia (algunas se agrupan), pero el barrido asegura
   no dejar funcionalidad afuera.

### 8.2 Roles

| Rol | Descripción |
|---|---|
| **Pasajero** | Usa la app con frecuencia para pedir un taxi a su ubicación. Prioriza la simplicidad; familiarizado con smartphone. Espera que el taxi llegue lo más rápido posible. |
| **Taxista** | Usa la app mientras trabaja: necesita notificaciones sonoras, manos libres, ver la posición del pasajero y minimizar interacciones con el celular. |
| **Central de taxis** | Trabaja con app web y Google Maps. Necesita ver los taxis con su estado y la localización de los pasajeros, en tiempo real, para asistir a los taxistas. |

### 8.3 Formato de tarjeta usado

```
Título de HU
<COMO>… <QUIERO>… <PARA>…                                          Estimación
Notas: Información complementaria de la HU
Criterio de Aceptación
  Escenario 1 … Escenario N
```

### 8.4 Historias (con estimación)

**Loguear pasajero — 1 (canónica)**
COMO pasajero QUIERO loguearme PARA poder visualizar los taxis más cercanos.
Notas: datos de login nombre, apellido, celular (opcional); pueden tomarse de Facebook
o del celular.
Escenarios: obtener datos de Facebook con conexión (feliz) · sin conexión (error) ·
solicitar datos manualmente si Facebook falla.

**Loguear taxista — 1**
COMO taxista QUIERO loguearme PARA poder visualizar los pedidos de taxis.
Notas: nombre, apellido, celular, dominio, número de móvil, central.
Escenarios: taxista asociado a una central (feliz) · no asociado (error).

**Alta de central de taxis — 1**
COMO administrador de la central QUIERO dar de alta la central PARA poder recibir y
tomar viajes con Taxi Mobile.
Escenarios: registrar central inexistente (feliz) · registrar central existente (error).

**Pedir taxi — 5**
COMO pasajero QUIERO poder pedir un taxi seleccionando el más conveniente de un mapa
PARA asegurarme de que el taxi está cerca.
Notas: el celular debe tener posicionamiento online; el pasajero se visualiza en un
mapa.
Escenarios: seleccionar un taxi entre varios (feliz) · sin taxis disponibles (error) ·
posicionamiento inactivo (error).

**Ocupar taxi — 2**
COMO taxista QUIERO marcar que el taxi se encuentra ocupado PARA no recibir pedidos
que no podré atender.
Escenarios: ocupar con viaje pendiente (se asocia al viaje) · ocupar sin viaje
pendiente (ok) · ocupar un taxi ya ocupado (error).

**Marcar taxi como fuera de servicio — 2**
COMO taxista QUIERO marcar que el taxi está fuera de servicio PARA dejar de trabajar y
no recibir pedidos.
Escenarios: taxi libre (ok) · con viaje pendiente (error) · ya ocupado (error).

**Notificar a taxista y central por un pedido — 2**
COMO pasajero QUIERO que el sistema notifique al taxista y a la central al solicitar
un viaje PARA que el taxista me busque y la central esté enterada.
Escenarios: ambos con conexión (feliz) · ninguno con conexión (error).

**Buscar taxis cercanos — 3**
COMO pasajero QUIERO ver los 5 taxis más cercanos a mi ubicación PARA pedir el que más
me convenga.
Notas: se muestra ubicación del taxi y tiempo estimado de llegada.
Escenarios: al menos 5 taxis libres (feliz) · ningún taxi libre (error).

---

## 9. Ejercicio resuelto — «Backlog Hospital» (estimación y priorización)

Canónica: **«HU - Alta Especialidad» = 2 puntos**.

### 9.1 Backlog sin ordenar

| Historia de usuario | Estimación |
|---|---:|
| HU - Administrar Consultorios | 3 |
| HU - Iniciar sesión con Facebook | 2 |
| HU - Invitar profesionales | 5 |
| HU - Alta Especialidad (canónica) | 2 |
| HU - Editar Especialidad | 1 |
| HU - Alta de Turno | 5 |

### 9.2 Backlog ordenado (una solución posible)

| Orden | Historia de usuario | Prioridad | Est. |
|---:|---|---|---:|
| 1 | HU - Alta Especialidad (canónica) | Alta | 2 |
| 2 | HU - Administrar Consultorios | Alta | 3 |
| 3 | HU - Invitar profesionales | Alta | 5 |
| 4 | HU - Iniciar sesión con Facebook | Media | 2 |
| 5 | HU - Alta de Turno | Media | 5 |
| 6 | HU - Editar Especialidad | Baja | 1 |

> «Alta Especialidad» encabeza no por su valor propio (bajo) sino porque **el resto
> depende de que existan las especialidades cargadas**. El backlog suma **18 puntos**;
> con velocidad de 10 puntos/sprint, las tres primeras entran en el sprint 1 y las
> tres restantes en el 2. Ese razonamiento —y no el conteo de horas— es el que permite
> planificar en Scrum.

---

## 10. Lista de control antes de entregar

**Roles**
- [ ] Cada rol persigue un objetivo propio y realiza acciones distintas. Ninguno es el
      mismo rol con otro nombre.

**Descripción de cada historia**
- [ ] El COMO nombra un rol de la tabla, no «el sistema» ni «el usuario» a secas.
- [ ] El QUIERO expresa una acción del usuario, no una solución técnica.
- [ ] El PARA expresa un beneficio real, no repite la acción con otras palabras.
- [ ] El título alcanza para identificar la historia en el tablero sin leer el resto.

**Notas**
- [ ] Contienen reglas de negocio, datos y restricciones — no diseño de pantallas ni
      nombres de tablas.

**Criterios de aceptación**
- [ ] Al menos un escenario de camino feliz y dos de error.
- [ ] Cada escenario tiene título, DADO, CUANDO y ENTONCES.
- [ ] Cada ENTONCES describe algo observable (pantalla, mensaje o datos guardados).
- [ ] Cada Y extiende claramente al DADO, CUANDO o ENTONCES que lo precede.
- [ ] Ningún escenario se repite casi igual en dos historias distintas.

**Bocetos**
- [ ] Adjuntados los de las pantallas principales.
- [ ] Cada campo y botón del boceto está cubierto por al menos un escenario.
- [ ] Ningún escenario menciona elementos que no aparecen en ningún boceto.

**Conjunto**
- [ ] Cada historia cumple los seis criterios INVEST.
- [ ] Todas las estimaciones se justificaron por comparación con la misma canónica.
- [ ] El backlog está ordenado, no es solo una lista de historias.

---

## 11. Glosario

| Término | Definición |
|---|---|
| **Historia de usuario (HU)** | Descripción breve de una funcionalidad, en lenguaje del usuario y desde su punto de vista; recordatorio de una conversación pendiente, no una especificación cerrada. |
| **Modelo 3C** | Las tres dimensiones de una HU (Jeffries): Ficha (Card), Conversación (Conversation), Confirmación (Confirmation). |
| **Criterio de aceptación** | Condición que la funcionalidad debe cumplir para considerarse terminada. Se expresa como uno o varios escenarios DADO / CUANDO / ENTONCES. |
| **Escenario** | Caso concreto de comportamiento descrito en un criterio de aceptación. Cada respuesta distinta del sistema ante una misma acción es un escenario. |
| **Camino feliz (happy path)** | Escenario en el que todo ocurre como se espera. Es el primero que se escribe; los demás describen qué pasa cuando algo falla. |
| **Gherkin** | Lenguaje estructurado y legible para escribir escenarios BDD con Given/When/Then (Dado/Cuando/Entonces). Lo interpretan herramientas como Cucumber. |
| **INVEST** | Seis criterios de calidad de una HU (Wake): Independiente, Negociable, Valiosa, Estimable, Small, Testeable. |
| **Épica** | HU demasiado grande para completarse en un sprint. Se divide a medida que se acerca su desarrollo. |
| **Tarea** | Paso técnico interno para completar una HU. La define el equipo; no se escribe con COMO/QUIERO/PARA. |
| **Prototipo** | Representación anticipada de pantallas para conversar antes de programar. Niveles: boceto, mockup, prototipo navegable. |
| **Boceto (wireframe)** | Prototipo de baja fidelidad, a mano alzada, bloques/campos/botones sin color. Nivel recomendado para trabajar HU. |
| **Mockup** | Prototipo de media fidelidad: estático con proporciones, disposición y textos reales, sin comportamiento. |
| **Prototipo navegable** | Alta fidelidad: pantallas enlazadas que simulan el flujo completo sin sistema por detrás. |
| **Backlog de producto** | Lista ordenada de todo lo que el producto necesita. El orden lo define el PO. No es cerrado: se refina de forma continua. |
| **Sprint** | Iteración de duración fija (1–4 semanas) en la que se construye un incremento terminado. Unidad de tiempo contra la que se dimensionan las historias. |
| **Incremento** | Conjunto de historias terminadas al cerrar un sprint que cumple la DoD y podría entregarse. |
| **Refinamiento del backlog** | Actividad continua: dividir historias grandes, aclarar criterios, estimar lo que falta, reordenar. |
| **Product Owner (PO)** | Representa la voz del cliente y del negocio. Define y ordena el backlog, fija prioridades. No estima. |
| **Experto en el dominio (ED)** | Conoce en profundidad el negocio; aporta reglas, restricciones y casos particulares en las entrevistas. |
| **Punto de historia** | Unidad de esfuerzo relativo (complejidad + volumen + incertidumbre). No equivale a horas; su escala es propia de cada equipo. |
| **Historia canónica** | Historia de referencia ya estimada contra la que se comparan las demás. El valor asignado es una convención interna. |
| **Planning Poker** | Estimación colaborativa: cada integrante elige una carta en privado, se revelan a la vez, se discuten diferencias y se converge por consenso. |
| **Velocidad** | Puntos de historia que un equipo completa efectivamente en un sprint. Predice cuánto comprometer en la siguiente iteración. |
| **Definition of Done (DoD)** | Acuerdo del equipo sobre las condiciones que debe cumplir **cualquier** incremento para considerarse terminado. Común a todas las historias (a diferencia de los criterios de aceptación, propios de cada una). En este repo: [23 · §9.2](23-plan-construccion-producto-llm.md). |
| **BDD (Behavior-Driven Development)** | Metodología centrada en definir el comportamiento esperado con ejemplos concretos («Dado, Cuando, Entonces»); promueve la colaboración entre técnicos y no técnicos y produce especificaciones ejecutables. Herramientas: Cucumber, Behave, RSpec, Serenity BDD, Gauge, Jasmine, Reqnroll, JBehave. |
