# Backlog ejecutable S0–S19

Cada bloque de S1 en adelante es una receta de implementación. Las horas de cada paquete son el **trabajo estimado** (desarrollo, pruebas, revisión, documentación y demo; no reuniones ni reserva) y suman **~208 h por sprint**. Esa cifra es una estimación gruesa anterior a los contratos, **no el tope**: la **capacidad del sprint es 571 h** ([23 · §2.1](../docs/23-plan-construccion-producto-llm.md)) y la diferencia (**~363 h**) es margen para re-estimar en Planning, imprevistos y coordinación de 12 personas. Reestimar en cada Planning según disponibilidad real. Leer antes el [playbook](06-playbook-de-construccion.md).

Este documento tiene tres capas: las **épicas** (para qué producto), el **Sprint 0** (qué se deja listo antes de construir) y las **recetas S1–S19** (cómo se construye cada incremento). La DoR y la DoD únicas están en [23 · §9.2](../docs/23-plan-construccion-producto-llm.md); el cálculo de capacidad, en [23 · §2](../docs/23-plan-construccion-producto-llm.md). La **vista de entrega** que reúne Sprint 0, DoD, capacidad, épicas e historias de S1 con el formato de la guía de cátedra es [30](../docs/30-arranque-agil-y-sprint-0.md).

---

## Épicas

Diez épicas cubren el alcance de las tres fases. Cada historia `LLM-Sxx-Hyy` pertenece a una épica: el **sprint** dice *cuándo* se construye y la **épica** *para qué sirve en el producto*. Las épicas no se estiman ni se comprometen; se cierran cuando todas sus historias pasan la DoD.

| Épica | Nombre | Resultado que habilita | Pareja líder | Sprints | Requisitos (orientativo) |
|---|---|---|---|---|---|
| **EP-01** | Plataforma, contratos e integración | El servicio arranca reproducible, expone `/api/llm/**` por Gateway, versiona su esquema y publica contratos que los demás equipos consumen | P1 | S1, S3, S6, S10, S19 | RF-NFR-01/03/04/09/10; contratos v1 |
| **EP-02** | AI Gateway, modelos y resiliencia | Toda llamada a un modelo pasa por un punto único con timeout, presupuesto, validación de salida y cambio de modelo por configuración | P2 | S3, S8, S9 | RF-IA-22/23/24/35; RF-IA-27 |
| **EP-03** | Golden set y referencia humana | Un docente autorizado construye, puntúa y versiona el set de referencia que habilita calibrar | P5 + P4 | S1, S2 | RF-IA-29/30 a 36 |
| **EP-04** | Calibración y gobernanza del modelo | No se activa un curso sin calibración dentro de tolerancia; el cambio de modelo se audita y dispara recalibración | P4 | S3, S4, S8 | RF-IA-30 a 36; PAR-14 |
| **EP-05** | Tutor seguro y guardarraíles | El alumno recibe ayuda socrática dentro del desafío; jailbreak y fuga de solución se bloquean antes de mostrarse | P3 + P2 | S5 | RF-IA-01/02/04/19/20 |
| **EP-06** | Evaluación, score y auditoría académica | Cerrar un intento produce un score desglosado, explicable y apelable; una caída difiere el cálculo sin perderlo | P4 + P1 | S6, S7 | RF-IA-12 a 18/25; RF-IA-27/34 |
| **EP-07** | Operación, cuotas y observabilidad | El operador ve costo, cuotas y trabajos, recupera lo recuperable y sostiene la carga objetivo | P2 + P1 | S9, S10 | RF-IA-22/25/33; RF-NFR-03/04 |
| **EP-08** | Moderación integrada (F2) | Los mensajes del chat real se permiten o bloquean antes de entregarse, con revisión humana y retención acordada | P3 + P2 + P1 | S11–S13 | RF-CHT-09 a 14 |
| **EP-09** | RAG y consulta de material (F3) | El material docente autorizado es consultable con fuente y página; sin respaldo el asistente se abstiene | P5 + P3 | S14–S16 | RF-IA-06/07/08 |
| **EP-10** | Personalización y agente (F3) | El alumno resuelve desafíos personalizados y menciona a `@agente`, con efectos idempotentes y salida moderada | P5 + P4 + P3 | S17–S18 | RF-DES-05; RF-CHT-05/08 |

> Los números de RF son orientativos. La traza fina historia → requisito se mantiene en [21 · matriz de trazabilidad](../docs/21-matriz-trazabilidad-llm.md). Las parejas P1–P5 se definen en [23 · §3](../docs/23-plan-construccion-producto-llm.md).

> **Cómo se pasa cada épica al template de Taiga.** La columna *Resultado que habilita* de esta tabla es el **Objetivo** del [template de Épica](../docs/plantillas/epica-taiga.md); *Sprints* y *Pareja líder* son contexto de planificación; los apartados *Suposiciones y Restricciones*, *Criterios de Aceptación a nivel Épico* y *Dependencias / Impactos* se completan en la ficha (las dependencias arrancan de [23 · §8](../docs/23-plan-construccion-producto-llm.md)). El template de épica **no** usa Como / Quiero / Para ni BDD —a diferencia de lo que sugiere [29 · §4](../docs/29-guia-catedra-historias-de-usuario.md) para épicas en general—: para la entrega manda el template oficial.

> **Publicación en Taiga.** Este documento es la versión de trabajo. Las épicas y las HU se cargan además en el backlog de Taiga con los *Templates de Épica y de Historia de Usuario* de la Wiki, y se referencian desde las páginas de la Wiki por su **enlace permanente** (ver [27 · Guía de la Wiki](../docs/27-guia-wiki-taiga.md)). El método para redactarlas y estimarlas es [29 · Guía de cátedra: Historias de Usuario](../docs/29-guia-catedra-historias-de-usuario.md).

---

## Sprint 0 — Arranque del proyecto

Sprint 0 **no produce incremento de software** ni consume la capacidad de entregables: es la primera Planning ampliada, con acuerdos y preparación de ambiente. Ocurre **antes de S1** y deja cumplida la DoR de todas las historias de S1.

### Qué deja cerrado (checklist de salida)

- [ ] Fecha de inicio, integrantes y **disponibilidad individual declarada** para el ciclo completo.
- [ ] Nombres de P1–P5 (10 integrantes), referente de producto y facilitador (los otros 2), y suplencias.
- [ ] **Cálculo de capacidad de S1** hecho con la fórmula de abajo a partir de la planilla de disponibilidad y registrado en una copia de la [plantilla de sprint](../docs/plantillas/sprint-llm.md).
- [ ] **DoR y DoD** ([23 · §9.2](../docs/23-plan-construccion-producto-llm.md)) leídas y aceptadas por el equipo.
- [ ] **Historia canónica** del equipo elegida y estimada ([29 · §5.3](../docs/29-guia-catedra-historias-de-usuario.md)): una HU pequeña, entendida por todos, que fija la referencia de puntos para todo el backlog. Candidata natural: `LLM-S01-H06` (consultar golden set tras reinicio).
- [ ] Épicas revisadas; cada historia de S1 asignada a una épica (ver tabla de S1).
- [ ] Épicas e historias de S1 cargadas en el backlog de Taiga con los templates oficiales ([HU](../docs/plantillas/historia-de-usuario-taiga.md) con BDD de camino feliz + 2 negativos, puntos Fibonacci contra la canónica e INVEST verificado; [épica](../docs/plantillas/epica-taiga.md) con objetivo y CA a nivel épico); permalinks anotados para referenciarlas en la Wiki.
- [ ] D01 (identidad, Gateway, Eureka, frontend compartido) y D02 (docentes, set base y cinco dimensiones) con responsable y estado comprobado; D03/D04 solicitadas con fecha.
- [ ] Requisitos locales verificados: `git --version`, `docker --version`, `docker compose version`, `java -version` → Java 21.
- [ ] Remoto `tpi-llm` con `main` y `develop` creadas y protegidas (PR obligatorio, CI, sin push directo ni force-push) según [GITFLOW](../docs/GITFLOW.md) y el [flujo diario y Git](05-flujo-diario-y-git.md). Las `feature/sNN/llm-sNN-hNN-<slug>` nacen de `develop` al arrancar cada historia de S1, no en Sprint 0.
- [ ] Carpeta de evidencia del equipo acordada (PR, CI, migración, comandos Docker, demo).
- [ ] Cada historia de S1 cumple la DoR: usuario, aceptación negativa, responsable/suplente y estimación dentro de la capacidad calculada.
- [ ] La estimación de S1 incluye adaptación del esqueleto, migraciones, contrato, interfaz, pruebas e integración —no solo el "camino feliz".
- [ ] Criterio de demo de S1 acordado: acceso autorizado, carga, consulta y recuperación tras reinicio.

### Cálculo de capacidad — resumen

| Paso | Fórmula | Referencia inicial |
|---|---|---:|
| Disponibilidad nominal | 408 h/semana declaradas × 2 semanas | 816 h |
| − Reuniones del ciclo | Σ(cantidad × duración × participantes internos), 12 personas | −102 h |
| − Soporte conocido | lo que ya se sabe que consumirá tiempo | según sprint |
| = Base | disponibilidad − reuniones − soporte | 714 h |
| − Reserva | 20 % de la base | −143 h |
| **= Capacidad comprometible** | máximo(0, Base × 0,80) | **≈ 571 h** |

El desglose de las 102 h de reuniones y la regla de **recalcular en cada Planning** están en [23 · §2](../docs/23-plan-construccion-producto-llm.md); no se repiten acá. El trabajo estimado de las recetas (~208 h/sprint) es un **piso**, no el tope: la diferencia con las 571 h es margen. El número real de S1 sale de la planilla de disponibilidad y es el que manda.

Esta capacidad está en **horas**; es la que usa el plan para dimensionar cada sprint. Los **puntos Fibonacci** de las HU en Taiga son otra escala (esfuerzo relativo a la canónica) y **no se convierten** en horas: sirven para planificar por **velocidad** una vez que el equipo tenga dos o tres sprints cerrados ([29 · §5.4 y §6](../docs/29-guia-catedra-historias-de-usuario.md)).

---

## S1 — Base operable y golden set (~208 h estimadas)

**No iniciar sin:** responsables de `admin-service`, Gateway/Eureka, PostgreSQL local y definición docente inicial de las cinco dimensiones. **Demo:** docente autorizado crea un golden set, carga una entrada y la consulta tras reiniciar.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | ADR técnico y árbol de módulos | 16 | Java 21, Boot/Maven, límites de paquetes, variables de entorno y decisiones registradas. |
| 2 | Entorno reproducible | 30 | Compose con Postgres, Kafka, Eureka/Gateway según plataforma; `up`, health y `down` documentados. |
| 3 | Esqueleto transversal | 34 | Nombre Eureka, `/api/llm`, security M2M, Problem Details, correlación, Actuator y pipeline CI. |
| 4 | Migración y dominio | 38 | Flyway crea rúbrica, golden set, entrada, versión/auditoría e índice de idempotencia desde DB vacía. |
| 5 | API golden set v1 | 38 | Implementar únicamente operaciones existentes del OpenAPI; validar actor/ownership/idempotencia. |
| 6 | Interfaz docente mínima | 24 | Formulario/listado real vía Gateway, estados de carga/error y autorización visible. |
| 7 | Pruebas y demo | 28 | Unitarias, Testcontainers/Flyway, WireMock Gateway, reinicio de Compose y guía de demo. |

**Gates:** no copiar `/ai/*` del ejemplo histórico; congelar cualquier campo ausente con `admin-service` antes de publicarlo. **Aceptación negativa:** token sin rol, segunda clave igual, rúbrica inválida y reinicio no crean/duplican datos.

### Historias de usuario de S1

Derivadas de los paquetes de arriba. Épica **EP-01** (H01–H04, H08, H09) y **EP-03** (H05–H07). Las horas ya incluyen backend, interfaz, pruebas, revisión, integración y demo, y suman las ~208 h de trabajo estimado del sprint (sobre 571 h de capacidad).

S1 es un sprint de arranque: **casi todo es habilitador técnico**. Sólo H05–H07 son **HU de valor** en el sentido de [29 · §3](../docs/29-guia-catedra-historias-de-usuario.md) (un docente percibe el resultado). H01–H04, H08 y H09 fallan la **V** de INVEST: son **tareas de sprint** con enunciado en primera persona del equipo, no HU de valor. Para el backlog de Taiga:

- **H05, H06, H07** se cargan como **HU** con el [template oficial](../docs/plantillas/historia-de-usuario-taiga.md): Como/Quiero/Para con rol real (docente), BDD de camino feliz + 2 negativos, puntos Fibonacci.
- **H01–H04, H08, H09** se cargan como **tareas** bajo EP-01 (o bajo una HU habilitadora «Base operable del servicio»), sin exigirles formato COMO/QUIERO/PARA ni puntos de valor.

La columna **h** es la referencia de planificación del plan. Los **puntos Fibonacci** de las HU se asignan en el Sprint 0 con Planning Poker contra la historia canónica (candidata: **H06**); no se pre-cargan acá para no inventar estimaciones. La columna *Aceptación* es la forma comprimida de trabajo; el BDD completo (≥ 3 escenarios) vive en la ficha de Taiga.

| ID | Como… / quiero… / para… | Aceptación (incluye negativa) | Épica | Pareja | Dep. | h |
|---|---|---|---|---|---|---:|
| **LLM-S01-H01** | Como equipo, quiero un ADR con el árbol de módulos y las convenciones técnicas para construir todos sobre las mismas fronteras | ADR registrado: Java 21, Boot/Maven, límites de paquetes (`api`/`application`/`domain`/`infrastructure`/`security`/`configuration`), variables de entorno. `domain` no depende de Spring, Kafka ni proveedores | EP-01 | P1 | — | 16 |
| **LLM-S01-H02** | Como desarrollador, quiero levantar todo el entorno con un comando para trabajar sin instalar dependencias a mano | `docker compose up` deja Postgres + Eureka/Gateway (+ Kafka si la plataforma lo pide) *healthy*; `up`, health y `down` documentados. Sin Docker el comando falla con mensaje claro, no a medias | EP-01 | P1 | H01 | 30 |
| **LLM-S01-H03** | Como plataforma, quiero exponer el esqueleto transversal del servicio para que los demás equipos integren contra algo estable | Registro Eureka `llm-service`, ruta `/api/llm/**` sin reescritura, M2M con `aud=llm-service`, Problem Details, correlación `traceparent`/`X-Request-Id`, Actuator y pipeline CI en verde. Token sin scope → `401`; header de identidad falsificado → rechazado | EP-01 | P1 | H02 | 34 |
| **LLM-S01-H04** | Como plataforma, quiero el esquema inicial versionado para que el dato académico nazca con auditoría | Flyway desde base vacía crea rúbrica, golden set, entrada, versión/auditoría e índice de idempotencia. Migración reproducible; tabla append-only no admite `UPDATE` destructivo; una puntuación incompleta se rechaza | EP-01 | P1 | H03 | 38 |
| **LLM-S01-H05** | Como docente autorizado, quiero dar de alta un golden set y cargar entradas para armar la referencia de mi curso | Alta y carga vía `/api/llm/**` por Gateway; se valida actor, ownership de la cohorte e idempotencia. Segunda carga con la misma `Idempotency-Key` no duplica. Docente de otra cohorte → `403` | EP-03 | P5 | H04 | 24 |
| **LLM-S01-H06** *(candidata a canónica)* | Como docente autorizado, quiero consultar mi golden set y sus entradas aunque el servicio se reinicie, para confiar en que el dato persiste | La consulta devuelve lo cargado tras `docker compose restart`; una cohorte ajena no aparece en la respuesta. Golden set inexistente → `404`, no `500` | EP-03 | P5 | H04 | 14 |
| **LLM-S01-H07** | Como docente, quiero una pantalla mínima para el alta, la carga y la consulta del golden set, sin depender de Swagger | Formulario/listado real vía Gateway, con estados de carga y error y la autorización visible. La pantalla no llama al servicio directo: pasa por el Gateway | EP-03 | P5 | H05, H06 | 24 |
| **LLM-S01-H08** | Como equipo de integración, quiero el contrato OpenAPI y un mock del golden set publicados para que `admin-service` avance sin el servicio real | Contrato en el repo con **solo** las operaciones existentes; mock levantable con una línea documentada. Cualquier campo ausente se congela con `admin-service` antes de publicarlo | EP-01 | P1 | H03 | 10 |
| **LLM-S01-H09** | Como equipo, quiero la suite de pruebas y la guía de demo de S1 para que la Review se valide con evidencia y no con relato | Unitarias + Testcontainers/Flyway + WireMock del Gateway; reinicio de Compose probado; cobertura según [24](../docs/24-convenciones-cobertura.md). Guía de demo reproducible: acceso autorizado → carga → consulta tras reinicio | EP-01 | todos | H04–H07 | 18 |
| | | | | | **Total** | **208** |

**Demo de S1:** un docente autorizado crea un golden set, carga una entrada y la consulta después de reiniciar el servicio.

## S2 — Referencia humana versionada (~208 h estimadas)

**No iniciar sin:** S1 integrado y protocolo docente de doble puntuación. **Demo:** dos docentes puntúan, resuelven diferencia y recuperan/exportan versión publicada.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Modelo append-only | 34 | Puntuación individual, resolución, publicación y snapshot con autor/fecha/rúbrica; prohibir update destructivo. |
| 2 | Casos de uso y reglas | 38 | Dos puntuaciones independientes; detectar diferencias por dimensión; solo rol autorizado resuelve. |
| 3 | API/contrato acordado | 30 | Completar adenda necesaria con admin; mocks y pruebas de compatibilidad. |
| 4 | Interfaz de doble ciego | 40 | Docente no ve puntuación ajena hasta enviar; comparador y motivo de resolución. |
| 5 | Exportación/recuperación | 24 | Exporta versión inmutable y restaura lectura, nunca pisa referencias publicadas. |
| 6 | Calidad y demo | 42 | Pruebas de concurrencia, acceso cruzado, auditoría, migración y recorrido real. |

**Aceptación negativa:** un docente no edita su nota publicada ni consulta una cohorte ajena; discrepancia sin resolución no habilita publicación.

## S3 — Calibración de plataforma (~208 h estimadas)

**No iniciar sin:** S2 con golden set publicado, credenciales de proveedor administradas fuera del repo y tolerancias PAR-14 confirmadas. **Demo:** ADMIN inicia runner, consulta job/reporte y un modelo fuera de tolerancia no se habilita.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Puerto AI Gateway y fake | 32 | Interfaz proveedor, `RestClient`, fake/WireMock, timeout y schema estricto de salida. |
| 2 | Catálogo/asignación/auditoría | 28 | Modelo, versión, función, costo/latencia y habilitación separados de configuración de secretos. |
| 3 | Jobs durables | 42 | Migración, lease, reintentos, endpoint de estado y recuperación tras reinicio. |
| 4 | Evaluación y métrica PAR-14 | 42 | Transcripción como dato; promedio y dimensión; evidencia modelo/prompt/rúbrica. |
| 5 | API, permisos y reporte | 28 | `POST /calibrations`, `GET /jobs`; ADMIN autorizado; reporte explicable. |
| 6 | Pruebas y demo | 36 | Aprobado, rechazado, provider timeout, job duplicado/reiniciado y costo registrado. |

**Aceptación negativa:** no hay fallback automático de modelo; fallo no habilita; prompt no interpreta transcript como instrucción.

## S4 — Calibración por curso y bloqueo real (~208 h estimadas)

**No iniciar sin:** S3 aprobado y compromiso de `courses-service` sobre consulta/bloqueo. **Demo:** curso no calibrado no activa; uno aprobado activa mediante integración real.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Extensión de set de cohorte | 34 | Referencias específicas sin cambiar pesos/dimensiones globales. |
| 2 | Runner por cohorte | 36 | Reutiliza motor S3; estado, evidencia y vencimiento por curso. |
| 3 | Consulta contractual | 32 | `GET /course-cohorts/{id}/calibration`, autorización y respuesta acordada con cursos. |
| 4 | Integración y avisos | 38 | Courses bloquea inclusive ADMIN; notificación/alerta de inicio sin calibrar. |
| 5 | Interfaz docente/admin | 24 | Iniciar, seguir estado, ver tolerancias y evidencia. |
| 6 | Pruebas y demo | 44 | Cohorte ajena, expiración, job duplicado, integración Gateway y bloqueo extremo a extremo. |

## S5 — Tutor seguro (~208 h estimadas)

**No iniciar sin:** contexto validado y canal separado de solución desde `practice-service`, corpus mínimo de ataque y política de cuota. **Demo:** alumno recibe ayuda socrática; fuga/jailbreak bloquean; caída no impide continuar.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Adenda con práctica | 22 | Contexto, riesgo, ownership, idempotencia, evidencia de indisponibilidad y solución solo para guardia. |
| 2 | Dominio/interacción/cuota | 34 | Estados `completed/blocked/unavailable`, metadata, límite por alumno y auditoría. |
| 3 | Guardia de entrada | 28 | Filtros deterministas, separación de datos, clasificador de intención y respuesta segura. |
| 4 | Orquestación tutor | 36 | Prompt con contexto mínimo, AI Gateway, timeout/circuito y respuesta socrática. |
| 5 | Guardia de salida | 36 | AST/similitud contra solución fuera de prompt; bloquear/regenerar y registrar incidente. |
| 6 | UI práctica y degradación | 20 | Estados claros, no explica evasión, permite continuar sin tutor. |
| 7 | Seguridad/pruebas/demo | 32 | Corpus jailbreak, fuga simulada, cuota 429, ownership, timeout y trazabilidad. |

**Aceptación negativa:** solución nunca llega al prompt/log; alto riesgo no devuelve streaming plaintext; respuesta bloqueada no llega al navegador.

## S6 — Evaluación asíncrona y diferida (~208 h estimadas)

**No iniciar sin:** S3, contrato `intento_cerrado.v1`, consumidor `challenges-service` y regla de cierre de cursos. **Demo:** cierre produce score; con proveedor caído la entrega se acepta y el score llega una sola vez después.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Consumidor/dedupe | 34 | Envelope, headers, `eventId` y `attemptId` únicos; rechazo seguro de versión inválida. |
| 2 | Evaluador y evidencia | 38 | Dimensiones, agregado, confianza, justificaciones y versión de rúbrica/modelo/prompt. |
| 3 | Outbox/resultados | 38 | `score_de_ia_calculado.v1` atómico, reintento sin doble evento. |
| 4 | Diferido/recuperación | 34 | `score_pendiente_diferido.v1`, job durable y reanudación. |
| 5 | Consulta pendientes + cursos | 24 | Conteo contractual y bloqueo de archivado por el dueño. |
| 6 | UI/observabilidad | 16 | Score/pendiente visible y estado operativo. |
| 7 | Pruebas/demo | 24 | Duplicado, caída, reinicio, outbox, no XP y recorrido integrado. |

## S7 — Apelación y override auditables (~208 h estimadas)

**No iniciar sin:** S6 y decisión de negocio sobre propagación de revisión. **Demo:** alumno apela, docente ve evidencia y resuelve; original y cambio son recuperables.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Datos append-only | 32 | Apelación, evidencia congelada, override y auditoría con original/nuevo. |
| 2 | API y ownership | 32 | Endpoints v1, idempotencia, alumno solo propio; docente solo cohorte autorizada. |
| 3 | Bandeja docente | 38 | Filtros por baja confianza/impacto, transcripción, dimensiones, versiones y motivo. |
| 4 | Propagación a negocio | 32 | Evento/contrato acordado, outbox y efecto una vez en dueño. |
| 5 | Política/muestreo | 22 | Priorización documentada, sin recalcular pasado automáticamente. |
| 6 | Pruebas/demo | 52 | Apelación repetida, acceso indebido, conflicto, auditoría y actualización real. |

## S8 — Cambio de modelo y deriva (~208 h estimadas)

**No iniciar sin:** S3/S4 y segundo adaptador o simulador compatible. **Demo:** ADMIN cambia modelo habilitado, conoce cohortes afectadas y recibe resultado de recalibración.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Política de asignación | 30 | Un evaluador activo, elegibilidad solo tras calibración y bitácora inmutable. |
| 2 | Adaptador portable | 34 | Segundo adaptador/fake que prueba aislamiento de proveedor. |
| 3 | Recalibración/deriva | 44 | Programada y por cambio; compara resultados, no reescribe históricos. |
| 4 | Cohortes afectadas/alertas | 32 | Reporte, señal a responsables y estado verificable. |
| 5 | API/UI admin | 24 | Cambio auditado, motivo, revisión de impacto. |
| 6 | Pruebas/demo | 44 | Modelo no habilitado, falla de recalibración, cambio concurrente y preservación histórica. |

## S9 — Consumo, cuotas y salud (~208 h estimadas)

**No iniciar sin:** instrumentación de S3–S8. **Demo:** ADMIN consulta costos/fallas, cambia límite y sistema aplica 429/`Retry-After`; readiness sigue útil sin proveedor.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Métricas/ledger | 38 | Tokens, costo, latencia, errores, cuotas por función/modelo/período. |
| 2 | Configuración auditada | 32 | Límites versionados, permisos admin, caché/invalidez controlada. |
| 3 | Resiliencia uniforme | 38 | Timeouts, circuito, 429 y backoff para rutas/procesos pertinentes. |
| 4 | Consultas/UI operación | 36 | Panel con filtros, estados y explicaciones sin datos sensibles. |
| 5 | Health/alertas | 22 | Liveness/readiness sin llamada a proveedor; umbrales acordados. |
| 6 | Pruebas/demo | 42 | Cambio inmediato, saturación, 429, circuito abierto y health aislado. |

## S10 — Hardening y release F1 (~208 h estimadas)

**No iniciar sin:** F1 integrada y ventana de pruebas con consumidores. **Demo:** operador recupera trabajos sin duplicar bajo carga; backup/restauración y rollback pasan.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Consola de jobs/runbook | 32 | Ver, reintentar y explicar fallidos/leases; permisos y auditoría. |
| 2 | Escenarios de recuperación | 36 | Reinicio en worker/outbox, reentrega Kafka, proveedor caído/vuelto. |
| 3 | Carga y seguridad F1 | 36 | 120 sesiones, corpus regresión, cuotas y resultados medidos. |
| 4 | Backup/restore/rollback | 36 | Procedimiento ensayado con Flyway/versiones y evidencia. |
| 5 | E2E consumidores | 36 | Golden→calibración→tutor→cierre→diferido→apelación. |
| 6 | Release y demo | 32 | Checklist F1, defectos priorizados, runbooks y decisión de salida. |

## S11 — Moderación previa a entrega (~208 h estimadas)

**No iniciar sin:** contrato/política con `chat-service` y notificaciones; no usar contrato MVP para inventar F2. **Demo:** chat real permite/bloquea antes de publicar y avisa incidentes graves.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Contrato F2 y corpus | 30 | Mensaje, contexto mínimo, categorías, severidad, retención, idempotencia y feedback. |
| 2 | Detectores clásicos | 34 | Spam, ofensivo, Base64 y código en desafío; reglas versionadas. |
| 3 | Clasificador contextual | 32 | Solo escalamiento de indeterminados, timeout y confianza. |
| 4 | Decisión/incidente/evento | 36 | Bloqueo previo, auditoría, outbox y notificación grave. |
| 5 | Integración/UI docente | 30 | Chat recibe decisión; bandeja mínima de incidentes. |
| 6 | Pruebas/demo | 46 | Permitido, bloqueos, duplicado, caída de clasificador y no filtración de reglas. |

## S12 — Revisión de moderación (~208 h estimadas)

**No iniciar sin:** S11, autorización de contexto inmediato y corpus etiquetado inicial. **Demo:** usuario apela y docente resuelve con evidencia permitida; chat refleja la resolución.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Apelación/evidencia | 34 | Datos auditables, plazo/estado y ownership. |
| 2 | Contexto mínimo | 28 | Solicitar/retener solo ventana aprobada, nunca historial completo implícito. |
| 3 | Bandeja/resolución | 38 | Categoría, confianza, evidencia, motivo y sincronización con chat. |
| 4 | Calidad de corpus | 30 | Etiquetado, falsos positivos/negativos y criterio de revisión docente. |
| 5 | Métricas/política | 20 | Tasa de error, tiempos, reiteración sin perfilado excesivo. |
| 6 | Pruebas/demo | 58 | Apelación doble, permisos, resolución, dato excesivo y casos de corpus. |

## S13 — Degradación y retención de moderación (~208 h estimadas)

**No iniciar sin:** política de caída aprobada por producto/seguridad y acuerdo de archivado del chat. **Demo:** caída aplica política aprobada y al archivar conserva sólo evidencia requerida.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Política ejecutable | 24 | Tabla por detector/clasificador/servicio total y decisión fail-open/fail-closed aprobada. |
| 2 | Degradación/remoderación | 38 | Capa clásica, marca de revisión, trabajo idempotente de recuperación. |
| 3 | Retención/purga | 36 | Eventos de archivado, evidencia media/alta y purga selectiva verificable. |
| 4 | Operación/alertas | 28 | Cola de revisión, métricas y runbook. |
| 5 | Regresión F2 | 38 | Corpus, recuperación y carga acordada. |
| 6 | Demo/cierre F2 | 44 | Chat integrado, revisión, archivo y evidencia de política. |

## S14 — RAG textual aislado (~208 h estimadas)

**No iniciar sin:** objeto/referencia autorizada, pgvector disponible y métricas de calidad acordadas. **Demo:** PDF textual genera respuesta citada; sin fuente suficiente el tutor se abstiene.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Contrato de ingesta/permiso | 28 | URI/referencia, hash, cohorte, unidad, versión y ownership. |
| 2 | Esquema documental/vector | 34 | Documento, versión, chunk, página, fuente, estado e índices pgvector. |
| 3 | Extracción/chunking | 32 | PDFBox/Tika, límites, hash, metadatos y fallas por página. |
| 4 | Embeddings/retrieval | 38 | ONNX local, top-k, umbral y filtro obligatorio de cohorte/material. |
| 5 | Contexto tutor/citas | 28 | Puerto de contexto S5, fuentes/página y abstención sin inventar respaldo. |
| 6 | UI/operación | 16 | Estado de ingesta y fuentes visibles. |
| 7 | Pruebas/demo | 32 | Aislamiento entre cohortes, fuente/página, umbral, `recall@3` y reinicio. |

## S15 — Ingesta visual con control de calidad (~208 h estimadas)

**No iniciar sin:** S14 y corpus autorizado de escaneos, tablas y figuras. **Demo:** docente activa índice visual tras revisar reporte; respuesta cita fuente correcta.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Clasificación por página | 28 | Texto/escaneo/tabla/figura y ruta de extracción trazable. |
| 2 | OCR y extracción visual | 42 | Tess4J, PDFBox/Tika y AI Gateway multimodal solo cuando aplique. |
| 3 | Referencias/objetos | 22 | Sin binarios en Postgres; hashes y referencias autorizadas. |
| 4 | Reporte y activación | 34 | Cobertura, errores, umbral de calidad y activación atómica docente. |
| 5 | Retrieval integrado | 30 | Chunks visuales citables sin degradar aislamiento S14. |
| 6 | Pruebas/demo | 52 | Tabla, diagrama, OCR fallido, página parcial, reintento e índice no activado. |

## S16 — Versionado y retiro documental (~208 h estimadas)

**No iniciar sin:** S14/S15 y política de archivo de cursos. **Demo:** nueva versión sustituye resultados; retiro elimina la versión de búsqueda sin dañar historial.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Detección/hash/versiones | 34 | Igual hash no duplica; nuevo hash crea versión aislada. |
| 2 | Máquina de estados | 34 | queued/running/failed/ready/active/retired, transiciones autorizadas. |
| 3 | Activación atómica/cache | 34 | Swap de versión e invalidación; consultas no mezclan chunks. |
| 4 | Retiro/curso archivado | 32 | Cancela jobs, retira índice conforme política y conserva auditoría. |
| 5 | UI/admin | 22 | Versiones, estado, error/reintento y retiro explícito. |
| 6 | Pruebas/demo | 52 | Carrera de ingestas, rollback, cache vacío, archivado y aislamiento. |

## S17 — Desafío personalizado seguro (~208 h estimadas)

**No iniciar sin:** formatos corregibles acordados por `challenges-service`, S14 y política de cuota/costo. **Demo:** alumno solicita/resuelve/entrega un desafío y motor aplica efectos una vez.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Contrato y catálogo formatos | 30 | Entrada/salida, dificultad, cobertura, ownership, Idempotency-Key y entrega. |
| 2 | Job de generación | 38 | Trabajo durable, RAG filtrado, cuota y AI Gateway. |
| 3 | Validadores de resultado | 34 | Schema, formato corregible, seguridad y rechazo seguro. |
| 4 | Entrega al motor/outbox | 32 | Efecto idempotente, reintento sin duplicar desafío/recompensa. |
| 5 | UI solicitud/estado | 22 | Solicitar, esperar, error y desafío recibido. |
| 6 | Pruebas/demo | 52 | Duplicado, formato inválido, cuota, caída, fuente ajena y entrega real. |

## S18 — Agente por mención (~208 h estimadas)

**No iniciar sin:** contrato con chat, S11–S13, S14 y reglas contra bucles. **Demo:** sólo `@agente` válido responde citado y moderado; la interacción IA se retiene correctamente.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Contrato/mención/actor | 30 | Sintaxis, curso, usuario actor, idempotencia, publicación y no responder bots. |
| 2 | Detección y anti-bucle | 26 | Mención válida únicamente; marcador de origen/TTL para impedir recursión. |
| 3 | Orquestación agente | 38 | RAG filtrado, cuotas, contexto mínimo, citas y abstención. |
| 4 | Guardias/moderación | 34 | Entrada/salida F1 + moderación F2 antes de publicar. |
| 5 | Retención diferenciada | 28 | Par mención/respuesta IA separado de chat social purgable. |
| 6 | Pruebas/UI/demo | 52 | Sin mención, mención ajena, bucle, jailbreak, fuente ajena, caída y archivo. |

## S19 — Operación integral y salida de producto (~208 h estimadas)

**No iniciar sin:** F1–F3 integradas, responsables operativos y datos de prueba no productivos. **Demo:** ADMIN observa/reanuda el producto completo tras restore/rollback y regresión combinada.

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | Vista operativa unificada | 34 | Jobs, ingestas, índices, calibraciones, incidentes y costos con permisos. |
| 2 | Recuperación integral | 38 | Backup/restore, rollback, cache vacía, reproceso y dedupe de las tres fases. |
| 3 | Carga/regresión combinada | 42 | Tutor/evaluador/chat/RAG/agente bajo perfil acordado y resultados medidos. |
| 4 | Seguridad/retención final | 30 | Corpus actualizado, permisos, purga/retención y secretos revisados. |
| 5 | Contratos/runbooks | 26 | OpenAPI/AsyncAPI/adendas F2/F3, operación y propietarios actualizados. |
| 6 | Release review/demo | 38 | Checklist de salida, evidencia, riesgos aceptados y backlog posterior. |

## Cierre de cada sprint

No trasladar automáticamente una fila incompleta: dividirla, medir horas restantes y decidir en Review/Planning siguiente. Cada sprint conserva la regresión de los anteriores. Las F2/F3 no se adelantan por disponibilidad técnica: se habilitan solo al cumplir los gates del sprint y las condiciones de salida de su fase.
