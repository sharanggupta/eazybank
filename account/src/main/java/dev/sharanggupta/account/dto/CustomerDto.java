package dev.sharanggupta.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "Customer", description = "Schema to hold Customer and Account information")
public class CustomerDto {
  @NotEmpty(message = "Name cannot be empty")
  @Size(min = 5, max = 30, message = "Name must be between 5 and 30 characters")
  @Schema(description = "Name of the customer", example = "Eazy Bytes")
  private String name;

  @NotEmpty(message = "Email cannot be empty")
  @Email(message = "Email address format incorrect")
  @Schema(description = "Email address of the customer", example = "tutor@eazybytes.com")
  private String email;

  @NotEmpty(message = "Mobile number cannot be empty")
  @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
  @Schema(description = "Mobile Number of the customer", example = "9345432123")
  private String mobileNumber;

  @Schema(description = "Account details of the Customer")
  private AccountDto accountDto;
}
