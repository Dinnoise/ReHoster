package com.rehoster.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rehoster.launcher.ProcessLauncher;
import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunConfig;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public class MultiRoundAnalyzer {

    public static class AnalysisRound {
        private int roundNumber;
        private Map<String, String> envConfig;
        private List<String> args;
        private LaunchResult result;
        private boolean success;
        private String description;

        public AnalysisRound(int roundNumber, String description) {
            this.roundNumber = roundNumber;
            this.description = description;
            this.envConfig = new HashMap<>();
            this.args = new ArrayList<>();
        }

        public int getRoundNumber() { return roundNumber; }
        public Map<String, String> getEnvConfig() { return envConfig; }
        public List<String> getArgs() { return args; }
        public LaunchResult getResult() { return result; }
        public void setResult(LaunchResult result) { this.result = result; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getDescription() { return description; }
    }

    private final ProcessLauncher launcher;
    private final int maxRounds;
    private final int roundTimeout;

    public MultiRoundAnalyzer() {
        this(3, 10);
    }

    public MultiRoundAnalyzer(int maxRounds, int roundTimeout) {
        this.launcher = new ProcessLauncher();
        this.maxRounds = maxRounds;
        this.roundTimeout = roundTimeout;
    }

    public List<AnalysisRound> analyze(RunConfig baseConfig, RuntimeSnapshot snapshot) {
        List<AnalysisRound> rounds = new ArrayList<>();

        AnalysisRound round1 = new AnalysisRound(1, "Base configuration");
        round1.getEnvConfig().putAll(baseConfig.getEnvOverrides());
        rounds.add(round1);

        List<AnalysisRound> additionalRounds = generateAdditionalRounds(baseConfig, snapshot);
        rounds.addAll(additionalRounds);

        for (AnalysisRound round : rounds) {
            if (round.getRoundNumber() == 1) {
                round.setSuccess(snapshot.getLaunchResult() != null && 
                    (snapshot.getLaunchResult().getExitCode() == null || 
                     snapshot.getLaunchResult().getExitCode() == 0));
                round.setResult(snapshot.getLaunchResult());
                continue;
            }

            try {
                RunConfig roundConfig = createRoundConfig(baseConfig, round);
                LaunchResult result = launcher.launch(roundConfig);
                round.setResult(result);
                round.setSuccess(result.getExitCode() != null && result.getExitCode() == 0);

                snapshot.addObservation(new Observation(
                    "multi_round",
                    round.isSuccess() ? Observation.Severity.INFO : Observation.Severity.WARNING,
                    "Round " + round.getRoundNumber() + " (" + round.getDescription() + "): " +
                        (round.isSuccess() ? "SUCCESS" : "FAILED with exit code " + result.getExitCode())
                ));
            } catch (IOException | InterruptedException e) {
                round.setSuccess(false);
                snapshot.addObservation(new Observation(
                    "multi_round",
                    Observation.Severity.ERROR,
                    "Round " + round.getRoundNumber() + " failed: " + e.getMessage()
                ));
            }
        }

        analyzeRoundResults(rounds, snapshot);

        return rounds;
    }

    private List<AnalysisRound> generateAdditionalRounds(RunConfig baseConfig, RuntimeSnapshot snapshot) {
        List<AnalysisRound> additionalRounds = new ArrayList<>();

        String appType = detectAppType(baseConfig);

        if ("php".equals(appType)) {
            AnalysisRound phpRound = new AnalysisRound(2, "PHP with development mode");
            phpRound.getEnvConfig().put("APP_ENV", "local");
            phpRound.getEnvConfig().put("APP_DEBUG", "true");
            additionalRounds.add(phpRound);
        }

        if ("java".equals(appType)) {
            AnalysisRound javaRound = new AnalysisRound(2, "Java with debug logging");
            javaRound.getEnvConfig().put("JAVA_OPTS", "-Xmx512m");
            javaRound.getEnvConfig().put("LOG_LEVEL", "DEBUG");
            additionalRounds.add(javaRound);
        }

        if ("nodejs".equals(appType)) {
            AnalysisRound nodeRound = new AnalysisRound(2, "Node.js development mode");
            nodeRound.getEnvConfig().put("NODE_ENV", "development");
            additionalRounds.add(nodeRound);
        }

        if ("python".equals(appType)) {
            AnalysisRound pythonRound = new AnalysisRound(2, "Python with debug");
            pythonRound.getEnvConfig().put("PYTHONDONTWRITEBYTECODE", "1");
            pythonRound.getEnvConfig().put("PYTHONUNBUFFERED", "1");
            additionalRounds.add(pythonRound);
        }

        AnalysisRound portTestRound = new AnalysisRound(additionalRounds.size() + 2, "Alternative port test");
        portTestRound.getEnvConfig().put("PORT", "3000");
        additionalRounds.add(portTestRound);

        return additionalRounds;
    }

    private String detectAppType(RunConfig config) {
        if (config.getLegacyCommand() == null || config.getLegacyCommand().isEmpty()) {
            return "generic";
        }

        String command = String.join(" ", config.getLegacyCommand()).toLowerCase();

        if (command.contains("php") || command.contains("artisan") || command.contains("composer")) {
            return "php";
        }
        if (command.contains("java") || command.contains(".jar")) {
            return "java";
        }
        if (command.contains("node") || command.contains("npm") || command.contains(".js")) {
            return "nodejs";
        }
        if (command.contains("python") || command.contains(".py")) {
            return "python";
        }

        return "generic";
    }

    private RunConfig createRoundConfig(RunConfig baseConfig, AnalysisRound round) {
        RunConfig roundConfig = new RunConfig();
        roundConfig.setLegacyCommand(new ArrayList<>(baseConfig.getLegacyCommand()));
        roundConfig.setWorkingDirectory(baseConfig.getWorkingDirectory());
        roundConfig.setOutputDirectory(baseConfig.getOutputDirectory());
        roundConfig.setTimeoutSeconds(roundTimeout);

        Map<String, String> env = new HashMap<>();
        if (baseConfig.getEnvOverrides() != null) {
            env.putAll(baseConfig.getEnvOverrides());
        }
        env.putAll(round.getEnvConfig());
        roundConfig.setEnvOverrides(env);

        return roundConfig;
    }

    private void analyzeRoundResults(List<AnalysisRound> rounds, RuntimeSnapshot snapshot) {
        int successCount = 0;
        int failCount = 0;

        for (AnalysisRound round : rounds) {
            if (round.isSuccess()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        if (successCount == rounds.size()) {
            snapshot.addObservation(new Observation(
                "multi_round_summary",
                Observation.Severity.INFO,
                "All " + rounds.size() + " analysis rounds completed successfully"
            ));
        } else if (successCount > 0) {
            snapshot.addObservation(new Observation(
                "multi_round_summary",
                Observation.Severity.WARNING,
                successCount + " of " + rounds.size() + " rounds succeeded. " +
                    "Application may require specific configuration."
            ));
        } else {
            snapshot.addObservation(new Observation(
                "multi_round_summary",
                Observation.Severity.ERROR,
                "All " + rounds.size() + " analysis rounds failed. " +
                    "Check application dependencies and configuration."
            ));
        }

        Map<String, String> workingEnv = new HashMap<>();
        for (AnalysisRound round : rounds) {
            if (round.isSuccess()) {
                workingEnv.putAll(round.getEnvConfig());
            }
        }

        if (!workingEnv.isEmpty()) {
            StringBuilder envNote = new StringBuilder("Working environment variables found: ");
            for (Map.Entry<String, String> entry : workingEnv.entrySet()) {
                envNote.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
            }
            snapshot.addObservation(new Observation(
                "multi_round_env",
                Observation.Severity.INFO,
                envNote.toString()
            ));
        }
    }
}
