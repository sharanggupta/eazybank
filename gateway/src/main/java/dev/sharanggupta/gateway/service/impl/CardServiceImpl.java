package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.client.CardServiceClient;
import dev.sharanggupta.gateway.dto.CardInfoDto;
import dev.sharanggupta.gateway.dto.CreateCardRequest;
import dev.sharanggupta.gateway.dto.UpdateCardRequest;
import dev.sharanggupta.gateway.exception.ResourceNotFoundException;
import dev.sharanggupta.gateway.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardServiceClient cardClient;

    @Override
    public void issueCard(String mobileNumber, CreateCardRequest request) {
        log.info("Issuing card for mobile number: {}", mobileNumber);
        cardClient.createCard(mobileNumber, request.cardType(), request.totalLimit());
    }

    @Override
    public CardInfoDto getCard(String mobileNumber) {
        log.info("Fetching card for mobile number: {}", mobileNumber);
        return cardClient.fetchCardByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "mobileNumber", mobileNumber));
    }

    @Override
    public void updateCard(String mobileNumber, UpdateCardRequest request) {
        log.info("Updating card for mobile number: {}", mobileNumber);
        cardClient.updateCard(mobileNumber, request.cardType(), request.totalLimit(), request.amountUsed());
    }

    @Override
    public void cancelCard(String mobileNumber) {
        log.info("Cancelling card for mobile number: {}", mobileNumber);
        cardClient.deleteCard(mobileNumber);
    }
}
