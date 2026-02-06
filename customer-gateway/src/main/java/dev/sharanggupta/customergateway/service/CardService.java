package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.dto.CardInfo;
import reactor.core.publisher.Mono;

public interface CardService {
    Mono<CardInfo> fetchCard(String mobileNumber);

    Mono<Void> deleteCard(String mobileNumber);
}
