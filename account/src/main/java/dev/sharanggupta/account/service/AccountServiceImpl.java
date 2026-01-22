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

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    private static final String SAVINGS = "Savings";
    private static final String ADDRESS = "123 Main Street, New York";
    private static final Random random = new Random();

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
        AccountDto accountDto = AccountMapper.mapToAccountDto(account, new AccountDto());
        customerDto.setAccountDto(accountDto);
        return customerDto;
    }

    @Override
    public void updateAccount(CustomerDto customerDto) {
        AccountDto accountDto = Optional.ofNullable(customerDto.getAccountDto())
                .orElseThrow(() -> new AccountDetailsMissingException("Account details are required for an update."));

        Long accountNumber = Optional.ofNullable(accountDto.getAccountNumber())
                .orElseThrow(() -> new AccountDetailsMissingException("Account Number is required for an update."));

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
        customerRepository.findByMobileNumber(mobileNumber).ifPresent(c -> {
            throw new CustomerAlreadyExistsException("Customer already registered with given mobileNumber " + mobileNumber);
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
        Account newAccount = new Account();
        newAccount.setCustomerId(customer.getCustomerId());
        newAccount.setAccountNumber(generateNewAccountNumber());
        newAccount.setAccountType(SAVINGS);
        newAccount.setBranchAddress(ADDRESS);
        return newAccount;
    }

    private long generateNewAccountNumber() {
        return 1000000000L + random.nextInt(900000000);
    }
}
