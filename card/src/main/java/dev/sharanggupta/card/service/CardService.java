package dev.sharanggupta.card.service;

import dev.sharanggupta.card.dto.CardCreateRequest;
import dev.sharanggupta.card.dto.CardDto;
import dev.sharanggupta.card.dto.CardUpdateRequest;
import reactor.core.publisher.Mono;

public interface CardService {

    Mono<Void> createCard(String mobileNumber, CardCreateRequest request);

    Mono<CardDto> fetchCard(String mobileNumber);

    Mono<Void> updateCard(String mobileNumber, CardUpdateRequest request);

    Mono<Void> deleteCard(String mobileNumber);
}
