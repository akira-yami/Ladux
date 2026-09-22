package org.akira.ladux.procurement.service;

import java.util.List;
import org.akira.ladux.procurement.dto.request.ProductSupplierRequest;
import org.akira.ladux.procurement.dto.response.ProductSupplierResponse;

public interface ProductSupplierService {

    ProductSupplierResponse link(ProductSupplierRequest request);

    ProductSupplierResponse update(long id, ProductSupplierRequest request);

    void unlink(long id);

    List<ProductSupplierResponse> getByProduct(int productId);

    List<ProductSupplierResponse> getBySupplier(int supplierId);
}
