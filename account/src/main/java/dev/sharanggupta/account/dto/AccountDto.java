package dev.sharanggupta.account.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(name = "Account", description = "Schema to hold Account information")
public class AccountDto {

    @NotEmpty(message = "Account number cannot be empty")
    @Pattern(regexp = "^\\d{17}$", message = "Account number must be 17 digits")
    @Schema(description = "Account Number of Eazy Bank account", example = "00010050123456789012")
    private final String accountNumber;

    @NotEmpty(message = "Account type cannot be empty")
    @Schema(description = "Account Type of Eazy Bank account", example = "Savings")
    private final String accountType;

    @NotEmpty(message = "Branch address cannot be empty")
    @Schema(description = "Eazy Bank branch address", example = "123 NewYork")
    private final String branchAddress;

    @JsonCreator
    @Builder(toBuilder = true)
    public AccountDto(
            @JsonProperty("accountNumber") String accountNumber,
            @JsonProperty("accountType") String accountType,
            @JsonProperty("branchAddress") String branchAddress) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.branchAddress = branchAddress;
    }
}
