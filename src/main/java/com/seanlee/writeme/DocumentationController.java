package com.seanlee.writeme;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class DocumentationController {

    private final DocumentationService documentationService;

    public DocumentationController(DocumentationService documentationService) {
        this.documentationService = documentationService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/generate-docs")
    public String generateDocs(
            @RequestParam("projectName") String projectName,
            @RequestParam("projectDescription") String projectDescription,
            @RequestParam("projectFolder") MultipartFile[] files,
            Model model
    ) {
        try {
            String readme = documentationService.generateReadme(projectName, projectDescription, files);
            model.addAttribute("readmeContent", readme);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error generating document" + e.getMessage());
        }
        return "index";
    }
}
