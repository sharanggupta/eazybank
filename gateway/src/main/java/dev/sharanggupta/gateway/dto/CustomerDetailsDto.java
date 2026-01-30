package dev.sharanggupta.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(name = "CustomerDetails", description = "Aggregated customer information including account, card, and loan details")
@Builder
public record CustomerDetailsDto(
        @Schema(description = "10-digit mobile number of the customer", example = "1234567890")
        String mobileNumber,

        @Schema(description = "Customer's account information")
        AccountInfoDto account,

        @Schema(description = "Customer's card information, null if no card exists")
        CardInfoDto card,

        @Schema(description = "Customer's loan information, null if no loan exists")
        LoanInfoDto loan
) {}
