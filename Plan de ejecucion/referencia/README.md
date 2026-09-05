# Referencia local del plan

Esta carpeta contiene las fuentes necesarias para construir `llm-service` sin tener que saltar continuamente a otras rutas del repositorio.

- `docs-vigentes/`: copia de los documentos normativos y operativos del producto.
- `contratos/`: OpenAPI y AsyncAPI v1. Son los contratos ejecutables; cualquier ampliación necesita acuerdo con consumidores.
- `gateway-y-discovery/`: reglas de API Gateway, Eureka, seguridad, comunicación, resiliencia y pruebas.
- `antecedentes/`: investigaciones y planes importados. Sirven como contexto, pero no pueden contradecir `docs-vigentes/00-fuentes-de-verdad-y-convenciones.md` ni los contratos.

## Cómo resolver una duda

1. Buscar primero en `docs-vigentes/00-fuentes-de-verdad-y-convenciones.md`.
2. Verificar el endpoint/evento en `contratos/`.
3. Consultar la fuente temática (seguridad, datos, operación, RAG).
4. Si continúa faltando información, abrir una decisión o dependencia; no inventar el contrato.

Estas copias deben actualizarse en el mismo PR que actualice las fuentes originales. El plan de ejecución usa la fuente vigente más reciente, no una copia antigua.
