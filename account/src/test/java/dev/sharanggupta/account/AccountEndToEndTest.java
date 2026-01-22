package dev.sharanggupta.account;

import dev.sharanggupta.account.dto.CustomerDto;
import dev.sharanggupta.account.dto.ResponseDto;
import dev.sharanggupta.account.repository.AccountRepository;
import dev.sharanggupta.account.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class AccountEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void tearDown() {
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    private static final String API_CREATE_PATH = "/account/api/create";
    private static final String API_FETCH_PATH = "/account/api/fetch";
    private static final String API_UPDATE_PATH = "/account/api/update";
    private static final String API_DELETE_PATH = "/account/api/delete";

    private static final String VALID_NAME = "Test Customer";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_MOBILE_NUMBER = "1234567890";
    private static final String STATUS_201 = "201";

    @Test
    @DisplayName("Should create a new account")
    void shouldCreateAccount() {
        CustomerDto customer = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);

        client.post()
                .uri(API_CREATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(customer)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ResponseDto.class)
                .value(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(STATUS_201);
                    assertThat(response.getStatusMessage()).isEqualTo("Account created successfully");
                });
    }

    @Test
    @DisplayName("Should fetch account by mobile number")
    void shouldFetchAccountByMobileNumber() {
        CustomerDto customer = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);
        createAccount(customer);

        client.get()
                .uri(API_FETCH_PATH + "?mobileNumber=" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerDto.class)
                .value(fetchedCustomer -> {
                    assertThat(fetchedCustomer.getName()).isEqualTo(VALID_NAME);
                    assertThat(fetchedCustomer.getEmail()).isEqualTo(VALID_EMAIL);
                    assertThat(fetchedCustomer.getMobileNumber()).isEqualTo(VALID_MOBILE_NUMBER);
                    assertThat(fetchedCustomer.getAccountDto()).isNotNull();
                    assertThat(fetchedCustomer.getAccountDto().getAccountNumber()).isNotNull();
                });
    }

    @Test
    @DisplayName("Should update account details")
    void shouldUpdateAccountDetails() {
        CustomerDto customer = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);
        createAccount(customer);

        CustomerDto fetchedCustomer = fetchAccount(VALID_MOBILE_NUMBER);
        fetchedCustomer.setName("Updated Name");
        fetchedCustomer.getAccountDto().setBranchAddress("456 New Address");

        client.put()
                .uri(API_UPDATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(fetchedCustomer)
                .exchange()
                .expectStatus().isNoContent();

        CustomerDto updatedCustomer = fetchAccount(VALID_MOBILE_NUMBER);
        assertThat(updatedCustomer.getName()).isEqualTo("Updated Name");
        assertThat(updatedCustomer.getAccountDto().getBranchAddress()).isEqualTo("456 New Address");
    }

    @Test
    @DisplayName("Should delete account by mobile number")
    void shouldDeleteAccountByMobileNumber() {
        CustomerDto customer = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);
        createAccount(customer);

        client.delete()
                .uri(API_DELETE_PATH + "?mobileNumber=" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isNoContent();

        client.get()
                .uri(API_FETCH_PATH + "?mobileNumber=" + VALID_MOBILE_NUMBER)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should reject duplicate account creation")
    void shouldRejectDuplicateAccountCreation() {
        CustomerDto customer = createCustomerRequest(VALID_NAME, VALID_EMAIL, VALID_MOBILE_NUMBER);
        createAccount(customer);

        client.post()
                .uri(API_CREATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(customer)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should not find non-existent account")
    void shouldNotFindNonExistentAccount() {
        client.get()
                .uri(API_FETCH_PATH + "?mobileNumber=9999999999")
                .exchange()
                .expectStatus().isNotFound();
    }

    private void createAccount(CustomerDto customerDto) {
        client.post()
                .uri(API_CREATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(customerDto)
                .exchange()
                .expectStatus().isCreated();
    }

    private CustomerDto fetchAccount(String mobileNumber) {
        return client.get()
                .uri(API_FETCH_PATH + "?mobileNumber=" + mobileNumber)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerDto.class)
                .returnResult()
                .getResponseBody();
    }

    private CustomerDto createCustomerRequest(String name, String email, String mobileNumber) {
        CustomerDto customerDto = new CustomerDto();
        customerDto.setName(name);
        customerDto.setEmail(email);
        customerDto.setMobileNumber(mobileNumber);
        return customerDto;
    }
}