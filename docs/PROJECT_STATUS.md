# Trạng thái snapshot portfolio

**Chốt phạm vi tính năng: 27/10/2026.** Ladux được giữ làm dự án cá nhân/portfolio; phát triển tính năng mới tạm dừng. Tài liệu này mô tả phiên bản được kiểm chứng, không mô tả một dịch vụ production đang vận hành.

## Bằng chứng hiện có

Source snapshot trước lần hoàn thiện tài liệu: `596d4ca659578e2cc69c32cfb31bf51ab1149a39` trên `main`. [CI tương ứng](https://github.com/phananhkhang/Ladux/actions/runs/40384064685) đạt cả backend và frontend; [workflow](../.github/workflows/ci.yml) xác định các gate thực tế.

| Kiểm tra | Kết quả tại snapshot |
| --- | --- |
| Backend Java 21 `clean verify` | **227 tests**, 0 failures/errors/skipped |
| Frontend `npm run check` | TypeScript đạt, **57 tests / 14 files**, production build đạt |
| Preflight và VNPay diagnostics | **13 tests đạt** |
| Frontend dependency audit | npm audit đạt, không phát hiện vulnerability tại thời điểm chạy |
| Backend dependency audit | Gate OSV đạt; lượt kiểm tra local ngày 27/10: **259 runtime packages, 0 packages có advisory** |

Hai lỗi test được phát hiện trong audit 27/10 đã được sửa và có trong commit `b670ca3`: test upgrade dùng số migration còn chờ thay vì cố định 7; test discovery tạo dữ liệu legacy rồi khôi phục chính CHECK của production trước khi kiểm tra. Test vẫn chứng minh giá âm mới bị từ chối.

Các con số là snapshot, không phải mục tiêu bất biến. Không đổi assertion chỉ để khớp số test và không coi dependency audit là chứng nhận không có lỗ hổng. Nó không scan OS package trong image.

## Phạm vi đã triển khai

- Storefront/admin, catalog theo SKU, filter/sort/page server-side, cart/coupon, COD và VNPay Sandbox.
- State machine đơn, hết hạn, hoàn kho/coupon, giao/trả và hoàn tiền có đối soát.
- Procurement/nhận hàng từng phần, stock ledger và kiểm soát cập nhật tồn kho.
- JWT/refresh rotation, Google OAuth, OTP email, phân quyền và distributed rate limiting.
- Smart Search, SmartFit, Smart Compare và contextual chat với thẻ catalog xác thực.
- Compose local/production, Caddy/Nginx, readiness, metrics, preflight, bootstrap admin và script backup/restore.

## Phân biệt các lớp kiểm chứng

**Test tự động:** unit/integration/controller tests với PostgreSQL/Redis Testcontainers, ArchUnit và Vitest. Chúng xác minh logic với fixture/mock; chưa phải browser E2E toàn bộ hành trình và nhà cung cấp thật.

**Production fixture ngày 25/10:** báo cáo [audit](PRODUCTION_AUDIT.md) ghi nhận 32 bước HTTP nghiệp vụ, 7 kiểm tra web, startup/build và backup DB/media + restore tạm. Stack riêng dùng JWT/credential giả; kết quả đó không chứng minh CAPTCHA, OAuth, SMTP hay chuyển tiền thực.

**VNPay/Google:** kiểm tra ngày 27/10 đã mở được trang chọn phương thức Sandbox của merchant được cấu hình và trang đăng nhập Google sau khi sửa callback. Chưa có bằng chứng giao dịch Sandbox hoàn tất/IPN/refund thật hoặc Google login hoàn tất bằng tài khoản trong các kiểm tra đó. Không đưa merchant/secret cụ thể vào tài liệu công khai.

**Giao diện/hiệu năng:** ảnh SmartFit và walkthrough là snapshot phát triển; [bài đo cuộn/hover](frontend-performance.md) dùng Chrome local với kịch bản cụ thể. Chưa có số đo tải API/SLO hoặc Core Web Vitals trên một website public đại diện.

## Giới hạn có chủ đích và trade-off

| Phần | Giới hạn hiện tại |
| --- | --- |
| SmartFit | Tối đa 500 SKU ứng viên trước bước parse RAM/SSD; điểm heuristic, chưa tối ưu toàn catalog hoặc benchmark phần cứng |
| Thông số | ROM không xác nhận SSD thì giữ thiếu dữ liệu; pin không suy ra thời lượng |
| Thanh toán | COD/VNPay được hỗ trợ; không công bố MoMo đã tích hợp; refund toàn bộ từng payment, chưa hỗ trợ partial refund |
| Gateway recovery | Worker query xét payment trong 24 giờ gần nhất; giao dịch ngoài phạm vi cần đối soát chủ động |
| AI | Chưa xác nhận toàn bộ chất lượng/luồng provider thật; embedding/index-all còn đồng bộ và cần batch khi catalog lớn |
| Cache/rate limit | Redis dùng chung, `allkeys-lru` có thể loại bucket; invalidation toàn cache cần đo/cải thiện khi tăng tải |
| Media/availability | File upload trên một host/volume; chưa có bằng chứng multi-host media, failover hoặc HA |
| Backup | Script cần dừng app để chụp DB/media nhất quán; lịch chạy, off-host storage, retention và alert cần cấu hình tại nơi triển khai |

## Điều kiện trước mở bán thật

1. Domain/DNS/HTTPS, CAPTCHA và callback/CORS vượt preflight, khớp cấu hình nhà cung cấp.
2. Credential/merchant đúng môi trường; smoke login/refresh/logout, Google, OTP và VNPay IPN/query/refund trên môi trường triển khai.
3. Migration và khôi phục trên bản sao dữ liệu đại diện, gồm media và đối soát tiền phát sinh sau backup.
4. Backup ngoài host, retention, cảnh báo đã thử và người phụ trách payment/refund.
5. Đo tải trên hạ tầng dự kiến trước khi cam kết p95/p99, throughput hoặc số người đồng thời.

Đây là các điều kiện vận hành, không phải danh sách tính năng cần mở rộng để hoàn tất portfolio. Khi quay lại, dùng [runbook](PRODUCTION_RUNBOOK.md) và kiểm tra lại CI của đúng commit sẽ phát hành.
