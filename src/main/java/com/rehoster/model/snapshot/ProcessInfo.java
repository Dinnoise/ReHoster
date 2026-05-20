package com.rehoster.model.snapshot;

import java.time.Instant;

public class ProcessInfo {
    private long pid;
    private long ppid;
    private String name;
    private String commandLine;
    private Instant startTime;

    public ProcessInfo() {
    }

    public ProcessInfo(long pid, long ppid, String name, String commandLine, Instant startTime) {
        this.pid = pid;
        this.ppid = ppid;
        this.name = name;
        this.commandLine = commandLine;
        this.startTime = startTime;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getPpid() {
        return ppid;
    }

    public void setPpid(long ppid) {
        this.ppid = ppid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
}
