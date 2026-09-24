# Cobertura — agente B: cuotas y contratos

- **Fecha:** 2026-09-22
- **Fuente de verdad:** `docs/03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md`.
- **Alcance:** D-21–D-38, D-78–D-119 y D-162–D-166.
- **Cambio trazado:** el inventario de contratos y la guía operativa anteriores
  trataban Notificaciones como brecha sin diseño y contenían referencias
  históricas de fallback del evaluador. La regla vigente conserva la brecha de
  acuerdo con Notificaciones —sin publicar contrato—, pero documenta outbox y
  propuesta; y prohíbe fallback para el evaluador único. El plan prevalece.
  D-28 y D-114 siguen como referencia futura del bloque de cuota; D-31–D-38
  preservan que el pipeline de seguridad está fuera del sprint.

| Decisión | Archivo(s) destino en docsV3 | Acción | Nota de contradicción si aplica |
|---|---|---|---|
| D-21 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Inventario anterior declaraba la brecha; ahora conserva separación de dueños y propuesta/estado pendiente. |
| D-22 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Inventario anterior declaraba la brecha; ahora conserva separación de dueños y propuesta/estado pendiente. |
| D-23 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: delimita dueño de catálogo y validaciones aún por acordar con Challenges. |
| D-24 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: delimita dueño de catálogo y validaciones aún por acordar con Challenges. |
| D-25 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: delimita dueño de catálogo y validaciones aún por acordar con Challenges. |
| D-26 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: delimita dueño de catálogo y validaciones aún por acordar con Challenges. |
| D-27 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla; D-28 queda como referencia futura del bloque de cuota fuera de sprint. |
| D-28 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla; D-28 queda como referencia futura del bloque de cuota fuera de sprint. |
| D-29 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla; D-28 queda como referencia futura del bloque de cuota fuera de sprint. |
| D-30 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla; D-28 queda como referencia futura del bloque de cuota fuera de sprint. |
| D-31 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla de dominio; el pipeline de seguridad sigue fuera de sprint. |
| D-32 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla de dominio; el pipeline de seguridad sigue fuera de sprint. |
| D-33 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla de dominio; el pipeline de seguridad sigue fuera de sprint. |
| D-34 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla de dominio; el pipeline de seguridad sigue fuera de sprint. |
| D-35 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla de dominio; el pipeline de seguridad sigue fuera de sprint. |
| D-36 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla de dominio; el pipeline de seguridad sigue fuera de sprint. |
| D-37 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla de dominio; el pipeline de seguridad sigue fuera de sprint. |
| D-38 | `02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md` | agregado/modificado | Nueva regla de dominio; el pipeline de seguridad sigue fuera de sprint. |
| D-78 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: registro acotado y sincronización; no transfiere catálogo ni publicación a LLM. |
| D-79 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: registro acotado y sincronización; no transfiere catálogo ni publicación a LLM. |
| D-80 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: registro acotado y sincronización; no transfiere catálogo ni publicación a LLM. |
| D-81 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Contrato acordado narrativamente; D-164 propone reemplazo pendiente, sin editar OpenAPI. |
| D-82 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: registro acotado y sincronización; no transfiere catálogo ni publicación a LLM. |
| D-83 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: registro acotado y sincronización; no transfiere catálogo ni publicación a LLM. |
| D-84 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: registro acotado y sincronización; no transfiere catálogo ni publicación a LLM. |
| D-85 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Agregado: registro acotado y sincronización; no transfiere catálogo ni publicación a LLM. |
| D-86 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-87 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-88 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-89 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-90 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-91 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-92 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-93 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-94 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; corrige la ausencia de política de verificación y limita fallback fuera del evaluador. |
| D-95 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Añadido; sustituye la idea de proyección/catálogo completo por registro acotado. |
| D-96 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Añadido; sustituye la idea de proyección/catálogo completo por registro acotado. |
| D-97 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Añadido; sustituye la idea de proyección/catálogo completo por registro acotado. |
| D-98 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Añadido; sustituye la idea de proyección/catálogo completo por registro acotado. |
| D-99 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Añadido; sustituye la idea de proyección/catálogo completo por registro acotado. |
| D-100 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-101 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-102 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-103 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-104 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-105 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-106 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-107 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-108 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; diferencia vigencia, deployment y fallo operativo sin inferencias. |
| D-109 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Corrige referencias históricas a fallback evaluador; D-114 queda fuera de sprint. |
| D-110 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md; contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Corrige referencias históricas a fallback evaluador; D-114 queda fuera de sprint. |
| D-111 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md; contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Corrige referencias históricas a fallback evaluador; D-114 queda fuera de sprint. |
| D-112 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md; contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Corrige referencias históricas a fallback evaluador; D-114 queda fuera de sprint. |
| D-113 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Corrige referencias históricas a fallback evaluador; D-114 queda fuera de sprint. |
| D-114 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Corrige referencias históricas a fallback evaluador; D-114 queda fuera de sprint. |
| D-115 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md; contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Añadido; transforma pendientes en precondición dura del cierre. |
| D-116 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md; contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Añadido; transforma pendientes en precondición dura del cierre. |
| D-117 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; define trazabilidad, reanudación administrativa y aviso. |
| D-118 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado/modificado | Añadido; define trazabilidad, reanudación administrativa y aviso. |
| D-119 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md; contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado/modificado | Añadido; define trazabilidad, reanudación administrativa y aviso. |
| D-162 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado (propuesta) | Propuesta técnica pendiente: no modifica schemas, persistencia ni implementación. |
| D-163 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado (propuesta) | Propuesta técnica pendiente: no modifica schemas, persistencia ni implementación. |
| D-164 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado (propuesta) | Propuesta técnica pendiente: no modifica schemas, persistencia ni implementación. |
| D-165 | `contracts/03-sincronizacion-calibracion-y-notificaciones.md` | agregado (propuesta) | Inventario anterior declaraba la brecha; ahora conserva separación de dueños y propuesta/estado pendiente. |
| D-166 | `06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md` | agregado (propuesta) | Propuesta técnica pendiente: no modifica schemas, persistencia ni implementación. |

## Reconciliación de índices pendiente

Cuando el coordinador reconcilie índices globales, agregar entradas a
`contracts/README.md` para `03-sincronizacion-calibracion-y-notificaciones.md`,
a `02-arquitectura-y-plataforma/README.md` para
`08-limites-seguridad-de-skills-y-evaluador.md`, y a
`06-operacion-calidad-y-pruebas/README.md` para
`06-evaluacion-diferida-verificacion-y-degradacion.md`. No se editaron dichos
índices por estar fuera de este encargo.
