package com.rehoster.collector.env;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.rehoster.collector.Collector;
import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunConfig;
import com.rehoster.model.snapshot.EnvVar;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public class EnvFileCollector implements Collector {

    private static final String[] ENV_FILE_NAMES = {
        ".env",
        ".env.local",
        ".env.production",
        ".env.development",
        ".env.example",
        "env.ini",
        ".env.dist"
    };

    @Override
    public String getName() {
        return "EnvFileCollector";
    }

    @Override
    public void collect(RunConfig config, LaunchResult launchResult, RuntimeSnapshot snapshot) {
        Path workDir = config.getWorkingDirectory();
        if (workDir == null) {
            return;
        }

        List<EnvVar> envFromFiles = new ArrayList<>();
        int filesFound = 0;

        for (String envFileName : ENV_FILE_NAMES) {
            File envFile = workDir.resolve(envFileName).toFile();
            if (envFile.exists() && envFile.isFile()) {
                filesFound++;
                try {
                    List<EnvVar> parsed = parseEnvFile(envFile, envFileName);
                    envFromFiles.addAll(parsed);
                    
                    snapshot.addObservation(new Observation(
                        "env_file",
                        Observation.Severity.INFO,
                        "Parsed " + parsed.size() + " variables from " + envFileName
                    ));
                } catch (IOException e) {
                    snapshot.addObservation(new Observation(
                        "env_file",
                        Observation.Severity.WARNING,
                        "Failed to parse " + envFileName + ": " + e.getMessage()
                    ));
                }
            }
        }

        scanForPhpEnvFiles(workDir, envFromFiles, snapshot);
        scanForDockerEnvFiles(workDir, envFromFiles, snapshot);

        List<EnvVar> existingEnv = snapshot.getEnvironment();
        for (EnvVar fileEnv : envFromFiles) {
            boolean exists = false;
            for (EnvVar existing : existingEnv) {
                if (existing.getKey().equals(fileEnv.getKey())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                existingEnv.add(fileEnv);
            }
        }

        if (filesFound > 0) {
            snapshot.addObservation(new Observation(
                "env_file",
                Observation.Severity.INFO,
                "Found " + filesFound + " environment file(s), added " + envFromFiles.size() + " variables"
            ));
        }
    }

    private List<EnvVar> parseEnvFile(File file, String source) throws IOException {
        List<EnvVar> vars = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();

                    if (key.startsWith("export ")) {
                        key = key.substring(7).trim();
                    }

                    value = unquote(value);
                    
                    vars.add(new EnvVar(key, value, "file:" + source));
                }
            }
        }
        
        return vars;
    }

    private void scanForPhpEnvFiles(Path workDir, List<EnvVar> envFromFiles, RuntimeSnapshot snapshot) {
        String[] phpConfigFiles = {"config/database.php", "config/app.php", ".env.php"};
        
        for (String configPath : phpConfigFiles) {
            File configFile = workDir.resolve(configPath).toFile();
            if (configFile.exists()) {
                try {
                    List<EnvVar> phpEnv = parsePhpEnvReferences(configFile);
                    envFromFiles.addAll(phpEnv);
                    if (!phpEnv.isEmpty()) {
                        snapshot.addObservation(new Observation(
                            "php_config",
                            Observation.Severity.INFO,
                            "Found " + phpEnv.size() + " env references in " + configPath
                        ));
                    }
                } catch (IOException e) {
                    // Ignore parsing errors for PHP files
                }
            }
        }
    }

    private List<EnvVar> parsePhpEnvReferences(File file) throws IOException {
        List<EnvVar> vars = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int envIndex = line.indexOf("env(");
                while (envIndex >= 0) {
                    int closeIndex = line.indexOf(")", envIndex);
                    if (closeIndex > envIndex) {
                        String envCall = line.substring(envIndex + 4, closeIndex);
                        String[] parts = envCall.split(",");
                        if (parts.length >= 1) {
                            String key = unquote(parts[0].trim());
                            String defaultValue = parts.length > 1 ? unquote(parts[1].trim()) : "";
                            vars.add(new EnvVar(key, defaultValue, "php:env()"));
                        }
                    }
                    envIndex = line.indexOf("env(", closeIndex > 0 ? closeIndex : envIndex + 1);
                }

                int getenvIndex = line.indexOf("getenv(");
                while (getenvIndex >= 0) {
                    int closeIndex = line.indexOf(")", getenvIndex);
                    if (closeIndex > getenvIndex) {
                        String key = unquote(line.substring(getenvIndex + 7, closeIndex).trim());
                        vars.add(new EnvVar(key, "", "php:getenv()"));
                    }
                    getenvIndex = line.indexOf("getenv(", closeIndex > 0 ? closeIndex : getenvIndex + 1);
                }

                int envVarIndex = line.indexOf("$_ENV[");
                while (envVarIndex >= 0) {
                    int closeIndex = line.indexOf("]", envVarIndex);
                    if (closeIndex > envVarIndex) {
                        String key = unquote(line.substring(envVarIndex + 6, closeIndex).trim());
                        vars.add(new EnvVar(key, "", "php:$_ENV"));
                    }
                    envVarIndex = line.indexOf("$_ENV[", closeIndex > 0 ? closeIndex : envVarIndex + 1);
                }
            }
        }
        
        return vars;
    }

    private void scanForDockerEnvFiles(Path workDir, List<EnvVar> envFromFiles, RuntimeSnapshot snapshot) {
        File dockerComposeFile = workDir.resolve("docker-compose.yml").toFile();
        if (!dockerComposeFile.exists()) {
            dockerComposeFile = workDir.resolve("docker-compose.yaml").toFile();
        }
        
        if (dockerComposeFile.exists()) {
            snapshot.addObservation(new Observation(
                "docker",
                Observation.Severity.INFO,
                "Found existing docker-compose.yml - will analyze for dependencies"
            ));
        }
    }

    private String unquote(String s) {
        if (s == null || s.length() < 2) {
            return s;
        }
        if ((s.startsWith("\"") && s.endsWith("\"")) || 
            (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
