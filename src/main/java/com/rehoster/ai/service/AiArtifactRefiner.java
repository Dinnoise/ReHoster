package com.rehoster.ai.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.rehoster.analysis.ServiceDependencyDetector.ServiceDependency;
import com.rehoster.ai.client.AiClient;
import com.rehoster.ai.client.AiClientResponse;
import com.rehoster.ai.config.AiConfig;
import com.rehoster.ai.model.AiExecutionReport;
import com.rehoster.ai.model.AiRefinementOutcome;
import com.rehoster.ai.model.AiRefinementRequest;
import com.rehoster.ai.model.AiRefinementResult;
import com.rehoster.ai.prompt.AiPromptBuilder;
import com.rehoster.model.analysis.AppDependencyModel;

public class AiArtifactRefiner {
    private final AiContextCollector contextCollector;
    private final AiSecretSanitizer secretSanitizer;
    private final AiPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AiResponseParser responseParser;
    private final AiResponseValidator responseValidator;
    private final ArtifactMergePolicy mergePolicy;

    public AiArtifactRefiner(AiContextCollector contextCollector,
                             AiSecretSanitizer secretSanitizer,
                             AiPromptBuilder promptBuilder,
                             AiClient aiClient,
                             AiResponseParser responseParser,
                             AiResponseValidator responseValidator,
                             ArtifactMergePolicy mergePolicy) {
        this.contextCollector = contextCollector;
        this.secretSanitizer = secretSanitizer;
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
        this.responseParser = responseParser;
        this.responseValidator = responseValidator;
        this.mergePolicy = mergePolicy;
    }

    public AiRefinementOutcome refine(Path workingDirectory,
                                      String serviceName,
                                      AppDependencyModel model,
                                      List<ServiceDependency> serviceDependencies,
                                      String baselineDockerfile,
                                      String baselineCompose,
                                      AiConfig config,
                                      String userRequirement) {
        AiRefinementOutcome outcome = new AiRefinementOutcome();
        AiExecutionReport report = createInitialReport(config);
        outcome.setExecutionReport(report);
        outcome.setFinalDockerfile(baselineDockerfile);
        outcome.setFinalDockerCompose(baselineCompose);

        if (config == null || !config.isEnabled()) {
            report.setSkipped(true);
            report.setFailureReason("AI refinement is disabled");
            return outcome;
        }

        try {
            AiRefinementRequest request = contextCollector.collect(
                workingDirectory,
                serviceName,
                model,
                serviceDependencies,
                baselineDockerfile,
                baselineCompose,
                config
            );
            request = secretSanitizer.sanitize(request);

            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(request, config, userRequirement);
            AiClientResponse aiResponse = aiClient.generate(systemPrompt, userPrompt, config);
            report.setUsedModel(aiResponse.getUsedModel());
            report.setFallbackUsed(aiResponse.isFallbackUsed());
            report.setRawResponse(aiResponse.getContent());

            if (!aiResponse.isSuccess()) {
                report.setFailureReason(aiResponse.getErrorMessage());
                report.getWarnings().add("AI request was not successful, baseline artifacts kept");
                return outcome;
            }

            AiRefinementResult result = responseParser.parse(aiResponse.getContent());
            outcome.setRefinementResult(result);

            List<String> validationErrors = responseValidator.validate(request, result);
            report.setValidationErrors(validationErrors);

            if (result != null) {
                report.setConfidence(result.getConfidence());
                if (result.getWarnings() != null) {
                    report.setWarnings(new ArrayList<String>(result.getWarnings()));
                }
            }

            if (result != null) {
                outcome.setAiDockerfile(result.getDockerfile());
                outcome.setAiDockerCompose(result.getDockerCompose());
            }

            boolean apply = mergePolicy.shouldApply(config, result, validationErrors);
            report.setApplied(apply);
            report.setSuccess(validationErrors.isEmpty() && result != null);

            if (apply && result != null) {
                outcome.setFinalDockerfile(result.getDockerfile());
                outcome.setFinalDockerCompose(result.getDockerCompose());
            }

            if (!apply && validationErrors.isEmpty() && config.getMode().name().equals("ADVISORY") && result != null) {
                report.getWarnings().add("AI result generated in advisory mode and was not auto-applied");
            }

            return outcome;
        } catch (IOException e) {
            report.setFailureReason(e.getMessage());
            report.getWarnings().add("AI request failed: " + e.getMessage());
            return outcome;
        } catch (Exception e) {
            report.setFailureReason(e.getMessage());
            report.getWarnings().add("AI refinement failed unexpectedly: " + e.getMessage());
            return outcome;
        }
    }

    private AiExecutionReport createInitialReport(AiConfig config) {
        AiExecutionReport report = new AiExecutionReport();
        report.setProvider(config != null && config.getProvider() != null ? config.getProvider().name() : null);
        report.setPrimaryModel(config != null ? config.getPrimaryModel() : null);
        report.setFallbackModel(config != null ? config.getFallbackModel() : null);
        report.setSuccess(false);
        report.setApplied(false);
        report.setValidationErrors(new ArrayList<String>());
        report.setWarnings(new ArrayList<String>());
        return report;
    }
}
