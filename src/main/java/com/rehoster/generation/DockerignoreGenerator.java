package com.rehoster.generation;

import com.rehoster.model.analysis.AppDependencyModel;

public class DockerignoreGenerator {

    public String generate(AppDependencyModel model) {
        String type = model != null && model.getDetectedType() != null ? model.getDetectedType() : "generic";

        StringBuilder sb = new StringBuilder();

        sb.append(".git\n");
        sb.append(".gitignore\n");
        sb.append(".idea\n");
        sb.append(".vscode\n");
        sb.append("*.iml\n");
        sb.append("rehoster-output\n");
        sb.append(".env\n");
        sb.append(".env.*\n");
        sb.append("*.log\n");

        if ("java".equals(type)) {
            sb.append("target\n");
            sb.append("*.class\n");
        }

        if ("nodejs".equals(type)) {
            sb.append("node_modules\n");
            sb.append("npm-debug.log\n");
            sb.append("yarn-error.log\n");
        }

        if ("python".equals(type)) {
            sb.append("__pycache__\n");
            sb.append("*.pyc\n");
            sb.append(".venv\n");
        }

        if ("php".equals(type)) {
            sb.append("vendor\n");
        }

        return sb.toString();
    }
}
