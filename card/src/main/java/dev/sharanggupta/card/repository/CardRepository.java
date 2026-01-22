package dev.sharanggupta.card.repository;

import dev.sharanggupta.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    Optional<Card> findByMobileNumber(String mobileNumber);
}
