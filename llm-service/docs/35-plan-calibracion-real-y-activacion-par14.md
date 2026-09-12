# Plan de implementación: calibración real y activación PAR-14

> **Estado:** Pendiente de implementación  
> **Última actualización:** 2026-09-10  
> **Ámbito:** `llm-service` y `llm-workbench`  
> **Propósito:** completar la calibración real de modelos candidatos y permitir su activación global únicamente después de aprobar PAR-14 en los niveles institucional y de curso.

## 1. Decisiones acordadas

| ID | Decisión |
|---|---|
| PAR14-01 | PAR-14 aprueba con MAE final ponderado menor o igual a **5**. |
| PAR14-02 | Ningún error individual por caso y dimensión puede superar **10** puntos. |
| PAR14-03 | El candidato debe aprobar dos evidencias reales: una institucional y una de curso. |
| PAR14-04 | La evidencia institucional usa un Golden Set base publicado y una rúbrica institucional publicada. |
| PAR14-05 | La evidencia de curso usa un Golden Set y una rúbrica publicados del curso. |
| PAR14-06 | El profesor administrador autoriza un único candidato para calibración; la pantalla de calibración solo lo muestra en modo lectura. |
| PAR14-07 | La API key permanece cifrada en PostgreSQL y se descifra únicamente dentro del AI Gateway interno al ejecutar una llamada real al proveedor. |

## 2. Resultado esperado

El administrador carga una credencial, descubre modelos reales, prueba un candidato en el chat y lo autoriza para calibración. El sistema ejecuta ese candidato contra casos de referencia humanos, persiste el resultado de cada caso y calcula las métricas PAR-14.

La activación como evaluador global se habilita solamente si el mismo deployment aprobó la corrida institucional y una corrida de curso. El evaluador activo se seguirá mostrando en tiempo real en el workbench. Golden Sets, rúbricas, asignaciones y calibraciones vigentes conservarán su comportamiento actual.

## 3. Implementación

### 3.1 Ejecución real de calibraciones

- Completar el worker actual para que, tras reclamar una corrida `QUEUED`, cargue el deployment candidato, la credencial cifrada, la rúbrica publicada y los casos publicados del Golden Set.
- Ampliar las lecturas de Golden Set para incluir conversación, contexto del desafío y puntuaciones humanas, sin alterar el flujo de edición, publicación o copia existente.
- Construir la instrucción del evaluador desde las dimensiones, criterios, anclas y pesos de la rúbrica; no usar respuestas predefinidas ni análisis simulados.
- Invocar al proveedor exclusivamente mediante el AI Gateway interno existente. El worker, controllers y frontend no llamarán directamente a OpenAI, Groq, Gemini ni Anthropic.
- Solicitar una respuesta estructurada con puntajes por dimensión y validar que contenga todas las dimensiones requeridas, valores numéricos enteros entre 0 y 100 y formato válido.
- Persistir cada resultado en `calibration_case_results`: puntajes del modelo, puntaje final humano y calculado, errores por dimensión, error final y un artefacto seguro de salida.
- Registrar consumo real de tokens como uso `EVALUATION`, actualizar progreso por caso y finalizar la corrida como `PASSED` o `FAILED` usando `CalibrationMetrics` como fuente de verdad.
- Ante timeout, 429, 5xx, respuesta malformada o puntaje inválido, terminar la corrida de forma controlada con diagnóstico seguro. No se expondrán API keys, prompts del sistema ni contenido sensible en respuestas o logs.

### 3.2 Doble evidencia y activación

- Incorporar una etapa de corrida `PLATFORM` o `COURSE`. Las filas históricas permanecerán como `COURSE`, preservando compatibilidad.
- Agregar una configuración institucional explícita que relacione un Golden Set base publicado y una rúbrica institucional publicada. El administrador podrá consultar y definir ese perfil antes de iniciar la evidencia institucional.
- Exponer una operación administrativa para crear y consultar corridas institucionales del candidato autorizado.
- Mantener el endpoint y flujo de creación de corridas del curso: el backend resolverá siempre el candidato autorizado y no aceptará que la UI sustituya el deployment.
- Reemplazar la regla actual de activación por una verificación transaccional: el deployment debe tener al menos una corrida `PASSED` de etapa `PLATFORM` y una de etapa `COURSE`.
- Mantener la activación de calibración por curso y la migración confirmada de desafíos como procesos independientes; activar un deployment global no modifica calibraciones ni evaluaciones históricas.

### 3.3 API y frontend

- Agregar contratos aditivos para perfil institucional, creación/listado de corridas `PLATFORM` y detalle de resultados por caso. Los contratos existentes de curso seguirán siendo compatibles.
- Incorporar en Administración LLM el estado de ambas evidencias, última corrida, MAE, error máximo, fecha de alta y consumo acumulado por deployment.
- Mantener el chat de prueba con streaming real y reflejar el consumo al finalizar la respuesta.
- En la pantalla de calibración del curso, conservar el candidato de solo lectura, mostrar progreso real y presentar MAE, error máximo y detalle de resultados persistidos.
- Habilitar “Activar evaluador global” solo con ambas evidencias aprobadas. Si no se cumple la regla, informar qué etapa o métrica falta en lugar de devolver un error ambiguo.
- Corregir cualquier texto de interfaz, prueba o componente que aún comunique MAE menor o igual a 10; el umbral visible y el del backend deben ser menor o igual a 5.

### 3.4 Persistencia, seguridad y trazabilidad

- Crear una migración Flyway aditiva para representar la etapa de calibración y la configuración institucional, sin modificar ni eliminar datos vigentes.
- Conservar todas las migraciones históricas, incluyendo aquellas que ya no introduzcan comportamiento nuevo: Flyway las necesita para reconstruir una base desde cero.
- Usar la master key configurada por `LLM_CREDENTIALS_MASTER_KEY` solo para cifrar/descifrar credenciales; nunca persistir una API key en texto plano.
- Propagar `X-Request-Id` y `traceparent` en los flujos HTTP que los reciban, y asociar la ejecución, consumo y fallos a la corrida y deployment correspondientes.

## 4. Auditoría de código muerto

La depuración queda limitada a código del flujo de credenciales, deployments y calibración que sea reemplazado por esta implementación. No incluye Golden Sets, rúbricas, asignaciones, evaluaciones, migraciones aplicadas ni funcionalidades externas al cambio.

Antes de eliminar cualquier símbolo, archivo o ruta se realizarán las tres comprobaciones siguientes y se documentará el resultado en el PR:

1. **Referencia estática:** búsqueda de imports, inyección Spring, rutas Angular, contratos OpenAPI, consultas SQL, migraciones, compose y documentación.
2. **Referencia de ejecución:** arranque del contexto Spring, build de producción de Angular, registro de rutas y flujo del contenedor.
3. **Referencia de pruebas y operación:** pruebas automatizadas, beans, endpoints públicos, dependencias de datos y escenarios operativos.

Solo se eliminará un elemento si las tres comprobaciones prueban que no tiene consumidores y que su reemplazo ya está cubierto por pruebas. Los cambios se mantendrán quirúrgicos; no habrá limpieza cosmética fuera del alcance.

## 5. Pruebas y criterios de aceptación

- Verificar `CalibrationMetrics` con MAE ponderado, umbral exacto 5, error individual exacto 10 y escenarios de rechazo.
- Ejecutar pruebas de integración con PostgreSQL para creación idempotente, persistencia por caso, progreso, métricas y transición terminal de corridas.
- Validar activación para: ninguna evidencia, solo institucional aprobada, solo curso aprobada, una evidencia fallida y ambas aprobadas para el mismo deployment.
- Probar respuestas estructuradas inválidas, timeout, 429 y 5xx para confirmar que la corrida falla de forma controlada y no activa el candidato.
- Ejecutar un smoke test real, con una credencial configurada, que cubra descubrimiento de modelos, chat por streaming, calibración institucional, calibración de curso y activación final. No se aceptará una simulación como evidencia de funcionamiento extremo a extremo.
- Ejecutar regresión de Golden Set, rúbricas, asignaciones y calibraciones existentes, además de compilación backend y build/pruebas frontend.

## 6. Límites explícitos

- No se borrarán Golden Sets, rúbricas, casos, resultados históricos, asignaciones ni evaluaciones existentes.
- No se reescribirán migraciones Flyway ya aplicadas ni se eliminarán por considerarlas código muerto.
- La seguridad específica del chat del alumno no forma parte de este plan; se conserva el modelo actual de administrador del workbench.
- La calibración evalúa el uso pedagógico de IA y no la corrección académica de respuestas o soluciones del alumno.
