package org.akira.ladux.catalog.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.response.WishlistResponse;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.Wishlist;
import org.akira.ladux.catalog.mapper.WishlistMapper;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.WishlistRepository;
import org.akira.ladux.catalog.service.WishlistService;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {
    private final WishlistRepository repo;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", allEntries = true)
    public void addItemToWishlist(int userId, int productId) {
        // 1. Check xem User và Product có tồn tại không
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User khong ton tai"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product khong ton tai"));

        // 2. CHỐT CHẶN: Check xem User này đã thả tim sản phẩm này chưa
        boolean alreadyExists = repo.existsByUserIdAndProductId(userId, productId);
        if (alreadyExists) {
            throw new BusinessRuleException("San pham nay da nam trong danh sach yeu thich");
        }

        // 3. Nếu chưa có thì mới tạo mới
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        repo.save(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "'user:' + #userId")
    public List<WishlistResponse> getWishlistsByUserId(int userId) {
        return repo.findByUserId(userId).stream()
                .map(WishlistMapper::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", allEntries = true)
    public void removeItemFromWishlist(int userId, int productId) {
        Wishlist wishlist = repo.findByUserIdAndProductId(userId, productId);
        if (wishlist == null) {
            throw new ResourceNotFoundException("Wishlist khong ton tai");
        }
        repo.delete(wishlist);
    }
}
