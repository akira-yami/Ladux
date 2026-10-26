# Triển khai và vận hành Ladux

Áp dụng cho snapshot portfolio 27/10/2026, schema tới V62. Phát triển tính năng đang tạm dừng; runbook này chuẩn bị cho một lần triển khai sau, không chứng minh hệ thống đang vận hành production. Xem [trạng thái và bằng chứng](PROJECT_STATUS.md).

Stack: frontend Nginx, backend Java 21, PostgreSQL 17/pgvector, Redis và Caddy. Chỉ Caddy mở cổng 80/443. Storefront gọi API cùng origin; một hostname API riêng phục vụ IPN và kiểm tra vận hành.

## 1. Chuẩn bị cấu hình

Tại thư mục gốc repo, chỉ tạo file nếu chưa có:

```bash
test -f .env.production || cp .env.production.example .env.production
chmod 600 .env.production
```

Điền các giá trị thật theo template. Không dùng credential từ `output/.env.production.smoke`; đây là dữ liệu giả cho test. Không commit `.env.production`.

Chép credential từ `backend/.env` chỉ tái sử dụng bộ môi trường nguồn. Nếu nguồn là Sandbox/local, các URL và credential đó vẫn là Sandbox/local; cần đồng bộ lại toàn bộ callback/CORS theo domain triển khai và dùng merchant production trước mở bán. Đổi `DB_PASSWORD` trong file không tự đổi password của database đã có volume.

- Trỏ DNS của `LADUX_WEB_DOMAIN` và `LADUX_API_DOMAIN` về máy triển khai, mở 80/443 và đặt `CADDY_EMAIL`.
- `JWT_SECRET`: Base64 của ít nhất 32 byte ngẫu nhiên. Sinh bằng công cụ quản lý secret hoặc `openssl rand -base64 48`.
- Google OAuth: khai báo callback `https://WEB_DOMAIN/login/oauth2/code/google` trong Google Cloud. Success redirect là `https://WEB_DOMAIN/auth/oauth2/success`.
- reCAPTCHA v3: đăng ký hostname storefront; public site key cho frontend, secret cho backend, action `login`. Production buộc CAPTCHA và secure cookie hoạt động.
- VNPay: dùng merchant và WebAPI URL do VNPay cấp. Return về storefront; IPN về `https://API_DOMAIN/api/v1/payments/vnpay-webhook`. Query/refund URL trỏ tới VNPay, không trỏ về Ladux.
- SMTP và hai nhà cung cấp AI: điền thông tin của môi trường triển khai. Xác minh SMTP/OTP thực tế. Các API AI chỉ được gọi khi dùng chức năng tương ứng.
- `LADUX_ALLOW_INDEXING=true` tạo robots/sitemap và cho index các trang công khai. Đặt `false` cho staging. Sitemap hiện gồm các route công khai tĩnh; chưa sinh toàn bộ URL sản phẩm.

Cho database mới, bật `BOOTSTRAP_ADMIN_ENABLED=true` và điền username/email/password riêng. Password phải từ 16 đến 72 byte UTF-8. Bootstrap chỉ tạo admin khi chưa có vai trò ADMIN gắn với bất kỳ user nào; không tự nâng quyền tài khoản có sẵn. Sau lần đầu, tắt flag và xóa password bootstrap khỏi file môi trường.

Database có sẵn admin không được đổi mật khẩu bằng bootstrap. Kiểm tra tài khoản quản trị đang hoạt động, loại bỏ credential demo và hoàn tất khôi phục tài khoản trước khi nhận khách thật.

## 2. Kiểm tra trước triển khai

```bash
node scripts/preflight.mjs
docker compose --env-file .env.production -f backend/docker-compose.prod.yml config --quiet
```

Nếu máy chủ chỉ cài Docker, chạy preflight không có bước Compose bên trong container, rồi chạy lệnh Compose ở trên:

```bash
docker run --rm --network none --mount "type=bind,src=$PWD,dst=/workspace,readonly" -w /workspace node:24-alpine node scripts/preflight.mjs --skip-docker
```

Preflight không in giá trị secret. Nó kiểm tra placeholder, độ dài key, HTTPS origin, đường dẫn callback, domain và cấu hình Compose. Nó không chứng minh DNS/TLS, merchant, API key hay email thật hoạt động. Staging có thể dùng `--allow-sandbox`; không dùng flag này để bỏ qua xác minh merchant live trước mở bán.

Các gate source được CI chạy:

```bash
mvn -B -f backend/pom.xml clean verify
cd frontend
npm ci
npm run check
npm audit --audit-level=moderate
cd ..
node --test scripts/preflight.test.mjs scripts/check-vnpay.test.mjs
```

Backend audit dùng dependency đã resolve:

```bash
mvn -B -f backend/pom.xml dependency:tree -DoutputType=json -DoutputFile="$PWD/output/backend-dependencies.json"
node scripts/audit-maven.mjs
```

OSV chỉ nhận tên/phiên bản dependency công khai. Kết quả có thời điểm kiểm tra; cần chạy lại khi phát hành. Audit này không thay cho scan image hoặc kiểm chứng khả năng khai thác của từng advisory.

## 3. Database và media hiện hữu

Không sửa hoặc chạy lại migration đã áp dụng. V57 giữ các ngày gateway, V58 đặt guard hoàn tiền theo từng payment, V59 thêm optimistic version cho tồn kho, V60 tạo role tham chiếu và V61 quản lý VectorStore bằng Flyway. V62 thêm giới hạn coupon theo khách, CHECK tiền/số lượng và index cho các truy vấn nóng.

Các CHECK `NOT VALID` của V62 áp dụng cho INSERT/UPDATE mới nhưng chưa chứng minh dữ liệu lịch sử đều hợp lệ. Đối chiếu dữ liệu cũ, xử lý bất thường và validate constraint trong kế hoạch nâng cấp; diễn tập CREATE INDEX trên bản sao đủ lớn để đo thời gian/ảnh hưởng khóa.

Trước nâng cấp, backup DB/media và chạy migration trên bản sao. Kiểm tra lịch sử/checksum, đặc biệt database từng dùng V49 đã bị bỏ hoặc các migration cũ đã sửa trước đây. Dùng [runbook V49 hiện có](../backend/docs/retired-v49-recovery.md); không tự `repair`, xóa bảng lịch sử hoặc bật `baseline-on-migrate` để vượt lỗi.

Production mới dùng volume `uploads-data`. Nếu bản cũ bind mount thư mục `uploads`, phải chép media vào volume mới trước khi đổi traffic, giữ bản cũ để khôi phục và đặt quyền sở hữu theo user `ladux` của image. Không đổi tên Compose project trong lúc nâng cấp: tên project quyết định volume được dùng. Nếu trước đây có `-p`/`COMPOSE_PROJECT_NAME`, giữ đúng tên đó.

Đối chiếu số lượng đơn, payment, khách hàng, tồn kho và tham chiếu ảnh sau diễn tập. Production không nạp `db/devdata` và không tự sửa schema qua Hibernate/AI.

## 4. Khởi động

```bash
docker compose --env-file .env.production -f backend/docker-compose.prod.yml up -d --build --wait --wait-timeout 300
docker compose --env-file .env.production -f backend/docker-compose.prod.yml ps
```

Readiness kiểm tra DB và Redis. App chỉ healthy sau khi startup/migration/bootstrap hoàn tất. Tắt bootstrap sau lần đầu rồi recreate app. Build frontend lại nếu thay domain, CAPTCHA public key hoặc chính sách index.

Smoke sau triển khai:

1. Mở storefront và deep link `/products/ID`, kiểm tra API/ảnh/SPA fallback qua HTTPS.
2. Đăng ký, đăng nhập khách/admin với CAPTCHA thật; refresh, reload, logout và Google OAuth. Kiểm tra cookie Secure/HttpOnly và OTP email.
3. Đặt đơn nhỏ với COD và VNPay thật/sandbox theo môi trường. Đối chiếu đúng VND, callback lặp, đóng trình duyệt và khôi phục giao dịch mất IPN.
4. Kiểm tra `https://API_DOMAIN/actuator/health/readiness`. `/actuator/prometheus` phải yêu cầu ADMIN; thu thập metrics qua kênh quản trị phù hợp.
5. Xác nhận backup ngoài host, người nhận cảnh báo và bài restore đã diễn tập.

Không chạy `production-http-smoke.mjs` vào production. Script bị giới hạn vào stack fixture và dùng JWT giả để kiểm tra HTTP/RBAC; nó không kiểm chứng login hay chuyển tiền thật.

## 5. Thanh toán và hoàn tiền

- COD: xác nhận đơn, chuyển đang giao, ghi nhận đã thu đủ tiền ở trang Thanh toán bằng mã chứng từ, rồi mới chuyển giao thành công.
- VNPay: admin không sửa provider/status thủ công. IPN/query đã xác minh cập nhật tiền; Return URL chỉ xác minh dữ liệu hiển thị.
- Tiền đến sau hủy hoặc có payment trả hai lần được đánh dấu cần đối soát. Không phục hồi đơn/tồn kho đã hủy.
- Tại trang Thanh toán, kiểm tra các giao dịch cần đối soát; hoàn đúng từng payment khi cần. Refund hỗ trợ toàn bộ payment, không hỗ trợ hoàn một phần.
- Kết quả refund chưa rõ được giữ `PROCESSING`/`REVIEW_REQUIRED`. Đối chiếu bằng merchant portal/sao kê trước khi xác nhận chứng từ. Không tự retry hoặc sửa SQL. Nếu phải hoàn tiền qua ngân hàng/portal, chỉ xác nhận sau khi có bằng chứng đã hoàn.
- Hoàn payment thừa giữ nguyên giao hàng hợp lệ; hoàn tất tiền cho đơn trả/hủy mới đóng đơn thành REFUNDED. Giữ lịch sử chứng từ.

## 6. Backup và restore

Script backup tạm dừng app để DB và media nhất quán, rồi khởi động lại nếu trước đó đang chạy. Chọn thời gian bảo trì phù hợp. `LADUX_BACKUP_DIR` phải nằm ngoài thư mục phục vụ web và có quyền truy cập hạn chế.

```bash
LADUX_BACKUP_DIR=/srv/private/ladux-backups bash scripts/production-backup.sh
bash scripts/restore-smoke.sh /srv/private/ladux-backups/TIMESTAMP
```

Mỗi bộ có `database.dump`, `uploads.tar.gz`, `SHA256SUMS`. Chép sang nơi lưu mã hóa ngoài máy chủ, đặt retention theo nhu cầu và theo dõi job thất bại. Không tự xóa backup cũ trước khi xác nhận bản mới dùng được.

Restore smoke tạo PostgreSQL tạm, không mở host port/không dùng volume production; xác minh checksum, restore DB và đọc các bảng chính. Sau đó còn phải đối chiếu tổng nghiệp vụ và tham chiếu media, chốt RPO/RTO và diễn tập khôi phục đầy đủ trên hạ tầng mới. Script không tự ghi đè database đang vận hành.

## 7. Theo dõi và rollback

Theo dõi readiness, HTTP 5xx/latency, dung lượng DB/uploads, memory Redis, số payment cần review, refund chưa rõ kết quả và lỗi SMTP/gateway. Request có `X-Request-ID`; log app chứa cùng ID. Không đưa token/cookie/secret vào log hỗ trợ.

Cache dùng namespace `ladux:modules:v2:`, TTL hữu hạn và invalidation sau commit. Không chạy `FLUSHALL`/`FLUSHDB`: rate limit và trạng thái xác thực dùng key riêng. Cache phiên bản cũ có thể dọn theo đúng prefix sau khi ngừng toàn bộ instance cũ.

Giữ image/tag hoặc digest của bản trước và backup trước migration. Chỉ rollback app khi tương thích schema; V58 đổi ràng buộc refund nên bản cũ không được ghi tiền đồng thời với bản mới. Nếu phải phục hồi DB, dừng traffic, khôi phục cả DB/media và đối soát mọi giao dịch gateway phát sinh sau mốc backup. Không suy ra tiền đã hoàn chỉ từ trạng thái DB được restore.

## Tài liệu tham chiếu

- [VNPay query/refund và chữ ký](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html).
- [Spring Redis cache: TTL và transaction awareness](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html).
- [PostgreSQL backup](https://www.postgresql.org/docs/17/backup.html).
