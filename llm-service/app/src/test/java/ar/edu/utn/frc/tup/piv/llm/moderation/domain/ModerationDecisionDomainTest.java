package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.DeterministicModerationPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDetectorPort;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationDecisionDomainTest {

    @Test
    void allowDecisionNeverHasIncidentIdAndNeverPersistsText() {
        ModerationDecision decision = ModerationDecision.allow(
                "msg-101",
                ModerationReasonCode.CLEAN,
                "deterministic",
                25L,
                "hash-123"
        );

        assertThat(decision.getMessageId()).isEqualTo("msg-101");
        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");
        assertThat(decision.getLatencyMs()).isEqualTo(25L);
        assertThat(decision.getIncidentId()).isNull();
        assertThat(decision.shouldPersistText()).isFalse();
    }

    @Test
    void blockAndPendingDecisionsMustHaveIncidentIdAndMustPersistText() {
        UUID incidentId = UUID.randomUUID();
        ModerationDecision block = ModerationDecision.block(
                "msg-102",
                ModerationReasonCode.SPAM,
                "contextual",
                120L,
                "hash-456",
                incidentId
        );

        assertThat(block.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(block.getIncidentId()).isEqualTo(incidentId);
        assertThat(block.shouldPersistText()).isTrue();

        ModerationDecision pending = ModerationDecision.pending(
                "msg-103",
                ModerationReasonCode.ENGINE_UNAVAILABLE,
                "fallback",
                850L,
                "hash-789"
        );

        assertThat(pending.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING);
        assertThat(pending.getIncidentId()).isNotNull();
        assertThat(pending.shouldPersistText()).isTrue();

        ModerationDecision pendingRev = ModerationDecision.pendingReview(
                "msg-104",
                ModerationReasonCode.NEEDS_REVIEW,
                "fallback",
                150L,
                "hash-abc",
                ModerationReasonCode.CONTEXTUAL_UNAVAILABLE
        );
        assertThat(pendingRev.getDecision()).isEqualTo(ModerationDecisionEnum.PENDING_REVIEW);
        assertThat(pendingRev.getReasonCode()).isEqualTo(ModerationReasonCode.NEEDS_REVIEW);
        assertThat(pendingRev.getClassifierUsed()).isEqualTo("fallback");
        assertThat(pendingRev.getDegradationReason()).isEqualTo(ModerationReasonCode.CONTEXTUAL_UNAVAILABLE);
        assertThat(pendingRev.getIncidentId()).isNotNull();
        assertThat(pendingRev.shouldPersistText()).isTrue();
    }

    @Test
    void messageContentValidatesLengthAndCalculatesDeterministicSha256() {
        MessageContent content1 = new MessageContent("Hola mundo");
        MessageContent content2 = new MessageContent("Hola mundo");

        assertThat(content1.getContentHash()).isEqualTo(content2.getContentHash());
        assertThat(content1.getContentHash()).hasSize(64);

        assertThatThrownBy(() -> new MessageContent(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");

        assertThatThrownBy(() -> new MessageContent("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");

        assertThatThrownBy(() -> new MessageContent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");

        String longText = "a".repeat(4097);
        assertThatThrownBy(() -> new MessageContent(longText))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede exceder los 4096 caracteres");

        assertThat(content1.getText()).isEqualTo("Hola mundo");
        assertThat(content1).isEqualTo(content2);
        assertThat(content1.hashCode()).isEqualTo(content2.hashCode());
        assertThat(content1.toString()).contains("length=10", content1.getContentHash());
        assertThat(content1).isNotEqualTo(null);
        assertThat(content1).isNotEqualTo(new MessageContent("Otro texto"));
    }

    @Test
    void moderationDecisionRejectsInvalidArguments() {
        assertThatThrownBy(() -> new ModerationDecision(
                null, ModerationDecisionEnum.ALLOW, "CLEAN", "deterministic", 10L, null, "hash"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ModerationDecision(
                "msg-1", null, "CLEAN", "deterministic", 10L, null, "hash"
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void detectionResultMethodsAndInvariants() {
        DetectionResult pass = DetectionResult.pass("testDetector", 5L);
        assertThat(pass.isPassed()).isTrue();
        assertThat(pass.isFailed()).isFalse();
        assertThat(pass.getDetectorName()).isEqualTo("testDetector");
        assertThat(pass.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
        assertThat(pass.getScore()).isEqualTo(0.0);
        assertThat(pass.getLatencyMs()).isEqualTo(5L);

        DetectionResult fail = DetectionResult.fail("testDetector", ModerationReasonCode.OFFENSIVE, 0.85, 8L);
        assertThat(fail.isPassed()).isFalse();
        assertThat(fail.isFailed()).isTrue();
        assertThat(fail.getReasonCode()).isEqualTo(ModerationReasonCode.OFFENSIVE);
        assertThat(fail.getScore()).isEqualTo(0.85);

        // Equals, hashCode y toString
        DetectionResult failClone = DetectionResult.fail("testDetector", ModerationReasonCode.OFFENSIVE, 0.85, 8L);
        assertThat(fail).isEqualTo(failClone);
        assertThat(fail.hashCode()).isEqualTo(failClone.hashCode());
        assertThat(fail.toString()).contains("testDetector", "OFFENSIVE");
        assertThat(fail).isNotEqualTo(pass);
        assertThat(fail).isNotEqualTo(null);
    }

    @Test
    void moderationDecisionEqualsHashCodeAndToString() {
        UUID id = UUID.randomUUID();
        ModerationDecision d1 = ModerationDecision.block("msg-x", ModerationReasonCode.SPAM, "deterministic", 10L, "h1", id);
        ModerationDecision d2 = ModerationDecision.block("msg-x", ModerationReasonCode.SPAM, "deterministic", 10L, "h1", id);

        assertThat(d1).isEqualTo(d2);
        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
        assertThat(d1.toString()).contains("msg-x", "SPAM");
        assertThat(d1.getContentHash()).isEqualTo("h1");
        assertThat(d1).isNotEqualTo(null);
    }

    @Test
    void defaultPortMethodsDelegateProperly() {
        ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDetectorPort detectorPort =
                new ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDetectorPort() {
                    @Override
                    public String getName() { return "custom"; }

                    @Override
                    public DetectionResult detect(String text, String courseId) {
                        return DetectionResult.pass(getName(), 1L);
                    }
                };

        assertThat(detectorPort.detect("texto simple").isPassed()).isTrue();

        ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.DeterministicModerationPort moderationPort =
                new ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.DeterministicModerationPort() {
                    @Override
                    public ModerationDecision evaluate(String messageId, String text, String courseId) {
                        return ModerationDecision.allow(messageId, ModerationReasonCode.CLEAN, "custom", 1L, "hash");
                    }
                };

        assertThat(moderationPort.evaluate("msg-def", "texto").getDecision())
                .isEqualTo(ModerationDecisionEnum.ALLOW);
    }
}
