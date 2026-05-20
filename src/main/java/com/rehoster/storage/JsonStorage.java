package com.rehoster.storage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rehoster.ai.model.AiExecutionReport;
import com.rehoster.model.analysis.AppDependencyModel;
import com.rehoster.model.generation.RunReport;
import com.rehoster.model.snapshot.RuntimeSnapshot;
import com.rehoster.util.InstantTypeAdapter;
import com.rehoster.util.PathTypeAdapter;

public class JsonStorage {

    private final Gson gson;

    public JsonStorage() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(Path.class, new PathTypeAdapter())
            .create();
    }

    public void saveSnapshot(RuntimeSnapshot snapshot, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("runtime-snapshot.json");
        String json = gson.toJson(snapshot);
        writeString(filePath, json);
    }

    public void saveAnalysisResult(AppDependencyModel model, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("analysis-result.json");
        String json = gson.toJson(model);
        writeString(filePath, json);
    }

    public void saveReport(RunReport report, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("report.json");
        String json = gson.toJson(report);
        writeString(filePath, json);
    }

    public void saveDockerfile(String content, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("Dockerfile");
        writeString(filePath, content);
    }

    public void saveDockerCompose(String content, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("docker-compose.yml");
        writeString(filePath, content);
    }

    public void saveBaselineDockerfile(String content, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("Dockerfile.baseline");
        writeString(filePath, content);
    }

    public void saveBaselineDockerCompose(String content, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("docker-compose.baseline.yml");
        writeString(filePath, content);
    }

    public void saveAiDockerfile(String content, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("Dockerfile.ai");
        writeString(filePath, content);
    }

    public void saveAiDockerCompose(String content, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("docker-compose.ai.yml");
        writeString(filePath, content);
    }

    public void saveAiRefinementReport(AiExecutionReport report, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("ai-refinement.json");
        String json = gson.toJson(report);
        writeString(filePath, json);
    }

    public void saveDockerignore(String content, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve(".dockerignore");
        writeString(filePath, content);
    }

    public void saveRecommendations(String content, Path outputDir) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = outputDir.resolve("recommendations.md");
        writeString(filePath, content);
    }

    public RuntimeSnapshot loadSnapshot(Path filePath) throws IOException {
        String json = readString(filePath);
        return gson.fromJson(json, RuntimeSnapshot.class);
    }

    public AppDependencyModel loadAnalysisResult(Path filePath) throws IOException {
        String json = readString(filePath);
        return gson.fromJson(json, AppDependencyModel.class);
    }

    private void ensureDirectoryExists(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    public Path createRunDirectory(Path baseOutputDir, String runId) throws IOException {
        Path runDir = baseOutputDir.resolve(runId);
        ensureDirectoryExists(runDir);
        return runDir;
    }

    private void writeString(Path path, String content) throws IOException {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private String readString(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
