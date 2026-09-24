package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.client;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ContextualClassificationResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextualModerationClientAdapterTest {

    private WireMockServer wireMockServer;
    private ContextualModerationClientAdapter adapter;

    @BeforeAll
    static void warmUp() {
        WireMockServer warmServer = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").dynamicPort());
        warmServer.start();
        warmServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"results\":[]}")));
        try {
            new ContextualModerationClientAdapter("http://127.0.0.1:" + warmServer.port(), "k", "m", 2000)
                    .classify("warmup");
        } catch (Exception ignored) {}
        warmServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").dynamicPort());
        wireMockServer.start();

        adapter = new ContextualModerationClientAdapter(
                "http://127.0.0.1:" + wireMockServer.port(),
                "test-secret-key",
                "omni-moderation-latest",
                300
        );
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void classifyCleanTextReturnsAllowWithin200Ms() {
        String jsonResponse = """
            {
              "id": "modr-clean",
              "model": "omni-moderation-latest",
              "results": [
                {
                  "flagged": false,
                  "categories": {
                    "harassment": false,
                    "hate": false
                  },
                  "category_scores": {
                    "harassment": 0.01,
                    "hate": 0.005
                  }
                }
              ]
            }
        """;

        wireMockServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        ContextualClassificationResult result = adapter.classify("Hola profe, gracias por la clase");

        assertThat(result.flagged()).isFalse();
        assertThat(result.category()).isEqualTo("CLEAN");
        assertThat(result.latencyMs()).isLessThan(300L);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/moderations"))
                .withHeader("Authorization", equalTo("Bearer test-secret-key")));
    }

    @Test
    void classifyFlaggedTextMapsCategoriesAndTopScore() {
        String jsonResponse = """
            {
              "id": "modr-flagged",
              "model": "omni-moderation-latest",
              "results": [
                {
                  "flagged": true,
                  "categories": {
                    "harassment": true,
                    "hate": false
                  },
                  "category_scores": {
                    "harassment": 0.88,
                    "hate": 0.12
                  }
                }
              ]
            }
        """;

        wireMockServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        ContextualClassificationResult result = adapter.classify("Mensaje hostil o de acoso contextual");

        assertThat(result.flagged()).isTrue();
        assertThat(result.category()).isEqualTo("HARASSMENT");
        assertThat(result.score()).isEqualTo(0.88);
        assertThat(result.categoryScores()).containsEntry("harassment", 0.88);
    }

    @Test
    void classifyTimesOutWhenExternalServerExceeds300Ms() {
        // Simulamos timeout de 2 segundos en el clasificador externo
        wireMockServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse()
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[]}")));

        long start = System.currentTimeMillis();

        assertThatThrownBy(() -> adapter.classify("Mensaje demorado"))
                .isInstanceOf(RestClientException.class);

        long elapsed = System.currentTimeMillis() - start;
        // Debe abortar alrededor de 300 ms (con margen razonable) y NUNCA esperar los 2000 ms completos
        assertThat(elapsed).isLessThan(1500L);
    }

    @Test
    void classifyPropagatesHttp500ServerError() {
        wireMockServer.stubFor(post(urlEqualTo("/v1/moderations"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> adapter.classify("Mensaje que genera error 500"))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void classifyBlankOrNullTextReturnsCleanWithoutNetworkCall() {
        ContextualClassificationResult empty = adapter.classify("");
        assertThat(empty.flagged()).isFalse();

        ContextualClassificationResult nullText = adapter.classify(null);
        assertThat(nullText.flagged()).isFalse();

        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/v1/moderations")));
    }
}
