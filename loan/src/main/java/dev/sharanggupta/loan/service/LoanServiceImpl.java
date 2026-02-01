package dev.sharanggupta.loan.service;

import dev.sharanggupta.loan.dto.LoanDto;
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

    private static final int LOAN_NUMBER_LENGTH = 12;
    private static final Random random = new Random();

    private final LoanRepository loanRepository;

    @Override
    public Mono<Void> createLoan(LoanDto loanDto) {
        return validateLoanDoesNotExist(loanDto.getMobileNumber())
                .then(Mono.defer(() -> {
                    Loan loan = LoanMapper.mapToLoan(loanDto, new Loan());
                    loan.setLoanNumber(generateLoanNumber());
                    loan.setAmountPaid(0);
                    return loanRepository.save(loan);
                }))
                .then();
    }

    @Override
    public Mono<LoanDto> fetchLoan(String mobileNumber) {
        return getLoanByMobileNumber(mobileNumber)
                .map(loan -> LoanMapper.mapToLoanDto(loan, new LoanDto()));
    }

    @Override
    public Mono<Void> updateLoan(LoanDto loanDto) {
        return getLoanByMobileNumber(loanDto.getMobileNumber())
                .flatMap(loan -> {
                    LoanMapper.mapToLoan(loanDto, loan);
                    return loanRepository.save(loan);
                })
                .then();
    }

    @Override
    public Mono<Void> deleteLoan(String mobileNumber) {
        return getLoanByMobileNumber(mobileNumber)
                .flatMap(loanRepository::delete)
                .then();
    }

    private Mono<Void> validateLoanDoesNotExist(String mobileNumber) {
        return loanRepository.findByMobileNumber(mobileNumber)
                .flatMap(loan -> Mono.error(new LoanAlreadyExistsException(
                        "Loan already exists for mobile number " + mobileNumber)))
                .then();
    }

    private Mono<Loan> getLoanByMobileNumber(String mobileNumber) {
        return loanRepository.findByMobileNumber(mobileNumber)
                .switchIfEmpty(Mono.error(() -> new ResourceNotFoundException("Loan", "mobileNumber", mobileNumber)));
    }

    private String generateLoanNumber() {
        StringBuilder loanNumber = new StringBuilder(LOAN_NUMBER_LENGTH);
        for (int i = 0; i < LOAN_NUMBER_LENGTH; i++) {
            loanNumber.append(random.nextInt(10));
        }
        return loanNumber.toString();
    }
}