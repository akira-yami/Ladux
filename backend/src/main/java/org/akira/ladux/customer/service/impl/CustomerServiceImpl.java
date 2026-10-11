package org.akira.ladux.customer.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.dto.request.AdminCustomerUpdateRequest;
import org.akira.ladux.customer.dto.request.PhoneUpdateRequest;
import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.enums.CustomerLevel;
import org.akira.ladux.customer.mapper.CustomerMapper;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.customer.service.CustomerService;
import org.akira.ladux.identity.dto.request.UpdateInformationPersonal;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.mapper.UserMapper;
import org.akira.ladux.identity.security.SecurityUtils;
import org.akira.ladux.identity.util.PhoneNumberUtils;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repo;
    private final PhoneNumberUtils phoneNumberUtils;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable).map(CustomerMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomersByLevel(CustomerLevel level, Pageable pageable) {
        return PageResponse.from(repo.findByLevel(level, pageable).map(CustomerMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByUserId(int userId) {
        return CustomerMapper.fromEntity(findOrThrow(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> searchCustomers(String name, String phone, Pageable pageable) {
        String searchName = cleanSearch(name);
        String searchPhone = cleanSearch(phone);
        if (searchPhone != null) {
            searchPhone = searchPhone.replaceAll("[\\s.()\\-]", "");
            try {
                searchPhone = phoneNumberUtils.normalize(searchPhone);
            } catch (IllegalArgumentException ignored) {
                // Keep raw input so partial/non-Vietnamese phone search still works.
            }
        }
        return PageResponse.from(repo.findByNameOrPhone(searchName, searchPhone, pageable));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(int userId, AdminCustomerUpdateRequest request) {
        Customer customer = findOrThrow(userId);
        if (request.fullName() != null) {
            customer.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            String normalizedPhone;
            try {
                normalizedPhone = phoneNumberUtils.normalize(request.phone());
            } catch (IllegalArgumentException exception) {
                throw new BusinessRuleException(exception.getMessage());
            }
            if (repo.existsByPhoneAndIdNot(normalizedPhone, userId)) {
                throw new BusinessRuleException("Số điện thoại đã được tài khoản khác sử dụng");
            }
            customer.setPhone(normalizedPhone);
        }
        if (request.avatarUrl() != null) {
            customer.setAvatarUrl(request.avatarUrl());
        }
        if (request.level() != null) {
            customer.setLevel(request.level());
        }
        if (request.loyaltyPoints() != null) {
            customer.setLoyaltyPoints(request.loyaltyPoints());
        }
        if (request.totalSpent() != null) {
            customer.setTotalSpent(request.totalSpent());
        }
        return CustomerMapper.fromEntity(customer);
    }
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateInformationPersonal(UpdateInformationPersonal request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Customer customer = findOrThrow(userId);
        customer.setFullName(request.fullName().trim());
        return UserMapper.fromEntity(customer.getUser());
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updatePhone(PhoneUpdateRequest request) {
        Customer customer = findOrThrow(SecurityUtils.getCurrentUserId());
        String phone;
        try {
            phone = phoneNumberUtils.normalize(request.phone());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        if (repo.existsByPhoneAndIdNot(phone, customer.getId())) {
            throw new BusinessRuleException("Số điện thoại đã được tài khoản khác sử dụng");
        }
        customer.setPhone(phone);
        return UserMapper.fromEntity(customer.getUser());
    }

    private Customer findOrThrow(int userId) {
        return repo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khach hang voi userId = " + userId));
    }

    private String cleanSearch(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
