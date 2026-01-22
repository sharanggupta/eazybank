package dev.sharanggupta.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loan")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Loan extends BaseEntity {

    @Id
    @Column(length = 12)
    private String loanNumber;

    @Column(length = 15, nullable = false)
    private String mobileNumber;

    @Column(length = 100, nullable = false)
    private String loanType;

    @Column(nullable = false)
    private int totalLoan;

    @Column(nullable = false)
    private int amountPaid;
}