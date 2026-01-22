package dev.sharanggupta.loan.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Loan {

    @Id
    private String loanNumber;

    private String mobileNumber;

    private String loanType;

    private int totalLoan;

    private int amountPaid;
}