# Código de ejemplo

Dos proyectos Java distintos, con propósitos distintos. Van separados a propósito:
no comparten `pom.xml`, ni paquete, ni ciclo de vida.

| Carpeta | Quién lo hizo | Qué es | Estado |
|---|---|---|---|
| [`ms-evaluacion-llm/`](ms-evaluacion-llm/) | **El equipo** · `facundosoria`, en `feat/qa-gate` | El **esqueleto del microservicio real** del Tema 07 | Compila y pasa sus tests |
| [`lara-heredia-demo-llm-spring-ai/`](lara-heredia-demo-llm-spring-ai/) | **Lara Heredia** · `412181-HerediaLara`, en la rama `lara` | Una **demo funcional** de tutor socrático con Spring AI + Groq | Anda de punta a punta, pero es material de exploración |

**La carpeta lleva el nombre de quien la escribió** cuando el proyecto es de una
persona. `ms-evaluacion-llm/` no: es el entregable del equipo de seis y su nombre es
el del servicio, el mismo que usan los documentos y el `docker` tag. Ponerle el
nombre de una persona diría algo que no es cierto.

La autoría sale del historial, no de la memoria de nadie:

```bash
git log --format='%an  %ad  %s' --date=short -- codigo-ejemplo/lara-heredia-demo-llm-spring-ai
```

## `ms-evaluacion-llm/`

El microservicio que efectivamente vamos a entregar. Hoy es un esqueleto Spring Boot
con el contrato del moderador y sus tests, deliberadamente mínimo: existe para que
haya sobre qué construir desde el día uno.

**Es el único proyecto del equipo.** El otro es material de exploración importado de
otra rama, y no comparte con éste ni `pom.xml` ni ciclo de vida.

Contexto de diseño: [`docs/02 · Arquitectura y stack`](../docs/02-arquitectura-y-stack.md).

Política de tests (cómo mockear el LLM y cuándo no gastar tokens): [`TESTING.md`](ms-evaluacion-llm/TESTING.md).

## `lara-heredia-demo-llm-spring-ai/`

Prueba de concepto del tutor socrático: Spring Boot con Spring AI apuntando a Groq
por su API compatible con OpenAI, H2 en memoria para las conversaciones, y Swagger
para probar los endpoints a mano.

Sirve para ver funcionando las primitivas que después usa el microservicio real
—`ChatClient`, prompt de sistema, historial de conversación— sin la maquinaria del
proyecto grande encima.

> ### 🔴 Antes de tocar esta demo
>
> `application.properties` trae una **API key de Groq hardcodeada**, tal como venía en
> la rama de origen. El código se importó sin modificar, así que la corrección está
> anotada y no aplicada: ver [`CORRECCIONES-SUGERIDAS.md`](CORRECCIONES-SUGERIDAS.md).
> **La key hay que rotarla en Groq**, no solo borrarla del archivo.

Para correrla:

```bash
cd codigo-ejemplo/lara-heredia-demo-llm-spring-ai/BE && ./mvnw spring-boot:run
```

Documentación propia de la demo, dentro de `BE/`:

| Archivo | Qué trae |
|---|---|
| `LLM_SpringAI.md` | Trazabilidad de cada RF-IA del PRD contra una idea de resolución técnica |
| `LLM_PrimerEntrega_SpringAI.md` | Recorte de esa trazabilidad para la primera entrega |
| `LOGICA_AGENTE_TUTOR.md` | Cómo está construido el tutor socrático de la demo |
| `DEMO_1DIA_PLAN.md` | Plan de armado de la demo en un día |
| `GUIA_API_KEYS.md` | Cómo sacar las keys de cada proveedor |
| `POSTMAN_TEST.md` | Recorrido de prueba manual, con la colección en `postman/` |

**Este proyecto no se edita**, igual que el resto del material importado de otras
ramas: entró byte a byte y lo que haya que corregirle va anotado en
[`CORRECCIONES-SUGERIDAS.md`](CORRECCIONES-SUGERIDAS.md). Que la key hardcodeada siga
ahí no significa que no sea un problema: significa que el arreglo se propone, no se
aplica sobre código ajeno.
