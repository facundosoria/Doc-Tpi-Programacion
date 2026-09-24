package ar.edu.utn.frc.tup.piv.llm.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Cliente hacia el Gateway para llamadas micro-a-micro.
 * Propaga la traza: copia traceparent y X-Request-Id del request entrante al saliente.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient gatewayRestClient(RestClient.Builder builder,
            @Value("${app.gateway-url:http://api-gateway:8080}") String gatewayUrl) {
        return builder
                .baseUrl(gatewayUrl)
                .requestInterceptor((request, body, execution) -> {
                    HttpServletRequest entrante = requestEntrante();
                    if (entrante != null) {
                        copiar(entrante, request, "traceparent");
                        copiar(entrante, request, IdentityHeaders.REQUEST_ID);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    private HttpServletRequest requestEntrante() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    private void copiar(HttpServletRequest entrante,
            org.springframework.http.HttpRequest saliente, String header) {
        String valor = entrante.getHeader(header);
        if (valor != null && !valor.isBlank()) {
            saliente.getHeaders().set(header, valor);
        }
    }
}
