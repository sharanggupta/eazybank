package dev.sharanggupta.loan.mapper;

import dev.sharanggupta.loan.dto.LoanDto;
import dev.sharanggupta.loan.entity.Loan;

public class LoanMapper {

    // Loan -> LoanDto
    public static LoanDto mapToDto(Loan loan) {
        return LoanDto.builder()
                .loanNumber(loan.getLoanNumber())
                .mobileNumber(loan.getMobileNumber())
                .loanType(loan.getLoanType())
                .totalLoan(loan.getTotalLoan())
                .amountPaid(loan.getAmountPaid())
                .build();
    }

    // LoanDto -> Loan (new entity)
    public static Loan mapToEntity(LoanDto dto) {
        return Loan.builder()
                .mobileNumber(dto.getMobileNumber())
                .loanType(dto.getLoanType())
                .totalLoan(dto.getTotalLoan())
                .amountPaid(dto.getAmountPaid())
                .build();
    }

    // LoanDto -> existing Loan (for updates)
    public static Loan updateEntity(LoanDto dto, Loan existing) {
        return existing.toBuilder()
                .loanType(dto.getLoanType())
                .totalLoan(dto.getTotalLoan())
                .amountPaid(dto.getAmountPaid())
                .build();
    }
}
