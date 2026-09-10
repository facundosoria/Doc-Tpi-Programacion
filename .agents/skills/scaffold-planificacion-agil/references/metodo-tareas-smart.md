# Método para desglosar una historia en tareas (SMART)

Guía condensada. Fuentes: Cohn (2004), Schwaber & Sutherland (2020), Doran (1981, criterio
SMART), Wake (2003, INVEST para historias). Es el método que la cátedra de Metodología de
Sistemas II evalúa para el nivel **tarea**.

---

## 1. Épica, historia y tarea

| Nivel | Qué es | Método / criterio | ¿`Como/Quiero/Para`? | ¿Puntos? |
|---|---|---|---|---|
| **Épica** | Funcionalidad grande que no entra en un sprint. | Objetivo + CA a nivel épico. | No (el template de Taiga no lo usa) | No |
| **Historia de usuario** | Unidad de **valor** entregable; se compromete en un sprint. | 3C, **INVEST**, BDD, Planning Poker. | Sí | Sí (Fibonacci) |
| **Tarea** | **Paso técnico interno** para completar una historia. La define el equipo de desarrollo. | **SMART**, ≤ 1 jornada efectiva. | **No** | **No** |

- El **sprint** dice *cuándo* se construye una historia; la **épica**, *para qué sirve*; la
  **tarea**, *qué paso técnico* la hace realidad.
- En Taiga la Tarea **cuelga de su Historia de Usuario**. Si el padre es un habilitador que
  se cargó como «tarea de sprint», las tareas cuelgan de esa tarea de sprint.

---

## 2. SMART aplicado a una tarea

| Letra | Criterio | Señal de que falla |
|---|---|---|
| **S** — Específica | Un solo paso técnico: **verbo + resultado concreto**. | El título necesita «y», «o», «además»: son dos tareas. |
| **M** — Medible | El *criterio de terminado* se responde **sí/no** sin discusión. | «Avanzar el caso de uso», «mejorar la validación»: no se sabe cuándo terminó. |
| **A** — Alcanzable | Cabe en **≤ 1 jornada efectiva**, para una persona o pareja, con lo que ya existe. | Depende de algo que todavía no está y no hay mock; o es medio sprint de trabajo. |
| **R** — Relevante | Sirve a un **criterio de aceptación o escenario BDD** concreto de la historia padre. | No se puede decir a qué CA/escenario aporta: o sobra, o falta un criterio en la historia. |
| **T** — Acotada en el tiempo | Tiene **estimación en horas**; si supera la jornada, se parte. | «Lo que lleve»: sin estimación no entra al tablero. |

> Regla práctica: **una tarea = un paso, un resultado observable, una jornada.**

### El criterio de terminado (Done) es observable

- Mal: «el caso de uso quedó más completo».
- Bien: «`POST /api/llm/golden-sets` devuelve `201` con header `Location` y la prueba de
  integración del camino feliz pasa».
- Bien: «la migración `V1` corre desde base vacía dos veces con el mismo resultado
  (prueba Testcontainers en verde)».

---

## 3. Cómo se parte una historia

1. **Barrido de los CA y del BDD.** Cada CA y cada escenario (incluidos los **negativos**)
   nombra algo que hay que construir o probar. Anotá cada uno como tarea candidata.
2. **Ordenar por paso de construcción.** Orden típico del equipo:
   `contrato y amenaza → dominio y migración → caso de uso → adaptadores → seguridad y
   resiliencia → observabilidad → prueba E2E → demo y evidencia`.
3. **Fusionar / separar.**
   - **Fusioná** candidatas que son el mismo paso técnico y juntas no pasan la jornada.
   - **Separá** si hay dos pasos distintos, si la prueba es sustancial (tarea aparte), o si
     una parte se puede terminar y revisar sin la otra.
4. **Cuadrar horas.** La suma de las horas de las tareas = la referencia en horas de la
   historia (columna *h* del plan / receta de sprint). Si no cierra, el corte está mal:
   **no** se maquillan los números.
5. **Cobertura.** Al terminar, cada CA y cada escenario BDD de la historia está cubierto por
   al menos una tarea. Los negativos suelen caer en una tarea de seguridad, de validación o
   de idempotencia; si ninguna los cubre, falta una tarea.

---

## 4. Qué NO es una tarea

- **No** lleva `Como / Quiero / Para` — eso describe valor para un usuario; una tarea es
  trabajo interno.
- **No** se estima en **puntos Fibonacci** ni lleva prioridad **MoSCoW** ni checklist
  **INVEST**: son criterios de Historia de Usuario.
- **No** entrega valor perceptible por sí sola. Si una «tarea» sí lo entrega y la protagoniza
  un rol real, probablemente es una **historia** mal clasificada.
- **No** es un recordatorio vago («revisar seguridad»). Es un paso con resultado observable
  y una estimación.

---

## 5. Lista de control

- [ ] Cada tarea: verbo + resultado en el título; las cinco letras SMART explícitas.
- [ ] Criterio de terminado que se responde sí/no.
- [ ] `Traza` a un CA## y/o Escenario BDD de la historia padre.
- [ ] Estimación en horas; ninguna tarea supera la jornada efectiva.
- [ ] Todos los CA y escenarios BDD de la historia (negativos incluidos) cubiertos.
- [ ] Suma de horas de las tareas = referencia de la historia.
- [ ] Tareas ordenadas por el paso de construcción; `Depende de` puesto donde hay orden.
- [ ] Ninguna tarea con `Como/Quiero/Para`, puntos, MoSCoW ni INVEST.
