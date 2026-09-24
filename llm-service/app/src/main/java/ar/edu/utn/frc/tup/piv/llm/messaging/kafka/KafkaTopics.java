package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

/**
 * Topics Kafka que usa {@code llm-service}, tal como figuran en
 * {@code docs/contracts/llm-service.asyncapi.yaml}. <b>Nombres PROVISORIOS:</b> los grupos no pueden
 * crear tópicos (KAFKA_EVENT_STANDARD.md §4) y el grupo de Notificaciones todavía no asignó los
 * definitivos. Cuando lo haga, se cambian solo los valores de estas constantes. Un topic agrupa
 * varios {@code eventType}; no crear un topic nuevo por cada tipo de evento.
 */
public final class KafkaTopics {

  /** Publicado por llm-service: incidentes y resoluciones de moderación del dominio propio. */
  public static final String MODERATION_EVENTS = "moderation-events";

  /**
   * Consumido desde practice-service (Tema 05); contrato en convergencia (fixture provisorio,
   * ver {@code docs/historias/ep-01/h07.md} §"Estrategia de autonomía").
   */
  public static final String PRACTICE_EVENTS = "practice-events";

  /** Publicado por llm-service: score de los intentos evaluados (`SCORE_CALCULATED`/`SCORE_DEFERRED`). */
  public static final String EVALUATION_EVENTS = "evaluation-events";

  private KafkaTopics() {
  }
}
