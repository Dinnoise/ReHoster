package com.rehoster.collector.args;

import java.util.ArrayList;
import java.util.List;

import com.rehoster.collector.Collector;
import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunConfig;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public class ArgsCollector implements Collector {

    @Override
    public String getName() {
        return "ArgsCollector";
    }

    @Override
    public void collect(RunConfig config, LaunchResult launchResult, RuntimeSnapshot snapshot) {
        List<String> args = new ArrayList<>();
        
        if (launchResult.getCommandLine() != null && launchResult.getCommandLine().size() > 1) {
            args.addAll(launchResult.getCommandLine().subList(1, launchResult.getCommandLine().size()));
        }

        snapshot.setArgs(args);

        snapshot.addObservation(new Observation(
            "args_collection",
            Observation.Severity.INFO,
            "Collected " + args.size() + " command line arguments"
        ));

        analyzeArgs(args, snapshot);
    }

    private void analyzeArgs(List<String> args, RuntimeSnapshot snapshot) {
        for (String arg : args) {
            if (arg.startsWith("-D") && arg.contains("=")) {
                String property = arg.substring(2);
                Observation obs = new Observation(
                    "args_analysis",
                    Observation.Severity.INFO,
                    "Detected Java system property: " + property
                );
                obs.addAttribute("property", property);
                snapshot.addObservation(obs);
            }

            if (arg.endsWith(".jar")) {
                Observation obs = new Observation(
                    "args_analysis",
                    Observation.Severity.INFO,
                    "Detected JAR file argument: " + arg
                );
                obs.addAttribute("jar_file", arg);
                snapshot.addObservation(obs);
            }

            if (arg.equals("-p") || arg.equals("--port") || arg.startsWith("-Dserver.port=")) {
                Observation obs = new Observation(
                    "args_analysis",
                    Observation.Severity.INFO,
                    "Detected port configuration in arguments"
                );
                snapshot.addObservation(obs);
            }
        }
    }
}
