# LLM Workbench

Frontend Angular temporal para desarrollar y probar golden sets en S1. No reemplaza el monolito Angular de la plataforma.

## Contenedor de desarrollo

```bash
cd ../llm-service
docker compose -f compose.yaml -f compose.workbench.yaml up --build
```

Abrir `http://localhost:4200`. El código fuente se monta en el contenedor y Angular recarga los cambios. El navegador llama siempre a `/api/llm/**`; `proxy.workbench.json` reenvía esas solicitudes al backend demo por la red de Docker.

## Integración futura

El monolito final conserva la misma ruta relativa `/api/llm/**`. Cuando API Gateway exista, el host del frontend debe enrutarla hacia el Gateway. `proxy.gateway.example.json` muestra la configuración equivalente para desarrollo. No se modifican componentes ni servicios Angular y el navegador no fabrica headers de identidad.
