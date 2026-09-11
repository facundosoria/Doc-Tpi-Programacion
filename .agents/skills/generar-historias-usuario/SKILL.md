---
name: generar-historias-usuario
description: >-
  Genera Historias de Usuario en el formato oficial de HU de la Wiki de Taiga
  (Como/Quiero/Para, Notas, Criterios de Aceptación con negativos, BDD ≥ 3
  escenarios, Prototipo, Estimación, Dependencias), a partir de la receta de
  sprint / requisitos / contrato que aporta el equipo. El desglose de cada
  historia en tareas lo hace el skill `generar-tareas`.
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

**Estilo por defecto: lenguaje simple.** La ficha lleva la **estructura del template de
Taiga** pero contada **sin jerga técnica** (para alguien que nunca programó): el resultado
esperado, resuelto de punta a punta en los dos estilos, está en
[`references/ejemplo-historia.md`](references/ejemplo-historia.md). El estilo con jerga
precisa (endpoints, códigos HTTP, nombres exactos) es una **variante bajo pedido** — ver
[`references/referencia-estilo-tecnico.md`](references/referencia-estilo-tecnico.md).

Lee primero, en este orden:

1. [`references/guia-metodo.md`](references/guia-metodo.md) — el método de cátedra
   condensado (3C, COMO/QUIERO/PARA, BDD, INVEST, épica/historia/tarea, Planning Poker).
2. [`references/dor-dod.md`](references/dor-dod.md) — las dos compuertas: qué exige la
   DoR para comprometer una historia y qué exige la DoD para aceptarla.
3. [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md)
   — el template en blanco (lenguaje simple) que hay que rellenar.
4. [`references/referencia-estilo-tecnico.md`](references/referencia-estilo-tecnico.md)
   — glosario de traducción y los cambios de rótulo para la variante técnica.
5. [`references/ejemplo-historia.md`](references/ejemplo-historia.md) — una historia
   resuelta de punta a punta en los dos estilos.

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
4. **Redactar cada apartado del template en lenguaje simple** (detalle en
   `guia-metodo.md`; plantilla y rótulos en `plantilla-historia-usuario.md`):
   - **Título**: en palabras, identifica la historia sin leer el detalle.
   - **Como / Quiero / Para**: rol real en palabras · acción del usuario (no solución
     técnica) · beneficio real (no repetir la acción).
   - **Notas / Observaciones**: la *Conversación* — «Reglas de trabajo», «Cómo se
     controla», «Qué tiene que incluir sí o sí», tiempos, seguridad, accesibilidad.
     Qué debe cumplirse, nunca cómo se programa. Traducí los términos técnicos
     (`referencia-estilo-tecnico.md` trae el glosario).
   - **Criterios de Aceptación (CA)**: lista de tildar (`- [ ] **CAn:** …`); condiciones
     concretas y verificables; **incluí siempre ≥ 2 casos** rotulados **«(caso que debe
     fallar)»**. Al pegar en Taiga van una línea por ítem, sin `code` inline.
   - **BDD (≥ 3 escenarios)**: encabezá con **«Qué se prueba:»**. **1 camino esperado +
     al menos 2 que deben fallar.** Cada escenario con Título, Dado, Cuando, Entonces (y
     opcional Y). El **Entonces es observable** (pantalla, mensaje o dato guardado). El
     **Y hereda** el paso que lo precede.
   - **Prototipo**: boceto simple de las pantallas principales (o «no aplica» + motivo si
     es una pieza interna). Cada campo/botón del boceto aparece en algún escenario.
   - **Estimación / Prioridad**: en **bullets**, no tabla. Abrí con el **«Formato
     rápido»** que deja explícitas las dos escalas: **Puntos (Fibonacci): [1/2/3/5/8/13]**
     y **Prioridad (MoSCoW / numérica): [Must/Should/Could/Won't] o [1..5]**. Si aún no
     hubo Planning Poker, dejá los corchetes con la escala y el valor como pendiente —
     **no lo inventes**. Debajo, en palabras: «Puntos de esfuerzo: se asignan en el
     Sprint 0…», «Prioridad: imprescindible (Must) / deseable (Should) / se puede
     posponer (Could) / fuera de este sprint (Won't)».
   - **Dependencias / Impactos**: «Partes involucradas», «Otros equipos / aprobaciones»,
     «Impacto en los datos», «Riesgos».
5. **Mapear a épica** (si te dieron el catálogo). Cada historia = una épica.
6. **Encabezado + índice + contexto.** El documento abre con el bloque «Qué es / Qué NO
   es», el glosario corto de términos repetidos, la sección «Antes de las fichas» (2–3
   párrafos que ubican al lector + la demo en una frase), y el índice (# · Título · Tipo
   · Grupo · Pareja · Depende de · Trabajo). Cierra con «En resumen».
   - **Formato de cada ficha:** encabezado `# Sxx-Hyy — <título>` (el ID interno es la
     ancla del índice; el título de Taiga es `GXX — <título>`, sin corchetes ni ID). Un
     separador `---` en línea propia **entre cada sección** `##` (y entre el bloque de
     metadatos y la primera sección); los `### Escenario N` del BDD no se separan.
7. **Verificá DoR** de cada historia contra `references/dor-dod.md`. Lo que no se cumpla
   se marca como pendiente de Refinamiento/Sprint 0, no se maquilla.
8. **Autocontrol** con la checklist de abajo.

## Salida

Un documento `docs/historias/sXX.md` con esta estructura exacta (lenguaje simple por
defecto). Si el proyecto agrupa sus historias **por épica** en vez de por sprint, el
mismo contenido va a `docs/historias/ep-0X.md` y la vista de sprint (índice + demo) a
`docs/sprints/`.

```markdown
# Historias de usuario — Sprint N, explicadas en palabras simples

> **Qué es este documento.** Las historias de SN con la **misma estructura del template
> de Taiga** (Como/Quiero/Para, Notas, Criterios de Aceptación, escenarios BDD,
> Prototipo, Estimación y Dependencias), contadas **sin jerga técnica**.
>
> **Qué NO es.** No es fuente de verdad para planificar. Si un dato no coincide:
>
> | Dato | Dónde manda |
> |---|---|
> | ID, grupo, pareja, dependencias, horas | `docs/backlog/backlog-ejecutable.md` · «SN» |
> | Requisitos para empezar y para dar por terminada una historia | `docs/dor-dod.md` |
>
> **Sobre los códigos raros.** `EP-01`, `P1`, `Sxx-Hyy`, `RF-*` son etiquetas internas
> del equipo para rastrear cada cosa. Se dejan porque son parte del formato de Taiga; no
> hace falta entenderlas.
>
> **Palabras que se repiten:** [glosario corto de 3–5 términos del dominio traducidos —
> «Sprint», «Demo», «Escenarios (BDD)», y el/los término(s) de plataforma que aparezcan,
> p. ej. «Recepción central» = la puerta única por la que entran los pedidos].

## Antes de las fichas: ¿de qué va todo esto?

[2–3 párrafos que ubican al lector: qué producto es, qué se construye en este sprint, y
la demo final en una frase.]

## Índice

| # | Título | Tipo | Grupo | Pareja | Depende de | Trabajo |
|---|---|---|---|---|---|--:|
| [H01](#sxx-h01--título) | … | Tarea interna / Historia de valor | EP-0X | P_ | — | N h |
| … | | | | | **Total** | **N h** |

**Tipo:** «historia de valor» = la protagoniza una persona real que nota el beneficio.
«tarea interna» = un cimiento del que depende el resto, que ningún usuario final vive.

---

# Sxx-H01 — <Título en palabras simples>
<... ficha completa según plantilla-historia-usuario.md ...>

---

## En resumen

[Lista de lo que queda funcionando al final del sprint, en una línea por historia.]
```

- Cada `# Sxx-Hyy — Título` sigue [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md)
  (lista de metadatos de 6 ítems con rótulos simples, Descripción, Notas, CA con «(caso
  que debe fallar)», BDD con «Qué se prueba», Prototipo, Estimación en bullets con el
  «Formato rápido» de escalas —Puntos Fibonacci y Prioridad MoSCoW/numérica—,
  Dependencias). El índice usa anclas a cada sección.
- El desglose en **tareas SMART** de cada historia es un documento aparte
  ([`generar-tareas`](../generar-tareas/SKILL.md)); **no** va en la ficha.
- Opcional: una fila por historia para la tabla compacta del backlog/plan.

### Variante técnica (bajo pedido)

Si el equipo pide la versión con jerga precisa, generá `sXX-tecnico.md` aplicando los
cambios de [`references/referencia-estilo-tecnico.md`](references/referencia-estilo-tecnico.md):
rótulos técnicos (Reglas de negocio, Validaciones, Datos obligatorios, Característica),
términos exactos (Gateway, `403`, `Idempotency-Key`, golden set), endpoints explícitos,
Estimación como tabla `| Puntos (Fibonacci) | Prioridad (MoSCoW / 1..5) |`. Mismo
contenido y mismos escenarios.

Regla de encabezado: la ficha es **formato de presentación**, no fuente de verdad de
planificación. Si un dato (ID, épica, horas, dependencias) no coincide con la
receta/plan del equipo, **manda la receta/plan**.

## Reglas de oro

- Encabezado de ficha `# Sxx-Hyy — <título>` (título de Taiga `GXX — <título>`, sin
  corchetes ni ID). Separador `---` entre cada sección `##`. CA como lista `- [ ]`.
  Al pegar en Taiga: criterios en una línea, sin `code` inline.
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

- [ ] Estilo **lenguaje simple** por defecto: sin jerga en la prosa; términos técnicos
      traducidos; los códigos internos se dejan pero no hacen falta para seguir la ficha.
- [ ] Documento con encabezado (qué es / qué no es), glosario corto, sección «Antes de
      las fichas» con la demo en una frase, índice con anclas, y «En resumen» al final.
- [ ] Cada ficha abre con la lista de metadatos de 6 ítems (Grupo de trabajo, Pareja a
      cargo, Depende de, Trabajo estimado, Tipo, Responsable del producto), en viñetas
      `- **Rótulo:** valor` — **no** en tabla (el renderer de Taiga rompe las tablas sin
      encabezado).
- [ ] Cada historia: un rol, una acción, un resultado observable (título sin «y/o»).
- [ ] COMO = rol real en palabras (o marcada «Tarea interna» si es habilitador).
- [ ] QUIERO = acción del usuario, no solución técnica. PARA = beneficio real.
- [ ] Notas con los rótulos simples («Reglas de trabajo», «Cómo se controla», «Qué tiene
      que incluir sí o sí»…), sin nombres de tablas ni clases.
- [ ] Encabezado `# Sxx-Hyy — <título>`; `---` entre cada sección `##` (no entre los
      `### Escenario N`).
- [ ] CA como lista de tildar `- [ ]`; verificables; ≥ 2 rotulados «(caso que debe fallar)».
- [ ] BDD: encabezado «Qué se prueba:»; ≥ 3 escenarios (1 esperado + ≥ 2 que fallan);
      cada Entonces observable; cada Y hereda su paso.
- [ ] Ningún escenario se repite casi igual en dos historias (INVEST · I).
- [ ] Prototipo adjunto o justificado; campos del boceto cubiertos por escenarios.
- [ ] Estimación en **bullets** con el «Formato rápido» visible: **Puntos (Fibonacci):
      [1/2/3/5/8/13]** y **Prioridad (MoSCoW / numérica): [Must/Should/Could/Won't] o
      [1..5]**; valor real puesto o marcado pendiente de Sprint 0 (nunca inventado).
- [ ] Cada historia mapeada a una épica / grupo (si hay catálogo).
- [ ] El desglose SMART de cada historia se genera aparte con `generar-tareas` (no va en
      la ficha).
- [ ] DoR revisada historia por historia; huecos marcados, no ocultados.

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/guia-metodo.md`](references/guia-metodo.md) | Método de cátedra condensado: 3C, COMO/QUIERO/PARA, BDD (regla del camino feliz + fallos, escenario de 5 partes, 2 reglas), INVEST, épica/historia/tarea, historia canónica, Planning Poker, priorización. |
| [`references/dor-dod.md`](references/dor-dod.md) | Definition of Ready y Definition of Done: las dos compuertas de cada historia y del incremento. |
| [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md) | Template en blanco, lenguaje simple (estilo por defecto). Copiar y completar. |
| [`references/referencia-estilo-tecnico.md`](references/referencia-estilo-tecnico.md) | Glosario de traducción simple↔técnico y los cambios de rótulo para la variante técnica. |
| [`references/ejemplo-historia.md`](references/ejemplo-historia.md) | Una historia resuelta en los dos estilos, con notas de por qué queda así. |

## Relación con épicas y tareas

El catálogo de épicas al que se mapea cada historia lo produce el skill
**`generar-epicas`**. Si no hay catálogo todavía, generá las historias igual y dejá la
columna «Épica» como `*(a asignar)*`.

El desglose de cada historia en **tareas SMART** lo hace el skill **`generar-tareas`**,
que toma la historia con sus CA y su BDD. El pipeline completo es: épica → historia →
tarea.
