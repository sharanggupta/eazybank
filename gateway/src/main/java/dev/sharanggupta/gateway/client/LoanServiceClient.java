package dev.sharanggupta.gateway.client;

import dev.sharanggupta.gateway.dto.LoanInfoDto;

import java.util.Optional;

public interface LoanServiceClient {

    Optional<LoanInfoDto> fetchLoanByMobileNumber(String mobileNumber);

    void createLoan(String mobileNumber, String loanType, int totalLoan);

    void updateLoan(String mobileNumber, String loanType, int totalLoan, int amountPaid);

    void deleteLoan(String mobileNumber);
}