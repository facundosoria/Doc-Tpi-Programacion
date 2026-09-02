# Documentación extendida

Material que llegó desde la rama `doc-tpi-unificada` y que **no está cubierto** por
los documentos de [`docs/`](../). Se conserva tal como fue escrito, con su
estilo y su numeración propia.

**Autor:** `Brf93` (`421562@tecnicatura.frc.utn.edu.ar`). Los 25 documentos son suyos
y entraron sin modificar. La autoría de cualquiera de ellos sale del historial:

```bash
git log --format='%an  %ad  %s' --date=short -- docs/importado/
```

> **Qué NO está acá:** los quince documentos de esa rama que eran copias exactas de
> las versiones viejas de `docs/01` a `docs/15`. Los de [`docs/`](../) son
> posteriores y los reemplazan. Descartarlos fue la única forma de que no existan
> dos versiones del mismo texto contradiciéndose.

> **Alcance del gate de calidad:** esta carpeta está excluida en `owned-paths.txt`
> con la línea `!docs/importado/**`, así que el gate la reporta como informativa pero
> nunca bloquea por ella. Es material de otra fuente y otro estilo de redacción;
> forzarlo al diccionario rioplatense sería reescribirlo entero.
>
> La exclusión es explícita porque la carpeta vive adentro de `docs/`, y `docs/**`
> sí es nuestro: sin esa línea, el anidado volvería propio material ajeno.

## `especificacion-tecnica/`

Ocho documentos de especificación con diagramas Mermaid, pensados como material de
defensa técnica.

| # | Documento |
|---|---|
| 01 | [Alcance y los 5 roles de IA](especificacion-tecnica/01_ALCANCE_Y_ROLES_DE_IA.md) |
| 02 | [Arquitectura híbrida y plataforma](especificacion-tecnica/02_ARQUITECTURA_HIBRIDA_Y_PLATAFORMA.md) |
| 03 | [Pipeline de seguridad anti-jailbreak y AST](especificacion-tecnica/03_PIPELINE_SEGURIDAD_ANTI_JAILBREAK_Y_AST.md) |
| 04 | [Evaluación analítica, scoring híbrido y LLMOps](especificacion-tecnica/04_EVALUACION_ANALITICA_SCORING_HIBRIDO_Y_LLMOPS.md) |
| 05 | [Catálogo de modelos, costos y FinOps](especificacion-tecnica/05_CATALOGO_MODELOS_COSTOS_Y_FINOPS.md) |
| 06 | [Persistencia, DDL e inmutabilidad](especificacion-tecnica/06_PERSISTENCIA_DDL_E_INMUTABILIDAD.md) |
| 07 | [Registro de decisiones (ADR)](especificacion-tecnica/07_REGISTRO_DE_DECISIONES_ADR.md) |
| 08 | [Glosario, metadata y contratos de cátedra](especificacion-tecnica/08_GLOSARIO_METADATA_Y_CONTRATOS_CATEDRA.md) |

## `planes-de-ejecucion/`

Nueve planes técnicos, uno por punto normativo de la Sección 15 del PRD. Cada uno
baja un requerimiento a diseño concreto antes de tocar código.
Empezá por el [índice maestro](planes-de-ejecucion/00_INDICE_MAESTRO.md).

## `investigacion-jailbreak/`

Compendio de investigación sobre inyección de prompts y jailbreak, a partir de tres
informes de IBM. El último documento, [matriz de aplicación al sistema
local](investigacion-jailbreak/06_MATRIZ_DE_APLICACION_AL_SISTEMA_LOCAL.md), es el
que traduce todo eso a decisiones de ingeniería nuestras.
Empezá por el [índice](investigacion-jailbreak/00_INDICE_ANALISIS_IBM.md).
