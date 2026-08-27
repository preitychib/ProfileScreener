package com.team.resume.rag.resume.dto;

import java.util.List;
import java.util.UUID;

public record CandidateMatch(
        UUID candidateId,
        String name,
        String profile,
        int yearsOfExperience,
        String technologies,
        Double graduationCgpa,
        Double graduationPercentage,
        List<String> matchedOn
) {
}
