# VNPay — luồng thanh toán local và triển khai

Đối chiếu ngày 21/10/2026 với tài liệu chính thức:

- [PAY 2.1.0, URL thanh toán, IPN và Return](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html)
- [API querydr và refund](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html)
- [Bảng mã lỗi](https://sandbox.vnpayment.vn/apis/docs/bang-ma-loi/)
- [Thẻ Sandbox công khai](https://sandbox.vnpayment.vn/apis/docs/gioi-thieu/)

## Luồng sau khi sửa

1. Checkout tạo đúng một đơn, backend chốt giá/tồn kho và dọn giỏ trong cùng transaction. Frontend chỉ tải lại giỏ; không gửi DELETE giỏ lần hai.
2. `POST /api/v1/payments` lấy giao dịch PENDING của chính chủ. Đã có URL thì trả lại cùng URL, cùng mã tham chiếu và ngày khởi tạo; không kéo dài thời hạn giữ hàng. Sau FAILED mới tạo attempt mới. Không đổi nhà cung cấp của một attempt đang chờ.
3. Backend tạo URL PAY từ cấu hình merchant thực; không có khóa mặc định nhúng trong code. HMAC-SHA512 ký chuỗi tham số đã sắp xếp và URL encode. Số tiền là VND nguyên × 100; hạn URL không vượt hạn đơn.
4. Khi trình duyệt quay về, người dùng phải đăng nhập. `POST /api/v1/payments/vnpay/verify-return` kiểm chữ ký, merchant, số tiền và quyền sở hữu; chỉ đọc trạng thái, không xác nhận đơn từ URL trình duyệt.
5. Khi còn PENDING, `POST /api/v1/payments/my/txn-ref/{ref}/reconcile` truy vấn server-to-server tới VNPay bằng `querydr`. Chỉ kết quả đã kiểm chữ ký và khớp merchant/ref/amount/loại thanh toán mới được áp dụng. Mỗi attempt giới hạn một query trong 5 phút, lưu trong DB để nhiều tiến trình không gửi trùng; timeout/lỗi vẫn là PENDING.
6. IPN công khai vẫn hoạt động tại `/api/v1/payments/vnpay-webhook`. IPN và querydr dùng chung settlement transaction: khóa đơn trước, payment sau; kiểm số tiền trước trạng thái; thành công cần cả responseCode và transactionStatus bằng `00`.
7. IPN trả JSON: `00` ghi nhận mới, `02` đã xử lý, `01` không tìm thấy, `04` sai số tiền, `97` sai chữ ký, `99` lỗi cần VNPay thử lại. Chỉ ACK sau commit. Không coi lỗi unique/DB là đã xử lý thành công.
8. Hủy/thất bại không lưu mã giao dịch `0` vào cột unique. Đơn còn chờ được thanh toán lại trong thời hạn giữ hàng. Các trạng thái chưa rõ, nghi ngờ gian lận hoặc đảo giao dịch không được tự chuyển FAILED để mời khách trả tiền lần nữa.
9. Tiền nhận sau khi đơn đã hủy vẫn được lưu và gắn cờ đối soát, ghi lịch sử; không phục hồi đơn hoặc tồn kho đã hoàn. Có thể hoàn tiền cho đơn CANCELLED đã nhận tiền muộn qua quy trình refund do admin thực hiện. Không tự gửi lại refund khi phản hồi không rõ.
10. Trang kết quả hiển thị riêng SUCCESS, PENDING, FAILED, REFUNDED và cần đối soát; hết thời gian polling không đồng nghĩa thất bại. Nút trả lại chỉ hiện khi backend xác nhận còn được phép.

Ngoài truy vấn lúc Return, một worker riêng đối soát nền các payment PENDING được tạo trong 24 giờ gần nhất, đã chờ ít nhất một phút. Mỗi lượt tối đa 5 giao dịch, không query lại trong 5 phút; ShedLock bảo vệ khi chạy nhiều instance. Worker không dùng chung luồng với job hết hạn đơn, nên VNPay chậm không chặn việc hoàn kho. Giao dịch cũ hơn 24 giờ cần tra cứu chủ động hoặc đối soát thủ công.

## Cấu hình Sandbox trên localhost

Trong `backend/.env` giữ các giá trị merchant riêng của bạn:

```dotenv
VNPAY_TMN_CODE=<merchant sandbox>
VNPAY_HASH_SECRET=<secret riêng>
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:5173/payment/vnpay/return
VNPAY_QUERY_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_REFUND_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_EXPIRE_MINUTES=15
```

VNPay không gọi được IPN về localhost. QueryDR cho phép backend local kiểm chứng kết quả khi khách quay lại. Với production, vẫn phải đăng ký IPN HTTPS công khai với VNPay; đóng trình duyệt không được làm mất thông báo nhận tiền. Không dùng Return URL để thay thế IPN. Endpoint QueryDR/refund và merchant production phải theo thông tin VNPay cấp.

Nếu phát triển frontend ở cổng khác, cần đồng bộ `VNPAY_RETURN_URL` và CORS backend rồi khởi động lại backend. Đừng mở thanh toán bằng `127.0.0.1` nhưng cấu hình Return dùng `localhost` nếu phiên đăng nhập đang gắn với hostname khác.

Migration V57 chỉ thêm ngày gốc gửi VNPay, ngày nhận tiền, thời điểm query và cờ đối soát. Nó backfill ngày gốc từ URL đã lưu; không sửa số tiền hay trạng thái giao dịch cũ. Refund dùng đúng `vnp_CreateDate` của yêu cầu gốc, và xác minh mã giao dịch hoàn tiền của gateway thay vì ép mã đó bằng mã thanh toán ban đầu.

## Kiểm thử

Các tests backend bao phủ canonical encoding có khoảng trắng/ký tự đặc biệt, checksum tạo URL và querydr, sai merchant/ref/amount, callback thiếu trường, số giao dịch bằng 0, callback trùng/đồng thời, timeout query, giới hạn query, quyền sở hữu, resume URL, hết hạn và nhận tiền muộn. PostgreSQL/Redis Testcontainers dùng dữ liệu riêng.

Frontend có kiểm thử phân loại trạng thái, URL thiếu/trùng tham số và giới hạn redirect về gateway HTTPS chính thức. Màn kết quả chỉ tin trạng thái backend; không lấy `vnp_ResponseCode=00` trên thanh địa chỉ làm bằng chứng thanh toán.

### Kết quả ngày 21/10/2026

- Java 21 `mvn verify`: **181 tests, 0 failures/errors/skipped**, build JAR thành công. Báo cáo cuối ở `output/vnpay/verify-passed.log` và `output/vnpay/passed-reports/` (Git ignore).
- Frontend `npm run check`: TypeScript đạt, **39 tests / 9 suites đạt**, production build đạt.
- 8 bài integration VNPay đạt, gồm 4 IPN đồng thời chỉ xác nhận đúng một lần; các yêu cầu còn lại nhận `02`. Fixtures được xóa sau mỗi test để không ảnh hưởng các bài kiểm tra seed data.
- Đã backup DB local trước V57 vào `backups/ladux-before-vnpay-v57.dump`. Migration và bản Docker có sửa đổi đã khởi động healthy lúc 19:45; kiểm tra sau đó không còn thấy app/frontend chạy. Cần kiểm tra/khởi động lại trước khi xác nhận trạng thái triển khai cuối.
- Checkout thực bằng tài khoản test riêng đã tạo đơn **#16**, payment **#16**; resume trả đúng attempt PENDING đó. Số tiền backend gồm 10.490.000 VND hàng + 30.000 VND phí giao hàng. Không tạo đơn thứ hai khi giỏ đã được backend dọn.
- HTTP tới URL Sandbox thật trả **`The terminal (website) not approved`** với mã tra cứu `T0iw53BX2r` lúc 19:52:33. Merchant Sandbox hiện tại chưa được VNPay phê duyệt. **Chưa thực hiện được bước nhập thẻ/OTP và chưa xác nhận một thanh toán Sandbox thực thành công.** Không có giao dịch hoàn tiền thực được gửi.
- Chrome còn gặp reset/đổi kết nối khi vào Sandbox; kiểm tra HTTP vẫn xác nhận lỗi merchant ở trên. Bài UI Return local chưa hoàn tất do kết nối local gián đoạn; không coi đây là smoke test đạt.

Để thử giao dịch thật trên Sandbox, chủ tài khoản cần [đăng ký/cập nhật merchant Sandbox của chính mình](https://sandbox.vnpayment.vn/devreg/), chờ VNPay duyệt và cập nhật `VNPAY_TMN_CODE`/`VNPAY_HASH_SECRET` trong `backend/.env`. Không dùng khóa merchant chia sẻ trên mạng. Khởi động lại backend sau khi thay cấu hình. Khi runtime chạy lại, cần hủy đơn test #16 nếu job hết hạn chưa hoàn kho và vô hiệu hóa tài khoản kiểm thử `vnpay_test_1792585632420` (id 14); đây là tài khoản do đợt kiểm thử này tạo, không phải tài khoản người dùng hiện có.
