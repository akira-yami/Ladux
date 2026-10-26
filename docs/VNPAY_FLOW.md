# VNPay — vòng đời payment và đối soát

Ladux tách **tạo payment**, **xác minh kết quả** và **hoàn tiền** thành các bước có ranh giới transaction riêng. Merchant/URL nằm trong môi trường backend; xem [cấu hình](VNPAY_SETUP.md) và [phạm vi đã kiểm chứng](PROJECT_STATUS.md).

## 1. Checkout và tạo URL

Checkout tạo order, snapshot item, payment PENDING và dọn giỏ trong cùng transaction. Sau đó client yêu cầu URL payment của chính đơn đó.

`POST /api/v1/payments`:

- Kiểm tra chủ sở hữu, provider, trạng thái và thời hạn đơn.
- Nếu attempt PENDING đã có URL, trả lại đúng URL/ref/ngày khởi tạo; không kéo dài thời gian giữ hàng.
- Sau attempt FAILED mới cho tạo attempt mới trong giới hạn hợp lệ. Không tùy ý đổi provider của một attempt đang chờ.
- Ký canonical query bằng HMAC-SHA512; dùng tổng VND nguyên × 100 và hạn không vượt hạn đơn.

Frontend commit trang Đơn hàng vào browser history trước khi rời sang VNPay. Hành động Back vì vậy không phục hồi checkout của giỏ đã bị tiêu thụ.

## 2. Return, IPN và QueryDR

```mermaid
sequenceDiagram
    participant U as Browser
    participant L as Ladux API
    participant V as VNPay
    participant D as PostgreSQL
    U->>L: Tạo/lấy payment URL
    L-->>U: URL của attempt hợp lệ
    U->>V: Mở trang thanh toán
    V-->>U: Redirect về Return URL
    U->>L: Verify Return + đọc payment của chính chủ
    L-->>U: Trạng thái đã lưu; Return không settlement
    V->>L: IPN có chữ ký
    L->>D: Khóa order rồi payment, kiểm tra và cập nhật
    D-->>L: Commit
    L-->>V: ACK
    opt Mất IPN / còn PENDING
        L->>V: QueryDR từ Return hoặc worker
        V-->>L: Phản hồi có chữ ký
        L->>D: Settlement sau xác minh
    end
```

Thứ tự IPN và Return trong thực tế có thể đảo hoặc trùng lặp; sơ đồ không yêu cầu IPN phải đến sau Return.

| Nguồn | Kiểm tra | Có thể settlement? |
| --- | --- | --- |
| Return URL | Chữ ký, merchant, amount, ref và quyền sở hữu | **Không** — chỉ xác minh dữ liệu hiển thị/đọc trạng thái |
| IPN | Chữ ký, merchant, amount, ref, response/status, transaction number | Có, sau kiểm tra và commit |
| QueryDR | Chữ ký response, merchant/ref/amount, loại giao dịch và status | Có, qua cùng settlement service |

IPN/query dùng cùng thứ tự khóa: **order trước, payment sau**. Kiểm số tiền trước khi xử lý trạng thái. Kết quả thành công cần cả response code và transaction status là `00`, cùng mã giao dịch hợp lệ. Mã giao dịch `0` của một lần hủy không được lưu vào cột unique như một giao dịch đã thu tiền.

### IPN acknowledgement

| RspCode | Ý nghĩa trong Ladux |
| --- | --- |
| `00` | Đã xử lý/ghi nhận kết quả |
| `02` | Giao dịch đã xử lý trước đó |
| `01` | Không tìm thấy giao dịch/đơn tương ứng |
| `04` | Số tiền không khớp |
| `97` | Chữ ký/merchant không hợp lệ |
| `99` | Chưa thể xử lý; cần retry |

ACK settlement chỉ được trả sau khi transaction commit. Lỗi DB/unique không được biến thành một lần xác nhận thành công giả.

## 3. Recovery khi trình duyệt đã đóng

Worker đối soát tìm payment VNPay PENDING có URL, tạo trong 24 giờ gần nhất và đã chờ ít nhất một phút. Mỗi lượt tối đa 5 giao dịch, không query lại một attempt trong 5 phút; thời điểm query được lưu tại DB. ShedLock hạn chế worker trùng giữa các instance.

Worker dùng scheduler riêng với job hết hạn đơn. Gateway chậm không giữ cùng scheduler của việc hoàn kho. Query HTTP chạy ngoài transaction khóa order/payment; timeout hoặc response chưa xác minh không tự kết luận chưa thu tiền.

Giao dịch cũ hơn phạm vi worker cần đối soát chủ động/thủ công. Chưa có cam kết worker tự giải quyết mọi trường hợp mất thông báo của gateway.

## 4. Tiền đến muộn hoặc nhận tiền nhiều lần

- Payment SUCCESS/REFUNDED đã xử lý được nhận diện để callback lặp không chạy lại nghiệp vụ.
- Có tiền sau khi order đã hủy: lưu khoản tiền và cờ review, giữ trạng thái hủy và tồn kho đã hoàn.
- Có payment khác đã thu tiền: giữ bằng chứng từng attempt và đưa vào đối soát.
- Những trạng thái nghi ngờ/đảo giao dịch/chưa rõ không được tự coi là FAILED để mời khách thanh toán thêm.

Màn kết quả hiển thị trạng thái backend; `vnp_ResponseCode=00` trên thanh địa chỉ không phải nguồn xác nhận. Hết thời gian polling cũng không có nghĩa là giao dịch thất bại.

## 5. Hoàn tiền

Refund chỉ hỗ trợ toàn bộ payment. Attempt được lưu trước HTTP; kết quả đã xác minh mới cập nhật payment/order trong transaction riêng. Guard theo payment cho phép xử lý khoản thừa của cùng một order.

`PROCESSING` hoặc `REVIEW_REQUIRED` cần đối chiếu portal/sao kê. Nếu hoàn qua kênh ngoài, người vận hành chỉ xác nhận sau khi đã có bằng chứng chuyển tiền và mã chứng từ. Không tự retry refund khi chưa biết request trước đã được thực hiện hay chưa.

Hoàn khoản trả trùng giữ nguyên đơn giao hàng hợp lệ. Hoàn tất tiền của đơn trả/hủy mới đóng vòng đời phù hợp. Xem [engineering notes](ENGINEERING.md#3-hoàn-tiền-và-trạng-thái-chưa-rõ).

## 6. Code và tests để đọc cùng nhau

| Phần | Code | Test |
| --- | --- | --- |
| Tạo/resume URL | [PaymentServiceImpl](../backend/src/main/java/org/akira/ladux/payment/service/impl/PaymentServiceImpl.java) | [VNPayFlowIntegrationTest](../backend/src/test/java/org/akira/ladux/payment/controller/VNPayFlowIntegrationTest.java) |
| Xác minh/cập nhật | [VNPaySettlementService](../backend/src/main/java/org/akira/ladux/payment/service/impl/VNPaySettlementService.java) | Cùng integration suite, gồm callback đồng thời/lặp |
| Query recovery | [VNPayQueryService](../backend/src/main/java/org/akira/ladux/payment/service/impl/VNPayQueryService.java), [worker](../backend/src/main/java/org/akira/ladux/payment/service/impl/VNPayReconciliationJob.java) | [VNPayGatewayClientTest](../backend/src/test/java/org/akira/ladux/payment/service/impl/VNPayGatewayClientTest.java) |
| Refund | [RefundWorkflowService](../backend/src/main/java/org/akira/ladux/payment/service/RefundWorkflowService.java) | [RefundWorkflowServiceTest](../backend/src/test/java/org/akira/ladux/payment/service/RefundWorkflowServiceTest.java) |
| UI Return/redirect | [paymentFlow](../frontend/src/utils/paymentFlow.ts), [navigation](../frontend/src/utils/vnpayNavigation.ts) | [paymentFlow tests](../frontend/src/utils/paymentFlow.test.ts), [navigation tests](../frontend/src/utils/vnpayNavigation.test.ts) |

Migration V57 giữ ngày gốc gửi gateway, ngày thu tiền, thời điểm query và cờ review. V58 chuyển guard refund sang từng payment. Không sửa lịch sử payment bằng SQL để ép khớp một lần kiểm thử.

Các suite xác minh logic với fixture/mock và DB riêng. Điều kiện merchant, callback public và giao dịch ngân hàng thật vẫn cần kiểm chứng theo [runbook](PRODUCTION_RUNBOOK.md).
