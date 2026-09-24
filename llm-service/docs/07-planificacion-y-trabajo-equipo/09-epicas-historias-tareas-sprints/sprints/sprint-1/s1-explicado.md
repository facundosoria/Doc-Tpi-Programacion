# Sprint 1 — las historias explicadas en palabras simples

> **Qué es este documento.** Empezó siendo las nueve historias originales del Sprint 1,
> en formato técnico en [`../../historias/ep-01/`](../../historias/ep-01/README.md) y
> [`../../historias/ep-03/`](../../historias/ep-03/README.md), con la **misma estructura del
> template de Taiga** (Como / Quiero / Para, Notas, Criterios de Aceptación, escenarios
> BDD, Prototipo, Estimación y Dependencias), pero contadas **sin jerga técnica**: como
> si se las explicara a alguien que nunca programó. Mismo contenido, mismo objetivo de
> la demo. Cambia solo el idioma.
>
> **Realineado el 2026-09-13 (noche).** El Sprint 1 creció de 9/10 historias a 30 (dos
> hilos nuevos, EP-07 y la extensión de EP-09, más lo que ya faltaba de EP-05, EP-09 base,
> EP-04, EP-03 y EP-06). Este documento se amplió para cubrir esas 20 historias nuevas —
> ver la sección **"Las historias que se agregaron en la recalibración del 2026-09-13"**
> después de H10. Están un poco más resumidas que las diez originales, mismo idioma sin
> jerga.
>
> **Qué NO es.** No es fuente de verdad para planificar. Si un número, una fecha
> o una responsabilidad no coincide, vale lo que dice:
>
> | Dato | Dónde manda |
> |---|---|
> | ID, grupo, pareja, dependencias, horas | [`35`](../../../04-backlog-ejecutable.md) · «S1» |
> | Ficha técnica completa (con los códigos y términos exactos) | [`../../historias/`](../../historias/README.md) |
> | Índice del sprint, tipo de cada historia y demo | [`s1-historias.md`](s1-historias.md) |
> | Desglose en tareas | [`../../tareas/`](../../tareas/README.md) |
> | Requisitos para empezar una historia y para darla por terminada | [23 · §9.2](../../../03-plan-de-construccion-del-producto.md) |
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

**En este primer tramo de trabajo (el "Sprint 1") todavía NO se corrige nada con
inteligencia artificial.** Se construyen los cimientos: ponerse de acuerdo,
lograr que el sistema encienda con un comando, armar el esqueleto del servicio,
preparar la base de datos, y darle a un profesor la posibilidad de **crear** su
colección de referencia, **cargarle** ejemplos y **volver a consultarlos**, con
una pantalla sencilla para hacerlo. En paralelo, y sin que dependa de nada de lo
anterior, arranca la pieza que más adelante va a "hablar" con un modelo de
lenguaje — pero en este tramo todavía habla solo con un **doble simulado**, no
con un modelo real (H10).

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
| [H10](#llm-s01-h10--el-camino-para-hablar-con-un-modelo-de-lenguaje-todavía-simulado) | El camino para hablar con un modelo de lenguaje, todavía simulado | Tarea interna | EP-02 | P2 | H01 | 32 h |
| | | | | | **Total** | **240 h** |

> **H10 se suma en la reprogramación a 8 semanas** (ver [`README.md`](README.md)): se
> adelanta desde un tramo posterior porque no depende de nada de lo que hace el profesor,
> solo del acuerdo técnico (H01). No cambia la demo de este tramo, que sigue siendo el
> golden set.

**Tipo:** "historia de valor" quiere decir que la protagoniza una persona de
carne y hueso (un profesor) que nota el beneficio. "Tarea interna" es un cimiento
del que depende el resto, pero que ningún usuario final "vive" directamente.

**Tareas:** cada historia se parte en **tareas**: los pasos concretos en los que el
equipo de desarrollo divide el trabajo. No se escriben en formato "Como / Quiero /
Para" (eso es para las historias) y cada una está pensada para durar **un día de
trabajo o menos**. Las tareas de las nueve historias, con su método (SMART) y sus
horas, están en [`../../tareas/`](../../tareas/README.md); el detalle técnico de cada
historia, en [`../../historias/`](../../historias/README.md).

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
  [documento base del contrato](../../../../contracts/llm-service.openapi.yaml).

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

- **Capturas / bocetos:** [bocetos simples](../../../../08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/prototipos/wireframes-h07-golden-set.md)
  de "Paso 1: crear la colección" y "Paso 2: cargar un ejemplo"; y una
  [demo que ya funciona](../../../../08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/prototipos/golden-set-calibration-demo.html).
- **Maqueta / documentación:** el agregado de este tramo al contrato (adenda S1, histórica — retirada de `docs/contracts/`).

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
- **Maqueta / documentación:** el agregado de este tramo al contrato (adenda S1, histórica — retirada de `docs/contracts/`).

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

- **Capturas / bocetos:** [bocetos simples](../../../../08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/prototipos/wireframes-h07-golden-set.md)
  de las tres vistas (el listado, el alta y el detalle con carga); y una
  [demo interactiva](../../../../08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/prototipos/golden-set-calibration-demo.html).
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
- **Maqueta / documentación:** el [contrato base](../../../../contracts/llm-service.openapi.yaml)
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

# LLM-S01-H10 — El camino para hablar con un modelo de lenguaje, todavía simulado

- **Grupo de trabajo:** IA, modelos y resiliencia (EP-02)
- **Pareja a cargo:** P2
- **Depende de:** H01 (el acuerdo de cómo se construye el programa)
- **Trabajo estimado:** 32 horas
- **Tipo:** Tarea interna (no la "vive" un usuario final)
- **Responsable del producto:** Se nombra en el Sprint 0

## Descripción (Como / Quiero / Para)

- **Como:** equipo que programa este servicio.
- **Quiero:** un único "camino" por el que pasa cualquier pedido a un modelo de
  inteligencia artificial, con un doble simulado para probarlo.
- **Para:** que ninguna función de IA quede "casada" con una empresa proveedora
  concreta, y que se pueda desarrollar y probar sin llamar a un modelo real ni
  gastar un centavo.

## Notas / Observaciones

- **Por qué está acá y no más adelante:** originalmente este trabajo estaba
  planeado para un tramo posterior, después de que el profesor terminara de
  puntuar su colección de referencia. Pero no necesita nada de eso: solo
  necesita el acuerdo técnico de la tarea 1. Por eso se adelanta y una pareja
  distinta (la que se ocupa de los modelos) puede avanzar **al mismo tiempo**
  que la que arma el golden set, en vez de esperar su turno.
- **Reglas de trabajo:** el "camino" es una pieza de código que no sabe nada de
  ninguna marca de inteligencia artificial en particular. Detrás de ese camino
  hay, por ahora, solo un **doble simulado**: no llama a ningún modelo real,
  devuelve respuestas de prueba. Existe también una lista de "qué función usa
  qué modelo", que arranca con un solo renglón.
- **Cómo se controla:** lo que devuelve el doble simulado se revisa contra un
  formato estricto antes de aceptarse; si no cumple el formato, se rechaza en
  vez de "arreglarse" a mano. Si tarda demasiado, se corta con un aviso, sin
  trabar el resto del programa.
- **Tiempos / volumen:** no aplica llamadas reales todavía.
- **Seguridad:** en este tramo no hay ninguna clave de un proveedor real
  guardada en ningún lado, porque no se llama a ninguno de verdad.
- **Accesibilidad:** no aplica (es una pieza interna, sin pantalla).
- **Otros:** esto **no forma parte de la demo** de este tramo. La demo sigue
  siendo la del profesor con su colección de referencia.

## Criterios de Aceptación (CA)

- **CA1:** existe el "camino" único y no tiene, en su parte más interna,
  ninguna referencia a una marca de inteligencia artificial concreta.
- **CA2:** el doble simulado responde a través de ese camino con datos de
  prueba, sin llamar a nada real.
- **CA3:** existe la lista "función → proveedor y modelo", con al menos un
  renglón, y cambiarla no exige tocar ni recompilar el código.
- **CA4 (caso que debe fallar):** una respuesta que no cumple el formato
  esperado se rechaza con un aviso claro, no se deja pasar como si estuviera
  bien.
- **CA5 (caso que debe fallar):** una respuesta que tarda más de lo permitido
  se corta con un aviso, sin dejar a nadie esperando para siempre.

## BDD

**Qué se prueba:** que cualquier función pueda "hablar" con un modelo sin
saber con cuál habla en realidad.

### Escenario 1 — El camino funciona con el doble simulado

- **Dado:** el camino configurado con el doble simulado.
- **Cuando:** una función pide una respuesta a través del camino.
- **Entonces:** el doble simulado responde con datos de prueba que cumplen el
  formato esperado.

### Escenario 2 — Respuesta con formato incorrecto

- **Dado:** el doble simulado configurado para responder algo que no cumple el
  formato.
- **Cuando:** una función pide una respuesta.
- **Entonces:** el camino la rechaza y avisa del error, sin dejarla pasar.

### Escenario 3 — Demora excesiva

- **Dado:** el doble simulado tardando más de lo permitido.
- **Cuando:** una función pide una respuesta.
- **Entonces:** el pedido se corta con un aviso al cumplirse el tiempo límite.

## Prototipo

- **Capturas:** no aplica (es una pieza interna, sin pantalla).

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en el Sprint 0.
- **Prioridad:** conviene hacerlo, pero no bloquea la demo de este tramo.

## Dependencias / Impactos

- **Partes involucradas:** ninguna externa real todavía (solo el doble
  simulado).
- **Otros equipos / aprobaciones:** ninguna en este tramo. Quién provee el
  modelo real, y con qué credenciales, se decide antes del tramo que sí llama
  a un modelo de verdad.
- **Impacto en los datos:** crea la lista "función → proveedor y modelo".
- **Riesgos:** ninguno propio de este tramo — al no llamar a un proveedor
  real, no depende de ninguna decisión legal pendiente sobre el uso de datos
  de alumnos.

---

# Las historias que se agregaron en la recalibración del 2026-09-13

> Todo lo de arriba (H01–H10) es el Sprint 1 **original**. Las historias que siguen se
> agregaron después, en dos momentos: la "reprogramación a 8 semanas" (que ya traía H10) y,
> sobre todo, la **recalibración del 2026-09-13 (noche)**, que expandió el Sprint 1 a un
> arranque de 5 hilos en paralelo y después le sumó dos hilos más (F y G). Mismo formato,
> mismo idioma sin jerga — un poco más resumidas que las diez de arriba para no repetir
> párrafos enteros de las fichas técnicas.

---

# LLM-EP02-H02 — Conectar con un proveedor de inteligencia artificial de verdad

- **Grupo de trabajo:** AI Gateway (EP-02) · **Pareja:** P2 · **Depende de:** H10 (el camino,
  todavía con el doble simulado) · **Trabajo estimado:** 20 horas · **Tipo:** Tarea interna

## Descripción (Como / Quiero / Para)

- **Como:** la plataforma.
- **Quiero:** que el mismo camino que hoy habla con el doble simulado (H10) empiece a hablar,
  cuando se lo configure así, con un proveedor de inteligencia artificial real.
- **Para:** que cualquier función que ya usa ese camino (el tutor, la calibración, el que
  corrige, el que busca en los PDF) deje de simular y empiece a razonar de verdad, sin que
  nadie tenga que tocarle el código.

## Notas / Observaciones

- Es literalmente el **mismo camino** de H10 — no se construye uno nuevo. Se agrega una
  segunda "puerta" al otro lado: en vez del doble simulado, un proveedor real. Cuál puerta usa
  cada función sigue decidiéndose en la misma lista de H10, cambiando un renglón, sin tocar
  código ni volver a instalar nada.
- La clave para hablar con el proveedor real nunca se guarda en el código ni se sube al
  archivo compartido del proyecto — vive aparte, como ya lo exige el acuerdo de H01.
- Si el proveedor responde algo que no tiene la forma esperada, se rechaza igual que hoy se
  rechaza una respuesta mal formada del doble simulado — no se relaja ningún control por ser
  "de verdad".
- Antes de dar esto por terminado, se corren **todas** las pruebas que ya existen de todo lo
  que usa este camino, contra el proveedor real al menos una vez — hoy todas corrieron
  siempre contra el doble simulado.
- **Es el único hueco propio que falta** de los seis puntos obligatorios de la cátedra — todo
  lo demás obligatorio, o ya está hecho, o depende de otro equipo.

## Criterios de Aceptación (CA)

- **CA1:** con la clave configurada, una función asignada al proveedor real responde de
  verdad, no con datos simulados.
- **CA2:** cambiar una función del doble simulado al proveedor real (o al revés) no exige
  tocar código ni reinstalar nada.
- **CA3 (caso que debe fallar):** una respuesta del proveedor real con forma incorrecta se
  rechaza, igual que con el simulado.
- **CA4 (caso que debe fallar):** si falta la clave para una función que la necesita, el
  sistema avisa claro al intentar usarla — nunca se queda en silencio.

## BDD

### Escenario 1 — Responde de verdad (camino feliz)

- **Dado:** una función configurada para el proveedor real, con la clave puesta.
- **Cuando:** se le pide una respuesta.
- **Entonces:** la respuesta viene generada de verdad, con la forma esperada.

### Escenario 2 — Falta la clave

- **Dado:** una función asignada al proveedor real sin la clave configurada.
- **Cuando:** se intenta usarla.
- **Entonces:** el sistema avisa con claridad cuál es el problema, en vez de fallar en
  silencio o devolver algo vacío.

## Prototipo

No aplica — pieza interna sin pantalla propia.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento contra la historia canónica.
- **Prioridad:** imprescindible — desbloquea el único punto obligatorio de la cátedra que
  todavía falta.

## Dependencias / Impactos

- **Partes involucradas:** el mismo camino y la misma lista de H10.
- **Otros equipos:** ninguno — es interno.
- **Riesgos:** un proveedor real se comporta distinto del simulado en casos raros (lentitud,
  límites propios de tráfico, errores con formato propio) — por eso se corren todas las
  pruebas viejas contra él antes de cerrar esto.

---

# LLM-EP05-H03 — Retomar la conversación con el tutor sin perder el hilo

- **Grupo de trabajo:** Tutor y RAG (EP-05) · **Pareja:** P3 · **Depende de:** las dos
  historias del tutor que ya construyeron la conversación básica · **Trabajo estimado:**
  ~20–25 horas · **Tipo:** Historia de valor (la vive un alumno)

## Descripción (Como / Quiero / Para)

- **Como:** alumno en medio de un desafío, ya con mensajes intercambiados con el tutor.
- **Quiero:** poder cerrar la pantalla y, al volver, encontrar mi conversación exactamente
  donde la dejé, con todos los mensajes en orden, y seguir desde ahí.
- **Para:** no tener que volver a explicarle todo al tutor cada vez que vuelvo.

## Notas / Observaciones

- Cada conversación es de **un solo alumno**, dentro de **un solo desafío**. Nadie puede ver
  ni escribir en la conversación de otro alumno, aunque conozca su identificador exacto.
- Solo se puede seguir conversando mientras el desafío siga abierto. Si ya cerró, o no existe,
  el sistema responde "no encontrado" — sin decir si estaba cerrado o si nunca existió, para
  no dar pistas de más.
- Intentar ver la conversación de otro alumno responde "prohibido", sin mostrar nada de su
  contenido.

## Criterios de Aceptación (CA)

- **CA1:** un alumno puede pedir el historial completo de su propia conversación, ordenado
  del mensaje más viejo al más nuevo.
- **CA2:** puede seguir escribiendo mientras el desafío esté abierto.
- **CA3 (caso que debe fallar):** pedir la conversación de otro alumno responde "prohibido",
  sin mostrar contenido.
- **CA4 (caso que debe fallar):** escribir en una conversación de un desafío ya cerrado
  responde "no encontrado", sin agregar ningún mensaje nuevo.

## BDD

### Escenario 1 — Retomar la conversación (camino feliz)

- **Dado:** un alumno con tres mensajes ya intercambiados en un desafío abierto.
- **Cuando:** pide el historial y después escribe un cuarto mensaje.
- **Entonces:** ve los tres mensajes en orden y el cuarto queda agregado al final.

### Escenario 2 — Desafío ya cerrado (caso que debe fallar)

- **Dado:** una conversación de un desafío que ya cerró.
- **Cuando:** el alumno intenta escribir un mensaje nuevo.
- **Entonces:** el sistema responde "no encontrado" y no agrega nada.

## Prototipo

Una pantalla de chat simple: los mensajes propios a un lado, los del tutor al otro, con la
hora de cada uno, y un cartel claro si todavía no hay ningún mensaje o si la conversación ya
no está disponible.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento contra la historia canónica.
- **Prioridad:** deseable — mejora real, pero el mínimo obligatorio ya funciona sin esto.

## Dependencias / Impactos

- **Otros equipos:** el equipo de desafíos tiene que confirmar cómo se consulta si un desafío
  está abierto o cerrado — sin eso, no se puede rechazar bien un desafío ya cerrado.
- **Riesgos:** las pruebas de esta pieza contra una base de datos real todavía no corrieron
  (quedaron bloqueadas por un problema de entorno en una sesión anterior) — no se da por
  verificada del todo hasta que corran.

---

# LLM-EP09-H01 — Que un profesor suba un PDF y el tutor pueda usarlo como material

- **Grupo de trabajo:** Tutor y RAG (EP-09) · **Pareja:** P3 · **Trabajo estimado:** sin número
  propio — es parte de un paquete conjunto con la siguiente historia (H02), de referencia
  histórica ~208 h para el paquete completo · **Tipo:** Historia de valor (la vive un docente)

## Descripción (Como / Quiero / Para)

- **Como:** docente de un curso.
- **Quiero:** subir un PDF de material de estudio y que quede organizado automáticamente para
  que el tutor lo pueda usar.
- **Para:** que el tutor responda preguntas de los alumnos citando ese material, no
  inventando.

## Notas / Observaciones

- Cada material pertenece a un curso/cohorte puntual — nunca se mezcla con el material de
  otro curso.
- Solo se aceptan archivos PDF, hasta 25MB. El texto se corta en pedacitos manejables, y las
  imágenes con contenido real (no íconos ni decoraciones) se detectan aparte.
- Un archivo vacío, que no sea realmente un PDF, o cifrado, se rechaza con un aviso claro —no
  se indexa nada.
- Retirar un material lo saca de las búsquedas futuras, pero no borra su historial.

## Criterios de Aceptación (CA)

- **CA1:** un docente sube un PDF válido y recibe confirmación de que quedó organizado.
- **CA2:** el material queda asociado únicamente a su propio curso.
- **CA3 (caso que debe fallar):** un archivo inválido se rechaza sin indexar nada.
- **CA4:** retirar un material lo saca de las búsquedas sin borrar su historial.

## BDD

### Escenario 1 — Subida exitosa (camino feliz)

- **Dado:** un docente con un PDF de texto válido.
- **Cuando:** lo sube.
- **Entonces:** queda disponible para que el tutor lo use en sus respuestas.

### Escenario 2 — Archivo inválido (caso que debe fallar)

- **Dado:** un archivo que en realidad no es un PDF, aunque tenga esa extensión.
- **Cuando:** el docente intenta subirlo.
- **Entonces:** se rechaza con un mensaje claro, sin indexar nada.

## Prototipo

Una pantalla sencilla donde el docente arrastra o elige el PDF y ve la lista de materiales ya
subidos con su nombre y cantidad de páginas.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento — todavía no tiene número individual.
- **Prioridad:** deseable — se adelantó porque ya había una versión de referencia construida
  en otro lado, lista para portar.

## Dependencias / Impactos

- **Riesgos:** todavía no hay un proveedor real de "entendimiento del texto" conectado —
  se usa un doble simulado, igual que el modelo de lenguaje en H10.

---

# LLM-EP09-H02 — Preguntarle al tutor sobre el material subido, con la cita exacta

- **Grupo de trabajo:** Tutor y RAG (EP-09) · **Pareja:** P3 · **Depende de:** H01 (la subida
  del material) · **Trabajo estimado:** sin número propio, ver H01 · **Tipo:** Historia de
  valor (la vive un alumno)

## Descripción (Como / Quiero / Para)

- **Como:** alumno de un curso.
- **Quiero:** preguntarle al tutor sobre uno o varios materiales que elijo, y que me responda
  citando de qué documento y qué página salió la respuesta.
- **Para:** confiar en que la respuesta viene del material real, no de algo inventado.

## Notas / Observaciones

- Si no elijo ningún material, el tutor no inventa una respuesta: avisa que necesita al menos
  uno seleccionado, sin gastar ningún recurso en pensar una respuesta.
- Preguntas ofensivas o intentos de manipular al tutor se bloquean antes de que el tutor
  llegue a procesarlas.
- Preguntar lo mismo dos veces sobre el mismo material responde al instante, desde lo que ya
  se calculó la primera vez.

## Criterios de Aceptación (CA)

- **CA1:** una pregunta sobre un material que tiene la respuesta, devuelve una respuesta con
  al menos una cita de documento y página.
- **CA2 (caso que debe fallar):** sin ningún material elegido, el tutor avisa que hace falta
  elegir uno, sin inventar nada.
- **CA3:** una pregunta con lenguaje inapropiado o un intento de manipulación se bloquea antes
  de llegar al tutor.

## BDD

### Escenario 1 — Respuesta con cita (camino feliz)

- **Dado:** un alumno con un material elegido que contiene la respuesta.
- **Cuando:** pregunta algo cubierto por ese material.
- **Entonces:** recibe una respuesta que indica de qué documento y página salió.

### Escenario 2 — Sin material elegido (caso que debe fallar)

- **Dado:** un alumno sin ningún material marcado.
- **Cuando:** intenta preguntar algo.
- **Entonces:** el tutor avisa que necesita al menos un material elegido, sin responder nada
  inventado.

## Prototipo

Una pantalla de chat con una lista de materiales para tildar antes de preguntar, y cada
respuesta del tutor con un pequeño link "ver fuente" debajo.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento — todavía no tiene número individual.
- **Prioridad:** deseable — se adelantó junto con H01 por el mismo motivo.

## Dependencias / Impactos

- **Riesgos:** sin proveedor real de modelo ni de "entendimiento del texto" conectado todavía
  — no se puede juzgar la calidad real de las respuestas hasta que se conecten.

---

# LLM-EP04-H01 — Que un docente calibre su curso antes de dejar corregir a la IA

- **Grupo de trabajo:** Calibración (EP-04) · **Pareja:** P4 · **Depende de:** la rúbrica y el
  golden set del curso, y el camino de H10 · **Trabajo estimado:** 46 horas (32 del esqueleto
  + 14 de una tarea de conexión ya cerrada) · **Tipo:** Historia de valor (la vive un docente)

## Descripción (Como / Quiero / Para)

- **Como:** docente autorizado de un curso.
- **Quiero:** correr una calibración de mi rúbrica y mis casos de referencia contra un modelo,
  ver qué tan parecido puntúa a como puntuaría yo, y decidir explícitamente qué desafíos
  migran cuando activo esa calibración.
- **Para:** saber si el modelo corrige como yo antes de dejarlo evaluar entregas reales, sin
  arriesgar desafíos que un alumno ya está resolviendo con la calibración anterior.

## Notas / Observaciones

- Una calibración se encola, corre en segundo plano, y termina "aprobada" o "no aprobada"
  según dos condiciones a la vez: qué tan lejos puntúa en promedio (no más de 5 puntos de
  diferencia) y que ninguna dimensión individual se aleje más de 10 puntos.
- Activar una calibración aprobada no es automático: primero se muestra qué desafíos pueden
  migrar sin problema y cuáles están bloqueados (porque un alumno ya empezó con la versión
  vieja), y solo con esa lista confirmada se activa.
- Solo puede haber una calibración activa por curso a la vez.
- Solo el docente autorizado de ese curso puede tocar su calibración — cualquier otro recibe
  "prohibido".

## Criterios de Aceptación (CA)

- **CA1:** un docente encola una calibración con su rúbrica, sus casos de referencia y un
  modelo candidato.
- **CA2:** una calibración dentro de tolerancia queda "aprobada" y puede activarse.
- **CA3:** al activar, los desafíos con un alumno ya empezado quedan bloqueados, no migran
  automáticamente.
- **CA4 (caso que debe fallar):** un docente de otro curso no puede tocar esta calibración.

## BDD

### Escenario 1 — Calibración dentro de tolerancia (camino feliz)

- **Dado:** una calibración cuyos puntajes se parecen a los humanos dentro del margen
  permitido.
- **Cuando:** termina de correr.
- **Entonces:** queda "aprobada" y lista para activarse.

### Escenario 2 — Desafío con un alumno ya empezado

- **Dado:** una calibración aprobada y un desafío que un alumno ya empezó con la versión
  vieja.
- **Cuando:** el docente pide activar la nueva calibración.
- **Entonces:** ese desafío aparece como bloqueado y no se puede incluir en la activación.

## Prototipo

Una pantalla donde el docente elige rúbrica, casos de referencia y modelo, ve el resultado de
la calibración por cada criterio, y — si aprueba — una lista de desafíos para confirmar antes
de activar.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento.
- **Prioridad:** imprescindible — es la demo central de este tramo del proyecto.

## Dependencias / Impactos

- **Otros equipos:** el servicio de cursos tiene que empezar a consultar si un curso tiene
  calibración vigente antes de dejarlo activar — el lado de acá ya está listo, falta que el
  otro equipo lo use.
- **Riesgos:** hoy la calibración corre siempre contra el doble simulado del modelo — hay que
  aclararlo si se muestra en una demo, para no confundirlo con un resultado real.

---

# LLM-S04-H01 (LLM-EP04-H02) — Calibrar un modelo para toda la plataforma, no solo un curso

- **Grupo de trabajo:** Calibración (EP-04) · **Pareja:** P4 · **Trabajo estimado:** sin número
  propio todavía — la ficha pide estimarlo en Refinamiento, no inventarlo acá · **Tipo:**
  Historia de valor (la vive un administrador)

## Descripción (Como / Quiero / Para)

- **Como:** administrador de toda la plataforma (no un docente de un curso puntual).
- **Quiero:** calibrar un modelo candidato contra la referencia de **toda la plataforma**
  (no la de un curso), con un reporte explicado criterio por criterio.
- **Para:** decidir si habilito ese modelo para todos los cursos, antes de que ninguno pueda
  usarlo.

## Notas / Observaciones

- Habilitar un modelo a nivel plataforma y calibrar un curso puntual son cosas **distintas** —
  ninguna reemplaza a la otra.
- Reutiliza el mismo motor que ya calibra por curso (H01 de arriba) — no se construye de
  nuevo, solo se corre contra la referencia de plataforma.
- Solo un administrador puede tocar esto — ni siquiera un docente autorizado en su curso.
- El reporte muestra el resultado **de cada criterio por separado**, no solo un promedio.

## Criterios de Aceptación (CA)

- **CA1:** un administrador encola una calibración de plataforma con rúbrica, casos de
  referencia y modelo candidato.
- **CA2:** el reporte final muestra el resultado de cada criterio por separado.
- **CA3 (caso que debe fallar):** un modelo fuera de tolerancia no queda habilitado para
  ningún curso.
- **CA4 (caso que debe fallar):** un docente (no administrador) que intenta tocar esto recibe
  "prohibido".

## BDD

### Escenario 1 — Calibración de plataforma dentro de tolerancia (camino feliz)

- **Dado:** un administrador con rúbrica y casos de plataforma.
- **Cuando:** calibra un modelo candidato y sale dentro de tolerancia.
- **Entonces:** el modelo queda habilitado para toda la plataforma, con el reporte por
  criterio.

### Escenario 2 — Un docente intenta calibrar a nivel plataforma (caso que debe fallar)

- **Dado:** un docente autorizado solo en su propio curso.
- **Cuando:** intenta encolar o ver una calibración de plataforma.
- **Entonces:** el sistema responde "prohibido".

## Prototipo

La misma pantalla de calibración de curso, con un selector para elegir "Plataforma" en vez de
un curso puntual, visible solo para administradores.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento — no se inventa un número acá, la propia
  ficha técnica lo pide explícitamente.
- **Prioridad:** a discutir — cierra un hueco real, pero ningún curso depende de que esto
  exista para poder calibrarse hoy.

## Dependencias / Impactos

- **Riesgos:** confundir "modelo habilitado para toda la plataforma" con "modelo calibrado en
  un curso" es el error más fácil de cometer — son cosas distintas y no deberían mezclarse.

---

# LLM-S04-H02 (LLM-EP04-H03) — Que una calibración vieja deje de usarse sola

- **Grupo de trabajo:** Calibración (EP-04) · **Pareja:** P4 · **Trabajo estimado:** ~12–15
  horas · **Tipo:** Historia de valor (la vive un docente o administrador)

## Descripción (Como / Quiero / Para)

- **Como:** docente o administrador dueño de una calibración activa.
- **Quiero:** que mi calibración vigente venza sola cuando cambia la rúbrica, cambian los
  casos de referencia, o pasa demasiado tiempo sin volver a calibrar.
- **Para:** no confiar para siempre en un resultado que ya no representa la referencia actual.

## Notas / Observaciones

- Una calibración vencida no se borra — sigue consultable como referencia pasada, solo deja
  de habilitar evaluaciones nuevas.
- El motivo del vencimiento siempre queda registrado (rúbrica nueva, casos nuevos, o tiempo
  cumplido) — sin esto, nadie entiende de golpe por qué hay evaluaciones frenadas otra vez.
- El tiempo límite es configurable, no un número fijo en el código.

## Criterios de Aceptación (CA)

- **CA1:** publicar una rúbrica nueva vence la calibración activa que usaba la vieja.
- **CA2:** pasar el tiempo límite configurado sin recalibrar también la vence.
- **CA3 (caso que debe fallar):** una calibración vencida no habilita evaluaciones nuevas.
- **CA4 (caso que debe fallar):** vencer una calibración no borra su historial.

## BDD

### Escenario 1 — Vencimiento por rúbrica nueva (camino feliz)

- **Dado:** una calibración activa sobre la rúbrica versión 1.
- **Cuando:** se publica la rúbrica versión 2 del mismo curso.
- **Entonces:** la calibración activa queda vencida, con ese motivo registrado.

### Escenario 2 — El historial sobrevive

- **Dado:** una calibración que acaba de vencer.
- **Cuando:** se consulta su detalle.
- **Entonces:** sigue disponible con su resultado original, solo marcada como vencida.

## Prototipo

No aplica ficha propia — se muestra dentro de la pantalla de calibración ya existente.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento.
- **Prioridad:** deseable — mejora real de gobierno, no bloquea la demo obligatoria.

## Dependencias / Impactos

- **Riesgos:** un tiempo límite mal elegido puede vencer calibraciones de sorpresa — conviene
  arrancar con un valor conservador (largo) hasta tener uso real.

---

# LLM-S04-H03 (LLM-EP04-H04) — Avisar cuando un curso tiene entregas frenadas sin calibración

- **Grupo de trabajo:** Calibración (EP-04) · **Pareja:** P4 · **Trabajo estimado:** ~15–20
  horas · **Tipo:** Historia de valor (la vive un docente o administrador)

## Descripción (Como / Quiero / Para)

- **Como:** docente o administrador de un curso.
- **Quiero:** recibir un aviso cuando mi curso tiene entregas de alumnos acumulándose sin
  poder evaluarse por falta de calibración vigente.
- **Para:** actuar antes de que se junte un lote grande de entregas sin corregir.

## Notas / Observaciones

- El aviso se dispara **una sola vez** por episodio, no uno por cada entrega pendiente — eso
  sería ruido.
- Mientras el mismo episodio siga abierto, no se repite el aviso. Si se resuelve y después se
  abre uno nuevo, ahí sí avisa de nuevo.
- El umbral (cuántas pendientes, o desde cuándo) es configurable.

## Criterios de Aceptación (CA)

- **CA1:** superar el umbral configurado dispara un aviso con el curso, la cantidad y desde
  cuándo.
- **CA2 (caso que debe fallar):** mientras el episodio sigue abierto, no se dispara un segundo
  aviso.
- **CA3 (caso que debe fallar):** un curso por debajo del umbral no dispara ningún aviso.

## BDD

### Escenario 1 — Se cruza el umbral (camino feliz)

- **Dado:** un curso cuyas entregas pendientes acaban de superar el umbral.
- **Cuando:** el sistema revisa su estado.
- **Entonces:** dispara un aviso con el detalle.

### Escenario 2 — El episodio sigue abierto

- **Dado:** un curso que ya avisó y sigue acumulando pendientes.
- **Cuando:** el sistema vuelve a revisarlo.
- **Entonces:** no dispara un segundo aviso mientras el mismo episodio siga abierto.

## Prototipo

Un cartel de aviso dentro de la pantalla del curso, con la cantidad de pendientes y desde
cuándo.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento.
- **Prioridad:** deseable — sin esto, un docente puede no enterarse hasta que un alumno se
  queje.

## Dependencias / Impactos

- **Riesgos:** un umbral mal elegido genera ruido (muy bajo) o silencio (muy alto) — arrancar
  conservador y ajustar con uso real.

---

# LLM-EP03-H04 — Que la rúbrica de un curso quede fija una vez publicada

- **Grupo de trabajo:** Golden set y referencia humana (EP-03) · **Pareja:** P4 (con P5 en la
  pantalla) · **Trabajo estimado:** sin número propio — la ficha dice que el código ya existe
  y que se estima en Refinamiento · **Tipo:** Historia de valor (la vive un docente)

## Descripción (Como / Quiero / Para)

- **Como:** docente autorizado de un curso.
- **Quiero:** crear, editar mientras es borrador, y publicar versiones sucesivas de la
  rúbrica de mi curso, con sus cinco criterios y el peso de cada uno.
- **Para:** fijar con qué se va a calibrar y evaluar a mis alumnos, sabiendo que una vez
  publicada nadie —ni yo— puede alterarla.

## Notas / Observaciones

- Una rúbrica siempre tiene exactamente cinco criterios fijos, cada uno con su peso. Los
  pesos tienen que **sumar exactamente 100** para poder publicarse.
- Mientras es borrador se puede editar. Una vez publicada, queda **inmutable para siempre** —
  ni el propio docente puede modificarla; hay que publicar una versión nueva.
- Solo el docente autorizado de ese curso puede tocar su rúbrica.

## Criterios de Aceptación (CA)

- **CA1:** un docente crea un borrador con los cinco criterios y sus pesos.
- **CA2:** publicar un borrador completo con pesos que suman 100 lo deja fijo para siempre.
- **CA3 (caso que debe fallar):** publicar con pesos que no suman 100 se rechaza y el borrador
  sigue editable.
- **CA4 (caso que debe fallar):** un docente de otro curso no puede tocar esta rúbrica.

## BDD

### Escenario 1 — Crear y publicar (camino feliz)

- **Dado:** un docente con un borrador de cinco criterios cuyos pesos suman 100.
- **Cuando:** lo publica.
- **Entonces:** queda fija, y ningún intento posterior de modificarla tiene efecto.

### Escenario 2 — Pesos que no suman 100 (caso que debe fallar)

- **Dado:** un borrador con pesos que suman 95.
- **Cuando:** el docente intenta publicarlo.
- **Entonces:** se rechaza y el borrador sigue editable.

## Prototipo

Una pantalla con los cinco criterios y un control numérico para cada peso, mostrando el total
en tiempo real para que el docente vea si ya llega a 100.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento — el código ya existe, así que el costo
  real es bajo, pero no se inventa el número acá.
- **Prioridad:** imprescindible — sin rúbrica publicada no hay contra qué calibrar.

## Dependencias / Impactos

- **Riesgos:** confirmar con el responsable del producto si cada curso puede elegir sus
  propios pesos libremente, o si tienen que coincidir con un valor fijo de referencia de toda
  la plataforma — es una duda abierta, no una decisión ya tomada.

---

# LLM-EP03-H05 — Que el docente tenga su propia colección de casos de referencia por curso

- **Grupo de trabajo:** Golden set y referencia humana (EP-03) · **Pareja:** P5 · **Trabajo
  estimado:** sin número propio — el código ya existe, se estima en Refinamiento · **Tipo:**
  Historia de valor (la vive un docente)

## Descripción (Como / Quiero / Para)

- **Como:** docente autorizado de un curso.
- **Quiero:** crear la colección de casos de referencia de mi curso —copiando la versión
  base de la plataforma, o armándola desde cero—, cargarle ejemplos y publicar versiones
  sucesivas.
- **Para:** tener mi propia referencia, sin que mis cambios afecten la base de la plataforma
  ni ninguna versión que ya publiqué.

## Notas / Observaciones

- Cada caso cargado incluye la conversación de ejemplo y los cinco puntajes de referencia que
  le pondría un humano.
- **Todo dato de alumno se anonimiza siempre**, cargado a mano o importado en lote — no es una
  promesa, está siempre activo.
- Se puede cargar de a un caso, o importar un lote entero; si una sola fila del lote está mal,
  no se carga ninguna hasta corregirla.
- Una colección ya publicada, calibrada o usada por un desafío no se puede borrar.

## Criterios de Aceptación (CA)

- **CA1:** un docente crea su colección (vacía o copiada de la base) y le agrega un caso
  válido.
- **CA2:** publicar la colección con al menos un caso la deja fija para siempre.
- **CA3:** los datos de alumno de cada caso llegan siempre anonimizados.
- **CA4 (caso que debe fallar):** un lote con una fila inválida no carga ningún caso hasta
  corregirla.
- **CA5 (caso que debe fallar):** borrar una colección ya en uso se rechaza, explicando el
  motivo.

## BDD

### Escenario 1 — Copiar y publicar (camino feliz)

- **Dado:** un docente y una versión base de plataforma ya publicada.
- **Cuando:** copia esa base, agrega un caso, y publica.
- **Entonces:** su colección queda publicada, sin afectar la base original.

### Escenario 2 — Importación con una fila inválida (caso que debe fallar)

- **Dado:** un lote de importación donde una fila tiene puntajes fuera de rango.
- **Cuando:** el docente intenta cargar el lote.
- **Entonces:** no se carga ningún caso hasta corregir esa fila.

## Prototipo

Una pantalla con la lista de casos cargados, un botón para agregar uno a mano y otro para
importar un archivo, con el resultado de la validación fila por fila.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento — el código ya existe.
- **Prioridad:** imprescindible — es el insumo directo de la calibración.

## Dependencias / Impactos

- **Riesgos:** todavía no está claro quién crea la colección **base de plataforma** de la que
  se copia — hay que confirmarlo antes de dar esto por completamente cerrado.

---

# LLM-EP06-H02 — Recibir un puntaje explicado, no solo un número

- **Grupo de trabajo:** Evaluación (EP-06) · **Pareja:** *(a asignar)* · **Depende de:** el
  encolado del intento cerrado y la rúbrica vigente del curso · **Trabajo estimado:** sin
  número propio — la ficha dice que se estima en Refinamiento · **Tipo:** Historia de valor
  (la vive un alumno)

## Descripción (Como / Quiero / Para)

- **Como:** alumno que acaba de cerrar un intento de un desafío.
- **Quiero:** recibir un puntaje explicado en los cinco criterios de la rúbrica de mi curso,
  con su justificación.
- **Para:** entender **cómo** usé la ayuda de la IA en el desafío, no solo ver un número sin
  explicación.

## Notas / Observaciones

- El puntaje total no es un promedio simple: sale de combinar los cinco criterios según el
  peso de **esa** rúbrica publicada, la misma con la que se calibró.
- Si la respuesta del modelo no tiene la forma esperada, no se inventa un puntaje parcial — la
  evaluación queda pendiente por falla, y se resuelve como otra historia (la siguiente).
- Una vez publicada, una evaluación **no se edita ni se borra** — una corrección futura es
  otra historia aparte, no una edición de esta.
- El alumno solo ve su propia evaluación.

## Criterios de Aceptación (CA)

- **CA1:** con calibración vigente, el sistema guarda un puntaje con los cinco criterios, su
  justificación y de qué versión de modelo y rúbrica salió.
- **CA2 (caso que debe fallar):** una respuesta del modelo mal formada no se guarda como
  evaluación válida.
- **CA3:** el puntaje total combina los cinco criterios según los pesos de la rúbrica vigente.
- **CA4 (caso que debe fallar):** una evaluación ya publicada no se puede editar ni borrar.

## BDD

### Escenario 1 — Evaluación con calibración vigente (camino feliz)

- **Dado:** un intento cerrado cuyo desafío tiene calibración aprobada.
- **Cuando:** el modelo responde con los cinco criterios en la forma esperada.
- **Entonces:** se guarda el puntaje explicado con su justificación.

### Escenario 2 — Respuesta del modelo mal formada (caso que debe fallar)

- **Dado:** una evaluación en curso.
- **Cuando:** el modelo responde con un formato que no cumple lo esperado.
- **Entonces:** no se guarda ningún puntaje; queda pendiente por falla.

## Prototipo

No existe todavía una pantalla de desglose de puntaje para el alumno — queda pendiente como
historia aparte.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento.
- **Prioridad:** candidata a imprescindible — es el corazón del producto según su objetivo.

## Dependencias / Impactos

- **Otros equipos:** falta acordar con quien consume el evento de puntaje qué campos exactos
  necesita — hoy el evento se publica vacío de ese detalle.
- **Riesgos:** confundir "el modelo no está calibrado para este curso" con "el modelo
  respondió mal esta vez" es el error más fácil de cometer — son casos distintos.

---

# LLM-EP06-H03 — Que mi entrega no se pierda si el corrector automático está caído

- **Grupo de trabajo:** Evaluación (EP-06) · **Pareja:** *(a asignar)* · **Depende de:** la
  historia anterior (el camino de evaluación) · **Trabajo estimado:** ~15 horas · **Tipo:**
  Historia de valor (la vive un alumno)

## Descripción (Como / Quiero / Para)

- **Como:** alumno que cierra un intento de un desafío.
- **Quiero:** que mi entrega quede registrada aunque el proveedor de IA que evalúa esté caído
  en ese momento.
- **Para:** no perder mi entrega ni tener que reintentarla por una falla que no es mía.

## Notas / Observaciones

- Si evaluar falla por una caída del proveedor, la entrega **no se pierde**: queda "pendiente
  por falla del evaluador" — distinto de "pendiente por falta de calibración".
- Un trabajo en segundo plano reintenta más tarde, siguiendo el mismo camino de siempre — no
  hay un "modelo de respaldo" ni un atajo automático.
- Reintentar nunca genera dos resultados para el mismo intento.

## Criterios de Aceptación (CA)

- **CA1:** si falla la invocación al evaluador, el intento queda aceptado y pendiente por
  falla — no se rechaza la entrega del alumno.
- **CA2:** cuando el proveedor vuelve a responder, un reintento retoma la evaluación pendiente
  y la resuelve.
- **CA3 (caso que debe fallar):** reintentar no genera dos evaluaciones ni dos avisos para el
  mismo intento.

## BDD

### Escenario 1 — El proveedor está caído (camino feliz de la resiliencia)

- **Dado:** un intento listo para evaluarse.
- **Cuando:** la invocación al evaluador falla.
- **Entonces:** el intento queda aceptado, pendiente por falla, sin rechazar nada del alumno.

### Escenario 2 — El proveedor se recupera

- **Dado:** una evaluación pendiente por falla.
- **Cuando:** un reintento programado vuelve a invocar al modelo y esta vez responde bien.
- **Entonces:** la evaluación termina con su puntaje, igual que en el camino normal.

## Prototipo

No aplica — pieza de resiliencia sin pantalla propia.

## Estimación / Prioridad

- **Puntos de esfuerzo:** se asignan en Refinamiento.
- **Prioridad:** candidata a imprescindible — es una regla que la épica exige explícitamente.

## Dependencias / Impactos

- **Riesgos:** si se reutiliza mal el mecanismo de reintentos que ya existe para "falta de
  calibración", se puede confundir esa causa con "el proveedor falló esta vez" — son dos casos
  distintos que no deberían compartir la misma lógica de decisión.

---

# Hilo F — EP-07: que alguien pueda operar el servicio, no solo usarlo

> Las tres historias que siguen son nuevas de la recalibración — el arranque de la operación
> del servicio (costos, cuotas, protección contra sobrecarga). La segunda mitad de esta épica
> (recuperar trabajos, salud, prueba de carga) se cuenta en el documento explicado de Sprint 2,
> no acá.

## LLM-S09-H01 — Ver cuánto está gastando el servicio y si algo está fallando

- **Pareja:** P2 (con P1) · **Trabajo estimado:** 36 horas · **Tipo:** Historia de valor (la
  vive un operador de la plataforma, no un alumno ni un docente)

**Descripción:** como operador de la plataforma, quiero un panel donde ver cuánto se gastó,
cuánto de la cuota diaria se consumió, y si el proveedor de IA está fallando o respondiendo
lento — para darme cuenta de un problema antes de que afecte a los alumnos.

**Notas:** el panel nunca muestra contenido de conversaciones ni datos de alumnos, solo
números agregados (tokens, costo estimado, porcentaje de cuota, errores, velocidad de
respuesta). Si el proveedor está caído, el panel sigue funcionando mostrando los últimos
números guardados, avisando que son "de hace un rato", no del momento. Solo un operador puede
verlo — un docente o alumno que lo intente recibe "prohibido".

**Criterios de Aceptación:** **CA1** el panel muestra costo, cuota usada y errores del período
elegido. **CA2** si el proveedor está caído, igual responde con los últimos datos guardados,
marcados como "no en vivo". **CA3 (debe fallar)** un docente o alumno que intenta verlo recibe
"prohibido". **CA4 (debe fallar)** el panel nunca contiene ningún dato identificable de un
alumno.

**BDD — Escenario 1 (camino feliz):** Dado un operador con permiso; cuando consulta el panel
de hoy; entonces ve el gasto y la cuota usada, sin ningún dato de alumnos. **Escenario 2 (debe
fallar):** Dado un docente sin permiso de operador; cuando intenta ver el panel; entonces
recibe "prohibido".

**Prototipo:** un tablero con una fila por función del servicio (tutor, evaluador, etc.),
mostrando gasto, cuota usada y errores, más un indicador de "en vivo" o "últimos datos
guardados".

**Estimación / Prioridad:** puntos a fijar en Refinamiento; prioridad deseable — imprescindible
para operar en serio, pero no bloquea la funcionalidad pedagógica.

**Dependencias:** ninguna externa a otro equipo; riesgo principal es que el costo mostrado es
una estimación propia, no la factura real del proveedor — se documenta así, no se promete
exactitud contable.

## LLM-S09-H02 — Cambiar cuánto puede gastar una función, con quién y por qué quedó registrado

- **Pareja:** P2 · **Trabajo estimado:** 28 horas · **Tipo:** Historia de valor (la vive un
  administrador)

**Descripción:** como administrador, quiero cambiar el límite diario de uso de una función (por
ejemplo, el tutor) sin tener que tocar código, y que quede registrado quién lo cambió, cuándo y
por qué motivo.

**Notas:** cada cambio se guarda como un registro nuevo, nunca se pisa uno viejo — el historial
completo queda siempre disponible. Cambiar un límite exige explicar el motivo por escrito (al
menos unas palabras, no un campo vacío). Solo un administrador puede cambiarlo; un operador
solo puede mirar el historial, no cambiar nada.

**Criterios de Aceptación:** **CA1** un administrador cambia el límite de una función indicando
un motivo, y el cambio queda guardado con quién y cuándo. **CA2** el historial completo de
cambios queda disponible, ordenado del más reciente al más viejo. **CA3 (debe fallar)** un
cambio sin motivo escrito se rechaza. **CA4 (debe fallar)** un docente que intenta cambiar un
límite recibe "prohibido".

**BDD — Escenario 1 (camino feliz):** Dado un administrador; cuando sube el límite del tutor
explicando el motivo; entonces el cambio queda guardado con su firma y motivo. **Escenario 2
(debe fallar):** Dado un administrador; cuando intenta cambiar un límite sin escribir un
motivo; entonces el sistema lo rechaza pidiendo el motivo.

**Prototipo:** un formulario simple con el límite nuevo y un campo de texto obligatorio para el
motivo, y debajo el historial de cambios anteriores.

**Estimación / Prioridad:** puntos a fijar en Refinamiento; prioridad deseable — habilita
ajustar cuotas sin tener que redesplegar el servicio.

**Dependencias:** ninguna externa; riesgo principal es que un cambio mal pensado deje al
servicio sin margen antes de terminar el día — se mitiga mostrando una alerta cuando el uso ya
está muy alto.

## LLM-S09-H03 — Que el servicio se proteja solo si alguien se pasa del límite

- **Pareja:** P2 · **Trabajo estimado:** 22 horas · **Tipo:** Tarea interna (protege al
  servicio, no la vive un usuario final directamente)

**Descripción:** como sistema, quiero que cualquier ruta con límite de uso avise claramente
"che, esperá un poco" cuando se supera la cuota del día, y que los reintentos internos al
proveedor no lo saturen más si ya está teniendo problemas.

**Notas:** al superar el límite diario, el sistema avisa cuánto falta para el próximo período,
sin siquiera intentar llamar al proveedor — ahorra el gasto. Si el proveedor falla, el sistema
reintenta un par de veces con una pausa creciente entre intento e intento, nunca sin límite. Si
más de la mitad de los intentos recientes a una función están fallando, esa función se "apaga"
un rato corto antes de volver a intentar, para no seguir insistiéndole a un proveedor que ya
está mal.

**Criterios de Aceptación:** **CA1** superar el límite diario de una función avisa cuánto
falta para el próximo período, sin llamar al proveedor. **CA2** si el proveedor falla, el
sistema reintenta un número limitado de veces con pausas crecientes, nunca indefinidamente.
**CA3 (debe fallar)** nunca se responde como si todo estuviera bien cuando en realidad el
límite se superó o el proveedor sigue caído.

**BDD — Escenario 1 (camino feliz del caso de error controlado):** Dado una función que ya usó
toda su cuota del día; cuando alguien intenta usarla de nuevo; entonces el sistema avisa
cuánto falta para el próximo período, sin gastar nada. **Escenario 2 (debe fallar):** Dado un
proveedor que falla todas las veces que se lo intenta; cuando se agotan los reintentos;
entonces el sistema responde con un error claro, nunca con un "todo bien" falso.

**Prototipo:** no aplica — pieza interna sin pantalla propia; se ve reflejada en el panel de
la primera historia de este hilo.

**Estimación / Prioridad:** puntos a fijar en Refinamiento; prioridad imprescindible — sin esto,
los límites configurables de la historia anterior son solo un número guardado sin efecto real.

**Dependencias:** ninguna externa; riesgo principal es un pequeño retraso (hasta un minuto) en
que un cambio de límite tenga efecto real, por cómo se guarda en memoria para no consultar la
base de datos en cada pedido.

---

# Hilo G — extensión de EP-09: todavía sin ficha propia

> A diferencia de todo lo de arriba, esta pieza (leer PDF escaneados con OCR, y manejar
> versiones de material sin mezclarlas) **todavía no tiene una ficha de historia escrita**.
> Se sabe qué hace por la receta vieja del catálogo general del proyecto, pero no hay
> Como/Quiero/Para, ni Criterios de Aceptación, ni escenarios todavía — eso se redacta en la
> Planning de Sprint 1, antes de comprometer horas exactas. Lo que sí se sabe: la referencia
> de horas heredada del catálogo viejo (~208 h) salió sobreestimada en las otras tres épicas
> que sí ya tienen ficha (EP-07, EP-08, EP-10 terminaron entre el 26% y el 41% de su referencia
> vieja), así que es razonable esperar que acá pase algo parecido — pero no se acorta el número
> sin una ficha real que lo respalde.

---

## En resumen

Al final de este tramo no hay corrección con inteligencia artificial todavía,
pero sí hay:

- un acuerdo escrito de cómo trabaja el equipo,
- un sistema que se enciende con un comando,
- el esqueleto del servicio con su seguridad puesta,
- un archivador de datos que no se puede romper ni borrar por error,
- un profesor que crea su colección de referencia, le carga ejemplos y los
  vuelve a ver después de reiniciar,
- una pantalla sencilla para hacerlo,
- un contrato y una maqueta para que otro equipo avance en paralelo,
- pruebas automáticas que demuestran que todo eso es verdad,
- y, en paralelo y sin bloquear nada de lo anterior, el camino listo para
  algún día hablar con un modelo real — hoy todavía habla solo con un doble
  simulado.

**Esto era el Sprint 1 original.** Con la recalibración del 2026-09-13, el mismo tramo también
suma: el camino hablando con un modelo real de verdad, un tutor completo con memoria de
conversación, un profesor subiendo material en PDF para que el tutor lo use con citas, la
calibración de curso y de plataforma con sus avisos de vencimiento, la rúbrica y la colección
de referencia versionadas del profesor, el arranque de la corrección automática con su
resiliencia si el corrector se cae, y el arranque de un panel para que alguien pueda operar el
servicio (ver costos, cuotas, protegerlo de sobrecarga). Lo único que queda totalmente afuera
de este tramo, sin ficha propia todavía, es la lectura de PDF escaneados con OCR — se define
recién en la reunión de arranque de este mismo tramo.
