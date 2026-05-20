package com.rehoster.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rehoster.analysis.ServiceDependencyDetector.ServiceDependency;
import com.rehoster.model.analysis.AppDependencyModel;
import com.rehoster.model.analysis.EnvVarSpec;
import com.rehoster.model.generation.ComposeService;
import com.rehoster.model.generation.ComposeSpec;

public class ComposeGenerator {

    private List<ServiceDependency> serviceDependencies = new ArrayList<>();

    public void setServiceDependencies(List<ServiceDependency> dependencies) {
        this.serviceDependencies = dependencies;
    }

    public ComposeSpec createSpec(AppDependencyModel model, String serviceName) {
        ComposeSpec spec = new ComposeSpec();
        
        ComposeService service = new ComposeService();
        service.setServiceName(serviceName);
        service.setBuild(".");
        
        for (Integer port : model.getExposedPorts()) {
            service.getPorts().add(port + ":" + port);
        }
        
        for (EnvVarSpec env : model.getRequiredEnv()) {
            if (env.isRequired() || env.getDefaultValue() != null) {
                String value;
                if ("DB_HOST".equalsIgnoreCase(env.getKey()) || "DATABASE_HOST".equalsIgnoreCase(env.getKey())) {
                    value = "host.docker.internal";
                } else {
                    value = env.getDefaultValue() != null
                        ? normalizeDefaultValue(env.getKey(), env.getDefaultValue())
                        : "${" + env.getKey() + "}";
                }
                service.getEnvironment().put(env.getKey(), value);
            }
        }

        if (!serviceDependencies.isEmpty()) {
            String preferredDbService = resolvePreferredDbService(service.getEnvironment().get("DB_CONNECTION"));
            boolean dbApplied = false;
            List<ServiceDependency> appliedDeps = new ArrayList<>();
            for (ServiceDependency dep : serviceDependencies) {
                if (isDatabaseService(dep.getServiceName())) {
                    if (preferredDbService != null && !preferredDbService.equals(dep.getServiceName())) {
                        continue;
                    }
                    if (preferredDbService == null && dbApplied) {
                        continue;
                    }
                    updateAppEnvForDependency(service, dep);
                    appliedDeps.add(dep);
                    dbApplied = true;
                } else {
                    updateAppEnvForDependency(service, dep);
                    appliedDeps.add(dep);
                }
            }

            for (ServiceDependency dep : appliedDeps) {
                ComposeService depService = new ComposeService();
                depService.setServiceName(dep.getServiceName());
                depService.setImage(dep.getImage());

                if (dep.getDefaultPort() > 0) {
                    depService.getPorts().add(dep.getDefaultPort() + ":" + dep.getDefaultPort());
                }
                if (dep.getEnvironment() != null && !dep.getEnvironment().isEmpty()) {
                    depService.getEnvironment().putAll(dep.getEnvironment());
                }
                if (dep.getVolumes() != null && !dep.getVolumes().isEmpty()) {
                    depService.getVolumes().addAll(dep.getVolumes());
                }

                if ("kafka".equals(dep.getServiceName()) && containsService(appliedDeps, "zookeeper")) {
                    depService.getDependsOn().add("zookeeper");
                }

                spec.addService(depService);
                service.getDependsOn().add(dep.getServiceName());
            }
        }
        
        spec.addService(service);
        
        return spec;
    }

    private void updateAppEnvForDependency(ComposeService appService, ServiceDependency dep) {
        String serviceName = dep.getServiceName();
        
        if ("mysql".equals(serviceName)) {
            putIfMissingOrPlaceholder(appService.getEnvironment(), "DB_HOST", "mysql");
            putIfMissingOrPlaceholder(appService.getEnvironment(), "DB_PORT", String.valueOf(dep.getDefaultPort()));
            putIfMissingOrPlaceholder(appService.getEnvironment(), "DB_CONNECTION", "mysql");
            putIfMissingOrPlaceholder(appService.getEnvironment(), "DB_CHARSET", "utf8mb4");
            putIfMissingOrPlaceholder(appService.getEnvironment(), "DB_COLLATION", "utf8mb4_unicode_ci");
        } else if ("postgres".equals(serviceName)) {
            putIfMissingOrPlaceholder(appService.getEnvironment(), "DB_HOST", "postgres");
            putIfMissingOrPlaceholder(appService.getEnvironment(), "DB_PORT", String.valueOf(dep.getDefaultPort()));
            putIfMissingOrPlaceholder(appService.getEnvironment(), "DB_CONNECTION", "pgsql");
        } else if ("redis".equals(serviceName)) {
            putIfMissingOrPlaceholder(appService.getEnvironment(), "REDIS_HOST", "redis");
            putIfMissingOrPlaceholder(appService.getEnvironment(), "REDIS_PORT", String.valueOf(dep.getDefaultPort()));
        } else if ("mongo".equals(serviceName)) {
            putIfMissingOrPlaceholder(appService.getEnvironment(), "MONGO_HOST", "mongo");
            putIfMissingOrPlaceholder(appService.getEnvironment(), "MONGO_PORT", String.valueOf(dep.getDefaultPort()));
        } else if ("kafka".equals(serviceName)) {
            putIfMissingOrPlaceholder(appService.getEnvironment(), "KAFKA_BOOTSTRAP_SERVERS", "kafka:9092");
        }
    }

    private void putIfMissingOrPlaceholder(Map<String, String> env, String key, String value) {
        if (!env.containsKey(key)) {
            env.put(key, value);
            return;
        }
        String existing = env.get(key);
        if (existing == null || existing.trim().isEmpty() || existing.trim().startsWith("${") || "host.docker.internal".equalsIgnoreCase(existing.trim())) {
            env.put(key, value);
        }
    }

    private boolean containsService(List<ServiceDependency> deps, String name) {
        if (deps == null || name == null) {
            return false;
        }
        for (ServiceDependency d : deps) {
            if (d != null && name.equals(d.getServiceName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDatabaseService(String serviceName) {
        return "mysql".equals(serviceName) || "postgres".equals(serviceName);
    }

    private String resolvePreferredDbService(String dbConnectionValue) {
        if (dbConnectionValue == null) {
            return null;
        }
        String v = dbConnectionValue.trim().toLowerCase();
        if (v.isEmpty() || v.startsWith("${")) {
            return null;
        }
        if (v.contains("mysql") || v.contains("mariadb")) {
            return "mysql";
        }
        if (v.contains("pgsql") || v.contains("postgres")) {
            return "postgres";
        }
        return null;
    }

    private String normalizeDefaultValue(String key, String defaultValue) {
        if (defaultValue == null) {
            return null;
        }

        String v = defaultValue.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1).trim();
        }

        String expectedPrefix = "${" + key + ":-";
        if (v.startsWith(expectedPrefix) && v.endsWith("}")) {
            return v.substring(expectedPrefix.length(), v.length() - 1);
        }

        return defaultValue;
    }

    public String generate(AppDependencyModel model, String serviceName) {
        ComposeSpec spec = createSpec(model, serviceName);
        return generateFromSpec(spec);
    }

    public String generateFromSpec(ComposeSpec spec) {
        StringBuilder compose = new StringBuilder();
        
        compose.append("version: '").append(spec.getVersion()).append("'\n\n");
        compose.append("services:\n");
        
        for (ComposeService service : spec.getServices()) {
            compose.append("  ").append(service.getServiceName()).append(":\n");
            
            if (service.getBuild() != null) {
                compose.append("    build: ").append(service.getBuild()).append("\n");
            }
            
            if (service.getImage() != null) {
                compose.append("    image: ").append(service.getImage()).append("\n");
            }
            
            if (!service.getPorts().isEmpty()) {
                compose.append("    ports:\n");
                for (String port : service.getPorts()) {
                    compose.append("      - \"").append(port).append("\"\n");
                }
            }
            
            if (!service.getEnvironment().isEmpty()) {
                compose.append("    environment:\n");
                for (Map.Entry<String, String> env : service.getEnvironment().entrySet()) {
                    compose.append("      ").append(env.getKey()).append(": ")
                           .append(formatYamlValue(env.getValue())).append("\n");
                }
            }
            
            if (!service.getVolumes().isEmpty()) {
                compose.append("    volumes:\n");
                for (String volume : service.getVolumes()) {
                    compose.append("      - ").append(volume).append("\n");
                }
            }
            
            if (service.getWorkingDir() != null) {
                compose.append("    working_dir: ").append(service.getWorkingDir()).append("\n");
            }
            
            if (!service.getCommand().isEmpty()) {
                compose.append("    command: ").append(formatCommand(service.getCommand())).append("\n");
            }

            if (!service.getDependsOn().isEmpty()) {
                compose.append("    depends_on:\n");
                for (String dep : service.getDependsOn()) {
                    compose.append("      - ").append(dep).append("\n");
                }
            }
            
            compose.append("\n");
        }

        if (hasVolumes(spec)) {
            compose.append("volumes:\n");
            for (ComposeService service : spec.getServices()) {
                for (String volume : service.getVolumes()) {
                    String volumeName = extractVolumeName(volume);
                    if (volumeName != null) {
                        compose.append("  ").append(volumeName).append(":\n");
                    }
                }
            }
        }
        
        return compose.toString();
    }

    private boolean hasVolumes(ComposeSpec spec) {
        for (ComposeService service : spec.getServices()) {
            for (String volume : service.getVolumes()) {
                if (volume.contains(":") && !volume.startsWith(".") && !volume.startsWith("/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractVolumeName(String volume) {
        if (volume.contains(":")) {
            String name = volume.split(":")[0];
            if (!name.startsWith(".") && !name.startsWith("/") && !name.contains("\\")) {
                return name;
            }
        }
        return null;
    }

    private String formatYamlValue(String value) {
        if (value == null || value.isEmpty()) {
            return "\"\"";
        }
        String normalized = value.replace("\\", "/");
        if (normalized.contains(":") || normalized.contains("#") || normalized.contains("'") || 
            normalized.contains("\"") || normalized.startsWith(" ") || normalized.endsWith(" ")) {
            return "\"" + normalized.replace("\"", "\\\"") + "\"";
        }
        return normalized;
    }

    private String formatCommand(List<String> command) {
        if (command.size() == 1) {
            return command.get(0);
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < command.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(command.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
