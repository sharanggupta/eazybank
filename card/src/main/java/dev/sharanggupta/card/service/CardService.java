package dev.sharanggupta.card.service;

import dev.sharanggupta.card.dto.CardDto;
import reactor.core.publisher.Mono;

public interface CardService {

    Mono<Void> createCard(CardDto cardDto);

    Mono<CardDto> fetchCard(String mobileNumber);

    Mono<Void> updateCard(CardDto cardDto);

    Mono<Void> deleteCard(String mobileNumber);
}
