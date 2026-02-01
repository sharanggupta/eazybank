package dev.sharanggupta.card.repository;

import dev.sharanggupta.card.entity.Card;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CardRepository extends ReactiveCrudRepository<Card, Long> {

    Mono<Card> findByMobileNumber(String mobileNumber);

    Mono<Card> findByCardNumber(String cardNumber);
}
