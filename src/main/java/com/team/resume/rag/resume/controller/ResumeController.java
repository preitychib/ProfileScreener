package com.team.resume.rag.resume.controller;

import com.team.resume.rag.resume.dto.CandidateResponse;
import com.team.resume.rag.resume.dto.IngestResponse;
import com.team.resume.rag.resume.dto.ResumeQueryRequest;
import com.team.resume.rag.resume.dto.ResumeQueryResponse;
import com.team.resume.rag.resume.service.ResumeIngestionService;
import com.team.resume.rag.resume.service.ResumeRagQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeIngestionService resumeIngestionService;
    private final ResumeRagQueryService resumeRagQueryService;

    public ResumeController(ResumeIngestionService resumeIngestionService,
                            ResumeRagQueryService resumeRagQueryService) {
        this.resumeIngestionService = resumeIngestionService;
        this.resumeRagQueryService = resumeRagQueryService;
    }

    @PostMapping("/ingest")
    public IngestResponse ingestAll() {
        try {
            ResumeIngestionService.IngestResult result = resumeIngestionService.ingestAllFromDirectory();
            return new IngestResponse(result.ingested(), result.skipped(), result.errors());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to ingest resumes", exception);
        }
    }

    @PostMapping("/upload")
    public IngestResponse upload(@RequestParam("file") MultipartFile file) {
        try {
            ResumeIngestionService.IngestResult result = resumeIngestionService.ingestUploadedFile(file);
            return new IngestResponse(result.ingested(), result.skipped(), result.errors());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload resume", exception);
        }
    }

    @GetMapping("/candidates")
    public List<CandidateResponse> listCandidates() {
        return resumeIngestionService.listCandidates().stream()
                .map(CandidateResponse::from)
                .toList();
    }

    @PostMapping("/query")
    public ResumeQueryResponse query(@RequestBody ResumeQueryRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }
        return resumeRagQueryService.query(request.query().trim());
    }
}
