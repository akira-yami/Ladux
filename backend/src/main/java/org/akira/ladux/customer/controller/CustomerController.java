package org.akira.ladux.customer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.dto.request.AdminCustomerUpdateRequest;
import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.customer.enums.CustomerLevel;
import org.akira.ladux.customer.service.CustomerService;
import org.akira.ladux.identity.dto.request.EmailRegisterRequest;
import org.akira.ladux.identity.dto.request.EmailVerifyRequest;
import org.akira.ladux.identity.dto.request.PhoneRegisterRequest;
import org.akira.ladux.identity.dto.request.PhoneVerifyRequest;
import org.akira.ladux.identity.dto.request.UpdateInformationPersonal;
import org.akira.ladux.identity.dto.response.EmailOtpSendResponse;
import org.akira.ladux.identity.dto.response.OtpSendResponse;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.service.DistributedRateLimitService;
import org.akira.ladux.identity.service.EmailVerificationService;
import org.akira.ladux.identity.service.PhoneVerificationService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/customers/me", "/api/v1/admin/customers"})
@RequiredArgsConstructor
public class CustomerController {

    private final PhoneVerificationService phoneVerificationService;
    private final CustomerService customerService;
    private final EmailVerificationService emailVerificationService;
    private final DistributedRateLimitService rateLimitService;

    @PostMapping("/phone/otp")
    public ResponseEntity<OtpSendResponse> sendPhoneOtp(@Valid @RequestBody PhoneRegisterRequest request) {
        rateLimitService.checkOtpDestination("phone", request.phone());
        return ResponseEntity.ok(phoneVerificationService.sendPhoneOtp(request));
    }

    @PostMapping("/phone/verify")
    public ResponseEntity<CustomerResponse> verifyPhoneOtp(@Valid @RequestBody PhoneVerifyRequest request) {
        return ResponseEntity.ok(phoneVerificationService.verifyPhoneOtp(request));
    }

    @PutMapping("/information-personal")
    public ResponseEntity<UserResponse> updateInformationPersonal(
            @Valid @RequestBody UpdateInformationPersonal request) {
        return ResponseEntity.ok(customerService.updateInformationPersonal(request));
    }

    @PostMapping("/email/otp")
    public ResponseEntity<EmailOtpSendResponse> sendEmailOtp(@Valid @RequestBody EmailRegisterRequest request) {
        rateLimitService.checkOtpDestination("email", request.email());
        return ResponseEntity.ok(emailVerificationService.sendEmailUpdateOtp(request));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<UserResponse> verifyEmailOtp(@Valid @RequestBody EmailVerifyRequest request) {
        return ResponseEntity.ok(emailVerificationService.verifyEmailUpdateOtp(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CustomerResponse>> getAllCustomers(Pageable pageable) {
        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CustomerResponse>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.searchCustomers(name, phone, pageable));
    }

    @GetMapping("/level/{level}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CustomerResponse>> getByLevel(
            @PathVariable CustomerLevel level, Pageable pageable) {
        return ResponseEntity.ok(customerService.getCustomersByLevel(level, pageable));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> getByUserId(@PathVariable int customerId) {
        return ResponseEntity.ok(customerService.getCustomerByUserId(customerId));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable int customerId,
            @Valid @RequestBody AdminCustomerUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }
}
