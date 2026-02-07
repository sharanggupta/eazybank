package dev.sharanggupta.card.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Card extends BaseEntity {

    @Id
    @Column("card_id")
    private Long cardId;

    @Column("card_number")
    private String cardNumber;

    @Column("mobile_number")
    private String mobileNumber;

    @Column("card_type")
    private String cardType;

    @Column("total_limit")
    private int totalLimit;

    @Column("amount_used")
    private int amountUsed;

    @Column("available_amount")
    private int availableAmount;
}
