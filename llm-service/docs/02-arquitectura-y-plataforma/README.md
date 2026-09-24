# 02 — Arquitectura y plataforma

Esta carpeta describe **dónde empieza y termina el servicio**, cómo se despliega y qué reglas de
plataforma no se pueden eludir. Leela antes de modificar backend, configuración, comunicación entre
servicios o dependencias.

## Qué vas a encontrar

1. [Arquitectura y stack](01-arquitectura-y-stack.md): componentes, responsabilidades, decisiones
   tecnológicas y límites del backend.
2. [Sincronización de arquitectura y despliegue](02-sincronizacion-arquitectura-y-despliegue.md):
   acuerdos y contexto de la plataforma compartida.
3. [Herramientas y librerías](03-herramientas-y-librerias.md): dependencias y propósito de cada
   herramienta.
4. [Estructura del backend](04-estructura-del-backend.md): organización de paquetes y capas.
5. [Servicios Docker](05-servicios-docker.md): dependencias locales y operación del entorno.
6. [Gateway y discovery](06-gateway-y-discovery/README.md): red, Eureka, ruteo, identidad,
   comunicación micro a micro, resiliencia y pruebas obligatorias.
7. [Arquitectura y fronteras](../02-arquitectura-y-fronteras.md): referencia breve del límite entre Tema 07 y
   las demás partes de la plataforma.
8. [Límites, seguridad de skills y evaluador](08-limites-seguridad-de-skills-y-evaluador.md):
   protección preventiva de cuota/contexto, límites de carga administrados, deshabilitación y
   revisión de skills por seguridad, y el modelo de evaluador único sin fallback
   (D-27 a D-38 y D-88 a D-91).

## Cómo leerla

Para entender el servicio, empezá por `01`, seguí por `04` y después `06`. Consultá `02`, `03` y
`05` cuando el cambio involucre despliegue, librerías o infraestructura. El README de Gateway tiene
su propio orden porque sus reglas se aplican a toda comunicación de red.

Arquitectura explica el mecanismo y las fronteras; no define por sí sola los campos de una llamada.
Antes de crear o cambiar un endpoint/evento, continuá con [`contracts/`](../contracts/README.md).

## Resultado esperado al terminar

Deberías poder identificar la capa donde vive un cambio, saber por qué el Gateway es obligatorio y
distinguir una dependencia interna de un contrato con otro microservicio.
