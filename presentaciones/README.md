# Presentaciones

| Archivo | Qué es | Cómo se usa | Alineado con `docs/` |
|---|---|---|---|
| [`defensa-43-slides.html`](defensa-43-slides.html) | **La presentación de defensa.** 43 slides | Se abre en el navegador. `←` `→` para navegar, `O` para el índice, `F` para pantalla completa | ✅ Sí |
| [`prd-wiki-consulta.html`](prd-wiki-consulta.html) | El PRD, sección IA, como wiki navegable | Documento de consulta con barra lateral. **No es una presentación** | ✅ Sí |
| [`mapa-conceptual-interactivo.html`](mapa-conceptual-interactivo.html) | Los 36 requerimientos de IA como mapa navegable, con matriz de trazabilidad, ADR, defensa oral, planes y seguridad | Seis solapas arriba. Cada requerimiento se abre y muestra rol, componente, regla y mapeo externo | ⚠️ **Parcial** |
| [`guia-golden-set.html`](guia-golden-set.html) | El golden set explicado entero: qué es, cómo se opera, DDL, runner y Q&A de cátedra | Documento largo con índice lateral. **No es una presentación** | ⚠️ **Parcial** |
| [`informe-gestion-modelos.html`](informe-gestion-modelos.html) | Proveedores, costos, seguridad y local contra cloud, con calculadora de 5 intensidades | Documento con índice y botón de imprimir a PDF | ⚠️ **Parcial** |

Los cinco son un archivo HTML solo: no necesitan servidor ni compilación, se abren con
doble clic. Lo que sí necesitan es **internet**: cuatro de los cinco bajan Tailwind y
las tipografías de un CDN, y `informe-gestion-modelos.html` baja además la librería que
renderiza todo su texto — **sin red no muestra nada**. El detalle, y qué hacer si el
aula no tiene wifi, está en [`CORRECCIONES-SUGERIDAS.md`](CORRECCIONES-SUGERIDAS.md) §8.

## Las tres nuevas están construidas sobre otra rama

`mapa-conceptual-interactivo.html`, `guia-golden-set.html` e
`informe-gestion-modelos.html` se armaron contra `doc-tpi-unificada` —la rama que se
importó en [`docs/importado/`](../docs/importado/)—, no contra `docs/`. Entraron **sin
tocar una sola línea**, igual que ese material.

**El núcleo normativo coincide:** los 36 RF-IA, los pesos 30/25/20/15/10, PAR-14 con
±5 y ±10, el muestreo del 10 %, el golden set en dos niveles y los cinco costos
unitarios por función están todos iguales que en `docs/`.

**Lo que se corrió es la capa de ingeniería:** el registro de ADR, la arquitectura
(traen el ADR-005 viejo, con FastAPI), los endpoints, la frecuencia de la detección de
deriva y el costo total del cuatrimestre.

👉 **Las ocho diferencias, con qué manda en cada una, están en
[`CORRECCIONES-SUGERIDAS.md`](CORRECCIONES-SUGERIDAS.md).** Leelo antes de proyectar
cualquiera de las tres.

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

Diez slides nuevas con material del proyecto que no estaba en ninguno de los dos
decks, sacadas de la documentación de `docs/`:

| # | Slide | De dónde sale |
|---|---|---|
| 03 | Los siete hallazgos que ordenan todo el trabajo | `README` y `docs/08` |
| 09 | Los tres «gateway» que se dicen igual | `docs/15` §2 |
| 28 | El gate de calidad: qué problema resuelve, y cuál no | `docs/16` parte 1 |
| 29 | Las trece etapas, y sobre qué corre cada una | `docs/16` parte 5 |
| 30 | Dos perfiles, cuatro niveles y tres filtros de alcance | `docs/16` partes 4 y 8 |
| 31 | Dónde corre: tu máquina, GitHub y el server | `docs/16` partes 2, 6 y 7 |
| 37 | El costo real, y dónde está la palanca | `docs/03` |
| 40 | Los ocho módulos, y el reparto entre seis | `docs/10` parte 2 |
| 41 | Decisiones cerradas, y lo que bloquea hoy | `README` y `docs/08` |
| 42 | El mapa del repositorio unificado | esta unificación |

**Todos los números son los medidos, no estimaciones**: los 12 y 46 segundos de las
corridas, el rango de USD 5 a 22 del cuatrimestre, las 22 anclas rotas que encontró el
gate. Si alguno cambia en la documentación, hay que cambiarlo también acá — el deck no
se genera solo.
