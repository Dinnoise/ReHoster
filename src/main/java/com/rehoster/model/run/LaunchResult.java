package com.rehoster.model.run;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public class LaunchResult {
    private long pid;
    private List<String> commandLine;
    private Path workingDirectory;
    private Integer exitCode;
    private Instant startTime;
    private Instant endTime;
    private String stdout;
    private String stderr;

    public LaunchResult() {
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public List<String> getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(List<String> commandLine) {
        this.commandLine = commandLine;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }
}
