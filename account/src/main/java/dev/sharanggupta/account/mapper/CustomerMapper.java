package dev.sharanggupta.account.mapper;

import dev.sharanggupta.account.dto.AccountDto;
import dev.sharanggupta.account.dto.CustomerDto;
import dev.sharanggupta.account.entity.Customer;

public class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerDto mapToDto(Customer customer, AccountDto account) {
        return CustomerDto.builder()
                .name(customer.getName())
                .email(customer.getEmail())
                .mobileNumber(customer.getMobileNumber())
                .account(account)
                .build();
    }

    public static CustomerDto mapToDto(Customer customer) {
        return mapToDto(customer, null);
    }

    public static Customer mapToEntity(CustomerDto dto) {
        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setMobileNumber(dto.getMobileNumber());
        return customer;
    }

    public static Customer updateEntity(CustomerDto dto, Customer customer) {
        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setMobileNumber(dto.getMobileNumber());
        return customer;
    }
}