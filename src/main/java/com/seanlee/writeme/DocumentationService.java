package com.seanlee.writeme;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class DocumentationService {

    private final AiService aiService;

    public DocumentationService(AiService aiService) {
        this.aiService = aiService;
    }

    public String generateReadme(
            String projectName,
            String projectDescription,
            MultipartFile[] files
    ) throws IOException {
        ProjectInfo projectInfo = analyseProject(projectName, projectDescription, files);

        String prompt = buildPrompt(projectInfo);

        return aiService.generateReadmeFromAi(prompt);
    }

    private ProjectInfo analyseProject(
            String projectName,
            String projectDescription,
            MultipartFile[] files
    ) throws IOException {
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.projectName = projectName;
        projectInfo.projectDescription = projectDescription;
        projectInfo.projectStructure = extractStructure(files);

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (isCodeFile(fileName)) {
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);

                FileInfo fileInfo = new FileInfo();
                fileInfo.fileName = fileName;
                fileInfo.content = content;
                extractFileMetadata(fileInfo, content);

                projectInfo.files.add(fileInfo);
            }
        }

        return projectInfo;
    }

    private String extractStructure(MultipartFile[] files) {
        String T_JUNCTION = "├── ";
        String L_JUNCTION = "└── ";

        StringBuilder projectStructure = new StringBuilder();
        projectStructure.append("```\n");

        List<String> paths = Arrays.stream(files)
                .map(MultipartFile::getOriginalFilename)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        String prevDir = "";
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            String[] parts = path.split("/");

            for (int j = 0; j < parts.length; j++) {
                String currentPath = String.join("/", Arrays.copyOfRange(parts, 0, j + 1));
                if (!prevDir.startsWith(currentPath)) {
                    String prefix = "    ".repeat(j);
                    boolean isLast = (j == parts.length - 1);
                    projectStructure.append(prefix)
                            .append(isLast ? L_JUNCTION : T_JUNCTION)
                            .append(parts[j])
                            .append("\n");
                }
            }
            prevDir = path;
        }

        projectStructure.append("```");

        return projectStructure.toString();
    }

    private void extractFileMetadata(FileInfo fileInfo, String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("public class")) {
                String className = extractClassName(line);
                if (className != null) {
                    fileInfo.classes.add(className);
                }
            }

            if (line.contains("public") && (line.contains("(") && line.contains(")"))) {
                String methodName = extractMethodName(line);
                if (methodName != null) {
                    fileInfo.methods.add(methodName);
                }
            }
        }
    }

    private String extractClassName(String line) {
        String[] parts = line.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("class")) {
                return parts[i + 1].split("\\{")[0].trim();
            }
        }
        return null;
    }

    private String extractMethodName(String line) {
        String beforeParen = line.substring(0, line.indexOf("("));
        String[] parts = beforeParen.split("\\s+");
        if (parts.length > 2) {
            return parts[parts.length - 1];
        }
        return null;
    }

    private String buildPrompt(ProjectInfo projectInfo) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Analyse this codebase and generate a comprehensive README.md file. ");
        prompt.append("Focus on being clear, concise, and helpful for developers.\n\n");

        prompt.append("Project name: ").append(projectInfo.projectName).append("\n");
        prompt.append("Project description: ").append(projectInfo.projectDescription).append("\n");
        prompt.append("Project structure (exclude unnecessary items):\n").append(projectInfo.projectStructure).append("\n");

        prompt.append("Key classes and methods:\n");
        for (FileInfo file : projectInfo.files) {
            if (!file.classes.isEmpty() || !file.methods.isEmpty()) {
                prompt.append("File: ").append(file.fileName).append("\n");
                for (String className : file.classes) {
                    prompt.append("  Class: ").append(className).append("\n");
                }
                for (String method : file.methods) {
                    prompt.append("  Method: ").append(method).append("\n");
                }
            }
        }

        prompt.append("\nGenerate a README.md that includes:\n");
        prompt.append("1. Project Title and Description\n");
        prompt.append("2. Technologies Used\n");
        prompt.append("3. Project Structure\n");
        prompt.append("4. Main Features (based on code analysis)\n");
        prompt.append("5. How to Run/Setup\n");
        prompt.append("6. API Endpoints (if it's a web application)\n\n");
        prompt.append("Make it professional but approachable. Use proper markdown formatting.");

        return prompt.toString();
    }

    private boolean isCodeFile(String filename) {
        return filename != null && filename.endsWith(".java");
    }

    private static class ProjectInfo {
        String projectName;
        String projectDescription;
        String projectStructure;
        List<FileInfo> files = new ArrayList<>();
    }

    private static class FileInfo {
        String fileName;
        String content;
        List<String> classes = new ArrayList<>();
        List<String> methods = new ArrayList<>();
    }
}
