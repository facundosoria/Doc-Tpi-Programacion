# Correcciones aplicadas — las tres presentaciones importadas

`mapa-conceptual-interactivo.html`, `guia-golden-set.html` e
`informe-gestion-modelos.html` se armaron sobre la rama `doc-tpi-unificada` —la que se
importó en [`docs/importado/`](../docs/importado/)—, no sobre [`docs/`](../docs/). Eso
las dejó desalineadas en ocho puntos.

**Los ocho están corregidos.** Este documento es el registro de qué se cambió y por
qué, para que nadie tenga que reconstruirlo desde el historial.

> **Cuál manda cuando difieren:** `docs/`, por la misma razón de siempre — es la
> documentación de trabajo y tiene el registro de decisiones en
> [`docs/08`](../docs/08-decisiones-y-pendientes.md).

> **Estas tres ya no son byte a byte idénticas a su rama de origen.** Fue una decisión
> explícita: se privilegió que digan lo mismo que `docs/` antes que conservarlas
> intactas. El original sigue disponible:
>
> ```bash
> git show origin/doc-tpi-unificada:MAPA_CONCEPTUAL_IA_INTERACTIVO.html
> ```

---

## Lo que ya coincidía, y no se tocó

Verificado valor por valor contra `docs/`:

| Qué | `docs/` |
|---|---|
| Los 36 requerimientos, de RF-IA-01 a RF-IA-36 | los mismos códigos |
| Las 5 dimensiones y sus pesos, 30/25/20/15/10 | [13](../docs/13-rubrica-y-prompts.md) §1 |
| Tolerancia PAR-14: MAE ≤ ±5,0 promedio · ≤ ±10,0 por dimensión | [03](../docs/03-modelos-costos-y-contexto.md) §3 |
| Muestreo docente PAR-10 del 10 % | [04](../docs/04-funciones-de-ia.md) §3 |
| Golden set en dos niveles: 50 base · 15-20 por curso | [04](../docs/04-funciones-de-ia.md) §4c |
| Quién puntúa el golden set: docentes, nunca un modelo | [04](../docs/04-funciones-de-ia.md) §4b |
| Los cinco roles de IA (RF-IA-23) | [01](../docs/01-problema-y-alcance.md) |
| Evaluador sin plan B (RF-IA-25) frente a pools para el resto | [04](../docs/04-funciones-de-ia.md) §5 |
| Los cinco costos unitarios por función | [03](../docs/03-modelos-costos-y-contexto.md) §2 |
| El evaluador nunca corre local | ADR-011 |

---

## 1 · El registro de ADR

El mapa traía los 16 ADR de la rama importada, donde del 010 en adelante el mismo
número significaba otra decisión que en [`docs/08`](../docs/08-decisiones-y-pendientes.md).

**Aplicado:** los 15 ADR del mapa son ahora los de `docs/08`, uno a uno. Se eliminó la
última referencia al **ADR-016**, que no existe en nuestro registro: el marco GRC de las
4 T's de IBM quedó citado como lo que es —un marco externo, fuera del registro— tanto en
el mapa como en las dos slides del deck de defensa que lo mencionaban.

## 2 · Java, no FastAPI

`docs/08` marca el ADR-005 con *«⚠️ Revisada. Antes decía Python FastAPI»*: el servicio
va en Java Spring Boot.

**Aplicado:**

- El buffer anti-fuga dice **JavaParser**, no `tree-sitter`. Es el argumento de
  [02](../docs/02-arquitectura-y-stack.md): con los desafíos en Java, JavaParser es
  mejor, y `tree-sitter` queda como contingencia si algún día entran otros lenguajes.
  La fila del panel de debate que daba ganador a `tree-sitter` decía lo contrario que el 02.
- El scoring híbrido se calcula en **código Java determinístico**, no Python.
- En la guía: `Pydantic` → Bean Validation, `SQLAlchemy` → JPA/Hibernate.
- El panel *«Java Spring Boot vs Python FastAPI»* se conserva —la comparación es
  material de defensa— pero ahora dice que la decisión está tomada, y sus columnas
  se leen «adoptado» y «descartado» en vez de «Gateway» y «Engine».

## 3 · Los endpoints

[`docs/17`](../docs/17-mapa-de-integracion.md) fija el contrato bajo `/ai/…`.

**Aplicado:** no queda ningún `/api/v1/…` de los nuestros. Los tres que faltaban pasaron
a `POST /ai/auditoria/override`, `GET /ai/admin/health/calibracion` y
`PUT /ai/admin/parametros/PAR-14`.

> Queda un `/api/v1/cursos` en un ejemplo de código de la guía. **Es correcto:** ese
> controller es del Core Académico (Tema 02), no nuestro, y el doc 17 sólo gobierna
> el prefijo `/ai/`.

## 4 · La deriva es mensual, no nocturna

La guía la corría con un cron a las 03:00 todas las noches; `docs/` la corre **mensual
(PAR-15)** y además ante cualquier cambio de versión del proveedor. Son 30 corridas por
mes contra una: poca plata, mucha cuota de API.

**Aplicado:** las nueve menciones —tres en el mapa, seis en la guía— dicen mensual y
citan PAR-15.

## 5 · El costo: la hipótesis subió a `docs/03`

El informe ubica el escenario realista en **USD 62 a 118 al año**;
[`docs/03`](../docs/03-modelos-costos-y-contexto.md) decía **USD 5 a 22 por
cuatrimestre** para el mismo volumen.

**No era un error del informe.** Es otra hipótesis sobre el tamaño del prompt: de 350
tokens crudos a **~1.550 tokens** una vez que se le suman el system prompt pedagógico,
los delimitadores XML, la rúbrica y el RAG.

**Aplicado:** el razonamiento completo —la anatomía del payload y por qué el sobrecosto
neto de seguridad es del 20-25 % y no del 400 % gracias al prompt caching— está ahora en
[`docs/03`](../docs/03-modelos-costos-y-contexto.md) §1, y el **ADR-010** de
[`docs/08`](../docs/08-decisiones-y-pendientes.md) suma la cláusula de revisión: si el
payload medido se acerca a 1.550 tokens, el escenario se rehace con ese número. Los
números del informe no se tocaron: son el escenario conservador, y ahora `docs/03`
explica por qué hay dos rangos.

## 6 · El catálogo de modelos

**Aplicado:** salieron de la tabla del informe las cinco filas de generaciones
anteriores —GPT-4o, GPT-4o mini, Claude 3.5 Haiku, Claude 3.7 Sonnet y Gemini 3.8
Flash—, se agregó **Claude Sonnet 5**, que faltaba, y los dos DeepSeek quedaron con los
precios off-peak vigentes de `docs/03` (V4-Flash `0,22 / 0,66`, V4-Pro `0,66 / 1,98`).

El aviso que el informe traía y `docs/03` no tenía también subió: **Gemini 3.5
Flash-Lite** tiene dos fuentes externas que discrepan, `0,15 / 1,25` contra
`0,30 / 2,50`. Está anotado en el catálogo del 03 para confirmarlo en la consola de
Google antes de fijarlo.

## 7 · Los enlaces del mapa

Cada requerimiento y cada plan tenía un `repoPath` con la estructura de la rama vieja, y
el botón *«Ver en GitHub»* apuntaba a `doc-tpi-unificada`.

**Aplicado:** los 93 `repoPath` apuntan a rutas que existen en `main`, y el botón
también. Se verificó archivo por archivo que los 25 destinos distintos existen.

## 8 · Abren sin internet — y ya no sólo estas tres

Los tres bajaban Tailwind y tipografías de un CDN; el informe además bajaba `marked`
—sin el cual quedaba **la página en blanco**, porque todo su texto vive en una variable
de JavaScript—, el mapa `lucide` y la guía Font Awesome.

**Aplicado:** las librerías viven en [`vendor/`](vendor/) y se cargan por ruta relativa.
Las tipografías pasaron a un stack del sistema en vez de bajarse de Google Fonts.
**Se extendió a las seis presentaciones**, no sólo a estas tres: el deck de defensa, la
presentación de integración y el PRD-wiki tenían el mismo problema.

| Archivo | Quién lo usa |
|---|---|
| `vendor/tailwind-play-3.4.16.js` | las seis |
| `vendor/lucide.min.js` (0.469.0) | las cinco que tienen íconos |
| `vendor/marked.min.js` (12.0.2) | el informe |
| `vendor/font-awesome/` (6.5.1, CSS y woff2) | la guía |

> **Cada archivo tiene respaldo por CDN.** Vendorizar por ruta relativa arregló el aula
> y rompió otro caso: el `.html` que viaja solo, sin `vendor/` al lado, se veía sin
> estilo —antes del cambio, esos tres se lo bajaban del CDN y funcionaban—. Ahora cada
> tag local va seguido de un respaldo que sólo actúa si la librería no quedó definida
> (`window.tailwind`, `window.lucide`, `window.marked`; Font Awesome, por `onerror` del
> `<link>`). Con `vendor/` presente el respaldo no dispara y no sale un pedido a la red.

> **Mermaid se sacó del todo.** Tres archivos lo cargaban —3,3 MB— y **ninguno de los
> seis dibuja un solo diagrama**: sólo llamaban a `mermaid.initialize()` sobre una
> página sin diagramas. Se fue la librería y se fueron las llamadas.

---

## Y una novena, que no estaba en la lista

**El mapa no funcionaba.** Al abrirlo mostraba el encabezado y nada más: un error de
sintaxis en JavaScript cortaba todo el script. Le faltaba la clave `'spec':` a uno de
los seis glosarios, así que el objeto entero era inválido y no se ejecutaba ni el
render de las tarjetas ni los íconos.

**Lo rompió la importación, no venía así.** El archivo original de
`doc-tpi-unificada` tiene la clave; se perdió en el commit que trajo las
presentaciones a este repositorio, junto con los cambios de endpoints, Java y ADR. No se
notaba sin abrir la consola del navegador. Corregido y verificado: los 36
requerimientos, los 15 ADR y los seis glosarios cargan.

---

## Antes de proyectar

1. **Abrilas igual una vez en la máquina del aula.** Ya no necesitan red, pero un doble
   clic de prueba cuesta treinta segundos.
2. El informe tiene su propio botón de imprimir a PDF, por si preferís llevarlo así.
3. Si el archivo viaja sin `vendor/`, el estilo sale del CDN y hace falta red. Para
   proyectar sin internet, llevate `presentaciones/` completa, no un `.html` suelto.
