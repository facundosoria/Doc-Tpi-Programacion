# llm-service — S1

Servicio de golden sets con PostgreSQL, Flyway, autorización M2M delegada e idempotencia. La composición base no expone puertos de negocio: en la plataforma, sólo API Gateway publica la API.

## Backend aislado

```bash
docker compose up --build
```

El servicio queda disponible sólo dentro de la red Docker. Su healthcheck es `http://llm-service:8080/actuator/health` desde otro contenedor.

## Workbench demo

```bash
docker compose -f compose.yaml -f compose.workbench.yaml up --build
```

Abrir `http://localhost:4200`. Esta composición activa el perfil `workbench` exclusivamente en el entorno demo: asigna una identidad docente de prueba dentro del servidor y el proxy Angular reenvía `/api/llm/**` a `llm-service` por la red Docker.

El perfil no se usa en la integración real. Allí API Gateway valida la sesión y agrega los headers M2M que el servicio exige.
