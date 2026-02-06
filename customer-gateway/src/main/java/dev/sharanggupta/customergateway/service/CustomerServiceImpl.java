package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.dto.CustomerDto;
import reactor.core.publisher.Mono;

public class CustomerServiceImpl implements CustomerService {
    @Override
    public Mono<Void> onboardCustomer(CustomerDto customerDto) {
        return null;
    }

    @Override
    public Mono<CustomerDto> getCustomerDetails(String mobileNumber) {
        return null;
    }

    @Override
    public Mono<Void> updateCustomer(CustomerDto customerDto) {
        return null;
    }

    @Override
    public Mono<Void> offboardCustomer(String mobileNumber) {
        return null;
    }
}
