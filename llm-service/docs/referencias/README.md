# Referencias bibliográficas — épicas, historias de usuario y tareas

Notas propias (resumidas, no transcripción) de dos libros de referencia sobre
requisitos ágiles, guardadas acá para tenerlas a mano al escribir o revisar
épicas, historias de usuario (HU) y tareas del proyecto — coherente con las
plantillas de [`docs/plantillas/`](../plantillas/) y con el método SMART de
[`29-guia-catedra-historias-de-usuario.md`](../29-guia-catedra-historias-de-usuario.md).

No son normativa de la cátedra ni reemplazan ningún doc de `docs/00`…`docs/36`:
son bibliografía general de la disciplina que un doc de cátedra puede citar o
no. Ante cualquier conflicto entre estas notas y un doc de cátedra o una
plantilla del repo, **gana el doc/plantilla del repo**.

| Documento | Fuente | Qué aporta |
|---|---|---|
| [`historias-de-usuario-scrum-manager.md`](historias-de-usuario-scrum-manager.md) | *Historias de usuario* v5.0, Scrum Manager (2026) | Visión general en español: jerarquía épica→tema→HU→tarea, anatomía de HU, INVEST, criterios de aceptación (gherkin), DoD, priorización (MoSCoW/WSJF), estimación (Fibonacci), 10 estrategias de división + SPIDR, historias técnicas y deuda técnica, user story mapping, uso y riesgos de IA. |
| [`user-stories-applied-cohn.md`](user-stories-applied-cohn.md) | *User Stories Applied*, Mike Cohn (2004) | El origen de INVEST y de varias técnicas que Scrum Manager retoma, con más profundidad en: cómo escribir una historia (independiente/negociable/valiosa/estimable/pequeña/comprobable con ejemplos), historia compuesta vs. compleja, *user role modeling*, guías de redacción ("slice the cake", "write closed stories", constraints), desagregación de HU en tareas, en qué se diferencia una HU de un caso de uso / requisito IEEE 830 / escenario de interacción, catálogo de "olores" (*story smells*) que delatan un backlog mal armado, y cómo tratar NFR y bugs como historias.

## Cómo usarlas al escribir una épica / HU / tarea

- **Épica** → confirmar que cumple el patrón de la plantilla
  ([`epica-taiga.md`](../plantillas/epica-taiga.md)) y que de verdad es
  "demasiado grande para un sprint" (Scrum Manager, §Épicas) o un "compound/complex
  story" (Cohn, cap. 2) — si no, es una HU disfrazada de épica.
- **Historia de usuario** → repasar INVEST antes de darla por cerrada; si falla
  en *Small* o *Independent*, mirar las 10 estrategias de Verwijs / SPIDR
  (Scrum Manager) o "Slice the Cake" / "Split compound vs. complex" (Cohn) para
  dividirla.
- **Tarea** → usar "Disaggregating into Tasks" (Cohn, cap. 10): la tarea
  describe el *cómo* construir, no el *qué*; una HU no necesita una tarea por
  cada detalle, solo las suficientes para repartir el trabajo y no olvidar
  pasos (tests, documentación, etc.).
- **Antes de descartar una idea por "no es una buena historia"** → revisar el
  catálogo de *story smells* (Cohn, cap. 14) — puede ser un síntoma de HU mal
  cortada, no de que la idea esté mal.
