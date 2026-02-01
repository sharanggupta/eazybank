package dev.sharanggupta.loan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("loan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Loan extends BaseEntity {

    @Id
    @Column("loan_id")
    private Long loanId;

    @Column("loan_number")
    private String loanNumber;

    @Column("mobile_number")
    private String mobileNumber;

    @Column("loan_type")
    private String loanType;

    @Column("total_loan")
    private int totalLoan;

    @Column("amount_paid")
    private int amountPaid;
}