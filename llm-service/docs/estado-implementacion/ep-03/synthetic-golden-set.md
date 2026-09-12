# Generación de casos sintéticos — sin ficha de historia

- **Estado:** 🔴 Placeholder — no genera nada con un LLM
- **Épica tentativa:** EP-03
- **Código:** `SyntheticGoldenSetController` + `SyntheticGoldenSetProposalService`
- **Evidencia:** [`verificacion-v2-golden-set-calibracion.md` · §5](../../entregas/verificacion-v2-golden-set-calibracion.md),
  [`CORRECCIONES-SUGERIDAS.md` ítem 5](../../../llm-service/CORRECCIONES-SUGERIDAS.md)

## Qué hay en el código

`SyntheticGoldenSetProposalService` rota **tres plantillas fijas** por índice (`i % 3`) y setea
el campo `author` como `"Generador Asistido / LLM"` — **sin que ningún LLM las genere**. No es un
error de programación: es un placeholder razonable mientras no existe la integración real. El
riesgo concreto es el nombre del campo `author`: alguien que lo lea en una demo puede creer que
ya hay un modelo generando casos.

## Qué falta

1. Conectar esto a un modelo real (o al fake de [`ep-02/h10.md`](../ep-02/h10.md), cuando exista)
   para que "sintético" signifique generado, no rotado entre 3 plantillas fijas.
2. Hasta entonces, **aclarar explícitamente en cualquier demo o entrega** que esto es un dato de
   muestra, no una función de IA operando — mismo cuidado que
   [`eligible-interactions.md`](../pendiente-de-epica/eligible-interactions.md).
