package com.team.resume.rag.candidate.tool;

import com.team.resume.rag.resume.domain.Candidate;
import com.team.resume.rag.resume.service.CandidateService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CandidateTools {

    private final CandidateService candidateService;

    public CandidateTools(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @Tool("Count candidates matching a job profile")
    public String countCandidatesByProfile(@P("Job profile, e.g. Development or Java Developer") String profile) {
        long count = candidateService.countByProfile(profile);
        if (count == 0) {
            return "No candidates found for profile '%s'.".formatted(profile);
        }
        return "%d candidate(s) found for profile '%s'.".formatted(count, profile);
    }

    @Tool("Find candidates with hands-on experience in a specific technology")
    public String findCandidatesWithTechnology(@P("Technology name, e.g. Spring Boot or Java") String technology) {
        List<Candidate> candidates = candidateService.findByTechnology(technology);
        if (candidates.isEmpty()) {
            return "No candidates found with technology '%s'.".formatted(technology);
        }

        String details = candidates.stream()
                .map(this::formatCandidate)
                .collect(Collectors.joining("\n"));
        return "%d candidate(s) with '%s' experience:\n%s".formatted(candidates.size(), technology, details);
    }

    @Tool("List all ingested candidates with their profile and experience summary")
    public String listAllCandidates() {
        List<Candidate> candidates = candidateService.findAll();
        if (candidates.isEmpty()) {
            return "No candidates have been ingested yet.";
        }

        String details = candidates.stream()
                .map(this::formatCandidate)
                .collect(Collectors.joining("\n"));
        return "%d candidate(s) ingested:\n%s".formatted(candidates.size(), details);
    }

    private String formatCandidate(Candidate candidate) {
        return "- %s | profile: %s | experience: %d years | technologies: %s | email: %s".formatted(
                candidate.getName(),
                candidate.getProfile(),
                candidate.getYearsOfExperience(),
                candidate.getTechnologies(),
                candidate.getEmail()
        );
    }
}
