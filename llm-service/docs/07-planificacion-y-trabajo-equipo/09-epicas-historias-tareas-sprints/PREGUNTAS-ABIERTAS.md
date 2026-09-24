# Preguntas abiertas — auditoría de historias (todas las épicas)

> Generado por la auditoría historia-por-historia de `docs/historias/ep-XX/` iniciada el
> 2026-09-13. Cada ítem es una pregunta de producto, proceso o decisión técnica que **no**
> corresponde resolver por cuenta propia durante la auditoría — queda acá para tratarla con el
> Product Owner / referente de producto / equipo, historia por historia, sin bloquear el avance
> de la auditoría misma.
>
> Formato: `[EP-XX·Hyy] Pregunta — por qué importa (fuente)`. Se marca `✅ resuelta` con fecha
> cuando alguien la cierre, no se borra (queda como historial de decisiones).

---

## Transversales (afectan a varias épicas / a todo el backlog)

- [ ] **¿Ya ocurrió el Sprint 0 formal** (Planning Poker, historia canónica elegida y estimada)?
  Varias fichas siguen con el placeholder "Referente de producto: a nombrar en Sprint 0" pese a
  que ya hay commits de sprints avanzados (p. ej. T7 en EP-04). Si ya pasó, falta volcar el
  resultado en las fichas; si no pasó, el equipo viene estimando/implementando sin ese paso.
- [ ] **Nombrar al "referente de producto"** en todas las fichas que todavía dicen *"a nombrar en
  Sprint 0"* (EP-01 H01–H07, EP-02 H01–H03, EP-03 H01–H05 y las que se sumen).

## EP-01 · Plataforma, contratos e integración

- [ ] **[EP-01·H01]** ¿El ADR de convenciones técnicas ya se redactó y mergeó en algún commit
  posterior, o el hueco reportado en `estado-implementacion/ep-01/h01.md` (🔴 "no existe ningún
  ADR") sigue abierto? Es bloqueante por diseño de la propia ficha.
- [ ] **[EP-01·H01]** ¿CA4/CA5 (rechazar imports de framework en `domain`, variables de entorno no
  documentadas) se hacen cumplir con ArchUnit automatizado o con disciplina de revisión manual?
  La ficha no lo fija y no hay evidencia de cuál es la fuente real hoy.
- [ ] **[EP-01·H03]** ¿El código nunca devuelve `401` (todo colapsa a `403`,
  `estado-implementacion/ep-01/h03.md`) es una decisión de seguridad deliberada (no revelar si el
  problema es de autenticación o autorización) o un hueco de implementación? Define si se corrige
  el código o se reescribe CA4/Escenario 2 de [h03.md](historias/ep-01/h03.md).
- [ ] **[EP-01·H03]** ¿Existe hoy un pipeline de CI real para `llm-service` (CA6)? La auditoría de
  código no encontró workflow en `.github/workflows/`.
- [x] ✅ **2026-09-21 [EP-01·H05]** ¿Con qué herramienta se va a levantar el mock del golden set con "un solo
  comando" (CA2)? Prism sobre el OpenAPI publicado: se ejecutó y responde conforme al contrato (URL sin el prefijo `/api/llm`;
  ver `contracts/MOCK.md`).
- [ ] **[EP-01·H05]** ¿La adenda S1 del contrato ya está revisada y firmada por `admin-service`, o
  sigue pendiente?
- [ ] **[EP-01·H06]** ¿Qué herramienta de cobertura (JaCoCo u otra) se usa para el gate de CI
  (CA2/CA5)? No hay evidencia de que se haya decidido.
- [ ] **[EP-01·H06]** ¿Existe algún borrador de la guía de demo paso a paso, o hay que escribirla
  desde cero?
- [ ] **[EP-01]** Falta una historia futura (propuesta como [H07](historias/ep-01/h07.md), pendiente de alta
  en `35`) que cubra el CA de épica "publica/consume eventos sin duplicarlos" — confirmar en qué
  sprint de EP-01 (S3/S6/S10/S19) entra.

## EP-02 · AI Gateway, modelos y resiliencia

- [ ] **[EP-02·H02]** ¿El código de referencia `GroqAdapter` preservado en
  [`codigo-ejemplo/ms-evaluacion-llm.md`](../../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/codigo-ejemplo/ms-evaluacion-llm.md)
  sigue completo/compilable? Confirmar antes de comprometer la estimación de ~15–22 h.
  - Resuelto: ✅ **2026-09-13.** Es el único hueco de código propio del alcance obligatorio de la
    cátedra ([38 · Parte 1](../05-plan-de-cinco-sprints.md)) — prioridad ya confirmada como Must, no
    hace falta re-confirmarla.
- [ ] **[EP-02·H02]** ¿Groq es la elección definitiva de proveedor real, o solo la referencia
  heredada del código de ejemplo? El ADR (EP-01·H01) debería nombrarlo si ya es firme.
- [ ] **[EP-02]** Falta una historia (propuesta como [H03](historias/ep-02/h03.md), pendiente de alta en
  `35`) que cubra la restricción de épica de reintentos/circuit breaker — H01/H02 solo resuelven
  el timeout de una llamada individual.

## EP-03 · Golden set y referencia humana

- [ ] **[EP-03·H02]** La designación de "historia canónica" quedó sobre una ficha (H02, ex-H06)
  cuyo código real fue reemplazado por H05 (`605f381`). ¿Se estima Fibonacci contra H02 (tal como
  está documentada) o contra H05 (el código vigente)? Son tamaños de esfuerzo distintos.
- [ ] **[EP-03·H03]** ¿La pantalla de `llm-workbench` ya se migró para consumir los endpoints de
  H04/H05 (`/api/llm/courses/{courseId}/...`), o sigue apuntando al contrato viejo de H01/H02?
  Ninguna ficha vigente lo confirma.
- [ ] **[EP-03·H04]** *(ya señalada en la propia ficha)* ¿El invariante de pesos 30/25/20/15/10
  ([23 §4.2](../03-plan-de-construccion-del-producto.md)) aplica solo a la rúbrica base de
  plataforma, o también a cada rúbrica por curso? El código hoy solo exige sumar 100.
- [ ] **[EP-03·H05]** *(ya señalada en la propia ficha)* ¿Quién crea/publica la base de plataforma
  que `copyFromPublishedBase` necesita leer? Hoy no existe ningún endpoint que la genere.
- [x] ✅ **2026-09-13 [EP-03]** Faltaba la historia de **doble puntuación independiente y
  resolución de discrepancias** — redactada como propuesta en
  [`ep-03/h06.md`](historias/ep-03/h06.md), pendiente de alta en `35` y de estimación en Refinamiento.
- [ ] **[EP-03]** Ningún CA de H01/H02/H05 mide el KPI de épica "las consultas se resuelven en
  menos de 100 ms" — todas dicen "baja latencia" en prosa, sin umbral verificable.
- [ ] **[EP-03]** `synthetic-golden-set` y `eligible-interactions-golden-set` (código ya
  confirmado dentro de EP-03, ambos 🔴 placeholder) no tienen ficha de historia redactada —
  deuda documental, no solo de código.

## EP-04 · Calibración y gobernanza del modelo

> La carpeta ya es ejemplar en autodocumentar sus propios huecos (H01 trae una sección
> "⚠️ Diferencia de alcance con la épica" y H02 es la respuesta propuesta) — menos preguntas
> nuevas que en otras épicas, quedan las que siguen sin decisión.

- [ ] **[EP-04·H01]** El archivo mezcla el checklist de CA/Escenarios BDD (que el template exige
  dejar como `- [ ]` sin marcar y sin código inline para pegar en Taiga) con anotaciones de
  estado reales (`[x]`, 🟢, nombres de test entre backticks). Antes de pegar esta ficha en Taiga
  hay que separar "criterio" de "evidencia de que ya se cumplió" — si no, el renderer de Taiga en
  modo lectura descoloca las tildas (regla ya escrita en el propio template).
- [ ] **[EP-04·H02]** ¿Se reutiliza `calibration_runs` con `course_id` nulo para representar
  "plataforma", o conviene una tabla separada? Decisión de diseño de datos abierta, ya señalada
  en la propia ficha.
- [ ] **[EP-04·H03]** El tiempo límite de vencimiento por inactividad no tiene valor por defecto
  — a definir con el Product Owner en Refinamiento (la ficha ya lo marca, recomienda arrancar
  conservador).
- [ ] **[EP-04·H03] — hallazgo nuevo de esta auditoría:** la épica exige explícitamente
  *"cambiar de modelo dispara una recalibración con alertas"*, pero H03 solo dispara vencimiento
  por nueva rúbrica, nuevo golden set o tiempo límite — **cambiar el despliegue de modelo no está
  en la lista de disparadores de vencimiento**. Falta agregarlo como CA/escenario o confirmar que
  se cubre en otro lado.
- [ ] **[EP-04·H04]** ¿Qué canal usa el evento de "curso con evaluaciones frenadas" — el mismo bus
  de `CALIBRATION_OUT_OF_TOLERANCE` u otro propio? La ficha lo deja explícitamente abierto.
- [ ] **[EP-04] — hallazgo nuevo:** el KPI de épica *"toda habilitación o cambio de modelo queda
  registrada de forma permanente"* no tiene ningún CA dedicado en H01/H02 que verifique el
  registro de auditoría de habilitación/cambio de modelo en sí (más allá del audit log genérico
  de H04·EP-01).

---

## EP-05 · Tutor seguro y guardarraíles

- [x] ✅ **2026-09-13 [EP-05·H03]** La ficha dependía de **`challenges-service`** para consultar
  si un desafío está abierto/cerrado ("el equipo del `challenges-service` debe confirmar el
  endpoint o evento..."), pero [00 §6](../../00-gobierno-y-evolucion/01-fuentes-de-verdad-y-convenciones.md), cambiado el
  **2026-09-13**, dice explícitamente que **`llm-service` dejó de comunicarse directo con
  `challenges-service`** — todo pasa ahora por `practice-service`. Corregida la sección de
  Dependencias de [`ep-05/h03.md`](historias/ep-05/h03.md) para consultar el estado del desafío vía
  `practice-service`.
- [x] ✅ **2026-09-13 [EP-05]** Faltaba la historia de **cuota por alumno** (KPI de épica
  "aviso claro al superar la cuota") — redactada como propuesta en
  [`ep-05/h04.md`](historias/ep-05/h04.md), reutilizando el mecanismo de `EP-07·H02`/`H03` en vez de
  duplicarlo. Pendiente de alta en `35` y de que producto fije el valor numérico del límite.
- [ ] **[EP-05] — hallazgo nuevo:** la épica exige *"si el tutor no está disponible, el alumno
  puede seguir su intento sin asistencia, y eso queda registrado"* — ninguna ficha cubre qué
  registra `llm-service` cuando el tutor está caído/degradado (más allá del guardarraíl normal).
- [ ] **[EP-05·H02/H03]** Solapamiento parcial de escenarios entre H02 (lista/historial de
  conversación) y H03 (mismo historial + control de pertenencia): ambas prueban "historial
  ordenado" y "conversación inexistente/no accesible". Es una capa de endurecimiento válida
  (H03 depende de H02), pero conviene revisar en Refinamiento si no convendría fusionar el CA de
  ownership dentro de H02 en vez de repetir el escenario base en una tercera ficha (criterio I de
  INVEST — ver [29 §3](../08-guia-de-historias-de-usuario.md)).
- [ ] **[EP-05·H02]** `ConversationRepository`/`MessageRepository` (JDBC directo) siguen en 0 % de
  cobertura de integración real por bloqueo de Docker/Testcontainers en la sesión que las
  construyó — no dar el CRUD por verificado de punta a punta hasta correr esa integración.
- [ ] **[EP-05·H01]** CA6 (rechazo sin autenticación de servicio válida) está cubierto por código
  (`TutorGatewayAuthorization`) pero sin test dedicado a nivel de esta ficha todavía.

---

## EP-06 · Evaluación, score y auditoría académica

> Calidad de redacción notablemente alta (H04/H05/H06 traen wireframes, ejemplos JSON exactos y
> distinguen bien "pendiente por falta de calibración" de "pendiente por falla del evaluador").
> Todavía son borradores (⚪), la carpeta completa arrancó en 🔴 (cero código).

- [x] ✅ **2026-09-13 [EP-06·H04/H05]** Faltaba todo mecanismo para sacar una apelación de
  `PENDING_REVIEW` (bloqueaba el cierre de curso de H06 para siempre). Corregido: **H05** ahora
  acepta un `appealId` opcional en el override (lo resuelve como `ADJUSTED`) y agrega el endpoint
  `POST .../appeals/{appealId}/resolve` para que el docente confirme la nota sin cambiarla
  (`UPHELD`) — mismo patrón que [`EP-08·H04`](historias/ep-08/h04.md). **H04** referencia la corrección.
  Nuevos CA6–CA8/Escenarios 5–6 en H05.
- [ ] **[EP-06]** Ninguna pareja líder confirmada en el catálogo para EP-06 (todas las fichas
  dicen "a asignar").
- [ ] **[EP-06·H01]** Decisión de diseño abierta: ¿la evaluación "pendiente por intento recién
  cerrado, no arrancada" (EP-06) es un estado distinto o el mismo que "pendiente por falta de
  calibración" (EP-04, `pending_evaluations`)? Ya señalada en la propia ficha como riesgo.
- [ ] **[EP-06·H02/H03]** El schema de `data` de `SCORE_CALCULATED` y
  `SCORE_DEFERRED` sigue siendo un `Envelope` vacío en el AsyncAPI — falta acordarlo
  con quien consume esos eventos (probablemente el servicio de cálculo académico) antes de
  implementar la publicación. Ya señalado en ambas fichas, no inventado por ellas — correcto.
- [ ] **[EP-06·H03]** Intervalo entre reintentos y límite antes de escalar a "pendiente
  prolongada" sin valor por defecto — a definir en Refinamiento (ya señalado en la ficha).
- [ ] **[EP-06·H04]** El plazo de apelación depende de que `courses-service` exponga esa
  configuración por curso — si no está listo, la propia ficha ya propone un plan de contingencia
  razonable ("siempre abierto" en una primera iteración, marcado explícitamente como
  provisional). Buena práctica a imitar en otras fichas con la misma dependencia externa.
- [ ] **[EP-06·H05]** La verificación de rol `TEACHER` por curso (no solo por plataforma) es la
  pieza de seguridad más crítica de la ficha y depende de un contrato con
  identidad/`courses-service` todavía no confirmado — la ficha ya advierte no lanzar con un
  chequeo de rol "plano" sin esa verificación.
- [ ] **[EP-06·H06]** Riesgo ya anotado en la propia ficha: si H01 o H04 no están construidas
  todavía, este endpoint devuelve `canClose: true` siempre, aunque haya pendientes reales — no
  desplegar sin marcarlo explícitamente como comportamiento provisional.

---

## EP-07 · Operación, cuotas y observabilidad

> Calidad de redacción excelente (H01–H03): ejemplos JSON completos, CA con valores numéricos
> exactos, BDD verificable literalmente. Mismo nivel que EP-06.

- [x] ✅ **2026-09-13 [EP-07]** Faltaban las 3 fichas de S10 indexadas sin archivo — redactadas:
  [`h04.md`](historias/ep-07/h04.md) (recuperar trabajos detenidos), [`h05.md`](historias/ep-07/h05.md) (salud útil
  sin el proveedor), [`h06.md`](historias/ep-07/h06.md) (prueba de carga y backup/restore).
- [ ] **[EP-07·H03] / [EP-02] — hallazgo nuevo, cruce entre épicas:** `LLM-S09-H03` (EP-07) ya
  especifica **reintentos con backoff exponencial + jitter y circuit-breaker por función** contra
  el proveedor de modelos — es prácticamente el mismo mecanismo que la propuesta
  [`EP-02·H03`](historias/ep-02/h03.md) (armada en esta misma auditoría) para resolver la restricción de
  resiliencia síncrona de EP-02. **Hay que decidir en Refinamiento cuál construye el decorador de
  resiliencia real** (recomendado: EP-02, por ser la épica dueña conceptual de "resiliencia
  síncrona de la llamada al modelo") **y que la otra lo reutilice** en vez de reimplementarlo —
  si se construyen las dos por separado, quedan dos circuit-breakers independientes vigilando la
  misma llamada al proveedor, lo cual es peor que no tener ninguno (estados inconsistentes entre
  ambos). Ya anoté esta corrección en la propia ficha de `EP-02·H03`.
- [ ] **[EP-07·H01]** Depende de `LLM-S03-H11` (proveedor real conectado, ex-numeración de
  `EP-02·H02`) — coherente, pero confirmar que la referencia se actualice al ID vigente
  `LLM-EP02-H02`.
- [ ] **[EP-07·H01]** Riesgo ya anotado en la ficha: el costo es **estimado** (tokens reportados ×
  precio configurado), no viene de facturación real del proveedor — aclarar esto en cualquier
  demo para no generar expectativas de precisión contable.

---

## EP-08 · Moderación integrada (F2)

> Misma calidad alta que EP-06/EP-07 (contratos JSON exactos, wireframes, CA medibles). Buena
> práctica a destacar: **H04 (docente resuelve incidente) sí cierra correctamente el ciclo de
> apelación** — resuelve `CONFIRMED`/`REVERSED`, notifica al alumno y dispara el desbloqueo. Es
> exactamente el patrón que le falta a EP-06 (ver arriba) — vale la pena usar H04 como plantilla
> al corregir EP-06·H04/H05.

- [x] ✅ **2026-09-13 [EP-08]** Faltaban las 2 fichas de S13 indexadas sin archivo — redactadas:
  [`h05.md`](historias/ep-08/h05.md) (degradación cuando el clasificador contextual falla),
  [`h06.md`](historias/ep-08/h06.md) (retención y purga de evidencia).
- [ ] **[EP-08·H01]** El contrato define `409 Conflict` para "mismo `message_id` con texto
  distinto", pero el CA_negativo_2 describe el mismo caso devolviendo la decisión original en vez
  de 409 — hay una pequeña inconsistencia entre el cuerpo de ejemplo (`RESPONSE 409`) y el CA/BDD
  correspondiente (que no menciona código de estado); conviene unificar cuál es el comportamiento
  exacto antes de implementar.
- [ ] **[EP-08·H02]** Los umbrales de detección "ofensivo" son configurables por curso (política
  del docente) — falta el endpoint o mecanismo de configuración en sí; no hay ficha que lo cubra
  (¿vive acá, en un futuro H de EP-08, o en el panel de EP-07?).

---

## EP-09 · RAG y consulta de material (F3)

> Dos fichas, honestas sobre su propio alcance reducido ("adelanto", embeddings fake, PDF como
> `BYTEA` en vez de referencia externa — todo declarado explícitamente, no escondido).

- [ ] **[EP-09] — hallazgo nuevo:** el KPI de épica *"el asistente encuentra el material correcto
  en al menos el 85 % de las consultas de prueba"* no tiene ningún CA en H01/H02 que lo mida —
  ambas fichas verifican mecanismos (citas, abstención, filtro por cohorte) pero no un corpus de
  prueba con métrica de recall. Falta definir ese conjunto de prueba y su umbral antes de dar la
  épica por cerrada, aunque no bloquea construir H01/H02 tal como están.
- [ ] **[EP-09·H01]** Decisión ya tomada y documentada como simplificación temporal: PDF guardado
  como `BYTEA` en Postgres en vez de referencia externa (contradice el catálogo original de S15,
  "no guardar binarios") — a revisar antes de pasar de adelanto a compromiso de sprint real.
  Ya está en el propio estado de implementación, no es un hallazgo nuevo, solo lo dejo listado.
- [ ] **[EP-09·H02]** La autorización con scope propio para RAG queda "pendiente de decisión de
  producto final" — ya señalado en la ficha, sin resolver todavía.

---

## EP-10 · Personalización y agente (F3)

- [x] ✅ **2026-09-13 [EP-10]** Faltaban las 2 fichas de "@agente" indexadas sin archivo —
  redactadas: [`h03.md`](historias/ep-10/h03.md) (mención citada y moderada),
  [`h04.md`](historias/ep-10/h04.md) (validación de mención real, anti-bucle entre bots).
- [ ] **[EP-10·H02] — hallazgo nuevo, inconsistencia de stack:** la ficha especifica la cola
  durable del job de generación como **"Celery + Redis (AOF)"** — Celery es un framework de colas
  de **Python**, incompatible con el stack declarado en el ADR de EP-01 (Java 21 / Spring Boot 3).
  El propio patrón de "estado persistido + worker que retoma por checkpoint" ya existe funcionando
  en el repo en Java puro (`CalibrationRunWorker` de EP-04, `@Scheduled` + tabla de estado) — lo
  más consistente es reusar ese mismo patrón acá en vez de introducir Celery/Redis. Revisar antes
  de comprometer esta historia a un sprint: probablemente quedó de una referencia genérica sin
  adaptar al stack real del proyecto.
- [ ] **[EP-10·H01]** Dependencia de `LLM-S09-H03` (cuotas) — coherente con el resto del backlog,
  sin objeciones nuevas.

---

## Cómo usar este documento

Cada vez que el equipo resuelva un ítem, marcarlo con fecha y decisión tomada (no borrar la línea,
así queda historial). Los ítems marcados **"hallazgo nuevo"** son los que esta auditoría encontró
por primera vez (no estaban ya señalados en la ficha o en `estado-implementacion/`); el resto son
preguntas que la propia documentación ya dejaba abiertas y esta auditoría solo consolidó en un
único lugar.

### Patrón transversal — ✅ cerrado el 2026-09-13

Los tres patrones que se repetían en varias épicas ya quedaron resueltos con fichas redactadas
(todas siguen pendientes de **alta formal en `35`, estimación en Refinamiento y revisión del
equipo** — redactarlas no las convierte en compromiso de sprint):

1. **Índices de README que prometían más fichas de las que existían** — EP-07 (h04–h06), EP-08
   (h05–h06) y EP-10 (h03–h04) ya están escritas.
2. **Ciclos de apelación/revisión que no cerraban** — EP-06·H04/H05 corregidas con el vínculo
   apelación↔override, siguiendo el patrón que ya usaba EP-08·H04.
3. **Historias que faltaban porque un CA de épica no tenía ninguna ficha** — EP-01·H07, EP-02·H03,
   EP-03·H06 y EP-05·H04 redactadas como propuestas.

**Lo único que sigue pendiente de acción real** (no de redacción): que el equipo revise las 11
fichas nuevas/corregidas de esta pasada, las estime, les asigne pareja/referente de producto, y
decida en qué sprint entra cada una — nada de esto se inventa desde una auditoría de documentos.
