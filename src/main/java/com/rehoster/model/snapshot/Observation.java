package com.rehoster.model.snapshot;

import java.util.HashMap;
import java.util.Map;

public class Observation {
    
    public enum Severity {
        INFO, WARNING, ERROR
    }

    private String type;
    private Severity severity;
    private String message;
    private Map<String, Object> attributes;

    public Observation() {
        this.attributes = new HashMap<>();
    }

    public Observation(String type, Severity severity, String message) {
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.attributes = new HashMap<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }
}
