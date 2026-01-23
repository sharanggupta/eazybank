package dev.sharanggupta.loan.mapper;

import dev.sharanggupta.loan.dto.LoanDto;
import dev.sharanggupta.loan.entity.Loan;

public class LoanMapper {

    public static LoanDto mapToLoanDto(Loan source, LoanDto destination) {
        destination.setLoanNumber(source.getLoanNumber());
        destination.setMobileNumber(source.getMobileNumber());
        destination.setLoanType(source.getLoanType());
        destination.setTotalLoan(source.getTotalLoan());
        destination.setAmountPaid(source.getAmountPaid());
        return destination;
    }

    public static Loan mapToLoan(LoanDto source, Loan destination) {
        destination.setMobileNumber(source.getMobileNumber());
        destination.setLoanType(source.getLoanType());
        destination.setTotalLoan(source.getTotalLoan());
        destination.setAmountPaid(source.getAmountPaid());
        return destination;
    }
}