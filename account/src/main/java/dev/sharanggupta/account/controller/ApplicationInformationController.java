package dev.sharanggupta.account.controller;

import dev.sharanggupta.account.config.ApplicationSupport;
import dev.sharanggupta.account.dto.ApplicationInformationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Application Information", description = "REST APIs to fetch application information and configuration")
@RestController
@RequestMapping(path = "/info")
@RequiredArgsConstructor
public class ApplicationInformationController {

    private static final String JAVA_VERSION_PROPERTY = "java.version";
    private static final String JAVA_VERSION_DEFAULT = "Unknown";

    @Value("${app.build.version:1.0.0}")
    private String buildVersion;

    private final Environment environment;
    private final ApplicationSupport applicationSupport;

    @Operation(summary = "Get application information", description = "REST API to fetch build version, Java version, and support contact information")
    @GetMapping
    public ResponseEntity<ApplicationInformationDto> getApplicationInfo() {
        String javaVersion = environment.getProperty(JAVA_VERSION_PROPERTY, JAVA_VERSION_DEFAULT);
        ApplicationInformationDto info = new ApplicationInformationDto(buildVersion, javaVersion, applicationSupport);
        return ResponseEntity.ok(info);
    }
}