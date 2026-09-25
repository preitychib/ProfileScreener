package com.team.resume.rag.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.resume.rag.config.ResumeProperties;
import com.team.resume.rag.resume.domain.Candidate;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import com.team.resume.rag.resume.prompt.ExtractionPrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ResumeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ResumeIngestionService.class);


    private static final String EXTRACTION_PROMPT = ExtractionPrompt.EXTRACTION_PROMPT;

    private final ResumeProperties resumeProperties;
    private final CandidateService candidateService;
    private final EmbeddingStoreIngestor embeddingStoreIngestor;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final ApachePdfBoxDocumentParser pdfParser;

    public ResumeIngestionService(ResumeProperties resumeProperties,
                                  CandidateService candidateService,
                                  EmbeddingStoreIngestor embeddingStoreIngestor,
                                  ChatModel chatModel,
                                  ObjectMapper objectMapper) {
        this.resumeProperties = resumeProperties;
        this.candidateService = candidateService;
        this.embeddingStoreIngestor = embeddingStoreIngestor;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.pdfParser = new ApachePdfBoxDocumentParser();
        log.info("ResumeIngestionService ready: chatModel={}, embeddingStoreIngestor={}",
                chatModel.getClass().getName(),
                embeddingStoreIngestor.getClass().getName());
    }

    public IngestResult ingestAllFromDirectory() throws IOException {
        long startedAt = System.currentTimeMillis();
        Path directory = resolveResumeDirectory();
        log.info("Ingestion started from directory={}", directory);
        List<String> ingested = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (!Files.isDirectory(directory)) {
            Files.createDirectories(directory);
            log.info("Ingestion finished: directory was empty/missing. Took {} ms",
                    System.currentTimeMillis() - startedAt);
            return new IngestResult(ingested, skipped, errors);
        }

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .forEach(path -> ingestPath(path, ingested, skipped, errors));
        }

        log.info("Ingestion finished: ingested={}, skipped={}, errors={}, took {} ms",
                ingested.size(), skipped.size(), errors.size(), System.currentTimeMillis() - startedAt);
        return new IngestResult(ingested, skipped, errors);
    }

    public IngestResult ingestUploadedFile(MultipartFile file) throws IOException {
        long startedAt = System.currentTimeMillis();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required");
        }
        if (!isPdf(file.getOriginalFilename())) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        log.info("Ingestion started for upload fileName={}, sizeBytes={}",
                file.getOriginalFilename(), file.getSize());
        Path directory = resolveResumeDirectory();
        Files.createDirectories(directory);
        Path targetPath = directory.resolve(sanitizeFileName(file.getOriginalFilename()));
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        List<String> ingested = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        ingestPath(targetPath, ingested, skipped, errors);
        log.info("Ingestion finished for upload fileName={}, ingested={}, skipped={}, errors={}, took {} ms",
                file.getOriginalFilename(), ingested.size(), skipped.size(), errors.size(),
                System.currentTimeMillis() - startedAt);
        return new IngestResult(ingested, skipped, errors);
    }

    public List<Candidate> listCandidates() {
        return candidateService.findAll();
    }

    private void ingestPath(Path path, List<String> ingested, List<String> skipped, List<String> errors) {
        String fileName = path.getFileName().toString();
        if (candidateService.existsByResumeFileName(fileName)) {
            log.info("Skipping already-ingested resume {}", fileName);
            skipped.add(fileName);
            return;
        }

        long fileStartedAt = System.currentTimeMillis();
        log.info("Ingesting resume {}", fileName);
        try {
            long stepAt = System.currentTimeMillis();
            Document document = FileSystemDocumentLoader.loadDocument(path, pdfParser);
            log.info("PDF parsed for {}: chars={}, took {} ms",
                    fileName, document.text().length(), System.currentTimeMillis() - stepAt);

            UUID candidateId = UUID.randomUUID();
            stepAt = System.currentTimeMillis();
            ExtractedCandidateMetadata extracted = extractMetadata(document.text(), fileName);
            log.info("Metadata extracted for {}: name={}, profile={}, took {} ms",
                    fileName, extracted.name(), extracted.profile(), System.currentTimeMillis() - stepAt);

            Metadata metadata = Metadata.from(Map.of(
                    "candidateId", candidateId.toString(),
                    "fileName", fileName,
                    "candidateName", extracted.name(),
                    "profile", extracted.profile()
            ));
            Document enrichedDocument = Document.from(document.text(), metadata);

            stepAt = System.currentTimeMillis();
            embeddingStoreIngestor.ingest(enrichedDocument);
            log.info("Embeddings stored for {}: took {} ms", fileName, System.currentTimeMillis() - stepAt);

            Candidate candidate = new Candidate(
                    candidateId,
                    extracted.name(),
                    extracted.email(),
                    extracted.profile(),
                    extracted.yearsOfExperience(),
                    extracted.technologies(),
                    extracted.graduationCgpa(),
                    extracted.graduationPercentage(),
                    fileName
            );
            candidateService.save(candidate);
            ingested.add(fileName);
            log.info("Ingested resume {} in {} ms", fileName, System.currentTimeMillis() - fileStartedAt);
        } catch (Exception exception) {
            log.error("Failed to ingest resume {} after {} ms",
                    fileName, System.currentTimeMillis() - fileStartedAt, exception);
            errors.add(fileName + ": " + exception.getMessage());
        }
    }

    private ExtractedCandidateMetadata extractMetadata(String resumeText, String fileName) {
        try {
            log.info("Calling chatModel={} for metadata extraction of {}",
                    chatModel.getClass().getSimpleName(), fileName);
            long chatStartedAt = System.currentTimeMillis();
            String response = chatModel.chat(EXTRACTION_PROMPT.formatted(truncate(resumeText, 12000)));
            log.info("ChatModel response for {}: chars={}, took {} ms",
                    fileName,
                    response == null ? 0 : response.length(),
                    System.currentTimeMillis() - chatStartedAt);
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);
            return new ExtractedCandidateMetadata(
                    textValue(node, "name", fallbackName(fileName)),
                    textValue(node, "email", ""),
                    textValue(node, "profile", fallbackProfile(fileName)),
                    intValue(node, "yearsOfExperience", fallbackExperience(fileName)),
                    textValue(node, "technologies", ""),
                    doubleValue(node, "graduationCgpa"),
                    doubleValue(node, "graduationPercentage")
            );
        } catch (Exception exception) {
            log.warn("Metadata extraction failed for {}. Using filename fallback.", fileName, exception);
            return fallbackFromFileName(fileName);
        }
    }

    private ExtractedCandidateMetadata fallbackFromFileName(String fileName) {
        return new ExtractedCandidateMetadata(
                fallbackName(fileName),
                "",
                fallbackProfile(fileName),
                fallbackExperience(fileName),
                "",
                null,
                null
        );
    }

    private String fallbackName(String fileName) {
        String baseName = fileName.replace(".pdf", "").replace(".PDF", "");
        return baseName.replace('_', ' ').replace('-', ' ').trim();
    }

    private String fallbackProfile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.contains("java") || lower.contains("developer") || lower.contains("dev")) {
            return "Development";
        }
        if (lower.contains("qa") || lower.contains("test")) {
            return "QA";
        }
        return "General";
    }

    private int fallbackExperience(String fileName) {
        String digits = fileName.replaceAll("\\D+", " ").trim();
        if (digits.isBlank()) {
            return 0;
        }
        String[] parts = digits.split("\\s+");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        throw new IllegalArgumentException("No JSON found in extraction response");
    }

    private String textValue(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        return value.asText(defaultValue);
    }

    private int intValue(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        return value.asInt(defaultValue);
    }

    private Double doubleValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isNumber()) {
            return null;
        }
        return value.asDouble();
    }

    private Path resolveResumeDirectory() {
        return Paths.get(resumeProperties.directory()).toAbsolutePath().normalize();
    }

    private boolean isPdf(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private String sanitizeFileName(String fileName) {
        return Paths.get(fileName).getFileName().toString();
    }

    public record IngestResult(List<String> ingested, List<String> skipped, List<String> errors) {
    }

    private record ExtractedCandidateMetadata(
            String name,
            String email,
            String profile,
            int yearsOfExperience,
            String technologies,
            Double graduationCgpa,
            Double graduationPercentage
    ) {
    }
}
