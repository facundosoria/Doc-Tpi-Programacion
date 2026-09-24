package ar.edu.utn.frc.tup.piv.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class LlmServiceApplicationTest {

  @Test
  void mainBootsTheSpringApplication() {
    try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
      LlmServiceApplication.main(new String[]{"--spring.main.banner-mode=off"});

      spring.verify(() -> SpringApplication.run(LlmServiceApplication.class,
          new String[]{"--spring.main.banner-mode=off"}));
    }
  }

  @Test
  void canBeInstantiated() {
    LlmServiceApplication application = new LlmServiceApplication();

    assertThat(application).isNotNull();
  }
}