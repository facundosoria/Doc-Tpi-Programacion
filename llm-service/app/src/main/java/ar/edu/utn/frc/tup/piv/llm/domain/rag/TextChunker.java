package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Segmentación de texto en fragmentos contextuales con solapamiento, respetando fronteras de
 * oraciones cuando es posible. Puro, sin Spring (doc 37 §1) — portado tal cual de
 * `demoLLMSpringAi/.../rag/service/TextChunkerService.java`. */
public final class TextChunker {
  private static final int DEFAULT_CHUNK_SIZE = 1000;
  private static final int DEFAULT_CHUNK_OVERLAP = 200;
  private static final int MIN_CHUNK_SIZE = 100;

  public List<DocumentChunk> createChunks(UUID documentId, String documentName, List<ExtractedPage> pages) {
    List<DocumentChunk> chunks = new ArrayList<>();
    int globalChunkIndex = 0;

    for (ExtractedPage page : pages) {
      String pageText = page.text();
      if (pageText == null || pageText.isBlank()) {
        continue;
      }

      for (String piece : splitIntoPieces(pageText, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP)) {
        if (piece.length() >= MIN_CHUNK_SIZE) {
          chunks.add(DocumentChunk.nuevo(documentId, documentName, page.pageNumber(), globalChunkIndex, piece));
          globalChunkIndex++;
        }
      }
    }
    return chunks;
  }

  private List<String> splitIntoPieces(String text, int targetSize, int overlap) {
    List<String> pieces = new ArrayList<>();
    int textLength = text.length();

    if (textLength <= targetSize) {
      pieces.add(text.trim());
      return pieces;
    }

    int start = 0;
    while (start < textLength) {
      int end = Math.min(start + targetSize, textLength);

      if (end < textLength) {
        int naturalBoundary = findNaturalBoundary(text, start, end);
        if (naturalBoundary > start + (targetSize / 2)) {
          end = naturalBoundary;
        }
      }

      String chunkContent = text.substring(start, end).trim();
      if (!chunkContent.isEmpty()) {
        pieces.add(chunkContent);
      }

      if (end >= textLength) {
        break;
      }
      start = Math.max(end - overlap, start + 1);
    }
    return pieces;
  }

  private int findNaturalBoundary(String text, int start, int end) {
    int paragraphBreak = text.lastIndexOf("\n\n", end);
    if (paragraphBreak > start + 150) return paragraphBreak + 2;

    int sentenceEnd = text.lastIndexOf(". ", end);
    if (sentenceEnd > start + 150) return sentenceEnd + 2;

    int questionEnd = text.lastIndexOf("? ", end);
    if (questionEnd > start + 150) return questionEnd + 2;

    int exclamationEnd = text.lastIndexOf("! ", end);
    if (exclamationEnd > start + 150) return exclamationEnd + 2;

    int lineBreak = text.lastIndexOf("\n", end);
    if (lineBreak > start + 100) return lineBreak + 1;

    int space = text.lastIndexOf(" ", end);
    if (space > start + 50) return space + 1;

    return end;
  }
}
