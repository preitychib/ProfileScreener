package com.team.resume.rag.interview.service;

import com.team.resume.rag.interview.domain.ScheduledInterview;
import com.team.resume.rag.interview.repository.InterviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;

    public InterviewService(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    @Transactional
    public ScheduledInterview scheduleInterview(String candidateName,
                                                String candidateEmail,
                                                String profile,
                                                int yearsOfExperience,
                                                LocalDate interviewDate,
                                                LocalTime interviewTime) {
        ScheduledInterview interview = new ScheduledInterview(
                candidateName,
                candidateEmail,
                profile,
                yearsOfExperience,
                interviewDate,
                interviewTime
        );
        return interviewRepository.save(interview);
    }

    @Transactional(readOnly = true)
    public List<ScheduledInterview> findByProfileAndDate(String profile, LocalDate interviewDate) {
        return interviewRepository.findByProfileContainingIgnoreCaseAndInterviewDate(profile, interviewDate);
    }

    @Transactional(readOnly = true)
    public long countByProfileAndDate(String profile, LocalDate interviewDate) {
        return interviewRepository.countByProfileContainingIgnoreCaseAndInterviewDate(profile, interviewDate);
    }
}
