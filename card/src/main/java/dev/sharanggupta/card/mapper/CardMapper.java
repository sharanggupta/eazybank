package dev.sharanggupta.card.mapper;

import dev.sharanggupta.card.dto.CardDto;
import dev.sharanggupta.card.entity.Card;

public class CardMapper {

    // Card -> CardDto
    public static CardDto mapToDto(Card card) {
        return CardDto.builder()
                .mobileNumber(card.getMobileNumber())
                .cardNumber(card.getCardNumber())
                .cardType(card.getCardType())
                .totalLimit(card.getTotalLimit())
                .amountUsed(card.getAmountUsed())
                .build();
    }

    // CardDto -> Card (new)
    public static Card mapToEntity(CardDto dto) {
        return Card.builder()
                .mobileNumber(dto.getMobileNumber())
                .cardType(dto.getCardType())
                .totalLimit(dto.getTotalLimit())
                .amountUsed(dto.getAmountUsed())
                .availableAmount(dto.getAvailableAmount())
                .build();
    }

    // CardDto -> existing Card (for updates)
    public static Card updateEntity(CardDto dto, Card existing) {
        return existing.toBuilder()
                .cardType(dto.getCardType())
                .totalLimit(dto.getTotalLimit())
                .amountUsed(dto.getAmountUsed())
                .availableAmount(dto.getAvailableAmount())
                .build();
    }
}
