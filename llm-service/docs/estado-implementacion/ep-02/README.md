# EP-02 · AI Gateway, modelos y resiliencia — estado

> Ficha fuente: [`docs/historias/ep-02/h10.md`](../../historias/ep-02/h10.md). H10 se cerró el
> 2026-09-12 portando `LlmGateway`/`GroqAdapter` de
> [`codigo-ejemplo/ms-evaluacion-llm`](../codigo-ejemplo/ms-evaluacion-llm.md) (carpeta ya
> eliminada, ver [`codigo-ejemplo/README.md`](../codigo-ejemplo/README.md)) a
> `llm-service/domain/ai/` + `infrastructure/ai/`.

## Índice

| ID | Título | Estado | Nota en una línea |
|---|---|---|---|
| [H10](h10.md) | Puerto del proveedor de modelos (AI Gateway) y fake para pruebas | 🟢 | 6 de 6 tareas — portado de `codigo-ejemplo/ms-evaluacion-llm` |
| [model-deployments](model-deployments.md) | Catálogo de despliegues de modelo (sin ficha) | 🔴 | `ModelDeploymentController.listAdapters` devuelve proveedores hardcodeados — **sin tocar**, es otro catálogo (por curso), no el `function_model_config` de H10 |

**Consecuencia:** H10 ya no bloquea a EP-04, pero **la calibración todavía no está conectada** al
puerto — nadie llama a `ModelInvocationService` desde `CalibrationWorkflowService` todavía. Ver
[`ep-04/s03-h01.md`](../ep-04/s03-h01.md), que sigue 🔴 por esa razón. El primer consumidor real
del puerto es el tutor: [`ep-05/interactions.md`](../ep-05/interactions.md).
