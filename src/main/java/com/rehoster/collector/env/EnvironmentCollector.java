package com.rehoster.collector.env;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rehoster.collector.Collector;
import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunConfig;
import com.rehoster.model.snapshot.EnvVar;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public class EnvironmentCollector implements Collector {

    @Override
    public String getName() {
        return "EnvironmentCollector";
    }

    @Override
    public void collect(RunConfig config, LaunchResult launchResult, RuntimeSnapshot snapshot) {
        List<EnvVar> environment = new ArrayList<>();

        if (config.getEnvOverrides() != null) {
            for (Map.Entry<String, String> entry : config.getEnvOverrides().entrySet()) {
                environment.add(new EnvVar(entry.getKey(), entry.getValue(), "override"));
            }
        }

        Map<String, String> systemEnv = System.getenv();
        for (Map.Entry<String, String> entry : systemEnv.entrySet()) {
            boolean isOverridden = config.getEnvOverrides() != null 
                && config.getEnvOverrides().containsKey(entry.getKey());
            
            if (!isOverridden) {
                environment.add(new EnvVar(entry.getKey(), entry.getValue(), "inherited"));
            }
        }

        snapshot.setEnvironment(environment);

        int overrideCount = config.getEnvOverrides() != null ? config.getEnvOverrides().size() : 0;
        snapshot.addObservation(new Observation(
            "env_collection",
            Observation.Severity.INFO,
            "Collected " + environment.size() + " environment variables (" 
                + overrideCount + " overridden)"
        ));

        detectImportantEnvVars(environment, snapshot);
    }

    private void detectImportantEnvVars(List<EnvVar> environment, RuntimeSnapshot snapshot) {
        String[] importantVars = {
            "JAVA_HOME", "PATH", "HOME", "USER", "TEMP", "TMP",
            "DATABASE_URL", "DB_HOST", "DB_PORT", "DB_USER",
            "API_KEY", "SECRET_KEY", "PORT", "HOST"
        };

        for (EnvVar env : environment) {
            for (String important : importantVars) {
                if (env.getKey().toUpperCase().contains(important)) {
                    Observation obs = new Observation(
                        "env_detection",
                        Observation.Severity.INFO,
                        "Detected potentially important variable: " + env.getKey()
                    );
                    obs.addAttribute("variable", env.getKey());
                    obs.addAttribute("source", env.getSource());
                    snapshot.addObservation(obs);
                    break;
                }
            }
        }
    }
}
