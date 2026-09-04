package com.example.demo.rag.service;

import com.example.demo.rag.model.DocumentChunk;
import com.example.demo.rag.model.RagDocumentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class InMemoryRagVectorStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRagVectorStore.class);

    // Stopwords comunes en español e inglés para optimizar vectores
    private static final Set<String> STOP_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no",
            "una", "su", "al", "lo", "como", "mas", "más", "pero", "sus", "le", "ya", "o", "este", "si", "porque",
            "esta", "son", "entre", "cuando", "muy", "sin", "sobre", "ser", "tiene", "tambien", "también",
            "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni",
            "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mi", "antes", "algunos", "unos", "yo",
            "otro", "otras", "otra", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual",
            "sea", "poco", "ella", "estar", "haber", "estas", "estaba", "estamos", "estan", "están",
            // English common
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with",
            "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up", "out", "if"
    )));

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}_]{2,}");

    // docId -> metadata
    private final Map<String, RagDocumentInfo> documents = new ConcurrentHashMap<>();

    // docId -> lista de chunks
    private final Map<String, List<DocumentChunk>> documentChunks = new ConcurrentHashMap<>();

    // docId -> IDF map (palabra -> log(N / DF))
    private final Map<String, Map<String, Double>> documentIdf = new ConcurrentHashMap<>();

    /**
     * Indexa los fragmentos de un documento generando sus vectores de términos TF-IDF normalizados.
     */
    public synchronized void indexDocument(RagDocumentInfo docInfo, List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("No hay fragmentos para indexar.");
        }

        String docId = docInfo.getDocumentId();
        int totalChunks = chunks.size();

        // 1. Calcular Document Frequency (DF) por palabra
        Map<String, Integer> documentFrequency = new HashMap<>();
        List<Map<String, Integer>> chunkTermFreqs = new ArrayList<>(totalChunks);

        for (DocumentChunk chunk : chunks) {
            Map<String, Integer> tf = extractTermFrequencies(chunk.getContent());
            chunkTermFreqs.add(tf);

            for (String term : tf.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        // 2. Calcular IDF suavizado
        Map<String, Double> idfMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            String term = entry.getKey();
            int df = entry.getValue();
            double idf = Math.log(1.0 + ((double) totalChunks / (double) df));
            idfMap.put(term, idf);
        }

        // 3. Calcular vector normalizado TF-IDF para cada chunk
        for (int i = 0; i < totalChunks; i++) {
            DocumentChunk chunk = chunks.get(i);
            Map<String, Integer> tfMap = chunkTermFreqs.get(i);

            Map<String, Double> tfIdfVector = new HashMap<>();
            double sumSquares = 0.0;

            for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
                String term = entry.getKey();
                int tf = entry.getValue();
                double idf = idfMap.getOrDefault(term, 0.0);
                double weight = (1.0 + Math.log(tf)) * idf;
                tfIdfVector.put(term, weight);
                sumSquares += weight * weight;
            }

            // Normalización unitaria (L2 norm) para cálculo veloz de similitud coseno
            double norm = Math.sqrt(sumSquares);
            if (norm > 0) {
                for (Map.Entry<String, Double> entry : tfIdfVector.entrySet()) {
                    entry.setValue(entry.getValue() / norm);
                }
            }

            chunk.setTermVector(tfIdfVector);
        }

        documents.put(docId, docInfo);
        documentChunks.put(docId, chunks);
        documentIdf.put(docId, idfMap);

        log.info("Indexación RAG completada para docId={}: {} chunks vectorizados, vocabulario={}",
                docId, totalChunks, idfMap.size());
    }

    /**
     * Búsqueda por similitud de coseno y relevancia léxica (Top-K chunks).
     */
    public List<DocumentChunk> searchTopK(String docId, String query, int k) {
        List<DocumentChunk> chunks = documentChunks.get(docId);
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> idfMap = documentIdf.getOrDefault(docId, Collections.emptyMap());
        Map<String, Integer> queryTf = extractTermFrequencies(query);

        if (queryTf.isEmpty()) {
            return chunks.stream().limit(k).collect(Collectors.toList());
        }

        // Vectorizar la consulta
        Map<String, Double> queryVector = new HashMap<>();
        double sumSquares = 0.0;

        for (Map.Entry<String, Integer> entry : queryTf.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();
            double idf = idfMap.getOrDefault(term, 0.5); // Peso base si es término nuevo
            double weight = (1.0 + Math.log(tf)) * idf;
            queryVector.put(term, weight);
            sumSquares += weight * weight;
        }

        double queryNorm = Math.sqrt(sumSquares);
        if (queryNorm > 0) {
            for (Map.Entry<String, Double> entry : queryVector.entrySet()) {
                entry.setValue(entry.getValue() / queryNorm);
            }
        }

        // Puntuación por similitud coseno con enriquecimiento semántico
        String queryLower = query.toLowerCase(Locale.ROOT).trim();
        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            Map<String, Double> chunkVector = chunk.getTermVector();
            if (chunkVector == null || chunkVector.isEmpty()) continue;

            double dotProduct = 0.0;
            int commonTerms = 0;

            for (Map.Entry<String, Double> qEntry : queryVector.entrySet()) {
                Double chunkWeight = chunkVector.get(qEntry.getKey());
                if (chunkWeight != null) {
                    dotProduct += qEntry.getValue() * chunkWeight;
                    commonTerms++;
                }
            }

            double finalScore = dotProduct + (commonTerms * 0.08);

            String contentLower = chunk.getContent().toLowerCase(Locale.ROOT);

            // Boost por coincidencia de frase o pregunta exacta
            if (contentLower.contains(queryLower)) {
                finalScore += 0.40;
            }

            // Boost conceptual si contiene patrones de definición explicativa
            boolean hasDefinitionClues = contentLower.contains("es un") ||
                    contentLower.contains("se define") ||
                    contentLower.contains("definición") ||
                    contentLower.contains("consiste en") ||
                    contentLower.contains("concepto");
            if (hasDefinitionClues && commonTerms > 0) {
                finalScore += 0.25;
            }

            // Penalización si es un índice o tabla de contenidos para priorizar la página de desarrollo real
            if (isTableOfContentsChunk(chunk.getContent())) {
                finalScore *= 0.30;
            }

            if (finalScore > 0.01) {
                scoredChunks.add(new ScoredChunk(chunk, finalScore));
            }
        }

        scoredChunks.sort((a, b) -> Double.compare(b.score(), a.score()));

        // Tomar top K y asignar score
        List<DocumentChunk> results = new ArrayList<>();
        int count = Math.min(k, scoredChunks.size());
        for (int i = 0; i < count; i++) {
            ScoredChunk sc = scoredChunks.get(i);
            DocumentChunk c = sc.chunk();
            c.setSimilarityScore(Math.round(sc.score() * 1000.0) / 1000.0);
            results.add(c);
        }

        // Si no hubo coincidencia estricta pero hay chunks, devolver el primer chunk de introducción como fallback
        if (results.isEmpty() && !chunks.isEmpty()) {
            DocumentChunk fallback = chunks.get(0);
            fallback.setSimilarityScore(0.05);
            results.add(fallback);
        }

        return results;
    }

    private boolean isTableOfContentsChunk(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase(Locale.ROOT);
        // Índices con puntos suspensivos o tablas de contenidos
        return content.contains(".....") ||
                (lower.contains("índice") && lower.contains("pág")) ||
                (lower.contains("contenido") && lower.contains("introducción") && lower.contains(".."));
    }

    public Optional<RagDocumentInfo> getDocument(String docId) {
        return Optional.ofNullable(documents.get(docId));
    }

    public List<DocumentChunk> getAllChunks(String docId) {
        return documentChunks.getOrDefault(docId, Collections.emptyList());
    }

    public boolean hasDocument(String docId) {
        return documents.containsKey(docId);
    }

    public void removeDocument(String docId) {
        documents.remove(docId);
        documentChunks.remove(docId);
        documentIdf.remove(docId);
    }

    private Map<String, Integer> extractTermFrequencies(String text) {
        Map<String, Integer> frequencies = new HashMap<>();
        if (text == null || text.isBlank()) return frequencies;

        var matcher = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = matcher.group();
            if (!STOP_WORDS.contains(word) && word.length() > 2) {
                frequencies.merge(word, 1, Integer::sum);
            }
        }
        return frequencies;
    }

    private record ScoredChunk(DocumentChunk chunk, double score) {}
}
