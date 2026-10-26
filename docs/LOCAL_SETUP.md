# Chạy Ladux từ một bản clone mới

Hướng dẫn cho môi trường **local/demo**. Stack đầy đủ gồm frontend Nginx, backend Spring Boot, PostgreSQL/pgvector và Redis. Production dùng [runbook riêng](PRODUCTION_RUNBOOK.md).

## 1. Yêu cầu

| Nhu cầu | Công cụ |
| --- | --- |
| Chạy cả stack bằng container | Docker Engine/Desktop và Docker Compose v2 |
| Frontend hot reload hoặc test | Node.js 22+ và npm |
| Backend test ngoài container | JDK 21, Maven 3.9+, Docker đang chạy |

CI dùng Java 21 và Node.js 22; Dockerfile frontend build bằng Node.js 24. Maven Wrapper cần metadata trong `.mvn`; repo hiện không track thư mục đó, nên các lệnh tái hiện bên dưới dùng Maven đã cài trên máy.

## 2. Chuẩn bị cấu hình

Tại thư mục gốc repo, chỉ copy khi file đích **chưa tồn tại**. Không ghi đè bộ credential đang dùng.

Bash:

```bash
test -f .env || cp .env.example .env
test -f backend/.env || cp backend/.env.example backend/.env
```

PowerShell:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
if (-not (Test-Path backend/.env)) { Copy-Item backend/.env.example backend/.env }
```

Sinh hai giá trị mới bằng Node.js; lệnh này chỉ in giá trị trên máy của bạn:

```bash
node -e "const c=require('node:crypto'); console.log('DB_PASSWORD='+c.randomBytes(24).toString('base64url')); console.log('JWT_SECRET='+c.randomBytes(48).toString('base64'))"
```

- Điền **cùng DB_USERNAME và DB_PASSWORD** vào `.env` và `backend/.env`.
- Điền `JWT_SECRET` mới vào `backend/.env`. Key phải có ít nhất 32 byte sau giải mã Base64.
- Giữ `SPRING_PROFILES_ACTIVE=dev`, cookie Secure tắt và CAPTCHA tắt cho HTTP local.
- Credential `local-demo-*` là dữ liệu giả để cấu hình component. Chúng không đăng nhập Google, gửi email, gọi AI hay thanh toán được. Với các dịch vụ chưa cấu hình, dùng luồng tài khoản local/COD để khám phá nghiệp vụ.

| File | Được đọc bởi | Vai trò |
| --- | --- | --- |
| `.env` | Compose ở thư mục gốc | Thay biến DB và public CAPTCHA key khi build frontend |
| `backend/.env` | `env_file` của app | JWT, credential và URL callback backend |
| `frontend/.env` | Vite khi chạy frontend source | Public API base URL và cấu hình frontend |
| `.env.production` | Lệnh Compose production có `--env-file` | Bộ cấu hình triển khai riêng; không tự lấy từ dev |

Spring Boot chạy trực tiếp từ IDE/Maven **không tự đọc dotenv** trong dự án này. Nếu dùng cách đó, cần nạp biến vào tiến trình Java và trỏ DB/Redis tới địa chỉ truy cập được từ host. Chạy Docker theo hướng dẫn này không cần bước nạp tay đó.

## 3. Khởi động đầy đủ

Chạy từ thư mục chứa `compose.yaml`:

```bash
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
```

Lần build đầu phải tải dependency/image. Flyway nạp schema và dữ liệu mẫu của profile dev; PostgreSQL có thể cần thêm thời gian init trên Docker Desktop.

| Thành phần | Địa chỉ |
| --- | --- |
| Storefront | http://localhost:5173 |
| Admin | http://localhost:5173/admin |
| API | http://localhost:8080/api/v1 |
| Swagger dev | http://localhost:8080/swagger-ui.html |
| Readiness gồm DB và Redis | http://localhost:8080/actuator/health/readiness |

Compose gốc dùng project name `backend`, bind mount `uploads/` và named volume cho PostgreSQL/Redis. Giữ project/volume hiện có khi dùng lại dữ liệu.

### Tài khoản và kịch bản demo

Trên **database dev mới được seed**, migration V6 đặt tài khoản demo `admin` / `Admin@123`. Thông tin này chỉ dành cho dữ liệu mẫu local; database đã chỉnh tài khoản có thể có mật khẩu khác. Production không nạp dev seed và có quy trình bootstrap admin riêng.

1. Đăng ký một tài khoản khách, duyệt catalog và chọn cấu hình còn hàng.
2. Thêm vào giỏ, kiểm tra tổng tiền và đặt **COD**.
3. Vào admin, xác nhận đơn và chuyển sang đang giao.
4. Ghi nhận thu tiền COD bằng mã chứng từ tại trang thanh toán, rồi mới chuyển đơn sang giao thành công.
5. Thử một đơn khác và hủy để đối chiếu hoàn kho/coupon và lịch sử.
6. Mở `/smartfit`, chọn ngân sách/nhu cầu, so sánh SKU và mở trang chi tiết từ kết quả.

Đây là dữ liệu demo. Đăng ký/login local và COD không cần credential VNPay/Google thật; OTP email, đổi mật khẩu qua OTP và liên hệ cần SMTP hoạt động.

### Bật các dịch vụ ngoài khi cần

| Tính năng | Cấu hình |
| --- | --- |
| Google login | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`; đăng ký callback local `http://localhost:8080/login/oauth2/code/google` |
| OTP/email liên hệ | `MAIL_USERNAME`, `MAIL_PASSWORD`, `CONTACT_RECEIVER_EMAIL` |
| VNPay Sandbox | Cặp merchant/HashSecret và toàn bộ URL cùng môi trường; xem [VNPay setup](VNPAY_SETUP.md) |
| CAPTCHA v3 | Secret backend, public site key frontend, hostname/action phù hợp; bật `CAPTCHA_ENABLED=true` |
| Chat/semantic retrieval | `DEEPSEEK_API_KEY`, `GOOGLE_GENAI_API_KEY`; semantic retrieval cần dữ liệu đã index |

Smart Search, SmartFit và Smart Compare lấy dữ liệu từ catalog mà không gọi LLM. Contextual chat với SKU đã chọn có thể giữ thẻ hợp lệ khi phần diễn giải lỗi; truy vấn semantic không có SKU còn phụ thuộc embedding provider.

Sau khi đổi env của container, dùng `docker compose up -d --force-recreate app`. Restart đơn thuần không nạp env mới. Thay public build variable của frontend cần build lại frontend.

## 4. Vite hot reload

Giữ backend/DB/Redis Docker, dừng frontend Nginx để giải phóng cổng 5173:

```bash
docker compose stop frontend
cd frontend
```

Copy `frontend/.env.example` thành `frontend/.env` nếu chưa có, sau đó:

```bash
npm ci
npm run dev -- --port 5173 --strictPort
```

Giữ `VITE_API_BASE_URL=/api/v1` để Vite proxy API/uploads/OAuth. Dùng nhất quán `localhost`; đổi qua `127.0.0.1` hoặc tunnel tạo một cookie origin khác. Cách thử tunnel được ghi trong [VNPay setup](VNPAY_SETUP.md).

## 5. Kiểm tra source

Từ thư mục gốc:

```bash
mvn -B -f backend/pom.xml clean verify
node --test scripts/preflight.test.mjs scripts/check-vnpay.test.mjs
```

Trong `frontend/`:

```bash
npm ci
npm run check
npm audit --audit-level=moderate
```

Backend integration tests tự tạo PostgreSQL/Redis tạm bằng Testcontainers; không trỏ vào DB demo đang chạy. Nếu Maven báo `release version 21 not supported`, sửa `JAVA_HOME` về JDK 21. Nếu Vitest trên Windows báo `EPERM` khi rename file tạm, đặt `TEMP`/`TMP` vào một thư mục local ghi được rồi chạy lại; giữ nguyên test.

## 6. Khắc phục lỗi thường gặp

| Triệu chứng | Kiểm tra |
| --- | --- |
| Compose báo thiếu DB_PASSWORD | File `.env` gốc đã tạo và thay placeholder chưa? |
| Backend báo JWT secret yếu | `JWT_SECRET` đã là Base64 của đủ byte, không còn placeholder? |
| DB authentication failed sau khi đổi env | Volume PostgreSQL cũ vẫn giữ password cũ; sửa cấu hình khớp DB hoặc đổi credential có chủ đích. Env mới không tự đổi password trong DB. |
| Không thấy ảnh mẫu | Chạy Compose gốc để mount đúng `uploads/`; kiểm tra đường dẫn `/uploads/...` trong Network |
| Flyway checksum/version mismatch | Không tự sửa migration/repair/xóa volume. Đối chiếu DB và [runbook V49](../backend/docs/retired-v49-recovery.md) nếu DB thuộc trường hợp đó. |
| Cổng 5173/8080 bận | Tránh chạy đồng thời hai frontend/backend trên cùng cổng |
| Login thành công rồi mất phiên khi return | Kiểm tra frontend, cookie, OAuth redirect và VNPay Return dùng cùng hostname dự kiến |

Xem log bằng `docker compose logs --tail=100 app`. Dừng stack bằng `docker compose stop`. Không dùng `down -v` trên dữ liệu cần giữ.
