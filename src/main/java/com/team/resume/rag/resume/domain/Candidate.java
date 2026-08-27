package com.team.resume.rag.resume.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String email;

    @Column(nullable = false)
    private String profile;

    @Column(nullable = false)
    private int yearsOfExperience;

    @Column(nullable = false, length = 2000)
    private String technologies;

    @Column
    private Double graduationCgpa;

    @Column
    private Double graduationPercentage;

    @Column(nullable = false)
    private String resumeFileName;

    @Column(nullable = false)
    private Instant createdAt;

    protected Candidate() {
    }

    public Candidate(UUID id,
                     String name,
                     String email,
                     String profile,
                     int yearsOfExperience,
                     String technologies,
                     String resumeFileName) {
        this(id, name, email, profile, yearsOfExperience, technologies, null, null, resumeFileName);
    }

    public Candidate(UUID id,
                     String name,
                     String email,
                     String profile,
                     int yearsOfExperience,
                     String technologies,
                     Double graduationCgpa,
                     Double graduationPercentage,
                     String resumeFileName) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profile = profile;
        this.yearsOfExperience = yearsOfExperience;
        this.technologies = technologies;
        this.graduationCgpa = graduationCgpa;
        this.graduationPercentage = graduationPercentage;
        this.resumeFileName = resumeFileName;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProfile() {
        return profile;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getTechnologies() {
        return technologies;
    }

    public Double getGraduationCgpa() {
        return graduationCgpa;
    }

    public Double getGraduationPercentage() {
        return graduationPercentage;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
