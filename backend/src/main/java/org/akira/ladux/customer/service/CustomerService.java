package org.akira.ladux.customer.service;

import org.akira.ladux.customer.dto.request.AdminCustomerUpdateRequest;
import org.akira.ladux.customer.dto.request.PhoneUpdateRequest;
import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.customer.enums.CustomerLevel;
import org.akira.ladux.identity.dto.request.UpdateInformationPersonal;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    PageResponse<CustomerResponse> getAllCustomers(Pageable pageable);

    PageResponse<CustomerResponse> getCustomersByLevel(CustomerLevel level, Pageable pageable);

    CustomerResponse getCustomerByUserId(int userId);

    CustomerResponse updateCustomer(int userId, AdminCustomerUpdateRequest request);

    PageResponse<CustomerResponse> searchCustomers(String name, String phone, Pageable pageable);

    UserResponse updatePhone(PhoneUpdateRequest request);

    UserResponse updateInformationPersonal(UpdateInformationPersonal request);

}
