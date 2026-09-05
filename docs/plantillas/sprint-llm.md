# Plantilla — Registro de sprint LLM

Usar con el [plan vigente](../23-plan-construccion-producto-llm.md). Copiar para cada sprint y
completar durante Planning, ejecución y cierre. Los valores de referencia no acreditan asistencia,
horas reales ni funcionalidades terminadas. No marcar una casilla sin su evidencia.

## 1. Identificación y objetivo

| Campo | Valor a completar |
|---|---|
| Sprint / fase | S__ / F__ |
| Inicio y cierre del ciclo | |
| Fecha de Planning previa a ejecución | |
| Objetivo y usuario beneficiado | |
| Recorrido funcional que se demostrará | |
| Límites del incremento | |
| Versión anterior que debe seguir funcionando | |
| Referente de producto / facilitador | |
| Ambiente de integración | |

## 2. Capacidad individual

Las horas disponibles incluyen reuniones. No multiplicar por diez la disponibilidad efectiva
de cinco personas. La suma nominal inicial del equipo es 350 h por ciclo, no 700 h.

| Integrante | Pareja / suplente | Disponibilidad declarada (h) | Reuniones (h) | Soporte conocido (h) | Base restante (h) | Reserva 20% (h) | Entregables (h) |
|---|---|---:|---:|---:|---:|---:|---:|
| 1 | | | | | | | |
| 2 | | | | | | | |
| 3 | | | | | | | |
| 4 | | | | | | | |
| 5 | | | | | | | |
| 6 | | | | | | | |
| 7 | | | | | | | |
| 8 | | | | | | | |
| 9 | | | | | | | |
| 10 | | | | | | | |
| **Total real planificado** | | | | | | | |

```text
Base = disponibilidad − reuniones − soporte conocido
Entregables = máximo(0, Base × 0,80)
Referencia sin soporte conocido: (350 − 90) × 0,80 = 208 h
```

Revisar bases individuales negativas; no trasladarlas silenciosamente a otra persona.

## 3. Reuniones del ciclo

La Planning se carga a este sprint aunque ocurra el día anterior al comienzo operativo.
La demo pertenece a la Review. Los cinco asistentes a coordinación son representantes internos.

| Reunión | Cantidad | Minutos por sesión | Asistentes internos | Referencia horas-persona | Fecha(s) | Horas-persona reales |
|---|---:|---:|---:|---:|---|---:|
| Planning | 1 | 120 | 10 | 20 | | |
| Daily / sincronización | 4 | 45 | 10 | 30 | | |
| Review con demo | 1 | 90 | 10 | 15 | | |
| Retrospectiva | 1 | 60 | 10 | 10 | | |
| Refinamiento | 2 | 30 | 10 | 10 | | |
| Dependencias | 2 | 30 | 5 | 5 | | |
| **Total programado** | | | | **90** | | |

Reuniones extraordinarias, preparación, actas y coordinación asincrónica se registran en la
reserva. Si una reunión planificada se extiende, la diferencia consume reserva; no se vuelve a
cargar su duración completa en ambos lugares.

## 4. Historias comprometidas y dependencias

| ID estable | Usuario y resultado | RF / contrato | Aceptación | Responsable / suplente | Dependencias y evidencia | Estimación restante (h) | Sprint objetivo | Estado |
|---|---|---|---|---|---|---:|---|---|
| LLM-S__-H__ | | | | | | | | Pendiente |

IDs históricos no se reutilizan. Una historia reprogramada conserva su ID y cambia de sprint
objetivo. La estimación incluye backend, interfaz, pruebas, review de código, integración,
documentación y despliegue. Las tareas se dividen para permitir relevo.

| Dependencia | Responsable interno / contraparte | Necesaria para | Fecha requerida | Contrato / evidencia | Estado / acción si falta |
|---|---|---|---|---|---|
| D__ | | | | | |

- [ ] El trabajo cabe en la capacidad individual y total.
- [ ] Existe un caso de uso completo, no solo tareas de infraestructura.
- [ ] Las integraciones comprometidas tienen disponibilidad comprobada o acuerdo explícito.
- [ ] Un mock no se confunde con evidencia de integración finalizada.
- [ ] Las mejoras acordadas en la retro anterior consumen capacidad asignada.

## 5. Seguimiento y reserva

| Fecha | Bloqueo / decisión / actividad no prevista | Responsable | Horas-persona | Impacto en objetivo | Próxima acción y fecha |
|---|---|---|---:|---|---|
| | | | | | |

| Control | Planificado | Real al cierre |
|---|---:|---:|
| Disponibilidad | | |
| Reuniones programadas | | |
| Soporte conocido | | |
| Reserva | | |
| Entregables | | |

Si se agota la reserva, registrar la renegociación del alcance. No cubrir diferencias con
horas extra supuestas, ni reducir pruebas o controles académicos.

## 6. Review y demo

| Evidencia | Registro |
|---|---|
| Versión desplegada / PR / ambiente | |
| Datos de prueba y usuario autorizado | |
| Pasos reproducibles del recorrido | |
| Resultado esperado y observado | |
| Pruebas de contrato, integración y regresión | |
| Fallas, permisos e idempotencia probados | |
| Evidencia de calidad del modelo, si aplica | |
| Feedback y nuevas historias | |
| Historias aceptadas / no terminadas | |

- [ ] Interfaz y persistencia reales; recorrido completo.
- [ ] Consumidores incluidos en el compromiso integrados realmente.
- [ ] Contratos y migraciones coinciden con la implementación.
- [ ] Pruebas aplicables pasan; incrementos anteriores funcionan.
- [ ] Recuperación y documentación disponibles.
- [ ] No se presenta como terminado trabajo parcial o sin evidencia.

## 7. Retrospectiva y siguiente Planning

| Pregunta | Registro |
|---|---|
| ¿Qué ayudó a terminar el objetivo? | |
| ¿Qué consumió más tiempo del previsto? | |
| ¿Qué dependencias bloquearon y durante cuánto tiempo? | |
| ¿Cuánto trabajo quedó sin terminar y qué falta exactamente? | |
| Mejora concreta / responsable / fecha | |
| Horas necesarias para aplicar la mejora | |
| Ajuste de capacidad y previsión del producto | |

El trabajo pendiente se reestima por lo que falta y consume capacidad del próximo sprint.
La siguiente Planning se contabiliza una sola vez, en el sprint que prepara.
