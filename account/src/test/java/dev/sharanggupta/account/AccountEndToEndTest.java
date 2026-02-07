package dev.sharanggupta.account;

import dev.sharanggupta.account.dto.AccountDto;
import dev.sharanggupta.account.dto.CustomerDto;
import dev.sharanggupta.account.dto.ResponseDto;
import dev.sharanggupta.account.repository.AccountRepository;
import dev.sharanggupta.account.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class AccountEndToEndTest extends BaseEndToEndTest {

    private static final String API_CREATE_PATH = "/api";
    private static final String API_FETCH_PATH = "/api";
    private static final String API_UPDATE_PATH = "/api";
    private static final String API_DELETE_PATH = "/api";

    private static final String VALID_NAME = "Test Customer";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_MOBILE_NUMBER = "1234567890";
    private static final String STATUS_201 = "201";
    private static final String ACCOUNT_CREATED_MESSAGE = "Account created successfully";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void tearDown() {
        accountRepository.deleteAll().block();
        customerRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Should create a new account")
    void shouldCreateAccount() {
        CustomerDto customerRequest = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);

        client.post()
                .uri(API_CREATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(customerRequest), CustomerDto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ResponseDto.class)
                .value(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(STATUS_201);
                    assertThat(response.getStatusMessage()).isEqualTo(ACCOUNT_CREATED_MESSAGE);
                });
    }

    @Test
    @DisplayName("Should fetch account by mobile number")
    void shouldFetchAccountByMobileNumber() {
        CustomerDto customerRequest = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);
        createAccount(customerRequest);

        client.get()
                .uri(API_FETCH_PATH + "/" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerDto.class)
                .value(customer -> {
                    assertThat(customer.getName()).isEqualTo(VALID_NAME);
                    assertThat(customer.getEmail()).isEqualTo(VALID_EMAIL);
                    assertThat(customer.getMobileNumber()).isEqualTo(VALID_MOBILE_NUMBER);
                    assertThat(customer.getAccount()).isNotNull();
                    assertThat(customer.getAccount().getAccountNumber()).isNotNull();
                });
    }

    @Test
    @DisplayName("Should update account details using mobile number")
    void shouldUpdateAccountDetails() {
        // Given: A customer account exists
        CustomerDto customerRequest = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);
        createAccount(customerRequest);

        // When: Update using mobile number (no need to know account number)
        CustomerDto updateRequest = CustomerDto.builder()
                .name("Updated Name")
                .email(VALID_EMAIL)
                .mobileNumber(VALID_MOBILE_NUMBER)
                .account(AccountDto.builder()
                        .accountType("Savings")
                        .branchAddress("456 New Address")
                        .build())
                .build();

        client.put()
                .uri(API_UPDATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateRequest), CustomerDto.class)
                .exchange()
                .expectStatus().isNoContent();

        // Then: Changes are persisted
        CustomerDto updatedCustomer = fetchAccount(VALID_MOBILE_NUMBER);
        assertThat(updatedCustomer.getName()).isEqualTo("Updated Name");
        assertThat(updatedCustomer.getAccount().getBranchAddress()).isEqualTo("456 New Address");
    }

    @Test
    @DisplayName("Should delete account by mobile number")
    void shouldDeleteAccountByMobileNumber() {
        CustomerDto customerRequest = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);
        createAccount(customerRequest);

        client.delete()
                .uri(API_DELETE_PATH + "/" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isNoContent();

        client.get()
                .uri(API_FETCH_PATH + "/" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should reject duplicate account creation")
    void shouldRejectDuplicateAccountCreation() {
        CustomerDto customerRequest = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);
        createAccount(customerRequest);

        client.post()
                .uri(API_CREATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(customerRequest), CustomerDto.class)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should return not found for non-existent account")
    void shouldReturnNotFoundForNonExistentAccount() {
        client.get()
                .uri(API_FETCH_PATH + "/9999999999")
                .exchange()
                .expectStatus().isNotFound();
    }

    private void createAccount(CustomerDto customerDto) {
        client.post()
                .uri(API_CREATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(customerDto), CustomerDto.class)
                .exchange()
                .expectStatus().isCreated();
    }

    private CustomerDto fetchAccount(String mobileNumber) {
        return client.get()
                .uri(API_FETCH_PATH + "/" + mobileNumber)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerDto.class)
                .returnResult()
                .getResponseBody();
    }

    private CustomerDto createCustomerRequest(String name, String email, String mobileNumber) {
        return CustomerDto.builder()
                .name(name)
                .email(email)
                .mobileNumber(mobileNumber)
                .build();
    }
}
