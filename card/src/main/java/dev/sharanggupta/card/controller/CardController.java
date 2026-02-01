package dev.sharanggupta.card.controller;

import dev.sharanggupta.card.dto.CardDto;
import dev.sharanggupta.card.dto.ErrorResponseDto;
import dev.sharanggupta.card.dto.ResponseDto;
import dev.sharanggupta.card.service.CardService;
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

@Tag(name = "Card REST APIs", description = "REST APIs to CREATE, UPDATE, FETCH and DELETE card details")
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class CardController {

    private static final String STATUS_201 = "201";
    private static final String MESSAGE_201 = "Card created successfully";
    private static final String MOBILE_NUMBER_PATTERN = "^\\d{10}$";
    private static final String MOBILE_NUMBER_MESSAGE = "Mobile number must be 10 digits";

    private final CardService cardService;

    @Operation(summary = "Create card", description = "REST API to create a new card")
    @ApiResponse(responseCode = "201", description = "Card created successfully")
    @PostMapping
    public Mono<ResponseEntity<ResponseDto>> createCard(
            @Valid @RequestBody CardDto cardDto) {

        return cardService.createCard(cardDto)
                .thenReturn(
                        ResponseEntity.status(HttpStatus.CREATED)
                                .body(new ResponseDto(STATUS_201, MESSAGE_201))
                );
    }

    @Operation(summary = "Fetch card", description = "REST API to fetch card details by mobile number")
    @ApiResponse(responseCode = "200", description = "Card fetched successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
    )
    @GetMapping
    public Mono<ResponseEntity<CardDto>> fetchCard(
            @RequestParam
            @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE)
            String mobileNumber) {

        return cardService.fetchCard(mobileNumber)
                .map(ResponseEntity::ok);
    }


    @Operation(summary = "Update card", description = "REST API to update card details")
    @ApiResponse(responseCode = "204", description = "Card updated successfully")
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @PutMapping
    public Mono<ResponseEntity<Void>> updateCard(@Valid @RequestBody CardDto cardDto) {
        return cardService.updateCard(cardDto)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @Operation(summary = "Delete card", description = "REST API to delete card by mobile number")
    @ApiResponse(responseCode = "204", description = "Card deleted successfully")
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @DeleteMapping
    public Mono<ResponseEntity<Void>> deleteCard(
            @RequestParam @Pattern(regexp = MOBILE_NUMBER_PATTERN, message = MOBILE_NUMBER_MESSAGE)
            String mobileNumber) {
        return cardService.deleteCard(mobileNumber)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}