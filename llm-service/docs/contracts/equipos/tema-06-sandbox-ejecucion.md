# Tema 06 — Sandbox de ejecución — contratos

## Estado: sin contrato directo

No existe contrato directo entre `llm-service` y el Sandbox de ejecución.

| Dirección | HTTP | Kafka |
|---|---|---|
| Tema 06 → Tema 07 | No hay endpoint del Sandbox hacia Tema 07. | Tema 07 no consume resultados de ejecución. |
| Tema 07 → Tema 06 | Tema 07 no invoca ni orquesta contenedores del Sandbox. | Tema 07 no publica comandos ni resultados al Sandbox. |

La ejecución aislada, sus artefactos y sus secretos permanecen bajo propiedad de Tema 06. En
particular, Tema 07 no recibe mounts, imágenes, logs sin depurar ni credenciales del entorno.

## Futuro

Si se quisiera usar la salida del Sandbox como evidencia de evaluación (por ejemplo, resultado de
tests automáticos sobre código del alumno), haría falta un contrato nuevo con artefactos
permitidos, límites, retención, anonimización y autorización — **faltante por definir**. Hasta
entonces, no debe implementarse ni asumirse ningún acoplamiento entre ambos servicios.
