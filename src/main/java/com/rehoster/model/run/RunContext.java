package com.rehoster.model.run;

import java.nio.file.Path;
import java.time.Instant;

public class RunContext {
    private String runId;
    private Instant startedAt;
    private Instant finishedAt;
    private Path outputDir;

    public RunContext() {
    }

    public RunContext(String runId, Path outputDir) {
        this.runId = runId;
        this.outputDir = outputDir;
        this.startedAt = Instant.now();
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void markFinished() {
        this.finishedAt = Instant.now();
    }
}
