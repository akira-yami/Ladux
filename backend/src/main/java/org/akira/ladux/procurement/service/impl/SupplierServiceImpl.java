package org.akira.ladux.procurement.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.util.PhoneNumberUtils;
import org.akira.ladux.procurement.dto.request.SupplierRequest;
import org.akira.ladux.procurement.dto.response.SupplierResponse;
import org.akira.ladux.procurement.entity.Supplier;
import org.akira.ladux.procurement.mapper.SupplierMapper;
import org.akira.ladux.procurement.repository.SupplierRepository;
import org.akira.ladux.procurement.service.SupplierService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository repo;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getAllSuppliers(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable).map(SupplierMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getActiveSuppliers(Pageable pageable) {
        return PageResponse.from(repo.findByIsActiveTrue(pageable).map(SupplierMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(int id) {
        return SupplierMapper.fromEntity(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> searchSuppliers(String name, String phone, Pageable pageable) {
        String searchName = (name != null && !name.isBlank()) ? name.trim() : null;
        String searchPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;
        return PageResponse.from(repo.searchByNameOrPhone(searchName, searchPhone, pageable));
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được để trống");
        }
        if (request.phone() == null || request.phone().isBlank()) {
            throw new IllegalArgumentException("Số điện thoại nhà cung cấp không được để trống");
        }
        String phoneNormalized = request.phone().trim();
        phoneNormalized = PhoneNumberUtils.normalize(phoneNormalized);
        Supplier supplier = Supplier.builder()
                .name(request.name().trim())
                .address(request.address())
                .phone(phoneNormalized)
                .email(request.email())
                .isActive(request.isActive() == null || request.isActive())
                .build();
        return SupplierMapper.fromEntity(repo.save(supplier));
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(int id, SupplierRequest request) {
        Supplier supplier = findOrThrow(id);
        supplier.setName(request.name().trim());
        supplier.setAddress(request.address());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        if (request.isActive() != null) {
            supplier.setActive(request.isActive());
        }
        return SupplierMapper.fromEntity(supplier);
    }

    @Override
    @Transactional
    public void deleteSupplierById(int id) {
        repo.delete(findOrThrow(id));
    }

    private Supplier findOrThrow(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nha cung cap voi id = " + id));
    }
}
