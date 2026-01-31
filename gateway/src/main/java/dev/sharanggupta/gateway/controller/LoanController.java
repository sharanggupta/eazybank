package dev.sharanggupta.gateway.controller;

import dev.sharanggupta.gateway.dto.CreateLoanRequest;
import dev.sharanggupta.gateway.dto.ErrorResponseDto;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.dto.ResponseDto;
import dev.sharanggupta.gateway.dto.UpdateLoanRequest;
import dev.sharanggupta.gateway.exception.ResourceNotFoundException;
import dev.sharanggupta.gateway.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Loan", description = "APIs for managing customer loans — apply, fetch, update, and close")
@RestController
@RequestMapping(path = "/api/customer/{mobileNumber}/loan", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class LoanController {

    private static final String STATUS_201 = "201";
    private static final String MESSAGE_201 = "Loan created successfully";
    private static final String MOBILE_NUMBER_PATTERN = "^\\d{10}$";
    private static final String MOBILE_NUMBER_MESSAGE = "Mobile number must be 10 digits";

    private final LoanService loanService;

    @Operation(summary = "Apply for a loan", description = "Creates a new loan for the customer identified by mobile number")
    @ApiResponse(responseCode = "201", description = "Loan created successfully")
    @PostMapping
    public ResponseEntity<ResponseDto> applyForLoan(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber,
            @Valid @RequestBody CreateLoanRequest request) {
        loanService.applyForLoan(mobileNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(STATUS_201, MESSAGE_201));
    }

    @Operation(summary = "Get loan details", description = "Fetches the loan information for the customer identified by mobile number")
    @ApiResponse(responseCode = "200", description = "Loan details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Loan not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @GetMapping
    public ResponseEntity<LoanInfoDto> getLoan(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber) {
        LoanInfoDto loan = loanService.getLoan(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "mobileNumber", mobileNumber));
        return ResponseEntity.ok(loan);
    }

    @Operation(summary = "Update loan", description = "Updates the loan details for the customer identified by mobile number")
    @ApiResponse(responseCode = "204", description = "Loan updated successfully")
    @ApiResponse(responseCode = "404", description = "Loan not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @PutMapping
    public ResponseEntity<Void> updateLoan(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber,
            @Valid @RequestBody UpdateLoanRequest request) {
        loanService.updateLoan(mobileNumber, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Close loan", description = "Closes the loan for the customer identified by mobile number")
    @ApiResponse(responseCode = "204", description = "Loan closed successfully")
    @ApiResponse(responseCode = "404", description = "Loan not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @DeleteMapping
    public ResponseEntity<Void> closeLoan(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber) {
        loanService.closeLoan(mobileNumber);
        return ResponseEntity.noContent().build();
    }
}
