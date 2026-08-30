# 15 — Estado actual y cómo continuar

> **Empezá por acá si te sumás al proyecto o retomás después de un tiempo.**
>
> Foto del **2026-08-30**. Dice dónde está todo, qué está decidido, qué no, y qué hacer a
> continuación — sin tener que leer los otros 14 documentos primero.

## 1. Dónde está el proyecto

**Etapa: diseño terminado, construcción sin empezar.**

| | Estado |
|---|---|
| Documentación de diseño | ✅ Completa — 15 documentos |
| Decisiones de arquitectura | ✅ Tomadas, salvo una (§3) |
| Código | ❌ **Cero líneas** |
| Golden set | ❌ Sin responsable ni fecha |
| Sesión de integración con los otros equipos | ❌ Pendiente |

**No se escribió código a propósito.** Faltan definiciones que cambiarían lo que se construya, y
están listadas en §4.

## 2. Qué construimos, en una frase

**El servicio que le pone nota a cómo un alumno usó la IA, más el mecanismo que demuestra que esa
nota es confiable.**

Somos el **Tema 07 — Evaluación LLM** de un TPI repartido entre 12 equipos.

## 3. Lo que está decidido

| Decisión | Por qué | Dónde |
|---|---|---|
| **Java Spring Boot** para el servicio | La materia es de Java y la integración con Spring Cloud pesa más que el ecosistema de IA de Python | [02](02-arquitectura-y-stack.md) Parte 2 |
| **Un microservicio**, con las 5 funciones adentro | Comparten gateway, guardarraíles, cuotas y log. Separarlas multiplica la maquinaria transversal | [02](02-arquitectura-y-stack.md) |
| **Sin orquestador basado en LLM** | La ruta la sabe la UI. Un router agrega latencia, costo, falla e injection | [09](09-preguntas-y-respuestas.md) Q-01 |
| **Sincrónico solo para tutor y moderador** | El resto por cola: Batch al 50%, RF-IA-27 gratis, pico absorbido | [06](06-operacion-e-ingenieria.md) |
| **PostgreSQL + pgvector, base propia** | Sin base vectorial dedicada | [12](12-almacenamiento-e-ingesta.md) |
| **La solución de referencia nunca entra al contexto del tutor** | No se filtra lo que no se tiene | [05](05-seguridad.md) |
| **El perímetro temático lo hace cumplir el retrieval**, no el prompt | Una instrucción se sortea hablando; un filtro en el servidor no | [05](05-seguridad.md) |
| **Damos el score, no aplicamos el XP** | La economía tiene un solo dueño y no somos nosotros | [01](01-problema-y-alcance.md) |

### La única grande sin cerrar

> 🔴 **Nuestro alcance.** El reparto oficial de la cátedra pone en el Tema 07 seis cosas: rúbrica,
> invocación del modelo, golden set, calibración, bloqueo de activación y salvaguarda anti-fuga.
> **El RAG, el tutor, el generador y el corrector no están asignados a ningún equipo.**
>
> **El argumento para reclamarlos:** la cátedra nos dio la salvaguarda anti-fuga, que corre sobre la
> respuesta del tutor justo antes de que el alumno la vea. **No se puede ser dueño de ese guardarraíl
> sin estar en el camino del tutor.** Y sin tutor no hay transcripción, así que el evaluador se queda
> sin insumo.

## 4. Lo que bloquea, y a quién preguntarle

| # | Qué | A quién | Si no se resuelve |
|---|---|---|---|
| 1 | 🔴 **Golden set: responsable y fecha** | Product Owner | **Ningún curso puede arrancar.** Sin override, ni de ADMIN |
| 2 | 🔴 **¿El free tier puede tocar datos de alumnos?** | Consulta legal | Cae el modelo de costos y cambian los T&C |
| 3 | 🔴 **Tema 05: cómo accedemos a la solución esperada** | Sesión de integración | La salvaguarda no tiene contra qué comparar |
| 4 | 🔴 **Tema 11: nuestros campos en el contrato de eventos** | Sesión de integración, **antes de que lo cierren** | Después es renegociar con cinco equipos |
| 5 | 🟡 **El alcance** (§3) | Sesión de integración | Hoy nadie hace el tutor ni el RAG |
| 6 | 🟡 **¿La cátedra permite Python?** | Cátedra | Ya asumimos que no, pero conviene confirmarlo |

**Cinco de los seis son conversaciones, no código.**

## 5. Qué hacer a continuación, en orden

### Esta semana — nada depende de nadie

| # | Acción | Quién |
|---|---|---|
| 1 | **El glosario** — medio día, todo el equipo. Acordar qué significa "evaluación", "corrección", "calibración". **Llevarlo a la integración** | Todos |
| 2 | 🔴 **Definir el esquema de metadata y empezar a capturarla** | P5 |
| 3 | Mandar los cuatro pedidos de §4 | Quien corresponda |

> **El punto 2 es el único urgente de verdad.** Los tiempos entre mensajes y las ediciones de código
> **no se pueden reconstruir después**, y son la evidencia de autonomía — la dimensión que pesa 30%.
> Todo lo demás se puede posponer; esto no.

### Después — los 12 pasos

El plan completo está en [10](10-entregables-y-plan.md), Parte 2. El resumen:

```
0  Glosario                    ← todos, medio día
1  Esqueleto: docker + llamar_modelo()   ← P1 + P6, semana 1
2  Capturar metadata           ← P5, urgente
3  La rúbrica como artefacto   ← P3
4  Features determinísticos    ← P3 + P5
5  Evaluar una transcripción   ← P3
6  Golden set chico (10)       ← P4
7  Runner de calibración       ← P4
8  Endpoint de estado          ← P6, entregarlo temprano aunque sea mock
9  El RAG                      ← P2
10 El generador                ← P2
11 Guardarraíles y tutor       ← P5 + P6
12 El corrector                ← P3
```

**Los pasos 0 a 8 no dependen de nadie externo:** son ~5 semanas para 6 personas y cubren el núcleo
entero del Tema 07.

## 6. ⚠️ Decisiones que se revisaron durante el diseño

**Leé esto antes de reabrir una discusión.** Siete decisiones cambiaron mientras se armaba la
documentación, y los documentos ya reflejan la versión final — pero si encontrás una afirmación que
parece contradecir a otra, probablemente sea una de estas.

| # | Antes | Ahora | Por qué cambió |
|---|---|---|---|
| 1 | Monolito modular | **Microservicios** | La cátedra los declara no negociables |
| 2 | pgvector en base compartida | **Base propia y exclusiva** | *"Cada servicio es dueño exclusivo de su base"* |
| 3 | Python FastAPI | **Java Spring Boot** | La materia es de Java + fricción con Spring Cloud |
| 4 | Sin streaming en prácticos | **Buffer Interceptor** | La guía didáctica lo resuelve mejor ([14](14-sincronizacion-guia-didactica.md) C-2) |
| 5 | Redis obligatorio | **Probablemente innecesario** | A 120 usuarios, Postgres alcanza |
| 6 | El corrector no usa el RAG | **Sí lo usa**, por el chunk trazado | La pregunta nació de un fragmento; ese fragmento sirve al corregir |
| 7 | ~USD 125 por cuatrimestre | **USD 5 a 22** | Al optimizar el contexto del tutor |

> **La #4 vino de afuera:** existe otro set de documentación (la *guía didáctica*) que en ese punto
> tenía mejor solución que la nuestra. La comparación completa está en
> [14](14-sincronizacion-guia-didactica.md).

## 7. El otro set de documentación

Existe `Plan anti jailbreak/guia_didactica_ia/` — **7 capítulos de implementación con código Python,
DDL de PostgreSQL e hiperparámetros**.

**No compite con esta documentación: la complementa.**

| Set | Rol |
|---|---|
| **Este repositorio** | El **qué** y el **por qué**: alcance, decisiones, economía, riesgos, coordinación |
| **La guía didáctica** | El **cómo**: algoritmos, esquema de base, hiperparámetros, capas |

**Seis conflictos entre ambas** están identificados y resueltos en
[14](14-sincronizacion-guia-didactica.md). Los dos que más importan:

- **A favor de la guía:** el Buffer Interceptor para streaming, `temperature: 0` con `seed` fijo en el
  evaluador, y **triggers de PostgreSQL para forzar inmutabilidad de las notas** — mejor que nuestra
  regla por convención.
- **A favor nuestro:** la guía asume Python y asume que hacemos las 5 funciones, sin el análisis de
  alcance ni la infraestructura que la cátedra impone.

> **Y el dato que da confianza:** las dos llegaron por separado a **diez decisiones estructurales
> idénticas** — un solo microservicio, pgvector sin base dedicada, evaluador asincrónico por evento,
> AST al 70%, un único modelo evaluador. Que dos análisis independientes coincidan es la mejor
> validación disponible.

## 8. Cómo leer la documentación

**No la leas en orden.** Según para qué:

| Objetivo | Leé |
|---|---|
| **Entender el proyecto en 20 minutos** | El `README.md` y este documento |
| **Entender el porqué de las decisiones** | [09](09-preguntas-y-respuestas.md) — 22 preguntas con el caso a favor **y el caso en contra**. El mejor material para la defensa |
| **Empezar a programar** | [11](11-glosario-y-metadata.md) → [10](10-entregables-y-plan.md) Parte 2 → [02](02-arquitectura-y-stack.md) |
| **Prepararse para la integración** | [08](08-decisiones-y-pendientes.md) Parte B → [01](01-problema-y-alcance.md) |
| **Elegir modelos o discutir costos** | [03](03-modelos-costos-y-contexto.md) |
| **Escribir prompts o la rúbrica** | [13](13-rubrica-y-prompts.md) |
| **Diseñar la base de datos** | [12](12-almacenamiento-e-ingesta.md) |

## 9. ⚠️ Qué es borrador y qué no

**Varios documentos traen contenido de ejemplo**: anclas de la rúbrica, prompts, una transcripción
con puntajes, umbrales, supuestos de volumen.

**Nada de eso es definitivo.** El inventario completo —**29 ítems**, con su marca, dónde está, quién
lo define y cuándo— está en [08](08-decisiones-y-pendientes.md), **Parte C**.

> 🔴 **El que más cuidado requiere:** los puntajes de la transcripción de ejemplo del golden set
> ([04](04-funciones-de-ia.md), Parte 3) **los inventamos nosotros para ilustrar el formato**. Si un
> docente los toma como referencia, **la calibración deja de medir nada**. Aclarálo siempre que se
> los muestres.

## 10. Los gráficos

Están publicados aparte del repositorio:

| Gráfico | Qué muestra |
|---|---|
| [Arquitectura interna](https://claude.ai/code/artifact/3289c8d8-1ecb-45b6-8ec8-f064b70089c5) | Los 8 módulos, cómo se conectan los modelos, el reparto entre 6 |
| [Construcción del Tema 07](https://claude.ai/code/artifact/ddc1b820-b1aa-4dc2-ad20-aad24af54ca9) | Los 12 pasos en 4 semanas, con dependencias |
| [Java o Python](https://claude.ai/code/artifact/6bda78ed-698b-48dd-aa10-3268c107be13) | Comparación capa por capa de los dos stacks |
| [Pantalla del golden set](https://claude.ai/code/artifact/0854689d-1746-486e-b30f-a9cdced0a2d2) | Mockup con las 7 decisiones a debatir |

Los documentos además tienen diagramas Mermaid, que GitHub renderiza directo.

**Falta uno:** el diagrama de sistema bajo las reglas de la cátedra — dónde encaja nuestro servicio
entre el API Gateway, el Service Discovery, el bus y los otros temas. **Depende de que se resuelva el
alcance** (§3).

## 11. Las cinco cosas que no hay que perder de vista

1. **La metadata de tiempos y ediciones se pierde para siempre si no se captura desde el primer día.**
2. **El golden set es el ítem de plazo más largo del proyecto y no depende de ningún equipo técnico.**
   ~26 h de dos docentes, o ~4 h en versión reducida.
3. **La herramienta de carga del golden set tiene que existir *antes* de que ese trabajo empiece.**
   Parece "una pantalla de admin más" y destraba el camino crítico.
4. **El evaluador no tiene plan B de modelo** (RF-IA-25: un único modelo activo, sin pool ni
   enrutamiento). Su único plan B es la cola diferida.
5. **Cinco de los 16 criterios de release dependen de nosotros, y ninguno lo podemos completar solos.**
   Faltan siete pantallas que construye otro equipo.

## 12. Fuentes

- `PRD-Plataforma-Gamificada-TP.pdf` (v2.1) — definición funcional del producto
- `TUP_PIV_BE_PROPUESTA_ARQ.pdf` — propuesta de arquitectura de la cátedra
- `Plan anti jailbreak/guia_didactica_ia/` — el otro set de documentación

**Ninguno se versiona en este repositorio.** El PRD y la propuesta se distribuyen por los canales de
la cátedra; la guía didáctica está en manos de quien la escribió.
