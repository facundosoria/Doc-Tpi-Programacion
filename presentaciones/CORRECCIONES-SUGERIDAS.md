# Correcciones sugeridas — las tres presentaciones nuevas

`mapa-conceptual-interactivo.html`, `guia-golden-set.html` e
`informe-gestion-modelos.html` entraron **sin tocar una sola línea**, igual que el
material de [`docs/importado/`](../docs/importado/). Lo que sigue son las diferencias
contra [`docs/`](../docs/), anotadas acá en vez de aplicadas.

**Las tres están construidas sobre la rama `doc-tpi-unificada`, no sobre `docs/`.** El
mapa lo dice en su encabezado —*«Rama GitHub: facundosoria/Doc-Tpi-Programacion@doc-tpi-unificada»*—
y enlaza a rutas como `03_RUBRICAS_PROMPTS_Y_CALIBRACION/planes_ejecucion/`. El informe
declara como fuente primaria `Doc-TPI-Completa/` y cita los archivos por su nombre viejo.

Esa rama es la que se importó en `docs/importado/`, y su
[`CORRECCIONES-SUGERIDAS.md`](../docs/importado/CORRECCIONES-SUGERIDAS.md) §3 ya avisa
que el solapamiento con `docs/` **no está armonizado**. Las tres presentaciones heredan
exactamente ese problema.

> **Cuál manda cuando difieren:** `docs/`, por la misma razón de siempre — es la
> documentación de trabajo y tiene el registro de decisiones en
> [`docs/08`](../docs/08-decisiones-y-pendientes.md).

---

## Lo que sí coincide

Esto se verificó valor por valor contra `docs/`, no a ojo:

| Qué | Presentación | `docs/` |
|---|---|---|
| Requerimientos de IA | 36, de RF-IA-01 a RF-IA-36 | 36, los mismos códigos |
| Las 5 dimensiones y sus pesos | 30/25/20/15/10, fijos por RF-IA-15 | [13](../docs/13-rubrica-y-prompts.md) §1: idénticos |
| Tolerancia PAR-14 | MAE ≤ ±5,0 promedio · ≤ ±10,0 por dimensión | [03](../docs/03-modelos-costos-y-contexto.md) §3, [04](../docs/04-funciones-de-ia.md) §3 |
| Muestreo docente PAR-10 | 10 % | [04](../docs/04-funciones-de-ia.md) §3: default 10 % |
| Golden set en dos niveles | 50 casos base · 15 a 20 por curso | [04](../docs/04-funciones-de-ia.md) §4c: 40-50 base · 15-20 por curso |
| Quién puntúa el golden set | Docentes humanos, nunca un modelo | [04](../docs/04-funciones-de-ia.md) §4b: la única regla sin atajo |
| Los cinco roles de IA (RF-IA-23) | Moderador, tutor, evaluador, generador, RAG | [01](../docs/01-problema-y-alcance.md): «las 5 funciones de IA» |
| Evaluador sin plan B (RF-IA-25) frente a pools para el resto (RF-IA-26) | La distinción está bien hecha | [04](../docs/04-funciones-de-ia.md) §5 |
| Costo unitario por función | 0,00052 tutor · 0,006 evaluador · 0,00083 generador · 0,002 corrector · 0,0001 RAG | [03](../docs/03-modelos-costos-y-contexto.md) §2: los mismos cinco números |
| El evaluador nunca corre local | Salvo que la calibración lo demuestre | ADR-011 de [08](../docs/08-decisiones-y-pendientes.md) |

**El núcleo normativo está alineado.** Lo que se corrió es la capa de ingeniería:
arquitectura, decisiones, endpoints y el total de plata.

---

## 🔴 1. El registro de ADR es otro, y los números chocan

El mapa trae 16 ADR, que son los de
[`docs/importado/especificacion-tecnica/07_REGISTRO_DE_DECISIONES_ADR.md`](../docs/importado/especificacion-tecnica/07_REGISTRO_DE_DECISIONES_ADR.md).
[`docs/08`](../docs/08-decisiones-y-pendientes.md) tiene 15, y **de ADR-010 en adelante
el mismo número significa otra decisión**:

| # | Dice el mapa | Dice `docs/08` |
|---|---|---|
| 001 a 003 | Servicio único · ruteo determinístico · sincrónico acotado | Lo mismo. El 003 de `docs/` suma al moderador |
| **004** | pgvector en un **PostgreSQL dedicado del microservicio** | pgvector en el **Postgres existente, sin base dedicada** |
| **005** | **Híbrida: Spring Boot sidecar + FastAPI engine** | **Java Spring Boot**, y nada más |
| 006 a 008 | Embeddings locales · perímetro temático · solución fuera del prompt | Lo mismo |
| **009** | **Streaming SSE con buffer interceptor AST** | **Sin streaming token a token en desafíos prácticos** (MVP) |
| **010** | Parámetros de reproducibilidad (temperature 0, seed 42) | **Escenario B de costos: ~USD 21 por cuatrimestre** |
| **011** | Scoring híbrido, 45 % código y 55 % LLM | **El evaluador nunca corre en un modelo local** |
| **012** | Inmutabilidad forzada por triggers PL/pgSQL | **El moderador resuelve con técnica clásica** |
| **013** | Cuotas FinOps en Redis | **Rolling update** como estrategia de despliegue |
| **014** | Calibración nocturna y circuit breaker | **El proveedor de LLM no entra en la sonda de readiness** |
| **015** | Frontera de XP en el motor de desafíos | **Nginx vive en el borde** |
| 016 | Marco GRC de las 4 Ts | No existe |

**Por qué importa en la defensa:** si alguien pregunta qué dice el ADR-011, el mapa
proyectado y el documento abierto contestan dos cosas distintas. Es el conflicto más
caro de los ocho, y el más fácil de disparar sin querer.

**Qué hacer:** o el mapa renumera contra `docs/08`, o se aclara en voz alta que está
mostrando el registro de la rama importada. No hay una tercera.

---

## 🔴 2. ADR-005 quedó revisado, y las presentaciones traen la versión vieja

[`docs/08`](../docs/08-decisiones-y-pendientes.md) marca el ADR-005 con
*«⚠️ Revisada. Antes decía Python FastAPI»*. Hoy el servicio va en **Java Spring Boot**,
igual que el resto de la plataforma. La decisión está repetida en D-1 y en el
[15](../docs/15-sincronizacion-arquitectura-y-despliegue.md) §6, donde ya se corrigió
una tabla que decía Python contra el ADR-005.

Dónde aparece la versión vieja:

| Archivo | Qué dice |
|---|---|
| `guia-golden-set.html` | Todo el §9 y el §10 son Python: `from fastapi import APIRouter`, `sqlalchemy.orm`, Pydantic v2, Celery Beat. Y una pantalla de ejemplo lista la materia como *«Programación IV (Python 3.12 · FastAPI · PostgreSQL)»*, cuando el fundamento de Java arranca justamente de que la materia es de Java |
| `mapa-conceptual-interactivo.html` | Mayormente Java —`Java Validator`, `ProviderPoolManager`, `ChatClient Factory`—, pero su ADR-005 dice híbrida, y quedan tres componentes en `Python FastAPI / pgvector`, `Python / Java Worker` y `Celery Beat (03:00 AM)` |
| `informe-gestion-modelos.html` | No toma partido: razona por rol y por proveedor, que es agnóstico del lenguaje |

El mapa además atribuye el análisis de código a `Java (Tree-sitter/Levenshtein)`, cuando
[02](../docs/02-arquitectura-y-stack.md) argumenta que **con los desafíos en Java,
`JavaParser` es mejor que `tree-sitter`** — y es uno de los dos motivos por los que
Python dejó de ganar.

---

## 🔴 3. Los endpoints no son los del doc 17

[`docs/17`](../docs/17-mapa-de-integracion.md) fija el contrato bajo `/ai/…`. El mapa
usa `/api/v1/…`, con rutas que no aparecen en ningún documento:

| El mapa dice | El [17](../docs/17-mapa-de-integracion.md) dice |
|---|---|
| `POST /api/v1/calibration/run` · `GET /api/v1/calibration/status` | `POST /ai/calibracion` · `GET /ai/calibracion/{curso_cohorte_id}` |
| `GET /api/v1/evaluaciones-pendientes` | `GET /ai/pendientes/{curso_cohorte_id}` |
| `POST /api/v1/tutor/stream` · `POST /api/v1/rag/query` | `POST /ai/tutor` · `POST /ai/{funcion}` |
| — | `GET /ai/jobs/{job_id}` · `POST /internal/ai-result` · `POST /ai/ingesta` |

La guía tiene un solo endpoint, `PUT /api/v1/admin/parametros/PAR-14`, con el mismo
prefijo ajeno.

**Este es el que rompe algo real**, no solo una lámina: el 17 es lo que los otros
equipos van a leer para integrarse. Si la presentación muestra otras rutas, alguien va a
codear contra las equivocadas.

---

## 🟡 4. La deriva: la guía la corre de noche, `docs/` la corre por mes

| Fuente | Cada cuánto |
|---|---|
| `guia-golden-set.html` | Celery Beat **a las 03:00**, todas las noches, con circuit breaker HTTP 503 |
| `mapa-conceptual-interactivo.html` | RF-IA-32, *«detección de deriva (drift nocturno)»* |
| [`docs/04`](../docs/04-funciones-de-ia.md) y [`docs/06`](../docs/06-operacion-e-ingenieria.md) | **Mensual (PAR-15)** y ante cambio de versión del proveedor |

No es una contradicción de fondo —las dos detectan lo mismo—, pero sí de costo: correr
el golden set completo cada noche son 30 corridas por mes en lugar de una. Con el
evaluador a USD 0,006 la evaluación y 50 casos, la diferencia es chica en plata y grande
en cuota de API. **Hay que elegir uno de los dos números y ponerlo en los dos lados.**

---

## 🟡 5. El costo total: USD 62 a 118 al año contra USD 5 a 22 el cuatrimestre

El informe modela cinco intensidades de uso para 120 alumnos y ubica el escenario
realista —su «nivel 3»— en **USD 62 a 118 al año**.
[`docs/03`](../docs/03-modelos-costos-y-contexto.md) §1 dice **USD 5 a 22 por
cuatrimestre** para la misma población.

Y el volumen es el mismo. El nivel 3 del informe son 5 consultas de tutor por alumno por
semana × 32 semanas × 120 alumnos = **19.200 consultas**, que es exactamente la cifra que
el [03](../docs/03-modelos-costos-y-contexto.md) usa **por cuatrimestre**. O sea que el
informe cuenta la mitad del uso y cobra entre tres y cinco veces más.

La diferencia no es un error de cuentas: es otra hipótesis sobre el tamaño del prompt. El
informe la explicita y la defiende bien —*«de 350 tokens crudos a 1.550 tokens
seguros»*: system prompt pedagógico, delimitadores XML, rúbrica y RAG—, y concluye que el
sobrecosto neto de seguridad es del 20 al 25 % gracias al prompt caching. **Ese
razonamiento no está en el 03, y probablemente debería estar.**

**Qué hacer:** esto no es una corrección al informe, es una pregunta abierta para el
[03](../docs/03-modelos-costos-y-contexto.md). Si el payload real son 1.550 tokens y no
350, el rango del 03 está corto, y el ADR-010 —«escenario B, ~USD 21 por cuatrimestre»—
hay que revisarlo con el número medido en vez del estimado. Es material para el
[08](../docs/08-decisiones-y-pendientes.md), no para una lámina.

---

## 🟡 6. El catálogo del informe mezcla dos generaciones de modelos

La tabla comparativa del informe suma modelos que no están en
[`docs/03`](../docs/03-modelos-costos-y-contexto.md): **Claude 3.5 Haiku**,
**Claude 3.7 Sonnet**, **GPT-4o**, **GPT-4o mini** y **Gemini 3.8 Flash**, al lado de los
cinco que sí usamos: GPT-5 nano, Gemini 3.5 Flash-Lite, Claude Haiku 4.5, Claude Sonnet 5
y DeepSeek V4.

Los cinco nuestros coinciden **al centavo** con el 03, incluido el `1,00 / 5,00` de
Haiku 4.5 y el `0,05 / 0,40` de GPT-5 nano. Los otros cinco son de generaciones
anteriores y le agregan filas a una decisión que ya está tomada.

Dos avisos del informe que **sí** conviene subir al 03, porque son posteriores a su fecha
de corte:

- **Gemini 3.5 Flash-Lite:** dos fuentes externas discrepan, `0,15 / 1,25` contra
  `0,30 / 2,50`. Conviene confirmarlo en la consola de Google antes de fijarlo.
- **DeepSeek V4-Flash:** pasó a tarifas peak/off-peak en agosto de 2026, con subas de
  hasta el 1.100 % en algunas franjas. La advertencia del 03 sobre su volatilidad quedó,
  si acaso, corta.

---

## 🟡 7. Los enlaces del mapa apuntan a rutas que ya no existen

Cada requerimiento y cada plan del mapa tiene un `repoPath` con la estructura vieja:

```text
03_RUBRICAS_PROMPTS_Y_CALIBRACION/planes_ejecucion/05_PLAN_TRAZABILIDAD_CAMBIO_MODELO.md
```

En este repositorio ese archivo vive en:

```text
docs/importado/planes-de-ejecucion/05_PLAN_TRAZABILIDAD_CAMBIO_MODELO.md
```

El botón *«Ver en GitHub (doc-tpi-unificada)»* funciona, porque la rama sigue en
`origin`. Pero apunta a un estado congelado del repositorio, no a `main`. **Nada está
roto; todo está desactualizado.**

---

## 🔴 8. Ninguno de los tres abre sin internet

Los tres cargan Tailwind desde `cdn.tailwindcss.com` y las tipografías desde Google
Fonts. Además:

| Archivo | Qué más baja | Qué pasa sin red |
|---|---|---|
| `informe-gestion-modelos.html` | `marked.min.js` desde jsDelivr | 🔴 **Página en blanco.** Todo el texto vive en una variable de JavaScript y lo renderiza `marked` |
| `mapa-conceptual-interactivo.html` | `lucide` desde unpkg | 🔴 **Sin estilo y sin íconos.** Los datos son inline, así que el contenido está, pero la página queda ilegible |
| `guia-golden-set.html` | Font Awesome desde cdnjs | 🟡 Sin estilo y sin íconos, pero el texto es HTML estático y se lee |

`defensa-39-slides.html` tiene el mismo problema con Tailwind, lucide y Mermaid, así que
no es nuevo. Pero acá se agrava: **el informe no muestra absolutamente nada.**

**Qué hacer:** si alguno se va a proyectar, abrirlo antes en la máquina y la red del
aula. Si el aula no tiene wifi confiable, la salida es imprimir el informe a PDF con su
propio botón —desde una máquina con red— y llevar el PDF.

---

## Qué hacer, en orden

| # | Qué | Por qué ese orden |
|---|---|---|
| 1 | Abrir los tres en la máquina y la red del aula, antes del día | Es lo único que se arregla el mismo día o no se arregla |
| 2 | Decidir si el mapa renumera sus ADR o se presenta como «el registro de la rama importada» | Es lo único que puede hacer quedar mal a alguien en vivo |
| 3 | Corregir los endpoints del mapa contra el [17](../docs/17-mapa-de-integracion.md) | Es lo único que puede hacer que otro equipo codee mal |
| 4 | Elegir un número para la deriva, nocturna o mensual, y ponerlo en los dos lados | Barato de arreglar, y hoy son dos respuestas a la misma pregunta |
| 5 | Llevar la hipótesis de los 1.550 tokens al [03](../docs/03-modelos-costos-y-contexto.md) y revisar el ADR-010 | Es un aporte del informe, no un defecto suyo |
| 6 | Sacar del catálogo del informe las cinco filas de generaciones viejas | Cosmético |
| 7 | Actualizar los `repoPath` del mapa | Cosmético, y solo si el mapa se va a mantener |

Del 1 al 3 son de las presentaciones. Del 4 al 7 son, en realidad, trabajo sobre
`docs/`: el informe encontró cosas que a la documentación le faltan.
