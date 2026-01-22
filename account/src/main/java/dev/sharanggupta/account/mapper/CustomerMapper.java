package dev.sharanggupta.account.mapper;

import dev.sharanggupta.account.dto.CustomerDto;
import dev.sharanggupta.account.entity.Customer;

public class CustomerMapper {

    public static CustomerDto mapToCustomerDto(Customer source, CustomerDto destination) {
        destination.setName(source.getName());
        destination.setEmail(source.getEmail());
        destination.setMobileNumber(source.getMobileNumber());
        return destination;
    }

    public static Customer mapToCustomer(CustomerDto source, Customer destination) {
        destination.setName(source.getName());
        destination.setEmail(source.getEmail());
        destination.setMobileNumber(source.getMobileNumber());
        return destination;
    }
}