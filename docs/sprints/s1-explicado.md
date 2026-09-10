# Sprint 1 — las nueve historias explicadas en palabras simples

> **Qué es este documento.** Son las mismas nueve historias del Sprint 1 que están,
> en formato técnico, en [`../historias/ep-01.md`](../historias/ep-01.md) y
> [`../historias/ep-03.md`](../historias/ep-03.md), con la **misma estructura del
> template de Taiga** (Como / Quiero / Para, Notas, Criterios de Aceptación, escenarios
> BDD, Prototipo, Estimación y Dependencias), pero contadas **sin jerga técnica**: como
> si se las explicara a alguien que nunca programó. Mismo contenido, mismo objetivo de
> la demo. Cambia solo el idioma.
>
> **Qué NO es.** No es fuente de verdad para planificar. Si un número, una fecha
> o una responsabilidad no coincide, vale lo que dice:
>
> | Dato | Dónde manda |
> |---|---|
> | ID, grupo, pareja, dependencias, horas | [`Plan de ejecucion/07`](<../../Plan de ejecucion/07-backlog-ejecutable-sprints.md>) · «S1» |
> | Ficha técnica completa (con los códigos y términos exactos) | [`../historias/`](../historias/README.md) |
> | Índice del sprint, tipo de cada historia y demo | [`s1-historias.md`](s1-historias.md) |
> | Desglose en tareas | [`../tareas/`](../tareas/README.md) |
> | Requisitos para empezar una historia y para darla por terminada | [23 · §9.2](../23-plan-construccion-producto-llm.md) |
>
> **Sobre los códigos raros que van a aparecer.** Cosas como `EP-01`, `P1`,
> `LLM-S01-H04` o `RF-NFR-01` son etiquetas internas del equipo para poder
> rastrear cada cosa. Las dejo en las fichas porque son parte del formato de
> Taiga, pero no hace falta entenderlas para seguir el documento.
>
> **Cuatro palabras que se repiten:**
>
> - **Sprint:** un tramo de trabajo de unas pocas semanas con un objetivo cerrado.
>   Este documento es sobre el "Sprint 1", el primer tramo. El **"Sprint 0"** es la
>   reunión de arranque previa, donde se planifica y se estima.
> - **Demo:** la demostración que se hace al final del tramo para mostrar lo que
>   quedó funcionando.
> - **Escenarios (o "BDD"):** ejemplos concretos escritos como "**Dado** tal
>   situación, **Cuando** pasa tal cosa, **Entonces** el sistema hace tal otra cosa".
>   Sirven para acordar de antemano qué se considera "bien hecho".
> - **Recepción central:** la puerta única por la que entran todos los pedidos a
>   la plataforma. Ningún programa habla con otro directamente; todos pasan por
>   ahí, que es donde se controla la identidad y los permisos.

---

## Antes de las fichas: ¿de qué va todo esto?

Hay una plataforma donde los estudiantes aprenden a programar jugando. Se quiere
que, más adelante, una **inteligencia artificial** ayude a corregir los
ejercicios y a darles una devolución a los alumnos.

Antes de dejar que la máquina corrija sola, hay que **enseñarle con ejemplos
bien hechos por personas**. A ese conjunto de ejercicios ya corregidos a mano lo
llamamos **golden set** (la "colección de referencia" o "colección dorada"). Es
como el solucionario del profesor: la máquina después se compara contra eso para
ver si corrige parecido a un humano.

**En este primer tramo de trabajo (el "Sprint 1") todavía NO se construye la
inteligencia artificial.** Se construyen los cimientos: ponerse de acuerdo,
lograr que el sistema encienda con un comando, armar el esqueleto del servicio,
preparar la base de datos, y darle a un profesor la posibilidad de **crear** su
colección de referencia, **cargarle** ejemplos y **volver a consultarlos**, con
una pantalla sencilla para hacerlo.

**La demo final de este tramo, en una frase:** un profesor con permiso crea su
colección de referencia, le carga un ejemplo, apagamos y volvemos a encender el
sistema, y el ejemplo sigue ahí cuando lo consulta.

---

## Índice

| # | Título | Tipo | Grupo | Pareja | Depende de | Trabajo |
|---|---|---|---|---|---|---:|
| [H01](#llm-s01-h01--ponerse-de-acuerdo-sobre-cómo-se-construye-el-programa) | Ponerse de acuerdo sobre cómo se construye el programa | Tarea interna | EP-01 | P1 | — | 16 h |
| [H02](#llm-s01-h02--encender-todo-el-entorno-con-un-solo-comando) | Encender todo el entorno con un solo comando | Tarea interna | EP-01 | P1 | H01 | 30 h |
| [H03](#llm-s01-h03--el-esqueleto-del-servicio-puertas-de-entrada-y-seguridad) | El esqueleto del servicio: puertas de entrada y seguridad | Tarea interna | EP-01 | P1 | H02 | 34 h |
| [H04](#llm-s01-h04--preparar-la-base-de-datos-para-que-no-se-pueda-romper-ni-borrar) | Preparar la base de datos para que no se pueda romper ni borrar | Tarea interna | EP-01 | P1 | H03 | 38 h |
| [H05](#llm-s01-h05--que-el-profesor-cree-su-colección-de-referencia-y-le-cargue-ejemplos) | Que el profesor cree su colección de referencia y le cargue ejemplos | Historia de valor | EP-03 | P5 | H04 | 24 h |
| [H06](#llm-s01-h06--consultar-la-colección-aunque-el-sistema-se-reinicie) | Consultar la colección aunque el sistema se reinicie | Historia de valor *(patrón)* | EP-03 | P5 | H04 | 14 h |
| [H07](#llm-s01-h07--la-pantalla-sencilla-para-el-profesor) | La pantalla sencilla para el profesor | Historia de valor | EP-03 | P5 | H05, H06 | 24 h |
| [H08](#llm-s01-h08--publicar-el-acuerdo-y-una-maqueta-para-que-otro-equipo-avance) | Publicar el acuerdo y una maqueta para que otro equipo avance | Tarea interna | EP-01 | P1 | H03 | 10 h |
| [H09](#llm-s01-h09--las-pruebas-automáticas-y-la-guía-para-la-demostración) | Las pruebas automáticas y la guía para la demostración | Tarea interna | EP-01 | todas | H04–H07 | 18 h |
| | | | | | **Total** | **208 h** |

**Tipo:** "historia de valor" quiere decir que la protagoniza una persona de
carne y hueso (un profesor) que nota el beneficio. "Tarea interna" es un cimiento
del que depende el resto, pero que ningún usuario final "vive" directamente.

**Tareas:** cada historia se parte en **tareas**: los pasos concretos en los que el
equipo de desarrollo divide el trabajo. No se escriben en formato "Como / Quiero /
Para" (eso es para las historias) y cada una está pensada para durar **un día de
trabajo o menos**. Las tareas de las nueve historias, con su método (SMART) y sus
horas, están en [`../tareas/`](../tareas/README.md); el detalle técnico de cada
historia, en [`../historias/`](../historias/README.md).

---

# LLM-S01-H01 — Ponerse de acuerdo sobre cómo se construye el programa

- **Grupo de trabajo:** Plataforma, contratos e integración (EP-01)
- **Pareja a cargo:** P1
- **Depende de:** Nada
- **Trabajo estimado:** 16 horas
- **Tipo:** Tarea interna (no la "vive" un usuario final)
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** equipo que programa este servicio.
- **Quiero:** un documento con las reglas de cómo se arma el programa.
- **Para:** que las cinco parejas de programadores construyan todas sobre las
  mismas bases, y no que cada una arme las cosas a su manera.

## Notas / Observaciones

- **Reglas de trabajo:** el documento fija con qué lenguaje y con qué
  herramientas se programa, y cómo se divide el programa en partes (la "puerta de
  entrada", la lógica del negocio, el "corazón" con las reglas, la parte que
  habla con la base de datos, la seguridad y la configuración). La regla más
  importante: **el "corazón" del programa no puede depender de herramientas
  externas**, para que siga funcionando aunque esas herramientas cambien.
- **Cómo se controla:** que cada parte respete su lugar se puede revisar con un
  control automático o en la revisión de un compañero.
- **Qué tiene que incluir sí o sí:** la lista de "perillas de configuración" del
  sistema (qué hace cada una y qué valor trae por defecto), y la decisión escrita
  con fecha, alternativas que se descartaron y quiénes la tomaron.
- **Tiempos / volumen:** no aplica.
- **Seguridad:** el documento dice **dónde viven las claves y secretos** (fuera
  del "repositorio", que es el archivo compartido donde vive todo el código del
  proyecto) y cómo se le entregan al programa.
- **Accesibilidad:** no aplica (es un documento interno).
- **Otros:** el documento queda guardado junto con el código del proyecto. Cambiar una regla
  exige escribir un documento nuevo que reemplaza al anterior; no se edita el
  texto viejo.

## Criterios de Aceptación (CA)

- **CA1:** existe el documento, publicado y aprobado por el equipo, con fecha y
  con los nombres de quiénes lo escribieron.
- **CA2:** el documento enumera las partes del programa y qué parte puede
  apoyarse en cuál.
- **CA3:** el documento incluye la tabla de "perillas de configuración".
- **CA4 (caso que debe fallar):** una propuesta de cambio que haga que el
  "corazón" del programa dependa de una herramienta externa prohibida es
  **rechazada**, citando este documento.
- **CA5 (caso que debe fallar):** usar una "perilla de configuración" que no
  está en la lista se marca en la revisión.

## BDD

**Qué se prueba:** que las cinco parejas de programadores arranquen todas de la
misma forma.

### Escenario 1 — El acuerdo permite arrancar sin desprolijidades

- **Dado:** el documento está aprobado y publicado.
- **Cuando:** una pareja crea la parte del programa que le toca.
- **Entonces:** su estructura coincide con la del documento y la revisión no
  encuentra desvíos.

### Escenario 2 — Se protege el "corazón" del programa

- **Dado:** el documento prohíbe que el "corazón" dependa de herramientas
  externas.
- **Cuando:** una propuesta de cambio agrega esa dependencia prohibida.
- **Entonces:** el control falla y el cambio no se puede incorporar.

### Escenario 3 — Configuración fuera del acuerdo

- **Dado:** el documento lista las "perillas de configuración" admitidas.
- **Cuando:** un cambio usa una que no está documentada.
- **Entonces:** la revisión lo marca y pide actualizar el documento antes de
  aprobar.

## Prototipo

- **Capturas:** no aplica (es un documento).
- **Maqueta / pantallas:** no aplica.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0, con la técnica de
  estimación del equipo.
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** todo el servicio.
- **Otros equipos / aprobaciones:** ninguna externa.
- **Impacto en los datos:** ninguno.
- **Riesgos:** si este documento llega tarde, obliga a rehacer trabajo. Se
  resuelve el primer día del sprint y bloquea al resto de las historias hasta
  estar aprobado.

---

# LLM-S01-H02 — Encender todo el entorno con un solo comando

- **Grupo de trabajo:** Plataforma, contratos e integración (EP-01)
- **Pareja a cargo:** P1
- **Depende de:** H01
- **Trabajo estimado:** 30 horas
- **Tipo:** Tarea interna
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** cualquier programador del equipo.
- **Quiero:** prender todo el entorno de trabajo en mi computadora escribiendo
  una sola línea.
- **Para:** empezar a trabajar sin pasar días instalando y configurando
  programas a mano.

## Notas / Observaciones

- **Reglas de trabajo:** un solo comando levanta la base de datos y —según lo
  que haga falta— el resto de las piezas comunes de la plataforma, y deja todo
  "sano" (listo para usar). Cómo prender, cómo comprobar que quedó bien y cómo
  apagar están todos documentados.
- **Cómo se controla:** cada pieza tiene su propio "chequeo de salud"; el
  comando no se da por exitoso hasta que **todas** reportan que están sanas.
- **Qué tiene que incluir sí o sí:** el archivo que describe el entorno, un
  archivo de ejemplo con las "perillas de configuración" del documento de la
  tarea 1, y una sección del manual con los tres comandos (prender, comprobar,
  apagar).
- **Tiempos:** en frío, el entorno arranca en un tiempo razonable en una
  notebook del equipo.
- **Seguridad:** el entorno usa credenciales de prueba, nunca claves reales; el
  archivo con la configuración real no se guarda junto con el código.
- **Accesibilidad:** no aplica.
- **Otros:** si en la computadora falta la herramienta base, el arranque falla
  con un mensaje que la nombra; no deja las cosas encendidas a medias.

## Criterios de Aceptación (CA)

- **CA1:** en una computadora limpia con la herramienta base, el comando deja
  **todo funcionando**.
- **CA2:** el manual explica cómo prender, comprobar y apagar, y esos tres pasos
  funcionan de verdad.
- **CA3 (caso que debe fallar):** sin la herramienta base instalada, el comando
  falla con un mensaje claro que dice qué falta, y **no** arranca cosas a medias.
- **CA4 (caso que debe fallar):** si un "enchufe" (puerto) necesario ya está
  ocupado, el error dice exactamente cuál es el que está en conflicto.

## BDD

**Qué se prueba:** que el entorno se pueda reproducir en cualquier máquina.

### Escenario 1 — Arranque en una computadora limpia

- **Dado:** una computadora con la herramienta base y el proyecto descargado,
  sin nada más instalado.
- **Cuando:** el programador ejecuta el comando de arranque.
- **Entonces:** la base de datos (y las demás piezas necesarias) quedan sanas.
- **Y:** el programador puede usar el servicio sin instalar nada más.

### Escenario 2 — Falta la herramienta base

- **Dado:** una computadora sin la herramienta base.
- **Cuando:** el programador ejecuta el comando de arranque.
- **Entonces:** el comando falla y avisa que esa herramienta es un requisito,
  sin dejar cosas a medio encender.

### Escenario 3 — Un enchufe ocupado

- **Dado:** el "enchufe" de la base de datos ya está en uso por otro programa.
- **Cuando:** el programador ejecuta el comando de arranque.
- **Entonces:** el arranque falla con un mensaje que nombra el enchufe en
  conflicto.

## Prototipo

- **Capturas:** la salida esperada en pantalla al prender y al apagar (se
  adjunta en la ficha de Taiga).
- **Maqueta / pantallas:** no aplica.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0.
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** la base de datos y las piezas comunes de la
  plataforma.
- **Otros equipos / aprobaciones:** los responsables de esas piezas comunes.
- **Impacto en los datos:** el "cajón" de la base de datos tiene que
  **sobrevivir a los reinicios** (lo necesita la tarea 6).
- **Riesgos:** que el entorno local no se parezca al real. Se acota levantando
  solo lo que la plataforma realmente expone y documentando cada pieza.

---

# LLM-S01-H03 — El esqueleto del servicio: puertas de entrada y seguridad

- **Grupo de trabajo:** Plataforma, contratos e integración (EP-01)
- **Pareja a cargo:** P1
- **Depende de:** H02
- **Trabajo estimado:** 34 horas
- **Tipo:** Tarea interna
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** la plataforma de aprendizaje (los otros equipos que después van a
  usar este servicio).
- **Quiero:** que este servicio muestre su "esqueleto" —sus puertas de entrada y
  su seguridad— antes de que existan las funciones de inteligencia artificial.
- **Para:** que los demás equipos empiecen a conectarse contra algo estable, sin
  esperar a que todo esté terminado.

## Notas / Observaciones

- **Reglas de trabajo:** el servicio se "anota" en un **directorio** para que
  los demás lo encuentren. Solo se entra por la **recepción central** de la
  plataforma (nadie entra por la ventana). Cuando algo sale mal, el error se
  devuelve en un **formato ordenado y siempre igual**, no como un mensaje
  improvisado.
- **Cómo se controla:** cada visitante recibe de vuelta un **número de
  seguimiento** para poder rastrear su pedido. Un visitante sin los permisos
  necesarios recibe un "no autorizado". Una identificación **falsificada** o mal
  armada recibe un "prohibido".
- **Qué viaja en cada pedido:** quién es el programa que llama, qué permisos
  tiene, en nombre de qué persona actúa, y el número de seguimiento.
- **Tiempos:** el "chequeo de salud" responde sin llamar a servicios externos.
- **Seguridad:** ninguna función queda accesible sin pasar por la recepción
  central; en los registros del sistema **nunca** se anotan las credenciales.
- **Accesibilidad:** no aplica.
- **Otros:** el control de calidad automático del servicio queda "en verde"
  (todo pasa). En esta historia solo se exponen el "chequeo de salud" y la
  puerta general protegida; las funciones de la colección de referencia llegan
  en las tareas 5 y 6.

## Criterios de Aceptación (CA)

- **CA1:** el servicio aparece en el directorio y responde a través de la
  recepción central.
- **CA2:** un pedido con credencial válida y permisos correctos obtiene
  respuesta y recibe de vuelta su número de seguimiento.
- **CA3:** los errores se devuelven en el formato ordenado, con tipo, título,
  código y detalle.
- **CA4 (caso que debe fallar):** credencial sin el permiso necesario → "no
  autorizado", en el formato ordenado.
- **CA5 (caso que debe fallar):** identificación de la persona ausente o
  falsificada → "prohibido".
- **CA6:** el control de calidad automático del servicio está "en verde".

## BDD

**Qué se prueba:** que exista un punto de conexión estable contra la cual conectarse.

### Escenario 1 — Conectarse contra un punto de conexión estable

- **Dado:** el servicio anotado en el directorio y la recepción central
  enviándole los pedidos.
- **Cuando:** otro servicio llama con credencial válida y permisos correctos.
- **Entonces:** recibe una respuesta.
- **Y:** la respuesta incluye el mismo número de seguimiento que él envió.

### Escenario 2 — Credencial sin permiso

- **Dado:** otro servicio con credencial válida pero sin el permiso necesario.
- **Cuando:** llama a una función del servicio.
- **Entonces:** recibe un "no autorizado", en el formato ordenado.

### Escenario 3 — Identificación de persona falsificada

- **Dado:** un pedido que trae una identificación de persona que no vino de la
  recepción de confianza.
- **Cuando:** llega al servicio.
- **Entonces:** se rechaza con "prohibido" y no se ejecuta ninguna acción.

## Prototipo

- **Capturas:** no aplica (es una pieza interna, no tiene pantalla).
- **Maqueta / documentación:** el "chequeo de salud" y el
  [documento base del contrato](../contracts/llm-service-v1.openapi.yaml).

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0.
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** la recepción central, el directorio de servicios y el
  emisor de credenciales.
- **Otros equipos / aprobaciones:** el equipo de la recepción central y el
  directorio (ruta, identificación del servicio y permisos).
- **Impacto en los datos:** ninguno.
- **Riesgos:** el "contrato" de qué viaja en cada pedido puede cambiar. Se aísla
  en la parte de seguridad y se cubre con pruebas.

---

# LLM-S01-H04 — Preparar la base de datos para que no se pueda romper ni borrar

- **Grupo de trabajo:** Plataforma, contratos e integración (EP-01)
- **Pareja a cargo:** P1
- **Depende de:** H03
- **Trabajo estimado:** 38 horas
- **Tipo:** Tarea interna
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** la plataforma, que es la dueña de los datos académicos.
- **Quiero:** que la base de datos se cree de forma **ordenada y con registro**
  de cada cambio.
- **Para:** que los datos de referencia nazcan sabiéndose quién hizo qué y
  cuándo, y sin que nadie pueda borrarlos de un plumazo.

## Notas / Observaciones

- **Reglas de trabajo:** un proceso automático, corriendo sobre una base vacía,
  crea el archivador completo y la **plantilla de corrección "versión 1.0"** con
  las **cinco cosas que se evalúan** de cada ejercicio y cuánto pesa cada una:
  autonomía (30), claridad (25), progresión (20), cumplimiento (15), eficiencia
  (10).
- **Cómo se controla:** las tablas de datos académicos **solo admiten agregar,
  nunca borrar ni tachar**. Un puntaje de referencia incompleto (menos de cinco
  criterios) o fuera de rango (0 a 100) se rechaza. No pueden existir dos
  registros "iguales" para la misma operación y la misma persona (esto evita que
  algo se cargue dos veces por un problema de conexión).
- **Qué tiene que incluir sí o sí:** la versión de la plantilla, el idioma
  (español), y la definición de los cinco criterios con sus pesos.
- **Tiempos:** el proceso corre en segundos sobre una base vacía y se puede
  repetir dando siempre el mismo resultado.
- **Seguridad:** el registro guarda quién actuó (qué programa y en nombre de qué
  persona), sobre qué, y el número de seguimiento; **no** guarda las
  conversaciones de los alumnos como texto libre.
- **Accesibilidad:** no aplica.
- **Otros:** cambiar la plantilla de corrección exige una versión nueva, no
  editar la 1.0.

## Criterios de Aceptación (CA)

- **CA1:** el proceso automático sobre una base vacía deja creado todo el
  archivador y la plantilla 1.0 con sus cinco criterios y sus pesos.
- **CA2:** correr el mismo proceso otra vez desde cero da **exactamente el mismo
  resultado**.
- **CA3 (caso que debe fallar):** un intento de borrar o tachar algo del
  archivador falla con un error que dice que esa tabla no admite cambios.
- **CA4 (caso que debe fallar):** cargar un ejemplo con solo cuatro criterios, o
  con un puntaje de 120 sobre 100, se rechaza.
- **CA5 (caso que debe fallar):** dos registros "iguales" para la misma
  operación y la misma persona no pueden coexistir.

## BDD

**Qué se prueba:** un archivador académico ordenado y que no se puede alterar.

### Escenario 1 — Armado desde una base vacía

- **Dado:** una base de datos vacía.
- **Cuando:** el servicio arranca y corre el proceso automático de armado.
- **Entonces:** existen las tablas de la colección de referencia y la plantilla
  1.0 con sus cinco criterios y pesos.

### Escenario 2 — La tabla no admite borrado ni edición

- **Dado:** una colección de referencia ya guardada.
- **Cuando:** alguien intenta borrarla o modificarla.
- **Entonces:** la base rechaza la operación e indica que esa tabla solo admite
  agregar.

### Escenario 3 — Puntaje de referencia inválido

- **Dado:** la regla de que un ejemplo tiene los cinco criterios con valores de
  0 a 100.
- **Cuando:** se intenta cargar un ejemplo con solo cuatro criterios (o con un
  valor de 120).
- **Entonces:** la carga se rechaza y no queda ningún registro.

## Prototipo

- **Capturas:** un diagrama de las tablas creadas y cómo se relacionan (se
  adjunta en Taiga).
- **Maqueta / documentación:** no aplica (es la capa de datos).

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0.
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** la base de datos.
- **Otros equipos / aprobaciones:** la definición docente de los cinco criterios
  y sus pesos.
- **Impacto en los datos:** crea el archivador base; todas las historias
  siguientes dependen de él.
- **Riesgos:** si los criterios o pesos cambian después de este tramo, se crea
  una versión nueva de la plantilla; la 1.0 no se toca.

---

# LLM-S01-H05 — Que el profesor cree su colección de referencia y le cargue ejemplos

- **Grupo de trabajo:** Colección de referencia y calibración humana (EP-03)
- **Pareja a cargo:** P5 (con P4 en la definición de la plantilla)
- **Depende de:** H04
- **Trabajo estimado:** 24 horas
- **Tipo:** Historia de valor (la protagoniza un profesor)
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** profesor **autorizado** de un curso.
- **Quiero:** crear mi colección de referencia y cargarle ejemplos ya corregidos
  por mí.
- **Para:** armar la referencia humana con la que después se calibra la
  corrección automática de **mi** curso.

## Notas / Observaciones

- **Reglas de trabajo:** crear la colección y cargar ejemplos se hace siempre
  por la recepción central. El servicio chequea tres cosas antes de aceptar:
  que el profesor **sea quién dice ser y tenga permiso**, que la colección
  **sea de su curso** y no del de otro, y que **no esté cargando lo mismo dos
  veces**. Cada carga queda anotada en el registro firmado.
- **Cómo se controla:** la conversación del alumno se guarda como **texto de
  dato**, nunca se "obedece" como si fueran órdenes para la máquina. Los puntajes
  de referencia tienen que traer exactamente los cinco criterios, con valores
  enteros de 0 a 100. El idioma es español. La versión de la plantilla tiene que
  existir.
- **Qué tiene que incluir sí o sí:** para crear la colección, la versión de la
  plantilla y el idioma; para cada ejemplo, la conversación del alumno y los
  cinco puntajes (autonomía, claridad, progresión, cumplimiento, eficiencia).
- **Tiempos:** operaciones rápidas; sin llamadas a servicios de inteligencia
  artificial.
- **Seguridad:** un profesor sin el permiso necesario, o que no fue verificado
  por la recepción central, o que no es dueño del curso → "prohibido". Nunca se
  entra sin pasar por la recepción central.
- **Accesibilidad:** no aplica en esta historia (la pantalla es la tarea 7).
- **Otros:** al crear algo, el servicio devuelve su identificador y la dirección
  donde queda disponible.
- **Operaciones nuevas:** "crear colección de referencia" y "cargar un ejemplo
  en una colección".

## Criterios de Aceptación (CA)

- **CA1:** un profesor autorizado de su curso crea una colección y le agrega un
  ejemplo válido; el servicio confirma las dos cosas y devuelve su dirección.
- **CA2:** repetir el mismo pedido con la misma "marca antiduplicado" devuelve
  el mismo resultado sin crear un segundo registro.
- **CA3 (caso que debe fallar):** un profesor que no es dueño del curso recibe
  "prohibido" y no se crea nada.
- **CA4 (caso que debe fallar):** un ejemplo con puntajes incompletos (cuatro
  criterios) o fuera de rango recibe un error que explica qué falta.
- **CA5 (caso que debe fallar):** un pedido que intenta saltearse la recepción
  central → "prohibido".

## BDD

**Qué se prueba:** el profesor construyendo su colección de referencia.

### Escenario 1 — Creación y carga por el profesor dueño (camino esperado)

- **Dado:** un profesor autorizado sobre su curso, verificado por la recepción
  central.
- **Cuando:** crea una colección y luego carga un ejemplo con la conversación y
  los cinco puntajes de referencia.
- **Entonces:** el servicio confirma ambas cosas y devuelve el identificador y la
  dirección de cada una.
- **Y:** la colección queda visible al consultarla (ver [H06](#llm-s01-h06--consultar-la-colección-aunque-el-sistema-se-reinicie)).

### Escenario 2 — Segunda carga con la misma "marca antiduplicado"

- **Dado:** un ejemplo ya cargado con una "marca antiduplicado" determinada.
- **Cuando:** el profesor reenvía exactamente el mismo pedido con la misma marca.
- **Entonces:** el servicio devuelve la misma respuesta que la primera vez y no
  crea un segundo ejemplo.

### Escenario 3 — Profesor de otro curso

- **Dado:** un profesor verificado que no es dueño del curso de la colección.
- **Cuando:** intenta crear una colección o cargar un ejemplo en ese curso.
- **Entonces:** el servicio responde "prohibido" y no guarda nada.

### Escenario 4 — Puntaje de referencia incompleto

- **Dado:** el formulario de carga de un ejemplo.
- **Cuando:** el profesor manda los puntajes con solo cuatro criterios.
- **Entonces:** el servicio responde con un error que indica qué falta, y no
  crea el ejemplo.

## Prototipo

- **Capturas / bocetos:** [bocetos simples](../prototipos/wireframes-h07-golden-set.md)
  de "Paso 1: crear la colección" y "Paso 2: cargar un ejemplo"; y una
  [demo que ya funciona](../../Demos/Golden%20Set/README.md).
- **Maqueta / documentación:** el [agregado de este tramo al contrato](../contracts/llm-service-v1-s1-golden-set-adenda.md).

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0, comparando contra la
  historia patrón (H06).
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** la recepción central (verificación de la persona),
  el servicio de administración (contrato) y el servicio de cursos (para saber de
  quién es cada curso).
- **Otros equipos / aprobaciones:** el servicio de administración tiene que
  "congelar" el contrato antes de publicarlo; definición docente de los cinco
  criterios.
- **Impacto en los datos:** usa el archivador de la tarea 4; escribe en las
  tablas de colecciones, ejemplos, "marcas antiduplicado" y registro firmado.
- **Riesgos:** saber de quién es cada curso depende del servicio de cursos; si no
  está listo, se acuerda un criterio provisional documentado.

---

# LLM-S01-H06 — Consultar la colección aunque el sistema se reinicie

- **Grupo de trabajo:** Colección de referencia y calibración humana (EP-03)
- **Pareja a cargo:** P5
- **Depende de:** H04
- **Trabajo estimado:** 14 horas
- **Tipo:** Historia de valor · **historia patrón** (la vara para medir las demás)
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** profesor autorizado de un curso.
- **Quiero:** consultar mi colección y sus ejemplos **aunque el sistema se haya
  reiniciado**.
- **Para:** confiar en que lo que cargué no se pierde y puedo seguir trabajando
  sobre eso.

## Notas / Observaciones

- **Reglas de trabajo:** la consulta se hace por la recepción central, con el
  mismo permiso que la carga. El listado viene **de a páginas** y ordenado del
  más nuevo al más viejo. El detalle incluye los ejemplos de la colección. La
  colección de **otro** curso **no aparece** nunca.
- **Cómo se controla:** pedir una colección que no existe devuelve "no
  encontrado" (no un error de servidor). Pedir una página con un tamaño imposible
  devuelve "pedido inválido".
- **Qué tiene que incluir sí o sí:** nada en el cuerpo (es una consulta); como
  opciones, la versión de la plantilla, el número de página y el tamaño de
  página.
- **Tiempos:** los datos se leen del "cajón" persistente de la base; después de
  apagar y prender, la lectura devuelve exactamente lo que se había cargado.
- **Seguridad:** sin el permiso necesario y sin identificación de la persona →
  "prohibido". El filtro por curso se aplica siempre, no es opcional.
- **Accesibilidad:** no aplica en esta historia (la pantalla es la tarea 7).
- **Otros:** esta es la **historia patrón** de la lista de trabajo pendiente: es chiquita, la
  entienden todos y tiene el recorrido completo (permiso + lectura + que los
  datos sobrevivan). Su tamaño en puntos se fija primero en el Sprint 0 y queda
  como referencia de todas las demás.
- **Operaciones nuevas:** "listar mis colecciones" y "ver el detalle de una
  colección con sus ejemplos".

## Criterios de Aceptación (CA)

- **CA1:** una colección creada y con un ejemplo se puede consultar y devuelve
  ese ejemplo.
- **CA2:** después de apagar y volver a prender, la misma consulta devuelve **lo
  mismo**.
- **CA3:** el listado respeta el número y el tamaño de página, y el orden del más
  nuevo al más viejo.
- **CA4 (caso que debe fallar):** la colección de otro curso no aparece en el
  listado y pedir su detalle responde "no encontrado".
- **CA5 (caso que debe fallar):** pedir una colección con un identificador bien
  formado pero inexistente → "no encontrado", nunca un error de servidor.
- **CA6 (caso que debe fallar):** pedir una página de tamaño 500 → "pedido
  inválido".

## BDD

**Qué se prueba:** la lectura de la colección con datos que sobreviven.

### Escenario 1 — La consulta sobrevive al reinicio (camino esperado)

- **Dado:** un profesor que creó una colección y le cargó un ejemplo.
- **Cuando:** se apaga y se vuelve a prender el servicio y su base.
- **Y:** el profesor vuelve a consultar la colección por la recepción central.
- **Entonces:** la respuesta contiene la colección y el ejemplo tal como se
  cargaron.

### Escenario 2 — Cada profesor ve solo lo suyo

- **Dado:** dos colecciones, una del curso del profesor y otra de un curso ajeno.
- **Cuando:** el profesor pide el listado.
- **Entonces:** solo aparece la colección de su curso.
- **Y:** pedir el detalle de la ajena responde "no encontrado".

### Escenario 3 — Colección inexistente

- **Dado:** un identificador bien formado que no corresponde a ninguna colección.
- **Cuando:** el profesor consulta su detalle.
- **Entonces:** el servicio responde "no encontrado", no un error de servidor.

### Escenario 4 — Página de tamaño fuera de rango

- **Dado:** el listado, cuyo tamaño de página va de 1 a 100.
- **Cuando:** se pide una página de tamaño 500.
- **Entonces:** el servicio responde "pedido inválido" y no devuelve datos.

## Prototipo

- **Capturas:** boceto del listado de colecciones (versión, plantilla, idioma,
  fecha) y del detalle con los ejemplos y sus cinco puntajes.
- **Maqueta / documentación:** el [agregado de este tramo al contrato](../contracts/llm-service-v1-s1-golden-set-adenda.md).

## Estimación / Prioridad

- **Puntos de esfuerzo:** es la **historia patrón**, se estima primera en el
  Sprint 0.
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** la recepción central, la base de datos y el servicio
  de cursos (para el filtro por curso).
- **Otros equipos / aprobaciones:** el contrato de lectura acordado con el
  servicio de administración.
- **Impacto en los datos:** solo lectura; exige que el "cajón" de la base de la
  tarea 2 sobreviva a los reinicios.
- **Riesgos:** si el "cajón" no sobrevive, la demo falla; se verifica con la
  prueba de reinicio de la tarea 9.

---

# LLM-S01-H07 — La pantalla sencilla para el profesor

- **Grupo de trabajo:** Colección de referencia y calibración humana (EP-03)
- **Pareja a cargo:** P5
- **Depende de:** H05, H06
- **Trabajo estimado:** 24 horas
- **Tipo:** Historia de valor (la protagoniza un profesor)
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** profesor.
- **Quiero:** una pantalla mínima para crear la colección, cargar ejemplos y
  consultarlos.
- **Para:** trabajar la referencia de mi curso sin depender de herramientas
  técnicas ni de pedirle a un programador que me lo haga.

## Notas / Observaciones

- **Reglas de trabajo:** la pantalla (una página web sencilla) hace crear,
  cargar y consultar **siempre a través de la recepción central**, nunca directo
  al servicio. Muestra **quién está operando** (con qué permiso).
- **Cómo se controla:** la conversación del alumno se revisa antes de enviarla,
  con el aviso visible de que **no se interpreta como órdenes** para la máquina;
  los cinco puntajes aceptan valores de 0 a 100.
- **Qué tiene que incluir sí o sí:** la conversación del alumno y los cinco
  puntajes de referencia para cargar un ejemplo.
- **Tiempos:** carteles de "cargando" mientras la pantalla espera respuesta.
- **Seguridad:** si el servicio responde "prohibido", la pantalla lo muestra de
  forma explícita, no como un error genérico.
- **Accesibilidad:** pensada para una persona con dificultades visuales o que
  navega solo con el teclado: cada campo con su etiqueta, el grupo de puntajes
  agrupado y titulado, los mensajes de error anunciados por el lector de
  pantalla, el foco bien manejado.
- **Otros:** si el servicio no está disponible, la pantalla muestra "no se pudo
  contactar al servidor", sin romperse ni quedar en blanco.

## Criterios de Aceptación (CA)

- **CA1:** desde la pantalla, el profesor crea una colección, carga un ejemplo y
  lo ve en el detalle, sin usar herramientas técnicas.
- **CA2:** todas las llamadas salen hacia la recepción central, no al servicio
  directo.
- **CA3:** hay carteles visibles de "cargando" y de "error" en cada paso.
- **CA4 (caso que debe fallar):** con el servicio caído, la pantalla muestra un
  aviso claro y se puede seguir navegando.
- **CA5 (caso que debe fallar):** una conversación mal escrita muestra un
  mensaje de aviso y **no** se envía.
- **CA6 (caso que debe fallar):** una respuesta "prohibido" del servicio se
  refleja como "no autorizado" en la pantalla.

## BDD

**Qué se prueba:** la pantalla mínima del profesor.

### Escenario 1 — Recorrido completo desde la pantalla (camino esperado)

- **Dado:** un profesor autorizado con la pantalla abierta y el servicio
  disponible.
- **Cuando:** crea una colección, carga un ejemplo con conversación y puntajes, y
  abre el detalle.
- **Entonces:** el ejemplo aparece listado con sus cinco puntajes y sus pesos.

### Escenario 2 — Servicio no disponible

- **Dado:** el servicio detenido.
- **Cuando:** el profesor abre el listado de colecciones.
- **Entonces:** la pantalla muestra "no se pudo contactar al servidor" y no
  queda en blanco ni con un error sin contexto.

### Escenario 3 — Conversación mal escrita

- **Dado:** el formulario de carga de un ejemplo.
- **Cuando:** el profesor pega un texto con formato inválido y confirma.
- **Entonces:** la pantalla muestra un mensaje de aviso y no envía el pedido.

### Escenario 4 — Operación no autorizada

- **Dado:** un profesor cuyo servicio responde "prohibido" a la operación.
- **Cuando:** intenta crear una colección.
- **Entonces:** la pantalla muestra el estado "no autorizado" con el detalle
  recibido.

## Prototipo

- **Capturas / bocetos:** [bocetos simples](../prototipos/wireframes-h07-golden-set.md)
  de las tres vistas (el listado, el alta y el detalle con carga); y una
  [demo interactiva](../../Demos/Golden%20Set/README.md).
- **Maqueta / documentación:** usa las operaciones de H05 y H06 a través de la
  recepción central.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0.
- **Prioridad:** imprescindible (la demo del tramo la necesita).

## Dependencias / Impactos

- **Partes involucradas:** la recepción central y el servicio (tareas 5 y 6).
- **Otros equipos / aprobaciones:** el equipo de diseño de la plataforma, para
  el encuadre visual y el ingreso de usuarios.
- **Impacto en los datos:** ninguno.
- **Riesgos:** si el marco visual compartido no está definido, se entrega como
  página web autónoma que apunta a la recepción central y se integra después.

---

# LLM-S01-H08 — Publicar el acuerdo y una maqueta para que otro equipo avance

- **Grupo de trabajo:** Plataforma, contratos e integración (EP-01)
- **Pareja a cargo:** P1
- **Depende de:** H03
- **Trabajo estimado:** 10 horas
- **Tipo:** Tarea interna
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** el equipo de integración y otro servicio (el de administración) que
  va a usar este.
- **Quiero:** que se publiquen el **"contrato"** (el documento que dice
  exactamente cómo se piden y cómo se devuelven las cosas) y una **maqueta** que
  responde como si fuera el servicio real.
- **Para:** que el otro equipo avance su parte **sin tener que esperar** a que
  nuestro servicio esté terminado.

## Notas / Observaciones

- **Reglas de trabajo:** el contrato, guardado junto con el código del proyecto, describe **solo**
  las operaciones que existen. El agregado de este tramo se fusiona al contrato
  cuando el otro equipo aprueba el cambio.
- **Cómo se controla:** el contrato es válido contra su propio formato; la
  maqueta responde con contenidos que respetan el contrato.
- **Qué tiene que incluir sí o sí:** el archivo del contrato, el agregado de
  este tramo, el comando de una línea para levantar la maqueta y dónde está todo
  documentado.
- **Tiempos:** no aplica.
- **Seguridad:** la maqueta no expone datos reales.
- **Accesibilidad:** no aplica.
- **Otros:** cualquier campo que hoy no exista se **acuerda y "congela"** con el
  otro equipo antes de publicarlo; no se agrega al contrato "por las dudas".

## Criterios de Aceptación (CA)

- **CA1:** el contrato publicado es válido y describe únicamente las operaciones
  hechas (crear colección, cargar ejemplo, listar, ver detalle).
- **CA2:** la maqueta se levanta con un comando documentado y responde según el
  contrato.
- **CA3:** el agregado de este tramo está revisado con el otro equipo y
  registrado.
- **CA4 (caso que debe fallar):** una propuesta de cambio que agrega al contrato
  una operación que no existe se rechaza.
- **CA5 (caso que debe fallar):** un campo nuevo sin acuerdo con el otro equipo
  frena la publicación.

## BDD

**Qué se prueba:** el contrato y la maqueta para quienes van a usar el servicio.

### Escenario 1 — El otro equipo trabaja contra la maqueta

- **Dado:** el contrato y la maqueta publicados.
- **Cuando:** el otro equipo levanta la maqueta con el comando documentado y
  prueba su integración.
- **Entonces:** la maqueta responde según el contrato y el otro equipo avanza
  sin el servicio real.

### Escenario 2 — Operación inexistente en el contrato

- **Dado:** el acuerdo de publicar solo operaciones hechas.
- **Cuando:** una propuesta de cambio agrega al contrato una operación que el
  servicio no ofrece.
- **Entonces:** la revisión rechaza la propuesta.

### Escenario 3 — Campo no acordado

- **Dado:** un campo de respuesta que hoy no existe en el servicio.
- **Cuando:** alguien intenta incluirlo en el contrato publicado.
- **Entonces:** la publicación se frena hasta acordar el campo con el otro
  equipo.

## Prototipo

- **Capturas:** no aplica.
- **Maqueta / documentación:** el [contrato base](../contracts/llm-service-v1.openapi.yaml)
  y el agregado de este tramo.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0.
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** el servicio de administración (que va a usar el
  contrato).
- **Otros equipos / aprobaciones:** el servicio de administración aprueba el
  agregado y los campos.
- **Impacto en los datos:** ninguno.
- **Riesgos:** que el contrato y lo que hace el servicio se desalineen; se cubre
  con pruebas en la tarea 9.

---

# LLM-S01-H09 — Las pruebas automáticas y la guía para la demostración

- **Grupo de trabajo:** Plataforma, contratos e integración (EP-01)
- **Pareja a cargo:** Todas (una persona por pareja)
- **Depende de:** H04, H05, H06, H07
- **Trabajo estimado:** 18 horas
- **Tipo:** Tarea interna
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** equipo de desarrollo.
- **Quiero:** un paquete de pruebas automáticas y una guía paso a paso para la
  demostración.
- **Para:** validar el trabajo del tramo **con evidencia que cualquiera puede
  reproducir**, no con un "confíen en mí, funciona".

## Notas / Observaciones

- **Reglas de trabajo:** el paquete de pruebas cubre las reglas del negocio, que
  los datos se guarden y se recuperen bien, que el acuerdo con la recepción
  central se respete, y —clave— la **prueba de apagar y prender** para confirmar
  que nada se pierde. El porcentaje de código probado llega al mínimo que exige
  el equipo (95 % en la parte de servidor y en la de pantalla).
- **Cómo se controla:** los **escenarios de aceptación** de las tareas 5, 6 y 7
  (camino esperado y casos que deben fallar) quedan ejecutados con evidencia.
- **Qué tiene que incluir sí o sí:** la guía de demo paso a paso: entrar con
  permiso → crear la colección → cargar un ejemplo → consultarlo después del
  reinicio.
- **Tiempos:** el paquete de pruebas corre en el control automático en un tiempo
  acotado.
- **Seguridad:** se prueban los casos de "no autorizado" / "prohibido" y de
  "marca antiduplicado".
- **Accesibilidad:** la pantalla incluye pruebas propias; las verificaciones de
  accesibilidad de la tarea 7 se listan en la guía.
- **Otros:** la guía de demo la puede ejecutar cualquier integrante en un
  ambiente compartido, no solo en una computadora aislada.

## Criterios de Aceptación (CA)

- **CA1:** el paquete completo de pruebas corre "en verde" en el control
  automático.
- **CA2:** el porcentaje de código probado, en servidor y en pantalla, alcanza
  el mínimo exigido.
- **CA3:** existe una prueba automática que apaga y prende el sistema y verifica
  que la colección **sigue ahí**.
- **CA4:** la guía de demo se puede seguir paso a paso y termina en la consulta
  después del reinicio.
- **CA5 (caso que debe fallar):** si el porcentaje de código probado baja del
  mínimo, el control automático **frena** el cambio.
- **CA6 (caso que debe fallar):** si la prueba de reinicio detecta que se
  perdieron datos, **falla** y no se puede dar por aprobado el tramo.

## BDD

**Qué se prueba:** evidencia reproducible del trabajo del tramo.

### Escenario 1 — El paquete valida el trabajo (camino esperado)

- **Dado:** el código del tramo integrado.
- **Cuando:** el control automático ejecuta el paquete completo.
- **Entonces:** todas las pruebas pasan y el porcentaje de código probado queda
  por encima del mínimo.

### Escenario 2 — Cobertura de pruebas insuficiente

- **Dado:** un cambio que agrega código sin pruebas.
- **Cuando:** el control automático mide el porcentaje de código probado.
- **Entonces:** el porcentaje cae bajo el mínimo y el control marca el cambio
  como fallido.

### Escenario 3 — Pérdida de datos en el reinicio

- **Dado:** la prueba automática de apagar y prender.
- **Cuando:** después del reinicio la colección cargada antes no está.
- **Entonces:** la prueba falla y el tramo no se puede dar por aprobado.

## Prototipo

- **Capturas:** no aplica.
- **Maqueta / documentación:** simuladores de la base de datos y de la recepción
  central para las pruebas.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0.
- **Prioridad:** imprescindible.

## Dependencias / Impactos

- **Partes involucradas:** el control automático, la base de datos y la recepción
  central (con simuladores para las pruebas).
- **Otros equipos / aprobaciones:** ninguna externa.
- **Impacto en los datos:** las pruebas usan bases temporales; no tocan datos
  reales.
- **Riesgos:** una demo local que no represente el ambiente compartido; se
  mitiga exigiendo que la guía corra en el ambiente compartido.

---

## En resumen

Al final de este tramo no hay inteligencia artificial todavía, pero sí hay:

- un acuerdo escrito de cómo trabaja el equipo,
- un sistema que se enciende con un comando,
- el esqueleto del servicio con su seguridad puesta,
- un archivador de datos que no se puede romper ni borrar por error,
- un profesor que crea su colección de referencia, le carga ejemplos y los
  vuelve a ver después de reiniciar,
- una pantalla sencilla para hacerlo,
- un contrato y una maqueta para que otro equipo avance en paralelo,
- y pruebas automáticas que demuestran que todo eso es verdad.
