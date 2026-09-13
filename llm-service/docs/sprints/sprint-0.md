# Sprint 0 — Arranque del proyecto `llm-service`

> **Qué es.** La primera Planning ampliada. Deja acuerdos, backlog inicial refinado y
> ambiente reproducible **antes de S1**. **No produce incremento de software** ni consume la
> capacidad de entregables ([30 · §1](../30-arranque-agil-y-sprint-0.md)).
>
> **Qué NO se hace en Sprint 0.**
> - No se compromete ninguna historia a un sprint (eso es la Planning de S1).
> - No se asignan puntos Fibonacci al backlog… **salvo** estimar la **historia canónica**
>   ([29 · §5.3](../29-guia-catedra-historias-de-usuario.md)).
> - No se cargan las 24 h-persona de la primera Planning acá: van a **S1**
>   ([30 · §1.2](../30-arranque-agil-y-sprint-0.md)).
> - No se crean las ramas `feature/sNN/...`: nacen de `develop` al arrancar cada historia de S1.
> - **No se generan épicas.** Las épicas EP-01..EP-10 ya existen en [`../epicas/`](../epicas/README.md)
>   y son transversales a los 19 sprints; no se estiman ni pertenecen a un sprint. El Sprint 0
>   solo las **revisa**, mapea cada historia de S1 a una épica (§2 · #6) y carga en Taiga las que
>   tocan a S1 —EP-01 y EP-03— con el template oficial (§6). Las HU de S1 tampoco se redactan
>   acá: ya están, agrupadas por épica, en [`../historias/ep-01/`](../historias/ep-01/README.md) y
>   [`../historias/ep-03/`](../historias/ep-03/README.md) (índice del sprint en
>   [`s1-historias.md`](s1-historias.md)); el Sprint 0 las refina y las sube.
>
> **Fuente de verdad del checklist:**
> [`35` · «Sprint 0»](../35-backlog-ejecutable.md).
> Este archivo es el **acta**: se completa el día del Sprint 0 y se guarda como evidencia.

---

## 1. Identificación

| Campo | Valor a completar |
|---|---|
| Fecha del Sprint 0 | |
| Fecha prevista de inicio de S1 | |
| Integrantes presentes (de 12) | |
| Facilitador de la sesión | |
| Referente de producto | |
| Carpeta de evidencia del equipo | |

---

## 2. Checklist de salida

La casilla se marca **solo con su evidencia** (enlace a acta, PR, captura, planilla).

| #   | Ítem                                                                                                                                                                             | Evidencia | Estado |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | ------ |
| 1   | Fecha de inicio, integrantes y **disponibilidad individual declarada** para el ciclo completo                                                                                    |           | ☐      |
| 2   | Nombres de **P1–P5** (10 integrantes), **referente de producto** y **facilitador** (los otros 2), y suplencias                                                                   |           | ☐      |
| 3   | **Cálculo de capacidad de S1** hecho con la fórmula de §4 desde la planilla de disponibilidad y registrado en una copia de la [plantilla de sprint](../plantillas/sprint-llm.md) |           | ☐      |
| 4   | **DoR y DoD** ([23 · §9.2](../23-plan-construccion-producto-llm.md)) leídas y aceptadas por el equipo                                                                            |           | ☐      |
| 5   | **Historia canónica** elegida y estimada (§5). Candidata: `LLM-S01-H06`                                                                                                          |           | ☐      |
| 6   | Épicas revisadas; cada historia de S1 asignada a una épica ([../epicas/](../epicas/README.md))                                                                                   |           | ☐      |
| 7   | Épicas e historias de S1 cargadas en el backlog de Taiga con los templates oficiales; **permalinks anotados** (§6)                                                               |           | ☐      |
| 8   | **D01** y **D02** con responsable y estado comprobado; **D03/D04** solicitadas con fecha (§7)                                                                                    |           | ☐      |
| 9   | Requisitos locales verificados: `git --version`, `docker --version`, `docker compose version`, `java -version` → **Java 21** (§8)                                                |           | ☐      |
| 10  | Remoto `tpi-llm` con `main` y `develop` creadas y **protegidas** (PR obligatorio, CI en verde, sin push directo ni force-push) según [GITFLOW](../GITFLOW.md) (§8)               |           | ☐      |
| 11  | Carpeta de evidencia del equipo acordada (PR, CI, migración, comandos Docker, demo)                                                                                              |           | ☐      |
| 12  | Cada historia de S1 cumple la DoR: usuario, aceptación negativa, responsable/suplente y estimación dentro de la capacidad                                                        |           | ☐      |
| 13  | La estimación de S1 incluye esqueleto, migraciones, contrato, interfaz, pruebas e integración —no solo el «camino feliz»                                                         |           | ☐      |
| 14  | Criterio de **demo de S1** acordado: acceso autorizado, carga, consulta y recuperación tras reinicio                                                                             |           | ☐      |

---

## 3. Equipo — parejas y roles

Los identificadores P1–P5 no suponen personas ya asignadas ([23 · §3](../23-plan-construccion-producto-llm.md)).

| Pareja | Responsabilidad principal | Integrantes | Suplente |
|---|---|---|---|
| **P1** · Plataforma e integración | Contratos, infra compartida, migraciones transversales, Kafka, CI/CD, operación | | |
| **P2** · Modelos y resiliencia | AI Gateway, adaptadores, cuotas, validación de salidas, costos | | |
| **P3** · Asistencia y seguridad | Tutor, guardarraíles, interfaz de asistencia | | |
| **P4** · Evaluación y auditoría | Rúbrica, evaluador, calibración, revisión humana, calidad | | |
| **P5** · Contenido y conocimiento | Golden set, interfaces docentes, ingesta/RAG | | |
| **Referente de producto** | Voz del cliente, prioriza el backlog, valida DoR/DoD | | — |
| **Facilitador** | Facilita ceremonias, cuida la capacidad y los bloqueos | | — |

---

## 4. Cálculo de capacidad de S1

El sprint dura **2 semanas**. La disponibilidad sale de las **horas declaradas por cada
integrante** en la planilla del equipo, no de un supuesto ([30 · §3](../30-arranque-agil-y-sprint-0.md)).

```text
Disponibilidad = suma de horas declaradas por los 12 integrantes para el sprint
Reuniones      = Σ(cantidad × duración en horas × participantes internos)
Base           = Disponibilidad − Reuniones − Soporte conocido
Reserva        = 20 % de la Base
Capacidad comprometible = máximo(0, Base × 0,80)
```

| Concepto | Referencia inicial | Valor real de S1 |
|---|---:|---:|
| Disponibilidad nominal (408 h/sem × 2) | 816 h | |
| − Reuniones programadas (§4.1) | −102 h | |
| − Soporte conocido | según sprint | |
| = Base | 714 h | |
| − Reserva (20 % de la Base) | −143 h | |
| **= Capacidad comprometible en entregables** | **≈ 571 h** | |

> El trabajo estimado de la receta de S1 es **~208 h de paquetes** (piso, no tope). La
> diferencia con la capacidad es **margen** para re-estimar en Planning, imprevistos y
> coordinación de 12 personas.

### 4.1 Reuniones del ciclo (referencia: 102 h-persona)

| Reunión | Cant. | Duración | Participantes | Horas-persona |
|---|---:|---:|---:|---:|
| Sprint Planning | 1 | 120 min | 12 | 24 |
| Daily / sincronización (incl. coordinación de dependencias) | 4 | 45 min | 12 | 36 |
| Sprint Review con demo | 1 | 90 min | 12 | 18 |
| Retrospectiva | 1 | 60 min | 12 | 12 |
| Refinamiento del backlog | 2 | 30 min | 12 | 12 |
| **Total** | | | | **102** |

> La Planning de S1 se carga a **S1**, aunque se haga el día previo al inicio operativo.

---

## 5. Historia canónica

La historia de referencia contra la que se estiman **todas** las demás del backlog. Debe ser
**pequeña, entendida por todos y con un recorrido completo** ([29 · §5.3](../29-guia-catedra-historias-de-usuario.md)).

| Campo | Valor |
|---|---|
| Historia elegida | `LLM-S01-H06` — *Consulta del golden set que sobrevive al reinicio* *(candidata; confirmar)* |
| Por qué es buena canónica | Recorrido completo (autorización + lectura + persistencia), chica, la entienden las cinco parejas |
| Puntos asignados (convención interna) | *(fijar en la sesión: p. ej. 2)* |
| Registrada en | Ficha de Taiga de `LLM-S01-H06` + [`../historias/ep-03/h06.md`](../historias/ep-03/h06.md) |

> El valor en puntos es una **convención del equipo**; no se compara con otros equipos ni se
> convierte a horas ([30 · §3.5](../30-arranque-agil-y-sprint-0.md)).

---

## 6. Backlog inicial en Taiga

| Ítem | Tipo en Taiga | Template | Permalink |
|---|---|---|---|
| EP-01 · Plataforma, contratos e integración | Epic | [`epicas/ep-01.md`](../epicas/ep-01.md) | |
| EP-03 · Golden set y referencia humana | Epic | [`epicas/ep-03.md`](../epicas/ep-03.md) | |
| `LLM-S01-H01` · ADR de arquitectura | Tarea (EP-01) | [`historias/ep-01/h01.md`](../historias/ep-01/h01.md) · [tareas](../tareas/ep-01/h01.md) | |
| `LLM-S01-H02` · Entorno reproducible | Tarea (EP-01) | [`historias/ep-01/h02.md`](../historias/ep-01/h02.md) · [tareas](../tareas/ep-01/h02.md) | |
| `LLM-S01-H03` · Esqueleto transversal | Tarea (EP-01) | [`historias/ep-01/h03.md`](../historias/ep-01/h03.md) · [tareas](../tareas/ep-01/h03.md) | |
| `LLM-S01-H04` · Esquema versionado con auditoría | Tarea (EP-01) | [`historias/ep-01/h04.md`](../historias/ep-01/h04.md) · [tareas](../tareas/ep-01/h04.md) | |
| `LLM-S01-H05` · Alta de golden set y carga de entradas | **HU** (EP-03) | [`historias/ep-03/h05.md`](../historias/ep-03/h05.md) · [tareas](../tareas/ep-03/h05.md) | |
| `LLM-S01-H06` · Consulta que sobrevive al reinicio | **HU** (EP-03) · **canónica** | [tareas](../tareas/ep-03/h06.md) | |
| `LLM-S01-H07` · Pantalla docente mínima | **HU** (EP-03) | [tareas](../tareas/ep-03/h07.md) | |
| `LLM-S01-H08` · Contrato OpenAPI y mock | Tarea (EP-01) | [`historias/ep-01/h08.md`](../historias/ep-01/h08.md) · [tareas](../tareas/ep-01/h08.md) | |
| `LLM-S01-H09` · Suite de pruebas y guía de demo | Tarea (EP-01) | [`historias/ep-01/h09.md`](../historias/ep-01/h09.md) · [tareas](../tareas/ep-01/h09.md) | |

> **H05–H07** se cargan como **HU** (rol real: docente; BDD camino feliz + 2 negativos;
> puntos Fibonacci contra la canónica; INVEST verificado). **H01–H04, H08, H09** como
> **tareas** bajo EP-01 (fallan la **V** de INVEST). Detalle en
> [30 · §5](../30-arranque-agil-y-sprint-0.md).

---

## 7. Dependencias externas

| ID | Dependencia | Referente LLM | Necesaria para | Fecha requerida | Responsable contraparte | Estado |
|---|---|---|---|---|---|---|
| **D01** | Identidad, Gateway, Eureka, ambiente y frontend compartido | P1 | S1 (primeros días) | | | ☐ comprobado |
| **D02** | Docentes responsables, set base y **cinco dimensiones** de la rúbrica | P5 + P4 | S1 (nombrar); S2–S4 (referencias de curso) | | | ☐ comprobado |
| **D03** | Contexto, solución esperada y metadata de práctica | P3 | Contrato en S1; integración antes de S5 | | | ☐ solicitada con fecha |
| **D04** | Cierre de intento, efectos académicos y pendientes | P1 + P4 | Contrato en S1; integración antes de S6 | | | ☐ solicitada con fecha |

> Un mock acordado habilita trabajo interno; **no** cierra una historia cuya demo exige
> integración real ([23 · §8](../23-plan-construccion-producto-llm.md)).

---

## 8. Ambiente y Git

### 8.1 Requisitos locales (cada integrante)

| Comando | Resultado esperado | OK |
|---|---|---|
| `git --version` | ≥ 2.30 | ☐ |
| `docker --version` | Docker Engine instalado | ☐ |
| `docker compose version` | Compose v2 | ☐ |
| `java -version` | **Java 21** (LTS) | ☐ |

### 8.2 Remoto y ramas protegidas

- [ ] Repo remoto `tpi-llm` creado.
- [ ] Ramas `main` y `develop` creadas.
- [ ] Protección en ambas: **PR obligatorio**, **CI en verde** para mergear, **sin push
  directo**, **sin force-push** ([GITFLOW](../GITFLOW.md), [flujo diario y Git](../WORKFLOW_DIARIO.md)).
- [ ] Las ramas `feature/sNN/llm-sNN-hNN-<slug>` **no** se crean en Sprint 0: nacen de
  `develop` al arrancar cada historia de S1.

---

## 9. Criterio de demo de S1 (acordado)

> Un **docente autorizado** crea un golden set para su cohorte, carga una entrada con
> transcripción y las cinco puntuaciones de referencia, y la **consulta después de reiniciar
> el servicio** (`docker compose restart`), obteniendo exactamente lo que cargó.
> La demo corre en un **ambiente integrado**, no en una máquina local aislada.

**Aceptación negativa mínima:** token sin rol, segunda `Idempotency-Key` igual, rúbrica
inválida y reinicio no crean ni duplican datos.

---

## 10. Cierre del Sprint 0

- [ ] Los 14 ítems del §2 están marcados con evidencia.
- [ ] El acta (este archivo) se guardó en la carpeta de evidencia del equipo.
- [ ] La Planning de S1 tiene fecha y la capacidad calculada en §4 queda como su punto de partida.

| | Nombre | Fecha |
|---|---|---|
| Cierre validado por (referente de producto) | | |
| Cierre validado por (facilitador) | | |
