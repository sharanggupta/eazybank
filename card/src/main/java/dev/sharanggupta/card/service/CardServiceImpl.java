package dev.sharanggupta.card.service;

import dev.sharanggupta.card.dto.CardDto;
import dev.sharanggupta.card.entity.Card;
import dev.sharanggupta.card.exception.ResourceNotFoundException;
import dev.sharanggupta.card.mapper.CardMapper;
import dev.sharanggupta.card.repository.CardRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@AllArgsConstructor
@Transactional
public class CardServiceImpl implements CardService {

    private static final int CARD_NUMBER_LENGTH = 16;
    private static final Random random = new Random();

    private final CardRepository cardRepository;

    @Override
    public void createCard(CardDto cardDto) {
        Card card = CardMapper.mapToCard(cardDto, new Card());
        card.setCardNumber(generateCardNumber());
        card.setAvailableAmount(cardDto.getTotalLimit());
        card.setAmountUsed(0);
        cardRepository.save(card);
    }

    @Override
    @Transactional(readOnly = true)
    public CardDto fetchCard(String mobileNumber) {
        Card card = getCardByMobileNumber(mobileNumber);
        return CardMapper.mapToCardDto(card, new CardDto());
    }

    @Override
    public void updateCard(CardDto cardDto) {
        Card card = getCardByMobileNumber(cardDto.getMobileNumber());
        CardMapper.mapToCard(cardDto, card);
        cardRepository.save(card);
    }

    @Override
    public void deleteCard(String mobileNumber) {
        Card card = getCardByMobileNumber(mobileNumber);
        cardRepository.delete(card);
    }

    private Card getCardByMobileNumber(String mobileNumber) {
        return cardRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "mobileNumber", mobileNumber));
    }

    private String generateCardNumber() {
        StringBuilder cardNumber = new StringBuilder(CARD_NUMBER_LENGTH);
        for (int i = 0; i < CARD_NUMBER_LENGTH; i++) {
            cardNumber.append(random.nextInt(10));
        }
        return cardNumber.toString();
    }
}