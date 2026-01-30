package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Response", description = "Standard success response for create operations")
public record ResponseDto(
        @Schema(description = "Status code of the response", example = "201")
        String statusCode,

        @Schema(description = "Status message describing the result", example = "Customer onboarded successfully")
        String statusMessage
) {}
