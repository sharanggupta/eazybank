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
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final String DEFAULT_ACCOUNT_TYPE = "Savings";
    private static final String DEFAULT_BRANCH_ADDRESS = "123 Main Street, New York";

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    public Mono<Void> createAccount(CustomerDto customerDto) {
        return validateCustomerDoesNotExist(customerDto.getMobileNumber())
                .then(Mono.defer(() -> {
                    Customer customer = CustomerMapper.mapToEntity(customerDto);
                    return customerRepository.save(customer);
                }))
                .flatMap(savedCustomer -> {
                    Account account = createNewAccount(savedCustomer);
                    return accountRepository.save(account);
                })
                .then();
    }

    @Override
    public Mono<CustomerDto> fetchAccountDetails(String mobileNumber) {
        return customerRepository.findByMobileNumber(mobileNumber)
                .switchIfEmpty(Mono.error(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)))
                .flatMap(customer -> accountRepository.findByCustomerId(customer.getCustomerId())
                        .switchIfEmpty(Mono.error(() -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())))
                        .map(account -> CustomerMapper.mapToDto(customer, AccountMapper.mapToDto(account)))
                );
    }

    @Override
    public Mono<Void> updateAccount(CustomerDto customerDto) {
        String mobileNumber = customerDto.getMobileNumber();
        AccountDto accountDto = Optional.ofNullable(customerDto.getAccount())
                .orElseThrow(() -> new AccountDetailsMissingException("Account details are required for update"));

        return customerRepository.findByMobileNumber(mobileNumber)
                .switchIfEmpty(Mono.error(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)))
                .flatMap(customer -> accountRepository.findByCustomerId(customer.getCustomerId())
                        .switchIfEmpty(Mono.error(() -> new ResourceNotFoundException("Account", "mobileNumber", mobileNumber)))
                        .flatMap(account -> {
                            CustomerMapper.updateEntity(customerDto, customer);
                            AccountMapper.updateEntity(accountDto, account);
                            return customerRepository.save(customer)
                                    .then(accountRepository.save(account));
                        })
                )
                .then();
    }

    @Override
    public Mono<Void> deleteAccount(String mobileNumber) {
        return customerRepository.findByMobileNumber(mobileNumber)
                .switchIfEmpty(Mono.error(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)))
                .flatMap(customer -> accountRepository.deleteByCustomerId(customer.getCustomerId())
                        .then(customerRepository.delete(customer))
                );
    }

    private Mono<Void> validateCustomerDoesNotExist(String mobileNumber) {
        return customerRepository.findByMobileNumber(mobileNumber)
                .flatMap(customer -> Mono.error(new CustomerAlreadyExistsException(
                        "Customer already registered with mobile number " + mobileNumber)))
                .then();
    }

    private Account createNewAccount(Customer customer) {
        Account account = new Account();
        account.setCustomerId(customer.getCustomerId());
        account.setAccountNumber(accountNumberGenerator.generate());
        account.setAccountType(DEFAULT_ACCOUNT_TYPE);
        account.setBranchAddress(DEFAULT_BRANCH_ADDRESS);
        return account;
    }
}