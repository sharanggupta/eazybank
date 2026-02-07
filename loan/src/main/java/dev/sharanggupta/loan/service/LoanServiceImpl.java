package dev.sharanggupta.loan.service;

import dev.sharanggupta.loan.dto.LoanCreateRequest;
import dev.sharanggupta.loan.dto.LoanDto;
import dev.sharanggupta.loan.dto.LoanUpdateRequest;
import dev.sharanggupta.loan.entity.Loan;
import dev.sharanggupta.loan.exception.LoanAlreadyExistsException;
import dev.sharanggupta.loan.exception.ResourceNotFoundException;
import dev.sharanggupta.loan.mapper.LoanMapper;
import dev.sharanggupta.loan.repository.LoanRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Random;

@Service
@AllArgsConstructor
public class LoanServiceImpl implements LoanService {

    private static final int LOAN_NUMBER_LENGTH = 12; // example length
    private final LoanRepository loanRepository;
    private final Random random = new Random();

    @Override
    public Mono<Void> createLoan(String mobileNumber, LoanCreateRequest request) {
        Loan loan = Loan.builder()
                .mobileNumber(mobileNumber)
                .loanNumber(generateLoanNumber())
                .loanType(request.getLoanType())
                .totalLoan(request.getTotalLoan())
                .amountPaid(0)
                .build();

        return loanRepository.findByMobileNumber(mobileNumber)
                .flatMap(existing -> Mono.error(new LoanAlreadyExistsException(
                        "Loan already exists for mobile number " + mobileNumber
                )))
                .switchIfEmpty(loanRepository.save(loan))
                .then();
    }

    @Override
    public Mono<LoanDto> fetchLoan(String mobileNumber) {
        return getLoanByMobileNumber(mobileNumber)
                .map(LoanMapper::mapToDto);
    }

    @Override
    public Mono<Void> updateLoan(String mobileNumber, LoanUpdateRequest request) {
        return getLoanByMobileNumber(mobileNumber)
                .map(existing -> {
                    existing.setLoanNumber(request.getLoanNumber());
                    existing.setLoanType(request.getLoanType());
                    existing.setTotalLoan(request.getTotalLoan());
                    existing.setAmountPaid(request.getAmountPaid());
                    return existing;
                })
                .flatMap(loanRepository::save)
                .then();
    }

    @Override
    public Mono<Void> deleteLoan(String mobileNumber) {
        return getLoanByMobileNumber(mobileNumber)
                .flatMap(loanRepository::delete)
                .then();
    }

    // -----------------------
    // Helpers
    // -----------------------

    private Mono<Loan> getLoanByMobileNumber(String mobileNumber) {
        return loanRepository.findByMobileNumber(mobileNumber)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Loan", "mobileNumber", mobileNumber
                )));
    }

    private String generateLoanNumber() {
        StringBuilder sb = new StringBuilder(LOAN_NUMBER_LENGTH);
        for (int i = 0; i < LOAN_NUMBER_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
