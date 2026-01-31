package dev.sharanggupta.gateway.service.impl;

import dev.sharanggupta.gateway.client.LoanServiceClient;
import dev.sharanggupta.gateway.dto.CreateLoanRequest;
import dev.sharanggupta.gateway.dto.LoanInfoDto;
import dev.sharanggupta.gateway.dto.UpdateLoanRequest;
import dev.sharanggupta.gateway.exception.LoanServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LoanServiceImplFallbackTest {

    private LoanServiceClient loanClient;
    private LoanServiceImpl loanService;

    @BeforeEach
    void setUp() {
        loanClient = mock(LoanServiceClient.class);
        loanService = new LoanServiceImpl(loanClient);
    }

    @Test
    @DisplayName("getLoan should return empty when loan not found")
    void getLoan_shouldReturnEmpty_whenNotFound() {
        String mobileNumber = "9876543210";
        when(loanClient.fetchLoanByMobileNumber(mobileNumber))
                .thenReturn(Optional.empty());

        Optional<LoanInfoDto> result = loanService.getLoan(mobileNumber);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getLoan should return loan when found")
    void getLoan_shouldReturnLoan_whenFound() {
        String mobileNumber = "9876543210";
        LoanInfoDto loan = mock(LoanInfoDto.class);
        when(loanClient.fetchLoanByMobileNumber(mobileNumber))
                .thenReturn(Optional.of(loan));

        Optional<LoanInfoDto> result = loanService.getLoan(mobileNumber);

        assertTrue(result.isPresent());
        assertEquals(loan, result.get());
    }

    @Test
    @DisplayName("getLoan should throw when service unavailable")
    void getLoan_shouldThrow_whenServiceUnavailable() {
        String mobileNumber = "9876543210";
        when(loanClient.fetchLoanByMobileNumber(mobileNumber))
                .thenThrow(new LoanServiceException("Service unavailable"));

        assertThrows(LoanServiceException.class, () -> loanService.getLoan(mobileNumber));
    }

    @Test
    @DisplayName("applyForLoan should throw when service unavailable")
    void applyForLoan_shouldThrow_whenServiceUnavailable() {
        String mobileNumber = "9876543210";
        CreateLoanRequest request = new CreateLoanRequest("HOME", 500000);

        doThrow(new LoanServiceException("Service unavailable"))
                .when(loanClient).createLoan(mobileNumber, request.loanType(), request.totalLoan());

        assertThrows(LoanServiceException.class, () -> loanService.applyForLoan(mobileNumber, request));
    }

    @Test
    @DisplayName("updateLoan should throw when service unavailable")
    void updateLoan_shouldThrow_whenServiceUnavailable() {
        String mobileNumber = "9876543210";
        UpdateLoanRequest request = new UpdateLoanRequest("HOME", 600000, 50000);

        doThrow(new LoanServiceException("Service unavailable"))
                .when(loanClient).updateLoan(mobileNumber, request.loanType(), request.totalLoan(), request.amountPaid());

        assertThrows(LoanServiceException.class, () -> loanService.updateLoan(mobileNumber, request));
    }

    @Test
    @DisplayName("closeLoan should throw when service unavailable")
    void closeLoan_shouldThrow_whenServiceUnavailable() {
        String mobileNumber = "9876543210";

        doThrow(new LoanServiceException("Service unavailable"))
                .when(loanClient).deleteLoan(mobileNumber);

        assertThrows(LoanServiceException.class, () -> loanService.closeLoan(mobileNumber));
    }

    @Test
    @DisplayName("Read fallback method is present")
    void readFallbackMethod_shouldBePresent() {
        assertTrue(java.util.Arrays.stream(LoanServiceImpl.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("getLoanFallback")),
                "Fallback method getLoanFallback should exist");
    }
}