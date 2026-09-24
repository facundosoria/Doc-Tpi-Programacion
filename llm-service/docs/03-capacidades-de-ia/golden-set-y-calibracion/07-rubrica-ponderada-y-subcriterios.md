# Rúbrica jerárquica: pesos por dimensión, subcriterios ponderables y Golden Set derivado

> **Estado:** vigente para diseño; **evolución material del dominio** — requiere
> [ADR-021](../../00-gobierno-y-evolucion/adr/ADR-021-rubrica-jerarquica-dimensiones-extendidas-y-subcriterios-ponderables.md),
> contrato y migración aprobados antes de implementar. No se ha tocado código ni persistencia por este
> documento.
> **Decisiones:** D-52 a D-55, D-60 a D-64 y D-66 a D-71.
> **Contradice** parcialmente la sección 3 ("Cinco dimensiones obligatorias") de
> [02 — Especificación funcional](02-especificacion-funcional.md) y la fila `RubricDimension` de
> [03 — Modelo de dominio](03-modelo-de-dominio-y-transiciones.md). Ambos documentos fueron anotados
> con una nota de evolución que remite acá; **no fueron reescritos** porque la migración de dominio
> real está pendiente de aprobación (ver ADR-021).

## 1. Punto de partida: qué dice hoy el modelo vigente (D-62, D-63 — hallazgo documental)

El modelo vigente distingue dimensión y criterio así:

- `RubricDimension` es el **único elemento ponderado**: tiene `key`, `criterio`, anclas, prompt y
  `weight`.
- La rúbrica vigente tiene **cinco dimensiones fijas** (`key`s cerradas).
- Cada dimensión tiene **un único campo de criterio** (texto) — no existe una colección de criterios
  con peso propio dentro de una dimensión.

**Qué significa hoy que un criterio "no tenga peso propio" (D-63):** el criterio sí se usa
activamente: describe qué debe observar el evaluador para asignar el puntaje de esa dimensión. Las
anclas y el prompt de la misma dimensión completan esa guía. Lo que **no existe** es un porcentaje
independiente para el criterio. Ejemplo: si `AUTONOMY` pesa 30 %, el modelo produce un puntaje 0–100
para autonomía siguiendo su criterio único; el resultado final incorpora ese puntaje multiplicado por
30 %. El criterio no agrega ni descuenta otro porcentaje aparte. Esta estructura es la que hoy permite
que Golden Set, calibración y PAR-14 comparen las mismas cinco puntuaciones de manera estable.

**Consecuencia (D-63):** en el modelo v2 **no se puede** "desactivar sólo un criterio" con 0 %, porque
el criterio es texto de la dimensión, no un elemento ponderado independiente. El nuevo alcance
(D-64) incorpora subcriterios identificables y ponderables dentro de una dimensión, con reglas claras
de suma y de agregación a su puntaje — ver §3.

**Impacto declarado por el plan (D-62):** permitir dimensiones adicionales en una subcalibración
(D-57) **contradice** la regla v2 de cinco `key`s fijas, y **requiere una ADR y evolución aprobada**
de dominio, contrato, persistencia, Golden Set, métricas PAR-14 y validadores.

## 2. Pesos de rúbrica y rigor específico de un desafío (D-52 a D-55)

### 2.1 Rigor ajustable por desafío (D-52)

El docente puede ajustar el rigor de un examen o desafío mediante una rúbrica específica. Ese ajuste
es una **política explícita y versionada** del `ChallengeOverlay` (ver
[06 — Subcalibración jerárquica](06-subcalibracion-jerarquica.md)), validada antes de calibrar y
visible en la comparación con la base global. **La IA no infiere ni arbitra pesos**: recibe una única
rúbrica efectiva cuya suma es 100, junto con reglas de precedencia deterministas (D-160). La
calibración global sigue aportando el marco, las restricciones y las skills no excluidas; la rúbrica
efectiva del desafío puede expresar un rigor diferente, dentro de las reglas de dimensiones y
porcentajes acordadas.

### 2.2 Dimensiones heredadas y porcentajes ajustables (D-53)

La subcalibración debe ajustarse a las **dimensiones definidas en la rúbrica del curso**. El docente
puede **redistribuir los porcentajes** de esas dimensiones según la necesidad del desafío o examen,
con validación de que el total sea **exactamente 100 %**.

> Nota: D-57 (ver [06 — Subcalibración jerárquica](06-subcalibracion-jerarquica.md#extensión-de-dimensiones-en-una-subcalibración-d-57))
> reemplaza explícitamente la restricción de D-53 que exigía conservar **el mismo conjunto** de
> dimensiones entre curso y desafío: ahora una subcalibración puede además **agregar** dimensiones
> nuevas de origen `CHALLENGE`. D-53 sigue vigente para las dimensiones **heredadas**: sus porcentajes
> son redistribuibles, pero el conjunto heredado en sí no se elimina (se puede llevar a 0 %, ver D-54).

La rúbrica efectiva conserva el mismo conjunto de dimensiones heredadas del curso (más las agregadas
por D-57) y materializa una única distribución de pesos para el desafío. La interfaz compara los
porcentajes heredados y los propuestos; el backend rechaza una dimensión desconocida, duplicada o un
total distinto de 100. Esto da más o menos énfasis a competencias concretas sin pedir a la IA que
fusione rúbricas ni resuelva contradicciones.

### 2.3 Cero permitido, negativos prohibidos (D-54)

Una dimensión puede recibir **0 %** en una subcalibración. **No se permiten porcentajes negativos.**
Todas las dimensiones heredadas se conservan en el snapshot y en la vista comparativa, aun con 0 %,
para dejar explícito que no participan de ese desafío. La validación exige valores numéricos ≥ 0 y
suma exacta de 100 %, antes de crear la calibración o enviar contexto al proveedor.

### 2.4 Sin criterios globales no desactivables (D-55)

No existen criterios mínimos globales que operen como condición adicional de aprobación, ni que
impidan asignar 0 % a una dimensión en un desafío. La calibración de curso aporta la plantilla de
dimensiones, los valores heredados por defecto y el contexto general; la **rúbrica efectiva del
desafío determina por completo** los pesos que intervienen en su resultado. La auditoría conserva la
comparación con la base para hacer visible cada diferencia, sin aplicar reglas de puntuación ocultas.

## 3. Subcriterios ponderables dentro de una dimensión (D-64)

### 3.1 Decisión de Producto

El nuevo alcance incorpora una **lista de subcriterios identificables y ponderables** dentro de cada
dimensión. La dimensión conserva su peso en el total de la rúbrica; **sus subcriterios distribuyen
internamente el 100 % de esa dimensión**.

### 3.2 Reglas de cálculo

```text
puntaje_dimension(d)  = suma( puntaje_subcriterio(s) * peso_interno(s) / 100 )  para s en subcriterios(d)
puntaje_final          = suma( puntaje_dimension(d) * peso_global(d) / 100 )    para d en dimensiones
```

- Los pesos de **dimensiones** totalizan 100 % (igual que hoy).
- Dentro de **cada dimensión**, sus subcriterios también totalizan 100 %.
- Un subcriterio en **0 %** queda trazable y no contribuye; los subcriterios restantes se redistribuyen
  dentro de su dimensión (coherente con D-61, ver §3.4).

### 3.3 Impacto técnico

Esta es una **evolución material** del modelo actual. Requiere:

- **ADR** — ver [ADR-021](../../00-gobierno-y-evolucion/adr/ADR-021-rubrica-jerarquica-dimensiones-extendidas-y-subcriterios-ponderables.md);
- **nuevo contrato** (OpenAPI de calibración/rúbrica, aditivo, congelado en el bloque 6 de contratos
  según lo indica D-161 §4);
- **migración de persistencia aprobada** (nueva tabla o extensión de `RubricDimension` para
  subcriterios ponderados, con Flyway aditivo — ver la regla de "bases de datos sagradas" del
  `AGENTS.md` de `llm-service`: ninguna migración se ejecuta sin instrucción explícita);
- adaptación de **Golden Set** (referencia humana por subcriterio, ver §4), **resultados por caso**,
  **calibración**, **métricas PAR-14** (extensión de errores individuales a pares
  caso/subcriterio), **reportes** y **UI**.

Este documento **no implementa** ninguno de esos cambios: deja la decisión de Producto documentada
íntegra para que la implementación cuente con el ADR, el contrato y la migración aprobados
explícitamente antes de tocar código.

### 3.4 Desactivación mediante peso cero (D-61)

Desactivar un criterio (o subcriterio) equivale a asignarle 0 %. **No existe una regla de exclusión
separada.** Los elementos restantes de esa misma dimensión deben redistribuirse para que la
ponderación efectiva vuelva a totalizar 100 %.

La edición debe recalcular o solicitar al docente el reparto restante; **nunca completa porcentajes de
forma implícita**. El snapshot conserva el elemento con peso cero y su motivo de modificación para
auditoría. El modelo recibe únicamente la ponderación efectiva ya resuelta, sin instrucciones
ambiguas de "evaluar y a la vez ignorar" un mismo criterio.

## 4. Golden Set con referencia humana por subcriterio (D-60, D-66 a D-70)

### 4.1 Reutilización flexible del Golden Set heredado (D-60)

Una subcalibración puede **reutilizar** el Golden Set de la calibración global y, al mismo tiempo,
modificar otros criterios, pesos o skills según necesite. También puede **reemplazarlo** por un
Golden Set propio. El Golden Set se hereda como **referencia versionada por defecto**, no como
obligación de copiar contenido. Toda ejecución calibra el **perfil efectivo completo** —base,
overrides, dimensiones y skills— contra el Golden Set finalmente seleccionado. El historial indica si
el recurso fue heredado o reemplazado y conserva su snapshot para reproducibilidad.

### 4.2 Subcriterios referenciados integran la aprobación de calibración (D-66)

El desvío de cada subcriterio **activo con referencia humana** participa de la aprobación o rechazo de
la calibración, con las **mismas reglas de MAE y error individual** aplicadas hoy a las dimensiones
(ver [02 — Especificación funcional §10](02-especificacion-funcional.md#10-cálculo-de-par-14)). La
métrica conserva el MAE final ponderado y **amplía el conjunto de errores individuales** a cada par
caso/subcriterio activo. Un desvío que exceda el umbral vigente hace fallar la corrida. El reporte
diferencia claramente las métricas por dimensión y por subcriterio, sin ocultar cuál causó el fallo.

### 4.3 Referencia humana obligatoria y reporte explicable (D-67)

El Golden Set **siempre** cuenta con referencia humana. Cada caso tiene puntuación humana por
**dimensión** y por **cada subcriterio activo**.

La calibración global y la subcalibración de desafío deben mostrar:

- su puntaje o métricas,
- su estado `PASSED`/`FAILED`,
- **dónde y por qué** aprobó o falló.

El docente debe poder identificar: el caso, la dimensión o subcriterio, la referencia humana, el
puntaje del modelo, el desvío, el umbral aplicable y la configuración efectiva que originó el
resultado. El reporte no se limita a un indicador global: incluye MAE final ponderado, máximo error
individual, desglose por dimensión y —cuando aplique— por subcriterio, además de causas técnicas
controladas (presupuesto de contexto, skill no habilitada, error de proveedor). La vista y la
auditoría conservan el snapshot y hashes de rúbrica, Golden Set, skills y modelo para explicar una
corrida histórica.

### 4.4 Referencia humana obligatoria por subcriterio activo (D-68)

Todo **subcriterio activo** debe tener puntuación humana en cada caso del Golden Set. La validación de
publicación y de inicio de calibración **rechaza** un Golden Set incompleto para los subcriterios
ponderados. El modelo debe devolver el mismo conjunto de puntajes por subcriterio, y la calibración
aplica las métricas acordadas sobre todos ellos. Un subcriterio con peso 0 % permanece trazable, pero
no requiere participar del cálculo ni de la referencia activa.

**Consecuencia sobre D-60:** el Golden Set heredado puede reutilizarse sin cambios **si el desafío no
altera su conjunto de subcriterios activos**. Si agrega o activa uno nuevo, debe completarse la
referencia humana correspondiente antes de calibrar y conservar su procedencia versionada.

### 4.5 Golden Set derivado al extender subcriterios (D-69)

Si una subcalibración agrega o activa subcriterios que **no existen** en el Golden Set heredado, el
sistema crea una **copia derivada** del Golden Set, con sus casos y referencias existentes
precompletados. El docente completa las nuevas referencias humanas allí, **sin modificar el Golden
Set original**.

La copia conserva `baseVersionId`, autor, fecha y trazabilidad de procedencia; nace como **borrador** y
sólo puede seleccionarse al publicarse completa. El original permanece inmutable y apto para sus
calibraciones históricas. La subcalibración referencia la versión derivada publicada y el reporte
permite navegar a su origen.

### 4.6 Golden Set derivado al extender dimensiones (D-70)

Se aplica la **misma regla de copia derivada** cuando la subcalibración agrega una dimensión nueva
(D-57). No se modifica ningún Golden Set publicado ni histórico para adaptarlo. El Golden Set derivado
hereda sus casos y referencias de dimensión existentes, añade el espacio obligatorio para referencias
humanas de la nueva dimensión en cada caso, y se publica sólo al completarlas. Así, la rúbrica
efectiva, las referencias humanas y las métricas de calibración siempre describen exactamente el
mismo conjunto de dimensiones y subcriterios.

## 5. Máximo administrable de subcriterios por dimensión (D-71)

El administrador puede configurar la **cantidad máxima de subcriterios** que puede contener una
dimensión. El límite se valida al editar la rúbrica base o su overlay de desafío, y se muestra antes
de llegar al máximo. Complementa —sin sustituir— los límites de skills (D-27, fuera de este rango) y
de dimensiones adicionales (D-58), y protege la legibilidad del Golden Set, del reporte de
calibración y el presupuesto real de contexto del modelo. **No sustituye** el cálculo preventivo de
tokens (D-153, D-160).

## Trazabilidad de esta sección

| Decisión | Tema |
|---|---|
| D-52 | Rigor ajustable por desafío mediante rúbrica específica versionada. |
| D-53 | Dimensiones heredadas con porcentajes ajustables, suma 100 %. |
| D-54 | Cero permitido, pesos negativos prohibidos. |
| D-55 | Sin criterios globales no desactivables. |
| D-60 | Reutilización flexible del Golden Set heredado o reemplazo propio. |
| D-61 | Desactivación mediante peso cero, redistribución explícita. |
| D-62 | Hallazgo documental: el modelo vigente no soporta dimensiones adicionales — requiere ADR. |
| D-63 | Aclaración: hoy el criterio es texto sin peso propio; subcriterios son un modelo nuevo. |
| D-64 | Subcriterios ponderables dentro de cada dimensión — requiere ADR-021, contrato y migración. |
| D-66 | Subcriterios referenciados integran la aprobación de calibración (MAE/error individual). |
| D-67 | Referencia humana obligatoria y reporte explicable de calibración. |
| D-68 | Referencia humana obligatoria por subcriterio activo. |
| D-69 | Golden Set derivado al extender subcriterios. |
| D-70 | Golden Set derivado al extender dimensiones. |
| D-71 | Máximo administrable de subcriterios por dimensión. |
