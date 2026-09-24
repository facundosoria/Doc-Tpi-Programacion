package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import ar.edu.utn.frc.tup.piv.llm.provider.spi.AiProviderAdapter;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderDescriptor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Registry/Strategy selector. A duplicate key is a deployment error, never an arbitrary choice. */
@Component
public class ProviderRegistry {
  private final Map<String, AiProviderAdapter> adapters;

  public ProviderRegistry(List<AiProviderAdapter> candidates) {
    var resolved = new LinkedHashMap<String, AiProviderAdapter>();
    for (var candidate : candidates) {
      String key = normalize(candidate.descriptor().key());
      if (resolved.putIfAbsent(key, candidate) != null) {
        throw new IllegalStateException("Hay más de un adaptador registrado para " + key);
      }
    }
    adapters = Map.copyOf(resolved);
  }

  public AiProviderAdapter required(String providerKey) {
    var adapter = adapters.get(normalize(providerKey));
    if (adapter == null) throw new IllegalArgumentException("El proveedor no está instalado: " + providerKey);
    return adapter;
  }

  public List<ProviderDescriptor> descriptors() {
    return adapters.values().stream().map(AiProviderAdapter::descriptor)
        .sorted(java.util.Comparator.comparing(ProviderDescriptor::key)).toList();
  }

  private String normalize(String key) {
    if (key == null || !key.matches("[a-z][a-z0-9-]{1,63}")) {
      throw new IllegalArgumentException("providerKey inválido");
    }
    return key.toLowerCase(Locale.ROOT);
  }
}
