package com.team.resume.rag.resume.dto;

import com.team.resume.rag.resume.domain.Candidate;

import java.time.Instant;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        String name,
        String email,
        String profile,
        int yearsOfExperience,
        String technologies,
        Double graduationCgpa,
        Double graduationPercentage,
        String resumeFileName,
        Instant createdAt
) {

    public static CandidateResponse from(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getProfile(),
                candidate.getYearsOfExperience(),
                candidate.getTechnologies(),
                candidate.getGraduationCgpa(),
                candidate.getGraduationPercentage(),
                candidate.getResumeFileName(),
                candidate.getCreatedAt()
        );
    }
}
