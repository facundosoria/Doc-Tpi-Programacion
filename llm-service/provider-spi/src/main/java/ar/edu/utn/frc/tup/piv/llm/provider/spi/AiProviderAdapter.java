package ar.edu.utn.frc.tup.piv.llm.provider.spi;

import java.util.List;
import java.util.function.Consumer;

/**
 * Strategy boundary for a provider module. The application depends on this SPI only; an adapter
 * owns its SDK, authentication protocol and provider-specific request/response mapping.
 */
public interface AiProviderAdapter {
  ProviderDescriptor descriptor();

  void validate(ProviderCredentialMaterial credential);

  List<ModelDescriptor> discoverModels(ProviderCredentialMaterial credential);

  ProviderReply invoke(ProviderCredentialMaterial credential, ProviderInvocation invocation);

  default ProviderReply stream(ProviderCredentialMaterial credential, ProviderInvocation invocation,
      Consumer<String> onDelta) {
    ProviderReply reply = invoke(credential, invocation);
    onDelta.accept(reply.text());
    return reply;
  }
}
