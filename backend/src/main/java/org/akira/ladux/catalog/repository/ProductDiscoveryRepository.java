package org.akira.ladux.catalog.repository;

import java.util.List;

import org.akira.ladux.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Truy van phuc vu Smart Search (Intelligent Discovery).
 *
 * <p>Nguon su that: catalog + doanh so tu don hang. Tat ca truy van chi tra ve ID
 * san pham de giu nguyen pattern two-phase ID pagination; chi tiet (anh, bien the,
 * gia) duoc fetch rieng qua {@link ProductRepository#findSummariesByIdIn}.
 *
 * <p>Dieu kien "co the ban" dung chung: san pham active, co it nhat mot bien the
 * active con hang (stock_quantity > 0). Dung native SQL vi can pg_trgm
 * (word_similarity) va tong hop doanh so hieu qua.
 */
@Repository
public interface ProductDiscoveryRepository extends JpaRepository<Product, Integer> {

    /**
     * Top san pham ban chay trong N ngay gan nhat.
     *
     * <p>Chi tinh don da DELIVERED (loai tru huy/tra/hoan) de con so "ban chay" la
     * su that. Moc thoi gian dua tren order_histories: thoi diem don chuyen sang
     * DELIVERED. Chi lay san pham con co the ban (active + con hang) de khong goi y
     * mon da tat/het.
     */
    @Query(value = """
            SELECT oi.product_id
            FROM order_histories oh
            JOIN orders o ON o.id = oh.order_id AND o.status = 'DELIVERED'
            JOIN order_items oi ON oi.order_id = oh.order_id
            JOIN products p ON p.id = oi.product_id AND p.is_active = true
            WHERE oh.status = 'DELIVERED'
              -- Loc theo khung thoi gian TRUOC khi gom nhom (dung idx_order_histories_delivered_at),
              -- thay vi tinh MIN(created_at) tren toan bo lich su DELIVERED tu truoc den nay.
              AND oh.created_at >= NOW() - make_interval(days => :days)
              AND oh.created_at <= NOW()
              -- Chi tinh moc DELIVERED dau tien cua moi don (don bi tu choi tra hang se co them dong DELIVERED).
              AND NOT EXISTS (
                    SELECT 1 FROM order_histories earlier
                    WHERE earlier.order_id = oh.order_id AND earlier.status = 'DELIVERED'
                      AND (earlier.created_at < oh.created_at
                           OR (earlier.created_at = oh.created_at AND earlier.id < oh.id))
              )
              AND oi.quantity > 0
              AND oi.product_id IS NOT NULL
              AND EXISTS (
                    SELECT 1 FROM product_variants pv
                    WHERE pv.product_id = p.id AND pv.is_active = true AND pv.stock_quantity > 0
                      AND COALESCE(pv.discount_price, pv.price) > 0
              )
            GROUP BY oi.product_id
            ORDER BY SUM(oi.quantity) DESC, oi.product_id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Integer> findBestSellerProductIds(@Param("days") int days, @Param("limit") int limit);

    /**
     * San pham noi bat lam fallback khi chua du doanh so hop le: san pham co the ban,
     * uu tien hang moi (created_at desc) va co nhieu bien the con hang.
     */
    @Query(value = """
            SELECT p.id
            FROM products p
            WHERE p.is_active = true
              AND EXISTS (
                    SELECT 1 FROM product_variants pv
                    WHERE pv.product_id = p.id AND pv.is_active = true AND pv.stock_quantity > 0
                      AND COALESCE(pv.discount_price, pv.price) > 0
              )
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Integer> findFeaturedProductIds(@Param("limit") int limit);

    /**
     * Goi y theo tu khoa: uu tien khop chinh xac/tien to, sau do gan dung bang pg_trgm
     * (word_similarity bat loi go nhu "thikpad" -> "ThinkPad"). Chi tra san pham co
     * the ban. Nguong 0.4 giu "thikpad" -> "ThinkPad" va loai nhieu nhu "Thin".
     */
    @Query(value = """
            SELECT p.id
            FROM products p
            WHERE p.is_active = true
              AND EXISTS (
                    SELECT 1 FROM product_variants pv
                    WHERE pv.product_id = p.id AND pv.is_active = true AND pv.stock_quantity > 0
                      AND COALESCE(pv.discount_price, pv.price) > 0
              )
              AND (
                    lower(p.name) LIKE lower(:prefix)
                    OR lower(p.name) LIKE lower(:contains)
                    OR (length(:q) >= 3 AND word_similarity(lower(:q), lower(p.name)) >= 0.4)
              )
            ORDER BY
                (lower(p.name) = lower(:q)) DESC,
                (lower(p.name) LIKE lower(:prefix)) DESC,
                (lower(p.name) LIKE lower(:contains)) DESC,
                word_similarity(lower(:q), lower(p.name)) DESC,
                p.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Integer> findSuggestionProductIds(
            @Param("q") String q,
            @Param("prefix") String prefix,
            @Param("contains") String contains,
            @Param("limit") int limit);
}
