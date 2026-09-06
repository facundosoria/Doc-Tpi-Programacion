# 30 — Arranque ágil: Sprint 0, DoD, capacidad, épicas e historias iniciales

> **Vista de entrega.** Este documento reúne, en un solo lugar y con el formato de la
> [guía de cátedra de HU](29-guia-catedra-historias-de-usuario.md), las cinco piezas que la
> cátedra pide para el arranque del proyecto:
>
> 1. Sprint 0
> 2. Definition of Done (DoD) propia del equipo
> 3. Cálculo de capacidad del sprint
> 4. Primeras épicas
> 5. Primeras historias de usuario
>
> **No es fuente de verdad.** Cada sección indica dónde vive la versión que se mantiene:
>
> | Pieza | Fuente única |
> |---|---|
> | Sprint 0 y su checklist | [`Plan de ejecucion/07`](<../Plan de ejecucion/07-backlog-ejecutable-sprints.md>) · sección «Sprint 0» |
> | DoR / DoD | [23 · §9.2](23-plan-construccion-producto-llm.md) |
> | Cálculo de capacidad | [23 · §2](23-plan-construccion-producto-llm.md) |
> | Catálogo de épicas | [`Plan de ejecucion/07`](<../Plan de ejecucion/07-backlog-ejecutable-sprints.md>) · sección «Épicas» |
> | Historias de S1 | [`Plan de ejecucion/07`](<../Plan de ejecucion/07-backlog-ejecutable-sprints.md>) · sección «S1» |
> | Método para redactar/estimar HU | [29](29-guia-catedra-historias-de-usuario.md) |
> | Templates de Taiga | [`plantillas/historia-de-usuario-taiga.md`](plantillas/historia-de-usuario-taiga.md) · [`plantillas/epica-taiga.md`](plantillas/epica-taiga.md) |
>
> Si un número de acá no coincide con la fuente, **manda la fuente**.

---

## 1. Sprint 0 — arranque del proyecto

El **Sprint 0 no produce incremento de software** ni consume capacidad de entregables: es la
primera Planning ampliada, donde el equipo cierra acuerdos y prepara el ambiente. Ocurre **antes
de S1** y su objetivo es dejar cumplida la DoR (§2) de todas las historias de S1.

### 1.1 Qué deja cerrado (checklist de salida)

| # | Ítem | Estado |
|---|---|---|
| 1 | Fecha de inicio, integrantes y **disponibilidad individual declarada** para el ciclo | ☐ |
| 2 | Nombres de P1–P5 (10 integrantes), referente de producto y facilitador (los otros 2), suplencias | ☐ |
| 3 | **Cálculo de capacidad de S1** hecho con la fórmula de §3 y registrado en una copia de la [plantilla de sprint](plantillas/sprint-llm.md) | ☐ |
| 4 | **DoR y DoD** ([23 · §9.2](23-plan-construccion-producto-llm.md)) leídas y aceptadas por el equipo | ☐ |
| 5 | **Historia canónica** del equipo elegida y estimada ([29 · §5.3](29-guia-catedra-historias-de-usuario.md)); candidata: `LLM-S01-H06` | ☐ |
| 6 | Épicas revisadas; cada historia de S1 asignada a una épica | ☐ |
| 7 | Épicas e historias de S1 cargadas en Taiga con los templates oficiales (HU: BDD camino feliz + 2 negativos, puntos Fibonacci, INVEST); permalinks anotados | ☐ |
| 8 | D01 (identidad, Gateway, Eureka, frontend) y D02 (docentes, set base, cinco dimensiones) con responsable y estado; D03/D04 solicitadas con fecha | ☐ |
| 9 | Requisitos locales verificados: `git`, `docker`, `docker compose`, `java -version` → Java 21 | ☐ |
| 10 | Remoto `tpi-llm` con `main` y `develop` creadas y protegidas (PR obligatorio, CI en verde, sin push directo ni force-push) según [GITFLOW](GITFLOW.md). Las `feature/sNN/llm-sNN-hNN-<slug>` nacen de `develop` al arrancar cada historia de S1, no en Sprint 0 | ☐ |
| 11 | Carpeta de evidencia del equipo acordada (PR, CI, migración, comandos Docker, demo) | ☐ |
| 12 | Cada historia de S1 cumple la DoR: usuario, aceptación negativa, responsable/suplente, estimación dentro de la capacidad | ☐ |
| 13 | La estimación de S1 incluye esqueleto, migraciones, contrato, interfaz, pruebas e integración —no solo el «camino feliz» | ☐ |
| 14 | Criterio de demo de S1 acordado: acceso autorizado, carga, consulta y recuperación tras reinicio | ☐ |

### 1.2 Relación con Scrum

Scrum no define un «Sprint 0». Acá se usa como **iteración de preparación sin compromiso de
incremento**: nombres, capacidad, backlog inicial refinado y ambiente reproducible. La primera
Planning formal (con sus 24 h-persona) se carga a **S1**, no a Sprint 0.

---

## 2. Definition of Done (DoD) propia del equipo

La **DoD** es el acuerdo del equipo sobre las condiciones que cumple **cualquier** incremento
para considerarse terminado. Es **común a todas las historias**, a diferencia de los criterios de
aceptación, que son propios de cada una ([29 · §11](29-guia-catedra-historias-de-usuario.md)).
La acompaña la **DoR** (Definition of Ready): la compuerta de entrada.

| | **Definition of Ready — DoR** | **Definition of Done — DoD** |
|---|---|---|
| Qué es | La historia está **entendida y sin bloqueos** para empezar | La historia está **realmente terminada**: el usuario la usa y hay evidencia |
| Cuándo | En Refinamiento y Planning, **antes** de comprometerla | En la Review, **antes** de aceptarla |
| Si no se cumple | **No entra** al sprint: se refina o divide | **Vuelve a «en progreso»**: no se presenta como terminada |

### 2.1 DoR — una historia puede comprometerse solo si tiene

1. **Usuario y resultado observable**; el *COMO* nombra un rol real (docente, alumno, ADMIN,
   operador), no «el sistema» ni «el equipo». Un habilitador puramente técnico es **tarea**, no HU.
2. **Criterios de aceptación en BDD** (Dado / Cuando / Entonces): ≥ 1 camino feliz y ≥ 2
   negativos (no autorizado, entrada inválida, duplicado, dependencia caída). Cada *Entonces*
   describe algo observable.
3. **Requisito trazable** (`RF-*` / `PAR-*`) y **referente de producto** nombrado.
4. **Contrato definido** si cruza servicios: esquema, autenticación, correlación e idempotencia.
5. **Responsable y suplente** asignados.
6. **Datos de prueba** autorizados, o fixture sintético etiquetado.
7. **Dependencias externas** con dueño, fecha y alternativa. Un mock habilita trabajo interno,
   no cierra una historia cuya demo exige integración real.
8. **Estimación** hecha en equipo, **después** de los criterios de aceptación. En Taiga: puntos
   Fibonacci contra la historia canónica; en el plan: horas. Si supera ~40 h (o llega a 13
   puntos) se divide verticalmente antes de entrar.
9. **Historia sana según INVEST** (Independiente, Negociable, Valiosa, Estimable, Small,
   Testeable). Si falla la **V**, casi siempre es tarea técnica; si falla la **S**, es épica.

### 2.2 DoD — la historia y el incremento están terminados

**Por cada historia:**

1. Implementación revisada por PR, sin secretos, con migración versionada si cambia persistencia.
2. Autorización por audiencia, scope/rol y ownership; se valida el recurso referenciado, no los
   IDs del body.
3. Validación Bean Validation y respuesta RFC 7807 con `X-Request-Id` propagado.
4. Idempotencia: la misma `Idempotency-Key` / `eventId` no duplica efectos.
5. Trazas `traceparent` y `X-Request-Id` en HTTP, jobs y eventos. Logs sin prompt, solución,
   token ni dato sensible innecesario.
6. Pruebas: unitarias de reglas; integración de persistencia/migración; contrato con WireMock o
   consumer pactado; autorización y fallo relevante. Los **escenarios BDD de aceptación**
   (feliz + negativos) quedan ejecutados con evidencia. Cobertura según
   [24](24-convenciones-cobertura.md).
7. Métrica, health/readiness y runbook si introduce trabajo asíncrono, proveedor, dato retenido
   u operación manual.

**Por cada incremento de sprint:**

1. El usuario termina el recorrido comprometido con interfaz, persistencia y permisos reales.
2. Los servicios consumidores incluidos en el compromiso participan realmente.
3. Pruebas de contrato, fallas, idempotencia, seguridad y regresión aplicables pasan.
4. Las comprobaciones de calidad del modelo/RAG/moderación aplicables tienen evidencia.
5. Código revisado, migraciones y contratos coinciden con la versión desplegada.
6. Existe versión identificable, evidencia de demo/aceptación y procedimiento de recuperación.
7. Documentación y backlog reflejan lo entregado. La página del componente en la **Wiki de
   Taiga** está creada/actualizada según [27](27-guia-wiki-taiga.md): HU enlazadas por su
   permalink del backlog y diagramas de la secuencia (DER → BPMN → …) al día.
8. La demo del sprint pasa en un **ambiente integrado**, no en una máquina local aislada.

> Una funcionalidad parcial no se presenta como terminada. Si excede capacidad se reduce a un
> caso de uso menor completo, con acuerdo de producto, o se ajusta el calendario. **Nunca** se
> rebajan seguridad, pruebas o controles académicos para cumplir una fecha.

---

## 3. Cálculo de capacidad del sprint

El sprint dura **2 semanas**. La disponibilidad se toma de las **horas declaradas por cada
integrante** en la planilla del equipo (12 personas), no de un supuesto.

### 3.1 Cálculo inicial

| Concepto | Cálculo | Horas-persona por sprint |
|---|---|---:|
| Disponibilidad nominal | 408 h/semana declaradas × 2 semanas | **816** |
| − Reuniones programadas | Ver §3.2 (12 participantes) | **−102** |
| = Base | 816 − 102 | **714** |
| − Reserva | 20 % de 714 | **−143** |
| **= Capacidad comprometible en entregables** | 714 × 0,80 | **≈ 571** |

> El sprint dura **dos semanas**: el nominal es 408 h/semana × 2 = **816 h**. El colchón por
> cursada, exámenes y bajas es la **reserva del 20 %**, no un recorte del nominal.

### 3.2 Reuniones programadas

| Reunión | Cant. | Duración | Participantes | Horas-persona |
|---|---:|---:|---:|---:|
| Sprint Planning | 1 | 120 min | 12 | 24 |
| Daily / sincronización | 4 (2/semana) | 45 min | 12 | 36 |
| Sprint Review con demo | 1 | 90 min | 12 | 18 |
| Retrospectiva | 1 | 60 min | 12 | 12 |
| Refinamiento del backlog | 2 (1/semana) | 30 min | 12 | 12 |
| **Total** | | | | **102** |

La **coordinación de dependencias** no es una reunión aparte: es un punto fijo de la agenda de
las sincronizaciones, con un representante por pareja cuando hay cruces.

### 3.3 Capacidad ≠ presupuesto de trabajo

Las recetas S1–S19 estiman el trabajo de cada sprint en **~208 h de paquetes** (estimación
gruesa anterior a los contratos, **no un tope**). La diferencia con las 571 h —**~363 h por
sprint**— es **margen explícito** para re-estimar en Planning, absorber imprevistos y cubrir el
overhead de coordinar 12 personas. Al cerrar S2 se revisa la previsión con datos reales.

### 3.4 Recalcular en cada Planning

```text
Disponibilidad = suma de horas declaradas por los 12 integrantes para el sprint
Reuniones      = Σ(cantidad × duración en horas × participantes internos)
Base           = disponibilidad − reuniones − soporte conocido
Capacidad      = máximo(0, Base × 0,80)
```

Referencia por pareja: **≈ 114 h** (571 ÷ 5 parejas), no 571 h para cada una. Una persona en
reunión o trabajando en pareja no registra esas horas en otra tarea. Una ausencia no se compensa
con horas extra supuestas.

### 3.5 Puntos vs. horas

La capacidad va en **horas**. Los **puntos Fibonacci** de las HU en Taiga son otra escala
(esfuerzo relativo a la historia canónica) y **no se convierten** en horas: sirven para
planificar por **velocidad** una vez que el equipo tenga 2–3 sprints cerrados
([29 · §5–6](29-guia-catedra-historias-de-usuario.md)).

---

## 4. Primeras épicas

Diez épicas cubren el alcance de las tres fases. Cada historia `LLM-Sxx-Hyy` pertenece a una
épica: el **sprint** dice *cuándo* se construye y la **épica** *para qué sirve en el producto*.
Las épicas **no se estiman ni se comprometen**; se cierran cuando todas sus historias pasan la
DoD.

| Épica | Nombre | Resultado que habilita (→ *Objetivo* en Taiga) | Pareja | Sprints |
|---|---|---|---|---|
| **EP-01** | Plataforma, contratos e integración | El servicio arranca reproducible, expone `/api/llm/**` por Gateway, versiona su esquema y publica contratos | P1 | S1, S3, S6, S10, S19 |
| **EP-02** | AI Gateway, modelos y resiliencia | Toda llamada a un modelo pasa por un punto único con timeout, presupuesto, validación de salida y cambio por configuración | P2 | S3, S8, S9 |
| **EP-03** | Golden set y referencia humana | Un docente autorizado construye, puntúa y versiona el set de referencia que habilita calibrar | P5 + P4 | S1, S2 |
| **EP-04** | Calibración y gobernanza del modelo | No se activa un curso sin calibración dentro de tolerancia; el cambio de modelo se audita y dispara recalibración | P4 | S3, S4, S8 |
| **EP-05** | Tutor seguro y guardarraíles | El alumno recibe ayuda socrática dentro del desafío; jailbreak y fuga de solución se bloquean antes de mostrarse | P3 + P2 | S5 |
| **EP-06** | Evaluación, score y auditoría académica | Cerrar un intento produce un score desglosado, explicable y apelable; una caída difiere el cálculo sin perderlo | P4 + P1 | S6, S7 |
| **EP-07** | Operación, cuotas y observabilidad | El operador ve costo, cuotas y trabajos, recupera lo recuperable y sostiene la carga objetivo | P2 + P1 | S9, S10 |
| **EP-08** | Moderación integrada (F2) | Los mensajes del chat real se permiten o bloquean antes de entregarse, con revisión humana y retención acordada | P3 + P2 + P1 | S11–S13 |
| **EP-09** | RAG y consulta de material (F3) | El material docente autorizado es consultable con fuente y página; sin respaldo el asistente se abstiene | P5 + P3 | S14–S16 |
| **EP-10** | Personalización y agente (F3) | El alumno resuelve desafíos personalizados y menciona a `@agente`, con efectos idempotentes y salida moderada | P5 + P4 + P3 | S17–S18 |

> **En Taiga** cada épica se carga con el [template oficial](plantillas/epica-taiga.md):
> *Objetivo* (columna de arriba), *Suposiciones y Restricciones*, *Criterios de Aceptación a
> nivel épico* y *Dependencias / Impactos* (arrancan de [23 · §8](23-plan-construccion-producto-llm.md)).
> El template de épica **no** usa Como / Quiero / Para ni BDD.

---

## 5. Primeras historias de usuario (Sprint 1)

**Objetivo de S1:** un docente autorizado carga y consulta casos de referencia (golden set) y
los datos sobreviven al reinicio. **Trabajo estimado ~208 h** sobre 571 h de capacidad.

S1 es un sprint de arranque: **casi todo es habilitador técnico**. Solo **H05–H07** son **HU de
valor** en el sentido de [29 · §3](29-guia-catedra-historias-de-usuario.md) (un docente percibe
el resultado). **H01–H04, H08 y H09** fallan la **V** de INVEST: son **tareas de sprint**, no
HU. En Taiga:

- **H05, H06, H07** → **HU** con el [template oficial](plantillas/historia-de-usuario-taiga.md):
  Como/Quiero/Para con rol real (docente), BDD de camino feliz + 2 negativos, puntos Fibonacci.
- **H01–H04, H08, H09** → **tareas** bajo EP-01, sin formato Como/Quiero/Para ni puntos de valor.

Los **puntos Fibonacci** se asignan en el Sprint 0 con Planning Poker contra la historia canónica
(candidata: **H06**). La columna *Aceptación* es la forma comprimida; el BDD completo (≥ 3
escenarios) vive en la ficha de Taiga.

| ID | Como… / quiero… / para… | Aceptación (incluye negativa) | Tipo | Épica | Pareja | Dep. | h |
|---|---|---|---|---|---|---|---:|
| **H01** | Como equipo, quiero un ADR con el árbol de módulos y las convenciones técnicas, para construir todos sobre las mismas fronteras | ADR registrado: Java 21, Boot/Maven, límites de paquetes, variables de entorno. `domain` no depende de Spring, Kafka ni proveedores | Tarea | EP-01 | P1 | — | 16 |
| **H02** | Como desarrollador, quiero levantar todo el entorno con un comando, para trabajar sin instalar dependencias a mano | `docker compose up` deja Postgres + Eureka/Gateway (+ Kafka si aplica) *healthy*; `up`/health/`down` documentados. Sin Docker falla con mensaje claro | Tarea | EP-01 | P1 | H01 | 30 |
| **H03** | Como plataforma, quiero exponer el esqueleto transversal del servicio, para que los demás equipos integren contra algo estable | Registro Eureka `llm-service`, ruta `/api/llm/**`, M2M `aud=llm-service`, Problem Details, correlación, Actuator y CI en verde. Token sin scope → `401`; header de identidad falsificado → rechazado | Tarea | EP-01 | P1 | H02 | 34 |
| **H04** | Como plataforma, quiero el esquema inicial versionado, para que el dato académico nazca con auditoría | Flyway desde base vacía crea rúbrica, golden set, entrada, versión/auditoría e índice de idempotencia. Tabla append-only sin `UPDATE` destructivo; puntuación incompleta rechazada | Tarea | EP-01 | P1 | H03 | 38 |
| **H05** | Como docente autorizado, quiero dar de alta un golden set y cargar entradas, para armar la referencia de mi curso | Alta y carga vía Gateway; se valida actor, ownership de cohorte e idempotencia. Segunda carga con la misma `Idempotency-Key` no duplica. Docente de otra cohorte → `403` | **HU** | EP-03 | P5 | H04 | 24 |
| **H06** *(candidata a canónica)* | Como docente autorizado, quiero consultar mi golden set aunque el servicio se reinicie, para confiar en que el dato persiste | La consulta devuelve lo cargado tras `docker compose restart`; una cohorte ajena no aparece. Golden set inexistente → `404`, no `500` | **HU** | EP-03 | P5 | H04 | 14 |
| **H07** | Como docente, quiero una pantalla mínima para el alta, carga y consulta del golden set, sin depender de Swagger | Formulario/listado real vía Gateway, con estados de carga/error y autorización visible. La pantalla no llama al servicio directo: pasa por el Gateway | **HU** | EP-03 | P5 | H05, H06 | 24 |
| **H08** | Como equipo de integración, quiero el contrato OpenAPI y un mock del golden set publicados, para que `admin-service` avance sin el servicio real | Contrato con **solo** las operaciones existentes; mock levantable con una línea. Campo ausente se congela con `admin-service` antes de publicar | Tarea | EP-01 | P1 | H03 | 10 |
| **H09** | Como equipo, quiero la suite de pruebas y la guía de demo de S1, para validar la Review con evidencia y no con relato | Unitarias + Testcontainers/Flyway + WireMock del Gateway; reinicio de Compose probado; cobertura según [24](24-convenciones-cobertura.md). Guía de demo reproducible | Tarea | EP-01 | todos | H04–H07 | 18 |
| | | | | | | **Total** | **208** |

**Demo de S1:** un docente autorizado crea un golden set, carga una entrada y la consulta
después de reiniciar el servicio.

> El detalle de paquetes (orden de construcción, gates, pruebas) está en la receta de S1 del
> [backlog ejecutable](<../Plan de ejecucion/07-backlog-ejecutable-sprints.md>).

---

## 6. Glosario rápido

| Término | Definición |
|---|---|
| **Sprint 0** | Iteración de preparación sin compromiso de incremento: acuerdos, capacidad, backlog inicial y ambiente. |
| **DoR** | Condiciones para **empezar** una historia. Verifica el equipo con el referente de producto. |
| **DoD** | Condiciones para declarar **terminada** una historia o un incremento. Común a todas las historias. |
| **Criterio de aceptación** | Condición propia de **cada** historia, en escenarios BDD. Distinto de la DoD. |
| **Capacidad comprometible** | Horas-persona para entregables tras descontar reuniones y reserva: **~571 h/sprint**. |
| **Trabajo estimado** | Suma de los paquetes de la receta del sprint: **~208 h**. Es un piso, no el tope. |
| **Margen** | Diferencia capacidad − trabajo estimado (**~363 h/sprint**): re-estimación, imprevistos, coordinación. |
| **Épica** | Funcionalidad grande que no entra en un sprint; se divide en HU al acercarse. No se estima. |
| **Historia de usuario (HU)** | Unidad de valor entregable, con rol real, BDD y estimación en puntos. |
| **Tarea** | Paso técnico interno para completar una HU o habilitar el sprint. Sin Como/Quiero/Para. |
| **Historia canónica** | HU pequeña ya estimada contra la que se comparan todas las demás del backlog. |
