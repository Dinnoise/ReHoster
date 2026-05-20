package com.rehoster.model.analysis;

public class EnvVarSpec {
    private String key;
    private String defaultValue;
    private boolean required;
    private String description;

    public EnvVarSpec() {
    }

    public EnvVarSpec(String key, String defaultValue, boolean required) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.required = required;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
