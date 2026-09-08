# Definition of Ready (DoR) y Definition of Done (DoD)

Las **dos compuertas** de cada historia. Una no reemplaza a la otra y ninguna se negocia
por fecha. Los criterios de aceptación son **propios de cada historia**; la DoR y la DoD
son **comunes a todas**.

Este archivo trae una DoR/DoD de referencia. Si el equipo tiene la suya propia, esa
manda: pídela al invocar el skill.

| | **Definition of Ready — DoR** | **Definition of Done — DoD** |
|---|---|---|
| **Qué es** | La historia está **lo bastante entendida y sin bloqueos** para empezar a construirla. | La historia está **realmente terminada**: el usuario la puede usar y hay evidencia. |
| **Cuándo se aplica** | En Refinamiento y Planning, **antes** de comprometerla al sprint. | En la Review, **antes** de aceptarla como entregada. |
| **Pregunta que responde** | ¿Podemos empezar esto ya, sin adivinar ni esperar a nadie? | ¿Esto quedó hecho de verdad, o falta algo que no se ve? |
| **Si no se cumple** | La historia **no entra** al sprint: se refina o se divide. | La historia **vuelve a _en progreso_**: no se presenta como terminada. |

---

## DoR — una historia puede comprometerse en un sprint solo si tiene

1. **Usuario y resultado observable.** El **COMO** nombra un rol real (docente, alumno,
   operador, ADMIN…), no «el sistema» ni «el equipo». Un habilitador puramente técnico
   se registra como **tarea**, no como HU de valor.
2. **Criterios de aceptación comprobables en BDD** (Dado / Cuando / Entonces), con al
   menos **un camino feliz y dos negativos** (no autorizado, entrada inválida,
   duplicado, dependencia caída). Cada *Entonces* describe algo observable.
3. **Requisito trazable** (`RF-*` / `PAR-*`) y **referente de producto** nombrado.
4. **Contrato definido** si cruza servicios: esquema, autenticación, correlación e
   idempotencia de cada consumidor/productor.
5. **Responsable y suplente** asignados.
6. **Datos de prueba** autorizados, o un fixture sintético claramente etiquetado.
7. **Dependencias externas** con dueño, fecha de disponibilidad y alternativa explícita.
   Un mock habilita trabajo interno, pero **no** cierra una historia cuya demo exige
   integración real.
8. **Estimación** hecha en equipo, al final del refinamiento y **después** de tener los
   criterios de aceptación. En Taiga se registra en **puntos Fibonacci** contra la
   historia canónica; en el plan de sprints, en horas. El trabajo de integración va
   incluido y la historia entra **dentro de la capacidad** del sprint: si supera ~40 h
   (o llega a 13 puntos) se divide verticalmente antes de entrar.
9. **Historia sana según INVEST.** Si falla la **V**, casi siempre es una tarea técnica;
   si falla la **S**, es una épica y se divide.

---

## DoD — la historia y el incremento están terminados

Dos niveles. Los dos se verifican con **evidencia enlazada**; ninguno se da por cumplido
«porque compila».

### Por cada historia

1. Implementación revisada por PR, sin secretos, con migración versionada si cambia
   persistencia.
2. Autorización por audiencia, scope/rol y ownership; se valida el recurso referenciado,
   no se confía en los IDs del body.
3. Validación de entrada y respuesta de error estándar (RFC 7807) con `X-Request-Id`
   propagado.
4. Idempotencia: la misma `Idempotency-Key` o `eventId` no duplica efectos; el reintento
   devuelve o recupera el resultado correspondiente.
5. Trazas de correlación en HTTP, jobs y eventos. Logs estructurados sin prompt,
   solución, token ni dato sensible innecesario.
6. Pruebas: unitarias de reglas; integración de persistencia/migración; contrato con
   dobles o consumer pactado; autorización y fallo relevante. Los **escenarios BDD de
   aceptación** (camino feliz + negativos) quedan ejecutados con evidencia. Cobertura
   según la convención del equipo.
7. Métrica, health/readiness razonable y runbook si introduce trabajo asíncrono,
   proveedor, dato retenido u operación manual.

### Por cada incremento de sprint

1. El usuario termina el recorrido comprometido con interfaz, persistencia y permisos
   reales.
2. Los servicios consumidores incluidos en el compromiso participan realmente.
3. Pruebas de contrato, fallas, idempotencia, seguridad y regresión aplicables pasan.
4. Las comprobaciones de calidad del modelo/RAG/moderación aplicables tienen evidencia.
5. Código revisado, migraciones y contratos coinciden con la versión desplegada.
6. Existe versión identificable, evidencia de demo/aceptación y procedimiento de
   recuperación.
7. Documentación y estado del backlog reflejan lo efectivamente entregado; la página del
   componente en la Wiki está creada o actualizada (HU enlazadas por su permalink;
   diagramas al día).
8. La demo del sprint pasa en un **ambiente integrado**, no en una máquina local
   aislada.

> Una funcionalidad parcial no se presenta como terminada. Si excede capacidad se reduce
> a un caso de uso menor completo, con acuerdo de producto, o se ajusta el calendario.
> Nunca se rebajan seguridad, pruebas o controles académicos para cumplir una fecha.
