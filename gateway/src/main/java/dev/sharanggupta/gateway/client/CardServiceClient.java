package dev.sharanggupta.gateway.client;

import dev.sharanggupta.gateway.dto.CardInfoDto;

import java.util.Optional;

public interface CardServiceClient {

    Optional<CardInfoDto> fetchCardByMobileNumber(String mobileNumber);

    void createCard(String mobileNumber, String cardType, int totalLimit);

    void updateCard(String mobileNumber, String cardType, int totalLimit, int amountUsed);

    void deleteCard(String mobileNumber);
}