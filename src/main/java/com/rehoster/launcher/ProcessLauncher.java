package com.rehoster.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunConfig;

public class ProcessLauncher {

    public LaunchResult launch(RunConfig config) throws IOException, InterruptedException {
        LaunchResult result = new LaunchResult();
        List<String> command = resolveCommand(config);
        result.setCommandLine(command);
        result.setStartTime(Instant.now());

        ProcessBuilder builder = new ProcessBuilder(command);
        
        Path workDir = config.getWorkingDirectory();
        if (workDir != null) {
            builder.directory(workDir.toFile());
            result.setWorkingDirectory(workDir);
        }

        if (config.getEnvOverrides() != null && !config.getEnvOverrides().isEmpty()) {
            builder.environment().putAll(config.getEnvOverrides());
        }

        builder.redirectErrorStream(false);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            if (isCreateProcessError2(e)) {
                throw new IOException(
                    e.getMessage() + " (CreateProcess error=2). Executable not found. " +
                    "If you are launching Maven, either install Maven (mvn) into PATH or use the Maven Wrapper (mvnw/mvnw.cmd) in the project directory.",
                    e
                );
            }
            throw e;
        }
        result.setPid(getPid(process));

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            } catch (IOException e) {
                stderr.append("Error reading stdout: ").append(e.getMessage());
            }
        });

        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            } catch (IOException e) {
                stderr.append("Error reading stderr: ").append(e.getMessage());
            }
        });

        stdoutReader.start();
        stderrReader.start();

        boolean finished = process.waitFor(config.getTimeoutSeconds(), TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            stderr.append("\nProcess killed due to timeout after ")
                  .append(config.getTimeoutSeconds())
                  .append(" seconds");
        }

        stdoutReader.join(1000);
        stderrReader.join(1000);

        result.setEndTime(Instant.now());
        result.setExitCode(finished ? process.exitValue() : -1);
        result.setStdout(stdout.toString());
        result.setStderr(stderr.toString());

        return result;
    }

    public Process launchAsync(RunConfig config) throws IOException {
        List<String> command = resolveCommand(config);
        ProcessBuilder builder = new ProcessBuilder(command);
        
        Path workDir = config.getWorkingDirectory();
        if (workDir != null) {
            builder.directory(workDir.toFile());
        }

        if (config.getEnvOverrides() != null && !config.getEnvOverrides().isEmpty()) {
            builder.environment().putAll(config.getEnvOverrides());
        }

        return builder.start();
    }

    private List<String> resolveCommand(RunConfig config) {
        List<String> legacy = config != null ? config.getLegacyCommand() : null;
        if (legacy == null || legacy.isEmpty()) {
            return legacy;
        }

        Path workDir = config != null ? config.getWorkingDirectory() : null;

        String first = legacy.get(0);
        if (first == null) {
            return legacy;
        }

        String normalized = first.trim();
        String lower = normalized.toLowerCase();

        if (isMvnCommand(lower) && workDir != null) {
            List<String> wrapperCmd = tryBuildMavenWrapperCommand(workDir, legacy);
            if (wrapperCmd != null) {
                return wrapperCmd;
            }
        }

        return legacy;
    }

    private boolean isMvnCommand(String lower) {
        if (lower == null) {
            return false;
        }
        return "mvn".equals(lower) || "mvn.cmd".equals(lower) || "mvn.bat".equals(lower) || lower.endsWith("\\mvn") || lower.endsWith("\\mvn.cmd") || lower.endsWith("\\mvn.bat") || lower.endsWith("/mvn") || lower.endsWith("/mvn.cmd") || lower.endsWith("/mvn.bat");
    }

    private List<String> tryBuildMavenWrapperCommand(Path workDir, List<String> legacy) {
        boolean isWindows = System.getProperty("os.name") != null && System.getProperty("os.name").toLowerCase().contains("win");

        Path mvnwCmd = workDir.resolve("mvnw.cmd");
        Path mvnwBat = workDir.resolve("mvnw.bat");
        Path mvnw = workDir.resolve("mvnw");

        Path wrapper = null;
        if (isWindows) {
            if (mvnwCmd.toFile().exists()) wrapper = mvnwCmd;
            else if (mvnwBat.toFile().exists()) wrapper = mvnwBat;
            else return null;
        }
        if (wrapper == null) {
            if (mvnw.toFile().exists()) wrapper = mvnw;
        }

        if (wrapper == null) {
            return null;
        }

        List<String> resolved = new ArrayList<>();
        if (isWindows && (wrapper.toString().toLowerCase().endsWith(".cmd") || wrapper.toString().toLowerCase().endsWith(".bat"))) {
            resolved.add("cmd.exe");
            resolved.add("/c");
            resolved.add(wrapper.getFileName().toString());
        } else {
            resolved.add("sh");
            resolved.add(wrapper.getFileName().toString());
        }

        for (int i = 1; i < legacy.size(); i++) {
            resolved.add(legacy.get(i));
        }

        return resolved;
    }

    private boolean isCreateProcessError2(IOException e) {
        if (e == null || e.getMessage() == null) {
            return false;
        }
        String m = e.getMessage();
        return m.contains("CreateProcess error=2") || m.contains("error=2");
    }

    private long getPid(Process process) {
        long pid = -1;
        try {
            if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = process.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getInt(process);
            } else if (process.getClass().getName().equals("java.lang.ProcessImpl")) {
                Field f = process.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handle = f.getLong(process);
                pid = handle;
            }
        } catch (Exception e) {
            pid = System.currentTimeMillis() % 100000;
        }
        return pid;
    }
}
