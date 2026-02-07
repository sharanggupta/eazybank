package dev.sharanggupta.customergateway.controller;

import dev.sharanggupta.customergateway.annotation.ValidMobileNumber;
import dev.sharanggupta.customergateway.dto.ApiResponse;
import dev.sharanggupta.customergateway.dto.CustomerAccount;
import dev.sharanggupta.customergateway.dto.CustomerProfile;
import dev.sharanggupta.customergateway.dto.ErrorResponse;
import dev.sharanggupta.customergateway.exception.ResourceNotFoundException;
import dev.sharanggupta.customergateway.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(
    name = "Customer Gateway API",
    description = "Customer-centric API for managing accounts, cards, and loans")
@RestController
@RequestMapping(path = "/api/customer", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class CustomerController {

    // Response constants
    private static final String STATUS_CREATED = "201";
    private static final String STATUS_OK = "200";
    private static final String MESSAGE_ONBOARDED = "Customer onboarded successfully";
    private static final String MESSAGE_UPDATED = "Customer details updated successfully";
    private static final String MESSAGE_OFFBOARDED = "Customer offboarded successfully";

    private final CustomerService customerService;

    @Operation(summary = "Onboard Customer", description = "Creates a new customer account")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Customer onboarded successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Customer already exists or validation failed",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/onboard")
    public Mono<ResponseEntity<ApiResponse>> onboardCustomer(@Valid @RequestBody CustomerAccount customerAccount) {
        return customerService.onboardCustomer(customerAccount)
                .then(Mono.just(createResponse(HttpStatus.CREATED, STATUS_CREATED, MESSAGE_ONBOARDED)));
    }

    @Operation(summary = "Get Customer Details", description = "Fetch customer details by mobile number")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/details/{mobileNumber}")
    public Mono<ResponseEntity<CustomerProfile>> getCustomerDetails(
            @PathVariable @ValidMobileNumber String mobileNumber) {
        return customerService.getCustomerDetails(mobileNumber)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)));
    }

    @Operation(summary = "Update Customer", description = "Update customer details")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/update")
    public Mono<ResponseEntity<ApiResponse>> updateCustomer(@Valid @RequestBody CustomerAccount customerAccount) {
        return customerService.updateCustomer(customerAccount)
                .then(Mono.just(createResponse(HttpStatus.OK, STATUS_OK, MESSAGE_UPDATED)));
    }

    @Operation(summary = "Offboard Customer", description = "Delete customer account")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "417", description = "Expectation Failed",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/offboard/{mobileNumber}")
    public Mono<ResponseEntity<ApiResponse>> offboardCustomer(
            @PathVariable @ValidMobileNumber String mobileNumber) {
        return customerService.offboardCustomer(mobileNumber)
                .then(Mono.just(createResponse(HttpStatus.OK, STATUS_OK, MESSAGE_OFFBOARDED)));
    }

    private ResponseEntity<ApiResponse> createResponse(HttpStatus httpStatus, String status, String message) {
        return ResponseEntity.status(httpStatus).body(new ApiResponse(status, message));
    }
}
