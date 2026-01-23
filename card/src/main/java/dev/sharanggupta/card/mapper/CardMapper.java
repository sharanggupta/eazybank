package dev.sharanggupta.card.mapper;

import dev.sharanggupta.card.dto.CardDto;
import dev.sharanggupta.card.entity.Card;

public class CardMapper {

    public static CardDto mapToCardDto(Card source, CardDto destination) {
        destination.setCardNumber(source.getCardNumber());
        destination.setMobileNumber(source.getMobileNumber());
        destination.setCardType(source.getCardType());
        destination.setTotalLimit(source.getTotalLimit());
        destination.setAmountUsed(source.getAmountUsed());
        return destination;
    }

    public static Card mapToCard(CardDto source, Card destination) {
        destination.setMobileNumber(source.getMobileNumber());
        destination.setCardType(source.getCardType());
        destination.setTotalLimit(source.getTotalLimit());
        destination.setAmountUsed(source.getAmountUsed());
        destination.setAvailableAmount(source.getAvailableAmount());
        return destination;
    }
}