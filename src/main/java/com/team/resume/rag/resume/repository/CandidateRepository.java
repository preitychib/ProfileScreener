package com.team.resume.rag.resume.repository;

import com.team.resume.rag.resume.domain.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    long countByProfileContainingIgnoreCase(String profile);

    List<Candidate> findByProfileContainingIgnoreCase(String profile);

    List<Candidate> findByTechnologiesContainingIgnoreCase(String technology);

    List<Candidate> findByNameContainingIgnoreCase(String name);

    boolean existsByResumeFileName(String resumeFileName);
}
