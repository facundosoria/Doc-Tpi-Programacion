# Wireframes y Contrato de UI — Retomar Conversación con el Tutor (LLM-EP05-H03)

Este documento define la referencia visual y los estados mínimos de interfaz de usuario para la pantalla de conversación del tutor socrático, conforme a la tarea **T8** de la historia `LLM-EP05-H03`.

---

## Estado 1: Cargando Historial (Loading State)
Muestra un indicador visual mientras se consulta `GET /api/llm/tutor/conversations/{id}/messages`.

```text
+-----------------------------------------------------------------------+
|  <- Volver al Desafío     Desafío: Algoritmo Dijkstra                 |
|  Sesión: 21/09/2026       Estado: Activo                              |
+-----------------------------------------------------------------------+
|                                                                       |
|                          [ ... Cargando historial ... ]               |
|                                                                       |
|                                                                       |
+-----------------------------------------------------------------------+
|  [ Escribí tu consulta al tutor...                             ] [->] |
+-----------------------------------------------------------------------+
```

---

## Estado 2: Sin Mensajes Previos (Empty State)
Cuando la conversación recién se crea o no tiene mensajes registrados.

```text
+-----------------------------------------------------------------------+
|  <- Volver al Desafío     Desafío: Algoritmo Dijkstra                 |
|  Sesión: 21/09/2026       Mensajes: 0                                 |
+-----------------------------------------------------------------------+
|                                                                       |
|                     ( 💡 Tutor Socrático Activo )                     |
|           Todavía no realizaste preguntas en esta sesión.             |
|           ¿Tenés alguna duda sobre el planteo del ejercicio?          |
|                                                                       |
+-----------------------------------------------------------------------+
|  [ Escribí tu primera consulta aquí...                         ] [->] |
+-----------------------------------------------------------------------+
```

---

## Estado 3: Conversación Activa con Turnos (Camino Feliz - CA1, CA2, CA3)
Muestra los turnos cronológicos del alumno y las respuestas orientadoras del tutor con sus timestamps.

```text
+-----------------------------------------------------------------------+
|  <- Volver al Desafío     Desafío: Algoritmo Dijkstra                 |
|  Sesión: 21/09/2026       Mensajes: 4                                 |
+-----------------------------------------------------------------------+
| [Alumno - 10:14]                                                      |
| ¿Cómo elijo el siguiente nodo en Dijkstra?                            |
|                                                                       |
|                       [Tutor Socrático - 10:15]                       |
|                       Recordá cómo se actualizan las distancias       |
|                       mínimas acumuladas. ¿Qué estructura te ayuda    |
|                       a extraer el mínimo de forma eficiente?        |
|                                                                       |
| [Alumno - 10:18]                                                      |
| Pensaba usar una cola de prioridad.                                   |
|                                                                       |
|                       [Tutor Socrático - 10:19]                       |
|                       ¡Exacto! ¿Qué elemento necesitás encolar?       |
+-----------------------------------------------------------------------+
|  [ Escribí tu siguiente pregunta...                            ] [Enviar]|
+-----------------------------------------------------------------------+
```

---

## Estado 4A: Prohibido / Acceso Ajeno (Error 403 Forbidden - CA5, T6)
Cuando el `learnerId` del alumno autenticado no coincide con el dueño de la conversación referenciada.
Cuerpo de respuesta recibido: `HTTP 403 ProblemDetail` (`detail: "Conversación no encontrada: {id}"`, `error: "forbidden"`).
No se revelan datos ni existencia de mensajes ajenos.

```text
+-----------------------------------------------------------------------+
|  <- Volver a Mis Desafíos                                             |
+-----------------------------------------------------------------------+
|                                                                       |
|                       ⛔ Acceso No Autorizado                          |
|                                                                       |
|     No tenés permiso para ver o participar en esta conversación.      |
|     Verificá que estés ingresando con tu usuario correspondiente.     |
|                                                                       |
|                  [ Volver a mis conversaciones ]                      |
|                                                                       |
+-----------------------------------------------------------------------+
|  [ Entrada de texto deshabilitada                                  ]  |
+-----------------------------------------------------------------------+
```

---

## Estado 4B: No Disponible / Desafío Cerrado o Inexistente (Error 404 Not Found - CA6, T5)
Cuando el desafío asociado fue cerrado por tiempo o no existe en el sistema.
Cuerpo de respuesta recibido: `HTTP 404 ProblemDetail` (`detail: "Desafío no encontrado o no disponible: {id}"`, `error: "challenge_not_found"`).

```text
+-----------------------------------------------------------------------+
|  <- Volver a Desafíos                                                 |
+-----------------------------------------------------------------------+
|                                                                       |
|                       🔒 Desafío No Disponible                        |
|                                                                       |
|    El desafío asociado a esta conversación no se encuentra disponible |
|    o el período de entrega ha finalizado. No es posible enviar        |
|    nuevos mensajes al tutor en este ejercicio.                        |
|                                                                       |
|                  [ Ver solución / retroalimentación ]                 |
|                                                                       |
+-----------------------------------------------------------------------+
|  [ Conversación cerrada — no admite más mensajes                   ]  |
+-----------------------------------------------------------------------+
```
