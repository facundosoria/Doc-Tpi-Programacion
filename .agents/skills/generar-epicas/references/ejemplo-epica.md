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
catálogo (`docs/epicas/README.md`). La ficha generada (`ep-01.md`) es solo el heading y
las cuatro secciones, sin blockquote de presentación ni referencias a otros docs:

---

# G07 — Plataforma, contratos e integración

---

## Objetivo

Dar al resto de los equipos, desde el primer sprint, un contrato estable para
integrarse con nuestro servicio de IA y un registro confiable de los datos
académicos — antes de que exista ninguna función de IA en sí.

---

## Suposiciones y Restricciones

- **Suposiciones:**
  - El gateway institucional ya identifica de forma segura quién hace cada pedido
    antes de que nos llegue.
  - Cada equipo consumidor integra contra el contrato publicado, no contra nuestra
    implementación interna.
- **Restricciones (legales / técnicas):**
  - El stack lo fija la cátedra; no es una decisión de esta épica.
  - Los datos académicos nunca se editan ni se borran una vez guardados — solo se
    agregan versiones nuevas.
  - Ningún endpoint queda expuesto sin pasar por el gateway.

---

## Criterios de Aceptación a nivel Épico

- [ ] El conjunto mínimo de historias permite el flujo completo: un consumidor
  autenticado llega a nuestro servicio, opera sobre datos versionados y recibe
  respuestas conformes al contrato publicado.
- [ ] El esquema inicial se crea desde cero de forma repetible y con auditoría.
- [ ] El contrato publicado describe solo lo que ya está implementado, y hay una
  versión de prueba que permite integrar sin depender del servicio real.
- [ ] No hay regresiones críticas en el borde de seguridad al agregar funciones en
  sprints posteriores.
- [ ] Hay observabilidad mínima: el servicio informa su estado de salud, se puede
  rastrear un pedido de punta a punta, y nunca se loguean secretos.

> Al pegar estos criterios en Taiga: una línea por ítem y sin `code` inline (Taiga
> descoloca las tildas si el ítem trae `code` o sub-viñetas).

---

## Dependencias / Impactos

- **Servicios / APIs:** el gateway y el sistema de descubrimiento institucional, el
  servicio que emite los permisos de acceso entre servicios, y la base de datos.
- **Módulos afectados:** la estructura interna de nuestro propio servicio, y la
  configuración general del repo (cómo se arma y se prueba el proyecto).
- **Otros equipos:** el equipo de Gateway/Discovery nos reserva la ruta de acceso y
  valida que los pedidos vengan autorizados; `admin-service` aprueba los cambios al
  contrato antes de que se publiquen.
- **Impacto en datos / migraciones:** crea el esquema base del sistema (rúbrica,
  golden set, auditoría); todas las historias siguientes dependen de él.
- **Feature toggles / flags:** no en esta épica.

---

## Por qué queda así

- El **Objetivo** reformula la columna «resultado que habilita» en prosa de valor: qué
  ganan los otros equipos y el dato académico. Una sola oración, ~2 renglones, sin
  nombrar `outbox`, `OpenAPI` ni módulos: eso vive en las secciones de abajo.
- **No** hay bloque `| Campo | Valor |` ni blockquote de encabezado: pareja, sprints,
  fase y requisitos están en el catálogo (`docs/epicas/README.md`), que es la fuente —
  la ficha no lo menciona ni lo referencia, va directo al contenido.
- Las 4 secciones están en **lenguaje llano**, no solo el Objetivo: nada de nombres de
  header, versión de framework, paquete interno ni número de doc del repo — eso vive en
  la documentación técnica del servicio, no acá.
- **No** hay `Como/Quiero/Para` ni escenarios BDD: eso vive en las historias `S01-H01…`.
- **No** hay puntos, ni MoSCoW, ni INVEST, ni «se compromete en S1»: la épica se cierra
  cuando H01–H04, H08 y H09 (y las de sprints posteriores) pasan la DoD.
- Los **CA a nivel épico** hablan del **conjunto** (flujo e2e, migración reproducible,
  contrato + mock, no-regresión del borde), no de un criterio puntual de una historia.
- Lo que el equipo no fijó (KPIs numéricos) no se inventó: se ató a condiciones
  observables («migración reproducible», «solo operaciones implementadas»).
