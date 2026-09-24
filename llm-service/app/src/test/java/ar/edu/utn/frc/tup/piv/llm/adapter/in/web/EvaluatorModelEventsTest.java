package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class EvaluatorModelEventsTest {

  @Test
  void subscribeRegistersEmittersAndBroadcastDropsBrokenOnes() throws Exception {
    var events = new EvaluatorModelEvents();
    assertThat(events.subscribe()).isNotNull();
    assertThat(events.subscribe()).isNotNull();

    SseEmitter healthy = mock(SseEmitter.class);
    SseEmitter broken = mock(SseEmitter.class);
    doThrow(new IOException("closed")).when(broken).send(any(SseEmitter.SseEventBuilder.class));

    Field field = EvaluatorModelEvents.class.getDeclaredField("emitters");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Set<SseEmitter> emitters = (Set<SseEmitter>) field.get(events);
    emitters.add(healthy);
    emitters.add(broken);

    events.activeModelChanged("payload");

    assertThat(emitters).contains(healthy).doesNotContain(broken);
    verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
  }
}