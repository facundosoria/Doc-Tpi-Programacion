# Fase 2 — Moderación integrada al chat

El chat es propiedad de su equipo. `llm-service` recibe un mensaje, devuelve una decisión, registra evidencia y publica incidentes; no implementa canales, hilos o notificaciones completas.

Antes de comenzar, acordar contrato con `chat-service`, contexto permitido, destino de notificaciones y política de caída. Leer [funciones de IA](../docs/04-funciones-de-ia.md).
Ejecutar S11–S13 usando el [backlog ejecutable](07-backlog-ejecutable-sprints.md) y aplicar el [playbook](06-playbook-de-construccion.md).

## Resultado de la fase

Al cerrar S13, el chat real modera antes de entregar. El usuario recibe feedback que no enseña a evadir el filtro; el docente revisa incidentes y apelaciones; el archivado retiene solo la evidencia necesaria.

## S11 — Moderar antes de entregar

**Demo:** chat permite o bloquea mensajes por categoría/severidad, registra incidentes y notifica los graves.

1. Acordar operación: mensaje, metadata permitida, categorías, severidad, confianza, correlación e idempotencia.
2. Implementar detectores clásicos para lenguaje ofensivo, spam, bloques de código durante desafío y base64.
3. Usar clasificador contextual solo en casos que la capa clásica no decide.
4. Devolver decisión antes de entrega, registrar incidente y publicar evento de severidad alta.
5. Crear vista mínima de incidentes docentes.
6. Probar permitido, bloqueo medio/alto, spam, base64 y caso contextual.

## S12 — Apelar y revisar con contexto

**Demo:** usuario apela; docente revisa mensaje y contexto autorizado; la resolución es auditable y llega al chat.

1. Implementar apelación y autorización de emisor/docente.
2. Recibir solo contexto inmediato necesario, no historial completo por defecto.
3. Preparar corpus etiquetado y revisar falsos positivos con producto/docentes.
4. Crear vista con decisión, categorías, evidencia y motivo docente.
5. Probar apelación repetida, acceso indebido y resolución auditada.

## S13 — Caída, recuperación y retención

**Demo:** una caída aplica política aprobada; al archivar, se conserva evidencia de incidentes sin retener datos extras.

1. Documentar y aprobar política ante caída de clasificador y pérdida total de moderación.
2. Mantener capa clásica ante caída del clasificador.
3. Marcar para revisión/remoderación según política y recuperar al volver servicio.
4. Integrar archivado selectivo: chat purga lo suyo y conserva evidencia media/alta con contexto inmediato.
5. Ejecutar corpus de regresión y medir errores relevantes.

## Salida obligatoria

- [ ] Contrato integrado con chat real.
- [ ] Categorías, severidades, feedback, incidentes y notificaciones demostrados.
- [ ] Apelación y decisión docente auditables.
- [ ] Política de caída aprobada y comprobada.
- [ ] Retención/purga selectiva verificadas con el dueño del chat.
