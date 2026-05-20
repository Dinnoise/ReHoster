package com.rehoster.generation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rehoster.model.analysis.AppDependencyModel;
import com.rehoster.model.analysis.EnvVarSpec;
import com.rehoster.model.generation.DockerfileSpec;

public class DockerfileGenerator {

    private static final Map<String, String> BASE_IMAGES = new HashMap<>();
    
    static {
        BASE_IMAGES.put("java", "eclipse-temurin:11-jre");
        BASE_IMAGES.put("python", "python:3.11-slim");
        BASE_IMAGES.put("nodejs", "node:18-alpine");
        BASE_IMAGES.put("dotnet", "mcr.microsoft.com/dotnet/runtime:7.0");
        BASE_IMAGES.put("golang", "golang:1.21-alpine");
        BASE_IMAGES.put("php", "php:8.2-cli");
        BASE_IMAGES.put("generic", "ubuntu:22.04");
    }

    public DockerfileSpec createSpec(AppDependencyModel model) {
        DockerfileSpec spec = new DockerfileSpec();
        
        String appType = detectType(model);
        spec.setBaseImage(BASE_IMAGES.getOrDefault(appType, BASE_IMAGES.get("generic")));
        
        spec.setWorkDir("/app");

        addSystemDependencies(spec);
        
        spec.getCopyRules().add(". .");

        addAppDependencies(spec);
        
        for (EnvVarSpec env : model.getRequiredEnv()) {
            if (env.isRequired() && env.getDefaultValue() != null) {
                spec.getEnvVars().put(env.getKey(), env.getDefaultValue());
            }
        }
        
        spec.setExposePorts(model.getExposedPorts());

        if ("php".equals(appType) && isLaravelArtisanServe(model.getEntrypoint())) {
            int port = 8000;
            if (spec.getExposePorts() != null && !spec.getExposePorts().isEmpty() && spec.getExposePorts().get(0) != null) {
                port = spec.getExposePorts().get(0);
            }
            spec.setCmd(Arrays.asList("php", "artisan", "serve", "--host=0.0.0.0", "--port=" + port));
        } else if (!model.getEntrypoint().isEmpty()) {
            spec.setCmd(model.getEntrypoint());
        }
        
        return spec;
    }

    public String generate(AppDependencyModel model) {
        DockerfileSpec spec = createSpec(model);
        return generateFromSpec(spec);
    }

    public String generate(AppDependencyModel model, Path projectDir) {
        if (isMavenJavaProject(model, projectDir)) {
            return generateMavenMultiStageDockerfile(model, projectDir);
        }
        return generate(model);
    }

    public String generateFromSpec(DockerfileSpec spec) {
        StringBuilder dockerfile = new StringBuilder();
        
        dockerfile.append("FROM ").append(spec.getBaseImage()).append("\n\n");
        
        dockerfile.append("WORKDIR ").append(spec.getWorkDir()).append("\n\n");
        
        for (String runCmd : spec.getRunCommands()) {
            dockerfile.append("RUN ").append(runCmd).append("\n");
        }
        if (!spec.getRunCommands().isEmpty()) {
            dockerfile.append("\n");
        }
        
        for (String copyRule : spec.getCopyRules()) {
            dockerfile.append("COPY ").append(copyRule).append("\n");
        }
        if (!spec.getCopyRules().isEmpty()) {
            dockerfile.append("\n");
        }

        for (String runCmd : spec.getPostCopyRunCommands()) {
            dockerfile.append("RUN ").append(runCmd).append("\n");
        }
        if (!spec.getPostCopyRunCommands().isEmpty()) {
            dockerfile.append("\n");
        }
        
        for (Map.Entry<String, String> env : spec.getEnvVars().entrySet()) {
            dockerfile.append("ENV ").append(env.getKey()).append("=").append(env.getValue()).append("\n");
        }
        if (!spec.getEnvVars().isEmpty()) {
            dockerfile.append("\n");
        }
        
        for (Integer port : spec.getExposePorts()) {
            dockerfile.append("EXPOSE ").append(port).append("\n");
        }
        if (!spec.getExposePorts().isEmpty()) {
            dockerfile.append("\n");
        }
        
        if (!spec.getEntrypoint().isEmpty()) {
            dockerfile.append("ENTRYPOINT ").append(formatAsJsonArray(spec.getEntrypoint())).append("\n");
        }
        
        if (!spec.getCmd().isEmpty()) {
            dockerfile.append("CMD ").append(formatAsJsonArray(spec.getCmd())).append("\n");
        }
        
        return dockerfile.toString();
    }

    private boolean hasMavenProfile(Path pomPath, String profileId) {
        if (pomPath == null || profileId == null || profileId.trim().isEmpty()) {
            return false;
        }
        try {
            if (!Files.exists(pomPath)) {
                return false;
            }
            String content = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
            String c = content.toLowerCase();
            String id = profileId.trim().toLowerCase();
            return c.contains("<profile>") && c.contains("<id>" + id + "</id>");
        } catch (IOException ignored) {
            return false;
        }
    }

    private String detectType(AppDependencyModel model) {
        String detectedType = model.getDetectedType();
        if (detectedType != null && !detectedType.trim().isEmpty() &&
            !"generic".equals(detectedType)) {
            return detectedType;
        }

        if (model.getEntrypoint() != null) {
            for (String arg : model.getEntrypoint()) {
                String a = arg.toLowerCase();
                if ("php".equals(a) || a.contains("artisan") || a.contains("composer")) {
                    return "php";
                }
            }
        }

        return detectedType != null ? detectedType : "generic";
    }

    private void addSystemDependencies(DockerfileSpec spec) {
        String base = spec.getBaseImage() != null ? spec.getBaseImage().toLowerCase() : "";
        if (base.contains("alpine")) {
            spec.addRunCommand("apk add --no-cache curl git unzip ca-certificates");
        } else if (base.contains("php:")) {
            spec.addRunCommand(
                "apt-get update && apt-get install -y --no-install-recommends curl git unzip ca-certificates autoconf g++ make pkg-config && rm -rf /var/lib/apt/lists/*"
            );
        } else if (base.contains("debian") || base.contains("ubuntu") || base.contains("slim") || base.contains("temurin") ||
                   base.contains("mcr.microsoft.com")) {
            spec.addRunCommand("apt-get update && apt-get install -y --no-install-recommends curl git unzip ca-certificates && rm -rf /var/lib/apt/lists/*");
        }
    }

    private void addAppDependencies(DockerfileSpec spec) {
        String base = spec.getBaseImage() != null ? spec.getBaseImage().toLowerCase() : "";

        if (base.contains("php:")) {
            spec.addRunCommand("docker-php-ext-install pdo_mysql");
            spec.addPostCopyRunCommand(
                "if [ -f composer.json ]; then curl -sS https://getcomposer.org/installer | php -- --install-dir=/usr/local/bin --filename=composer && composer install --no-interaction --no-dev --prefer-dist; fi"
            );
        }

        if (base.contains("node:")) {
            spec.addPostCopyRunCommand(
                "if [ -f package-lock.json ]; then npm ci --omit=dev; elif [ -f package.json ]; then npm install --omit=dev; fi"
            );
        }

        if (base.contains("python:")) {
            spec.addPostCopyRunCommand(
                "if [ -f requirements.txt ]; then pip install --no-cache-dir -r requirements.txt; fi"
            );
        }
    }

    private boolean isLaravelArtisanServe(List<String> entrypoint) {
        if (entrypoint == null || entrypoint.isEmpty()) {
            return false;
        }

        boolean hasArtisan = false;
        boolean hasServe = false;
        for (String arg : entrypoint) {
            if (arg == null) continue;
            String a = arg.toLowerCase();
            if (a.contains("artisan")) hasArtisan = true;
            if ("serve".equals(a) || a.endsWith(" serve") || a.contains("artisan serve")) hasServe = true;
        }
        return hasArtisan && hasServe;
    }

    private String formatAsJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escapeString(items.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isMavenJavaProject(AppDependencyModel model, Path projectDir) {
        if (model == null || projectDir == null) {
            return false;
        }
        File pom = projectDir.resolve("pom.xml").toFile();
        if (!pom.exists() || !pom.isFile()) {
            return false;
        }

        String type = detectType(model);
        if ("java".equals(type)) {
            return true;
        }

        if (model.getEntrypoint() != null && !model.getEntrypoint().isEmpty()) {
            if (containsMavenOrSpringBootSignals(model.getEntrypoint())) {
                return true;
            }
        }

        return false;
    }

    private boolean containsMavenOrSpringBootSignals(List<String> command) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        for (String arg : command) {
            if (arg == null) continue;
            String a = arg.trim().toLowerCase();
            if (a.isEmpty()) continue;

            if ("mvn".equals(a) || "mvn.cmd".equals(a) || "mvn.bat".equals(a) ||
                "mvnw".equals(a) || "mvnw.cmd".equals(a) || "mvnw.bat".equals(a)) {
                return true;
            }

            if (a.endsWith("/mvn") || a.endsWith("\\mvn") || a.endsWith("/mvn.cmd") || a.endsWith("\\mvn.cmd") ||
                a.endsWith("/mvnw") || a.endsWith("\\mvnw") || a.endsWith("mvnw.cmd") || a.endsWith("mvnw.bat")) {
                return true;
            }

            if (a.contains("spring-boot:") || "spring-boot:run".equals(a) || a.contains("spring-boot:run")) {
                return true;
            }
        }

        return false;
    }

    private String generateMavenMultiStageDockerfile(AppDependencyModel model, Path projectDir) {
        Path pomPath = projectDir.resolve("pom.xml");
        int javaVersion = detectJavaVersionFromPom(pomPath);
        boolean hasProdProfile = hasMavenProfile(pomPath, "prod");

        List<Integer> ports = model != null ? model.getExposedPorts() : new ArrayList<>();
        if (ports == null || ports.isEmpty()) {
            ports = new ArrayList<>();
            ports.add(8080);
        }

        StringBuilder dockerfile = new StringBuilder();

        dockerfile.append("FROM maven:3-eclipse-temurin-")
                .append(javaVersion)
                .append("-alpine as build\n");
        dockerfile.append("WORKDIR /app\n");
        dockerfile.append("COPY . /app/.\n");
        dockerfile.append("RUN mvn -f /app/pom.xml clean package ");
        if (hasProdProfile) {
            dockerfile.append("-Pprod ");
        }
        dockerfile.append("-DskipTests\n\n");

        dockerfile.append("FROM eclipse-temurin:")
                .append(javaVersion)
                .append("-jre-alpine\n");
        dockerfile.append("WORKDIR /app\n\n");
        dockerfile.append("COPY --from=build /app/target/*.jar /app/app.jar\n\n");

        for (Integer port : ports) {
            dockerfile.append("EXPOSE ").append(port).append("\n");
        }
        dockerfile.append("\n");

        dockerfile.append("ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]\n");

        return dockerfile.toString();
    }

    private int detectJavaVersionFromPom(Path pomPath) {
        int fallback = 17;
        if (pomPath == null) {
            return fallback;
        }

        try {
            if (!Files.exists(pomPath)) {
                return fallback;
            }
            String content = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);

            Integer v;

            v = extractIntTag(content, "java.version");
            if (v != null) return normalizeJavaVersion(v, fallback);

            v = extractIntTag(content, "maven.compiler.release");
            if (v != null) return normalizeJavaVersion(v, fallback);

            v = extractIntTag(content, "maven.compiler.target");
            if (v != null) return normalizeJavaVersion(v, fallback);

            v = extractIntTag(content, "maven.compiler.source");
            if (v != null) return normalizeJavaVersion(v, fallback);

        } catch (IOException ignored) {
        }

        return fallback;
    }

    private Integer extractIntTag(String xml, String tagName) {
        if (xml == null || tagName == null) {
            return null;
        }
        String open = "<" + tagName + ">";
        String close = "</" + tagName + ">";

        int start = xml.indexOf(open);
        if (start < 0) return null;
        start += open.length();
        int end = xml.indexOf(close, start);
        if (end < 0) return null;

        String raw = xml.substring(start, end).trim();
        if (raw.startsWith("1.")) {
            raw = raw.substring(2);
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int normalizeJavaVersion(int detected, int fallback) {
        if (detected <= 0) {
            return fallback;
        }
        if (detected == 1) {
            return fallback;
        }
        if (detected < 8) {
            return 8;
        }
        return detected;
    }
}
