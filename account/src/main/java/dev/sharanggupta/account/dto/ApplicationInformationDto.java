package dev.sharanggupta.account.dto;

import dev.sharanggupta.account.config.ApplicationSupport;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApplicationInformation", description = "Application information including build version, Java version, and support details")
public record ApplicationInformationDto(
        @Schema(description = "Application build version", example = "1.0.0")
        String buildVersion,
        @Schema(description = "Java version running the application", example = "25")
        String javaVersion,
        @Schema(description = "Application support information")
        ApplicationSupport support
) {
}