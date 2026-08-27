package com.team.resume.rag.resume.dto;

public record RetrievedChunk(
        String text,
        Double score,
        String fileName,
        String candidateId,
        String candidateName
) {
}
