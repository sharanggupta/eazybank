package dev.sharanggupta.card.service;

import dev.sharanggupta.card.dto.CardDto;

public interface CardService {

    void createCard(CardDto cardDto);

    CardDto fetchCard(String mobileNumber);

    void updateCard(CardDto cardDto);

    void deleteCard(String mobileNumber);
}
