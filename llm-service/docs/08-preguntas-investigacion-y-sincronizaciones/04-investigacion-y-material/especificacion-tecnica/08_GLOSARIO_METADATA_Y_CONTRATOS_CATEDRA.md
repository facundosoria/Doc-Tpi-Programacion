# 08 — Glosario, Contratos de Integración y Plan de Ejecución

> **Cátedra:** Programación IV — Back End · UTN FRC  
> **Tema 07:** Capa de Inteligencia Artificial y Evaluación LLM  
> **Propósito:** Definir el glosario terminológico unificado, resolver las 7 colisiones conceptuales entre equipos, especificar los contratos de eventos AMQP (Tema 11 - RabbitMQ) y presentar el plan de trabajo de 12 pasos para el equipo.

---

## 1. Resolución de las 7 Colisiones Conceptuales

Para evitar malentendidos durante la integración con los otros 11 equipos de la cátedra:

| Término | ❌ Confusión Habitual | ✅ Significado Canónico en el Proyecto |
|---|---|---|
| **1. "Evaluación"** | Se confunde entre el examen del alumno, la nota de la IA y el test de modelos. | **a) Desafío/Parcial:** Instancia evaluativa del alumno.<br/>**b) Score IA:** Calificación en 5 dimensiones emitida por el Rol 3.<br/>**c) Calibración:** Validación del modelo contra el Golden Set (PAR-14). |
| **2. "XP / Score"** | Creer que la IA otorga la experiencia del alumno. | La IA emite **`score_final` (0 a 100)**; el Motor de Desafíos (Tema 03) calcula y otorga el **XP base $\pm 20\%$**. |
| **3. "Prompt"** | Confundir el mensaje del alumno con las instrucciones del sistema. | **a) Prompt de Sistema:** Instrucción fija del docente.<br/>**b) Query / Consulta:** Pregunta escrita por el estudiante. |
| **4. "Streaming"** | Asumir que la IA escribe directo en el socket del cliente. | El streaming pasa obligatoriamente por el **Buffer Interceptor AST en RAM** antes de llegar al navegador. |
| **5. "Sandbox"** | Creer que la IA corre el código del alumno. | La IA **nunca ejecuta código**; solo lee los errores de `stderr` capturados por el contenedor Docker aislado (gVisor). |
| **6. "Override"** | Creer que el docente sobreescribe la fila en la BD. | La nota original en `scores_ia` es inmutable; el cambio se agrega como nueva fila en `score_overrides`. |
| **7. "Cola vs. Bus"** | Confundir RabbitMQ con Celery/Redis. | **a) Celery/Redis:** Cola interna privada para procesamiento pesado.<br/>**b) RabbitMQ:** Bus de eventos compartido de la cátedra (Tema 11). |

---

## 2. Contratos de Eventos AMQP (Tema 11 — RabbitMQ)

### 2.1. Evento Consumido por la IA: `student.challenge.submitted`
Publicado por el Motor de Desafíos (Tema 03) cuando el alumno finaliza un ejercicio.

```json
{
  "event_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "event_type": "STUDENT_CHALLENGE_SUBMITTED",
  "timestamp": "2026-08-31T15:30:00Z",
  "payload": {
    "session_id": "c3b8a6e2-1234-4567-89ab-cdef01234567",
    "student_id": "a1b2c3d4-0000-0000-0000-000000000001",
    "challenge_id": "JAVA-OOP-042",
    "course_id": "PROG4-2026-C2",
    "transcription": [
      {
        "turn": 1,
        "role": "user",
        "query": "¿Por qué me da NullPointerException en la línea 12?",
        "code_snapshot": "public class Persona { private String nombre; ... }",
        "sandbox_error": "java.lang.NullPointerException at Persona.getNombre(Persona.java:12)"
      },
      {
        "turn": 1,
        "role": "assistant",
        "response": "¿Has verificado si el objeto fue instanciado antes de llamar al método?"
      }
    ],
    "final_submitted_code": "public class Persona { ... }",
    "tests_passed": true,
    "total_attempts": 3
  }
}
```

---

### 2.2. Evento Publicado por la IA: `ai.evaluation.completed`
Publicado por el microservicio de IA al bus RabbitMQ cuando el Worker finaliza el scoring forense.

```json
{
  "event_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "event_type": "AI_EVALUATION_COMPLETED",
  "timestamp": "2026-08-31T15:30:04Z",
  "payload": {
    "evaluation_id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "session_id": "c3b8a6e2-1234-4567-89ab-cdef01234567",
    "student_id": "a1b2c3d4-0000-0000-0000-000000000001",
    "challenge_id": "JAVA-OOP-042",
    "score_agregado": 87.50,
    "dimensiones_5": {
      "score_autonomia": 90.0,
      "justificacion_autonomia": "El estudiante probó hipótesis previas en el IDE antes de pedir asistencia.",
      "score_claridad": 85.0,
      "justificacion_claridad": "Presentó el stacktrace exacto y la línea afectada con vocabulario técnico adecuado.",
      "score_progresion": 88.0,
      "justificacion_progresion": "Modificó el constructor tras la pista socrática resolviendo la inicialización.",
      "score_cumplimiento": 100.0,
      "justificacion_cumplimiento": "Cero intentos de evasión de reglas o solicitud de solución directa.",
      "score_eficiencia": 75.0,
      "justificacion_eficiencia": "Resolvió la falla en 2 turnos concisos."
    },
    "confidence_score": 0.94,
    "requiere_auditoria_humana": false,
    "prompt_version_id": "e4b2d1c0-5555-4444-3333-222211110000",
    "evaluated_at": "2026-08-31T15:30:04Z"
  }
}
```

---

## 3. Plan de Construcción en 12 Pasos (4 Semanas)

```mermaid
gantt
    title Plan de Construcción del Microservicio de IA
    dateFormat  YYYY-MM-DD
    section Semana 1: Núcleo e Infraestructura
    Paso 1: DDL PostgreSQL + Triggers PL/pgSQL         :done, p1, 2026-09-01, 2d
    Paso 2: Factory Multi-Proveedor & Pydantic DTOs    :done, p2, after p1, 2d
    Paso 3: Spring Boot Sidecar (Eureka + WebClient)   :active, p3, after p2, 3d
    section Semana 2: Seguridad y Tutor
    Paso 4: Harmlessness Screen & PII Sanitizer        :p4, 2026-09-08, 2d
    Paso 5: Buffer Interceptor AST en Streaming SSE    :p5, after p4, 3d
    Paso 6: Memoria Anti-Puzzle en Redis               :p6, after p5, 2d
    section Semana 3: Evaluador y LLMOps
    Paso 7: Scoring Híbrido Determinístico             :p7, 2026-09-15, 2d
    Paso 8: Workers Celery & Listener AMQP             :p8, after p7, 3d
    Paso 9: Runner Golden Set & Circuit Breaker (PAR-14):p9, after p8, 2d
    section Semana 4: RAG, FinOps e Integración
    Paso 10: RAG Curricular con pgvector               :p10, 2026-09-22, 2d
    Paso 11: FinOps Guard & Contadores de Cuota        :p11, after p10, 2d
    Paso 12: Pruebas de Carga E2E (120 Concurrentes)   :p12, after p11, 3d
```
