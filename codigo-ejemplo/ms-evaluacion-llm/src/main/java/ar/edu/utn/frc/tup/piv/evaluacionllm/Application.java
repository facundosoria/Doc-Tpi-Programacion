package ar.edu.utn.frc.tup.piv.evaluacionllm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the LLM evaluation microservice.
 *
 * <p>Intentionally empty: this skeleton exists so the quality gate can verify its
 * Java stages today. The service design lives in docs/02-arquitectura-y-stack.md.
 *
 * <p>Note: source code is written in English on purpose. The documentation stays in
 * Spanish; the `idioma_codigo` check enforces the split.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
