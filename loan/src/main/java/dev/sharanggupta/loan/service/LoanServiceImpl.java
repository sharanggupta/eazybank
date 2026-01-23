package dev.sharanggupta.loan.service;

import dev.sharanggupta.loan.dto.LoanDto;
import dev.sharanggupta.loan.entity.Loan;
import dev.sharanggupta.loan.exception.LoanAlreadyExistsException;
import dev.sharanggupta.loan.exception.ResourceNotFoundException;
import dev.sharanggupta.loan.mapper.LoanMapper;
import dev.sharanggupta.loan.repository.LoanRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@AllArgsConstructor
@Transactional
public class LoanServiceImpl implements LoanService {

    private static final int LOAN_NUMBER_LENGTH = 12;
    private static final Random random = new Random();

    private final LoanRepository loanRepository;

    @Override
    public void createLoan(LoanDto loanDto) {
        validateLoanDoesNotExist(loanDto.getMobileNumber());
        Loan loan = LoanMapper.mapToLoan(loanDto, new Loan());
        loan.setLoanNumber(generateLoanNumber());
        loan.setAmountPaid(0);
        loanRepository.save(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanDto fetchLoan(String mobileNumber) {
        Loan loan = getLoanByMobileNumber(mobileNumber);
        return LoanMapper.mapToLoanDto(loan, new LoanDto());
    }

    @Override
    public void updateLoan(LoanDto loanDto) {
        Loan loan = getLoanByMobileNumber(loanDto.getMobileNumber());
        LoanMapper.mapToLoan(loanDto, loan);
        loanRepository.save(loan);
    }

    @Override
    public void deleteLoan(String mobileNumber) {
        Loan loan = getLoanByMobileNumber(mobileNumber);
        loanRepository.delete(loan);
    }

    private void validateLoanDoesNotExist(String mobileNumber) {
        loanRepository.findByMobileNumber(mobileNumber).ifPresent(loan -> {
            throw new LoanAlreadyExistsException(
                    "Loan already exists for mobile number " + mobileNumber);
        });
    }

    private Loan getLoanByMobileNumber(String mobileNumber) {
        return loanRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "mobileNumber", mobileNumber));
    }

    private String generateLoanNumber() {
        StringBuilder loanNumber = new StringBuilder(LOAN_NUMBER_LENGTH);
        for (int i = 0; i < LOAN_NUMBER_LENGTH; i++) {
            loanNumber.append(random.nextInt(10));
        }
        return loanNumber.toString();
    }
}