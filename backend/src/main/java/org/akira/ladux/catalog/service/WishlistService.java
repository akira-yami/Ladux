package org.akira.ladux.catalog.service;

import java.util.List;
import org.akira.ladux.catalog.dto.response.WishlistResponse;

public interface WishlistService {
    void addItemToWishlist(int userId, int productId);

    List<WishlistResponse> getWishlistsByUserId(int userId);

    void removeItemFromWishlist(int usrerId, int productId);
}
