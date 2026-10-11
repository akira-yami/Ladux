package org.akira.ladux.identity.service.impl;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.enums.CustomerLevel;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.identity.dto.request.RegisterRequest;
import org.akira.ladux.identity.dto.request.UserAdminUpdateRequest;
import org.akira.ladux.identity.dto.request.UserUpdatePassword;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.mapper.UserMapper;
import org.akira.ladux.identity.repository.RoleRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.PasswordVerificationService;
import org.akira.ladux.identity.service.RefreshTokenService;
import org.akira.ladux.identity.service.UserService;
import org.akira.ladux.ordering.entity.Cart;
import org.akira.ladux.ordering.repository.CartRepository;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.akira.ladux.shared.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repo;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final RefreshTokenService refreshTokenService;
    private final PasswordVerificationService passwordVerificationService;
    private final FileStorageService fileStorage;

    @Value("${app.upload.avatar-dir:avatar}")
    private String avatarUploadDir;

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse savedUser(RegisterRequest request) {
        String username = request.username().trim();

        if (repo.existsByUsername(username)) {
            throw new BusinessRuleException("Username nay da ton tai. Hay dung username khac.");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER);
        if (customerRole == null) {
            throw new ResourceNotFoundException("Khong tim thay role CUSTOMER");
        }
        User user = User.builder()
                .username(username)
                .password(encoder.encode(request.password()))
                .isActive(true)
                .roles(Set.of(customerRole))
                .build();
        // Ho so khach hang (shared PK voi User) — luu ten/sdt.
        Customer customer = Customer.builder()
                .user(user)
                .fullName(request.fullName().trim())
                .level(CustomerLevel.BROWSER)
                .loyaltyPoints(0L)
                .totalSpent(BigDecimal.ZERO)
                .build();
        user.setCustomer(customer);
        customerRepository.save(customer);
        User saved = repo.save(user);
        cartRepository.save(Cart.builder().user(saved).build());
        return UserMapper.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable)
                .map(UserMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'id:' + #id")
    public UserResponse getUserById(int id) {
        return UserMapper.fromEntity(repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user voi id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'email:' + #email")
    public UserResponse getUserByEmail(String email) {
        return UserMapper.fromEntity(
                repo.findByCustomerEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Khong tim thay user voi email = " + email
                        ))
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'active:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<UserResponse> getActiveUsers(Pageable pageable) {
        return PageResponse.from(repo.findByIsActiveTrue(pageable)
                .map(UserMapper::fromEntity));
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateUser(int id, UserAdminUpdateRequest request) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user voi id = " + id));
        if (request.fullName() != null) {
            getOrCreateCustomer(user).setFullName(request.fullName());
        }
        if (request.avatar() != null) {
            getOrCreateCustomer(user).setAvatarUrl(request.avatar());
        }
        if (request.isActive() != null) {
            user.setActive(request.isActive());
            if (!request.isActive()) {
                user.setTokenVersion(user.getTokenVersion() + 1);
                refreshTokenService.revokeAllRefreshTokens(id);
            }
        }
        if (request.roleIds() != null) {
            if (request.roleIds().isEmpty()) {
                throw new BusinessRuleException("User phai co it nhat 1 role");
            }
            Set<Role> roles = resolveRoles(request.roleIds());
            user.setRoles(roles);
        }
        return UserMapper.fromEntity(user);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    @Override
    public void changePassword(
            Integer id,
            UserUpdatePassword request
    ) {
        User user = repo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy người dùng"
                        )
                );

        if (!request.newPassword().equals(
                request.confirmPassword()
        )) {
            throw new BusinessRuleException(
                    "Mật khẩu xác nhận không khớp"
            );
        }

        if (!encoder.matches(
                request.currentPassword(),
                user.getPassword()
        )) {
            throw new BusinessRuleException(
                    "Mật khẩu hiện tại không chính xác"
            );
        }

        if (encoder.matches(
                request.newPassword(),
                user.getPassword()
        )) {
            throw new BusinessRuleException(
                    "Mật khẩu mới không được trùng mật khẩu hiện tại"
            );
        }

        /*
         * Proof xác thực email.
         */
        passwordVerificationService.consume(
                id,
                request.verificationId()
        );

        /*
         * Chỉ đổi password sau khi proof hợp lệ.
         */
        user.setPassword(
                encoder.encode(
                        request.newPassword()
                )
        );

        user.setTokenVersion(
                user.getTokenVersion() + 1
        );

        repo.save(user);

        refreshTokenService
                .revokeAllRefreshTokens(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateAvatar(int id, MultipartFile file) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user voi id = " + id));
        Customer customer = getOrCreateCustomer(user);
        fileStorage.deleteIfLocal(customer.getAvatarUrl());
        customer.setAvatarUrl(fileStorage.store(avatarUploadDir, file));
        return UserMapper.fromEntity(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse uploadAvatar(Integer id, MultipartFile file) {
        if (id == null) {
            throw new BusinessRuleException("User id khong duoc de trong");
        }
        return updateAvatar(id, file);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUserById(int id) {
        repo.deleteById(id);
    }

    /** Lay ho so Customer cua user, tao moi (gan vao user, cascade) neu chua co. */
    private Customer getOrCreateCustomer(User user) {
        Customer customer = user.getCustomer();
        if (customer == null) {
            customer = Customer.builder()
                    .user(user)
                    .level(CustomerLevel.BROWSER)
                    .loyaltyPoints(0L)
                    .totalSpent(BigDecimal.ZERO)
                    .build();
            user.setCustomer(customer);
        }
        return customer;
    }

    private Set<Role> resolveRoles(List<Integer> roleIds) {
        if (roleIds == null) {
            return null;
        }
        Set<Role> roles = new LinkedHashSet<>();
        for (Integer roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay role voi id = " + roleId));
            roles.add(role);
        }
        return roles;
    }
    public PageResponse<UserResponse> searchUserByNameOrPhone(String name, String phone, Pageable pageable) {
        String searchName = cleanSearch(name);
        String searchPhone = cleanSearch(phone);
        if (searchPhone != null) {
            try {
                searchPhone = normalizePhoneForSearch(searchPhone);
            } catch (IllegalArgumentException ignored) {
                // Keep raw input for partial/non-Vietnamese phone search.
            }
        }
        return PageResponse.from(repo.findByNameOrPhone(searchName, searchPhone, pageable)
                .map(UserMapper::fromEntity));
    }

    private String cleanSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizePhoneForSearch(String value) {
        String compact = value.trim().replaceAll("[\\s.()\\-]", "");
        if (compact.startsWith("+84") || compact.startsWith("84") || compact.startsWith("0")) {
            if (compact.startsWith("+84")) return compact;
            if (compact.startsWith("84")) return "+" + compact;
            return "+84" + compact.substring(1);
        }
        throw new IllegalArgumentException("Not a phone number");
    }
}
