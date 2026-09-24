# 03 — Dominio y flujos del MVP

## Estados que protegen la reproducibilidad

| Recurso | Ciclo permitido | Invariante |
|---|---|---|
| Rúbrica | `DRAFT` → `PUBLISHED` | Solo `DRAFT` se edita. Publicar congela esa versión. |
| Golden Set | `DRAFT` → `PUBLISHED` | Sus casos son anonimizados y tienen referencia humana. |
| Calibración | solicitada → en ejecución → `PASSED` o `FAILED` | Solo una corrida aprobada puede quedar activa para una cohorte. |
| Evaluación de intento | recibida → `COMPLETED` o `PENDING` | Un intento cerrado produce como máximo un resultado efectivo por versión aplicable. |

Una nueva versión de rúbrica o Golden Set no altera versiones publicadas ni resultados históricos.

## Flujo: configurar y calibrar

1. Back Office invoca a `admin-service`, que cruza Gateway hacia Tema 07 con identidad docente.
2. El docente crea o modifica una rúbrica y un Golden Set en estado borrador.
3. Publica ambos, selecciona un modelo candidato e inicia una corrida asíncrona.
4. Tema 07 publica `CALIBRATION_APPROVED` o `CALIBRATION_OUT_OF_TOLERANCE`.
5. Una calibración aprobada queda disponible para nuevas prácticas de esa cohorte.

## Flujo: práctica, intento y evaluación

1. Practice publica el hecho de que una práctica quedó disponible; Tema 07 asocia la calibración
   activa al desafío.
2. Al iniciar el primer intento, Practice emite el hecho que inmoviliza esa asociación.
3. Practice llama al tutor cuando el alumno lo necesita. La solución esperada, si existe, se usa
   únicamente para impedir una fuga.
4. Al cerrar el intento, Practice publica la evidencia mínima y Tema 07 encola la evaluación.
5. Tema 07 publica `SCORE_CALCULATED` o `SCORE_DEFERRED`. Practice es quien
   integra el resultado con su flujo y, si corresponde, con Challenges.

Los nombres, schemas y condiciones exactas de los hechos que entran están pendientes de acuerdo en
[requisitos-a-otros-micros.md](contracts/requisitos-a-otros-micros.md); no se deben inferir de
este diagrama.
