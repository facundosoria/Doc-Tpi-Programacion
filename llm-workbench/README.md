# LLM Workbench

Workbench Angular local para probar el Sprint 1 de golden sets. No reemplaza el frontend compartido de la plataforma.

## Ejecutar localmente

En otra terminal, iniciar la base y el backend con el perfil aislado:

```bash
cd ../llm-service
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=workbench mvn spring-boot:run
```

Luego iniciar este proyecto con Node.js 22:

```bash
npm install --legacy-peer-deps
npm start
```

Abrir `http://localhost:4200`. El perfil `workbench` asigna una identidad docente de prueba del lado del servidor y habilita CORS sólo para este origen. No debe activarse en un entorno integrado o de producción.
