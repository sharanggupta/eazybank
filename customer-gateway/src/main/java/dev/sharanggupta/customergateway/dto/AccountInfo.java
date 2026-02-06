package dev.sharanggupta.customergateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountInfo(
        String accountNumber,
        String accountType,
        String branchAddress
) {}
