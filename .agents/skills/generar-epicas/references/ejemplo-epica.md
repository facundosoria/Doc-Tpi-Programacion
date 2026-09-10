# Ejemplo resuelto — Épica en formato Taiga

Contexto de entrada que aportó el equipo (resumido):

- **Producto:** microservicio `llm-service` de una plataforma de aprendizaje de
  programación.
- **Fila del catálogo:** `EP-01 · Plataforma, contratos e integración · resultado que
  habilita: «el servicio arranca reproducible, expone /api/llm/** por Gateway, versiona
  su esquema y publica contratos que los demás equipos consumen» · fase F1 · pareja P1 ·
  sprints S1, S3, S6, S10, S19 · RF-NFR-01/03/04/09/10; contratos v1`.
- **Grupo:** G07.

La pareja, los sprints, la fase y los requisitos **no** entran en la ficha: van en el
catálogo (`docs/epicas/README.md`). La ficha generada (`ep-01.md`) es solo el heading,
un puntero al catálogo y las cuatro secciones:

---

# [G07] — EP-01: Plataforma, contratos e integración

> Ficha en el formato del [template oficial de Épica de la Wiki de Taiga](../plantillas/epica-taiga.md).
> El catálogo (pareja líder, sprints donde aporta, fase y requisitos que cubre) vive en
> [`docs/epicas/README.md`](README.md); si un dato no coincide, **manda el catálogo**.

## Objetivo

Que los demás equipos puedan integrarse con el `llm-service` desde el primer sprint
contra un contrato estable, y que todo dato académico quede registrado de forma
reproducible y sin pérdida, antes de que existan las funciones de IA.

## Suposiciones y Restricciones

- **Suposiciones:**
  - El Gateway, el descubrimiento de servicios (Eureka) y el emisor de tokens M2M los
    provee la plataforma y están disponibles en el ambiente integrado.
  - Cada equipo consumidor integra contra el contrato publicado, no contra la
    implementación.
- **Restricciones (legales / técnicas):**
  - Stack fijado por la cátedra: Java 21, Spring Boot 3 / Maven.
  - El paquete `domain` no puede depender de framework ni de SDKs de proveedores.
  - El dato académico es *append-only*: sin edición destructiva a nivel base.
  - Ningún endpoint funcional accesible sin pasar por el Gateway.

## Criterios de Aceptación a nivel Épico

- El conjunto mínimo de historias permite el flujo e2e: un consumidor autenticado por el
  Gateway llega al `llm-service`, opera sobre datos versionados y recibe respuestas
  conformes al contrato publicado.
- El esquema inicial se crea desde base vacía con migración reproducible y auditoría.
- El contrato OpenAPI publicado describe **solo** operaciones implementadas y un mock
  levantable permite integrar sin el servicio real.
- Sin regresiones críticas en el borde (autenticación M2M, identidad delegada,
  correlación de trazas) al agregar funciones en sprints posteriores.
- Observabilidad mínima: *health/readiness*, trazas `traceparent` / `X-Request-Id` y
  logs estructurados sin secretos.
- Documentación de arranque (`up` / health / `down`), del contrato y del ADR de
  arquitectura publicada y enlazada desde el CI.

## Dependencias / Impactos

- **Servicios / APIs:** Gateway, Eureka, emisor de tokens M2M, `admin-service`
  (consumidor del contrato), PostgreSQL.
- **Módulos afectados:** `api`, `application`, `domain`, `infrastructure`, `security`,
  `configuration`; raíz del repo (`compose.yaml`, README); `docs/contracts/`.
- **Otros equipos:** equipo de Gateway/Discovery (ruta, `aud`, scopes, headers de
  confianza); `admin-service` aprueba la adenda de contrato y los campos nuevos.
- **Impacto en datos / migraciones:** crea el esquema base (rúbrica, golden set,
  entrada, idempotencia, auditoría); todas las historias siguientes dependen de él.
- **Feature toggles / flags:** no en esta épica.

---

## Por qué queda así

- El **Objetivo** reformula la columna «resultado que habilita» en prosa de valor: qué
  ganan los otros equipos y el dato académico. Una sola oración, ~2 renglones, sin
  nombrar `outbox`, `OpenAPI` ni módulos: eso vive en las secciones de abajo.
- **No** hay bloque `| Campo | Valor |`: pareja, sprints, fase y requisitos están en el
  catálogo (`docs/epicas/README.md`), que es la fuente. La ficha solo apunta a él.
- **No** hay `Como/Quiero/Para` ni escenarios BDD: eso vive en las historias `S01-H01…`.
- **No** hay puntos, ni MoSCoW, ni INVEST, ni «se compromete en S1»: la épica se cierra
  cuando H01–H04, H08 y H09 (y las de sprints posteriores) pasan la DoD.
- Los **CA a nivel épico** hablan del **conjunto** (flujo e2e, migración reproducible,
  contrato + mock, no-regresión del borde), no de un criterio puntual de una historia.
- Lo que el equipo no fijó (KPIs numéricos) no se inventó: se ató a condiciones
  observables («migración reproducible», «solo operaciones implementadas»).
