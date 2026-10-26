# VNPay — cấu hình và troubleshooting

Hướng dẫn dùng bộ merchant của bạn với Ladux. Luồng xác minh/settlement nằm trong [VNPay flow](VNPAY_FLOW.md); trạng thái kiểm chứng hiện tại nằm trong [Project status](PROJECT_STATUS.md).

## 1. Bộ credential và môi trường

Cần cặp **TmnCode + HashSecret do VNPay cấp**, cùng PAY/query/refund URL thuộc đúng môi trường. HashSecret là khóa ký của merchant, không phải mật khẩu đăng nhập và không tự sinh để thay thế khóa của gateway.

Chỉnh `backend/.env` cho local. Template `backend/.env.example` chỉ có merchant giả; thay nguyên bộ để thử Sandbox:

```dotenv
VNPAY_TMN_CODE=REPLACE_WITH_YOUR_SANDBOX_MERCHANT
VNPAY_HASH_SECRET=REPLACE_WITH_YOUR_SANDBOX_HASH_SECRET
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_QUERY_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_REFUND_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_RETURN_URL=http://localhost:5173/payment/vnpay/return
VNPAY_IPN_URL=http://localhost:8080/api/v1/payments/vnpay-webhook
VNPAY_EXPIRE_MINUTES=15
PAYMENT_VNPAY_RECONCILIATIONENABLED=true
```

Không đưa HashSecret vào frontend hoặc biến `VITE_*`. Bộ Sandbox không thay thế merchant production.

Compose gốc nạp runtime app từ `backend/.env`; `.env` gốc cung cấp biến thay thế cho Compose. `.env.production` chỉ được dùng khi gọi stack production với `--env-file .env.production`.

## 2. Kiểm tra trước khi mở một đơn thử

Từ thư mục gốc:

```bash
node scripts/check-vnpay.mjs
node scripts/check-vnpay.mjs --gateway
```

Lệnh đầu đọc cấu hình file. `--gateway` gửi chẩn đoán tới Sandbox và có thể nhận diện trang chọn phương thức thanh toán, lỗi merchant chưa duyệt hoặc chữ ký sai. Nó không tạo order trong Ladux, không nhập thông tin ngân hàng và không xác nhận một thanh toán/refund đã hoàn tất. Công cụ không chứng minh container đang dùng đúng bộ env đó.

Sau khi đổi env, tạo lại app để nạp cấu hình:

```bash
docker compose up -d --no-deps --force-recreate app
```

Nếu dùng Compose backend riêng, lệnh tương ứng từ root là:

```bash
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d --no-deps --force-recreate app
```

Chỉ chọn stack đang sử dụng. `docker restart` không nạp lại env. Spring chạy trực tiếp từ IDE/Maven cũng cần nạp env vào tiến trình Java.

URL payment đã lưu giữ nguyên merchant/ref/ngày/chữ ký ban đầu. Sau khi đổi bộ credential, dùng một lần checkout thử mới; không sửa URL đã lưu hay ép payment sang SUCCESS.

## 3. Phân biệt callback

| Cấu hình | Người gọi / nơi nhận | Mục đích |
| --- | --- | --- |
| Return URL | Browser về frontend `/payment/vnpay/return` | Kiểm tra dữ liệu hiển thị và đọc trạng thái backend |
| IPN URL | VNPay gọi backend `/api/v1/payments/vnpay-webhook` | Thông báo server-to-server để settlement xác minh |
| Query/refund URL | Backend gọi API VNPay | Truy vấn/hoàn tiền theo hợp đồng gateway |

VNPay không truy cập được localhost của máy bạn. Muốn thử IPN, public backend bằng HTTPS/tunnel và đăng ký URL đầy đủ với VNPay. Đặt `VNPAY_IPN_URL` trong dotenv không tự đăng ký callback; request PAY không gửi biến này.

Local có thể kiểm tra QueryDR khi người mua quay lại và qua worker đối soát. Production vẫn cần IPN công khai được đăng ký; không dùng browser Return làm bằng chứng đã nhận tiền.

## 4. Google OAuth, Vite proxy và cookie local

Cấu hình Vite thường dùng:

```dotenv
VITE_API_BASE_URL=/api/v1
VITE_DEV_CANONICAL_ORIGIN=http://localhost:5173
```

Cấu hình backend:

```dotenv
OAUTH2_SUCCESS_REDIRECT=http://localhost:5173/auth/oauth2/success
OAUTH2_FAILURE_REDIRECT=http://localhost:5173/login?oauth2Error=true
VNPAY_RETURN_URL=http://localhost:5173/payment/vnpay/return
```

Google Authorized redirect URI local: **`http://localhost:8080/login/oauth2/code/google`**. Giữ callback đúng scheme/hostname/port/path của OAuth client. `localhost` và `127.0.0.1` là hai hostname khác nhau.

Khi cố ý dùng tunnel frontend, đặt `LADUX_DEV_PUBLIC_URL` vào URL HTTPS đang có và để trống canonical origin nếu muốn phục vụ thiết bị khác. Đăng nhập, OAuth success/failure và Return phải khớp origin bạn đang sử dụng; cookie localhost không tự chuyển sang tunnel. Vite chỉ cho phép hostname khai báo, không bật wildcard cho mọi host.

Sau khi đặt hàng, giỏ đã được backend dọn. Frontend thay entry checkout bằng trang đơn hàng trước khi ra gateway; Back quay về đơn đã tạo và có thể tiếp tục payment còn hợp lệ. Không dựng lại giỏ để tạo thêm đơn vì một lần return chưa rõ kết quả.

## 5. Lỗi thường gặp

| Triệu chứng | Cách xử lý |
| --- | --- |
| “Website này chưa được phê duyệt” | Kiểm tra trạng thái đúng merchant Sandbox với VNPay; source không tự phê duyệt merchant. Thông báo này chưa chứng minh HashSecret đúng. |
| Sai chữ ký | Kiểm tra cặp merchant/secret cùng bộ, nguyên giá trị và môi trường URL; tạo giao dịch thử mới sau khi thay cấu hình |
| Google `redirect_uri_mismatch` | Đối chiếu URI thực tế với Google Cloud, nhất là `localhost`/`127.0.0.1`, proxy và scheme |
| Vite chặn hostname | Khai báo URL tunnel hiện tại qua `LADUX_DEV_PUBLIC_URL`, khởi động lại Vite khi cần |
| Return yêu cầu đăng nhập lại | Kiểm tra origin cookie/Return; không mang token hoặc cookie trong query string |
| IPN không tới | Kiểm tra URL HTTPS public đã đăng ký với VNPay, firewall và tunnel còn hoạt động |
| UI còn PENDING sau khi đã thao tác ngân hàng | Đọc trạng thái backend, kiểm tra query/IPN và đối soát; không coi hết polling là thất bại hay tự trả tiền lần nữa |
| Refund chưa rõ | Giữ attempt để review; đối chiếu portal/sao kê trước khi xác nhận chứng từ hoặc thực hiện bước tiếp theo |

## 6. Trước thanh toán live

Merchant production và WebAPI URL phải theo bộ VNPay cấp. Domain/HTTPS, IPN, Return, OAuth, CORS và CAPTCHA phải đồng bộ. Chạy preflight và smoke các luồng thành công/thất bại, callback lặp/mất và refund theo [production runbook](PRODUCTION_RUNBOOK.md).

Mở được trang chọn phương thức Sandbox chỉ xác nhận tới bước đó. Các tests gateway mock/fixture cũng không thay thế chứng cứ tiền đã được nhận hoặc hoàn ở nhà cung cấp thật.

Tài liệu nhà cung cấp: [PAY/IPN/Return](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), [QueryDR/refund](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html).
