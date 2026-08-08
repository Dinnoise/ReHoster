package com.rehoster.ai.interactive;

import java.util.Scanner;

public class UserRequirementCollector {

    public String collect() {
        System.out.println();
        System.out.println("      Enter your requirements for Dockerfile/docker-compose refinement.");
        System.out.println("      Examples: \"use alpine image\", \"remove database services\", \"add healthcheck\"");
        System.out.println("      Press Enter to skip and apply default improvements.");
        System.out.print("      > ");
        System.out.flush();

        try {
            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                return line.isEmpty() ? null : line;
            }
        } catch (Exception e) {
            // Non-interactive environment — skip
        }
        return null;
    }
}
