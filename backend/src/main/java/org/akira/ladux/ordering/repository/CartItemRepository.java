package org.akira.ladux.ordering.repository;

import java.util.List;
import org.akira.ladux.ordering.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(Integer cartId);

    void deleteByProductVariantId(Integer variantId);
}

