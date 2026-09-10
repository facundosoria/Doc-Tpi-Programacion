# Método para épicas

Material fijo. Cómo se entiende y se redacta una épica en este esquema de trabajo.

## Qué es una épica y qué NO es

- **Es** una funcionalidad grande que **no entra en un sprint**. Se divide en historias
  de usuario a medida que se acerca su construcción. Agrupa historias por *para qué
  sirven en el producto*: el **sprint** dice *cuándo* se construye cada historia; la
  **épica**, *para qué*.
- **No se estima** en puntos Fibonacci **ni se compromete** a un sprint. Se **cierra**
  cuando **todas sus historias** cumplen la Definition of Done.
- El template oficial de épica **no** usa `Como / Quiero / Para` ni escenarios BDD — eso
  es exclusivo de las Historias de Usuario. (Algunas guías generales dicen que una épica
  es «una HU demasiado grande» y por eso admitiría el formato; para la carga en Taiga
  manda el template oficial, que no lo usa.)
- Cada historia `Sxx-Hyy` pertenece a **una sola** épica.

## Apartados de la ficha

| Apartado | Qué poner |
|---|---|
| **Objetivo** | 1–2 líneas: qué **valor** de negocio / usuario entrega la épica, en términos observables. No solución técnica. |
| **Suposiciones y Restricciones** | Suposiciones = lo que se da por cierto para que la épica tenga sentido. Restricciones = límites legales / técnicos / académicos que acotan *cómo* puede resolverse. |
| **Criterios de Aceptación a nivel épico** | El **cierre observable del conjunto**, no de una historia: el conjunto mínimo de historias permite un flujo extremo a extremo (nombrarlo); KPIs / umbrales iniciales; sin regresiones críticas en las áreas que toca; observabilidad y alertas donde aplique; documentación de uso y operación publicada. Ajustar a la épica: borrar lo que no aplique. |
| **Dependencias / Impactos** | Servicios / APIs, módulos afectados, otros equipos, impacto en datos / migraciones, feature flags (sí/no + plan de retiro). |

## De alcance de producto a catálogo de épicas

1. Buscar los **grandes resultados de producto** (no acciones sueltas de un usuario: eso
   son historias).
2. Regla de corte: si dos candidatas se cierran con la misma evidencia, o una no tiene
   sentido de producto sin la otra, son **una** épica. Apuntar a un número manejable
   (típico 6–12 para un producto entero).
3. Para cada épica: nombre, «resultado que habilita» (→ *Objetivo*), pareja/equipo
   responsable, sprints en que vive, requisitos que cubre (orientativo).
4. El **catálogo** (esa tabla) es la fuente; las fichas son su formato de presentación.
   Si un dato de la ficha no coincide con el catálogo, manda el catálogo.

## Reglas de oro

- Objetivo = valor, no implementación.
- Sin Como/Quiero/Para, sin BDD, sin puntos, sin sprint comprometido en la ficha.
- CA a nivel épico = del **conjunto** (flujo e2e, KPIs, no-regresión).
- No inventar KPIs, requisitos ni dependencias: lo desconocido se marca `*(a definir)*`.
- Numeración estable: una épica no se renumera; si se descarta, su número no se reutiliza.
