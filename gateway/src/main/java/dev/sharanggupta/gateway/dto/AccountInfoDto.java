package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(name = "AccountInfo", description = "Customer account information")
@Builder
public record AccountInfoDto(
        @Schema(description = "Customer's full name", example = "John Doe")
        String name,

        @Schema(description = "Customer's email address", example = "john.doe@example.com")
        String email,

        @Schema(description = "Unique account number (17-digit structured format)", example = "00010050123456789012")
        String accountNumber,

        @Schema(description = "Type of account", example = "Savings")
        String accountType,

        @Schema(description = "Branch address of the account", example = "123 Main Street, New York")
        String branchAddress
) {}
