package dev.sharanggupta.loan.controller;

import dev.sharanggupta.loan.dto.ErrorResponseDto;
import dev.sharanggupta.loan.dto.LoanDto;
import dev.sharanggupta.loan.dto.ResponseDto;
import dev.sharanggupta.loan.service.LoanService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Loan REST APIs", description = "REST APIs to CREATE, UPDATE, FETCH and DELETE loan details")
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class LoanController {

    private static final String STATUS_201 = "201";
    private static final String MESSAGE_201 = "Loan created successfully";
    private static final String MOBILE_NUMBER_PATTERN = "^\\d{10}$";
    private static final String MOBILE_NUMBER_MESSAGE = "Mobile number must be 10 digits";

    private final LoanService loanService;

    @Operation(summary = "Create loan", description = "REST API to create a new loan")
    @ApiResponse(responseCode = "201", description = "Loan created successfully")
    @PostMapping
    public Mono<ResponseEntity<ResponseDto>> createLoan(@Valid @RequestBody LoanDto loanDto) {
        return loanService.createLoan(loanDto)
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(new ResponseDto(STATUS_201, MESSAGE_201)));
    }

    @Operation(summary = "Fetch loan", description = "REST API to fetch loan details by mobile number")
    @ApiResponse(responseCode = "200", description = "Loan fetched successfully")
    @ApiResponse(responseCode = "404", description = "Loan not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @GetMapping
    public Mono<ResponseEntity<LoanDto>> fetchLoan(
            @RequestParam @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE)
            String mobileNumber) {
        return loanService.fetchLoan(mobileNumber)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Update loan", description = "REST API to update loan details")
    @ApiResponse(responseCode = "204", description = "Loan updated successfully")
    @ApiResponse(responseCode = "404", description = "Loan not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @PutMapping
    public Mono<ResponseEntity<Void>> updateLoan(@Valid @RequestBody LoanDto loanDto) {
        return loanService.updateLoan(loanDto)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @Operation(summary = "Delete loan", description = "REST API to delete loan by mobile number")
    @ApiResponse(responseCode = "204", description = "Loan deleted successfully")
    @ApiResponse(responseCode = "404", description = "Loan not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @DeleteMapping
    public Mono<ResponseEntity<Void>> deleteLoan(
            @RequestParam @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE)
            String mobileNumber) {
        return loanService.deleteLoan(mobileNumber)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
