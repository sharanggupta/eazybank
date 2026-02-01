package dev.sharanggupta.card.service;

import dev.sharanggupta.card.dto.CardDto;
import dev.sharanggupta.card.entity.Card;
import dev.sharanggupta.card.exception.CardAlreadyExistsException;
import dev.sharanggupta.card.exception.ResourceNotFoundException;
import dev.sharanggupta.card.mapper.CardMapper;
import dev.sharanggupta.card.repository.CardRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Random;

@Service
@AllArgsConstructor
public class CardServiceImpl implements CardService {

    private static final int CARD_NUMBER_LENGTH = 16;
    private final CardRepository cardRepository;
    private final Random random = new Random();

    @Override
    public Mono<Void> createCard(CardDto cardDto) {
        Card card = CardMapper.mapToEntity(cardDto)
                .toBuilder()
                .cardNumber(generateCardNumber())
                .amountUsed(0)
                .availableAmount(cardDto.getTotalLimit())
                .build();

        return cardRepository.findByMobileNumber(cardDto.getMobileNumber())
                .flatMap(existing -> Mono.error(new CardAlreadyExistsException(
                        "Card already exists for mobile number " + cardDto.getMobileNumber()
                )))
                .switchIfEmpty(cardRepository.save(card))
                .then();
    }

    @Override
    public Mono<CardDto> fetchCard(String mobileNumber) {
        return getCardByMobileNumber(mobileNumber)
                .map(CardMapper::mapToDto);
    }

    @Override
    public Mono<Void> updateCard(CardDto cardDto) {
        return getCardByMobileNumber(cardDto.getMobileNumber())
                .map(existing -> CardMapper.updateEntity(cardDto, existing))
                .flatMap(cardRepository::save)
                .then();
    }

    @Override
    public Mono<Void> deleteCard(String mobileNumber) {
        return getCardByMobileNumber(mobileNumber)
                .flatMap(cardRepository::delete)
                .then();
    }

    // -----------------------
    // Helpers
    // -----------------------

    private Mono<Card> getCardByMobileNumber(String mobileNumber) {
        return cardRepository.findByMobileNumber(mobileNumber)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Card", "mobileNumber", mobileNumber
                )));
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder(CARD_NUMBER_LENGTH);
        for (int i = 0; i < CARD_NUMBER_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
