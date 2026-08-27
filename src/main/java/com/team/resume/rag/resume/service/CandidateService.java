package com.team.resume.rag.resume.service;

import com.team.resume.rag.resume.domain.Candidate;
import com.team.resume.rag.resume.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Transactional
    public Candidate save(Candidate candidate) {
        return candidateRepository.save(candidate);
    }

    @Transactional(readOnly = true)
    public List<Candidate> findAll() {
        return candidateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long countByProfile(String profile) {
        return candidateRepository.countByProfileContainingIgnoreCase(profile);
    }

    @Transactional(readOnly = true)
    public List<Candidate> findByProfile(String profile) {
        return candidateRepository.findByProfileContainingIgnoreCase(profile);
    }

    @Transactional(readOnly = true)
    public List<Candidate> findByTechnology(String technology) {
        return candidateRepository.findByTechnologiesContainingIgnoreCase(technology);
    }

    @Transactional(readOnly = true)
    public Optional<Candidate> findByName(String name) {
        List<Candidate> candidates = candidateRepository.findByNameContainingIgnoreCase(name);
        return candidates.stream().findFirst();
    }

    @Transactional(readOnly = true)
    public boolean existsByResumeFileName(String resumeFileName) {
        return candidateRepository.existsByResumeFileName(resumeFileName);
    }

    @Transactional
    public void deleteById(UUID id) {
        candidateRepository.deleteById(id);
    }
}
