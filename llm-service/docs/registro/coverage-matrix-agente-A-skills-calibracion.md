# Matriz de cobertura — Agente A (skills y calibración jerárquica)

> Rango asignado: D-01 a D-20, D-39 a D-77, D-120 a D-161 de `llm-service/docs/03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md`.
> `D-65`, `D-72` y `D-75` no existen en el plan (numeración con huecos, verificado por búsqueda en el
> archivo fuente); no generan fila propia.
> Todas las decisiones son **aditivas** salvo D-57/D-62/D-63/D-64 (contradicen el modelo de cinco
> dimensiones fijas de docsV2 y requieren ADR-021) y D-49/D-160/D-161 (reemplazan la asociación simple
> desafío↔calibración de `02-especificacion-funcional.md` §11).

| Decisión | Archivo(s) destino en docsV3 | Acción | Nota de contradicción |
|---|---|---|---|
| D-01 | `03-.../golden-set-y-calibracion/05-skills-catalogo-y-formato.md` §1 | Agregado | Aditiva; no existía documentación de skills en docsV2. |
| D-02 | `05-skills-catalogo-y-formato.md` §2 | Agregado | Aditiva. |
| D-03 | `05-skills-catalogo-y-formato.md` §3 | Agregado | Aditiva. |
| D-04 | `05-skills-catalogo-y-formato.md` §4 | Agregado | Aditiva. |
| D-05 | `05-skills-catalogo-y-formato.md` §5 | Agregado | Aditiva. |
| D-06 | `05-skills-catalogo-y-formato.md` §6 | Agregado | Aditiva. |
| D-07 | `06-subcalibracion-jerarquica.md` §1 | Agregado | Aditiva; sustituye implícitamente la asociación simple de `02-especificacion-funcional.md` §11 para el flujo de subcalibración (ver nota de evolución agregada en ese archivo). |
| D-08 | `06-subcalibracion-jerarquica.md` §2 | Agregado | Ídem D-07. |
| D-09 | `06-subcalibracion-jerarquica.md` §3 | Agregado | Ídem D-07. |
| D-10 | `06-subcalibracion-jerarquica.md` §4.1 | Agregado | Ídem D-07; además conserva explícitamente un punto **pendiente de Producto** (desafío parcialmente realizado) que no debe resolverse por inferencia. |
| D-11 | `06-subcalibracion-jerarquica.md` §4.2 | Agregado | Ídem D-07. |
| D-12 | `04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md` §1.1 | Agregado | Aditiva. |
| D-13 | `04-seguridad-y-gobierno-de-skills.md` §1.2 | Agregado | Aditiva. |
| D-14 | `04-seguridad-y-gobierno-de-skills.md` §1.3 | Agregado | Aditiva. |
| D-15 | `04-seguridad-y-gobierno-de-skills.md` §2 | Agregado | Aditiva. |
| D-16 | `04-seguridad-y-gobierno-de-skills.md` §3 | Agregado | Aditiva. |
| D-17 | `04-seguridad-y-gobierno-de-skills.md` §5.1 | Agregado | Aditiva. |
| D-18 | `04-seguridad-y-gobierno-de-skills.md` §5.2 | Agregado | Aditiva; reutiliza el flujo de seguridad de archivos PDF del RAG ya vigente (`02-arquitectura-y-fronteras.md`, `rag-e-ingesta/`), sin contradecirlo. |
| D-19 | `04-seguridad-y-gobierno-de-skills.md` §5.3 | Agregado | Aditiva. |
| D-20 | `04-seguridad-y-gobierno-de-skills.md` §5.4 | Agregado | Aditiva (la integración asíncrona de la alerta en sí es D-21/D-22, fuera de este rango). |
| D-39 | `05-skills-catalogo-y-formato.md` §7.1 | Agregado | Aditiva. |
| D-40 | `05-skills-catalogo-y-formato.md` §7.2 | Agregado | Aditiva. |
| D-41 | `05-skills-catalogo-y-formato.md` §7.3 | Agregado | Aditiva. |
| D-42 | `05-skills-catalogo-y-formato.md` §7.4 | Agregado | Aditiva. |
| D-43 | `04-seguridad-y-gobierno-de-skills.md` §4 | Agregado | Aditiva; extiende el principio "todo texto de usuario es dato" ya vigente en `01-seguridad-y-guardarrailes.md` §1 al contenido de skills, sin contradecirlo. |
| D-44 | `05-skills-catalogo-y-formato.md` §8 | Agregado | Aditiva. |
| D-45 | `06-subcalibracion-jerarquica.md` §8.1 | Agregado | Aditiva. |
| D-46 | `06-subcalibracion-jerarquica.md` §8.2 | Agregado | Aditiva. |
| D-47 | `06-subcalibracion-jerarquica.md` §4.3 | Agregado | Aditiva. |
| D-48 | `06-subcalibracion-jerarquica.md` §4.4 | Agregado | Aditiva. |
| D-49 | `06-subcalibracion-jerarquica.md` §6 | Agregado | **Reemplaza** el modelo de asociación simple `ChallengeCalibrationAssignment` de `02-especificacion-funcional.md` §11 para el flujo de subcalibración; se agregó nota de evolución explícita en ese archivo en lugar de reescribirlo, porque el contrato definitivo se congela en el bloque 6. |
| D-50 | `06-subcalibracion-jerarquica.md` §5 | Agregado | Aditiva. |
| D-51 | `06-subcalibracion-jerarquica.md` §7 | Agregado | Aditiva. |
| D-52 | `07-rubrica-ponderada-y-subcriterios.md` §2.1 | Agregado | Aditiva sobre el mecanismo; el rigor específico por desafío no existía en docsV2. |
| D-53 | `07-rubrica-ponderada-y-subcriterios.md` §2.2 | Agregado, con precisión | Coexiste con docsV2 (redistribución de pesos sobre el mismo conjunto de dimensiones), pero queda parcialmente reemplazada por D-57 para el conjunto de dimensiones en sí; la nota explica exactamente qué parte sigue vigente y qué parte no. |
| D-54 | `07-rubrica-ponderada-y-subcriterios.md` §2.3 | Agregado | Aditiva (0% no estaba explícitamente permitido/prohibido en docsV2 para subcalibración). |
| D-55 | `07-rubrica-ponderada-y-subcriterios.md` §2.4 | Agregado | Aditiva. |
| D-56 | `06-subcalibracion-jerarquica.md` §9.1 | Agregado | Aditiva. |
| D-57 | `06-subcalibracion-jerarquica.md` §9.2 | Agregado | **Contradice** el conjunto cerrado de dimensiones de `02-especificacion-funcional.md` §3 y `03-modelo-de-dominio-y-transiciones.md` (`RubricDimension`). Requiere ADR-021 (creado). Nota de evolución agregada en ambos archivos de docsV2/V3 heredados; no se reescribieron por migración pendiente de aprobación. |
| D-58 | `06-subcalibracion-jerarquica.md` §9.3 | Agregado | Aditiva (límite administrable nuevo). |
| D-59 | `06-subcalibracion-jerarquica.md` §9.4 | Agregado | Aditiva; explícitamente no bloqueante para la primera entrega. |
| D-60 | `07-rubrica-ponderada-y-subcriterios.md` §4.1 | Agregado | Aditiva. |
| D-61 | `07-rubrica-ponderada-y-subcriterios.md` §3.4 | Agregado | Aditiva sobre el mecanismo general; su aplicación plena a "un criterio" depende del modelo nuevo de subcriterios (D-64). |
| D-62 | `07-rubrica-ponderada-y-subcriterios.md` §1 | Agregado (hallazgo documental) | Es la propia decisión que **declara** la contradicción con docsV2 y exige ADR — documentado íntegro, con cita textual. |
| D-63 | `07-rubrica-ponderada-y-subcriterios.md` §1 | Agregado (aclaración) | Aclara el modelo vigente (criterio = texto sin peso propio) antes de introducir subcriterios ponderables. |
| D-64 | `07-rubrica-ponderada-y-subcriterios.md` §3 | Agregado | **Contradice** materialmente el modelo de docsV2 (`RubricDimension` sin subcriterios). Requiere ADR-021 (creado), contrato y migración aprobados — explícitamente no implementado, documentado como pendiente. |
| D-66 | `07-rubrica-ponderada-y-subcriterios.md` §4.2 | Agregado | Depende de D-64; documentado junto a su impacto en PAR-14. |
| D-67 | `07-rubrica-ponderada-y-subcriterios.md` §4.3 | Agregado | Aditiva/ampliación del reporte de calibración ya descripto en `02-especificacion-funcional.md` §13-14, sin contradecirlo. |
| D-68 | `07-rubrica-ponderada-y-subcriterios.md` §4.4 | Agregado | Depende de D-64. |
| D-69 | `07-rubrica-ponderada-y-subcriterios.md` §4.5 | Agregado | Aditiva (Golden Set derivado es mecanismo nuevo). |
| D-70 | `07-rubrica-ponderada-y-subcriterios.md` §4.6 | Agregado | Aditiva. |
| D-71 | `07-rubrica-ponderada-y-subcriterios.md` §5 | Agregado | Aditiva (límite administrable nuevo). |
| D-73 | `06-subcalibracion-jerarquica.md` §10.1 | Agregado | Aditiva. |
| D-74 | `06-subcalibracion-jerarquica.md` §10.2 | Agregado | Aditiva. |
| D-76 | `06-subcalibracion-jerarquica.md` §10.3 | Agregado | Aditiva. |
| D-77 | `06-subcalibracion-jerarquica.md` §11 | Agregado | Aditiva. |
| D-120 | `05-skills-catalogo-y-formato.md` §9.1 | Agregado | Aditiva. |
| D-121 | `05-skills-catalogo-y-formato.md` §9.2 | Agregado | Aditiva. |
| D-122 | `05-skills-catalogo-y-formato.md` §9.3 | Agregado | Aditiva. |
| D-123 | `05-skills-catalogo-y-formato.md` §9.4 | Agregado | Aditiva. |
| D-124 | `05-skills-catalogo-y-formato.md` §10.1 | Agregado | Aditiva. |
| D-125 | `05-skills-catalogo-y-formato.md` §10.2 | Agregado | Aditiva. |
| D-126 | `05-skills-catalogo-y-formato.md` §10.3 | Agregado | Aditiva. |
| D-127 | `05-skills-catalogo-y-formato.md` §10.4 | Agregado | Aditiva. |
| D-128 | `04-seguridad-y-gobierno-de-skills.md` §6.1 | Agregado, con nota de alcance | Decisión de contenido vigente; el **pipeline** que la implementa es el bloque técnico 4, declarado fuera de alcance del sprint por el propio plan — documentado explícitamente como referencia futura, sin generar tareas/contratos/migraciones. |
| D-129 | `04-seguridad-y-gobierno-de-skills.md` §6.2 | Agregado, con nota de alcance | Ídem D-128. |
| D-130 | `04-seguridad-y-gobierno-de-skills.md` §6.3 | Agregado, con nota de alcance | Ídem D-128. |
| D-131 | `04-seguridad-y-gobierno-de-skills.md` §6.4 | Agregado, con nota de alcance | Ídem D-128. |
| D-132 | `04-seguridad-y-gobierno-de-skills.md` §6.5 | Agregado, con nota de alcance | Ídem D-128 (reproceso posterior al pipeline). |
| D-133 | `04-seguridad-y-gobierno-de-skills.md` §6.6 | Agregado | Aditiva; no depende del pipeline en sí, sino del aviso posterior al alumno. |
| D-134 | `05-skills-catalogo-y-formato.md` §3.1 | Agregado | Aditiva. |
| D-135 | `05-skills-catalogo-y-formato.md` §3.2 | Agregado | Aditiva. |
| D-136 | `05-skills-catalogo-y-formato.md` §3.3 | Agregado | Aditiva. |
| D-137 | `08-maquina-de-estados-corridas-y-activaciones.md` §5.1 | Agregado | Aditiva (control de concurrencia optimista no descripto en docsV2 para activación). |
| D-138 | `08-maquina-de-estados-corridas-y-activaciones.md` §5.2 | Agregado | Aditiva. |
| D-139 | `08-maquina-de-estados-corridas-y-activaciones.md` §6.1 | Agregado | Aditiva. |
| D-140 | `08-maquina-de-estados-corridas-y-activaciones.md` §6.2 | Agregado | Aditiva. |
| D-141 | `08-maquina-de-estados-corridas-y-activaciones.md` §6.3 | Agregado | Aditiva. |
| D-142 | `09-flujos-ui-calibracion-y-gestion-skills.md` §1 | Agregado | Aditiva. |
| D-143 | `09-flujos-ui-calibracion-y-gestion-skills.md` §2 | Agregado | Aditiva. |
| D-144 | `09-flujos-ui-calibracion-y-gestion-skills.md` §3.1 | Agregado | Aditiva. |
| D-145 | `09-flujos-ui-calibracion-y-gestion-skills.md` §3.2 | Agregado | Aditiva. |
| D-146 | `09-flujos-ui-calibracion-y-gestion-skills.md` §3.3 | Agregado | Aditiva. |
| D-147 | `09-flujos-ui-calibracion-y-gestion-skills.md` §4.1 | Agregado | Aditiva. |
| D-148 | `09-flujos-ui-calibracion-y-gestion-skills.md` §4.2 | Agregado | Aditiva. |
| D-149 | `09-flujos-ui-calibracion-y-gestion-skills.md` §4.3 | Agregado | Aditiva. |
| D-150 | `09-flujos-ui-calibracion-y-gestion-skills.md` §5.1 | Agregado | Aditiva. |
| D-151 | `09-flujos-ui-calibracion-y-gestion-skills.md` §5.2 | Agregado | Aditiva. |
| D-152 | `09-flujos-ui-calibracion-y-gestion-skills.md` §6 | Agregado | Aditiva. |
| D-153 | `09-flujos-ui-calibracion-y-gestion-skills.md` §7 | Agregado | Aditiva. |
| D-154 | `05-skills-catalogo-y-formato.md` §4.1 | Agregado | Aditiva. |
| D-155 | `08-maquina-de-estados-corridas-y-activaciones.md` §3.1 | Agregado | Aditiva; docsV2 no fijaba explícitamente la ausencia de cancelación manual. |
| D-156 | `08-maquina-de-estados-corridas-y-activaciones.md` §3.2 | Agregado | Aditiva. |
| D-157 | `08-maquina-de-estados-corridas-y-activaciones.md` §4 | Agregado | Aditiva. |
| D-158 | `08-maquina-de-estados-corridas-y-activaciones.md` §7 | Agregado, con nota de alcance | La regla de concurrencia/admisión se documenta completa; el libro de cuota persistente (bloque técnico 2) queda explícitamente fuera de alcance del sprint, tal como indica el plan. |
| D-159 | `08-maquina-de-estados-corridas-y-activaciones.md` §0 | Agregado | Aditiva; sección propia con "Decisión técnica" e "Implicancia de diseño" íntegras (inventario completo de agregados que persiste LLM y separación entre snapshots inmutables y metadatos/estados operativos), ubicada como introducción al modelo de agregados antes de la máquina de estados de D-161, con referencias cruzadas a dónde vive el detalle de cada agregado (05, 06, 04-seguridad-y-gobierno-de-skills). Corregido el 2026-09-22 tras observación del coordinador: la versión anterior sólo mencionaba D-159 sin contenido sustantivo propio. |
| D-160 | `06-subcalibracion-jerarquica.md` §6 y `08-maquina-de-estados-corridas-y-activaciones.md` (referenciado como "compilador determinista") | Agregado | Aditiva; documentado en el contexto del modelo híbrido (§6 de 06) porque es el mecanismo que resuelve `CourseBaseline`+`ChallengeOverlay`, con referencias cruzadas desde 07, 08 y 09. |
| D-161 | `08-maquina-de-estados-corridas-y-activaciones.md` §1 | Agregado | Pieza central del modelo de dominio; documentada completa (los cuatro subapartados: `CalibrationRun`, `CalibrationActivation`, `CalibrationVerification`, estado efectivo y concurrencia), incluida la nota explícita de que el estado `CANCELLED` histórico no tiene transición habilitada y se deprecará en el bloque 6. |

## Notas generales de cobertura

- **D-159** tiene sección propia y completa en `08-maquina-de-estados-corridas-y-activaciones.md` §0
  (ver fila arriba). **D-160** es una decisión técnica transversal que el propio plan liga directamente
  al modelo híbrido de subcalibración (D-49): se documentó integrada en `06-subcalibracion-jerarquica.md`
  §6 en lugar de crear una sección redundante, con referencias cruzadas desde 07, 08 y 09.
- Ninguna decisión del rango quedó "ya cubierta sin cambios": todas eran aditivas o de evolución
  respecto de docsV2, porque `docs/03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md` introduce una funcionalidad —skills y
  calibración jerárquica— que docsV2 no documentaba todavía.
- El corte de alcance del sprint (bloques 2, 4 y 9) se respetó explícitamente en D-128 a D-132 y D-158:
  se documentó la decisión de Producto completa, marcando con una nota visible qué parte de su
  implementación queda pendiente y por qué, sin generar tareas, contratos, endpoints, eventos, workers
  ni criterios de aceptación para esos bloques.
