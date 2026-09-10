---
name: generar-epicas
description: >-
  Genera épicas de producto en el formato oficial de Épica de la Wiki de Taiga
  (Objetivo / Suposiciones y Restricciones / Criterios de Aceptación a nivel
  épico / Dependencias e Impactos), una épica por archivo, a partir del alcance
  o catálogo que aporta el equipo. Úsalo cuando pidan "armar las épicas",
  "generar el catálogo de épicas", "pasar el alcance a épicas de Taiga",
  "fichas de épica" o "EP-01…EP-NN". No estima épicas ni las escribe en
  Como/Quiero/Para: eso es de las historias (ver skill generar-historias-usuario).
---

# Generar épicas (formato Taiga)

Convierte el **alcance de un producto** en un conjunto de **épicas** redactadas con el
template oficial de Épica de la Wiki de Taiga. El skill es **autónomo y reutilizable**:
no depende de la documentación de ningún proyecto. Todo el contexto del producto lo
aporta quien lo invoca (ver «Entradas que necesito»).

## Qué es una épica y qué NO es

- **Es** una funcionalidad grande que **no entra en un sprint**. Se divide en historias
  de usuario a medida que se acerca su construcción. Agrupa historias por *para qué
  sirven en el producto* (el **sprint** dice *cuándo* se construye cada historia; la
  **épica**, *para qué*).
- **No se estima** en puntos Fibonacci **ni se compromete** a un sprint. Se **cierra**
  cuando **todas sus historias** cumplen la Definition of Done.
- El template oficial de épica **no** usa `Como / Quiero / Para` ni escenarios BDD —eso
  es exclusivo de las Historias de Usuario—. La épica se describe por su **Objetivo**
  (valor de negocio/usuario) y sus **Criterios de Aceptación a nivel épico** (el cierre
  observable del conjunto).
- **MoSCoW, INVEST y puntos Fibonacci son criterios de Historia de Usuario, no de
  épica.** No van en la ficha. La priorización a nivel épica es **fase + sprint** y vive
  en el catálogo, no en la ficha.
- Cada historia `Sxx-Hyy` pertenece a **una sola** épica.

## Entradas que necesito (las pide el equipo al invocar)

Mínimo imprescindible:

1. **Catálogo o alcance**: lista de las capacidades grandes del producto. Puede venir
   como tabla ya armada (nombre + «resultado que habilita») o como texto de alcance /
   fases / alcance por release del que hay que **derivar** las épicas.
2. **Prefijo y numeración**: cómo se nombran (`EP-01`, `EPIC-1`, …) y el número de
   grupo/equipo para el título de Taiga (`GXX`), si aplica.

Opcional pero mejora el resultado:

3. **Requisitos trazables** (`RF-*`, `PAR-*`, RNF) para citar de forma orientativa.
4. **Equipos/parejas responsables** y **sprints** en que vive cada épica (contexto de
   planificación, va fuera de la ficha).
5. **Dependencias conocidas** entre servicios, equipos o datos.
6. **Restricciones** legales, técnicas o académicas.

Si falta el punto 1 o 2, **pídelos antes de generar**. Si faltan los opcionales, generá
igual y marcá los huecos como `*(a completar)*` — no inventes requisitos, KPIs ni
dependencias.

## Método (paso a paso)

1. **Identificar las épicas.** Si te dan una tabla, respetala. Si te dan alcance en
   prosa: buscá los **grandes resultados de producto** (no acciones sueltas de un
   usuario: eso son historias). Regla de corte: si dos candidatas se cierran con la
   misma evidencia o una no tiene sentido de producto sin la otra, son **una** épica.
   Apuntá a un número manejable (típico 6–12 para un producto entero).
2. **Redactar el _Objetivo_.** 1–2 líneas (que quepan en ~2 renglones al renderizar):
   **qué valor** entrega la épica al negocio o al usuario, en términos observables
   —específico y medible por los Criterios de Aceptación de la ficha—. Si te dieron la
   columna «resultado que habilita», esa frase **es** el objetivo (puliéndola). Nada de
   solución técnica: sin nombres de componentes internos (`AI Gateway`, `outbox`,
   `OpenAPI`…), sin pasos de implementación, sin enumerar módulos. Una sola oración
   preferentemente; si son dos, cortas.
3. **Suposiciones y Restricciones.** Suposiciones = lo que se da por cierto para que la
   épica tenga sentido. Restricciones = límites legales/técnicos/académicos que acotan
   *cómo* puede resolverse. Vienen de las entradas 5 y 6.
4. **Criterios de Aceptación a nivel épico.** Redactá el **cierre observable del
   conjunto**, no de una historia:
   - El conjunto mínimo de historias permite un **flujo extremo a extremo** (nómbralo).
   - KPIs / umbrales iniciales alcanzados (si el equipo los dio; si no, `*(a definir)*`).
   - Sin regresiones críticas en las áreas/sistemas que toca.
   - Observabilidad y alertas configuradas (logs, métricas, trazas) donde aplique.
   - Documentación de uso y operación publicada.
   Ajustá esta lista a la épica: borrá lo que no aplique, no dejes ítems de relleno.
5. **Dependencias / Impactos.** Servicios/APIs, módulos afectados, otros equipos,
   impacto en datos/migraciones, feature flags (sí/no + plan de retiro). Arrancá de la
   entrada 5; lo que no sepas va como `*(a confirmar)*`.
6. **Un archivo por épica**, bajo `docs/epicas/`. Nombre `docs/epicas/ep-01.md`,
   `ep-02.md`, … Sigue exactamente [`references/plantilla-epica.md`](references/plantilla-epica.md):
   una línea de encabezado `# [GXX] — EP-0X: <NOMBRE>`, un blockquote corto que apunta al
   catálogo como fuente de verdad, y las **cuatro secciones** — Objetivo, Suposiciones y
   Restricciones, Criterios de Aceptación a nivel Épico, Dependencias / Impactos. **Sin
   bloque `| Campo | Valor |`**: pareja, sprints, fase y requisitos **no** van en la
   ficha (ver paso 7).
7. **Índice / catálogo.** Generá además `docs/epicas/README.md` con la tabla resumen —
   Épica · Ficha · Nombre · **Fase** · Pareja líder · Sprints · Requisitos (orientativo)—.
   Este catálogo es **el único lugar** donde viven pareja/sprints/fase/requisitos. Dejá
   escrito que **si un dato de la ficha no coincide con el catálogo/plan, manda el
   catálogo** (`docs/backlog/backlog-ejecutable.md`).
8. **Autocontrol** con la checklist de abajo antes de entregar.

## Reglas de oro

- Sin `Como/Quiero/Para`, sin BDD, sin puntos, sin sprint comprometido, **sin MoSCoW,
  sin INVEST** en la ficha (todo eso es de las Historias de Usuario).
- El objetivo describe **valor observable**, no implementación, y entra en ~2 renglones.
- Los CA a nivel épico son del **conjunto** (flujo e2e, KPIs, no-regresión), no
  criterios de una historia.
- La ficha no lleva bloque de metadata: pareja/sprints/fase/requisitos van en el catálogo.
- No inventes KPIs, requisitos ni dependencias: lo desconocido se marca, no se rellena.
- Numeración y prefijo estables: una épica no se renumera; si se descarta, su número no
  se reutiliza.

## Checklist antes de entregar

- [ ] Cada épica tiene Objetivo en 1–2 líneas (~2 renglones), en términos de valor
      observable, sin nombres de componentes internos ni pasos de implementación.
- [ ] Ninguna ficha usa Como/Quiero/Para ni escenarios BDD.
- [ ] Ninguna ficha trae estimación en puntos, prioridad MoSCoW, checklist INVEST ni
      «se compromete en Sxx».
- [ ] Ninguna ficha tiene bloque `| Campo | Valor |`: solo heading + blockquote + 4 secciones.
- [ ] Los CA a nivel épico describen el cierre del conjunto (incluye un flujo e2e).
- [ ] Suposiciones y Restricciones separadas y concretas.
- [ ] Dependencias / Impactos con dueño o marcadas `*(a confirmar)*`.
- [ ] Un archivo por épica + README con el catálogo (con columna **Fase**) y la nota de
      «fuente que manda».
- [ ] Prefijo y numeración consistentes; títulos con `GXX` si el equipo lo pidió.

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/plantilla-epica.md`](references/plantilla-epica.md) | Template oficial en blanco. Copiar y completar. |
| [`references/ejemplo-epica.md`](references/ejemplo-epica.md) | Épica resuelta de punta a punta como referencia de tono y nivel de detalle. |

## Relación con historias de usuario

Este skill deja el **catálogo de épicas**. El desglose de cada épica en historias se
hace con el skill **`generar-historias-usuario`**, que mapea cada historia `Sxx-Hyy` a
una épica de este catálogo.
