package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * H05 (ex-H08)·CA1/CA4: el contrato OpenAPI publicado describe únicamente operaciones que el servicio
 * construyó. Un PR que agregue al contrato una operación sin controller falla acá.
 *
 * <p>Compara el contrato {@code docs/contracts/llm-service.openapi.yaml} con los {@code @RestController}
 * del módulo, sin levantar Spring ni la base. La dirección inversa (rutas construidas que el contrato
 * todavía no documenta) es deuda conocida y está fijada en {@link #UNDOCUMENTED_ROUTE_COUNT_CEILING}: solo
 * puede bajar.
 */
class OpenApiContractTest {

  private static final Path CONTRACT = Path.of("..", "docs", "contracts", "llm-service.openapi.yaml");
  private static final String BASE_PACKAGE = "ar.edu.utn.frc.tup.piv.llm";

  /** Rutas del código que el contrato aún no documenta. Solo puede achicarse: no subir este número. */
  private static final int UNDOCUMENTED_ROUTE_COUNT_CEILING = 41;

  @Test
  @SuppressWarnings("unchecked")
  void everyOperationInTheContractHasAControllerBehindIt() throws IOException {
    Set<String> declared = contractOperations();
    Set<String> built = controllerOperations();

    Set<String> withoutController = new TreeSet<>(declared);
    withoutController.removeAll(built);

    assertThat(declared).as("el contrato debe declarar operaciones").isNotEmpty();
    assertThat(withoutController)
        .as("operaciones del contrato SIN controller (el contrato solo describe lo construido)")
        .isEmpty();
  }

  @Test
  void reportsBuiltRoutesTheContractDoesNotDocumentYet() throws IOException {
    Set<String> undocumented = new TreeSet<>(controllerOperations());
    undocumented.removeAll(contractOperations());
    System.out.println("[contract] rutas construidas sin documentar: " + undocumented.size() + "\n  " + String.join("\n  ", undocumented));
    assertThat(undocumented.size()).isLessThanOrEqualTo(UNDOCUMENTED_ROUTE_COUNT_CEILING);
  }

  @SuppressWarnings("unchecked")
  private static Set<String> contractOperations() throws IOException {
    Map<String, Object> doc;
    try (InputStream in = Files.newInputStream(CONTRACT)) {
      doc = new Yaml().load(in);
    }
    String server = ((Map<String, Object>) ((java.util.List<?>) doc.get("servers")).get(0)).get("url").toString();
    Set<String> operations = new TreeSet<>();
    ((Map<String, Map<String, Object>>) doc.get("paths")).forEach((path, item) ->
        item.keySet().stream()
            .filter(k -> Set.of("get", "post", "put", "patch", "delete").contains(k))
            .forEach(method -> operations.add(method.toUpperCase() + " " + normalize(server + path))));
    return operations;
  }

  private static Set<String> controllerOperations() {
    StandardEnvironment env = new StandardEnvironment();
    env.getPropertySources().addFirst(new MapPropertySource("test", Map.of("app.api.private-path", "/api/llm")));
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    Set<String> operations = new TreeSet<>();
    for (BeanDefinition definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
      Class<?> controller = classOf(definition.getBeanClassName());
      RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
      String[] bases = classMapping == null || classMapping.path().length == 0 ? new String[] {""} : classMapping.path();
      for (Method method : controller.getDeclaredMethods()) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null) continue;
        String[] paths = mapping.path().length == 0 ? new String[] {""} : mapping.path();
        for (String base : bases) {
          for (String path : paths) {
            for (RequestMethod verb : mapping.method()) {
              operations.add(verb.name() + " " + normalize(env.resolvePlaceholders(base) + env.resolvePlaceholders(path)));
            }
          }
        }
      }
    }
    return operations;
  }

  private static String normalize(String path) {
    String slashed = path.replaceAll("/{2,}", "/");
    String noTrailing = slashed.length() > 1 && slashed.endsWith("/") ? slashed.substring(0, slashed.length() - 1) : slashed;
    return noTrailing.replaceAll("\\{[^/}]+}", "{}");
  }

  private static Class<?> classOf(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
