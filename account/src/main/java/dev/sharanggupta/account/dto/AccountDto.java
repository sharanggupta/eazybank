package dev.sharanggupta.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(name = "Account", description = "Schema to hold Account information")
public class AccountDto {

  @NotEmpty(message = "Account number cannot be empty")
  @Pattern(regexp = "^\\d{10}$", message = "Account number must be 10 digits")
  @Schema(description = "Account Number of Eazy Bank account", example = "3454433243")
  private Long accountNumber;

  @NotEmpty(message = "Account type cannot be empty")
  @Schema(description = "Account Type of Eazy Bank account", example = "Savings")
  private String accountType;

  @NotEmpty(message = "Branch address cannot be empty")
  @Schema(description = "Eazy Bank branch address", example = "123 NewYork")
  private String branchAddress;
}
