# 03 — Capacidades de IA

Esta carpeta explica **qué comportamientos de IA construye Tema 07 y con qué restricciones
pedagógicas**. No es un catálogo de APIs: define tutor, evaluación, modelos, rúbricas, Golden Set,
calibración y el alcance posterior de RAG.

## Qué vas a encontrar

1. [Modelos, costos y contexto](01-modelos-costos-y-contexto.md): proveedores, selección de
   modelo, presupuesto y contexto de ejecución.
2. [Funciones de IA](02-funciones-de-ia.md): capacidades del tutor, evaluador y funciones que no
   pertenecen al servicio.
3. [Rúbricas y prompts](03-rubricas-y-prompts.md): dimensiones obligatorias, versiones,
   editabilidad aprobada y reglas de prompts.
4. [Golden Set y calibración](golden-set-y-calibracion/README.md): recorrido especializado para
   referencia humana, publicación, calibración y gates.
5. [RAG e ingesta](rag-e-ingesta/README.md): diseño y antecedentes de una fase posterior, no una
   capacidad MVP que se pueda asumir implementada.
6. [Flujos MVP vigentes](../03-dominio-y-flujos-mvp.md): referencia breve del ciclo de vida de
   rúbricas, Golden Set, calibración y evaluación.

## Cómo leerla

Empezá por `02-funciones-de-ia.md` para conocer el mapa funcional. Leé `01` si el trabajo toca un
modelo o costo, y `03` si modifica evaluación o prompts. Entrá a las subcarpetas solo cuando el
trabajo sea de Golden Set/calibración o RAG.

Cada capacidad debe leerse junto con [Seguridad](../04-seguridad-datos-y-cumplimiento/README.md),
porque una respuesta útil que filtra solución, identidad o contexto sensible sigue siendo inválida.
Si la capacidad se expone a otro microservicio, confirmá el contrato antes de implementarla.

## Qué no concluye esta carpeta

Que un diseño esté documentado no significa que esté implementado. Contrastá siempre con
[Estado de implementación](../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/README.md).
