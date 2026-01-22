package dev.sharanggupta.account.service;

import dev.sharanggupta.account.dto.AccountDto;
import dev.sharanggupta.account.dto.CustomerDto;
import dev.sharanggupta.account.entity.Account;
import dev.sharanggupta.account.entity.Customer;
import dev.sharanggupta.account.exception.AccountDetailsMissingException;
import dev.sharanggupta.account.exception.CustomerAlreadyExistsException;
import dev.sharanggupta.account.exception.ResourceNotFoundException;
import dev.sharanggupta.account.mapper.AccountMapper;
import dev.sharanggupta.account.mapper.CustomerMapper;
import dev.sharanggupta.account.repository.AccountRepository;
import dev.sharanggupta.account.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final String DEFAULT_ACCOUNT_TYPE = "Savings";
    private static final String DEFAULT_BRANCH_ADDRESS = "123 Main Street, New York";
    private static final Random random = new Random();

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    @Override
    public void createAccount(CustomerDto customerDto) {
        validateCustomerDoesNotExist(customerDto.getMobileNumber());
        Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
        Customer savedCustomer = customerRepository.save(customer);
        accountRepository.save(createNewAccount(savedCustomer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto fetchAccountDetails(String mobileNumber) {
        Customer customer = getCustomerByMobileNumber(mobileNumber);
        Account account = getAccountByCustomerId(customer.getCustomerId());
        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
        customerDto.setAccountDto(AccountMapper.mapToAccountDto(account, new AccountDto()));
        return customerDto;
    }

    @Override
    public void updateAccount(CustomerDto customerDto) {
        AccountDto accountDto = Optional.ofNullable(customerDto.getAccountDto())
                .orElseThrow(() -> new AccountDetailsMissingException("Account details are required for update"));

        Long accountNumber = Optional.ofNullable(accountDto.getAccountNumber())
                .orElseThrow(() -> new AccountDetailsMissingException("Account number is required for update"));

        Account account = getAccountByAccountNumber(accountNumber);
        Customer customer = getCustomerByCustomerId(account.getCustomerId());

        CustomerMapper.mapToCustomer(customerDto, customer);
        AccountMapper.mapToAccount(accountDto, account);
    }

    @Override
    public void deleteAccount(String mobileNumber) {
        Customer customer = getCustomerByMobileNumber(mobileNumber);
        accountRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepository.deleteById(customer.getCustomerId());
    }

    private void validateCustomerDoesNotExist(String mobileNumber) {
        customerRepository.findByMobileNumber(mobileNumber).ifPresent(customer -> {
            throw new CustomerAlreadyExistsException(
                    "Customer already registered with mobile number " + mobileNumber);
        });
    }

    private Customer getCustomerByMobileNumber(String mobileNumber) {
        return customerRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));
    }

    private Account getAccountByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "customerId", customerId.toString()));
    }

    private Account getAccountByAccountNumber(Long accountNumber) {
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber.toString()));
    }

    private Customer getCustomerByCustomerId(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", customerId.toString()));
    }

    private Account createNewAccount(Customer customer) {
        Account account = new Account();
        account.setCustomerId(customer.getCustomerId());
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(DEFAULT_ACCOUNT_TYPE);
        account.setBranchAddress(DEFAULT_BRANCH_ADDRESS);
        return account;
    }

    private long generateAccountNumber() {
        return 1000000000L + random.nextInt(900000000);
    }
}