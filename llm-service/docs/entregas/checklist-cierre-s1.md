# Checklist de cierre de S1

> **Qué es este documento.** Lista accionable, con casilleros, de lo que falta para poder decir
> "S1 está cerrado" según la definición original de
> [`sprints/s1-historias.md`](../sprints/s1-historias.md) — no según lo que terminó construido.
> Nace del cruce entre esa ficha y el tablero de estado real en
> [`estado-implementacion/`](../estado-implementacion/README.md) (auditado 2026-09-12).
>
> **Qué NO es.** No reabre ninguna decisión ya tomada — [`decision-605f381.md`](decision-605f381.md)
> ya resolvió que el flujo v2 de golden set reemplaza al v1, y **esa parte no está en discusión
> acá**. Este checklist es sobre lo que quedó pendiente alrededor: documentación que no se
> actualizó y garantías técnicas de la ficha original que el código todavía no cumple.
>
> **Cómo se usa.** Cada ítem tiene una casilla, una referencia a dónde está el hueco, y un
> campo `Dueño:` / `Fecha:` para completar en la reunión de cierre. No se tacha nada sin dejar
> el link a la evidencia de que se resolvió (commit, PR o documento nuevo).
>
> **Fecha de esta versión:** 2026-09-12.

---

## 1. Bloqueantes duros — sin esto, la demo de S1 no es reproducible ni verificable

Estos cuatro son los que impiden decir "quedó demostrado y probado", no solo "funcionó cuando lo
probé a mano".

### 🔴 1.1 — Guía de demo escrita (H09, CA4)

- [ ] Existe un documento paso a paso: entrar con permiso → crear golden set → cargar una
      entrada → `docker compose restart` → consultar y confirmar que es lo mismo.
- [ ] La guía usa las rutas **reales** del flujo v2 (`/api/llm/courses/{courseId}/golden-sets/...`),
      no las de H05/H06/H07 (que ya no existen — ver [§3.1](#31--reescribir-h05h06h07-contra-el-código-v2-real)).
- [ ] Se probó siguiendo la guía al pie de la letra, en el ambiente integrado (no en una máquina
      aislada) — así lo exige [`sprints/s1-historias.md` · Demo de S1](../sprints/s1-historias.md).

**Evidencia del hueco:** [`estado-implementacion/ep-01/h09.md`](../estado-implementacion/ep-01/h09.md) —
"No se encontró guía de demo".

`Dueño: _____________`  `Fecha: _____________`

### 🔴 1.2 — Prueba automatizada de reinicio (H09, CA3 y CA6)

- [ ] Existe una clase de test (`*Restart*Test` o equivalente) que levanta el stack, crea un
      golden set + una entrada, reinicia el contenedor/servicio, y verifica que la lectura
      devuelve exactamente lo cargado.
- [ ] La prueba corre en el mismo pipeline que el resto de la suite (no manual).
- [ ] Si la prueba detecta pérdida de datos, el pipeline falla (no queda como advertencia).

**Evidencia del hueco:** [`estado-implementacion/ep-01/h09.md`](../estado-implementacion/ep-01/h09.md) —
"No hay prueba automatizada de reinicio de Compose"; `FlywaySchemaTest` prueba migración, no
persistencia tras reinicio.

`Dueño: _____________`  `Fecha: _____________`

### 🔴 1.3 — Gate de cobertura con JaCoCo (H09, CA2 y CA5)

- [ ] `pom.xml` tiene el plugin de JaCoCo configurado.
- [ ] El pipeline mide cobertura y **falla** si baja del 95 % acordado en
      [`24-convenciones-cobertura.md`](../24-convenciones-cobertura.md).
- [ ] Se corrió una vez y se registró el número real de cobertura actual (con 32 clases de test
      ya escritas, probablemente esté cerca — falta medirlo, no escribir tests desde cero).

**Evidencia del hueco:** [`estado-implementacion/ep-01/h09.md`](../estado-implementacion/ep-01/h09.md) —
`grep jacoco pom.xml` vacío.

`Dueño: _____________`  `Fecha: _____________`

### 🔴 1.4 — Registro en Eureka (H03, CA1)

- [ ] `llm-service` se registra en Eureka (`spring-cloud-starter-netflix-eureka-client` +
      bloque `eureka:` en `application.yml`).
- [ ] Se verificó que el API Gateway lo descubre y lo puede rutear en el ambiente integrado —
      **la demo de S1 tiene que correr ahí, no en local**, y hoy en local no se nota este hueco
      porque nadie pasa por el Gateway para probar.

**Evidencia del hueco:** [`estado-implementacion/ep-01/h03.md`](../estado-implementacion/ep-01/h03.md) —
cero menciones de Eureka en `llm-service/`.

`Dueño: _____________`  `Fecha: _____________`

---

## 2. Bloqueantes de documento — no impiden la demo técnica, pero sí impiden aprobar S1 "tal como se definió"

### 🟠 2.1 — ADR de arquitectura (H01, CA1–CA5)

- [ ] Existe el ADR versionado en el repo (no en `docs/importado/`, que es material histórico de
      otro proyecto).
- [ ] Enumera los seis paquetes (`api`/`application`/`domain`/`infrastructure`/`security`/
      `configuration`) y la regla de qué puede depender de qué.
- [ ] Incluye la tabla de variables de entorno con default y descripción.
- [ ] Hay un control automático (ArchUnit u otro) que rechaza un import de Spring en `domain` —
      hoy la frontera se respeta por convención, no está forzada.

**Nota:** el árbol de paquetes ya existe y ya respeta lo que un ADR debería pedir — este ítem es
**documentar y forzar** lo que el código ya hace, no reorganizar código.

**Evidencia:** [`estado-implementacion/ep-01/h01.md`](../estado-implementacion/ep-01/h01.md).

`Dueño: _____________`  `Fecha: _____________`

### 🟠 2.2 — `401` vs `403` (H03, CA4)

- [ ] Se decide explícitamente: ¿el servicio va a distinguir "sin autenticar" (`401`) de
      "autenticado sin permiso" (`403`), o es una decisión deliberada colapsar todo en `403`?
- [ ] Si se decide colapsar en `403`: actualizar la ficha de H03 y su escenario 2 para que dejen
      de prometer un `401` que nunca va a llegar.
- [ ] Si se decide distinguir: agregar el caso que devuelve `401` y su test.

**Evidencia:** [`estado-implementacion/ep-01/h03.md`](../estado-implementacion/ep-01/h03.md) — hoy
todo error de identidad/autorización es `403`.

`Dueño: _____________`  `Fecha: _____________`

### 🟠 2.3 — `.env.example` y `docker compose down` documentado (H02, CA2)

- [ ] Confirmar si el `llm-service/.env.example` que aparece sin trackear en el `git status`
      cierra este hueco una vez agregado al repo.
- [ ] El README documenta los tres comandos: `up`, comprobar *health* desde afuera del
      contenedor, `down`.

**Evidencia:** [`estado-implementacion/ep-01/h02.md`](../estado-implementacion/ep-01/h02.md).

`Dueño: _____________`  `Fecha: _____________`

### 🟠 2.4 — Mock levantable con un comando (H08, CA2)

- [ ] Existe un mock del contrato (WireMock, Prism, Mockoon o similar) que se levanta con un
      comando documentado, para que `admin-service` pueda avanzar sin el servicio real.

**Evidencia:** [`estado-implementacion/ep-01/h08.md`](../estado-implementacion/ep-01/h08.md) — sin
resultados para "mock" en `docs/contracts/`, `pom.xml` ni el README.

`Dueño: _____________`  `Fecha: _____________`

### 🟠 2.5 — Decidir qué pasa con `llm-service-v1.openapi.yaml` (H08, CA1)

- [x] **Decidido el 2026-09-12: se marca como histórico/reemplazado por v2** — el código ya
      prohíbe activamente sus rutas (`scripts/check-no-v1.sh` +
      `LegacyGoldenSetRouteAbsentTest`); fusionarlo no tiene sentido cuando una versión reemplazó
      por completo a la otra (mismo argumento que [`decision-605f381.md`](decision-605f381.md)
      ya usó para el código). Falta solo la ejecución: marcar el archivo y actualizar
      [`ep-01/h08.md` T1](../tareas/ep-01/h08.md).
- [ ] Una vez decidido, sincronizar el contrato v2 con los endpoints reales: agregar los tres que
      faltan (`next-version`, `GET /api/llm/courses`, `.../model-deployments`) y resolver los dos
      que están en el contrato pero no implementados (`/admin/base-golden-sets`,
      `/admin/model-adapters`).

**Evidencia:** [`estado-implementacion/ep-01/h08.md`](../estado-implementacion/ep-01/h08.md),
[`estado-implementacion/ep-03/s02-h02.md`](../estado-implementacion/ep-03/s02-h02.md),
[`estado-implementacion/ep-02/model-deployments.md`](../estado-implementacion/ep-02/model-deployments.md).

`Dueño: _____________`  `Fecha: _____________`

---

## 3. Deuda de documentación — no bloquea nada técnico, pero deja el backlog mintiendo

### 🟡 3.1 — Reescribir H05/H06/H07 contra el código v2 real

- [ ] Reemplazar las tres fichas (hoy describen `POST /api/llm/golden-sets` y el término
      "cohorte", que no existen en el código) por fichas que describan el flujo real:
      golden set versionado por curso, borrador → publicar → siguiente versión.
- [ ] Escribir una verificación nueva que reemplace a
      [`ep-03-s1-verificacion.md`](ep-03-s1-verificacion.md) (obsoleta: está escrita contra el
      `GoldenSetController` v1 que ya no existe).

**Evidencia:** [`estado-implementacion/ep-03/h05-h06-h07.md`](../estado-implementacion/ep-03/h05-h06-h07.md),
[`decision-605f381.md`](decision-605f381.md).

`Dueño: _____________`  `Fecha: _____________`

### 🟡 3.2 — Unificar "cohorte" vs "curso"

- [ ] Las historias dicen "cohorte"; el código usa "curso" (`courseId`) de punta a punta,
      incluida la autorización. Decidir un término único y actualizar la documentación — no el
      código, que ya está escrito y probado así.
- [ ] Confirmar con quien mantiene el módulo (Franco Brizzio) si `courseId` es el
      curso-plantilla o la cohorte/oferta concreta, antes de escribir cualquier historia nueva de
      calibración por curso (pregunta B-7 de
      [`08-decisiones-y-pendientes.md`](../08-decisiones-y-pendientes.md), todavía abierta).

**Evidencia:** [`decision-605f381.md` · §3](decision-605f381.md), [`decision-605f381.md` · §4](decision-605f381.md).

`Dueño: _____________`  `Fecha: _____________`

### 🟡 3.3 — Fichar lo que el código ya construyó de más

- [ ] El código de EP-03/EP-04 ya cubre pedazos de S2 (versionado/publicación), S3 (calibración,
      catálogo de modelos) y S4/EP-06 (estado de evaluación por curso) sin que existan las
      historias correspondientes en `docs/historias/`. Escribir esas fichas (aunque sea a
      posteriori, como ya se hizo con `s02-h01.md`/`s02-h02.md`) para que el backlog deje de
      subestimar el avance real.

**Evidencia:** [`estado-implementacion/README.md` · "El hallazgo transversal más importante"](../estado-implementacion/README.md).

`Dueño: _____________`  `Fecha: _____________`

---

## 4. Riesgo a vigilar en la demo (no es un "falta", es un "ojo")

### ⚠️ 4.1 — Atajo `llm.workbench.enabled=true`

- [ ] Confirmar que la demo de S1 en el ambiente integrado corre con este flag **apagado**.
      `CourseAuthorization` (igual que el `GoldenSetAuthorization` original) salta la
      autorización real cuando está prendido — aceptable en desarrollo local, **no** en una demo
      compartida, porque invalida justamente los escenarios negativos (CA3 de H05, CA4 de H06)
      que la ficha pide demostrar.

**Evidencia:** [`decision-605f381.md` · §3, punto 4](decision-605f381.md).

`Dueño: _____________`  `Fecha: _____________`

---

## 5. Lo que ya está bien y no hay que tocar

Para no perder tiempo revisando de nuevo en la reunión:

- **H04** (esquema versionado con auditoría) — 🟢 cumple las 5 CA.
  [`estado-implementacion/ep-01/h04.md`](../estado-implementacion/ep-01/h04.md)
- **H10** (puerto hacia modelos + fake) — 🟢 cumple las 6 tareas, cerrada 2026-09-12.
  [`estado-implementacion/ep-02/h10.md`](../estado-implementacion/ep-02/h10.md)
- El flujo v2 de golden set/rúbrica (aunque sin ficha de S1 todavía) trae su propia suite de
  tests, migraciones Flyway prolijas, y un guardarraíl que impide volver al v1 por accidente.

---

## 6. Cierre

- [ ] Todos los ítems de la [sección 1](#1-bloqueantes-duros--sin-esto-la-demo-de-s1-no-es-reproducible-ni-verificable) resueltos con evidencia linkeada.
- [ ] Ítems de la [sección 2](#2-bloqueantes-de-documento--no-impiden-la-demo-técnica-pero-sí-impiden-aprobar-s1-tal-como-se-definió) resueltos o explícitamente diferidos con una fecha y un dueño (no "en algún momento").
- [ ] Ítem 4.1 verificado en el ambiente donde se hace la demo real.
- [ ] Actualizar [`estado-implementacion/README.md`](../estado-implementacion/README.md) y las
      filas de [`estado-implementacion/ep-01/README.md`](../estado-implementacion/ep-01/README.md)
      con los íconos que cambiaron.
- [ ] Recién ahí, dar S1 por cerrado en la Wiki de Taiga.

*Este documento no inventa criterios nuevos — cada ítem viene de una CA ya escrita en
[`sprints/s1-historias.md`](../sprints/s1-historias.md) o de un hallazgo ya auditado en
[`estado-implementacion/`](../estado-implementacion/README.md). Cuando se resuelva un ítem, el
lugar para registrarlo es la ficha de esa historia en `estado-implementacion/`, no este checklist
(este es de un solo uso, para la reunión de cierre).*
