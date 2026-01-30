package dev.sharanggupta.gateway.controller;

import dev.sharanggupta.gateway.dto.CustomerDetailsDto;
import dev.sharanggupta.gateway.dto.ErrorResponseDto;
import dev.sharanggupta.gateway.dto.OnboardCustomerRequest;
import dev.sharanggupta.gateway.dto.ResponseDto;
import dev.sharanggupta.gateway.dto.UpdateProfileRequest;
import dev.sharanggupta.gateway.service.CustomerService;
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

@Tag(name = "Customer", description = "APIs for customer lifecycle — onboard, fetch details, update profile, and offboard")
@RestController
@RequestMapping(path = "/api/customer", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private static final String STATUS_201 = "201";
    private static final String MESSAGE_201 = "Customer onboarded successfully";
    private static final String MOBILE_NUMBER_PATTERN = "^\\d{10}$";
    private static final String MOBILE_NUMBER_MESSAGE = "Mobile number must be 10 digits";

    private final CustomerService customerService;

    @Operation(summary = "Onboard a new customer", description = "Creates a new customer account with the provided name, email, and mobile number")
    @ApiResponse(responseCode = "201", description = "Customer onboarded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @PostMapping
    public ResponseEntity<ResponseDto> onboardCustomer(@Valid @RequestBody OnboardCustomerRequest request) {
        customerService.onboardCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(STATUS_201, MESSAGE_201));
    }

    @Operation(summary = "Get customer details", description = "Fetches aggregated customer information including account, card, and loan details")
    @ApiResponse(responseCode = "200", description = "Customer details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @GetMapping("/{mobileNumber}")
    public ResponseEntity<CustomerDetailsDto> getCustomerDetails(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber) {
        return ResponseEntity.ok(customerService.getCustomerDetails(mobileNumber));
    }

    @Operation(summary = "Update customer profile", description = "Updates the customer's name and email for the given mobile number")
    @ApiResponse(responseCode = "204", description = "Profile updated successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @PutMapping("/{mobileNumber}")
    public ResponseEntity<Void> updateProfile(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber,
            @Valid @RequestBody UpdateProfileRequest request) {
        customerService.updateProfile(mobileNumber, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Offboard customer", description = "Deletes the customer's account, card, and loan data")
    @ApiResponse(responseCode = "204", description = "Customer offboarded successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @DeleteMapping("/{mobileNumber}")
    public ResponseEntity<Void> offboardCustomer(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber) {
        customerService.offboardCustomer(mobileNumber);
        return ResponseEntity.noContent().build();
    }
}
