# Plan de purga V1 y transición exclusiva a V2

> **Estado:** Pendiente de implementación  
> **Fecha:** 2026-09-07  
> **Relacionado con:** [Plan 31](31-plan-revision-golden-set-calibracion.md)  
> **Decisión:** migrar y conservar los datos heredados válidos; realizar un corte total de UI y API V1.

## 1. Objetivo

Dejar una única implementación funcional del evaluador de uso pedagógico de IA: la arquitectura V2 por curso bajo `/docente/cursos/:courseId/evaluador/*` y `/api/llm/courses/{courseId}/...`.

Al finalizar no deben quedar rutas, componentes, tipos, almacenamiento ni endpoints V1 funcionando. Los datos heredados se conservan de forma auditable, pero no se usan automáticamente en calibraciones V2.

## 2. Alcance y decisiones

| Área | Decisión |
|---|---|
| UI V1 | Se elimina por completo. `/docente` deja de cargar `TeacherComponent` y su flujo local. |
| API V1 | Se elimina por completo. `/api/llm/golden-sets` deja de existir y responde 404. |
| Datos PostgreSQL V1 | Se archivan en un esquema privado `legacy_v1`; no quedan repositorios ni endpoints que los expongan. |
| Datos migrables | Solo se crean borradores V2 en cuarentena; nunca se publican ni se activan automáticamente. |
| `localStorage` V1 | Se ofrece respaldo descargable y migración única de elementos convertibles por curso. |
| Compatibilidad externa | No hay ventana de compatibilidad ni adaptador V1. El corte es intencionalmente incompatible. |

## 3. Implementación

### 3.1 Navegación y contexto de curso V2

- Crear un `CourseContextService` HTTP que consulte los cursos autorizados para el actor actual.
- Sustituir `knownCourseGuard` basado en `TeacherStore` por un guard que resuelva el curso mediante ese servicio.
- Cambiar `/`, `/docente`, `/golden-sets`, `/golden-sets/new`, `/golden-sets/:id` y rutas desconocidas a una ruta V2 válida del primer curso autorizado. Si no hay cursos, mostrar un estado vacío autorizado, sin redirección infinita.
- Quitar del shell V2 toda dependencia de `TeacherStore`, IDs demo o estado de navegador.
- Completar cualquier endpoint V2 faltante para crear, publicar, clonar y consultar Golden Sets, de modo que la UI no requiera ningún contrato V1.

### 3.2 Retiro de frontend V1

- Eliminar `TeacherComponent`, `TeacherStore`, `PresetsComponent`, `BatchesComponent`, `CalibrationsComponent`, `WorkbenchComponent`, `GoldenSetApiService` y sus plantillas, estilos y pruebas.
- Eliminar tipos y aliases V1: `Exam`, `Batch`, `Preset`, `Calibration`, además de `STORAGE_KEY` y toda escritura/lectura funcional de `localStorage` V1.
- Conservar únicamente preferencias locales no críticas y nunca datos de negocio, resultados de calibración o borradores oficiales.
- Eliminar enlaces, textos y documentación de interfaz que mencionen el flujo anterior o corrección académica.

### 3.3 Retiro de backend V1

- Eliminar `GoldenSetController`, `GoldenSetService`, `GoldenSetRepository`, DTOs/modelos V1 y pruebas asociadas.
- Eliminar el contrato `/api/llm/golden-sets` de OpenAPI, clientes Angular y documentación activa.
- Mantener exclusivamente los controladores y servicios V2 por curso, más los endpoints ADMIN definidos por el contrato V2.
- Agregar una prueba de integración que confirme que cualquier método sobre `/api/llm/golden-sets` devuelve 404.

### 3.4 Archivo y cuarentena de datos V1

- Crear una migración Flyway que cree el esquema privado `legacy_v1` y mueva allí `golden_sets`, `golden_set_entries`, `rubric_versions`, `rubric_dimensions`, sus secuencias, índices y dependencias necesarias para preservar integridad.
- Registrar un inventario de migración en V2 con ID original, checksum, fecha, conteo de entradas y resultado de conversión.
- Para una entrada V1 con transcript válido y cinco puntajes enteros válidos, crear un caso V2 en un Golden Set `DRAFT` de cuarentena de plataforma:
  - `metadata.legacyV1` conserva IDs y checksum de origen.
  - `challengeContext` indica explícitamente que el contexto original no está disponible.
  - El caso se marca para revisión obligatoria y no puede publicarse hasta que un responsable complete contexto y reclasifique procedencia.
- Entradas V1 sin transcript o referencias válidas permanecen únicamente en `legacy_v1` y se reportan como no convertibles.
- No inventar curso, identidad de alumno ni procedencia. V1 no contiene evidencia suficiente para asignarlos de forma confiable.

### 3.5 Migración única del navegador

- Al primer inicio V2, detectar `llm-workbench.teacher.v1` antes de retirar la clave.
- Ofrecer un resumen de objetos convertibles, incompatibles y un archivo JSON descargable de respaldo.
- Convertir al curso ya indicado por el estado local solamente:
  - rúbricas válidas a borradores V2;
  - lotes/casos válidos a Golden Sets `DRAFT` de cuarentena;
  - nunca convertir simulaciones o ejecuciones locales en calibraciones oficiales.
- Borrar la clave local únicamente cuando todas las conversiones obtengan confirmación del backend o el usuario descargue el respaldo y acepte descartar los elementos incompatibles.
- Si falla la red, se conserva el estado local y se permite reintentar sin duplicar recursos mediante claves de idempotencia.

### 3.6 Prevención de regresiones

- Incorporar una comprobación estática en CI que falle ante referencias productivas a `TeacherStore`, `/api/llm/golden-sets`, `GoldenSetController`, `Exam`, `Batch`, `Preset`, `localStorage` V1 o rutas V1.
- Mantener las referencias históricas solo en material marcado explícitamente como histórico; excluirlas de la comprobación con una lista acotada y documentada.

## 4. Pruebas y aceptación

- Rutas: `/`, `/docente`, rutas V1 y URLs desconocidas llevan a V2 o a un estado vacío autorizado; nunca cargan componentes V1.
- API: todos los métodos contra `/api/llm/golden-sets` devuelven 404; OpenAPI y el frontend no contienen ese contrato.
- Datos: probar archivo y cuarentena con sets V1 válidos, inválidos, vacíos y parcialmente convertibles; verificar checksum, trazabilidad, idempotencia y rollback transaccional.
- Navegador: probar migración local válida, parcialmente inválida, sin almacenamiento y con error de red; no se pierde información sin respaldo o confirmación explícita.
- E2E V2: crear rúbrica, importar casos, publicar, ejecutar calibración, validar PAR-14, activar, migrar desafíos permitidos y consultar auditoría.
- Calidad: ejecutar suites backend y frontend, build de producción, verificación OpenAPI y búsqueda de términos/rutas V1 prohibidos.

## 5. Definición de terminado

- [ ] Solo V2 es alcanzable desde navegación, deep links y redirect inicial.
- [ ] No quedan componentes, servicios, tipos ni pruebas V1 en código productivo.
- [ ] La API V1 no está registrada ni documentada.
- [ ] Los datos V1 quedaron archivados, inventariados y los convertibles están en cuarentena V2.
- [ ] El navegador preserva o migra su estado anterior sin duplicación ni pérdida silenciosa.
- [ ] El shell V2 no depende de `TeacherStore`, `localStorage` V1 ni IDs demo.
- [ ] Las suites automatizadas, la prueba E2E y las comprobaciones anti-regresión pasan.
