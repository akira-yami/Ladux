package org.akira.ladux.procurement.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.procurement.dto.request.ProductSupplierRequest;
import org.akira.ladux.procurement.dto.response.ProductSupplierResponse;
import org.akira.ladux.procurement.entity.ProductSupplier;
import org.akira.ladux.procurement.entity.Supplier;
import org.akira.ladux.procurement.mapper.ProductSupplierMapper;
import org.akira.ladux.procurement.repository.ProductSupplierRepository;
import org.akira.ladux.procurement.repository.SupplierRepository;
import org.akira.ladux.procurement.service.ProductSupplierService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductSupplierServiceImpl implements ProductSupplierService {

    private final ProductSupplierRepository repo;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public ProductSupplierResponse link(ProductSupplierRequest request) {
        if (repo.existsByProductIdAndSupplierId(request.productId(), request.supplierId())) {
            throw new BusinessRuleException("San pham nay da duoc lien ket voi nha cung cap nay");
        }
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham id = " + request.productId()));
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nha cung cap id = " + request.supplierId()));

        ProductSupplier ps = ProductSupplier.builder()
                .product(product)
                .supplier(supplier)
                .costPrice(request.costPrice())
                .leadTimeDays(request.leadTimeDays())
                .build();
        return ProductSupplierMapper.fromEntity(repo.save(ps));
    }

    @Override
    @Transactional
    public ProductSupplierResponse update(long id, ProductSupplierRequest request) {
        ProductSupplier ps = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay lien ket san pham-nha cung cap id = " + id));
        ps.setCostPrice(request.costPrice());
        ps.setLeadTimeDays(request.leadTimeDays());
        return ProductSupplierMapper.fromEntity(ps);
    }

    @Override
    @Transactional
    public void unlink(long id) {
        ProductSupplier ps = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay lien ket san pham-nha cung cap id = " + id));
        repo.delete(ps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplierResponse> getByProduct(int productId) {
        return repo.findByProductId(productId).stream().map(ProductSupplierMapper::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplierResponse> getBySupplier(int supplierId) {
        return repo.findBySupplierId(supplierId).stream().map(ProductSupplierMapper::fromEntity).toList();
    }
}
