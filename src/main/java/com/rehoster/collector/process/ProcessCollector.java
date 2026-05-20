package com.rehoster.collector.process;

import java.util.ArrayList;
import java.util.List;

import com.rehoster.collector.Collector;
import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunConfig;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.ProcessInfo;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public class ProcessCollector implements Collector {

    @Override
    public String getName() {
        return "ProcessCollector";
    }

    @Override
    public void collect(RunConfig config, LaunchResult launchResult, RuntimeSnapshot snapshot) {
        List<ProcessInfo> processTree = new ArrayList<>();

        ProcessInfo mainProcess = new ProcessInfo();
        mainProcess.setPid(launchResult.getPid());
        mainProcess.setPpid(0);
        mainProcess.setCommandLine(String.join(" ", launchResult.getCommandLine()));
        mainProcess.setStartTime(launchResult.getStartTime());

        String processName = extractProcessName(launchResult.getCommandLine());
        mainProcess.setName(processName);

        processTree.add(mainProcess);

        snapshot.setProcessTree(processTree);
        
        snapshot.addObservation(new Observation(
            "process_collection",
            Observation.Severity.INFO,
            "Collected " + processTree.size() + " process(es)"
        ));
    }

    private String extractProcessName(List<String> commandLine) {
        if (commandLine == null || commandLine.isEmpty()) {
            return "unknown";
        }
        return extractNameFromPath(commandLine.get(0));
    }

    private String extractNameFromPath(String path) {
        if (path == null) return "unknown";
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
