package com.team.resume.rag.resume.dto;

import java.util.List;

public record IngestResponse(
        List<String> ingested,
        List<String> skipped,
        List<String> errors
) {
}
