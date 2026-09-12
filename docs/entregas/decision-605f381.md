# Decisión — commit `605f381` (Golden Set v2 / calibración)

> **Qué es este documento.** Resuelve el §8 de
> [ep-03-s1-verificacion.md](ep-03-s1-verificacion.md) ("Nota de coordinación — rama
> `605f381`"), que dejaba pendiente decidir entre rebasar, reconciliar o posponer. Ya no es
> una decisión por tomar: **ya ocurrió en el código**. Este documento la ratifica y explica
> qué cambia para la planificación.
>
> **Fecha de esta revisión.** 2026-09-12.

---

## 1. Los hechos, verificados en el repo

- `git merge-base --is-ancestor 605f381 HEAD` → **verdadero**: el commit ya es ancestro de
  `facu` (la rama actual). No es una rama paralela por fusionar: **ya está fusionado**.
- `git status` sobre `llm-service/` está limpio: no hay cambios pendientes de aplicar.
- El commit (autor **Franco Brizzio**, `feat(s01): implementar flujo de calibracion Golden
  Set v2`) toca **107 archivos** y agrega **3481 líneas**. Borra por completo
  `GoldenSetController`/`GoldenSetService`/`GoldenSetRepository` (el v1 de S1 descrito en la
  verificación original) y los reemplaza por un flujo **versionado y con ámbito de curso**:

  | Pieza nueva | Qué hace |
  |---|---|
  | `CourseGoldenSetController` | Golden set por curso: borrador, copiar desde base publicada, publicar, siguiente versión, borrar borrador sin usar |
  | `RubricController` | Rúbrica versionada por curso: borrador, publicar, siguiente versión |
  | `CalibrationRunController` / `CalibrationActivationController` | Correr una calibración (asincrónica, idempotente) y activarla con vista previa |
  | `ModelDeploymentController` | Catálogo de adaptadores de modelo por curso y a nivel admin |
  | `CourseEvaluationStatusController` | Calibración activa, asignaciones y evaluaciones pendientes por curso |
  | `EligibleInteractionsController` | Interacciones elegibles para el golden set, con vista previa de anonimización |
  | `GoldenSetUpdateProposalController` / `SyntheticGoldenSetController` / `GoldenSetImportController` | Propuestas de actualización, generación de casos sintéticos, importación masiva con validación/commit |

- **No es un experimento.** Trae su propia suite de tests por cada clase nueva, migraciones
  Flyway versionadas (`V2`…`V12`), y un mecanismo que impide activamente volver atrás:
  `scripts/check-no-v1.sh` + `LegacyGoldenSetRouteAbsentTest` verifican que las rutas v1
  **no reaparezcan**.

## 2. Resolución

**Se ratifica la opción (a)** de las tres que planteaba el §8 original: *"S1 se rebasa sobre
la v2"*. Las otras dos ya no aplican — reconciliar no tiene sentido cuando una reemplazó
completamente a la otra, y posponer la v2 a S2/EP-04 no es una opción porque ya está en
`facu`, probada y con un guardarraíl que impide reintroducir el v1.

**Nadie tiene que decidir nada acá.** El código ya decidió; este documento solo lo deja
registrado para que la planificación deje de contradecirlo.

## 3. Qué cambia para la planificación

1. **El bloqueante más grave del punch-list anterior ya está resuelto.** `CourseAuthorization.requireTeacher`
   valida rol docente (`X-User-Roles`) y pertenencia al curso (`X-Teacher-Course-Ids`,
   propagado por el Gateway) antes de cualquier operación, con `403 Problem Detail` si no
   corresponde. Esto cierra lo que las fichas de H05 (CA3) y H06 (CA4) pedían.
2. **El código va muy por delante de la documentación.** Ya cubre pedazos de lo que las
   recetas de sprint asignan a S2 (versionado/publicación), S3 (calibración, catálogo de
   modelos) y S4/EP-06 (estado de evaluación por curso, interacciones elegibles). Las
   historias escritas hoy (`docs/historias/`) sólo describen S1.
3. **Terminología a unificar.** Las historias dicen "cohorte"; el código dice "curso"
   (`courseId`) de punta a punta, incluida la autorización. No hay en el código un concepto
   de "cohorte" separado del "curso" — se recomienda que la documentación adopte **"curso"**
   como término único y deje de usar "cohorte" para este dato.
4. **El hallazgo del perfil `workbench` sigue vigente.** `CourseAuthorization` tiene el mismo
   atajo que el `GoldenSetAuthorization` original: con `llm.workbench.enabled=true` se
   salta la autorización real. Aceptable para desarrollo local; no para una demo en
   ambiente integrado.

## 4. Lo único que sigue sin decidir (y no es mío para decidir)

`courseId`, ¿es el curso-plantilla o la cohorte/oferta concreta? Es exactamente la pregunta
**B-7** de [08 · decisiones y pendientes](../08-decisiones-y-pendientes.md): *"el material
cuelga del template; la calibración y las evaluaciones son de la cohorte"*. Que golden set,
rúbrica y calibración compartan el mismo `courseId` es **consistente** con esa recomendación
(todo lo que B-7 dice que debe ser "de la cohorte" cuelga acá del mismo identificador) — pero
hay que confirmarlo con quien mantiene el módulo (Franco Brizzio) antes de escribir ninguna
historia nueva de calibración por curso, para no asumir algo que el código no garantiza.

## 5. Próximo paso obligado

La verificación de [ep-03-s1-verificacion.md](ep-03-s1-verificacion.md) quedó **obsoleta**:
está escrita contra el `GoldenSetController` v1, que ya no existe en el repo. Antes de poder
llamar a algo "Entrega 1" hace falta una verificación nueva, contra el código v2 actual,
historia por historia — incluidas las de S2 a S4 que probablemente ya tengan cobertura real
y todavía no tienen ficha escrita en `docs/historias/`.
