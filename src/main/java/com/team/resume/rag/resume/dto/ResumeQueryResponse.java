package com.team.resume.rag.resume.dto;

import java.util.List;

public record ResumeQueryResponse(
        String query,
        String answer,
        List<RetrievedChunk> retrievedChunks,
        List<CandidateMatch> matchedCandidates
) {
}
