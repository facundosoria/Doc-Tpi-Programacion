# Presentaciones

| Archivo | Qué es | Cómo se usa |
|---|---|---|
| [`presentacion-tpi-ia.html`](presentacion-tpi-ia.html) | **La presentación de defensa.** 43 slides | Se abre en el navegador. `←` `→` para navegar, `O` para el índice, `F` para pantalla completa |
| [`prd-ia-referencia.html`](prd-ia-referencia.html) | El PRD, sección IA, como wiki navegable | Documento de consulta con barra lateral. **No es una presentación** |

Los dos son archivos HTML autocontenidos: no necesitan servidor ni compilación. Se
abren con doble clic.

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
