package org.akira.ladux.catalog.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.PublicCatalogFilter;
import org.akira.ladux.catalog.dto.response.CatalogFacetsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PublicCatalogRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String PRICE = "COALESCE(v.discount_price, v.price)";
    private static final String FROM = """
            FROM products p JOIN brands b ON b.id = p.brand_id
            JOIN categories c ON c.id = p.category_id
            JOIN product_variants v ON v.product_id = p.id
            WHERE p.is_active AND c.is_active AND v.is_active
            """;

    public Page<Integer> search(PublicCatalogFilter filter, Pageable page) {
        var params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(FROM);
        addSearch(where, params, "search", filter.search());
        addSearch(where, params, "secondarySearch", filter.secondarySearch());
        if (filter.brandId() != null) { where.append(" AND p.brand_id = :brand"); params.addValue("brand", filter.brandId()); }
        if (filter.categoryId() != null) { where.append(" AND p.category_id = :category"); params.addValue("category", filter.categoryId()); }
        if (filter.ram() != null && !filter.ram().isBlank()) { where.append(" AND v.ram = :ram"); params.addValue("ram", filter.ram()); }
        if (filter.rom() != null && !filter.rom().isBlank()) { where.append(" AND v.rom = :rom"); params.addValue("rom", filter.rom()); }
        if (filter.maxPrice() != null) { where.append(" AND " + PRICE + " <= :maxPrice"); params.addValue("maxPrice", filter.maxPrice()); }
        // Only constant SQL fragments reach ORDER BY; all user input is bound.
        String sort = switch (filter.order() == null ? "newest" : filter.order()) {
            case "price-asc" -> "MIN(" + PRICE + ") ASC, p.id DESC";
            case "price-desc" -> "MIN(" + PRICE + ") DESC, p.id DESC";
            case "rating" -> "COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.product_id = p.id), 0) DESC, p.id DESC";
            default -> "p.id DESC";
        };
        Long total = jdbc.queryForObject("SELECT COUNT(DISTINCT p.id) " + where, params, Long.class);
        params.addValue("limit", page.getPageSize()).addValue("offset", page.getOffset());
        List<Integer> ids = jdbc.queryForList("SELECT p.id " + where + " GROUP BY p.id ORDER BY " + sort
                + " LIMIT :limit OFFSET :offset", params, Integer.class);
        return new PageImpl<>(ids, page, total == null ? 0 : total);
    }

    private void addSearch(StringBuilder where, MapSqlParameterSource params, String name, String value) {
        if (value == null || value.isBlank()) return;
        where.append(" AND STRPOS(LOWER(CONCAT_WS(' ', p.name, p.cpu, p.gpu, b.name, c.name, v.ram, v.rom)), LOWER(:")
                .append(name).append(")) > 0");
        params.addValue(name, value.trim());
    }

    public CatalogFacetsResponse facets() {
        var params = new MapSqlParameterSource();
        return new CatalogFacetsResponse(
                jdbc.queryForList("SELECT DISTINCT v.ram " + FROM + " AND v.ram IS NOT NULL AND v.ram <> '' ORDER BY v.ram LIMIT 100", params, String.class),
                jdbc.queryForList("SELECT DISTINCT v.rom " + FROM + " AND v.rom IS NOT NULL AND v.rom <> '' ORDER BY v.rom LIMIT 100", params, String.class),
                jdbc.queryForObject("SELECT COALESCE(MAX(" + PRICE + "), 0) " + FROM, params, java.math.BigDecimal.class));
    }

    public java.util.Map<Integer, double[]> ratings(List<Integer> ids) {
        if (ids.isEmpty()) return java.util.Map.of();
        var result = new java.util.HashMap<Integer, double[]>();
        jdbc.query("SELECT product_id, AVG(rating), COUNT(*) FROM reviews WHERE product_id IN (:ids) GROUP BY product_id",
                new MapSqlParameterSource("ids", ids), row -> {
                    result.put(row.getInt(1), new double[]{row.getDouble(2), row.getLong(3)});
                });
        return result;
    }
}
