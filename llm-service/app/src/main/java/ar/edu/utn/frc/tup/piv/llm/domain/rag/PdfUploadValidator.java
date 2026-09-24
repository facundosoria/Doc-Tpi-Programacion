package ar.edu.utn.frc.tup.piv.llm.domain.rag;

/** Validación de una fuente antes de indexarla (#669). Pura y sin Spring: son reglas de negocio
 * sobre qué se acepta como material de cátedra, no un detalle de transporte.
 *
 * <p>Deliberadamente NO valida por extensión ni por el `Content-Type` que manda el cliente: ambos
 * los elige quien sube el archivo. Un `.docx` renombrado a `.pdf` se detecta por su firma binaria
 * (`%PDF-`), y lo que la firma no alcanza a distinguir (un PDF cifrado o corrupto) lo detecta
 * PDFBox al abrirlo, en {@link PdfTextExtractionPort}. */
public final class PdfUploadValidator {
  /** Todo PDF empieza con `%PDF-` (ISO 32000-1 §7.5.2). */
  private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

  private final long maxUploadBytes;

  public PdfUploadValidator(long maxUploadBytes) {
    this.maxUploadBytes = maxUploadBytes;
  }

  /** Valida el tamaño declarado antes de leer el archivo entero en memoria. */
  public void validateDeclaredSize(long sizeInBytes, String fileName) {
    if (sizeInBytes <= 0) {
      throw new InvalidPdfSourceException(describe(fileName) + " está vacío: no tiene contenido para indexar.");
    }
    if (sizeInBytes > maxUploadBytes) {
      throw new InvalidPdfSourceException(describe(fileName) + " pesa " + megabytes(sizeInBytes)
          + " MB y supera el máximo permitido de " + megabytes(maxUploadBytes) + " MB.");
    }
  }

  /** Valida el contenido ya cargado: tamaño real y firma binaria de PDF. */
  public void validateContent(byte[] bytes, String fileName) {
    validateDeclaredSize(bytes == null ? 0 : bytes.length, fileName);
    if (!hasPdfSignature(bytes)) {
      throw new InvalidPdfSourceException(describe(fileName)
          + " no es un PDF: su contenido no empieza con la firma '%PDF-'. Verificá que no sea otro"
          + " formato renombrado con extensión .pdf.");
    }
  }

  private boolean hasPdfSignature(byte[] bytes) {
    if (bytes.length < PDF_MAGIC.length) {
      return false;
    }
    for (int i = 0; i < PDF_MAGIC.length; i++) {
      if (bytes[i] != PDF_MAGIC[i]) {
        return false;
      }
    }
    return true;
  }

  private static String describe(String fileName) {
    return (fileName == null || fileName.isBlank()) ? "El archivo" : "El archivo '" + fileName + "'";
  }

  private static long megabytes(long bytes) {
    return Math.max(1, Math.round(bytes / (1024.0 * 1024.0)));
  }
}
