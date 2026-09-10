# Plantilla — Receta de sprint

Una receta es una **guía de implementación** de un incremento. Vive en el backlog
ejecutable. Es fuente de: ID de historias, épica, pareja, dependencias y horas.

---

## S__ — [NOMBRE DEL INCREMENTO] (~[N] h estimadas)

**Objetivo:** [qué puede hacer el usuario al terminar el sprint, y **quién** (rol real)].

**Recorrido funcional de la demo:** [paso 1 → paso 2 → paso 3 → … — lo concreto que se
muestra en la Review].

**Límites del incremento:** [qué queda explícitamente afuera y para qué sprint es].

**Versión anterior que debe seguir funcionando:** [ninguna / el incremento de S__].

**No iniciar sin:** [precondiciones y dependencias con dueño — responsables de tal
servicio, definición de tal dato, ambiente integrado, …].

### Paquetes verificables

| Orden | Paquete verificable | h | Salida / prueba |
|---:|---|---:|---|
| 1 | [·] | · | [qué queda hecho y cómo se comprueba] |
| 2 | [·] | · | [·] |
| … | | | |
| | **Total (piso, no tope)** | **[N]** | |

> El total es el **trabajo estimado**: desarrollo, pruebas, revisión, documentación y
> demo. **No** incluye reuniones ni reserva. Comparar con la capacidad comprometible del
> sprint; la diferencia es margen.

### Gates

- [regla que no se salta — p. ej. «no copiar `/x/*` del ejemplo histórico»]
- [«congelar cualquier campo de contrato ausente con el consumidor antes de publicarlo»]

### Aceptación negativa

- [qué NO debe pasar — token sin rol no accede; segunda clave igual no duplica; dato
  inválido se rechaza; caída no pierde datos]

### Historias derivadas (orientativo — se refinan en el Refinamiento)

| ID | Como… / quiero… / para… | Aceptación (incluye negativa) | Tipo | Épica | Pareja | Dep. | h |
|---|---|---|---|---|---|---|--:|
| S__-H01 | [·] | [·] | HU / Tarea | EP-0X | P_ | — | · |
| | | | | | | **Total** | **[N]** |

> Tipo: **HU** si el COMO es un rol real que percibe el resultado; **Tarea** si es un
> habilitador técnico (falla la V de INVEST). Puntos Fibonacci: **no** se pre-cargan
> acá; se asignan en Sprint 0 con Planning Poker contra la historia canónica.

**Demo de S__:** [una frase — el recorrido que acepta la Review].
