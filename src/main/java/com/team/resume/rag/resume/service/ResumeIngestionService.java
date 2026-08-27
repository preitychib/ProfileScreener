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

    private static final String EXTRACTION_PROMPT = """
            Extract candidate metadata from the resume text below.
            Return ONLY valid JSON with these fields:
            {
              "name": "full name",
              "email": "email or empty string",
              "profile": "job profile such as Java Developer or QA Engineer",
              "yearsOfExperience": 5,
              "technologies": "comma-separated technologies",
              "graduationCgpa": 8.2,
              "graduationPercentage": null
            }
            Extract graduationCgpa or graduationPercentage only from the bachelor's/graduation degree.
            Use null when a graduation score is not present. Do not use school grades.
            Resume text:
            %s
            """;

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
    }

    public IngestResult ingestAllFromDirectory() throws IOException {
        Path directory = resolveResumeDirectory();
        List<String> ingested = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (!Files.isDirectory(directory)) {
            Files.createDirectories(directory);
            return new IngestResult(ingested, skipped, errors);
        }

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .forEach(path -> ingestPath(path, ingested, skipped, errors));
        }

        return new IngestResult(ingested, skipped, errors);
    }

    public IngestResult ingestUploadedFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required");
        }
        if (!isPdf(file.getOriginalFilename())) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

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
        return new IngestResult(ingested, skipped, errors);
    }

    public List<Candidate> listCandidates() {
        return candidateService.findAll();
    }

    private void ingestPath(Path path, List<String> ingested, List<String> skipped, List<String> errors) {
        String fileName = path.getFileName().toString();
        if (candidateService.existsByResumeFileName(fileName)) {
            skipped.add(fileName);
            return;
        }

        try {
            Document document = FileSystemDocumentLoader.loadDocument(path, pdfParser);
            UUID candidateId = UUID.randomUUID();
            ExtractedCandidateMetadata extracted = extractMetadata(document.text(), fileName);
            Metadata metadata = Metadata.from(Map.of(
                    "candidateId", candidateId.toString(),
                    "fileName", fileName,
                    "candidateName", extracted.name(),
                    "profile", extracted.profile()
            ));
            Document enrichedDocument = Document.from(document.text(), metadata);
            embeddingStoreIngestor.ingest(enrichedDocument);

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
        } catch (Exception exception) {
            log.error("Failed to ingest resume {}", fileName, exception);
            errors.add(fileName + ": " + exception.getMessage());
        }
    }

    private ExtractedCandidateMetadata extractMetadata(String resumeText, String fileName) {
        try {
            String response = chatModel.chat(EXTRACTION_PROMPT.formatted(truncate(resumeText, 12000)));
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
