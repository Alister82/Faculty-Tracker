package com.college.tracker;

public class CertificateExtractDto {
    private String facultyName;
    private String eventName;
    private String startDate;
    private String endDate;
    private String duration;
    private Boolean isNittt;
    private String hostInstitute;
    private String location;
    private String journalName;
    private String journalType;
    private String publicationLevel;

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public Boolean getIsNittt() { return isNittt; }
    public void setIsNittt(Boolean isNittt) { this.isNittt = isNittt; }

    public String getHostInstitute() { return hostInstitute; }
    public void setHostInstitute(String hostInstitute) { this.hostInstitute = hostInstitute; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getJournalName() { return journalName; }
    public void setJournalName(String journalName) { this.journalName = journalName; }

    public String getJournalType() { return journalType; }
    public void setJournalType(String journalType) { this.journalType = journalType; }

    public String getPublicationLevel() { return publicationLevel; }
    public void setPublicationLevel(String publicationLevel) { this.publicationLevel = publicationLevel; }
}
