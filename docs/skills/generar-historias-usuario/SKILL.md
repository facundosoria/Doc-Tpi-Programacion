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

**Estilo por defecto: lenguaje simple.** La ficha lleva la **estructura del template de
Taiga** pero contada **sin jerga técnica** (para alguien que nunca programó): así queda
como [`docs/historias/s01-test.md`](../../historias/s01-test.md) del proyecto de
referencia. El estilo con jerga precisa (endpoints, códigos HTTP, nombres exactos) es una
**variante bajo pedido** — ver [`references/referencia-estilo-tecnico.md`](references/referencia-estilo-tecnico.md).

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
   - **Criterios de Aceptación (CA)**: condiciones concretas y verificables; **incluí
     siempre ≥ 2 casos** rotulados **«(caso que debe fallar)»**.
   - **BDD (≥ 3 escenarios)**: encabezá con **«Qué se prueba:»**. **1 camino esperado +
     al menos 2 que deben fallar.** Cada escenario con Título, Dado, Cuando, Entonces (y
     opcional Y). El **Entonces es observable** (pantalla, mensaje o dato guardado). El
     **Y hereda** el paso que lo precede.
   - **Prototipo**: boceto simple de las pantallas principales (o «no aplica» + motivo si
     es una pieza interna). Cada campo/botón del boceto aparece en algún escenario.
   - **Estimación / Prioridad**: en **bullets**, no tabla. «Puntos de esfuerzo: se
     asignan en el Sprint 0…» (no los inventes). «Prioridad: imprescindible / deseable /
     se puede posponer».
   - **Dependencias / Impactos**: «Partes involucradas», «Otros equipos / aprobaciones»,
     «Impacto en los datos», «Riesgos».
5. **Mapear a épica** (si te dieron el catálogo). Cada historia = una épica.
6. **Desglose en tareas.** Cerrá cada ficha con una tabla de **tareas técnicas** del
   equipo (sin Como/Quiero/Para), en el orden de construcción que use el equipo
   (típico: contrato y amenaza → dominio y migración → caso de uso → adaptadores →
   seguridad y resiliencia → observabilidad → prueba E2E → demo). Cada tarea se parte en
   **una jornada efectiva o menos**. Las horas por tarea son orientativas y suman la
   referencia de la historia.
7. **Encabezado + índice + contexto.** El documento abre con el bloque «Qué es / Qué NO
   es», el glosario corto de términos repetidos, la sección «Antes de las fichas» (2–3
   párrafos que ubican al lector + la demo en una frase), y el índice (# · Título · Tipo
   · Grupo · Pareja · Depende de · Trabajo). Cierra con «En resumen».
8. **Verificá DoR** de cada historia contra `references/dor-dod.md`. Lo que no se cumpla
   se marca como pendiente de Refinamiento/Sprint 0, no se maquilla.
9. **Autocontrol** con la checklist de abajo.

## Salida

Un documento `docs/historias/sXX.md` con esta estructura exacta (lenguaje simple por
defecto):

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
  (tabla de metadatos de 6 filas con rótulos simples, Descripción, Notas, CA con «(caso
  que debe fallar)», BDD con «Qué se prueba», Prototipo, Estimación en bullets,
  Dependencias, Tareas). El índice usa anclas a cada sección.
- Opcional: una fila por historia para la tabla compacta del backlog/plan.

### Variante técnica (bajo pedido)

Si el equipo pide la versión con jerga precisa, generá `sXX-tecnico.md` aplicando los
cambios de [`references/referencia-estilo-tecnico.md`](references/referencia-estilo-tecnico.md):
rótulos técnicos (Reglas de negocio, Validaciones, Datos obligatorios, Característica),
términos exactos (Gateway, `403`, `Idempotency-Key`, golden set), endpoints explícitos,
Estimación como tabla Fibonacci + MoSCoW. Mismo contenido y mismos escenarios.

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

- [ ] Estilo **lenguaje simple** por defecto: sin jerga en la prosa; términos técnicos
      traducidos; los códigos internos se dejan pero no hacen falta para seguir la ficha.
- [ ] Documento con encabezado (qué es / qué no es), glosario corto, sección «Antes de
      las fichas» con la demo en una frase, índice con anclas, y «En resumen» al final.
- [ ] Cada ficha abre con la tabla de metadatos de 6 filas (Grupo de trabajo, Pareja a
      cargo, Depende de, Trabajo estimado, Tipo, Responsable del producto).
- [ ] Cada historia: un rol, una acción, un resultado observable (título sin «y/o»).
- [ ] COMO = rol real en palabras (o marcada «Tarea interna» si es habilitador).
- [ ] QUIERO = acción del usuario, no solución técnica. PARA = beneficio real.
- [ ] Notas con los rótulos simples («Reglas de trabajo», «Cómo se controla», «Qué tiene
      que incluir sí o sí»…), sin nombres de tablas ni clases.
- [ ] CA verificables; ≥ 2 rotulados «(caso que debe fallar)».
- [ ] BDD: encabezado «Qué se prueba:»; ≥ 3 escenarios (1 esperado + ≥ 2 que fallan);
      cada Entonces observable; cada Y hereda su paso.
- [ ] Ningún escenario se repite casi igual en dos historias (INVEST · I).
- [ ] Prototipo adjunto o justificado; campos del boceto cubiertos por escenarios.
- [ ] Estimación en **bullets** («Puntos de esfuerzo: se asignan en el Sprint 0»);
      prioridad puesta.
- [ ] Cada historia mapeada a una épica / grupo (si hay catálogo).
- [ ] Desglose en «Tareas (los pasos técnicos)», tabla `# · Tarea · h`, tareas ≤ 1
      jornada, suman el «Trabajo estimado».
- [ ] DoR revisada historia por historia; huecos marcados, no ocultados.

## Archivos del skill

| Archivo | Para qué |
|---|---|
| [`references/guia-metodo.md`](references/guia-metodo.md) | Método de cátedra condensado: 3C, COMO/QUIERO/PARA, BDD (regla del camino feliz + fallos, escenario de 5 partes, 2 reglas), INVEST, épica/historia/tarea, historia canónica, Planning Poker, priorización. |
| [`references/dor-dod.md`](references/dor-dod.md) | Definition of Ready y Definition of Done: las dos compuertas de cada historia y del incremento. |
| [`references/plantilla-historia-usuario.md`](references/plantilla-historia-usuario.md) | Template en blanco, lenguaje simple (estilo por defecto). Copiar y completar. |
| [`references/referencia-estilo-tecnico.md`](references/referencia-estilo-tecnico.md) | Glosario de traducción simple↔técnico y los cambios de rótulo para la variante técnica. |
| [`references/ejemplo-historia.md`](references/ejemplo-historia.md) | Una historia resuelta en los dos estilos, con notas de por qué queda así. |

## Relación con épicas

El catálogo de épicas al que se mapea cada historia lo produce el skill
**`generar-epicas`**. Si no hay catálogo todavía, generá las historias igual y dejá la
columna «Épica» como `*(a asignar)*`.
