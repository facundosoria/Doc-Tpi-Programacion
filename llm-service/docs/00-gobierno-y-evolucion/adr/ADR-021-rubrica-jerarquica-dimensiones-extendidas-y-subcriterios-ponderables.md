# ADR-021 — Rúbrica jerárquica: dimensiones extendidas por desafío y subcriterios ponderables

- **Estado:** Propuesto — pendiente de aprobación explícita de Producto/Arquitectura, contrato y
  migración antes de implementar. No autoriza por sí solo cambios de código, OpenAPI ni persistencia.
- **Fecha:** 2026-09-22
- **Decisiones de origen:** D-57, D-62, D-63, D-64 (y sus decisiones dependientes D-58, D-66 a D-71).
- **Numeración:** este ID es local de la carpeta `adr/`; no es el `ADR-021` de otro registro
  consolidado si llegara a existir uno.

## Contexto

[02 — Especificación funcional de Golden Set y calibración §3](../../03-capacidades-de-ia/golden-set-y-calibracion/02-especificacion-funcional.md#3-cinco-dimensiones-obligatorias)
y [03 — Modelo de dominio y transiciones §Entidades](../../03-capacidades-de-ia/golden-set-y-calibracion/03-modelo-de-dominio-y-transiciones.md#entidades)
fijan un modelo cerrado:

- la rúbrica tiene **cinco dimensiones fijas** (`key`s cerradas: no se agregan, eliminan, duplican ni
  reemplazan);
- cada `RubricDimension` tiene **un único campo de criterio** (texto libre: descripción, anclas,
  prompt) y **un único peso** (`weight`);
- no existe una colección de criterios o subcriterios con peso propio dentro de una dimensión;
- Golden Set, calibración y PAR-14 comparan exactamente esas cinco puntuaciones.

Dos decisiones introducen, para la subcalibración por desafío, capacidades que **no
caben en ese modelo**:

1. **D-57** — una subcalibración puede **agregar dimensiones nuevas** de origen `CHALLENGE`, además de
   heredar y modificar las del curso. D-57 establece textualmente: *"esta decisión reemplaza la
   restricción de D-53 que exigía conservar exactamente el mismo conjunto de dimensiones"* y que
   *"la implementación actual asume un conjunto fijo de dimensiones, por lo que esta capacidad exigirá
   evolucionar el modelo y sus validadores, contratos y persistencia mediante cambios aprobados
   explícitamente"*.
2. **D-64** — cada dimensión incorpora una **lista de subcriterios identificables y ponderables**, que
   distribuyen internamente el 100 % del peso de su dimensión. D-64 lo marca explícitamente: *"esta
   es una evolución material del modelo actual. Requiere ADR, nuevo contrato y migración
   aprobados, más adaptación de Golden Set, resultados por caso, calibración, métricas PAR-14,
   reportes y UI"*.

D-62 es el hallazgo documental que conecta ambas: la regla vigente de "cinco `key`s fijas, un solo
criterio por dimensión, sin subcriterios" es la que estas dos capacidades contradicen. D-63 aclara que
hoy el "criterio" es únicamente texto guía dentro de la dimensión, sin peso propio — por eso D-61
("desactivar un criterio equivale a asignarle 0 %") sólo tiene sentido pleno una vez que existan
subcriterios ponderables de verdad.

Estas dos capacidades **no aplican a la calibración global "pura"** de la misma manera: D-53 mantiene
que la subcalibración se ajusta al conjunto de dimensiones del curso (con porcentajes redistribuibles,
incluido 0 %), y D-57 es la extensión específica que permite agregar dimensiones nuevas **en el
desafío**. Los subcriterios ponderables (D-64), en cambio, se plantean como una capacidad general del
modelo de rúbrica, aplicable tanto a nivel de curso como de desafío.

## Decisión

Se aprueba, a nivel de documentación de arquitectura, **evolucionar el modelo de rúbrica** de
`llm-service` de la siguiente manera, sujeto a que la implementación real cree su propio contrato y
migración Flyway aditivos y aprobados explícitamente (regla no negociable del `AGENTS.md` de
`llm-service`: ninguna migración se ejecuta sin instrucción explícita del usuario):

1. **`RubricDimension` deja de ser una lista cerrada de cinco `key`s.** Una versión de rúbrica sigue
   teniendo un conjunto de dimensiones con peso (`weight`) que **suma exactamente 100 %**, pero ese
   conjunto se compone de:
   - dimensiones de origen `COURSE` (las heredadas de la calibración global, incluidas las cinco
     históricas mientras la plataforma las siga usando como base);
   - dimensiones de origen `CHALLENGE` (agregadas explícitamente por una subcalibración, con
     identificador inmutable propio, ver D-57).
   Cada dimensión conserva identificador estable, nombre, descripción, criterios, anclas, prompt y
   peso ≥ 0. Una dimensión heredada puede llevarse a peso 0 % (D-54) pero permanece trazable en el
   snapshot; nunca se elimina silenciosamente.

2. **Cada dimensión incorpora una colección de `RubricSubcriterion`** (D-64): identificador estable,
   nombre/descripción, y un peso interno que, junto con los demás subcriterios activos de la misma
   dimensión, **suma exactamente 100 %** dentro de esa dimensión. El puntaje de la dimensión se
   calcula como la suma ponderada de sus subcriterios (ver fórmula en
   [07 — Rúbrica ponderada y subcriterios §3.2](../../03-capacidades-de-ia/golden-set-y-calibracion/07-rubrica-ponderada-y-subcriterios.md#32-reglas-de-cálculo)).
   Un subcriterio en 0 % permanece trazable y no contribuye (D-61).

3. **Golden Set extiende su referencia humana** de "un entero 0–100 por dimensión" a "un entero 0–100
   por dimensión **y por cada subcriterio activo**" (D-67, D-68). Cuando una subcalibración activa
   subcriterios o dimensiones que el Golden Set heredado no cubre, el sistema no edita el original:
   crea una **copia derivada** en borrador, con los casos y referencias existentes precompletados, que
   se publica sólo cuando las referencias nuevas quedan completas (D-69, D-70).

4. **PAR-14 extiende su cálculo de error individual** de "por caso y dimensión" a "por caso, dimensión
   y subcriterio activo con referencia humana" (D-66), manteniendo el MAE final ponderado y el máximo
   error individual como las dos condiciones de aprobación ya vigentes en
   [02 — Especificación funcional §10](../../03-capacidades-de-ia/golden-set-y-calibracion/02-especificacion-funcional.md#10-cálculo-de-par-14).

5. **Límites administrables nuevos:** cantidad máxima de dimensiones adicionales por subcalibración
   (D-58) y cantidad máxima de subcriterios por dimensión (D-71), ambos configurables por
   Administración y validados antes de guardar o calibrar, sin sustituir el control preventivo real de
   tokens/contexto (D-28, D-153, D-160).

## Alternativas consideradas

- **Mantener las cinco dimensiones fijas y modelar el "rigor por desafío" únicamente como
  redistribución de pesos (D-53 sin D-57).** Rechazada: D-57 indica explícitamente que
  reemplaza esa restricción; una subcalibración necesita poder expresar una competencia nueva propia
  de un examen (por ejemplo: heredar cuatro dimensiones y agregar una quinta).
- **Modelar subcriterios como texto adicional dentro del campo `criterio` existente, sin peso
  propio.** Rechazada: no permite desactivar (0 %) ni ponderar un subcriterio de forma independiente,
  que es exactamente la capacidad que pide D-61/D-64; tampoco permite que PAR-14 reporte un desvío por
  subcriterio (D-66).
- **Tratar cada combinación dimensión+subcriterio como una "dimensión" más, aplanando la jerarquía.**
  Rechazada: rompería la relación "peso de dimensión × puntaje de dimensión" que hoy sostiene el
  cálculo de PAR-14 y la lectura de reportes por competencia; también complicaría innecesariamente el
  límite administrable de D-58 (dimensiones) frente al de D-71 (subcriterios), que se tratan como
  conceptos distintos.

## Consecuencias

**Positivas:**

- Permite expresar el rigor específico de un examen o desafío sin que el modelo (LLM) tenga que
  arbitrar reglas contradictorias entre dos rúbricas (D-52, D-160).
- El reporte de calibración puede explicar un fallo con precisión de subcriterio, no sólo de
  dimensión (D-67).
- El Golden Set derivado evita que una extensión de un desafío obligue a reeditar (y por lo tanto
  potencialmente invalidar) el Golden Set del curso completo.

**Negativas / costos declarados:**

- Evolución de dominio, contrato OpenAPI, persistencia (migración Flyway aditiva), validadores,
  cálculo de PAR-14, reportes y UI — todo pendiente de una implementación futura aprobada
  explícitamente; **no se toca código ni base de datos con este ADR**.
- Aumenta la superficie de validación (suma 100 % a dos niveles: dimensiones y, dentro de cada una,
  subcriterios) y el volumen de referencias humanas requeridas en el Golden Set.
- Los contratos y nombres de campos definitivos se **congelan en el bloque 6 de contratos** (fuera de
  este ADR), tal como indica D-161 §4 para los estados de calibración.

## Pendientes explícitos antes de implementar

1. Congelar el contrato OpenAPI aditivo de rúbrica jerárquica (dimensiones + subcriterios) en el
   bloque 6.
2. Diseñar y aprobar la migración Flyway aditiva sobre `rubric_dimension` (o tabla equivalente) y la
   nueva tabla de subcriterios, sin alterar datos ni contratos ya vigentes.
3. Adaptar `CalibrationMetrics`/PAR-14 para el desglose por subcriterio (D-66) sin romper la lectura
   histórica de corridas anteriores al modelo de cinco dimensiones fijas.
4. Adaptar el flujo de Golden Set derivado (D-69, D-70) en backend y frontend del workbench.
5. Definir los límites administrables por defecto de D-58 y D-71.

## Referencias

- [07 — Rúbrica ponderada y subcriterios](../../03-capacidades-de-ia/golden-set-y-calibracion/07-rubrica-ponderada-y-subcriterios.md)
  (documentación funcional completa de D-52 a D-71).
- [06 — Subcalibración jerárquica §9.2](../../03-capacidades-de-ia/golden-set-y-calibracion/06-subcalibracion-jerarquica.md#92-extensión-de-dimensiones-en-una-subcalibración-d-57)
  (D-57).
- [02 — Especificación funcional §3](../../03-capacidades-de-ia/golden-set-y-calibracion/02-especificacion-funcional.md#3-cinco-dimensiones-obligatorias)
  y [03 — Modelo de dominio](../../03-capacidades-de-ia/golden-set-y-calibracion/03-modelo-de-dominio-y-transiciones.md)
  (modelo vigente que este ADR evoluciona).
