---
trigger: always_on
---

# Restricción Crítica: Integridad y Preservación de Archivos

1. **PROHIBICIÓN ESTRICTA DE ELIMINACIÓN NO SOLICITADA**:
   - **Bajo ningún concepto** se debe eliminar, truncar, resumir o podar información, código, documentación, comentarios o secciones existentes de un archivo durante un reajuste, refactorización o agregado de funcionalidad, a menos que el usuario lo solicite de manera explícita y directa en el prompt.

2. **MODIFICACIONES QUIRÚRGICAS Y PRESERVACIÓN DE CONTEXTO**:
   - Al editar un archivo, únicamente se deben aplicar los cambios específicos requeridos. Todo el contenido previo que no esté directamente involucrado en la modificación debe permanecer intacto.
   - Está prohibido reemplazar archivos completos con versiones resumidas, marcadores de posición (`// TODO`, `...rest of code`), o versiones incompletas.
   - Se debe preservar siempre la integridad total de funciones, tipos, configuraciones y documentación preexistente.

## 🛑 Restricción: No Modificar en Consultas o Preguntas Teóricas

3. **CONSULTAS Y ANÁLISIS SIN MODIFICACIONES INVOLUNTARIAS**:
   - Cuando el usuario solicite información sobre un tema del proyecto, pida explicaciones o consulte sobre una supuesta/hipotética implementación o diseño, **está prohibido realizar modificaciones de ningún tipo en los archivos**.
   - Únicamente se realizarán cambios, ediciones o creación de archivos si el usuario indica de manera explícita y directa que se apliquen (por ejemplo: "aplica los cambios", "implementa esto", "edita el archivo X").
