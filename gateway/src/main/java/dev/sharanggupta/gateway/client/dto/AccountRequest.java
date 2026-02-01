package dev.sharanggupta.gateway.client.dto;

public record AccountRequest(
        String name,
        String email,
        String mobileNumber,
        AccountDto accountDto
) {
    public record AccountDto(
            String accountNumber,
            String accountType,
            String branchAddress
    ) {}
}