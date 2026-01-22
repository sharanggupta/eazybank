package dev.sharanggupta.account.controller;

import dev.sharanggupta.account.dto.CustomerDto;
import dev.sharanggupta.account.dto.ErrorResponseDto;
import dev.sharanggupta.account.dto.ResponseDto;
import dev.sharanggupta.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "CRUD REST APIs for Account in EazyBank",
    description = "CRUD REST APIs in EazyBank to CREATE, UPDATE, FETCH AND DELETE account details")
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class AccountController {

  private static final String MESSAGE_201 = "Account created successfully";
  private static final String STATUS_201 = "201";
  private static final String MOBILE_NUMBER_PATTERN = "^\\d{10}$";
  private static final String MOBILE_NUMBER_MESSAGE = "Mobile number must be 10 digits";

  private final AccountService accountService;

  @Operation(
      summary = "Create an account",
      description = "REST API to create a new customer and its account in EazyBank. ")
  @ApiResponse(responseCode = "201", description = "HTTP Status CREATED")
  @ApiResponse(
      responseCode = "500",
      description = "HTTP Status Internal Server Error",
      content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  @PostMapping("/create")
  public ResponseEntity<ResponseDto> createAccount(@Valid @RequestBody CustomerDto customerDto) {
    accountService.createAccount(customerDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDto(STATUS_201, MESSAGE_201));
  }

  @Operation(
      summary = "Fetch Account Details REST API",
      description = "REST API to fetch Customer &  Account details based on a mobile number")
  @ApiResponse(responseCode = "200", description = "HTTP Status OK")
  @ApiResponse(
      responseCode = "500",
      description = "HTTP Status Internal Server Error",
      content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  @GetMapping("/fetch")
  public ResponseEntity<CustomerDto> fetchAccountDetails(
      @RequestParam @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE)
          String mobileNumber) {
    CustomerDto customerDto = accountService.fetchAccountDetails(mobileNumber);
    return ResponseEntity.status(HttpStatus.OK).body(customerDto);
  }

  @Operation(
      summary = "Update Account Details REST API",
      description = "REST API to update Customer &  Account details based on a account number")
  @ApiResponse(responseCode = "204", description = "HTTP Status NO CONTENT")
  @ApiResponse(
      responseCode = "500",
      description = "HTTP Status Internal Server Error",
      content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  @PutMapping("/update")
  public ResponseEntity<Void> updateAccountDetails(@Valid @RequestBody CustomerDto customerDto) {
    accountService.updateAccount(customerDto);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Delete Account & Customer Details REST API",
      description = "REST API to delete Customer &  Account details based on a mobile number")
  @ApiResponse(responseCode = "204", description = "HTTP Status NO CONTENT")
  @ApiResponse(
      responseCode = "500",
      description = "HTTP Status Internal Server Error",
      content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  @DeleteMapping("/delete")
  public ResponseEntity<Void> deleteAccountDetails(
      @RequestParam @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE)
          String mobileNumber) {
    accountService.deleteAccount(mobileNumber);
    return ResponseEntity.noContent().build();
  }
}
