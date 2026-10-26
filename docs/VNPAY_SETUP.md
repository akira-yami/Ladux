# Cấu hình và xử lý lỗi VNPAY

**Cập nhật 27/10/2026:** đã chuyển backend dev sang merchant Sandbox mới `34U1GIZM` do chủ dự án cung cấp. Container đã được tạo lại và healthy. Probe có giữ cookie phiên nhận `PAYMENT_PAGE_REACHED`, mở được trang “Chọn phương thức thanh toán (Test)”. Chưa thực hiện bước ngân hàng hoặc xác minh giao dịch hoàn tất/IPN. Các thông báo chưa được duyệt bên dưới thuộc merchant trước khi thay cấu hình.

## Lỗi của merchant cũ: website chưa được phê duyệt

Ngày 27/10/2026 (Asia/Bangkok), kiểm tra trực tiếp VNPAY Sandbox bằng cặp thông tin merchant đang dùng nhận được `MERCHANT_NOT_APPROVED` — cùng thông báo trong ảnh: **Website này chưa được phê duyệt**. Backend đang chạy có mã và secret khớp `backend/.env`; không phải thiếu biến secret. Thông báo này không chứng minh secret đúng, vì gateway có thể kiểm tra trạng thái merchant trước chữ ký.

Chủ tài khoản cần yêu cầu VNPAY kiểm tra/kích hoạt **đúng TmnCode trên Sandbox**. Gửi cho đầu mối hỗ trợ trong email VNPAY: TmnCode, môi trường Sandbox, địa chỉ website đã đăng ký, ảnh thông báo, mã tra cứu và thời gian trong ảnh. Không gửi HashSecret trong yêu cầu hỗ trợ thông thường. Mã nguồn không thể tự phê duyệt merchant trên hệ thống VNPAY.

## Lấy mã và secret ở đâu?

1. Đăng ký merchant kiểm thử tại [VNPAY Sandbox](https://sandbox.vnpayment.vn/devreg/), dùng email bạn quản lý và thông tin website của bạn.
2. Kiểm tra email, cả mục spam, và làm các bước kích hoạt VNPAY yêu cầu. Theo [tài liệu tích hợp chính thức](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), hệ thống gửi thông tin kết nối về email đăng ký.
3. Lấy **cùng một cặp** `vnp_TmnCode` và `vnp_HashSecret` (có thể ghi là Secret Key) của merchant Sandbox được cấp. HashSecret là khóa ký do VNPAY cấp, không phải mật khẩu đăng nhập và không tự tạo ngẫu nhiên để thay thế.
4. Nếu đã có tài khoản nhưng gặp lỗi trong ảnh, yêu cầu VNPAY kiểm tra việc phê duyệt merchant hiện có trước khi thử lại. Không lấy cặp mã/secret demo chia sẻ trên mạng.

## Chạy local bằng Docker hiện tại

Chỉnh **`backend/.env`**. Đây là file `env_file` mà `backend/docker-compose.yml` nạp cho app; toàn bộ biến `VNPAY_*` trong cấu hình dev lấy từ file này. File `.env` ở thư mục gốc và `.env.production` không thay thế cấu hình VNPAY dev đó.

```dotenv
VNPAY_TMN_CODE=MA_SANDBOX_VNPAY_CAP
VNPAY_HASH_SECRET=SECRET_SANDBOX_VNPAY_CAP
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_QUERY_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_REFUND_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_RETURN_URL=http://localhost:5173/payment/vnpay/return
VNPAY_IPN_URL=http://localhost:8080/api/v1/payments/vnpay-webhook
VNPAY_EXPIRE_MINUTES=15
```

Thay hai giá trị đầu bằng thông tin thật VNPAY cấp. Dán nguyên giá trị, tránh khoảng trắng thừa. Không đưa HashSecret vào frontend, biến `VITE_*`, Git hoặc tin nhắn chat. Frontend hiện gọi API tại `http://localhost:8080/api/v1` và không cần secret thanh toán.

Từ thư mục gốc dự án, kiểm tra file và gateway:

```powershell
node scripts/check-vnpay.mjs
node scripts/check-vnpay.mjs --gateway
```

Lệnh đầu chỉ kiểm tra cấu hình file. Lệnh `--gateway` tạo một URL chẩn đoán riêng gửi tới Sandbox, không tạo đơn trong Ladux, không nhập thẻ/OTP, không thực hiện thanh toán hoặc hoàn tiền. Không in secret hoặc URL đã ký. Nó báo rõ lỗi chưa được duyệt/sai chữ ký khi nhận diện được; phản hồi khác hoặc lỗi kết nối là **chưa xác nhận**, không coi là thanh toán thành công. Công cụ không kiểm tra cấu hình của container đang chạy.

Sau khi cập nhật file, **tạo lại container app** để nạp biến mới:

```powershell
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d --no-deps --force-recreate app
docker compose --env-file backend/.env -f backend/docker-compose.yml ps app
```

Chỉ `docker restart` không nạp lại `.env`. Các lệnh trên dùng image hiện có vì thay credential không cần build mã. Sau khi thay cặp mã/secret, thử một đơn và URL thanh toán mới; URL PENDING đã lưu có thể vẫn mang mã/chữ ký cũ và không được tự thay để tránh tạo thanh toán trùng. Không sửa trạng thái payment để ép thành công.

Nếu chạy Spring trực tiếp từ IDE/Maven, Spring không tự đọc `backend/.env` trong dự án này: cần nạp các biến tương ứng vào cấu hình Run/Debug của IDE hoặc môi trường tiến trình Java rồi chạy lại. Công cụ có thể kiểm tra file khác bằng `--config PATH`.

## Return URL và IPN khác nhau thế nào?

### Frontend qua ngrok và lỗi Vite chặn hostname

Nếu Return báo `Blocked request. This host ... is not allowed`, cấu hình trong `frontend/.env`:

```dotenv
VITE_API_BASE_URL=/api/v1
LADUX_DEV_PUBLIC_URL=https://tidy-filth-sprinkled.ngrok-free.dev
```

Thay URL bằng tunnel frontend đang dùng. Vite chỉ cho phép hostname khai báo này, không bật `allowedHosts: true`. Dev server proxy `/api` và `/uploads` tới backend cổng 8080, nên trình duyệt trên ngrok không phải gọi `http://localhost:8080` khác origin. Thêm origin ngrok vào `APP_CORS_ALLOWED_ORIGINS` trong `backend/.env` (giữ các origin localhost cần dùng), rồi tạo lại container app để nạp CORS. Nếu Vite chưa tự khởi động lại sau khi sửa `.env`, chạy lại `npm run dev` trong `frontend/`.

Đăng nhập và bắt đầu checkout trên **cùng hostname với `VNPAY_RETURN_URL`**. Cookie đăng nhập localhost không tự chuyển sang ngrok. Nếu vừa nhập OTP và trang kết quả bị chặn, tải lại chính URL kết quả đó sau khi sửa cấu hình; nếu cần đăng nhập, dùng cùng hostname. Không tạo thêm đơn/thanh toán khi chưa kiểm tra trạng thái đơn vừa trả tiền.

Checkout đã tạo đơn trước khi chuyển sang VNPAY; backend dọn giỏ trong transaction đó. Frontend thay entry checkout trong browser history bằng Đơn hàng của tôi trước khi ra gateway. Back từ gateway quay về đơn đã tạo và có thể tiếp tục thanh toán; không dựng lại form đặt đơn 0 đồng hoặc khôi phục giỏ để tạo đơn trùng. Nếu mở checkout khi giỏ trống, giao diện hiển thị liên kết xem đơn và tiếp tục mua sắm. Giỏ chưa tải/lỗi API có trạng thái tải/thử lại riêng.

| Biến | Nơi nhận | Cấu hình |
| --- | --- | --- |
| `VNPAY_RETURN_URL` | Trình duyệt quay lại frontend | `http://localhost:5173/payment/vnpay/return` khi dev đúng cổng 5173; dùng cùng hostname bạn đăng nhập |
| `VNPAY_IPN_URL` | VNPAY gọi server backend | `/api/v1/payments/vnpay-webhook` trên backend HTTPS công khai |
| `VNPAY_QUERY_URL` / `VNPAY_REFUND_URL` | Backend gọi VNPAY | API giao dịch do VNPAY cấp, không phải URL của Ladux |

VNPAY không gọi IPN vào localhost được. Khi dev local, backend đã hỗ trợ QueryDR lúc người dùng quay về và worker đối soát nền. Để kiểm thử IPN, dùng tunnel HTTPS công khai tới cổng backend 8080 rồi **đăng ký URL đầy đủ với VNPAY**. Chỉ đặt `VNPAY_IPN_URL` trong `.env` không tự đăng ký callback: request PAY không gửi biến này. Kiểm tra lại URL tunnel mỗi lần nó thay đổi; không dùng địa chỉ tunnel cũ đã hết hoạt động.

Return URL chỉ dùng để kiểm tra/hiển thị kết quả. Chỉ IPN hoặc QueryDR đã xác minh chữ ký và số tiền mới xác nhận đã nhận tiền. Xem chi tiết tại [luồng VNPAY](VNPAY_FLOW.md).

## Khi triển khai thanh toán thật

- Làm việc với VNPAY để được cấp merchant **production**, kích hoạt website và đăng ký callback cho domain thực. Không dùng credential Sandbox với endpoint live.
- Điền `VNPAY_*` vào **`.env.production` ở thư mục gốc**, dựa trên `.env.production.example`. Return trỏ tới frontend HTTPS, IPN trỏ tới backend HTTPS; QueryDR/refund dùng URL live VNPAY cung cấp. File `.env.production` hiện tại còn URL Sandbox/localhost, chưa phải cấu hình thanh toán thật.
- Chạy theo [production runbook](PRODUCTION_RUNBOOK.md). Giữ secret ở máy chủ/secret manager phía backend; lưu ý biến shell có thể ghi đè giá trị `--env-file` trong Compose production.
- Sau khi merchant được duyệt, cần kiểm thử giao dịch Sandbox thực tới bước ngân hàng và đối chiếu trạng thái đơn/IPN/QueryDR trước khi dùng tiền thật. Chẩn đoán hiện tại dừng ở lỗi phê duyệt merchant, chưa xác nhận một giao dịch Sandbox hoàn tất.

## Kiểm tra mã và cấu hình ngày 27/10/2026

Đã sửa Compose dev để không khai báo lặp `VNPAY_*` trong `environment`: trước đó giá trị từ `.env` gốc/shell có thể ghi đè `backend/.env`; URL tùy chọn chưa khai báo còn bị biến thành chuỗi rỗng thay vì nhận mặc định Spring. Đã kiểm tra Compose thực tế: mã, secret và các URL khớp `backend/.env`, kể cả khi shell có mã merchant khác.

Kiểm thử backend toàn dự án bằng Java 21: 227 tests, 1 failure và 1 error. Các test thuộc payment đều đạt (44 tests, gồm 8 integration). Hai lỗi thuộc `ProductionMigrationSmokeTest` (kỳ vọng số migration cũ) và `DiscoveryRepositoryIntegrationTest` (fixture giá âm vi phạm constraint V62 hiện có trong workspace); không sửa chúng trong đợt cấu hình VNPAY này. Báo cáo local: `output/vnpay/verify-2026-10-27.log`.

Kiểm thử phân loại kết quả/URL VNPAY frontend: 5/5 đạt. Công cụ kiểm tra VNPAY và production preflight: 12/12 đạt, đã đưa vào CI. Chưa có giao dịch Sandbox qua bước ngân hàng vì merchant bị chặn tại gateway.

Lần sửa Back/Return tiếp theo: TypeScript, 46 tests frontend và production build đạt. Chrome chạy fixture API riêng đạt 6 tình huống: Back về đơn PENDING, tiếp tục thanh toán không tạo đơn mới, checkout giỏ trống, Return SUCCESS, Return PENDING được QueryDR xác nhận, và lỗi tải giỏ. Đây là kiểm thử giao diện bằng fixture, không phải giao dịch ngân hàng thật. HTTP thực qua ngrok trả trang Return 200; hostname khác vẫn bị chặn 403; API proxy trả 200 với CORS đúng origin ngrok. Backend đã nạp CORS mới và healthy.
