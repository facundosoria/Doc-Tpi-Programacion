package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalibrationInferencePolicyTest {
  @Test void resolvesCapabilityDrivenSettingsWithoutProviderNames() {
    var policy = new CalibrationInferencePolicy();
    var caps = new ProviderCapabilities(true, true, true, true, true, true, true, true);
    var resolved = policy.resolve(caps, 42L);
    assertThat(resolved.settings().policyVersion()).isEqualTo("v2");
    assertThat(resolved.settings().temperature()).isZero();
    assertThat(resolved.settings().topP()).isEqualTo(1d);
    assertThat(resolved.settings().topK()).isNull();
    assertThat(resolved.settings().seed()).isEqualTo(42L);
    assertThat(resolved.settings().structuredJson()).isFalse();
    assertThat(resolved.settings().maxOutputTokens()).isEqualTo(1024);
  }

  @Test void leavesUnsupportedCapabilitiesAsNull() {
    var policy = new CalibrationInferencePolicy();
    var caps = new ProviderCapabilities(false, false, false, false, false, false, false, false);
    var resolved = policy.resolve(caps, 7L);
    assertThat(resolved.settings().temperature()).isNull();
    assertThat(resolved.settings().topP()).isNull();
    assertThat(resolved.settings().seed()).isNull();
    assertThat(resolved.capabilities()).isSameAs(caps);
  }

  @Test void auditViewIsAnImmutableSnapshot() {
    var policy = new CalibrationInferencePolicy();
    var caps = new ProviderCapabilities(false, true, false, true, true, false, true, false);
    var view = policy.resolve(caps, 3L).auditView();
    assertThat(view).containsKeys("version", "temperature", "topP", "topK", "seed",
        "structuredJson", "maxOutputTokens", "capabilities");
    assertThat(view.get("capabilities")).isSameAs(caps);
    assertThatThrownBy(() -> view.put("extra", 1)).isInstanceOf(UnsupportedOperationException.class);
  }
}