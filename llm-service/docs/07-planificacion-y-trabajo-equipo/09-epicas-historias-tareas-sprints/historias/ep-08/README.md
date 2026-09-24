# Historias de usuario — EP-08 · Moderación integrada (F2)

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de Taiga](../../../10-plantillas/historia-de-usuario-taiga.md).
>
> **Fuente de verdad:** Épica: [`../../epicas/ep-08.md`](../../epicas/ep-08.md). Requisitos: RF-CHT-09 a 14. Backlog: [`35` · S11–S13](../../../04-backlog-ejecutable.md).
> **Prerequisito:** contrato de moderación acordado con `chat-service` y política de moderación aprobada por producto/seguridad.

## Índice

| ID | Título | Tipo | Pareja | Estado |
|---|---|---|---|---|
| [LLM-S11-H01](h01.md) | Que un mensaje del chat se permita o bloquee antes de entregarse | Tarea (habilitador — contrato con chat) | P3+P2+P1 | 🟡 Verificado en vivo — `CA_negativo_1` implementado (2026-09-19); `sender_id`/camino contextual mockeados |
| [LLM-S11-H02](h02.md) | Detectar spam, contenido ofensivo e intentos de ocultar código | Tarea (habilitador) | P3 | 🟢 Verificado en vivo — umbrales por defecto sin calibrar (2026-09-18) |
| [LLM-S12-H01](h03.md) | Apelar un mensaje que bloqueó la moderación | HU de valor | P3 | 🟢 Verificado en vivo (2026-09-18) |
| [LLM-S12-H02](h04.md) | Que el docente revise un incidente de moderación y lo resuelva | HU de valor | P3 | 🟡 Verificado en vivo — notificación real implementada (HTTP + Kafka, mock con `NOTIFICATIONS_ENABLED=false`); falta contrato con notifications-service (2026-09-19) |
| [LLM-S13-H01](h05.md) | Que la moderación siga funcionando (en modo degradado) si el clasificador contextual falla | Tarea (habilitador) | P3+P2 | 🟢 Verificado (2026-09-18) |
| [LLM-S13-H02](h06.md) | Que la evidencia de moderación se retenga el tiempo necesario y luego se purgue | Tarea (habilitador) | P1 | 🟡 Cubierto por tests, no ejercitado en vivo — períodos de retención hardcodeados (2026-09-18) |
