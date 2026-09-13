# Ejemplo resuelto — Catálogo de épicas + receta de S1

## Contexto de entrada que aportó el equipo (resumido)

- **Producto:** microservicio `llm-service` para una plataforma de aprendizaje
  gamificado de programación. Evalúa entregas de alumnos con un modelo LLM, con tutor
  socrático y moderación.
- **Fases:** F1 tutoría y evaluación académica (S1–S10) · F2 moderación del chat real
  (S11–S13) · F3 RAG y agente (S14–S18) · S19 cierre.
- **Requisitos:** lista `RF-IA-*`, `RF-CHT-*`, `RF-DES-*`, `RF-NFR-*`, `PAR-*`.
- **Equipo:** 12 integrantes, 408 h-persona/semana, sprints de 2 semanas, 5 parejas
  (P1–P5) + referente de producto + facilitador.
- **IDs:** `LLM-Sxx-Hyy`, épicas `EP-0X`.

## A. Cálculo de capacidad (resumen — detalle en `calculo-capacidad.md`)

```
nominal 816 − reuniones 102 − soporte 0 = base 714
reserva 20% = 143 → capacidad comprometible ≈ 571 h/sprint
```

Trabajo estimado por receta ≈ 208 h (piso). Margen ≈ 363 h. Referencia por pareja
≈ 114 h. Recalcular en cada Planning con la planilla real.

## B. Catálogo de épicas (extracto)

> Las épicas **no se estiman ni se comprometen**; se cierran cuando todas sus historias
> pasan la DoD. Esta tabla es **fuente**; las fichas de `generar-epicas` se le subordinan.

| Épica | Nombre | Resultado que habilita (→ *Objetivo* en Taiga) | Pareja | Sprints | Requisitos (orientativo) |
|---|---|---|---|---|---|
| **EP-01** | Plataforma, contratos e integración | El servicio arranca reproducible, expone `/api/llm/**` por Gateway, versiona su esquema y publica contratos que los demás equipos consumen | P1 | S1, S3, S6, S10, S19 | RF-NFR-01/03/04/09/10; contratos v1 |
| **EP-02** | AI Gateway, modelos y resiliencia | Toda llamada a un modelo pasa por un punto único con timeout, presupuesto, validación de salida y cambio de modelo por configuración | P2 | S3, S8, S9 | RF-IA-22/23/24/35 |
| **EP-03** | Golden set y referencia humana | Un docente autorizado construye, puntúa y versiona el set de referencia que habilita calibrar | P5 + P4 | S1, S2 | RF-IA-29/30 a 36 |
| … | | | | | |

(10 épicas en total para las tres fases.)

## C. Sprint 0 — checklist de salida (extracto)

- [ ] Disponibilidad individual declarada para el ciclo completo.
- [ ] Nombres de P1–P5, referente de producto y facilitador; suplencias.
- [ ] Cálculo de capacidad de S1 con la planilla real.
- [ ] DoR y DoD leídas y aceptadas.
- [ ] **Historia canónica** elegida y estimada. Candidata: `LLM-S01-H06`.
- [ ] Épicas revisadas; cada historia de S1 mapeada a una épica.
- [ ] Dependencias D01…D04 con responsable y fecha.
- [ ] Repos y ramas protegidas; carpeta de evidencia acordada.

## D. Receta de S1

## S1 — Base operable y golden set (~208 h estimadas)

**Objetivo:** un **docente autorizado** carga y consulta casos de referencia (*golden
set*) y **los datos sobreviven al reinicio** del servicio.

**Recorrido funcional de la demo:** acceso autorizado por Gateway → alta de golden set
para la cohorte → carga de una entrada (transcripción + 5 puntajes) →
`docker compose restart` → consulta que devuelve exactamente lo cargado.

**Límites del incremento:** solo golden set base (sin doble puntuación ciega ni
publicación de versiones: eso es S2). Sin funciones de IA. Un solo idioma (`es`),
rúbrica 1.0.

**Versión anterior que debe seguir funcionando:** ninguna (primer incremento).

**No iniciar sin:** responsables de `admin-service`, Gateway/Eureka, PostgreSQL local y
definición docente inicial de las cinco dimensiones de la rúbrica.

### Paquetes verificables

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | ADR técnico y árbol de módulos | 16 | Java 21, Boot/Maven, límites de paquetes, variables de entorno y decisiones registradas. |
| 2 | Entorno reproducible | 30 | Compose con Postgres (+ Eureka/Gateway/Kafka según plataforma); `up`, health y `down` documentados. |
| 3 | Esqueleto transversal | 34 | Nombre Eureka, `/api/llm`, security M2M, Problem Details, correlación, Actuator y pipeline CI. |
| 4 | Migración y dominio | 38 | Flyway crea rúbrica, golden set, entrada, versión/auditoría e índice de idempotencia desde DB vacía. |
| 5 | API golden set v1 | 38 | Implementar solo operaciones existentes del OpenAPI; validar actor/ownership/idempotencia. |
| 6 | Interfaz docente mínima | 24 | Formulario/listado real vía Gateway, estados de carga/error y autorización visible. |
| 7 | Pruebas y demo | 28 | Unitarias, Testcontainers/Flyway, WireMock Gateway, reinicio de Compose y guía de demo. |
| | **Total (piso, no tope)** | **208** | Capacidad del sprint ≈ 571 h → margen ≈ 363 h. |

### Gates

- No copiar `/ai/*` de ningún ejemplo histórico.
- Congelar cualquier campo de contrato ausente con `admin-service` antes de publicarlo.

### Aceptación negativa

- Token sin rol no accede. Segunda `Idempotency-Key` igual no duplica. Rúbrica inválida
  se rechaza. Reinicio no crea ni pierde datos. Cohorte ajena no aparece.

### Historias derivadas (orientativo)

| ID | Como… / quiero… / para… | Tipo | Épica | Pareja | Dep. | h |
|---|---|---|---|---|---|--:|
| LLM-S01-H01 | Como equipo, quiero un ADR con el árbol de módulos y las convenciones | Tarea | EP-01 | P1 | — | 16 |
| LLM-S01-H02 | Como desarrollador, quiero levantar el entorno con un comando | Tarea | EP-01 | P1 | H01 | 30 |
| LLM-S01-H03 | Como plataforma, quiero exponer el esqueleto transversal | Tarea | EP-01 | P1 | H02 | 34 |
| LLM-S01-H04 | Como plataforma, quiero el esquema inicial versionado con auditoría | Tarea | EP-01 | P1 | H03 | 38 |
| LLM-S01-H05 | Como docente autorizado, quiero dar de alta un golden set y cargar entradas | **HU** | EP-03 | P5 | H04 | 24 |
| LLM-S01-H06 *(canónica)* | Como docente autorizado, quiero consultar mi golden set aunque el servicio se reinicie | **HU** | EP-03 | P5 | H04 | 14 |
| LLM-S01-H07 | Como docente, quiero una pantalla mínima para alta, carga y consulta | **HU** | EP-03 | P5 | H05, H06 | 24 |
| LLM-S01-H08 | Como equipo de integración, quiero el contrato OpenAPI y un mock publicados | Tarea | EP-01 | P1 | H03 | 10 |
| LLM-S01-H09 | Como equipo, quiero la suite de pruebas y la guía de demo de S1 | Tarea | EP-01 | todos | H04–H07 | 18 |
| | | | | | **Total** | **208** |

**Demo de S1:** un docente autorizado crea un golden set, carga una entrada y la consulta
después de reiniciar el servicio.

---

## Por qué queda así

- **Capacidad y receta son cifras distintas.** 571 h de capacidad, 208 h de receta; la
  diferencia se declara como margen y **no** se reescala la receta ×2,7 para llenarla.
- **10 épicas para todo el producto**, no una por sprint: EP-01 vive en 5 sprints
  distintos. El «resultado que habilita» es una frase de valor, no una tarea técnica.
- **Las épicas no llevan horas ni puntos** en el catálogo.
- La receta de S1 sale de **requisitos y arquitectura** (RF-NFR, RF-IA-30 a 36,
  contrato con `admin-service`), no de imaginar features.
- Los **paquetes** son verificables: cada uno tiene una «salida / prueba», no es «avanzar
  con X».
- La **aceptación negativa** de la receta es la semilla directa de los escenarios
  negativos que después escribe `generar-historias-usuario`.
- Las **historias derivadas** van sin puntos Fibonacci: se asignan en Sprint 0. La
  columna `h` es la referencia del plan y no se convierte a puntos.
- S2..S19 se dejarían **gruesos** (objetivo + paquetes principales) y se refinan al
  acercarse; no se pre-cargan historias con estimaciones inventadas.
