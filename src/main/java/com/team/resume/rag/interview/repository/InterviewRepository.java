package com.team.resume.rag.interview.repository;

import com.team.resume.rag.interview.domain.ScheduledInterview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<ScheduledInterview, UUID> {

    List<ScheduledInterview> findByProfileContainingIgnoreCaseAndInterviewDate(String profile, LocalDate interviewDate);

    long countByProfileContainingIgnoreCaseAndInterviewDate(String profile, LocalDate interviewDate);
}
