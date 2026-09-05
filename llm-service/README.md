# llm-service — S1

Servicio definitivo del Tema 07. S1 administra golden sets con PostgreSQL, Flyway, autorización M2M delegada e idempotencia.

## Ejecutar

```bash
docker compose up -d postgres
mvn spring-boot:run
```

El servicio escucha en `http://localhost:8080`; en producción recibe tráfico sólo a través del API Gateway.

Para crear un golden set se requieren los headers `X-Service-Id: admin-service`, `X-Service-Scopes: llm.golden-set.manage`, `X-Delegated-User`, `Idempotency-Key` y, cuando exista, los de correlación.

La UI se integra en el monolito Angular compartido, que no está presente en este repositorio.
