package ar.edu.utn.frc.tup.piv.llm.messaging.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Enruta a dead-letter (CA4 de LLM-EP01-H07) eventos sin {@code eventId} o mal formados, para que
 * el consumo de la partición siga sin bloquearse. El dead-letter topic es {@code <topic>.dlt}, el
 * mismo sufijo que usa {@code DeadLetterPublishingRecoverer} de Spring Kafka.
 */
@Component
public class DeadLetterPublisher {

  private static final Logger log = LoggerFactory.getLogger(DeadLetterPublisher.class);

  private final KafkaTemplate<String, String> kafkaTemplate;

  public DeadLetterPublisher(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void send(String sourceTopic, String key, String rawValue, String reason) {
    String dlt = sourceTopic + ".dlt";
    log.warn("Evento enviado a dead-letter [topic={}, reason={}]", dlt, reason);
    kafkaTemplate.send(dlt, key, rawValue);
  }
}
