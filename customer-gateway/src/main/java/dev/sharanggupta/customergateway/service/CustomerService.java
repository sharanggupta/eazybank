package dev.sharanggupta.customergateway.service;

import dev.sharanggupta.customergateway.dto.CustomerAccount;
import dev.sharanggupta.customergateway.dto.CustomerProfile;
import reactor.core.publisher.Mono;

public interface CustomerService {
    Mono<Void> onboardCustomer(CustomerAccount customerAccount);
    Mono<CustomerProfile> getCustomerDetails(String mobileNumber);
    Mono<Void> updateCustomer(CustomerAccount customerAccount);
    Mono<Void> offboardCustomer(String mobileNumber);
}
