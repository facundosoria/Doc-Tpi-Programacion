package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.guard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.ParameterizedTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputAntiLeakGuardTest {

    private OutputAntiLeakGuard guard;

    @BeforeEach
    void setUp() {
        guard = new OutputAntiLeakGuard();
    }

    @Test
    void debeDetectarFugaCuandoContieneSolucionExacta() {
        String respuesta = "Para resolverlo puedes hacer return a + b;";
        String solucion = "return a + b;";
        assertTrue(guard.contieneFuga(respuesta, solucion));
    }

    @Test
    void debeDetectarFugaCuandoContieneBloqueDeCodigoExtenso() {
        String respuestaConMuchoCodigo = """
                Aquí tienes la solución:
                ```java
                public class Solucion {
                    public static void main(String[] args) {
                        int[] nums = {1, 2, 3};
                        for (int i = 0; i < nums.length; i++) {
                            System.out.println(nums[i]);
                        }
                        System.out.println("Fin");
                    }
                }
                ```
                Espero te sirva.
                """;
        assertTrue(guard.contieneFuga(respuestaConMuchoCodigo, null));
    }

    @Test
    void debeAceptarRespuestaPedagogicaSinFuga() {
        String respuesta = "¿Qué estructura de datos crees que te permitiría almacenar los elementos de manera ordenada?";
        assertFalse(guard.contieneFuga(respuesta, "List<Integer> list = new ArrayList<>();"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void debeManejarRespuestaNulaOVacia(String vacia) {
        assertFalse(guard.contieneFuga(vacia, "solucion"));
    }
}
