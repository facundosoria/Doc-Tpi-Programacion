# EP-01 · Plataforma, contratos e integración — estado

> Fichas fuente: [`docs/historias/ep-01/`](../../historias/ep-01/README.md). Auditoría de
> código: [`llm-service/CORRECCIONES-SUGERIDAS.md`](../../../CORRECCIONES-SUGERIDAS.md)
> ítems 16–22 (primera vez que estos habilitadores se contrastan contra código; hasta el
> 2026-09-12 ninguna ficha de EP-01 tenía marcador de estado).

## Índice

| ID | Título | Estado | Nota en una línea |
|---|---|---|---|
| [H01](h01.md) | ADR de arquitectura y convenciones técnicas | 🔴 | No existe ningún ADR de `llm-service` en el repo |
| [H02](h02.md) | Entorno reproducible con un comando | 🟡 | `docker compose` existe; falta `.env.example` y documentar `down` |
| [H03](h03.md) | Esqueleto transversal del servicio | 🔴 | No se registra en Eureka; nunca devuelve `401` (todo es `403`) |
| [H04](h04.md) | Esquema inicial versionado con auditoría | 🟢 | Migración `V1` cumple lo que pide la ficha |
| [H08](h08.md) | Contrato OpenAPI y mock del golden set publicados | 🟡 | Contratos publicados; no hay mock levantable con un comando |
| [H09](h09.md) | Suite de pruebas y guía de demo de S1 | 🔴 | 32 clases de test, pero sin JaCoCo no hay gate de cobertura; sin guía de demo |
