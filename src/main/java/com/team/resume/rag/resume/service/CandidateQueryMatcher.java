package com.team.resume.rag.resume.service;

import com.team.resume.rag.resume.domain.Candidate;
import com.team.resume.rag.resume.dto.CandidateMatch;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class CandidateQueryMatcher {

    public List<CandidateMatch> match(List<Candidate> candidates, CandidateQueryCriteria criteria) {
        return candidates.stream()
                .filter(candidate -> matchesTechnologies(candidate, criteria.requiredTechnologies()))
                .filter(candidate -> matchesExperience(candidate, criteria))
                .filter(candidate -> matchesEducation(candidate, criteria))
                .map(candidate -> toMatch(candidate, criteria))
                .toList();
    }

    private boolean matchesTechnologies(Candidate candidate, List<String> requiredTechnologies) {
        String technologies = candidate.getTechnologies().toLowerCase(Locale.ROOT);
        return requiredTechnologies.stream()
                .map(technology -> technology.toLowerCase(Locale.ROOT))
                .allMatch(technologies::contains);
    }

    private boolean matchesExperience(Candidate candidate, CandidateQueryCriteria criteria) {
        Integer minimum = criteria.minimumYearsExperience();
        if (minimum == null) {
            return true;
        }
        return criteria.experienceInclusive()
                ? candidate.getYearsOfExperience() >= minimum
                : candidate.getYearsOfExperience() > minimum;
    }

    private boolean matchesEducation(Candidate candidate, CandidateQueryCriteria criteria) {
        boolean hasCgpaFilter = criteria.minimumGraduationCgpa() != null;
        boolean hasPercentageFilter = criteria.minimumGraduationPercentage() != null;
        if (!hasCgpaFilter && !hasPercentageFilter) {
            return true;
        }

        boolean cgpaMatches = hasCgpaFilter && meetsMinimum(
                candidate.getGraduationCgpa(),
                criteria.minimumGraduationCgpa(),
                criteria.cgpaInclusive()
        );
        boolean percentageMatches = hasPercentageFilter && meetsMinimum(
                candidate.getGraduationPercentage(),
                criteria.minimumGraduationPercentage(),
                criteria.percentageInclusive()
        );

        if (hasCgpaFilter && hasPercentageFilter) {
            return criteria.educationScoreOperator() == EducationScoreOperator.ALL
                    ? cgpaMatches && percentageMatches
                    : cgpaMatches || percentageMatches;
        }
        return hasCgpaFilter ? cgpaMatches : percentageMatches;
    }

    private boolean meetsMinimum(Double actual, Double minimum, boolean inclusive) {
        if (actual == null) {
            return false;
        }
        return inclusive ? actual >= minimum : actual > minimum;
    }

    private CandidateMatch toMatch(Candidate candidate, CandidateQueryCriteria criteria) {
        List<String> matchedOn = new ArrayList<>();
        if (!criteria.requiredTechnologies().isEmpty()) {
            matchedOn.add("technologies: " + String.join(", ", criteria.requiredTechnologies()));
        }
        if (criteria.minimumYearsExperience() != null) {
            matchedOn.add("experience: " + candidate.getYearsOfExperience() + " years");
        }
        if (criteria.minimumGraduationCgpa() != null && candidate.getGraduationCgpa() != null) {
            matchedOn.add("graduation CGPA: " + candidate.getGraduationCgpa());
        }
        if (criteria.minimumGraduationPercentage() != null && candidate.getGraduationPercentage() != null) {
            matchedOn.add("graduation percentage: " + candidate.getGraduationPercentage() + "%");
        }
        return new CandidateMatch(
                candidate.getId(),
                candidate.getName(),
                candidate.getProfile(),
                candidate.getYearsOfExperience(),
                candidate.getTechnologies(),
                candidate.getGraduationCgpa(),
                candidate.getGraduationPercentage(),
                List.copyOf(matchedOn)
        );
    }
}
