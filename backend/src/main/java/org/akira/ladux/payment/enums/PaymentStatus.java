package org.akira.ladux.payment.enums;

// Trang thai mot lan thanh toan (payment attempt) gan voi don hang.
public enum PaymentStatus {
    PENDING,  // Cho khach thanh toan qua gateway hoac xac nhan COD
    SUCCESS,  // Thanh toan thanh cong -> confirmAfterSuccessfulPayment
    FAILED,  // Thanh toan that bai -> cancelOrder (hoan kho + coupon)
    REFUNDED   // Da hoan tien cho khach (qua refundPayment)
}
