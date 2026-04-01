package com.college.tracker;

import jakarta.persistence.*;
import com.college.tracker.security.ApprovalStatus;

@Entity
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String activityType; // FDP, Workshop, Conference, Publication
    private String facultyName;
    private String title;
    private String date;
    private String hostInstitute;
    private String location;
    private String duration;
    private boolean nitttCertified;
    private String journalName;
    private String journalType;
    private String publicationLevel;

    // NEW: This holds the Google Drive Link!
    @Column(length = 1000)
    private String certificateUrl;

    // RBAC & Workflow Fields
    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;

    private String createdByUsername;
    
    private String department;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHostInstitute() {
        return hostInstitute;
    }

    public void setHostInstitute(String hostInstitute) {
        this.hostInstitute = hostInstitute;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean isNitttCertified() {
        return nitttCertified;
    }

    public void setNitttCertified(boolean nitttCertified) {
        this.nitttCertified = nitttCertified;
    }

    public String getJournalName() {
        return journalName;
    }

    public void setJournalName(String journalName) {
        this.journalName = journalName;
    }

    public String getJournalType() {
        return journalType;
    }

    public void setJournalType(String journalType) {
        this.journalType = journalType;
    }

    public String getPublicationLevel() {
        return publicationLevel;
    }

    public void setPublicationLevel(String publicationLevel) {
        this.publicationLevel = publicationLevel;
    }

    public String getCertificateUrl() {
        return certificateUrl;
    }

    public void setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }    
}