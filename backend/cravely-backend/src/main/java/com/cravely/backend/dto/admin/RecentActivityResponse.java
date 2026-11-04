package com.cravely.backend.dto.admin;

import java.time.LocalDateTime;

public class RecentActivityResponse {

    private String id;
    private String type;
    private String title;
    private String description;
    private String actor;
    private String actorRole;
    private LocalDateTime timestamp;
    private String referenceId;
    private String referenceType;

    public RecentActivityResponse() {
    }

    public RecentActivityResponse(String id, String type, String title, String description,
                                  String actor, String actorRole, LocalDateTime timestamp,
                                  String referenceId, String referenceType) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.actorRole = actorRole;
        this.timestamp = timestamp;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
}
