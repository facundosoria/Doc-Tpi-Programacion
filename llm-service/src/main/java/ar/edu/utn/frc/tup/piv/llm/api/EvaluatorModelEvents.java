package ar.edu.utn.frc.tup.piv.llm.api;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class EvaluatorModelEvents {
  private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
  public SseEmitter subscribe() { SseEmitter emitter = new SseEmitter(0L); emitters.add(emitter); emitter.onCompletion(() -> emitters.remove(emitter)); emitter.onTimeout(() -> emitters.remove(emitter)); return emitter; }
  public void activeModelChanged(Object payload) { emitters.removeIf(emitter -> { try { emitter.send(SseEmitter.event().name("active-model").data(payload)); return false; } catch (IOException exception) { return true; } }); }
}
