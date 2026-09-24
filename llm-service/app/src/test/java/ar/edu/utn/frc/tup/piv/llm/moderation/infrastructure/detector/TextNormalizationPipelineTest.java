package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizationPipelineTest {

    private TextNormalizationPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new TextNormalizationPipeline();
    }

    @Test
    @DisplayName("T1: Variantes con tildes, separadores y leet speak normalizan al mismo término canónico")
    void evasionsNormalizeToSameCanonicalTerm() {
        String canonicalExpected = "pelotudo";

        String accented = pipeline.normalize("pélótúdó");
        String dotted = pipeline.normalize("p.e.l.o.t.u.d.o");
        String leet = pipeline.normalize("p3l0tud0");

        assertThat(accented).isEqualTo(canonicalExpected);
        assertThat(dotted).isEqualTo(canonicalExpected);
        assertThat(leet).isEqualTo(canonicalExpected);
    }

    @Test
    @DisplayName("T1: Letras unidas con guiones se normalizan correctamente ('p-e-l-o' -> 'pelo')")
    void hyphenSeparatedLettersNormalize() {
        String result = pipeline.normalize("p-e-l-o");
        assertThat(result).isEqualTo("pelo");
    }

    @Test
    @DisplayName("T1: Caracteres repetidos se colapsan ('holaaaa' -> 'hola')")
    void repeatingCharactersCollapse() {
        String result = pipeline.normalize("holaaaa");
        assertThat(result).isEqualTo("hola");

        String elongatedInsult = pipeline.normalize("pelotuuuudo");
        assertThat(elongatedInsult).isEqualTo("pelotudo");
    }

    @Test
    @DisplayName("T1: Homoglifos cirílicos comunes se mapean a caracteres latinos")
    void cyrillicHomoglyphsMapToLatin() {
        // 'р' (U+0440 cirílico) y 'е' (U+0435 cirílico)
        String cyrillicInput = "\u0440\u0435lotudo";
        String normalized = pipeline.normalize(cyrillicInput);
        assertThat(normalized).isEqualTo("pelotudo");
    }

    @Test
    @DisplayName("T1: Letras separadas por espacios se colapsan ('p e l o t u d o' -> 'pelotudo')")
    void spaceSeparatedLettersCollapse() {
        String result = pipeline.normalize("p e l o t u d o");
        assertThat(result).isEqualTo("pelotudo");
    }

    @Test
    @DisplayName("T1: Palabras legítimas con letras dobles en español se preservan")
    void legitimateDoubleLettersPreserved() {
        assertThat(pipeline.normalize("perro")).isEqualTo("perro");
        assertThat(pipeline.normalize("calle")).isEqualTo("calle");
        assertThat(pipeline.normalize("accion")).isEqualTo("accion");
        assertThat(pipeline.normalize("leer")).isEqualTo("leer");
        assertThat(pipeline.normalize("coordinar")).isEqualTo("coordinar");
    }

    @Test
    @DisplayName("T1: Rendimiento < 2 ms en la normalización de texto")
    void performanceLessThan2Ms() {
        String text = "Hola profesor, ¿p-u-e-d-o hacer una consulta sobre p3l0tud0 y el cálculo?";

        // Calentamiento JIT
        for (int i = 0; i < 50; i++) {
            pipeline.normalize(text);
        }

        long start = System.nanoTime();
        String result = pipeline.normalize(text);
        long elapsedNanos = System.nanoTime() - start;
        long elapsedMs = elapsedNanos / 1_000_000;

        assertThat(elapsedMs).isLessThan(2L);
        assertThat(result).isNotBlank();
    }

    @Test
    @DisplayName("T1: Manejo de nulo, vacío y cobertura completa de mapeos leet y homoglifos")
    void nullBlankAndAllHomoglyphsCovered() {
        assertThat(pipeline.normalize(null)).isEmpty();
        assertThat(pipeline.normalize("   ")).isEmpty();

        String allChars = "0 1 ! | 3 4 @ 5 $ 7 \u0430 \u0435 \u043E \u0440 \u0441 \u0443 \u0445 \u0456";
        String normalized = pipeline.normalize(allChars);
        assertThat(normalized).isEqualTo("oiiieaasstaeopcyxi");
    }
}
