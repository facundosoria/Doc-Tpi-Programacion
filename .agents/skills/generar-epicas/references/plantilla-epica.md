# Plantilla — Épica (Wiki / backlog de Taiga)

> Template oficial de Épica. Uso **obligatorio** para uniformidad del backlog.
>
> Copiar desde `# GXX — …` al crear la épica en el módulo **Epics** de Taiga.
> Reemplazar `GXX` por el número de grupo/equipo; borrar los textos de ejemplo antes de
> publicar. El identificador `EP-0X` **no** va en el título: vive en el catálogo
> (`docs/epicas/README.md`) y en el nombre de archivo.
>
> **Formato fijo de la ficha:**
> - Encabezado `# GXX — <título>` (sin corchetes, sin prefijo `EP-0X:`).
> - **Sin blockquote de encabezado**: después del título va directo un `---` y la
>   primera sección — la ficha no se presenta ni apunta a otro doc, se entiende sola.
> - Un separador `---` en línea propia entre cada sección (también entre el encabezado y
>   la primera sección).
> - Los Criterios de Aceptación como lista de tildar `- [ ]`.
>
> **Qué NO lleva la ficha de épica:**
> - No usa `Como / Quiero / Para` ni escenarios BDD — eso es exclusivo de las Historias
>   de Usuario.
> - No se estima en puntos Fibonacci ni se compromete a un sprint.
> - No lleva prioridad **MoSCoW** ni checklist **INVEST**: son criterios de Historia de
>   Usuario, no de épica. La épica se cierra cuando **todas sus historias** pasan la
>   Definition of Done.
> - No lleva jerga técnica en ninguna sección (no solo en el Objetivo): sin nombres de
>   paquete/clase, headers HTTP, versiones de framework, RFCs ni rutas de archivo —
>   importa el efecto, no la implementación. Ese detalle sigue viviendo en la
>   documentación técnica del servicio.
> - No cita documentos del repo dentro del cuerpo (`doc NN`, rutas `docs/...`): la
>   ficha no referencia ningún otro doc, ni siquiera al catálogo.
>
> El catálogo (pareja / equipo líder, sprints donde aporta, fase y requisitos que
> cubre) vive **fuera de la ficha**, en el índice de épicas (`docs/epicas/README.md`) o
> en el plan de sprints. Si un dato de la ficha no coincide con el catálogo, **manda el
> catálogo**.
>
> **Al pegar en Taiga:** los Criterios van **una línea por ítem y sin `code` inline** —
> el renderer de Taiga en modo lectura descoloca las tildas si el ítem trae texto en
> `code` o listas anidadas. En el repo (GitHub) el detalle largo con sub-viñetas y
> backticks se ve bien.

---

# GXX — [TÍTULO DEL ÉPICO]

---

## Objetivo

[1–2 líneas: qué valor de negocio / usuario entrega la épica, en términos observables
—específico y medible por los Criterios de Aceptación de abajo—. No describir la
solución técnica, los componentes internos ni los pasos de implementación.]

---

## Suposiciones y Restricciones

- **Suposiciones:** [lo que se da por cierto para que la épica tenga sentido — en
  lenguaje llano, el efecto o la regla, no el mecanismo interno]
- **Restricciones (legales / técnicas):** [límites que acotan cómo puede resolverse —
  igual: sin nombres de header, framework, RFC ni tabla/columna]

---

## Criterios de Aceptación a nivel Épico

- [ ] El conjunto mínimo de historias permite [flujo extremo a extremo, nombrado].
- [ ] KPIs iniciales alcanzan: [valores] *(o «a definir» si el equipo aún no los fijó)*.
- [ ] Sin regresiones críticas en [áreas / sistemas que la épica toca].
- [ ] Observabilidad y alertas configuradas (logs, métricas, trazas) donde aplique.
- [ ] Documentación de uso y operación publicada.

> Ajustar la lista a la épica concreta: borrar lo que no aplique, no dejar ítems de
> relleno. Preferir pocos criterios pero esenciales, en lenguaje llano (el efecto
> observable, no el mecanismo interno) — no hace falta agotar cada métrica posible.

---

## Dependencias / Impactos

- **Servicios / APIs:** [listar por función, no por nombre de librería/producto]
- **Módulos afectados:** [listar en lenguaje llano: qué parte propia se toca]
- **Otros equipos:** [listar — qué necesitan de nosotros o nosotros de ellos]
- **Impacto en datos / migraciones:** [detallar]
- **Feature toggles / flags:** [sí / no, plan de retiro]
