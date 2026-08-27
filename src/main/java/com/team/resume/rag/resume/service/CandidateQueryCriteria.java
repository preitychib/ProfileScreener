package com.team.resume.rag.resume.service;

import java.util.List;

public record CandidateQueryCriteria(
        List<String> requiredTechnologies,
        Integer minimumYearsExperience,
        boolean experienceInclusive,
        Double minimumGraduationCgpa,
        boolean cgpaInclusive,
        Double minimumGraduationPercentage,
        boolean percentageInclusive,
        EducationScoreOperator educationScoreOperator
) {

    public CandidateQueryCriteria {
        requiredTechnologies = requiredTechnologies == null ? List.of() : List.copyOf(requiredTechnologies);
        educationScoreOperator = educationScoreOperator == null ? EducationScoreOperator.ANY : educationScoreOperator;
    }

    public boolean hasFilters() {
        return !requiredTechnologies.isEmpty()
                || minimumYearsExperience != null
                || minimumGraduationCgpa != null
                || minimumGraduationPercentage != null;
    }
}
