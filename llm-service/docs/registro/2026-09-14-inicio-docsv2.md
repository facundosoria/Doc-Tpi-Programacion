# DOCV2-001 — Inicio de documentación V2

**Fecha:** 2026-09-14  
**Estado:** aplicado

## Qué había

La documentación histórica de `docs/` contiene la evolución del proyecto y contratos dispersos,
con puntos que requieren consolidación antes de que otros microservicios puedan integrarse sin
ambigüedad.

## Por qué cambia

Se necesita una fuente canónica incremental: una corrección debe actualizar el documento vigente y
dejar trazabilidad de la decisión, sin borrar los antecedentes ni crear adendas contradictorias.

## Qué hay ahora

Se creó `docsV2/` sin modificar ni eliminar `docs/`. Los contratos de comunicación están reunidos
en `docsV2/contracts/`; los requisitos de datos que Tema 07 espera de otros micros se concentran
en un único documento editable. OpenAPI y AsyncAPI declaran estado **Propuesto** hasta que sus
pares los acepten.

## Decisión incorporada

La regla vigente aprobada por Product Owner permite editar rúbricas mientras estén en borrador;
una vez publicadas son inmutables. Esta regla reemplaza la contradicción histórica y figura en el
gobierno, dominio y contrato V2.
