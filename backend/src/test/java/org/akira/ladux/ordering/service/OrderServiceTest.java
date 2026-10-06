package org.akira.ladux.ordering.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.BrandRepository;
import org.akira.ladux.catalog.repository.CategoryRepository;
import org.akira.ladux.catalog.repository.ColorRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.ordering.dto.request.OrderRequest;
import org.akira.ladux.ordering.dto.request.OrderStatusUpdateRequest;
import org.akira.ladux.ordering.dto.request.ShippingAddressRequest;
import org.akira.ladux.ordering.entity.Cart;
import org.akira.ladux.ordering.entity.CartItem;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.CartRepository;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.promotion.repository.CouponRepository;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test cho luong nghiep vu trong tam (luong tien): checkout / coupon / huy don.
 *
 * <p>@Transactional o cap class -> moi test ROLLBACK sau khi chay, du lieu seed khong bi thay doi
 * (cac test doc lap voi nhau). Vi cau lenh tru kho la JPQL bulk-update (bo qua persistence context),
 * ta goi {@code em.flush(); em.clear();} truoc khi doc lai de tranh doc phai entity cache cu (stale).
 */
@Transactional
class OrderServiceTest extends AbstractIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired CartRepository cartRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CouponRepository couponRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired BrandRepository brandRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ColorRepository colorRepository;
    @Autowired ProductVariantRepository productVariantRepository;

    @PersistenceContext
    EntityManager em;

    private static final int USER_ID = 2; // user seed: quang_huy

    private ProductVariant newVariant(int stock, String price) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Product product = productRepository.save(Product.builder()
                .brand(brandRepository.findAll().get(0))
                .category(categoryRepository.findAll().get(0))
                .name("IT Product " + unique)
                .slug("it-product-" + unique)
                .isActive(true)
                .build());
        return productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .color(colorRepository.findAll().get(0))
                .sku("IT-" + unique)
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .isActive(true)
                .build());
    }

    private void setCartSingleItem(int userId, ProductVariant productVariant, int qty) {
        Cart cart = cartRepository.findByUserIdForUpdate(userId).orElseThrow();
        cart.getItems().clear();
        cart.getItems().add(CartItem.builder().cart(cart).productVariant(productVariant).quantity(qty).build());
        cartRepository.save(cart);
    }

    private Order newestOrderOf(int userId) {
        Integer id = orderRepository
                .findIdsByUserId(userId, PageRequest.of(0, 1, Sort.by("id").descending()))
                .getContent()
                .get(0);
        return orderRepository.findById(id).orElseThrow();
    }

    private int stockOf(int productVariantId) {
        return productVariantRepository.findById(productVariantId).orElseThrow().getStockQuantity();
    }

    private OrderRequest orderRequest(String couponCode, PaymentProvider paymentProvider) {
        return new OrderRequest(couponCode, paymentProvider,
                new ShippingAddressRequest("Test User", "0900000000", "123 Test Street", "Ward 1", "District 1", "HCM"));
    }

    @Test
    void checkout_deductsStock_clearsCart_andCreatesPendingOrder() {
        ProductVariant productVariant = newVariant(10, "500.00");
        setCartSingleItem(USER_ID, productVariant, 3);

        orderService.createOrder(USER_ID, orderRequest(null, PaymentProvider.COD));

        em.flush();
        em.clear();

        // 1. Ton kho giam dung 3.
        assertEquals(7, stockOf(productVariant.getId()), "Checkout phai tru kho theo so luong mua");
        // 2. Gio hang duoc don sach sau khi dat hang.
        assertTrue(cartRepository.findByUserId(USER_ID).getItems().isEmpty(), "Gio hang phai rong sau checkout");
        // 3. Don moi o trang thai PENDING, subTotal = 3 * 500.
        Order order = newestOrderOf(USER_ID);
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(0, new BigDecimal("1500.00").compareTo(order.getSubTotal()));
    }

    @Test
    void checkout_withValidCoupon_appliesDiscount_andIncrementsUsedCount() {
        ProductVariant productVariant = newVariant(10, "1000.00");
        setCartSingleItem(USER_ID, productVariant, 1);
        int usedBefore = couponRepository.findByCode("GIAM10").getUsedCount();

        orderService.createOrder(USER_ID, orderRequest("GIAM10", PaymentProvider.COD));

        em.flush();
        em.clear();

        Order order = newestOrderOf(USER_ID);
        // GIAM10 = giam 10% -> 100 tien giam; final = 900 + phi giao hang cau hinh.
        assertEquals(0, new BigDecimal("100.00").compareTo(order.getDiscountAmount()));
        assertEquals(0, new BigDecimal("900.00").add(order.getShippingFee()).compareTo(order.getFinalAmount()));
        // Dung coupon -> usedCount tang 1.
        assertEquals(usedBefore + 1, couponRepository.findByCode("GIAM10").getUsedCount());
    }

    @Test
    void cancelOrder_restocksProduct_andRollsBackCouponUsage() {
        ProductVariant productVariant = newVariant(10, "1000.00");
        setCartSingleItem(USER_ID, productVariant, 4);
        int usedBefore = couponRepository.findByCode("GIAM10").getUsedCount();

        orderService.createOrder(USER_ID, orderRequest("GIAM10", PaymentProvider.COD));

        em.flush();
        em.clear();

        Order order = newestOrderOf(USER_ID);
        assertEquals(6, stockOf(productVariant.getId()), "Sau checkout: 10 - 4 = 6");
        assertEquals(usedBefore + 1, couponRepository.findByCode("GIAM10").getUsedCount());

        orderService.updateOrderStatus(order.getId(),
                new OrderStatusUpdateRequest(OrderStatus.CANCELLED, null));

        em.flush();
        em.clear();

        assertEquals(10, stockOf(productVariant.getId()), "Huy don -> hoan kho ve 10");
        assertEquals(usedBefore, couponRepository.findByCode("GIAM10").getUsedCount(), "Huy don -> hoan luot coupon");
        assertEquals(OrderStatus.CANCELLED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelOrder_requiresOwner_andDoesNotChangeAnotherUsersStock() {
        ProductVariant variant = newVariant(7, "1200.00");
        setCartSingleItem(USER_ID, variant, 2);
        orderService.createOrder(USER_ID, orderRequest(null, PaymentProvider.COD));
        em.flush();
        em.clear();

        int orderId = newestOrderOf(USER_ID).getId();
        assertEquals(5, stockOf(variant.getId()));
        assertThrows(ResourceNotFoundException.class, () -> orderService.cancelOrder(3, orderId));
        em.flush();
        em.clear();
        assertEquals(OrderStatus.PENDING, orderRepository.findById(orderId).orElseThrow().getStatus());
        assertEquals(5, stockOf(variant.getId()));

        orderService.cancelOrder(USER_ID, orderId);
        em.flush();
        em.clear();
        assertEquals(OrderStatus.CANCELLED, orderRepository.findById(orderId).orElseThrow().getStatus());
        assertEquals(7, stockOf(variant.getId()));
    }

    @Test
    void checkout_withEmptyCart_throwsBusinessRuleException() {
        Cart cart = cartRepository.findByUserIdForUpdate(USER_ID).orElseThrow();
        cart.getItems().clear();
        cartRepository.save(cart);
        em.flush();

        assertThrows(BusinessRuleException.class, () ->
                orderService.createOrder(USER_ID, orderRequest(null, PaymentProvider.COD)));
    }

    @Test
    void checkout_setsPaymentExpiry_forOnlineProvider_butNotForCod() {
        ProductVariant productVariant = newVariant(10, "500.00");

        // COD: khong co han thanh toan.
        setCartSingleItem(USER_ID, productVariant, 1);
        orderService.createOrder(USER_ID, orderRequest(null, PaymentProvider.COD));
        em.flush();
        em.clear();
        assertNotNull(newestOrderOf(USER_ID));
        assertTrue(newestOrderOf(USER_ID).getPaymentExpiresAt() == null, "COD khong dat han thanh toan");

        // VNPAY: co han thanh toan.
        ProductVariant productVariant2 = newVariant(10, "500.00");
        setCartSingleItem(USER_ID, productVariant2, 1);
        orderService.createOrder(USER_ID, orderRequest(null, PaymentProvider.VNPAY));
        em.flush();
        em.clear();
        assertNotNull(newestOrderOf(USER_ID).getPaymentExpiresAt(), "VNPAY phai dat han thanh toan");
    }
}
