package ar.edu.utn.frc.tup.piv.llm.application;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.Optional; import java.util.UUID; import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.assertThat; import static org.assertj.core.api.Assertions.assertThatThrownBy; import static org.mockito.Mockito.*;
class CalibrationWorkflowServiceTest {
 @Test void activatesOnlyPassedRun(){ var store=mock(CalibrationWorkflowService.CalibrationWorkflowStore.class); var service=new CalibrationWorkflowService(store); var run=UUID.randomUUID(); when(store.isPassed(run)).thenReturn(false); assertThatThrownBy(()->service.activate(UUID.randomUUID(),run,new CallerIdentity("llm-service", UUID.randomUUID(), null, null))).isInstanceOf(IllegalStateException.class); }
 @Test void resumesOnlyWhenClaimedItemHasCalibration(){ var store=mock(CalibrationWorkflowService.CalibrationWorkflowStore.class); var service=new CalibrationWorkflowService(store); var item=new CalibrationWorkflowService.QueuedEvaluation(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID()); when(store.claimNextQueued()).thenReturn(Optional.of(item)); when(store.hasValidCalibration(item.challengeId())).thenReturn(true); when(store.markRunning(item.id())).thenReturn(true); assertThat(service.resumeNext()).isTrue(); }
}
