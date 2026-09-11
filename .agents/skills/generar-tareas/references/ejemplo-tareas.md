# Ejemplo — una historia resuelta en sus tareas SMART

**ILUSTRACIÓN, no algo a copiar.** Muestra tono, nivel de detalle y trazabilidad. La
historia de partida es la **canónica** de un backlog de ejemplo: pequeña, con recorrido
completo (autorización + lectura + persistencia).

---

## Historia de partida (resumen)

**S01-H06 — Consulta del golden set que sobrevive al reinicio** · HU de valor · docente ·
14 h · depende de H04 (esquema).

**Criterios de aceptación:**

- **CA1:** un golden set con una entrada se consulta y devuelve esa entrada.
- **CA2:** después de `docker compose restart`, la misma consulta devuelve los mismos datos.
- **CA3:** el listado respeta `page`/`size` y el orden por creación descendente.
- **CA4 (negativo):** el golden set de otra cohorte no aparece y su detalle responde `404`.
- **CA5 (negativo):** `goldenSetId` con formato válido pero inexistente → `404`, nunca `500`.
- **CA6 (negativo):** `size=500` → `400`.

**Escenarios BDD:** (1) la consulta sobrevive al reinicio; (2) aislamiento entre cohortes;
(3) golden set inexistente; (4) paginación fuera de rango.

---

## Barrido → candidatas

| Origen | Candidata |
|---|---|
| CA3, CA4, Escenario 2 | listar con paginación, orden y filtro por cohorte |
| CA1, CA4, CA5, Escenario 3 | detalle con entradas; ajeno / inexistente → `404` |
| CA6, seguridad | autorización de lectura + validar `size` |
| CA2, Escenario 1 | prueba de persistencia con reinicio de Compose |

Cuatro candidatas, cuatro pasos de construcción distintos (caso de uso ×2, seguridad,
prueba E2E). Ninguna pasa la jornada. **No se fusionan.**

---

## Tareas

### T1 — Caso de uso «listar golden sets»

*(ficha completa, como se cargaría sola en Taiga)*

> Historia padre: `[GXX] — Consulta del golden set que sobrevive al reinicio`.

#### Objetivo (SMART)

- **Específica:** implementar el listado paginado de golden sets de la cohorte del docente.
- **Medible:** el criterio de terminado (abajo) se responde sí/no con una prueba.
- **Alcanzable:** 4 h, una persona de la pareja; el esquema de H04 ya existe.
- **Relevante:** cubre CA3 (paginación y orden) y la mitad de CA4 (aislamiento en el listado).
- **Acotada en el tiempo:** 4 h; si apareciera filtrado por rúbrica además, se partiría.

#### Pasos / alcance

- Endpoint `GET /api/llm/golden-sets` con `page` (desde 0) y `size` (1–100).
- Orden por fecha de creación descendente.
- Filtro por cohorte **siempre** aplicado (no es parámetro opcional).
- **Fuera de esta tarea:** el detalle con entradas (T2) y la validación de `size` (T3).

#### Criterio de terminado (Done)

- El listado devuelve solo golden sets de la cohorte del docente, paginados y ordenados
  descendente; prueba de integración del camino feliz en verde.

#### Estimación y dependencias

- **Horas:** 4 · **Paso:** caso de uso.
- **Depende de:** H04 (esquema). · **Traza:** CA3, CA4 (parcial); Escenario BDD 2.

---

### T2 — Caso de uso «detalle de golden set»

- **Específica:** implementar `GET /api/llm/golden-sets/{id}` con el golden set y sus `entries[]`.
- **Medible:** un id propio devuelve sus entradas; uno ajeno o inexistente → `404` *Problem Details*.
- **Alcanzable:** 4 h, una persona de la pareja.
- **Relevante:** CA1 (devuelve la entrada), CA4 (detalle ajeno → `404`), CA5 (inexistente → `404`, no `500`).
- **Acotada:** 4 h · paso *caso de uso*. **Depende de:** T1 · **Traza:** CA1, CA4, CA5; Escenarios 2 y 3.

### T3 — Autorización de lectura y validación de paginación

- **Específica:** aplicar a la lectura el mismo scope y usuario delegado que a la escritura, y
  rechazar `size` fuera de 1–100 con `400`.
- **Medible:** sin scope/usuario delegado → `403`; `size=500` → `400` y sin datos.
- **Alcanzable:** 3 h, una persona de la pareja.
- **Relevante:** CA6 y el requisito de seguridad de la historia.
- **Acotada:** 3 h · paso *seguridad y resiliencia*. **Depende de:** T1 · **Traza:** CA6; Escenario 4.

### T4 — Prueba de integración de persistencia (reinicio de Compose)

- **Específica:** prueba automatizada que hace `docker compose restart` y verifica que la
  consulta devuelve exactamente lo cargado antes.
- **Medible:** tras el reinicio, la respuesta `200` contiene el golden set y la entrada tal
  como se cargaron; si no, la prueba falla.
- **Alcanzable:** 3 h, una persona de la pareja.
- **Relevante:** CA2, que es el corazón de la demo del sprint.
- **Acotada:** 3 h · paso *prueba E2E*. **Depende de:** T2 · **Traza:** CA1, CA2; Escenario 1.

> **Total S01-H06:** 4 + 4 + 3 + 3 = **14 h** = referencia del plan. ✔

---

## Por qué queda así

- **Cuatro tareas, no dos.** «Listar» y «detalle» son casos de uso distintos con criterios
  distintos; unirlos daría una tarea de > 1 jornada con un criterio de terminado difuso.
- **La seguridad es su propia tarea (T3).** Los negativos CA6 y el `403` no se dan por
  incluidos en el camino feliz de T1/T2.
- **La prueba de reinicio es una tarea (T4), no un sub-paso.** Es la garantía de la demo y
  vale una estimación propia.
- **Las horas cuadran con el plan (14 h).** Si no cuadraran, el corte estaría mal: no se
  ajustan los números a mano.
- **Ninguna tarea tiene `Como/Quiero/Para` ni puntos.** Eso quedó en la historia.
