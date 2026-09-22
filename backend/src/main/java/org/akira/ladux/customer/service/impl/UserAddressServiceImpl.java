package org.akira.ladux.customer.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.dto.request.UserAddressRequest;
import org.akira.ladux.customer.dto.response.UserAddressResponse;
import org.akira.ladux.customer.entity.UserAddress;
import org.akira.ladux.customer.mapper.UserAddressMapper;
import org.akira.ladux.customer.repository.UserAddressRepository;
import org.akira.ladux.customer.service.UserAddressService;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {
    private final UserAddressRepository repo;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userAddresses", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<UserAddressResponse> getAllUserAddresses(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable)
                .map(UserAddressMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userAddresses", key = "'user:' + #userId + ':id:' + #addressId")
    public UserAddressResponse getUserAddressById(int userId, int addressId) {
        UserAddress address = repo.findByIdWithUser(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user address voi id = " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Bạn không có quyền xem địa chỉ này!");
        }

        return UserAddressMapper.fromEntity(address);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userAddresses", key = "'admin:id:' + #addressId")
    public UserAddressResponse getUserAddressByIdForAdmin(int addressId) {
        return UserAddressMapper.fromEntity(repo.findByIdWithUser(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user address voi id = " + addressId)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userAddresses", key = "'user:' + #userId + ':list'")
    public List<UserAddressResponse> getUserAddressesByUserId(int userId) {
        return repo.findByUserId(userId).stream()
                .map(UserAddressMapper::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userAddresses", key = "'user:' + #userId + ':default'")
    public List<UserAddressResponse> getDefaultUserAddressesByUserId(int userId) {
        return repo.findByUserIdAndIsDefaultTrue(userId).stream()
                .map(UserAddressMapper::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "userAddresses", allEntries = true)
    public UserAddressResponse createUserAddress(int userId, UserAddressRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user voi id = " + userId));

        if (Boolean.TRUE.equals(request.isDefault())) {
            repo.findByUserIdForUpdate(userId);
            clearDefaultAddresses(userId);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .receiverName(request.receiverName())
                .phone(request.phone())
                .street(request.street())
                .ward(request.ward())
                .district(request.district())
                .city(request.city())
                .isDefault(request.isDefault() == null ? false : request.isDefault())
                .build();

        return UserAddressMapper.fromEntity(repo.save(address));
    }

    @Override
    @Transactional
    @CacheEvict(value = "userAddresses", allEntries = true)
    public UserAddressResponse updateUserAddress(int userId, int addressId, UserAddressRequest request) { // 💡 Nhận cả userId và addressId
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user voi id = " + userId));
        UserAddress address = repo.findByUserIdAndIdForUpdate(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user address voi id = " + addressId));

        if (request.receiverName() != null) address.setReceiverName(request.receiverName());
        if (request.phone() != null) address.setPhone(request.phone());
        if (request.street() != null) address.setStreet(request.street());
        if (request.ward() != null) address.setWard(request.ward());
        if (request.district() != null) address.setDistrict(request.district());
        if (request.city() != null) address.setCity(request.city());

        if (request.isDefault() != null) {
            if (Boolean.TRUE.equals(request.isDefault())) {
                repo.findByUserIdForUpdate(userId);
                clearDefaultAddresses(userId);
            }
            address.setDefault(request.isDefault());
        }

        return UserAddressMapper.fromEntity(address);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userAddresses", allEntries = true)
    public void deleteUserAddressById(int userId, int addressId) {
        UserAddress address = repo.findByUserIdAndIdForUpdate(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user address voi id = " + addressId));
        repo.delete(address);
    }
    private void clearDefaultAddresses(Integer userId) {
        repo.clearDefaultByUserId(userId);
    }
}
