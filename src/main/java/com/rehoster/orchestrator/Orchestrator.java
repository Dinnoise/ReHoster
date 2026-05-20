package com.rehoster.orchestrator;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.rehoster.ai.client.OpenRouterAiClient;
import com.rehoster.ai.config.AiConfig;
import com.rehoster.ai.model.AiExecutionReport;
import com.rehoster.ai.model.AiRefinementOutcome;
import com.rehoster.ai.service.AiArtifactRefiner;
import com.rehoster.ai.service.AiContextCollector;
import com.rehoster.ai.service.AiResponseParser;
import com.rehoster.ai.service.AiResponseValidator;
import com.rehoster.ai.service.AiSecretSanitizer;
import com.rehoster.ai.service.ArtifactMergePolicy;
import com.rehoster.analysis.DependencyAnalyzer;
import com.rehoster.analysis.MultiRoundAnalyzer;
import com.rehoster.analysis.ServiceDependencyDetector;
import com.rehoster.analysis.ServiceDependencyDetector.ServiceDependency;
import com.rehoster.collector.Collector;
import com.rehoster.collector.args.ArgsCollector;
import com.rehoster.collector.env.EnvFileCollector;
import com.rehoster.collector.env.EnvironmentCollector;
import com.rehoster.collector.process.ProcessCollector;
import com.rehoster.generation.ComposeGenerator;
import com.rehoster.generation.DockerfileGenerator;
import com.rehoster.generation.DockerignoreGenerator;
import com.rehoster.generation.RecommendationGenerator;
import com.rehoster.launcher.ProcessLauncher;
import com.rehoster.model.analysis.AppDependencyModel;
import com.rehoster.model.generation.RunReport;
import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunConfig;
import com.rehoster.model.run.RunContext;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.RuntimeSnapshot;
import com.rehoster.storage.JsonStorage;

public class Orchestrator {

    private final ProcessLauncher launcher;
    private final List<Collector> collectors;
    private final JsonStorage storage;
    private final DependencyAnalyzer analyzer;
    private final ServiceDependencyDetector serviceDependencyDetector;
    private final MultiRoundAnalyzer multiRoundAnalyzer;
    private final DockerfileGenerator dockerfileGenerator;
    private final DockerignoreGenerator dockerignoreGenerator;
    private final ComposeGenerator composeGenerator;
    private final RecommendationGenerator recommendationGenerator;
    private final AiArtifactRefiner aiArtifactRefiner;
    private boolean deepAnalysisEnabled = true;

    public Orchestrator() {
        this.launcher = new ProcessLauncher();
        this.collectors = new ArrayList<>();
        this.collectors.add(new ProcessCollector());
        this.collectors.add(new EnvironmentCollector());
        this.collectors.add(new EnvFileCollector());
        this.collectors.add(new ArgsCollector());
        
        this.storage = new JsonStorage();
        this.analyzer = new DependencyAnalyzer();
        this.serviceDependencyDetector = new ServiceDependencyDetector();
        this.multiRoundAnalyzer = new MultiRoundAnalyzer();
        this.dockerfileGenerator = new DockerfileGenerator();
        this.dockerignoreGenerator = new DockerignoreGenerator();
        this.composeGenerator = new ComposeGenerator();
        this.recommendationGenerator = new RecommendationGenerator();
        this.aiArtifactRefiner = new AiArtifactRefiner(
            new AiContextCollector(),
            new AiSecretSanitizer(),
            new com.rehoster.ai.prompt.AiPromptBuilder(),
            new OpenRouterAiClient(),
            new AiResponseParser(),
            new AiResponseValidator(),
            new ArtifactMergePolicy()
        );
    }

    public void setDeepAnalysisEnabled(boolean enabled) {
        this.deepAnalysisEnabled = enabled;
    }

    public RunReport execute(RunConfig config) {
        RunReport report = new RunReport();
        String runId = generateRunId();
        AiConfig aiConfig = AiConfig.fromRunConfig(config);
        report.setRunId(runId);
        report.setLegacyCommand(String.join(" ", config.getLegacyCommand()));
        report.setAiEnabled(aiConfig.isEnabled());
        report.setAiProvider(aiConfig.getProvider().name());
        report.setAiPrimaryModel(aiConfig.getPrimaryModel());

        System.out.println("=== ReHoster ===");
        System.out.println("Run ID: " + runId);
        System.out.println("Command: " + String.join(" ", config.getLegacyCommand()));
        System.out.println();

        try {
            Path outputDir = storage.createRunDirectory(config.getOutputDirectory(), runId);
            RunContext context = new RunContext(runId, outputDir);

            System.out.println("[1/9] Launching legacy process...");
            LaunchResult launchResult = launcher.launch(config);
            report.addCollectedData("Launch completed, PID: " + launchResult.getPid());
            System.out.println("      PID: " + launchResult.getPid());
            System.out.println("      Exit code: " + launchResult.getExitCode());

            RuntimeSnapshot snapshot = new RuntimeSnapshot();
            snapshot.setRunContext(context);
            snapshot.setLaunchResult(launchResult);

            System.out.println("[2/9] Collecting runtime data...");
            for (Collector collector : collectors) {
                try {
                    collector.collect(config, launchResult, snapshot);
                    report.addCollectedData(collector.getName() + " completed");
                    System.out.println("      " + collector.getName() + " - done");
                } catch (Exception e) {
                    snapshot.addObservation(new Observation(
                        "collector_error",
                        Observation.Severity.WARNING,
                        collector.getName() + " failed: " + e.getMessage()
                    ));
                    System.out.println("      " + collector.getName() + " - failed: " + e.getMessage());
                }
            }

            context.markFinished();

            System.out.println("[3/9] Saving runtime snapshot...");
            storage.saveSnapshot(snapshot, outputDir);
            report.addGeneratedArtifact("runtime-snapshot.json");

            System.out.println("[4/9] Analyzing dependencies...");
            AppDependencyModel model = analyzer.analyze(snapshot);
            storage.saveAnalysisResult(model, outputDir);
            report.addGeneratedArtifact("analysis-result.json");
            System.out.println("      Detected type: " + model.getDetectedType());
            System.out.println("      Environment vars: " + model.getRequiredEnv().size());
            System.out.println("      Exposed ports: " + model.getExposedPorts());

            System.out.println("[5/9] Detecting service dependencies (databases, caches)...");
            List<ServiceDependency> serviceDeps = serviceDependencyDetector.detectDependencies(
                snapshot, config.getWorkingDirectory());
            if (!serviceDeps.isEmpty()) {
                System.out.println("      Found " + serviceDeps.size() + " service dependency(ies):");
                for (ServiceDependency dep : serviceDeps) {
                    System.out.println("        - " + dep.getServiceName() + " (" + dep.getImage() + ")");
                }
                report.addCollectedData("Service dependencies: " + serviceDeps.size());
            } else {
                System.out.println("      No external service dependencies detected");
            }

            if (deepAnalysisEnabled) {
                System.out.println("[6/9] Running multi-round analysis...");
                multiRoundAnalyzer.analyze(config, snapshot);
                report.addCollectedData("Multi-round analysis completed");
            } else {
                System.out.println("[6/9] Skipping multi-round analysis (disabled)");
            }

            System.out.println("[7/9] Generating container artifacts...");

            String baselineDockerfile = dockerfileGenerator.generate(model, config.getWorkingDirectory());

            String dockerignore = dockerignoreGenerator.generate(model);
            storage.saveDockerignore(dockerignore, outputDir);
            report.addGeneratedArtifact(".dockerignore");
            System.out.println("      .dockerignore - done");
            
            String serviceName = extractServiceName(config.getLegacyCommand(), config.getWorkingDirectory());
            composeGenerator.setServiceDependencies(serviceDeps);
            String baselineCompose = composeGenerator.generate(model, serviceName);

            System.out.println("[8/9] Running AI refinement...");
            AiRefinementOutcome aiOutcome = aiArtifactRefiner.refine(
                config.getWorkingDirectory(),
                serviceName,
                model,
                serviceDeps,
                baselineDockerfile,
                baselineCompose,
                aiConfig
            );

            AiExecutionReport aiExecutionReport = aiOutcome.getExecutionReport();
            storage.saveAiRefinementReport(aiExecutionReport, outputDir);
            report.addGeneratedArtifact("ai-refinement.json");

            boolean aiSucceeded = aiExecutionReport != null && aiExecutionReport.isSuccess();

            if (aiSucceeded && aiOutcome.getAiDockerfile() != null && !aiOutcome.getAiDockerfile().trim().isEmpty()) {
                storage.saveAiDockerfile(aiOutcome.getAiDockerfile(), outputDir);
                report.addGeneratedArtifact("Dockerfile.ai");
            }
            if (aiSucceeded && aiOutcome.getAiDockerCompose() != null && !aiOutcome.getAiDockerCompose().trim().isEmpty()) {
                storage.saveAiDockerCompose(aiOutcome.getAiDockerCompose(), outputDir);
                report.addGeneratedArtifact("docker-compose.ai.yml");
            }

            storage.saveDockerfile(aiOutcome.getFinalDockerfile(), outputDir);
            report.addGeneratedArtifact("Dockerfile");
            System.out.println("      Dockerfile - done");

            storage.saveDockerCompose(aiOutcome.getFinalDockerCompose(), outputDir);
            report.addGeneratedArtifact("docker-compose.yml");
            System.out.println("      docker-compose.yml - done");

            report.setAiFallbackUsed(aiExecutionReport != null && aiExecutionReport.isFallbackUsed());
            report.setAiApplied(aiExecutionReport != null && aiExecutionReport.isApplied());
            report.setAiConfidence(aiExecutionReport != null ? aiExecutionReport.getConfidence() : 0.0d);
            report.setAiWarnings(aiExecutionReport != null ? new ArrayList<>(aiExecutionReport.getWarnings()) : new ArrayList<String>());

            String recommendations = recommendationGenerator.generate(snapshot, model);
            storage.saveRecommendations(recommendations, outputDir);
            report.addGeneratedArtifact("recommendations.md");
            System.out.println("      recommendations.md - done");

            System.out.println("[9/9] Saving report...");
            report.setSuccess(true);
            report.setSummary(report.isAiApplied()
                ? "Successfully analyzed, refined with AI, and generated container artifacts"
                : "Successfully analyzed and generated container artifacts");
            report.setWarnings(new ArrayList<>(snapshot.getObservations()));
            report.addGeneratedArtifact("report.json");
            storage.saveReport(report, outputDir);

            System.out.println();
            System.out.println("=== Complete ===");
            System.out.println("Output directory: " + outputDir.toAbsolutePath());
            System.out.println();
            System.out.println("Generated files:");
            for (String artifact : report.getGeneratedArtifacts()) {
                System.out.println("  - " + artifact);
            }

        } catch (IOException e) {
            report.setSuccess(false);
            report.setSummary("Failed: " + e.getMessage());
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            report.setSuccess(false);
            report.setSummary("Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            System.err.println("Process interrupted: " + e.getMessage());
        }

        return report;
    }

    private String generateRunId() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(java.time.LocalDateTime.now());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "run-" + timestamp + "-" + uuid;
    }

    private String extractServiceName(List<String> command, Path workingDirectory) {
        if (command == null || command.isEmpty()) {
            return "app";
        }
        
        String firstArg = command.get(0);

        if (firstArg != null) {
            String f = firstArg.trim().toLowerCase();
            if ("mvn".equals(f) || "mvn.cmd".equals(f) || "mvnw".equals(f) || "mvnw.cmd".equals(f) || "mvnw.bat".equals(f)) {
                if (workingDirectory != null && workingDirectory.getFileName() != null) {
                    return sanitizeServiceName(workingDirectory.getFileName().toString());
                }
            }
        }
        
        for (String arg : command) {
            if (arg.endsWith(".jar")) {
                String name = arg.substring(arg.lastIndexOf('/') + 1);
                name = name.substring(name.lastIndexOf('\\') + 1);
                name = name.replace(".jar", "");
                return sanitizeServiceName(name);
            }
            if (arg.endsWith(".py") || arg.endsWith(".js")) {
                String name = arg.substring(arg.lastIndexOf('/') + 1);
                name = name.substring(name.lastIndexOf('\\') + 1);
                name = name.replaceAll("\\.[^.]+$", "");
                return sanitizeServiceName(name);
            }
        }
        
        String name = firstArg.substring(firstArg.lastIndexOf('/') + 1);
        name = name.substring(name.lastIndexOf('\\') + 1);
        return sanitizeServiceName(name);
    }

    private String sanitizeServiceName(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
