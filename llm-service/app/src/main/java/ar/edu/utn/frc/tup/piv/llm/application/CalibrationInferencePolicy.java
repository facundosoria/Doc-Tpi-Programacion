package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.provider.spi.InferenceSettings;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Policy is capability-driven; it never branches on a provider name.
 */
@Component
public class CalibrationInferencePolicy {
    public EffectiveSettings resolve(ProviderCapabilities capabilities, long seed) {
        var settings = new InferenceSettings("v2", capabilities.temperature() ? 0d : null,
                capabilities.topP() ? 1d : null, null, capabilities.seed() ? seed : null,
                false, 1024);
        return new EffectiveSettings(settings, capabilities);
    }

    public record EffectiveSettings(InferenceSettings settings, ProviderCapabilities capabilities) {
        public Map<String, Object> auditView() {
            var values = new LinkedHashMap<String, Object>();
            values.put("version", settings.policyVersion());
            values.put("temperature", settings.temperature());
            values.put("topP", settings.topP());
            values.put("topK", settings.topK());
            values.put("seed", settings.seed());
            values.put("structuredJson", settings.structuredJson());
            values.put("maxOutputTokens", settings.maxOutputTokens());
            values.put("capabilities", capabilities);
            return java.util.Collections.unmodifiableMap(values);
        }
    }
}
