# Kết quả hoàn thiện Ladux — 25/10/2026

> **Báo cáo lịch sử ngày 25/10.** Số test, cấu hình môi trường và các điều kiện trong tài liệu là snapshot của đợt đó. Trạng thái mới nhất được tổng hợp tại [Project status](PROJECT_STATUS.md); hướng dẫn đang dùng nằm ở [Production runbook](PRODUCTION_RUNBOOK.md).

Đây là báo cáo cho workspace hiện tại, không phải chứng nhận 10/10 hoặc bằng chứng đã triển khai lên Internet. Các thay đổi VNPay/header/Wishlist có sẵn trước audit được giữ lại. Không sửa `.env` thật, database đang dùng hoặc migration đã áp dụng; không tạo commit/deploy công khai.

## Lỗi đã xử lý

| Phần | Vấn đề | Hành vi sau sửa |
| --- | --- | --- |
| Payment | Admin đổi provider/ghi SUCCESS/REFUNDED tùy ý | VNPay chỉ nhận kết quả đã xác minh; COD cần chứng từ và đơn đang giao; refund qua quy trình riêng |
| Order | Xác nhận/xuất hàng VNPay chưa trả; COD giao thành công trước thu tiền | Kiểm tra điều kiện thanh toán ở backend trong transaction |
| Checkout | Tổng tiền hiển thị có thể khác tổng live | Frontend gửi `expectedAmount`; sai tổng thì rollback và yêu cầu kiểm tra lại |
| Inventory | Cộng kho từ entity cũ, ghi đè khi catalog edit, khóa SKU không thống nhất | Update nguyên tử có giới hạn số nguyên, refresh trong lock, optimistic version và thứ tự SKU ổn định |
| Inventory/admin | Sửa thẳng số lượng trong form SKU; tắt SKU có thể xóa stock | SKU hiện hữu đổi stock qua nghiệp vụ điều chỉnh; ngừng bán giữ số lượng thật |
| Procurement | Đổi thẳng trạng thái nhận hàng, dòng nhận lặp/âm | Trạng thái nhận chỉ qua nghiệp vụ nhận hàng; kiểm tra dòng và khóa có thứ tự |
| Refund | Guard một lần theo order chặn hoàn khi trả hai lần; thiếu công cụ xử lý UNKNOWN | Guard theo payment, trang review, xác nhận bằng chứng, hoàn payment thừa giữ giao hàng hợp lệ |
| Loyalty | Từ chối trả hàng cộng điểm lại; cập nhật đồng thời mất điểm; refund chưa trừ | Chỉ cộng khi SHIPPED → DELIVERED, cập nhật nguyên tử, hoàn tiền đơn trả trừ điểm/chi tiêu và invalidation cache |
| Catalog | Chỉ tải page đầu 50 dòng; lọc RAM/ROM có thể khớp hai SKU khác nhau; deep link ngoài danh sách lỗi | Filter/sort/page ở server, thông số cùng một SKU, detail theo ID, facets toàn catalog, ẩn product/SKU/category ngừng bán |
| Catalog/create | Bỏ trống số quạt hợp lệ theo DTO nhưng gây 500 khi unbox null | Dùng mặc định 0; kiểm chứng bằng request tối thiểu qua test và HTTP production fixture |
| API/UX | Request chờ vô hạn, preview coupon bỏ mức trần, giá 0 bị xử lý như null | Timeout theo tác vụ; áp mức trần/điều kiện coupon; dùng nullable price đúng cách |
| Session | Response cũ có thể đổ dữ liệu sang tài khoản mới; refresh muộn phục hồi memory token | Phân phiên request/response, loại response cũ và refresh sau khi phiên đổi; test cả storefront/admin |
| Identity | Hạ quyền/khóa admin cuối, đổi role chưa thu hồi phiên; bcrypt >72 byte thành lỗi hệ thống | Giữ một admin active, revoke khi đổi quyền; deactivate giữ audit; giới hạn byte trước bcrypt |
| Registration | Lưu Customer trước User mới khiến shared primary key chưa tồn tại | Persist User aggregate, cascade Customer rồi tạo Cart trong cùng transaction; có integration/HTTP regression |
| JWT/errors | Token hết hạn giữa các bước validation có thể gây 500; lỗi HTTP/SQL thiếu phân loại | Tiếp tục như anonymous; response validation/404/409/413 rõ, không lộ tên constraint/cột |
| Media | Xóa ảnh trước commit có thể làm mất ảnh khi transaction thất bại | Xóa sau commit, dọn upload mới khi rollback, đóng input stream |
| Deploy | Compose chỉ có backend; template thiếu biến/URL refund sai; chưa có bootstrap | Stack frontend/API/HTTPS đầy đủ, preflight, bootstrap admin và readiness |
| Cache/ops | Prod cache không TTL, invalidation trước commit, thiếu registry metrics | TTL/transaction-aware cache, key có sort, namespace mới, Prometheus bảo vệ ADMIN, request ID |
| Dependency | Npm có 8 package cảnh báo; OSV có 30 package backend cảnh báo | Vá cùng dòng tương thích; Spring Boot 4.0.9, Spring AI 2.0.2, Jackson 2.21.8/3.1.8; bỏ advisor milestone không dùng |

## Kiểm chứng

Kết quả tại workspace ngày 25/10/2026. Log chi tiết nằm trong `output/` bị Git ignore; không phải kết quả CI từ xa.

| Gate | Kết quả thực tế | Bằng chứng trong workspace |
| --- | --- | --- |
| Backend Java 21 | `clean verify` đạt **212 tests**, 0 failures/errors/skipped; PostgreSQL 17 + Redis thật qua Testcontainers | `output/backend-verification.log`, `backend/target/surefire-reports/` |
| Frontend | TypeScript đạt; **44 tests / 11 files** đạt; Vite production build đạt | `output/frontend-verification.log` |
| Preflight regression | **6/6 tests** đạt, gồm placeholder/secret yếu/URL refund sai/sandbox opt-in | `node --test scripts/preflight.test.mjs` |
| Dependency frontend | `npm audit`: **0 vulnerabilities**, cả dependency phát triển | `output/frontend-audit.json` |
| Dependency backend | OSV: **259 runtime Maven packages**, **0 packages có advisory**, kiểm tra lúc 21:30 giờ Bangkok | `output/backend-osv-audit.json`, `scripts/audit-maven.mjs` |
| Production build/start | Image backend Java 21 và frontend build được; app/frontend/PostgreSQL/Redis healthy; profile `prod`, Flyway đến V61, bootstrap admin | `output/production-image-build-final.log`, `output/production-startup-final.log` |
| HTTP nghiệp vụ | **32 bước** đạt: readiness, 401/403, metrics ADMIN, chữ ký IPN thiếu, bảo vệ admin cuối, tạo product, đăng ký, rollback tổng tiền sai, COD, giao/trả/hoàn tiền và loyalty/stock/cache | `output/production-http-smoke.log`, `scripts/production-http-smoke.mjs` |
| HTTP frontend | **7 kiểm tra** đạt: SPA/deep link/admin route, entry no-cache, hashed JS immutable, asset thiếu 404, robots và sitemap production | `output/production-web-smoke.log` |
| Proxy/server config | Compose config, Caddy validate và `nginx -t` đạt; chưa cấp/chứng thực TLS cho hostname thật | `output/caddy-validation.log` |
| Backup/restore | Backup DB + uploads với checksum đạt; restore vào PostgreSQL tạm đạt: 1 order, 1 payment, 2 products, migration V61; đọc lại media fixture đúng nội dung; app healthy sau backup | `output/backup-smoke.log`, `output/restore-smoke.log`, `output/production-backups/20261025T144752Z/` |
| `.env.production` đang có | **Chưa đạt** preflight; giữ nguyên file thật | Xem các mục cấu hình dưới đây |

Test tích hợp bao gồm tranh chấp SKU cuối, hủy song song, hoàn tiền lặp/khôi phục, migration mới/nâng cấp và rollback toàn bộ checkout. HTTP smoke chạy trong project Docker `ladux-audit-smoke` riêng, không mở host ports và dùng JWT fixture: nó kiểm tra authorization/nghiệp vụ, không chứng minh login/CAPTCHA/OAuth/SMTP/VNPay thật. Backup/restore chỉ dùng dữ liệu giả; chưa thay thế diễn tập với bản sao database thực tế. Đã dọn container/volume của riêng project fixture sau kiểm chứng.

## Đồng bộ GitHub trước khi push

Khi chuẩn bị push, đã đồng bộ `origin/main` tới merge PR #10 (`d1322a5`). Giữ ràng buộc provider bất biến, VNPay chỉ thành công từ kết quả xác minh và từ chối MoMo chưa tích hợp gateway; không dùng hành vi tự ghi SUCCESS từ yêu cầu client. Giữ đủ 6 tình huống test mới từ upstream, cập nhật kỳ vọng theo các ràng buộc này và chạy lại Java 21 `clean verify`: 212 tests đạt, không bỏ qua. Source ứng dụng sau giải quyết xung đột giữ đúng phiên bản đã chạy HTTP production fixture.

## Những mục đang chặn cấu hình triển khai

Preflight của `.env.production` hiện có phát hiện thiếu `LADUX_WEB_DOMAIN`, `LADUX_API_DOMAIN`, `CADDY_EMAIL`, `CAPTCHA_SECRET`, `VITE_RECAPTCHA_SITE_KEY`, `VNPAY_QUERY_URL`; còn URL VNPay sandbox/return chưa HTTPS, callback OAuth/CORS chưa khớp storefront; JWT chưa đạt Base64 ≥32 byte và mật khẩu DB dưới 16 ký tự. Không in/đưa giá trị secret vào báo cáo và không tự thay credential thật. Điền theo `.env.production.example` rồi chạy lại preflight.

Vì các mục này còn thiếu, hiện chưa thể coi việc chạy lệnh deploy là đã đủ điều kiện mở bán. Phần code và gate local đã hoàn tất; các kiểm chứng tích hợp thật bên dưới vẫn cần hoàn thành trên môi trường triển khai.

## Các điều kiện trước mở bán thật

1. Điền `.env.production` đúng domain/credential và vượt preflight; cấu hình DNS/TLS cùng callback trong tài khoản nhà cung cấp.
2. Kiểm chứng Google OAuth, CAPTCHA, OTP email, VNPay merchant/IPN/query/refund trên môi trường thật. HTTP smoke dùng JWT fixture, không thay thế các bước này.
3. Diễn tập migration/khôi phục trên bản sao dữ liệu thực tế; chuyển media cũ nếu đang bind mount; kiểm tra tài khoản demo.
4. Đặt backup ngoài host, retention, alert và người xử lý payment/refund. Đo tải trên máy triển khai trước khi cam kết SLO/số người đồng thời.

Chưa kiểm chứng tải lớn, Core Web Vitals trên thiết bị thật, failover hoặc giao dịch tiền thật; chưa scan toàn bộ OS package trong image. Chức năng AI fallback vẫn hoạt động khi nhà cung cấp không đáp ứng; không dùng fallback để tuyên bố LLM thật đã được kiểm thử.

Xem [kế hoạch đã thực hiện](PRODUCTION_PLAN.md) và [runbook triển khai](PRODUCTION_RUNBOOK.md).
