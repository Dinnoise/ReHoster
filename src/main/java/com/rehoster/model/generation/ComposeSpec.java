package com.rehoster.model.generation;

import java.util.ArrayList;
import java.util.List;

public class ComposeSpec {
    private String version;
    private List<ComposeService> services;

    public ComposeSpec() {
        this.version = "3.8";
        this.services = new ArrayList<>();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ComposeService> getServices() {
        return services;
    }

    public void setServices(List<ComposeService> services) {
        this.services = services;
    }

    public void addService(ComposeService service) {
        this.services.add(service);
    }
}
