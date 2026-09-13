# Historias de usuario — notas de Scrum Manager (v5.0, 2026)

> Resumen propio, no transcripción. Fuente: *Historias de usuario*, Formación
> complementaria v5.0, Scrum Manager®, agosto 2026. Ver [`README.md`](README.md)
> para cómo se relaciona con las plantillas del repo.

## 1. Jerarquía: tema → épica → historia → tarea

- **Tema:** colección de épicas e HU relacionadas que describen un sistema o
  subsistema completo (parte de la visión de producto, no una funcionalidad).
- **Épica:** una HU de gran tamaño y alta incertidumbre — una etiqueta para
  algo que no puede completarse en un sprint. Se descompone en HU más chicas
  cerca del momento de implementarla. Puede pensarse como una serie de
  hipótesis (*hypothesis driven development*): "Creemos que [capacidad]
  resultará en [resultado]. Tendremos confianza para proceder cuando [señal
  medible]."
- **Historia de usuario:** unidad de trabajo entregable en un sprint.
- **Tarea:** por debajo de la HU. Describe el *cómo* construir, no el *qué*;
  sale de descomponer la HU en unidades de trabajo gestionables.

Cuanto más prioritario es un elemento, más fino conviene su grano (las épicas
son grano grueso; las tareas, grano fino).

## 2. Anatomía de una historia de usuario

**Campos esenciales:** descripción (`Como [rol], quiero [objetivo], para poder
[beneficio]`), estimación, prioridad.

**Campos opcionales frecuentes:** ID, título, valor de negocio, criterios de
aceptación, requerimientos no funcionales, DoD, dependencias, persona
asignada, sprint, riesgo, módulo, observaciones.

- **Rol:** no es un cargo, es la persona detrás de la necesidad (user
  persona) — para qué se construye.
- **Objetivo:** la intención, no la funcionalidad que se va a usar.
- **Beneficio:** cómo encaja con la visión de producto, el problema de fondo.

**Valor de negocio** y **prioridad** son datos independientes: algo puede
tener valor de negocio nulo para el cliente pero ser de prioridad alta
(ej. infraestructura necesaria para que el resto funcione).

## 3. Estimación

- Escala relativa recomendada: **Fibonacci** (1, 2, 3, 5, 8, 13, 21…) — la
  distancia creciente entre números refleja que la incertidumbre crece más
  rápido que el tamaño. Rompe el sesgo humano de pensar en múltiplos de 2.
- Para elementos grandes (épicas, temas) Fibonacci da falsa precisión → usar
  **tallas de camisa** (S/M/L/XL…), puramente relativas.
- *Planning poker*: el objetivo no es predecir con exactitud sino detectar
  desalineación de conocimiento en el equipo (si las estimaciones difieren
  mucho, alguien sabe algo que otros no).

## 4. Priorización

- **MoSCoW** (Dai Clegg, 1994): Must have / Should have / Could have / Won't
  have (por ahora). Da un significado intrínseco a la prioridad, más útil que
  alta/media/baja.
- **ROI = Valor de negocio / Tamaño** — orden descendente = orden de
  prioridad.
- **WSJF (Weighted Shortest Job First,** Reinertsen 2009): `Coste de demora /
  Tamaño`, donde `Coste de demora = Valor de negocio + Criticidad en el tiempo
  + Reducción de riesgo y valor de oportunidad`. Ventaja: las historias
  técnicas (sin valor de negocio directo) sí tienen coste de demora, así que
  WSJF las puede priorizar contra las funcionales.

## 5. Criterios de aceptación

Se miden con el método **SMART** (Specific, Measurable, Achievable, Relevant,
Time-boxed). Formatos habituales: "Comprobar [criterio]", "Verificar que
cuando [rol] hace [acción] consigue [resultado]", o **gherkin** (BDD):

```
Escenario [n] [título]:
Dado que [contexto] y adicionalmente [contexto],
Cuando [evento],
Entonces [resultado esperado].
```

Elementos del escenario: número, título, contexto, evento, resultado.

## 6. Requerimientos no funcionales (NFR)

Cualidades/restricciones transversales (seguridad, usabilidad, rendimiento,
disponibilidad…) que no se expresan bien como criterio de una sola HU. Se
describen con: nombre (`cualidad.subcualidad`), escala, métrica, objetivo,
restricción (nivel de fallo a evitar) y línea base. Si son persistentes, se
agregan a la DoD; si no, se reflejan como criterio de aceptación puntual.

## 7. Definición de hecho (DoD)

Es un acuerdo de calidad ("¿qué tiene que cumplir X para estar hecho?"),
distinto de los criterios de aceptación (que son por-historia). Aplica en 4
niveles, cada uno acumulando al anterior:

- **Tarea:** implementada, cumple estilo, pruebas unitarias, integrada,
  gestor de tareas actualizado, métricas satisfechas, aprobada por el tester.
- **Historia:** tareas asociadas hechas, código documentado, satisface
  criterios de aceptación, pruebas de integración, cumple NFR, aprobada por
  el PO.
- **Sprint:** historias planificadas hechas, revisión + retrospectiva hechas,
  aprobado por el PO.
- **Release:** todo lo anterior + medios de distribución, documentación de
  usuario, pruebas de seguridad/regresión, comunicación de release.

## 8. INVEST y calidad de HU

**I**ndependent, **N**egotiable, **V**aluable, **E**stimable, **S**mall,
**T**estable (Bill Wake, 2003; popularizado por Mike Cohn — ver
[`user-stories-applied-cohn.md`](user-stories-applied-cohn.md)). Preguntas
de chequeo por atributo (Thomas Wallet) — ejemplo para *Small*: ¿tiene menos
de N criterios de aceptación? ¿ya no tiene sentido subdividir más? ¿no hay
riesgo de que crezca?

Buenas prácticas: describir el *qué*, no el *cómo*; no sobre-detallar;
estimar siempre (no hacerlo genera falsas expectativas); apoyarse en
documentación externa (wiki) sin volcar todo a la tarjeta.

## 9. División de historias grandes

**Horizontal** (por capa técnica) se evita: genera piezas sin valor de
negocio individual y cuellos de botella por especialidad. **Vertical** (por
capa funcional, "una porción de la torta completa") es la preferida.

### Las 10 estrategias de Verwijs (2015, sobre Lawrence 2009)

0. **Spike** — extraer una investigación time-boxed cuando hay incertidumbre
   técnica/funcional (ej. viabilidad de una pasarela de pago).
1. **Pasos de flujo de trabajo** — MVP = primer y último paso; los pasos
   intermedios se agregan después.
2. **Reglas de negocio** — empezar con la regla mínima/simplificada,
   incorporar el resto incrementalmente.
3. **Happy / unhappy flow** — separar el camino ideal de los alternativos
   (errores, excepciones).
4. **Opciones / plataformas de entrada** — por dispositivo, complejidad de
   interfaz, etc.
5. **Tipos de datos o parámetros** — por tipo de búsqueda, tipo de dato
   devuelto, entidades de un DER.
6. **Operaciones (CRUD)** — separar alta/lectura/modificación/baja; "gestionar
   X" suele delatar que hay que aplicar esta estrategia.
7. **Casos/escenarios de test** — cuando es difícil dividir por funcionalidad,
   dividir por los escenarios gherkin derivados de los criterios.
8. **Roles** — cuando varios roles se benefician de forma distinta de la
   misma historia.
9. **Optimizar ahora o más tarde** — "hazlo funcionar" primero, "hazlo rápido"
   después; aplica a NFR como rendimiento, no a atajos de código (deuda
   técnica).
10. **Compatibilidad de navegador** — priorizar el navegador de mayor uso.

### Modelo SPIDR (Mike Cohn) — ~80 % de los casos con 5 técnicas

**S**pike, **P**ath (caminos alternativos), **I**nterface (complejidad de UI
o plataformas), **D**ata (tipos de dato/parámetros), **R**ules (reglas de
negocio). Es iterativo: se puede dividir primero por Path y después cada
resultado por Data. Preguntas guía en orden: ¿incertidumbre? → Spike;
¿múltiples formas de lograr el objetivo? → Path; ¿interfaz con niveles de
complejidad? → Interface; ¿tipos de datos distintos? → Data; ¿reglas de
negocio múltiples? → Rules.

## 10. Otras formas de tomar requisitos (y cuándo NO usar HU)

- **Casos de uso (UML):** especifican el *cómo* con actores, pre/post
  condiciones, flujo principal y alternativos; documentación pesada,
  mantenimiento alto. Útiles en sistemas regulados (salud, aeronáutica),
  integración con legacy o contratos de alcance cerrado.
- **Requisitos funcionales (IEEE 830/29148):** "El sistema deberá…", con ID,
  prioridad, precondiciones/postcondiciones, trazabilidad formal. Necesarios
  cuando hay auditoría/certificación obligatoria.
- La HU es preferible en productos iterativos con feedback continuo; los
  enfoques formales, en contextos regulados o de documentación exhaustiva
  obligatoria. Pueden convivir (capas: épicas formales arriba, HU para
  implementar iterativamente).

## 11. Historias técnicas

Aportan valor indirecto (sostenibilidad, rendimiento, seguridad), no siempre
visible al usuario final. Tipos: **arquitectura**, **infraestructura de
producto**, **infraestructura del equipo**, **refactorización**, **spikes**
(técnico o funcional). Criterios de aceptación técnicos y mensurables (ej.
"cobertura de tests > 80 %"), verificados por audiencia técnica, no por el
PO/usuario final.

**Deuda técnica** (Ward Cunningham, 1992): deliberada (atajo consciente y
excepcional), accidental (falta de conocimiento) o por negligencia (ignorar
buenas prácticas). Gestión: refactor progresivo al tocar código legacy,
reservar % fijo del sprint (referencia: 20-30 % en producto maduro, 10-15 %
en proyecto nuevo con arquitectura sólida, 40-50 % temporal si hay deuda alta
acumulada), priorizar por coste de demora técnico o matriz impacto/esfuerzo.

## 12. User Story Mapping (Jeff Patton, 2014)

Técnica colaborativa para armar el backlog en 2D en vez de una lista plana:
**backbone** (actividades de alto nivel del flujo de usuario) + HU por
actividad, ordenadas por valor de arriba a abajo + versiones (MPV / v2 / v3…)
recortando horizontalmente. Complementa con **impact mapping** (Gojko Adzic:
por qué → quién → cómo → qué, para justificar cada feature contra un
objetivo de negocio) y **Opportunity Solution Trees** (Teresa Torres: outcome
→ oportunidades → soluciones, para *continuous discovery*).

## 13. IA e historias de usuario — postura y riesgos

La IA es apoyo, no reemplazo de la conversación (las 3 Cs de Jeffries: Card,
Conversation, Confirmation siguen siendo el centro). Usos razonables:
generar borradores iniciales (siempre con validación humana), sugerir
mejoras/criterios sobre HU ya escritas, detectar duplicados/gaps/dependencias
en un backlog grande, extraer necesidades de fuentes no estructuradas
(entrevistas, tickets de soporte).

**Riesgos a tener presentes:** la IA no conoce usuarios reales ni el contexto
organizacional (estrategia, restricciones regulatorias, decisiones pasadas);
puede generar HU "perfectas" que inhiben la conversación real; puede producir
historias técnicamente correctas pero estratégicamente irrelevantes;
sobreconfianza/alucinaciones (sugerir integraciones inexistentes, inventar
"mejores prácticas"); atrofia de habilidades si se usa como primera línea en
vez de como revisión de un borrador humano. No introducir datos sensibles
(personales, credenciales, secretos comerciales) en prompts.
