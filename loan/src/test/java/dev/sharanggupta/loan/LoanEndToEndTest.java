package dev.sharanggupta.loan;

import dev.sharanggupta.loan.dto.LoanDto;
import dev.sharanggupta.loan.dto.ResponseDto;
import dev.sharanggupta.loan.repository.LoanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEndToEndTest extends BaseEndToEndTest {

    private static final String LOAN_API_PATH = "/api";
    private static final String VALID_MOBILE_NUMBER = "1234567890";
    private static final String HOME_LOAN_TYPE = "Home Loan";
    private static final int DEFAULT_TOTAL_LOAN = 500_000;
    private static final String STATUS_201 = "201";
    private static final String LOAN_CREATED_MESSAGE = "Loan created successfully";

    @Autowired
    private LoanRepository loanRepository;

    @AfterEach
    void tearDown() {
        loanRepository.deleteAll().block(); // block here is ok for cleanup
    }

    @Test
    @DisplayName("Should create a new loan")
    void shouldCreateLoan() {
        LoanDto loanRequest = createLoanRequest(VALID_MOBILE_NUMBER, HOME_LOAN_TYPE, DEFAULT_TOTAL_LOAN);

        client.post()
                .uri(LOAN_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loanRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ResponseDto.class)
                .value(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(STATUS_201);
                    assertThat(response.getStatusMessage()).isEqualTo(LOAN_CREATED_MESSAGE);
                });
    }

    @Test
    @DisplayName("Should fetch loan by mobile number")
    void shouldFetchLoanByMobileNumber() {
        LoanDto loanRequest = createLoanRequest(VALID_MOBILE_NUMBER, HOME_LOAN_TYPE, DEFAULT_TOTAL_LOAN);
        createLoan(loanRequest);

        client.get()
                .uri(LOAN_API_PATH + "/" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoanDto.class)
                .value(loan -> {
                    assertThat(loan.getMobileNumber()).isEqualTo(VALID_MOBILE_NUMBER);
                    assertThat(loan.getLoanType()).isEqualTo(HOME_LOAN_TYPE);
                    assertThat(loan.getTotalLoan()).isEqualTo(DEFAULT_TOTAL_LOAN);
                    assertThat(loan.getAmountPaid()).isZero();
                    assertThat(loan.getOutstandingAmount()).isEqualTo(DEFAULT_TOTAL_LOAN);
                    assertThat(loan.getLoanNumber()).isNotNull();
                    assertThat(loan.getLoanNumber()).hasSize(12);
                });
    }

    @Test
    @DisplayName("Should update loan details")
    void shouldUpdateLoan() {
        LoanDto loanRequest = createLoanRequest(VALID_MOBILE_NUMBER, HOME_LOAN_TYPE, DEFAULT_TOTAL_LOAN);
        createLoan(loanRequest);

        LoanDto existingLoan = fetchLoan(VALID_MOBILE_NUMBER);

        int amountPaid = 100_000;
        LoanDto updatedLoan = existingLoan.toBuilder()
                .amountPaid(amountPaid)
                .build();

        client.put()
                .uri(LOAN_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedLoan)
                .exchange()
                .expectStatus().isNoContent();

        LoanDto fetched = fetchLoan(VALID_MOBILE_NUMBER);
        assertThat(fetched.getAmountPaid()).isEqualTo(amountPaid);
        assertThat(fetched.getOutstandingAmount()).isEqualTo(DEFAULT_TOTAL_LOAN - amountPaid);
    }

    @Test
    @DisplayName("Should delete loan by mobile number")
    void shouldDeleteLoan() {
        LoanDto loanRequest = createLoanRequest(VALID_MOBILE_NUMBER, HOME_LOAN_TYPE, DEFAULT_TOTAL_LOAN);
        createLoan(loanRequest);

        client.delete()
                .uri(LOAN_API_PATH + "/" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isNoContent();

        client.get()
                .uri(LOAN_API_PATH + "/" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should reject duplicate loan creation")
    void shouldRejectDuplicateLoanCreation() {
        LoanDto loanRequest = createLoanRequest(VALID_MOBILE_NUMBER, HOME_LOAN_TYPE, DEFAULT_TOTAL_LOAN);
        createLoan(loanRequest);

        client.post()
                .uri(LOAN_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loanRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should return not found for non-existent loan")
    void shouldReturnNotFoundForNonExistentLoan() {
        client.get()
                .uri(LOAN_API_PATH + "/9999999999")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ----------------------
    // Helpers
    // ----------------------

    private void createLoan(LoanDto loanDto) {
        client.post()
                .uri(LOAN_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loanDto)
                .exchange()
                .expectStatus().isCreated();
    }

    private LoanDto fetchLoan(String mobileNumber) {
        return client.get()
                .uri(LOAN_API_PATH + "/" + mobileNumber)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoanDto.class)
                .returnResult()
                .getResponseBody();
    }

    private LoanDto createLoanRequest(String mobileNumber, String loanType, int totalLoan) {
        return LoanDto.builder()
                .mobileNumber(mobileNumber)
                .loanType(loanType)
                .totalLoan(totalLoan)
                .amountPaid(0)
                .build();
    }
}
