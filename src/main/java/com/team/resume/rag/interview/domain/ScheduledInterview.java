package com.team.resume.rag.interview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_interviews")
public class ScheduledInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String candidateName;

    @Column
    private String candidateEmail;

    @Column(nullable = false)
    private String profile;

    @Column(nullable = false)
    private int yearsOfExperience;

    @Column(nullable = false)
    private LocalDate interviewDate;

    @Column(nullable = false)
    private LocalTime interviewTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected ScheduledInterview() {
    }

    public ScheduledInterview(String candidateName,
                              String candidateEmail,
                              String profile,
                              int yearsOfExperience,
                              LocalDate interviewDate,
                              LocalTime interviewTime) {
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.profile = profile;
        this.yearsOfExperience = yearsOfExperience;
        this.interviewDate = interviewDate;
        this.interviewTime = interviewTime;
        this.status = InterviewStatus.SCHEDULED;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public String getProfile() {
        return profile;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public LocalDate getInterviewDate() {
        return interviewDate;
    }

    public LocalTime getInterviewTime() {
        return interviewTime;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
