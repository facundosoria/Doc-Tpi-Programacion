# Cálculo de capacidad del sprint

Material fijo. La capacidad se planifica **en horas-persona**. Los puntos Fibonacci de
las historias son otra escala (esfuerzo relativo a la canónica) y **no se convierten**.

## Fórmula

| Paso | Fórmula | De dónde sale |
|---|---|---|
| Disponibilidad nominal | `h-persona/semana declaradas × semanas por sprint` | Planilla de disponibilidad del equipo |
| − Reuniones del ciclo | `Σ(cantidad × duración × participantes internos)` | Esquema de reuniones del equipo |
| − Soporte conocido | lo que ya se sabe que consumirá tiempo (guardias, soporte a otros) | Contexto del sprint |
| = Base | `nominal − reuniones − soporte` | |
| − Reserva | `20 % de la Base` | Convención |
| **= Capacidad comprometible** | `máximo(0, Base × 0,80)` | **Techo de compromiso del sprint** |

## Reglas

- **Recalcular en cada Planning** con la planilla real de disponibilidad. Los números de
  arranque son referencia, no acreditan horas reales.
- La **Planning** se carga al sprint que prepara, aunque ocurra el día anterior.
- La coordinación de dependencias **no** es una reunión aparte: va dentro de la
  sincronización diaria.
- Reuniones extraordinarias, actas y coordinación asincrónica salen de la **reserva**,
  no se vuelven a cargar como reunión.
- **Bases individuales negativas**: se revisan, no se trasladan en silencio a otra
  persona.

## Capacidad ≠ presupuesto de trabajo

El **trabajo estimado de las recetas** (suma de horas de los paquetes verificables) es un
**piso**, no un tope. La diferencia entre ese piso y la capacidad comprometible es
**margen explícito**: re-estimación en Planning, imprevistos, coordinación. No se
reescalan los paquetes para «llenar» la capacidad.

## Ejemplo resuelto

Datos que dio el equipo: **12 integrantes**, **408 h-persona/semana** declaradas,
**sprint de 2 semanas**. Esquema de reuniones (12 participantes):

| Reunión | Cant. | Min. | h-persona |
|---|--:|--:|--:|
| Planning | 1 | 120 | 24 |
| Daily / sincronización (incl. dependencias) | 4 | 45 | 36 |
| Review con demo | 1 | 90 | 18 |
| Retrospectiva | 1 | 60 | 12 |
| Refinamiento | 2 | 30 | 12 |
| **Total** | | | **102** |

```text
nominal   = 408 × 2                 = 816 h
reuniones                            = 102 h
soporte conocido                     =   0 h   (este sprint)
base      = 816 − 102 − 0            = 714 h
reserva   = 20 % × 714               = 143 h
capacidad = máximo(0, 714 × 0,80)    ≈ 571 h   ← comprometible
```

Referencia por pareja (5 parejas): `571 / 5 ≈ 114 h` — no 571 para cada una.

Si la receta del sprint suma ~208 h de paquetes, el **margen** es `571 − 208 ≈ 363 h`.
Ese margen se documenta; no se infla la receta hasta 571.

## Errores comunes

- **Convertir puntos a horas** (o al revés) con un factor. No se hace.
- Cargar la duración completa de una reunión que se extendió, **además** de descontar la
  reserva por la extensión. Se cuenta una vez.
- Comprometer el sprint contra la suma de paquetes (~piso) en vez de contra la capacidad.
- Tratar los números de arranque como si acreditaran asistencia u horas trabajadas.
