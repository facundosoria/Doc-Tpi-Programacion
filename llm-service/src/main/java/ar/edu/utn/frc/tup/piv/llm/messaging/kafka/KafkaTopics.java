package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

/**
 * Topics Kafka de la plataforma organizados por dominio (KAFKA_EVENT_STANDARD.md §4), tal como
 * quedaron congelados en {@code docs/contracts/llm-service-v1.asyncapi.yaml}. Un topic agrupa
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

  private KafkaTopics() {
  }
}
