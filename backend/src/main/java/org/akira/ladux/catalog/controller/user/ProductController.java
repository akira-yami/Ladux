package org.akira.ladux.catalog.controller.user;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.ProductPage;
import org.akira.ladux.catalog.dto.ProductSearch;
import org.akira.ladux.catalog.dto.ProductVariantView;
import org.akira.ladux.catalog.dto.ProductView;
import org.akira.ladux.catalog.service.CatalogQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final CatalogQueryService catalogQueryService;

    @GetMapping
    public ResponseEntity<ProductPage> getAllProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return ResponseEntity.ok(catalogQueryService.searchProducts(toSearch(search, null, null, page, size, sort, direction)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductView> getProductById(@PathVariable int id) {
        return ResponseEntity.ok(catalogQueryService.getProduct(id));
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ProductPage> getProductsByBrandId(
            @PathVariable int brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(catalogQueryService.searchProducts(toSearch(null, brandId, null, page, size, sort, direction)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ProductPage> getProductsByCategoryId(
            @PathVariable int categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(catalogQueryService.searchProducts(toSearch(null, null, categoryId, page, size, sort, direction)));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<ProductVariantView> getProductVariantById(
            @PathVariable Integer variantId) {
        return ResponseEntity.ok(catalogQueryService.getVariant(variantId));
    }

    private ProductSearch toSearch(String text, Integer brandId, Integer categoryId,
                                   int page, int size, String sort, String direction) {
        String[] sortParts = sort.split(",", 2);
        String property = sortParts[0].isBlank() ? "id" : sortParts[0];
        String sortDirection = sortParts.length == 2 ? sortParts[1] : direction;
        return new ProductSearch(text, brandId, categoryId, page, size, property,
                !"desc".equalsIgnoreCase(sortDirection));
    }
}
