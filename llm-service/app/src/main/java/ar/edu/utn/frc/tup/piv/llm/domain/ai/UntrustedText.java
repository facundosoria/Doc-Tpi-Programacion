package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import java.util.regex.Pattern;

/** Separación estructural (capa 2, `docs/05-seguridad.md`): todo texto que no controlamos (lo que
 * escribe el alumno y lo que ya se dijo en turnos previos) viaja dentro de marcadores explícitos,
 * y la instrucción del sistema aclara que ese contenido es dato, nunca instrucción.
 *
 * <p>Para que el alumno no pueda cerrar el marcador y "salirse" del bloque, cualquier apertura o
 * cierre de nuestras propias etiquetas que aparezca dentro de su texto se neutraliza. El resto del
 * texto (código con `<`, `{`, comillas) queda intacto. */
public final class UntrustedText {
  public static final String STUDENT_TAG = "mensaje_alumno";
  public static final String HISTORY_TAG = "historial";
  public static final String TURN_TAG = "turno";

  private static final Pattern OWN_TAGS = Pattern.compile(
      "<(\\s*/?\\s*(?:" + STUDENT_TAG + "|" + HISTORY_TAG + "|" + TURN_TAG + ")\\b)",
      Pattern.CASE_INSENSITIVE);

  private UntrustedText() {}

  /** Rompe cualquier etiqueta propia escrita dentro del texto del alumno (`<` pasa a `‹`). */
  public static String neutralize(String text) {
    if (text == null) return "";
    return OWN_TAGS.matcher(text).replaceAll("‹$1");
  }

  public static String studentMessage(String text) {
    return "<" + STUDENT_TAG + ">\n" + neutralize(text) + "\n</" + STUDENT_TAG + ">";
  }

  /** Un turno previo. `rol` viene de la base ("alumno"/"tutor"); cualquier otro valor se degrada
   * a "otro" para que no pueda inyectar atributos. */
  public static String historyTurn(String rol, String content) {
    String safeRole = ("alumno".equals(rol) || "tutor".equals(rol)) ? rol : "otro";
    return "<" + TURN_TAG + " rol=\"" + safeRole + "\">\n" + neutralize(content) + "\n</" + TURN_TAG + ">";
  }
}
