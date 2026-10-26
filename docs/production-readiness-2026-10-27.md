# Đánh giá production readiness — 27/10/2026

**Kết luận: NO-GO cho mở bán production ở trạng thái hiện tại.** Ladux có nền tảng nghiệp vụ và cấu hình vận hành đủ tốt để tiếp tục kiểm chứng trên staging, nhưng còn hai gate đang thất bại trực tiếp: bộ test backend và cấu hình production. Ngoài ra, chưa có đủ bằng chứng về tích hợp, khôi phục và tải trên môi trường triển khai thật.

Không quy đổi thành phần trăm readiness hoặc số người dùng đồng thời: các con số đó cần tiêu chí và phép đo cụ thể.

## Phạm vi và nguồn bằng chứng

- Workspace được kiểm tra: commit `cbf26f77de3af89e6c7be8616d5ccdb937714b7f`, tree `18a2bf7d57ea8e2165946681c5a873892feb9fd5`.
- GitHub `main` tại thời điểm đọc: `f024d19b77eee3c067b4e1b9a8fd48df27099346`, cùng tree với workspace. Vì vậy kết quả CI của bản merge này áp dụng cho source được đánh giá.
- Đọc source backend/frontend, cấu hình Spring/proxy/Docker/CI, migration V62, script preflight/backup/restore và báo cáo trước.
- Chạy lại frontend, bộ test cấu hình, preflight trên `.env.production` hiện có, audit npm/OSV và backend Java 21 với Testcontainers PostgreSQL/Redis riêng.
- Không sửa source ứng dụng, migration, secret hoặc dữ liệu nghiệp vụ; không deploy, gửi email hay thực hiện giao dịch gateway.
- Container local đang chạy profile `dev`; tình trạng healthy của nó không chứng minh production đã được triển khai.

## Các điểm chặn phát hành

### P1 — Backend release gate đang đỏ

[CI mới nhất trên main](https://github.com/phananhkhang/Ladux/actions/runs/40378649486) thất bại ở job backend: **227 tests, 1 failure, 1 error, 0 skipped**. Frontend CI đạt. Hai lỗi cũng tái hiện trong lượt chạy Java 21 tại workspace:

1. `backend/src/test/java/org/akira/ladux/architecture/ProductionMigrationSmokeTest.java:78` còn kỳ vọng `migrationsExecuted == 7`; khi đã có V62, thực tế là 8. Migration đã chạy thành công trong tình huống này; assertion bị lỗi thời.
2. `backend/src/test/java/org/akira/ladux/catalog/repository/DiscoveryRepositoryIntegrationTest.java:59` cố ghi `discount_price = -100`, bị CHECK `ck_pv_discount_price_range` của V62 từ chối. Fixture không còn phù hợp với schema mới.

Hai lỗi này chưa chứng minh checkout production hỏng. Tuy nhiên, chúng làm `clean verify` thất bại và khiến bước audit dependency backend trong CI bị bỏ qua, nên chưa có release gate xanh cho bản hiện tại.

**Cần làm:** cập nhật kỳ vọng migration và cách chuẩn bị dữ liệu kiểm thử legacy. Giữ kiểm tra loại giá không hợp lệ; không bỏ test hoặc nới CHECK chỉ để đạt build. Chạy lại toàn bộ Java 21 `clean verify` và CI trên commit phát hành. PostgreSQL vẫn áp CHECK `NOT VALID` cho INSERT/UPDATE mới; cách dựng fixture phải phản ánh đúng thời điểm dữ liệu legacy xuất hiện. [Tài liệu PostgreSQL](https://www.postgresql.org/docs/17/sql-altertable.html).

### P1 — Cấu hình production hiện có không vượt preflight

`node scripts/preflight.mjs --skip-docker` trả exit 1 với **17 thông báo**; không in giá trị secret. Các nhóm lỗi:

- Thiếu `LADUX_WEB_DOMAIN`, `LADUX_API_DOMAIN`, `CADDY_EMAIL`, `CAPTCHA_SECRET`, `VITE_RECAPTCHA_SITE_KEY`, `VNPAY_QUERY_URL`.
- JWT chưa đạt yêu cầu Base64 giải mã được ít nhất 32 byte; mật khẩu database chưa đạt ngưỡng 16 ký tự của preflight.
- VNPay pay/refund vẫn dùng sandbox; return chưa là HTTPS hợp lệ.
- OAuth success redirect, VNPay return/IPN và CORS chưa khớp hostname/route production dự kiến.

**Tác động:** chưa thể coi file cấu hình này là đầu vào dùng được cho stack production đã thiết kế. Code có cơ chế từ chối khởi động khi một số cấu hình bảo mật production không hợp lệ.

**Cần làm:** điền domain và credential đúng theo template, đồng bộ callback ở nhà cung cấp, vượt preflight đầy đủ gồm Compose, rồi kiểm tra DNS/TLS thực tế. Không dùng `--allow-sandbox` để chứng nhận môi trường thanh toán live.

### P1 — Chưa có bằng chứng tích hợp đầy đủ trên môi trường phát hành

Báo cáo 25/10 mô tả HTTP smoke dùng JWT fixture, không thay cho login/CAPTCHA/OAuth/SMTP/VNPay thật. Báo cáo đó cũng chưa chứng minh TLS cho hostname thật. Lượt audit này không thực hiện giao dịch tiền thật hoặc gửi OTP.

**Cần chứng minh trước mở bán:** đăng ký/login/refresh/logout, Google OAuth, CAPTCHA và OTP email qua domain HTTPS; VNPay thành công/thất bại, IPN lặp hoặc mất, query recovery và refund có đối soát. Test backend hiện có cung cấp bằng chứng logic tốt nhưng không xác nhận merchant/callback/credential bên ngoài hoạt động.

AI có fallback; chưa xác nhận LLM thật không nên được ghi là đã đạt. Mức độ chặn phát hành của AI phụ thuộc phạm vi tính năng được công bố.

### P1 — Khôi phục và cảnh báo vận hành chưa được xác minh cho triển khai thật

Repo có script backup DB + uploads, checksum và restore vào PostgreSQL tạm. Báo cáo trước đã diễn tập với dữ liệu fixture. Tuy nhiên, chưa có bằng chứng trong phạm vi audit về:

- lịch backup tự động, bản sao mã hóa ngoài host, retention và cảnh báo job thất bại;
- restore đầy đủ dữ liệu/media đại diện trên hạ tầng thay thế và RPO/RTO đã đo;
- người nhận cảnh báo readiness, 5xx/latency, dung lượng và payment/refund cần xử lý.

Metrics/readiness đã có; đây chưa phải bằng chứng rằng hệ thống thu thập và cảnh báo đang hoạt động. `scripts/production-backup.sh` chủ động dừng app trong lúc backup, nên cần tính thời gian gián đoạn vào kế hoạch vận hành.

**Cần làm:** cấu hình và diễn tập các bước trong `PRODUCTION_RUNBOOK.md`, giữ bằng chứng restore và thử gửi cảnh báo tới đúng người vận hành. Kiểm tra migration V62 trên bản sao dữ liệu thực tế trước nâng cấp; các CHECK `NOT VALID` chưa xác nhận tính hợp lệ của toàn bộ dữ liệu lịch sử.

## Rủi ro cần xử lý hoặc đo trước khi tăng tải

| Mức | Bằng chứng trong code | Tác động và hướng xử lý |
| --- | --- | --- |
| P2 | `backend/docker-compose.prod.yml:141` dùng chung Redis cho cache/rate limit với `allkeys-lru`, 256 MB | Khi chịu áp lực bộ nhớ, key bucket cũng có thể bị loại, làm mất lịch sử giới hạn request. Tách cache và bộ đếm bảo mật hoặc chọn thiết kế không cho cache eviction xóa bộ đếm; đo memory/eviction và khả năng chịu lỗi. Đây là rủi ro theo điều kiện, chưa phải cuộc tấn công đã tái hiện. |
| P2 | `backend/src/main/java/org/akira/ladux/shared/config/RedisCacheConfig.java:19` dùng cache writer mặc định; checkout/payment có nhiều `@CacheEvict(allEntries = true)` | Clear toàn cache có thể quét keyspace và làm tăng cache miss khi ghi nhiều. Chuyển batch clear sang SCAN, thu hẹp invalidation khi có số đo hỗ trợ, kiểm tra Redis latency và tải DB. |
| P2 | `ProductEmbeddingService.java:28` tải toàn catalog và gọi `vectorStore.add` trong transaction | Catalog lớn hoặc dịch vụ embedding chậm có thể giữ connection lâu và tăng bộ nhớ. Chạy theo batch/job, đưa phần gọi external model ra ngoài transaction đọc catalog và giới hạn concurrency. Chỉ ADMIN gọi được endpoint này. |
| Chưa đo | Chưa có load test API trên máy triển khai và dữ liệu đại diện trong bằng chứng được kiểm tra | Chưa thể cam kết concurrency, throughput, p95/p99 hoặc SLO. Bài đo cuộn/hover local có ích nhưng không thay load test backend và Core Web Vitals trên môi trường thật. |

Nguồn đối chiếu: [Redis eviction](https://redis.io/docs/latest/develop/reference/eviction/) mô tả `allkeys-lru` áp dụng trên toàn keyspace; [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html) mô tả cache clear mặc định dùng KEYS/DEL và lựa chọn SCAN. Các tác động tới Ladux ở bảng trên là suy luận từ cấu hình và đường gọi trong repo, chưa phải kết quả stress test.

## Những phần đã có nền tảng tốt

- Checkout tính giá/tổng tiền ở backend, kiểm tra `expectedAmount`, và gom nghiệp vụ tồn kho/coupon/order trong transaction.
- Tồn kho dùng cập nhật nguyên tử, CHECK không âm, thứ tự khóa SKU và optimistic version; có kiểm thử tranh chấp và rollback.
- VNPay kiểm tra chữ ký/merchant/số tiền, xử lý callback lặp, phân biệt Return URL với settlement, có query recovery và quy trình đối soát/refund.
- JWT ngắn hạn, refresh token băm/xoay vòng, cookie HttpOnly/Secure, token version và phân quyền admin/customer.
- Production tách network, chỉ publish Caddy, backend chạy non-root, có readiness, shutdown có thời gian chờ, Flyway validate, bootstrap admin và cache TTL sau commit.
- CI có backend integration tests, frontend typecheck/test/build, cấu hình regression tests và dependency audit.

Các kiểm soát này giảm rủi ro rõ rệt; đây không phải chứng nhận đã loại hết lỗi bảo mật/nghiệp vụ.

## Kết quả kiểm chứng ngày 27/10

| Kiểm tra | Kết quả | Bằng chứng |
| --- | --- | --- |
| Frontend `npm run check` | TypeScript đạt; **51 tests / 13 files đạt**; production build đạt | `output/readiness-frontend-20261027-retry.log` |
| Preflight/VNPay diagnostics regression | **13/13 tests đạt** | `node --test scripts/preflight.test.mjs scripts/check-vnpay.test.mjs` |
| Preflight `.env.production` hiện có | **Không đạt**, 17 thông báo | `output/readiness-preflight-20261027.log` |
| Backend Java 21 `clean verify` | **227 tests, 1 failure, 1 error, 0 skipped**; build thất bại, khớp hai lỗi của CI | `output/readiness-backend-20261027.log` |
| GitHub CI `main` | Backend **227 tests, 1 failure, 1 error, 0 skipped**; frontend đạt | Run `40378649486` |
| npm audit hiện tại | **0 vulnerabilities**, gồm dependency phát triển | `output/readiness-npm-audit-20261027.json` |
| OSV trên dependency Maven vừa resolve | **259 runtime packages, 0 packages có advisory** | `output/readiness-backend-osv-20261027.json` |

Audit dependency là snapshot tại thời điểm chạy; không scan OS package trong image và không chứng minh ứng dụng không có lỗ hổng.

Lần chạy frontend đầu bị Windows sandbox chặn thao tác rename file tạm trước khi thực thi test. Đặt TEMP/TMP vào `.test-tmp` trong workspace rồi chạy lại toàn bộ đã đạt; không thay source hoặc bỏ test. Các log trong `output/` bị Git ignore.

## Điều kiện chuyển sang GO

1. Sửa hai lỗi fixture/assertion và có CI xanh trên đúng commit sẽ phát hành.
2. Cấu hình production vượt preflight + Compose; DNS/TLS và callback đúng domain.
3. Hoàn tất smoke luồng khách/admin và tích hợp nhà cung cấp trên staging, rồi xác minh cấu hình/giao dịch live theo kế hoạch mở bán.
4. Diễn tập nâng cấp/restore với dữ liệu đại diện; có backup ngoài host và cảnh báo đã thử hoạt động.
5. Chốt tải mục tiêu, đo trên hạ tầng triển khai và đặt giới hạn vận hành theo kết quả.

Sau các gate này có thể đánh giá lại để mở pilot có giới hạn. Hiện tại phù hợp tiếp tục staging/UAT; chưa đủ bằng chứng để mở bán production hoặc hứa hẹn quy mô người dùng.
