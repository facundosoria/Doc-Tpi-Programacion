# 04 — Seguridad, datos y cumplimiento

Esta carpeta reúne las reglas que protegen al alumno, la información académica y la plataforma.
Leela antes de tocar prompts, transcripciones, soluciones esperadas, autenticación, proveedores,
cuotas o degradación de servicio.

## Qué vas a encontrar

1. [Seguridad y guardarraíles](01-seguridad-y-guardarrailes.md): anti-jailbreak, anti-fuga,
   validación y límites de comportamiento del tutor.
2. [Datos, retención y términos](02-datos-retencion-y-terminos.md): ownership, conservación,
   privacidad y semántica de datos.
3. [Rate limit y resiliencia](03-rate-limit-y-resiliencia.md): cuotas, degradación, reintentos y
   continuidad controlada.
4. [Seguridad y gobierno de skills](04-seguridad-y-gobierno-de-skills.md): auditoría del flujo de
   skills/calibración, confianza cero sobre el contenido de una skill, publicación, archivado,
   suspensión preventiva y reglas de contenido/sanitización de Markdown.
5. [Reglas de seguridad V2](../04-seguridad-y-datos-sensibles.md): referencia breve de datos
   sensibles, secretos, solución esperada y transcripciones.

## Cómo leerla

Para cualquier funcionalidad que llame a un modelo, empezá por `01`. Si persiste, muestra o recibe
datos, continuá con `02`. Si agrega un proveedor, cola, cuota o fallback, leé también `03` y
Operación. No relegues esta lectura al final: estos documentos determinan qué implementaciones son
aceptables.

## Relación con contratos y código

Las reglas de seguridad indican qué puede circular y qué no. El formato concreto que otro servicio
debe enviar o recibir está en [`contracts/`](../contracts/README.md). La evidencia de que una regla
está realmente implementada se consulta en
[Estado de implementación](../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/README.md).
