package org.akira.ladux.customer.service;

import java.util.List;
import org.akira.ladux.customer.dto.request.UserAddressRequest;
import org.akira.ladux.customer.dto.response.UserAddressResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UserAddressService {
    PageResponse<UserAddressResponse> getAllUserAddresses(Pageable pageable);

    UserAddressResponse getUserAddressById(int userId, int addressId);

    /** For admin use: fetch any address by ID without ownership restriction */
    UserAddressResponse getUserAddressByIdForAdmin(int addressId);

    List<UserAddressResponse> getUserAddressesByUserId(int userId);

    List<UserAddressResponse> getDefaultUserAddressesByUserId(int userId);

    UserAddressResponse createUserAddress(int userId, UserAddressRequest request);

    UserAddressResponse updateUserAddress(int userId, int addressId, UserAddressRequest request);

    void deleteUserAddressById(int userId, int addressId);
}
