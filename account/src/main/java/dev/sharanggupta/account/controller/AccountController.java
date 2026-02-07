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
import reactor.core.publisher.Mono;

@Tag(name = "Account REST APIs", description = "REST APIs to CREATE, UPDATE, FETCH and DELETE account details")
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class AccountController {

    private static final String MOBILE_PATTERN = "^\\d{10}$";
    private static final String MOBILE_MESSAGE = "Mobile number must be 10 digits";

    private final AccountService accountService;

    @Operation(summary = "Create account", description = "REST API to create a new customer and account")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Customer already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @PostMapping
    public Mono<ResponseEntity<ResponseDto>> createAccount(@Valid @RequestBody CustomerDto customerDto) {
        return accountService.createAccount(customerDto)
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED)
                        .body(new ResponseDto(String.valueOf(HttpStatus.CREATED.value()), "Account created successfully"))));
    }

    @Operation(summary = "Fetch account", description = "REST API to fetch customer and account details by mobile number")
    @ApiResponse(responseCode = "200", description = "Account fetched successfully")
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @GetMapping("/{mobileNumber}")
    public Mono<ResponseEntity<CustomerDto>> fetchAccountDetails(
            @PathVariable @Pattern(regexp = MOBILE_PATTERN, message = MOBILE_MESSAGE) String mobileNumber) {
        return accountService.fetchAccountDetails(mobileNumber)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Update account", description = "REST API to update customer and account details")
    @ApiResponse(responseCode = "204", description = "Account updated successfully")
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @PutMapping
    public Mono<ResponseEntity<Void>> updateAccountDetails(@Valid @RequestBody CustomerDto customerDto) {
        return accountService.updateAccount(customerDto)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @Operation(summary = "Delete account", description = "REST API to delete customer and account by mobile number")
    @ApiResponse(responseCode = "204", description = "Account deleted successfully")
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @DeleteMapping("/{mobileNumber}")
    public Mono<ResponseEntity<Void>> deleteAccountDetails(
            @PathVariable @Pattern(regexp = MOBILE_PATTERN, message = MOBILE_MESSAGE) String mobileNumber) {
        return accountService.deleteAccount(mobileNumber)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
