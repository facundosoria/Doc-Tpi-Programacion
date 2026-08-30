# Tema 07 — Evaluación LLM

Documentación de diseño de la capa de inteligencia artificial de la **Plataforma de Aprendizaje
Gamificado**.

**UTN FRC · Tecnicatura Universitaria en Programación · Programación IV — Back End · 2.º año, 4.º cuatrimestre**

---

## El problema

**Construimos el servicio que le pone nota a *cómo* un alumno usó la IA, y tenemos que demostrar que
esa nota es confiable.**

Lo que lo vuelve difícil no es integrar un modelo de lenguaje:

1. **Es una IA evaluando a otra IA**, y el resultado modifica el XP — que define si un alumno
   promociona. Una nota mal puesta no es un bug: es un resultado académico que no se deshace.
2. **Hay que demostrar que evalúa como un humano.** Y para eso primero hay que lograr que **dos
   humanos se pongan de acuerdo entre ellos**, que es más difícil que el problema técnico.
3. **La restricción es asimétrica:** un único modelo activo, sin fallback (RF-IA-25) — pero su caída
   **no puede bloquear al alumno** (RF-IA-27). Un solo camino, y prohibido cortarlo.
4. **Hay que evitar que la IA filtre la solución, sin que la IA vea la solución.**
5. **El texto del alumno es a la vez el dato evaluado y un vector de ataque.**
6. **Dependemos de cinco equipos y de docentes que no controlamos.**

## 👉 Si te sumás al proyecto

Empezá por **[15 · Estado actual y cómo continuar](docs/15-estado-y-como-continuar.md)**: dice dónde
está todo, qué está decidido, qué bloquea y qué hacer a continuación, sin tener que leer los otros 14
documentos primero.

## Documentación

| # | Documento | Qué responde |
|---|---|---|
| 01 | [Problema, alcance y equipo](docs/01-problema-y-alcance.md) | Qué es nuestro, qué no, y qué reclamarle a los otros equipos |
| 02 | [Arquitectura y stack](docs/02-arquitectura-y-stack.md) | Reglas de la cátedra, diagrama de sistema, los 8 módulos, el AI Gateway, y el fundamento completo de Java vs Python |
| 03 | [Modelos, costos y contexto](docs/03-modelos-costos-y-contexto.md) | Qué modelo para cada función, costo por consulta, cuánto contexto meter, qué puede ser gratis |
| 04 | [Las funciones de IA](docs/04-funciones-de-ia.md) | El generador de evaluaciones, los dos jueces del sistema, y el golden set |
| 05 | [Seguridad](docs/05-seguridad.md) | Prompt injection, fuga de solución, dónde corre cada guardarraíl, de dónde sale cada nota |
| 06 | [Operación e ingeniería](docs/06-operacion-e-ingenieria.md) | Colas y prioridades, pico, degradación, caché — y cómo se prueba algo no determinístico |
| 07 | [Datos y T&C](docs/07-datos-y-terminos.md) | Qué se guarda, quién lo ve, cuánto dura, y el borrador de Términos y Condiciones |
| 08 | [Decisiones y pendientes](docs/08-decisiones-y-pendientes.md) | Registro de decisiones (ADR) y **lo que falta definir** |
| 09 | [Preguntas y respuestas](docs/09-preguntas-y-respuestas.md) | El porqué de cada decisión, **con el caso a favor y el caso en contra** |
| 10 | [Qué entregamos y cómo](docs/10-entregables-y-plan.md) | El inventario del aporte del equipo y el plan de 12 pasos para 6 personas |
| 11 | [Glosario y metadata](docs/11-glosario-y-metadata.md) | El vocabulario para la integración y las tres tablas que hay que crear ya |
| 12 | [Almacenamiento e ingesta](docs/12-almacenamiento-e-ingesta.md) | Qué base de datos y cuántas, MinIO, y cómo se baja a texto un PDF con imágenes |
| 13 | [La rúbrica y los prompts](docs/13-rubrica-y-prompts.md) | 📝 El artefacto central del equipo: las 5 dimensiones con sus anclas y los prompts de cada función |
| 14 | [Sincronización con la guía didáctica](docs/14-sincronizacion-guia-didactica.md) | 🔄 **Comparación con el otro set de documentación**: los 6 conflictos, lo que hay que adoptar y lo que aportamos |
| 15 | [Estado actual y cómo continuar](docs/15-estado-y-como-continuar.md) | 👉 **Empezá por acá.** Dónde está todo, qué bloquea, qué hacer a continuación y las 7 decisiones que se revisaron |

> ### 📝 Sobre el contenido de ejemplo
>
> Varios documentos traen anclas, prompts, transcripciones y números **de ejemplo**, para que el
> mecanismo se entienda y para no arrancar de cero. **Nada de eso es definitivo.**
> El inventario completo —los 29 ítems, qué marca tiene cada uno, quién lo define y cuándo— está en
> [08 · Decisiones y pendientes](docs/08-decisiones-y-pendientes.md), **Parte C**.

### Por dónde entrar

- **Para entender antes que implementar** → [09 · Preguntas y respuestas](docs/09-preguntas-y-respuestas.md).
  Es el razonamiento en lenguaje llano y el mejor material para la defensa.
- **Para empezar a trabajar** → [11 · Glosario y metadata](docs/11-glosario-y-metadata.md) y
  [10 · Plan](docs/10-entregables-y-plan.md).
- **Para la sesión de integración** → [08 · Decisiones y pendientes](docs/08-decisiones-y-pendientes.md), parte B.
- **Para decidir modelos** → [03 · Modelos y costos](docs/03-modelos-costos-y-contexto.md).

## Los hallazgos que ordenan todo

1. **Nuestro alcance oficial es más angosto de lo que asumíamos.** El Tema 07 son seis cosas:
   rúbrica, invocación del modelo, golden set, calibración, bloqueo de activación y salvaguarda
   anti-fuga. **El RAG, el tutor y el generador no están asignados a ningún equipo.**

2. **Pero la salvaguarda anti-fuga nos pone en el camino del tutor.** Corre sobre la respuesta del
   tutor justo antes de que el alumno la vea: **no se puede ser dueño de ese guardarraíl sin estar
   ahí.** Y sin tutor no hay transcripción, así que el evaluador se queda sin insumo.

3. **El costo es de USD 5 a 22 por cuatrimestre.** Y la palanca principal **no es qué modelo elegís,
   es cuántos tokens le mandás**: recortar el contexto del tutor de 6.000 a 3.000 ahorra más que
   cambiar de modelo, sin costar calidad.

4. **Buena parte de la rúbrica se puede calcular con código**, no con un modelo: entre el 45% y el
   60% del score. Y no por ahorro — por **reproducibilidad, inmunidad a injection, auditabilidad y
   ausencia de deriva**.

5. **RF-IA-20 mata el streaming token a token** en desafíos prácticos: no se puede bloquear una
   respuesta que el alumno ya está leyendo.

6. **El golden set es el ítem de plazo más largo y no depende de nadie técnico.** Sin calibración
   aprobada, **ningún curso pasa de borrador a activo** — sin override, ni de ADMIN.

7. **La herramienta de carga del golden set tiene que existir antes** de que ese trabajo docente
   pueda empezar. Parece "una pantalla de admin más" y en realidad destraba el camino crítico.

## Diagramas

- [Arquitectura interna](https://claude.ai/code/artifact/3289c8d8-1ecb-45b6-8ec8-f064b70089c5) —
  los 8 módulos, cómo se conectan los modelos, el reparto entre 6
- [Construcción del Tema 07](https://claude.ai/code/artifact/ddc1b820-b1aa-4dc2-ad20-aad24af54ca9) —
  los 12 pasos en 4 semanas, con dependencias
- [Java o Python](https://claude.ai/code/artifact/6bda78ed-698b-48dd-aa10-3268c107be13) —
  comparación capa por capa de los dos stacks
- [Pantalla del golden set](https://claude.ai/code/artifact/0854689d-1746-486e-b30f-a9cdced0a2d2) —
  mockup con las 7 decisiones a debatir

Los documentos incluyen además diagramas Mermaid, que GitHub renderiza directamente.

## Lo que bloquea hoy

| Qué | Quién decide |
|---|---|
| 🔴 Golden set: responsable con nombre y fecha | Product Owner |
| 🔴 Free tier y datos de alumnos | Consulta legal |
| 🔴 Tema 05: cómo accedemos a la solución esperada | Sesión de integración |
| 🔴 Tema 11: nuestros campos en el contrato de eventos, **antes de que lo cierren** | Sesión de integración |
| 🟡 Alcance: ¿RAG, tutor, generador y corrector son nuestros? | Sesión de integración |

Detalle y recomendación de cada uno en [08 · Decisiones y pendientes](docs/08-decisiones-y-pendientes.md).

## Decisiones cerradas

| Decisión | Fundamento |
|---|---|
| **Java Spring Boot** para el servicio | La materia es de Java y la integración con Spring Cloud pesa más que el ecosistema de IA de Python. [Detalle](docs/02-arquitectura-y-stack.md) |
| **Un microservicio, no cinco** | Las cinco funciones comparten gateway, guardarraíles, cuotas y log. Separarlas multiplica la maquinaria transversal |
| **Sin orquestador basado en LLM** | La ruta la sabe la UI. Un router agrega latencia, costo, un punto de falla y una superficie de injection |
| **Sincrónico solo para tutor y moderador** | El resto va por cola: Batch al 50%, RF-IA-27 implementado por construcción, y el pico absorbido |
| **La solución de referencia nunca entra al contexto del tutor** | No se puede filtrar lo que no se tiene |
| **El perímetro temático lo hace cumplir el retrieval, no el prompt** | Una instrucción se sortea hablando; un filtro en el servidor no |

## Fuentes

- `PRD-Plataforma-Gamificada-TP.pdf` (v2.1) — definición funcional del producto
- `TUP_PIV_BE_PROPUESTA_ARQ.pdf` — propuesta de arquitectura de la cátedra

> Los PDF de origen no se versionan en este repositorio. Se distribuyen por los canales de la cátedra.

---

*Documentación viva. Última actualización: 2026-08-30.*
