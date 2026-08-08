package com.rehoster.cli;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rehoster.ai.config.AiMode;
import com.rehoster.model.run.RunConfig;

public class CliParser {

    public RunConfig parse(String[] args) {
        RunConfig config = new RunConfig();
        List<String> legacyCommand = new ArrayList<>();
        Map<String, String> envOverrides = new HashMap<>();
        
        boolean afterDoubleDash = false;
        int i = 0;
        
        while (i < args.length) {
            String arg = args[i];
            
            if ("--".equals(arg)) {
                afterDoubleDash = true;
                i++;
                continue;
            }
            
            if (afterDoubleDash) {
                legacyCommand.add(arg);
                i++;
                continue;
            }
            
            switch (arg) {
                case "run":
                    i++;
                    break;
                    
                case "-t":
                case "--timeout":
                    if (i + 1 < args.length) {
                        try {
                            config.setTimeoutSeconds(Integer.parseInt(args[i + 1]));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid timeout value: " + args[i + 1]);
                        }
                        i += 2;
                    } else {
                        i++;
                    }
                    break;
                    
                case "-d":
                case "--dir":
                    if (i + 1 < args.length) {
                        config.setWorkingDirectory(Paths.get(args[i + 1]));
                        i += 2;
                    } else {
                        i++;
                    }
                    break;
                    
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        config.setOutputDirectory(Paths.get(args[i + 1]));
                        i += 2;
                    } else {
                        i++;
                    }
                    break;
                    
                case "-e":
                case "--env":
                    if (i + 1 < args.length) {
                        String envPair = args[i + 1];
                        int eqIndex = envPair.indexOf('=');
                        if (eqIndex > 0) {
                            String key = envPair.substring(0, eqIndex);
                            String value = envPair.substring(eqIndex + 1);
                            envOverrides.put(key, value);
                        }
                        i += 2;
                    } else {
                        i++;
                    }
                    break;
                    
                case "-h":
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;

                case "--ai":
                    config.setAiEnabled(true);
                    if (config.getAiMode() == null || config.getAiMode() == AiMode.OFF) {
                        config.setAiMode(AiMode.AUTO_APPLY);
                    }
                    i++;
                    break;

                case "--ai-mode":
                    if (i + 1 < args.length) {
                        AiMode mode = AiMode.fromCliValue(args[i + 1]);
                        config.setAiMode(mode);
                        config.setAiEnabled(mode != AiMode.OFF);
                        i += 2;
                    } else {
                        i++;
                    }
                    break;

                case "--ai-model":
                    if (i + 1 < args.length) {
                        config.setAiEnabled(true);
                        config.setAiModel(args[i + 1]);
                        if (config.getAiMode() == null || config.getAiMode() == AiMode.OFF) {
                            config.setAiMode(AiMode.AUTO_APPLY);
                        }
                        i += 2;
                    } else {
                        i++;
                    }
                    break;

                case "--ai-timeout":
                    if (i + 1 < args.length) {
                        try {
                            config.setAiEnabled(true);
                            config.setAiTimeoutSeconds(Integer.parseInt(args[i + 1]));
                            if (config.getAiMode() == null || config.getAiMode() == AiMode.OFF) {
                                config.setAiMode(AiMode.AUTO_APPLY);
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid AI timeout value: " + args[i + 1]);
                        }
                        i += 2;
                    } else {
                        i++;
                    }
                    break;

                case "--no-ai-fallback":
                    config.setAiEnabled(true);
                    config.setAiFallbackEnabled(false);
                    if (config.getAiMode() == null || config.getAiMode() == AiMode.OFF) {
                        config.setAiMode(AiMode.AUTO_APPLY);
                    }
                    i++;
                    break;
                    
                default:
                    i++;
                    break;
            }
        }
        
        config.setLegacyCommand(legacyCommand);
        config.setEnvOverrides(envOverrides);
        
        if (config.getWorkingDirectory() == null) {
            config.setWorkingDirectory(Paths.get(System.getProperty("user.dir")));
        }
        
        if (config.getOutputDirectory() == null) {
            config.setOutputDirectory(Paths.get("rehoster-output"));
        }
        
        return config;
    }

    public void printHelp() {
        System.out.println("ReHoster - Legacy Application Containerization Framework");
        System.out.println();
        System.out.println("Usage: rehoster run [options] -- <legacy command>");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -t, --timeout <seconds>        Timeout for legacy process (default: 60)");
        System.out.println("  -d, --dir <path>               Working directory for legacy process");
        System.out.println("  -o, --output <path>            Output directory (default: rehoster-output)");
        System.out.println("  -e, --env <KEY=VALUE>          Environment variable override (can be repeated)");
        System.out.println("  --ai                           Enable AI artifact refinement via LM Studio");
        System.out.println("  --ai-mode <off|advisory|auto>  AI refinement mode (default: auto)");
        System.out.println("  --ai-model <model>             Override AI model name (default: qwen3-9b)");
        System.out.println("  --ai-timeout <seconds>         Timeout for AI request (default: 120)");
        System.out.println("  -h, --help                     Show this help message");
        System.out.println();
        System.out.println("AI mode requires LM Studio running locally on http://localhost:1234");
        System.out.println("Recommended model: Qwen3-9B-Q4_K_M (load it in LM Studio before running)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  rehoster run -- java -jar myapp.jar");
        System.out.println("  rehoster run -t 120 -e DB_HOST=localhost -- ./myapp");
        System.out.println("  rehoster run --output ./output -- python app.py");
        System.out.println("  rehoster run --ai -- java -jar myapp.jar");
        System.out.println("  rehoster run --ai --ai-model qwen3-9b -- java -jar myapp.jar");
    }

    public boolean validate(RunConfig config) {
        if (config.getLegacyCommand() == null || config.getLegacyCommand().isEmpty()) {
            System.err.println("Error: No legacy command specified.");
            System.err.println("Usage: rehoster run -- <legacy command>");
            return false;
        }
        return true;
    }
}
