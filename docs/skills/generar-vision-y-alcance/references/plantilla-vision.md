# Visión y alcance — [PROYECTO / SERVICIO]

> **Documento fuente.** Este documento decide **alcance funcional, fases y funciones**.
> Las épicas (`docs/backlog/`) y las historias (`docs/historias/`) se le subordinan: si
> algo no coincide, manda este documento. La aplicación concreta a arquitectura y
> contratos vive en `docs/00`, `docs/contracts/` y los ADR (capa 3).

## 1. El problema

[1–2 párrafos: qué duele hoy, para quién, qué cambia si esto existe.]

> **Hallazgo crítico** *(si lo hay)*: [la dependencia o el riesgo que puede hundir el
> release aunque el servicio esté perfecto. Va arriba de todo.]

## 2. Alcance por fase

### [Fase / release 1 — p. ej. MVP]

| Entra | Requisito | Nota |
|---|---|---|
| [función / capacidad] | [RF-*] | [·] |

**Queda explícitamente afuera de esta fase:** [listar, con la fase a la que va cada cosa].

### [Fase 2] · [Fase 3]

*(igual estructura; gruesas si están lejos)*

## 3. Funciones / capacidades

| # | Función | Qué hace (para el usuario) | Modo | Requisitos | Nota |
|---|---|---|---|---|---|
| 1 | [·] | [·] | sync / async / — | [RF-*] | [·] |

> Estas filas alimentan `generar-contratos-servicio` (endpoints/eventos) y el catálogo de
> épicas de `generar-backlog-y-recetas`.

## 4. Fronteras

| Lo que construimos y decidimos | Zona de negociación (con otro equipo) | Lo que depende de otros |
|---|---|---|
| [·] | [contrato de la API · dueño de la cola · quién persiste · quién hace las pantallas · formato de correlación] | [·] |

## 5. Objetivos medibles

- [qué tiene que ser cierto para decir «funciona»: KPI, umbral, criterio de release]
- *(a definir con el PO)* — lo que el equipo aún no fijó

## 6. Roles

| Rol | Objetivo propio | Aparece en |
|---|---|---|
| [docente / alumno / operador / …] | [qué persigue] | historias `Sxx-Hyy` |

## 7. Riesgos del recorte de alcance

| Riesgo | Por qué pasa | Mitigación | Dueño / fecha |
|---|---|---|---|
| [cada equipo asume que lo hace el otro] | [·] | [·] | 🔴 |

## 8. Glosario del dominio

| Término | Definición |
|---|---|
| [·] | [·] |
