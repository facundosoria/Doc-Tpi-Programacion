# Presentaciones

| Archivo | Qué es | Cómo se usa | Alineado con `docs/` |
|---|---|---|---|
| [`defensa-39-slides.html`](defensa-39-slides.html) | **La presentación de defensa.** 39 slides | Se abre en el navegador. `←` `→` para navegar, `O` para el índice, `F` para pantalla completa | ✅ Sí |
| [`presentacion-integracion-servicios.html`](presentacion-integracion-servicios.html) | **Integración y contratos inter-servicios.** 17 slides | Se abre en el navegador. Teclado (`←`/`→`/`Espacio`), touch, selector directo de slide | ✅ Sí |
| [`prd-wiki-consulta.html`](prd-wiki-consulta.html) | El PRD, sección IA, como wiki navegable | Documento de consulta con barra lateral. **No es una presentación** | ✅ Sí |
| [`mapa-conceptual-interactivo.html`](mapa-conceptual-interactivo.html) | Los 36 requerimientos de IA como mapa navegable, con matriz de trazabilidad, ADR, defensa oral, planes y seguridad | Seis solapas arriba. Cada requerimiento se abre y muestra rol, componente, regla y mapeo externo | ✅ Sí |
| [`guia-golden-set.html`](guia-golden-set.html) | El golden set explicado entero: qué es, cómo se opera, DDL, runner y Q&A de cátedra | Documento largo con índice lateral. **No es una presentación** | ✅ Sí |
| [`informe-gestion-modelos.html`](informe-gestion-modelos.html) | Proveedores, costos, seguridad y local contra cloud, con calculadora de 5 intensidades | Documento con índice y botón de imprimir a PDF | ✅ Sí |

Los seis son un archivo HTML solo: no necesitan servidor ni compilación, se abren con
doble clic.

**Las seis abren sin internet.** Cargan sus librerías desde [`vendor/`](vendor/), que
está versionado acá al lado, y usan tipografías del sistema. Si movés esa carpeta, las
seis pierden el estilo: las rutas son relativas a `presentaciones/`.

## Las tres nuevas venían de otra rama, y ya están alineadas

`mapa-conceptual-interactivo.html`, `guia-golden-set.html` e
`informe-gestion-modelos.html` se armaron contra `doc-tpi-unificada` —la rama que se
importó en [`docs/importado/`](../docs/importado/)—, no contra `docs/`. Eso las dejaba
desalineadas en ocho puntos: el registro de ADR, el ADR-005 viejo con FastAPI, los
endpoints, la frecuencia de la detección de deriva, el costo total, el catálogo de
modelos, los enlaces al repositorio y la dependencia del CDN.

**Los ocho están corregidos**, y con ellos apareció un noveno que no estaba en la lista:
el mapa no funcionaba —un error de sintaxis en JavaScript lo dejaba en blanco—.

**El núcleo normativo ya coincidía y no se tocó:** los 36 RF-IA, los pesos
30/25/20/15/10, PAR-14 con ±5 y ±10, el muestreo del 10 %, el golden set en dos niveles
y los cinco costos unitarios por función.

👉 **El registro de qué se cambió en cada una está en
[`CORRECCIONES-SUGERIDAS.md`](CORRECCIONES-SUGERIDAS.md).** A diferencia del resto del
material importado, estas tres **ya no son byte a byte idénticas a su rama de origen**:
se privilegió que digan lo mismo que `docs/`.

## Qué se unificó

Había **dos decks distintos** repartidos en dos ramas, con el mismo diseño y contenido
solapado:

| Origen | Slides | Qué pasó |
|---|---|---|
| `doc-tpi-unificada` · `Presentacion_TPI_IA_Propuesta_Unificada_Master.html` | 31 | Es la base del deck unificado |
| `lara` · `PROPUESTA_IA_LLM.html` | 14 | 12 de sus slides ya estaban en el otro; **las 2 que faltaban se portaron** |

Las dos que se portaron son «Topología Híbrida: Java + FastAPI» y «Alineación con PRD
y Decisiones de Arquitectura», que entraron sin retocar una línea de su marcado.

El original de `lara` no se duplica acá porque su contenido está entero adentro del
deck unificado. Si alguien lo necesita tal cual:

```bash
git show origin/lara:demoLLMSpringAi/PROPUESTA_IA_LLM.html > propuesta-original.html
```

## Qué se agregó

Seis slides nuevas con material del proyecto que no estaba en ninguno de los dos
decks, sacadas de la documentación de `docs/`:

| # | Slide | De dónde sale |
|---|---|---|
| 03 | Los siete hallazgos que ordenan todo el trabajo | `README` y `docs/08` |
| 09 | Los tres «gateway» que se dicen igual | `docs/15` §2 |
| 33 | El costo real, y dónde está la palanca | `docs/03` |
| 36 | Los ocho módulos, y el reparto entre seis | `docs/10` parte 2 |
| 37 | Decisiones cerradas, y lo que bloquea hoy | `README` y `docs/08` |
| 38 | El mapa del repositorio unificado | esta unificación |

**Todos los números son los medidos, no estimaciones**: el rango de USD 5 a 22 del
cuatrimestre, los costos unitarios por función, los pesos de la rúbrica. Si alguno
cambia en la documentación, hay que cambiarlo también acá — el deck no se genera solo.
