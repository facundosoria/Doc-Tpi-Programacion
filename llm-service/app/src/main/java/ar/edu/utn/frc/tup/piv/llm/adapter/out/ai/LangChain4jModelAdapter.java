package ar.edu.utn.frc.tup.piv.llm.adapter.out.ai;

import ar.edu.utn.frc.tup.piv.llm.application.port.out.ModelInvocationPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationRequest;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderInvocationGateway;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.InferenceSettings;
import org.springframework.stereotype.Component;

/**
 * Runtime port implementation delegated to the provider Strategy registry.
 */
@Component
public class LangChain4jModelAdapter implements ModelInvocationPort {
    private final ProviderCredentialRepository deployments;
    private final FunctionModelConfigRepository configs;
    private final ProviderInvocationGateway gateway;

    public LangChain4jModelAdapter(ProviderCredentialRepository deployments, FunctionModelConfigRepository configs, ProviderInvocationGateway gateway) {
        this.deployments = deployments;
        this.configs = configs;
        this.gateway = gateway;
    }

    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
        var config = configs.find(request.function()).filter(FunctionModelConfigRepository.Config::enabled).orElseThrow(() -> new IllegalStateException("La función no tiene modelo habilitado"));
        var deployment = deployments.forId(config.modelDeploymentId()).orElseThrow(() -> new IllegalStateException("El deployment asignado no está disponible"));
        var credential = deployments.get(deployment.credentialId()).filter(value -> "ACTIVE".equals(value.state())).orElseThrow(() -> new IllegalStateException("La credencial del deployment no está activa"));
        var reply = gateway.invoke(credential, deployment.modelId(), request.systemPrompt() + "\n\n" + request.userPrompt(), new InferenceSettings("runtime-v2", null, null, null, null, false, 512), request.timeout());
        return new ModelInvocationResult(reply.text(), deployment.providerKey(), deployment.modelId());
    }

    @Override
    public String provider() {
        return "deployment-selected";
    }

    @Override
    public String model() {
        return "deployment-selected";
    }
}
