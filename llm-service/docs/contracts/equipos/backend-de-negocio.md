# Backend de negocio — contratos

> Este documento es el contrato completo y vigente con Backend de negocio — `18` ya no repite
> este detalle, solo indexa hacia acá. Reglas generales:
> 18 §0.

## Qué nos da (acordado)

- Propagación del JWT en cada llamada — nunca confiamos en parámetros del cliente para
  identidad (regla general de doc 18 §0/§7).

## Relación con la degradación de RF-IA-27

La escalera completa de degradación (modelo primario → otro proveedor → modelo local →
degradación funcional) es enteramente responsabilidad **nuestra**
(`06-operacion-e-ingenieria.md`). Lo único que le corresponde al Backend es **aceptar la
entrega igual cuando el evaluador está caído** — ver [`pendientes.md`](../../07-planificacion-y-trabajo-equipo/11-equipos/backend-de-negocio/pendientes.md).

## Qué pasa si esto falla (la secuencia completa que el Backend tiene que soportar)

Técnica común en
[transversales del README](../README.md#resiliencia-y-manejo-de-errores-técnica-común-a-todos-los-endpoints).
Este es el único equipo donde "qué pasa si falla" **es** el contrato, no un detalle aparte:

1. El alumno entrega el intento → Tema 05 publica `ATTEMPT_CLOSED` (desde el 2026-09-13; antes lo publicaba Tema 03) → nosotros encolamos.
2. Si el proveedor no responde tras backoff+tope, publicamos `score_pendiente_diferido` con
   `motivo: proveedor_no_disponible` y `reintentar_desde`.
3. **El Backend tiene que haber aceptado la entrega en el paso 1, sin esperar el score.** El
   registro académico queda con `score_agregado = null` hasta que llegue
   `SCORE_CALCULATED` (éxito) o se resuelva por otra vía (override docente).
4. Mientras el pendiente exista, `GET /course-cohorts/{courseCohortId}/pending-evaluations` lo
   cuenta y **bloquea el cierre del curso** — el Backend/Tema 02 tienen que consultarlo antes de
   cerrar, no asumir que "sin error, está todo evaluado".

Si el Backend no implementa el paso 3, el síntoma es el que doc 17 marca como **"el que más se
cae entre equipos"**: la entrega queda trabada esperando un score que puede tardar minutos u
horas.
