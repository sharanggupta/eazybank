package dev.sharanggupta.gateway.service;

import dev.sharanggupta.gateway.dto.CustomerDetailsDto;
import dev.sharanggupta.gateway.dto.OnboardCustomerRequest;
import dev.sharanggupta.gateway.dto.UpdateProfileRequest;

public interface CustomerService {

    CustomerDetailsDto getCustomerDetails(String mobileNumber);

    void onboardCustomer(OnboardCustomerRequest request);

    void updateProfile(String mobileNumber, UpdateProfileRequest request);

    void offboardCustomer(String mobileNumber);
}
