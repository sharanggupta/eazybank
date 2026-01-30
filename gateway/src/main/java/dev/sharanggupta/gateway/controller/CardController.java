package dev.sharanggupta.gateway.controller;

import dev.sharanggupta.gateway.dto.CardInfoDto;
import dev.sharanggupta.gateway.dto.CreateCardRequest;
import dev.sharanggupta.gateway.dto.ErrorResponseDto;
import dev.sharanggupta.gateway.dto.ResponseDto;
import dev.sharanggupta.gateway.dto.UpdateCardRequest;
import dev.sharanggupta.gateway.service.CardService;
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

@Tag(name = "Card", description = "APIs for managing customer cards — issue, fetch, update, and cancel")
@RestController
@RequestMapping(path = "/api/customer/{mobileNumber}/card", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class CardController {

    private static final String STATUS_201 = "201";
    private static final String MESSAGE_201 = "Card issued successfully";
    private static final String MOBILE_NUMBER_PATTERN = "^\\d{10}$";
    private static final String MOBILE_NUMBER_MESSAGE = "Mobile number must be 10 digits";

    private final CardService cardService;

    @Operation(summary = "Issue a card", description = "Issues a new card for the customer identified by mobile number")
    @ApiResponse(responseCode = "201", description = "Card issued successfully")
    @PostMapping
    public ResponseEntity<ResponseDto> issueCard(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber,
            @Valid @RequestBody CreateCardRequest request) {
        cardService.issueCard(mobileNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(STATUS_201, MESSAGE_201));
    }

    @Operation(summary = "Get card details", description = "Fetches the card information for the customer identified by mobile number")
    @ApiResponse(responseCode = "200", description = "Card details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @GetMapping
    public ResponseEntity<CardInfoDto> getCard(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber) {
        return ResponseEntity.ok(cardService.getCard(mobileNumber));
    }

    @Operation(summary = "Update card", description = "Updates the card details for the customer identified by mobile number")
    @ApiResponse(responseCode = "204", description = "Card updated successfully")
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @PutMapping
    public ResponseEntity<Void> updateCard(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber,
            @Valid @RequestBody UpdateCardRequest request) {
        cardService.updateCard(mobileNumber, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel card", description = "Cancels the card for the customer identified by mobile number")
    @ApiResponse(responseCode = "204", description = "Card cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @DeleteMapping
    public ResponseEntity<Void> cancelCard(
            @PathVariable @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE) String mobileNumber) {
        cardService.cancelCard(mobileNumber);
        return ResponseEntity.noContent().build();
    }
}
