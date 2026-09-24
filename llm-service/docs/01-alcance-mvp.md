# 01 — Alcance del MVP

## Propósito

Este documento separa qué debe entregar `llm-service` en el MVP de lo que corresponde a fases
posteriores. Los contratos detallados se encuentran únicamente en `contracts/`.

## Dentro del MVP

- Tutor de IA invocado por `practice-service`, con respuesta pedagógica y protección contra fuga
  de la solución esperada.
- Gestión docente de rúbricas versionadas, Golden Set y corridas de calibración por
  `courseCohortId`.
- Rúbricas editables mientras son borrador; al publicarse quedan inmutables. Conservan las cinco
  dimensiones obligatorias y sus pesos suman 100 %.
- Configuración de credenciales y modelos de proveedor desde Back Office a través de
  `admin-service`, sin exponer secretos al navegador.
- Evaluación asíncrona de un intento cerrado y publicación a Practice de score calculado o estado
  diferido.
- Consulta de calibración activa y pendientes para que Courses aplique sus reglas de activación o
  cierre.

## Fuera del MVP

- Chat general de alumnos, menciones `@`, moderación conversacional y RAG: son capacidades de
  fases posteriores, no endpoints ni eventos del MVP.
- Calificación académica, XP, publicación de desafíos y persistencia de prácticas: pertenecen a
  sus microservicios dueños.
- Acceso directo del frontend a `llm-service` o a proveedores externos.

## Resultado verificable

El MVP está completo documentalmente cuando un equipo externo puede, sin consultar este texto,
usar el OpenAPI para invocar Tema 07, consumir el AsyncAPI, y aportar los datos requeridos en
[requisitos-a-otros-micros.md](contracts/requisitos-a-otros-micros.md). El estado real de código
se verifica en [06-trazabilidad-y-estado.md](06-trazabilidad-y-estado.md).
