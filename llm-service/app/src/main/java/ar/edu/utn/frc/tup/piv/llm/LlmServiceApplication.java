package ar.edu.utn.frc.tup.piv.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LlmServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(LlmServiceApplication.class, args);
  }
}
