# Tema 04 — Desafío Teórico — pendientes

> Carpeta nueva (creada 2026-09-13, a partir de una consulta directa de Tema 04 sobre respuestas
> cortas mal corregidas por comparación exacta). No hay agenda de integración previa que citar acá
> — es la primera vez que este equipo queda registrado en `docs/equipos/`.
>
> **Identificación confirmada (2026-09-13):** "Desafío Teórico" es Tema 04. Hasta acá la única
> base escrita era una mención de refilón en
> [`11-glosario-y-metadata.md`](../../../00-gobierno-y-evolucion/03-glosario-y-metadata.md#las-ocho-colisiones) ("Tema 04:
> corregir") y en [`10-entregables-y-plan.md:71`](../../../01-vision-alcance-y-entrega/02-entregables-y-plan.md); queda cerrado.

## 🟡 Si van a implementar la recomendación de normalización + distancia de edición

Les dejamos la técnica en
[`docs/entregas/recomendacion-correccion-respuestas-cortas.md`](../../../01-vision-alcance-y-entrega/03-entregas/recomendacion-correccion-respuestas-cortas.md),
pero queda sin confirmar:

- Si la van a implementar de su lado (no depende de nosotros para hacerlo).
- Si necesitan que los acompañemos a calibrar el umbral de distancia de edición contra ejemplos
  reales de alumnos ya corregidos a mano — eso sí sería colaboración puntual, no un contrato.

## 🔴 Cruzado — si en algún momento piden corrección semántica real

Lo que la nota técnica **no** resuelve (respuesta parcial no anticipada, paráfrasis genuina) exige
reabrir [P-01](../../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md#-p-01--corrector-de-respuestas-abiertas-fuera-de-alcance)
con el Product Owner — requisitos, riesgos, responsable y validación propios, según quedó escrito
en esa decisión. **No es algo que Tema 07 y Tema 04 puedan acordar solos entre sí**, ni algo que
deba resolverse ampliando la nota técnica actual. Si vuelve a aparecer el tema, el primer paso es
llevarlo al PO, no directo a un contrato técnico.

## 🟡 Esta carpeta todavía no tiene entrada en el resto de los documentos madre

A diferencia de los demás equipos, esta relación **no** tiene sección propia en
`18-contratos-inter-equipos.md` ni en
`17-mapa-de-integracion.md`, porque hoy no hay contrato técnico
que documentar ahí — esos documentos son la fuente de verdad de integraciones reales, y esta no lo
es (todavía). Si la relación con Tema 04 crece más allá de la recomendación (por ejemplo, si se
reabre P-01 y sí termina habiendo una llamada a `llm-service`), ahí sí corresponde sumarle una
sección en 18 y actualizar el mapa — no antes.
