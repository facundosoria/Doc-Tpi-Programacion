---
name: generar-historias-usuario
description: >-
  Genera Historias de Usuario en el formato oficial de HU de la Wiki de Taiga
  (Como/Quiero/Para, Notas, Criterios de Aceptación con negativos, BDD ≥ 3
  escenarios, Prototipo, Estimación, Dependencias), más el desglose en tareas,
  a partir de la receta de sprint / requisitos / contrato que aporta el equipo.
  Aplica el método de cátedra (3C, INVEST, camino feliz + 2 negativos, historia
  canónica, Planning Poker) y las compuertas DoR/DoD. Úsalo cuando pidan
  "armar las historias del sprint", "pasar la receta a HU de Taiga", "fichas
  largas de HU", "historias Sxx" o "desglosar una épica en historias".
---

# Generar historias de usuario (formato Taiga)

Convierte una **receta de sprint / lista de requisitos / contrato** en un conjunto de
**Historias de Usuario** con el template oficial de la Wiki de Taiga y el método que la
cátedra evalúa. El skill es **autónomo y reutilizable**: no depende de la documentación
de ningún proyecto. Todo el contexto lo aporta quien lo invoca (ver «Entradas que
necesito»).

Lee primero, en este orden:

1. [`references/guia-metodo.md`](references/guia-metodo.md) — el método de cátedra
   condensado (3C, COMO/QUIERO/PARA, BDD, INVEST, épica/historia/tarea, Planning Poker).
2. [`references/dor-dod.md`](references/dor-dod.md) — las dos compuertas: qué exige la
   DoR para comprometer una historia y qué exige la DoD para aceptarla.
3. [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md)
   — el template en blanco que hay que rellenar.
4. [`references/ejemplo-historia.md`](references/ejemplo-historia.md) — una historia
   resuelta de punta a punta (ficha larga + tareas).

## Entradas que necesito (las pide el equipo al invocar)

Mínimo imprescindible:

1. **Fuente del alcance del sprint**: la «receta» / lista de paquetes verificables con
   sus horas, o los requisitos `RF-*` del incremento, o el objetivo del sprint + el
   recorrido funcional a demostrar.
2. **Prefijo de ID y sprint**: cómo se numeran (`LLM-S01-H01`, `S3-H12`, …).
3. **Roles reales** del producto (docente, alumno, operador, ADMIN, equipo consumidor…).

Opcional pero mejora el resultado:

4. **Contrato(s)** HTTP/evento que cruzan servicios: endpoints, esquema, auth,
   correlación, idempotencia.
5. **Historia canónica** del equipo (o candidata) para anclar la estimación.
6. **Épicas** a las que mapear cada historia (del skill `generar-epicas`).
7. **Dependencias externas** con dueño y fecha.
8. **Bocetos / wireframes** existentes, o permiso para describir los que faltan.
9. **DoR/DoD propia del equipo** si difiere de la de `references/dor-dod.md`.

Si falta 1, 2 o 3, **pídelos antes de generar**. Si faltan los opcionales: generá igual,
dejá los puntos en `*(a definir en Sprint 0 / Refinamiento)*` y **no inventes** contratos,
estimaciones ni dependencias.

## Método (paso a paso)

1. **Barrido de la fuente.** Recorré la receta/requisitos y subrayá, por cada rol, las
   **acciones distintas** que quiere realizar. Cada acción es una historia candidata
   (método de dos pasos en `guia-metodo.md` §2.1). Un paquete técnico puede dar varias
   historias; varios paquetes chicos pueden fundirse en una.
2. **Clasificar HU de valor vs. tarea de sprint (INVEST · V).**
   - Si el **COMO** es un **rol real que percibe el resultado** → **HU de valor**: se
     carga con el template completo, puntos Fibonacci y todo.
   - Si el enunciado es «Como equipo / Como la plataforma, quiero \<algo técnico\>» y
     ningún usuario percibe el resultado → **tarea de sprint / habilitador**: se puede
     redactar igual en formato largo para trazar los escenarios, pero en el backlog va
     como **tarea** bajo la épica (o bajo una HU habilitadora), **sin** puntos de valor.
   - Marcá el tipo explícitamente en cada ficha.
3. **Cortar bien (INVEST · I, S).** Separá si hay dos verbos de acción, dos roles, una
   parte entregable sin la otra, o más de 6–7 escenarios. Dejá junto si por separado
   nada entrega valor o los escenarios se repetirían. Regla: **un rol, una acción, un
   resultado observable**; si el título necesita «y/o/además», son dos historias.
4. **Redactar cada apartado del template** (detalle en `guia-metodo.md`):
   - **Título**: identifica la historia en el tablero sin leer el detalle.
   - **Como / Quiero / Para**: rol real · acción del usuario (no solución técnica) ·
     beneficio real (no repetir la acción).
   - **Notas / Observaciones**: la *Conversación* — reglas de negocio, validaciones,
     datos obligatorios, performance, seguridad, accesibilidad. Qué debe cumplirse,
     nunca cómo se programa.
   - **Criterios de Aceptación (CA)**: condiciones medibles; **incluí siempre los
     negativos** (no autorizado, entrada inválida, duplicado, dependencia caída).
   - **BDD (≥ 3 escenarios)**: **1 camino feliz + al menos 2 negativos**. Cada escenario
     con Título, Dado, Cuando, Entonces (y opcional Y). El **Entonces es observable**
     (pantalla, mensaje o dato guardado). El **Y hereda** el paso que lo precede.
     Derivá los escenarios recorriendo el boceto/contrato elemento por elemento:
     *¿qué pasa si lo usa bien? ¿y si lo usa mal?*
   - **Prototipo**: boceto de baja fidelidad de las pantallas principales (o «no aplica»
     con motivo si es borde técnico). Cada campo/botón del boceto debe aparecer en algún
     escenario.
   - **Estimación / Prioridad**: **no** asignes puntos Fibonacci vos — dejalos
     `*(a asignar en Sprint 0 con Planning Poker contra la canónica)*`. Prioridad MoSCoW
     sí (del valor/dependencias). Si el equipo planifica en horas, poné la referencia de
     horas del plan como dato separado, sin convertirla a puntos.
   - **Dependencias / Impactos**: servicios, módulos, otros equipos/aprobaciones,
     impacto en datos/migraciones, riesgos y mitigación.
5. **Mapear a épica** (si te dieron el catálogo). Cada historia = una épica.
6. **Desglose en tareas.** Cerrá cada ficha con una tabla de **tareas técnicas** del
   equipo (sin Como/Quiero/Para), en el orden de construcción que use el equipo
   (típico: contrato y amenaza → dominio y migración → caso de uso → adaptadores →
   seguridad y resiliencia → observabilidad → prueba E2E → demo). Cada tarea se parte en
   **una jornada efectiva o menos**. Las horas por tarea son orientativas y suman la
   referencia de la historia.
7. **Índice.** Generá una tabla al inicio del documento: ID · Título · Tipo (HU/tarea) ·
   Épica · Responsable · Dependencias · h. Y una línea con el **criterio de demo** del
   sprint.
8. **Verificá DoR** de cada historia contra `references/dor-dod.md`. Lo que no se cumpla
   se marca como pendiente de Refinamiento/Sprint 0, no se maquilla.
9. **Autocontrol** con la checklist de abajo.

## Salida

Un documento `sXX.md` (p. ej. `s01.md`) con esta estructura exacta:

```markdown
# Historias de usuario — Sprint N (fichas largas)

> **Qué es este documento.** Las historias de SN con el template oficial de Historia de
> Usuario de Taiga (Como/Quiero/Para, Notas, CA con negativos, BDD ≥ 3 escenarios,
> Prototipo, Estimación y Dependencias).
>
> **Qué NO es.** No es fuente de verdad de planificación. Si un dato no coincide:
>
> | Dato | Fuente única |
> |---|---|
> | ID, épica, pareja, dependencias, horas | `backlog/backlog-ejecutable.md` · «SN» |
> | Tipo (HU de valor / habilitador) y demo | `backlog/backlog-ejecutable.md` |
> | DoR / DoD | `dor-dod.md` |
> | Método para redactar y estimar | `metodo/historias-de-usuario.md` |
> | Contrato HTTP y adendas | `contracts/` |
>
> **Título en Taiga.** Cada ficha se carga con `GXX — TÍTULO`. El ID interno `Sxx-Hyy`
> es el del equipo.
>
> **Estimación en puntos.** Ninguna ficha trae puntos Fibonacci: se asignan en el Sprint
> 0 con Planning Poker contra la historia canónica. La columna *h* es la referencia del
> plan y **no se convierte** a puntos.

## Índice

| ID | Título | Tipo | Épica | Pareja | Dep. | h |
|---|---|---|---|---|---|--:|
| [Sxx-H01](#sxx-h01--título) | … | Tarea / HU | EP-0X | P_ | — | N |
| … | | | | | **Total** | **N** |

**Demo de SN:** <una frase con el recorrido que acepta la Review>.

---

# Sxx-H01 — <Título>
<... ficha completa según plantilla-historia-usuario.md ...>
```

- Cada `# Sxx-Hyy — Título` sigue [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md)
  (tabla de metadatos de 7 filas, Descripción, Notas, CA, BDD, Prototipo, Estimación,
  Dependencias, Tareas). El índice usa anclas a cada sección.
- Opcional: una fila por historia para la tabla compacta del backlog/plan.

### Variante en lenguaje simple (opcional)

Si el equipo la pide, generá además `sXX-lenguaje-simple.md`: **las mismas historias, la
misma estructura de secciones**, contadas **sin jerga técnica** (para alguien que nunca
programó). Reglas: mismo contenido y mismos escenarios; se traducen los términos
(«Gateway» → «recepción central», «golden set» → «colección de referencia», `403` → «no
autorizado»); los CA negativos se rotulan «(caso que debe fallar)»; se agrega un glosario
corto al inicio y un «En resumen» al final. No es fuente de verdad: encabezado igual que
el principal.

Regla de encabezado: la ficha es **formato de presentación**, no fuente de verdad de
planificación. Si un dato (ID, épica, horas, dependencias) no coincide con la
receta/plan del equipo, **manda la receta/plan**.

## Reglas de oro

- **1 camino feliz + 2 negativos** como mínimo, siempre.
- El **COMO** nombra un rol real; si es «el equipo/el sistema», es tarea, no HU de valor.
- Las **Notas** dicen *qué*, nunca *cómo se programa*.
- Cada **Entonces** es observable desde afuera.
- **No inventes** puntos de estimación: se fijan en equipo con Planning Poker contra la
  canónica, después de tener los CA.
- **No inventes** contratos, endpoints, KPIs ni dependencias que el equipo no dio.
- Habilitadores técnicos: formato largo permitido para trazar escenarios, pero en Taiga
  van como **tarea**.

## Checklist antes de entregar

- [ ] Documento con encabezado (qué es / qué no es / tabla de fuente única), índice con
      anclas y línea «Demo de SN».
- [ ] Cada ficha abre con la tabla de metadatos de 7 filas (Épica, Pareja, Dependencias,
      Estimación (plan), Tipo, Requisito, Referente de producto).
- [ ] Cada historia: un rol, una acción, un resultado observable (título sin «y/o»).
- [ ] COMO = rol real (o marcada explícitamente como tarea/habilitador).
- [ ] QUIERO = acción del usuario, no solución técnica. PARA = beneficio real.
- [ ] Notas = reglas de negocio / datos / restricciones, sin diseño de pantallas ni
      nombres de tablas.
- [ ] CA medibles e incluyen los negativos.
- [ ] BDD: ≥ 3 escenarios (1 feliz + ≥ 2 negativos); cada uno con Título/Dado/Cuando/
      Entonces; cada Entonces observable; cada Y hereda su paso.
- [ ] Ningún escenario se repite casi igual en dos historias (INVEST · I).
- [ ] Prototipo adjunto o justificado; campos del boceto cubiertos por escenarios.
- [ ] Estimación en puntos = `*(a asignar en Sprint 0)*`; prioridad MoSCoW puesta.
- [ ] Dependencias con dueño/fecha o marcadas `*(a confirmar)*`.
- [ ] Cada historia mapeada a una épica (si hay catálogo).
- [ ] Desglose en tareas técnicas (sin Como/Quiero/Para), tareas ≤ 1 jornada.
- [ ] Índice + criterio de demo del sprint.
- [ ] DoR revisada historia por historia; huecos marcados, no ocultados.

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/guia-metodo.md`](references/guia-metodo.md) | Método de cátedra condensado: 3C, COMO/QUIERO/PARA, BDD (regla del camino feliz + fallos, escenario de 5 partes, 2 reglas), INVEST, épica/historia/tarea, historia canónica, Planning Poker, priorización. |
| [`references/dor-dod.md`](references/dor-dod.md) | Definition of Ready y Definition of Done: las dos compuertas de cada historia y del incremento. |
| [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md) | Template oficial en blanco. Copiar y completar. |
| [`references/ejemplo-historia.md`](references/ejemplo-historia.md) | Historia resuelta de punta a punta (ficha larga + tabla de tareas) con notas de por qué queda así. |

## Relación con épicas

El catálogo de épicas al que se mapea cada historia lo produce el skill
**`generar-epicas`**. Si no hay catálogo todavía, generá las historias igual y dejá la
columna «Épica» como `*(a asignar)*`.
