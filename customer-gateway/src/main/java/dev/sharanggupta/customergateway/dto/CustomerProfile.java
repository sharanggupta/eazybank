package dev.sharanggupta.customergateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerProfile(
        String name,
        String email,
        String mobileNumber,
        AccountInfo account,
        CardInfo card,
        LoanInfo loan
) {}
