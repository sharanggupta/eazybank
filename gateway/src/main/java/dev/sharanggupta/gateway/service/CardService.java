package dev.sharanggupta.gateway.service;

import dev.sharanggupta.gateway.dto.CardInfoDto;
import dev.sharanggupta.gateway.dto.CreateCardRequest;
import dev.sharanggupta.gateway.dto.UpdateCardRequest;

import java.util.Optional;

public interface CardService {

    void issueCard(String mobileNumber, CreateCardRequest request);

    Optional<CardInfoDto> getCard(String mobileNumber);

    void updateCard(String mobileNumber, UpdateCardRequest request);

    void cancelCard(String mobileNumber);
}
