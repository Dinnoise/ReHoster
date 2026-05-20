package com.rehoster.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rehoster.model.analysis.AppDependencyModel;
import com.rehoster.model.analysis.EnvVarSpec;
import com.rehoster.model.snapshot.EnvVar;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public class DependencyAnalyzer {

    private static final Set<String> IMPORTANT_ENV_PATTERNS = new HashSet<>(Arrays.asList(
        "DATABASE", "DB_", "REDIS", "MONGO", "MYSQL", "POSTGRES",
        "API_KEY", "SECRET", "TOKEN", "PASSWORD", "CREDENTIALS",
        "PORT", "HOST", "URL", "URI", "ENDPOINT",
        "JAVA_HOME", "JAVA_OPTS", "JVM",
        "APP_", "SERVICE_", "CONFIG_"
    ));

    public AppDependencyModel analyze(RuntimeSnapshot snapshot) {
        AppDependencyModel model = new AppDependencyModel();

        if (snapshot.getLaunchResult() != null && snapshot.getLaunchResult().getCommandLine() != null) {
            model.setEntrypoint(new ArrayList<>(snapshot.getLaunchResult().getCommandLine()));
        }

        String appType = detectApplicationType(snapshot);
        model.setDetectedType(appType);

        List<EnvVarSpec> requiredEnv = analyzeEnvironment(snapshot);
        model.setRequiredEnv(requiredEnv);

        List<Integer> ports = detectPorts(snapshot, appType);
        model.setExposedPorts(ports);

        addAnalysisNotes(model, snapshot, appType);

        return model;
    }

    private String detectApplicationType(RuntimeSnapshot snapshot) {
        if (snapshot.getLaunchResult() == null || snapshot.getLaunchResult().getCommandLine() == null) {
            return "generic";
        }

        String commandLine = String.join(" ", snapshot.getLaunchResult().getCommandLine()).toLowerCase();

        if (commandLine.contains("java") || commandLine.contains(".jar")) {
            return "java";
        }
        if (commandLine.contains("php") || commandLine.contains("artisan") || commandLine.contains("composer")) {
            return "php";
        }
        if (commandLine.contains("python") || commandLine.contains(".py")) {
            return "python";
        }
        if (commandLine.contains("node") || commandLine.contains(".js")) {
            return "nodejs";
        }
        if (commandLine.contains("dotnet") || commandLine.contains(".dll")) {
            return "dotnet";
        }
        if (commandLine.contains("go ") || commandLine.endsWith(".go")) {
            return "golang";
        }

        return "generic";
    }

    private List<EnvVarSpec> analyzeEnvironment(RuntimeSnapshot snapshot) {
        List<EnvVarSpec> requiredEnv = new ArrayList<>();

        for (EnvVar env : snapshot.getEnvironment()) {
            if ("inherited".equals(env.getSource())) {
                continue;
            }

            if ("override".equals(env.getSource())) {
                requiredEnv.add(new EnvVarSpec(env.getKey(), env.getValue(), true));
                continue;
            }

            for (String pattern : IMPORTANT_ENV_PATTERNS) {
                if (env.getKey().toUpperCase().contains(pattern)) {
                    EnvVarSpec spec = new EnvVarSpec(env.getKey(), env.getValue(), false);
                    spec.setDescription("Detected as potentially important");
                    requiredEnv.add(spec);
                    break;
                }
            }
        }

        return requiredEnv;
    }

    private List<Integer> detectPorts(RuntimeSnapshot snapshot, String appType) {
        List<Integer> ports = new ArrayList<>();

        List<String> args = snapshot.getArgs();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);

            if (arg.startsWith("-Dserver.port=")) {
                try {
                    int port = Integer.parseInt(arg.substring("-Dserver.port=".length()));
                    addPort(ports, port);
                } catch (NumberFormatException ignored) {}
            }

            if (arg.equals("-p") || arg.equals("--port")) {
                if (i + 1 < args.size()) {
                    try {
                        int port = Integer.parseInt(args.get(i + 1));
                        addPort(ports, port);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (arg.startsWith("--port=")) {
                try {
                    int port = Integer.parseInt(arg.substring("--port=".length()));
                    addPort(ports, port);
                } catch (NumberFormatException ignored) {}
            }
        }

        for (EnvVar env : snapshot.getEnvironment()) {
            String key = env.getKey() != null ? env.getKey().toUpperCase() : "";
            if (key.equals("PORT") || key.equals("APP_PORT") || key.equals("SERVER_PORT") ||
                key.equals("HTTP_PORT") || key.equals("LISTEN_PORT")) {
                try {
                    int port = Integer.parseInt(env.getValue());
                    addPort(ports, port);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (ports.isEmpty()) {
            if ("php".equals(appType)) {
                ports.add(8000);
            } else if ("nodejs".equals(appType)) {
                ports.add(3000);
            } else if ("python".equals(appType)) {
                ports.add(8000);
            } else {
                ports.add(8080);
            }
        }

        return ports;
    }

    private void addPort(List<Integer> ports, int port) {
        if (port >= 1 && port <= 65535 && !ports.contains(port)) {
            ports.add(port);
        }
    }

    private void addAnalysisNotes(AppDependencyModel model, RuntimeSnapshot snapshot, String appType) {
        Observation typeNote = new Observation(
            "analysis",
            Observation.Severity.INFO,
            "Detected application type: " + appType
        );
        model.addNote(typeNote);

        if (snapshot.getLaunchResult() != null && snapshot.getLaunchResult().getExitCode() != null) {
            int exitCode = snapshot.getLaunchResult().getExitCode();
            if (exitCode != 0) {
                Observation exitNote = new Observation(
                    "analysis",
                    Observation.Severity.WARNING,
                    "Application exited with non-zero code: " + exitCode
                );
                model.addNote(exitNote);
            }
        }

        if (model.getRequiredEnv().size() > 10) {
            Observation envNote = new Observation(
                "analysis",
                Observation.Severity.INFO,
                "Application uses many environment variables (" + model.getRequiredEnv().size() + ")"
            );
            model.addNote(envNote);
        }
    }
}
