# Correcciones sugeridas — documentación extendida

Estos documentos llegaron desde la rama `doc-tpi-unificada` y **se importaron sin
tocar una sola línea**: cada archivo es byte a byte idéntico al original. Lo que
sigue son correcciones que conviene aplicar, anotadas acá en vez de aplicadas, para
que las haga quien es dueño del texto.

---

## 🟡 1. Dos índices apuntan a carpetas con el nombre viejo

Al unificar, las carpetas cambiaron de nombre. Dos documentos siguen nombrando el
anterior:

| Archivo | Línea | Dice | Debería decir |
|---|---|---|---|
| `investigacion-jailbreak/00_INDICE_ANALISIS_IBM.md` | 15 | `investigacion_jailbreak/` | `investigacion-jailbreak/` |
| `planes-de-ejecucion/00_INDICE_MAESTRO.md` | 5 | `planes_seccion_15/` | `docs-extendidos/planes-de-ejecucion/` |

Son menciones en prosa, no enlaces: nada está roto, solo desactualizado. Todos los
enlaces reales entre documentos son relativos y siguen funcionando, porque las
carpetas se movieron enteras.

---

## 🟡 2. Quince documentos quedaron afuera por ser copias exactas

La rama traía también estas carpetas:

- `02_FUNDAMENTOS_Y_DEFENSA_ORAL/` (8 archivos)
- `04_GESTION_ROADMAP_Y_EQUIPO/` (5 archivos)
- `03_RUBRICAS_PROMPTS_Y_CALIBRACION/01_...` y `02_...` (2 archivos)

Los quince eran **copias byte a byte** de los documentos `docs/01` a `docs/15` tal
como estaban en `main`, solo que renombrados y repartidos en carpetas temáticas. Los
de [`docs/`](../docs/) son posteriores: crecieron bastante desde entonces —
`04-funciones-de-ia.md` pasó de 54 KB a 101 KB, por ejemplo— y además se sumó
`docs/16`.

Traer las dos versiones habría dejado el mismo texto dos veces, contradiciéndose. Se
conservó la más nueva.

**Equivalencias, por si alguien busca un documento por el nombre viejo:**

| Nombre en `doc-tpi-unificada` | Dónde está ahora |
|---|---|
| `04_GESTION.../01_ALCANCE_Y_FRONTERAS_INTER_EQUIPOS.md` | [`docs/01-problema-y-alcance.md`](../docs/01-problema-y-alcance.md) |
| `02_FUNDAMENTOS.../02_DEBATE_ARQUITECTURA_JAVA_VS_PYTHON.md` | [`docs/02-arquitectura-y-stack.md`](../docs/02-arquitectura-y-stack.md) |
| `02_FUNDAMENTOS.../03_ANALISIS_ECONOMICO_Y_CONTEXTO_LLM.md` | [`docs/03-modelos-costos-y-contexto.md`](../docs/03-modelos-costos-y-contexto.md) |
| `03_RUBRICAS.../02_ESPECIFICACION_FUNCIONES_Y_JUECES.md` | [`docs/04-funciones-de-ia.md`](../docs/04-funciones-de-ia.md) |
| `02_FUNDAMENTOS.../08_SEGURIDAD_FRONTERAS_Y_GUARDARRAILES.md` | [`docs/05-seguridad.md`](../docs/05-seguridad.md) |
| `02_FUNDAMENTOS.../04_OPERACION_INGENIERIA_Y_CARGA.md` | [`docs/06-operacion-e-ingenieria.md`](../docs/06-operacion-e-ingenieria.md) |
| `02_FUNDAMENTOS.../05_DATOS_TRAZABILIDAD_Y_TERMINOS_LEGALES.md` | [`docs/07-datos-y-terminos.md`](../docs/07-datos-y-terminos.md) |
| `04_GESTION.../02_DECISIONES_ABIERTAS_Y_PENDIENTES.md` | [`docs/08-decisiones-y-pendientes.md`](../docs/08-decisiones-y-pendientes.md) |
| `02_FUNDAMENTOS.../01_PREGUNTAS_Y_RESPUESTAS_DEFENSA.md` | [`docs/09-preguntas-y-respuestas.md`](../docs/09-preguntas-y-respuestas.md) |
| `04_GESTION.../03_PLAN_DE_TRABAJO_12_PASOS_6_PERSONAS.md` | [`docs/10-entregables-y-plan.md`](../docs/10-entregables-y-plan.md) |
| `04_GESTION.../04_GLOSARIO_Y_METADATA_INTEGRACION.md` | [`docs/11-glosario-y-metadata.md`](../docs/11-glosario-y-metadata.md) |
| `02_FUNDAMENTOS.../06_ALMACENAMIENTO_E_INGESTA_MULTIMODAL.md` | [`docs/12-almacenamiento-e-ingesta.md`](../docs/12-almacenamiento-e-ingesta.md) |
| `03_RUBRICAS.../01_RUBRICA_ANCLAS_Y_PROMPTS_COMPLETOS.md` | [`docs/13-rubrica-y-prompts.md`](../docs/13-rubrica-y-prompts.md) |
| `02_FUNDAMENTOS.../07_HISTORIAL_DE_SINCRONIZACION_Y_CONFLICTOS.md` | [`docs/14-sincronizacion-guia-didactica.md`](../docs/14-sincronizacion-guia-didactica.md) |
| `04_GESTION.../05_ESTADO_ACTUAL_BLOQUEOS_Y_CONTINUACION.md` | [`docs/15-sincronizacion-arquitectura-y-despliegue.md`](../docs/15-sincronizacion-arquitectura-y-despliegue.md) ⚠️ |

⚠️ El último no es equivalencia directa: el `docs/15` de esta rama es un documento
**distinto** —la sincronización con la U1 de Front End—, porque el «estado y cómo
continuar» que ocupaba ese número se reorganizó. El contenido viejo sigue disponible
en `main`, en `docs/15-estado-y-como-continuar.md`.

---

## 🟢 3. Hay solapamiento de contenido con `docs/`, y no está resuelto

`especificacion-tecnica/` y `investigacion-jailbreak/` cubren temas que `docs/05` y
`docs/02` también tratan, a veces con números o decisiones distintas. **No se
armonizaron**: cada set conserva lo que decía.

Cuál manda, cuando difieren: los `docs/` son la documentación de trabajo del equipo y
tienen registro de decisiones (ADR) en `docs/08`. Lo de acá es material de
profundización y defensa. Antes de la entrega conviene una pasada de conciliación,
igual que la que quedó registrada en
[`docs/14`](../docs/14-sincronizacion-guia-didactica.md) para el otro set.
