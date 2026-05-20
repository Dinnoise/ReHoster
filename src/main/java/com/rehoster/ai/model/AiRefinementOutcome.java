package com.rehoster.ai.model;

public class AiRefinementOutcome {
    private String finalDockerfile;
    private String finalDockerCompose;
    private String aiDockerfile;
    private String aiDockerCompose;
    private AiRefinementResult refinementResult;
    private AiExecutionReport executionReport;

    public String getFinalDockerfile() {
        return finalDockerfile;
    }

    public void setFinalDockerfile(String finalDockerfile) {
        this.finalDockerfile = finalDockerfile;
    }

    public String getFinalDockerCompose() {
        return finalDockerCompose;
    }

    public void setFinalDockerCompose(String finalDockerCompose) {
        this.finalDockerCompose = finalDockerCompose;
    }

    public String getAiDockerfile() {
        return aiDockerfile;
    }

    public void setAiDockerfile(String aiDockerfile) {
        this.aiDockerfile = aiDockerfile;
    }

    public String getAiDockerCompose() {
        return aiDockerCompose;
    }

    public void setAiDockerCompose(String aiDockerCompose) {
        this.aiDockerCompose = aiDockerCompose;
    }

    public AiRefinementResult getRefinementResult() {
        return refinementResult;
    }

    public void setRefinementResult(AiRefinementResult refinementResult) {
        this.refinementResult = refinementResult;
    }

    public AiExecutionReport getExecutionReport() {
        return executionReport;
    }

    public void setExecutionReport(AiExecutionReport executionReport) {
        this.executionReport = executionReport;
    }
}
