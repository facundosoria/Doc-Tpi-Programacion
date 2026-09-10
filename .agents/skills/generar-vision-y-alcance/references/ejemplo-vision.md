# Ejemplo resuelto — Visión y alcance (extracto)

> Ilustración. El skill genera la misma **estructura** para cualquier proyecto.

## Contexto de entrada que aportó el equipo (resumido)

- **Producto:** microservicio `llm-service` de una plataforma de aprendizaje de
  programación gamificado. Evalúa entregas de alumnos con un modelo LLM, con tutor
  socrático y moderación.
- **Fases:** MVP (tutor + evaluación + golden set + calibración) · Fase 2 (moderación del
  chat) · Fase 3 (RAG + agente).
- **De qué se hace cargo el equipo:** solo el LLM y la parte de IA. No: usuarios, roles,
  cursos, motor de desafíos, economía de gamificación, pantallas.

## Salida — extracto

# Visión y alcance — llm-service

> **Documento fuente.** Decide alcance funcional, fases y funciones. Las épicas y las
> historias se le subordinan.

## 1. El problema

Los docentes corrigen a mano el uso que los alumnos hacen de la IA en sus entregas: es
lento, poco consistente entre cursos y no escala. `llm-service` da una evaluación
automática con desglose y apelación, un tutor socrático que no filtra la solución, y la
referencia humana (golden set) contra la que se calibra.

> **Hallazgo crítico:** 5 de los 16 criterios de release del MVP dependen de la IA y
> **ninguno lo completa el equipo de IA solo** — faltan pantallas y flujos que son de
> otros equipos. El servicio puede estar perfecto y el MVP no salir.

## 2. Alcance por fase

### MVP

| Entra | Requisito | Nota |
|---|---|---|
| Tutor en desafío (socrático, anti-fuga) | RF-IA-01/04/19/20 | Sincrónico |
| Evaluador de uso de IA (score + desglose) | RF-IA-12 a 18 | Async; **no** asigna XP |
| Golden set y calibración | RF-IA-29 a 36 | Referencia humana |
| Apelación y score diferido | RF-IA-18/27 | |

**Explícitamente afuera del MVP:** chat interno y moderador (Fase 2); RAG, ingesta,
desafíos personalizados, agente `@mención` (Fase 3); todas las pantallas (otro equipo).

## 3. Funciones / capacidades

| # | Función | Qué hace | Modo | Requisitos |
|---|---|---|---|---|
| 1 | Tutor | asiste al alumno dentro del desafío sin darle la solución | sync | RF-IA-01/04 |
| 2 | Evaluador | puntúa un intento cerrado en 5 dimensiones (0–100) + confianza + justificación | async | RF-IA-12 a 18 |
| 3 | Golden set | el docente crea, carga y versiona la referencia de su curso | sync | RF-IA-29/30 |
| 4 | Calibración | corre el golden set contra el modelo y aprueba/rechaza por tolerancia | async | RF-IA-32 a 36 |

## 4. Fronteras

| Construimos y decidimos | Negociamos | Dependemos de otros |
|---|---|---|
| `llm-service` completo, AI Gateway, las funciones de IA, rúbricas y prompts versionados, runner de calibración, workers | contrato de la API interna · dueño de la cola · quién persiste el score · quién construye las pantallas de IA | backend de negocio, gamificación/XP, auth, ciclo de vida del curso, base de datos académica, todas las pantallas |

## 5. Objetivos medibles

- El evaluador dentro de tolerancia (kappa ≥ umbral acordado) contra el golden set antes
  de activar un curso.
- Ningún curso se activa sin calibración aprobada (bloqueo duro, sin override).
- Degradación: si el proveedor cae, la entrega se acepta y el score se difiere, no se
  pierde.
- *(umbrales numéricos exactos: a definir con el PO)*

## 7. Riesgos del recorte de alcance

| Riesgo | Por qué pasa | Mitigación | Dueño |
|---|---|---|---|
| La IA queda lista y no se puede usar | faltan las 7 pantallas | reclamarlas ahora con IDs de requisito y puntos del DoD | 🔴 PO + Front |
| El golden set no existe el día del go-live | nadie lo agendó, no es trabajo de dev | escalar al PO esta semana | 🔴 PO |
| El backend no implementa la degradación | asume que «la resiliencia es de la IA» | explicitarlo en el contrato: aceptar la entrega con el evaluador caído es del backend | 🔴 Backend |

---

## Por qué queda así

- **El problema arriba, y el hallazgo crítico primero**: si una dependencia puede hundir
  el release, se dice antes que el detalle de funciones.
- **Alcance «afuera» explícito por fase**: sin eso, alguien asume el chat en el MVP.
- **Funciones = qué hace para el usuario** (tutor «asiste sin dar la solución»), no cómo
  (nada de «prompt template», «vector store»).
- **Fronteras en 3 columnas**: la zona de negociación es la que más se malinterpreta;
  nombrarla evita que dos equipos construyan lo mismo o ninguno.
- **KPIs que el equipo no fijó → `*(a definir con el PO)*`**, no un número inventado.
- Este documento **no** baja a stack ni a endpoints: eso es `docs/00`, `docs/contracts/`
  y los ADR.
