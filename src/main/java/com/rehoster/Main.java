package com.rehoster;

import com.rehoster.cli.CliParser;
import com.rehoster.model.generation.RunReport;
import com.rehoster.model.run.RunConfig;
import com.rehoster.orchestrator.Orchestrator;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            printBanner();
            new CliParser().printHelp();
            System.exit(0);
        }

        CliParser cliParser = new CliParser();
        RunConfig config = cliParser.parse(args);

        if (!cliParser.validate(config)) {
            System.exit(1);
        }

        Orchestrator orchestrator = new Orchestrator();
        RunReport report = orchestrator.execute(config);

        System.exit(report.isSuccess() ? 0 : 1);
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  ____      _   _           _            ");
        System.out.println(" |  _ \\ ___| | | | ___  ___| |_ ___ _ __ ");
        System.out.println(" | |_) / _ \\ |_| |/ _ \\/ __| __/ _ \\ '__|");
        System.out.println(" |  _ <  __/  _  | (_) \\__ \\ ||  __/ |   ");
        System.out.println(" |_| \\_\\___|_| |_|\\___/|___/\\__\\___|_|   ");
        System.out.println();
        System.out.println(" Legacy Application Containerization Framework");
        System.out.println(" Version 1.0.0");
        System.out.println();
    }
}
