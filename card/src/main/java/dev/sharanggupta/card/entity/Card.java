package dev.sharanggupta.card.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "card")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Card extends BaseEntity {

    @Id
    @Column(length = 16)
    private String cardNumber;

    @Column(length = 15, nullable = false)
    private String mobileNumber;

    @Column(length = 100, nullable = false)
    private String cardType;

    @Column(nullable = false)
    private int totalLimit;

    @Column(nullable = false)
    private int amountUsed;

    @Column(nullable = false)
    private int availableAmount;
}
