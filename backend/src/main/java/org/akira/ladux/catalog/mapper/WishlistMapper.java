package org.akira.ladux.catalog.mapper;

import org.akira.ladux.catalog.dto.response.WishlistResponse;
import org.akira.ladux.catalog.entity.Wishlist;

public final class WishlistMapper {
    private WishlistMapper() {
    }

    public static WishlistResponse fromEntity(Wishlist wishlist) {
        if (wishlist == null) {
            return null;
        }
        return new WishlistResponse(
                wishlist.getId(),
                wishlist.getProduct() == null ? null : ProductMapper.fromEntity(wishlist.getProduct())
        );
    }
}
