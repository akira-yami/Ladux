# Kế hoạch hoàn thiện Ladux trước triển khai

> **Kế hoạch lịch sử ngày 25/10, giữ để đối chiếu phạm vi đã làm.** Dự án hiện đóng băng tính năng cho portfolio; xem [Project status](PROJECT_STATUS.md) và [runbook hiện tại](PRODUCTION_RUNBOOK.md).

Ngày: 25/10/2026. Phạm vi: workspace hiện tại, bao gồm các sửa đổi VNPay và giao diện đã có trước lần làm việc này. Không sửa migration đã áp dụng, không thay dữ liệu/secret đang dùng và không tự triển khai lên máy chủ thật.

## Tiêu chí hoàn thành

- Giá và tồn kho do backend quyết định; mọi thay đổi tiền/kho có transaction, kiểm tra trạng thái và dấu vết.
- Không ghi nhận VNPay thành công bằng thao tác sửa payment; không xuất hàng khi chưa đủ điều kiện thanh toán.
- Giữ tồn kho đúng khi checkout, nhập kho, hủy và nhận hàng trả chạy đồng thời; khóa SKU theo thứ tự ổn định.
- Refund chưa rõ kết quả phải có đường xem và giải quyết bằng chứng, không tự gửi tiền lại.
- Một cấu hình production phục vụ đủ SPA, API, uploads, HTTPS và healthcheck; không mở cổng DB/Redis ra Internet.
- Có bootstrap admin, kiểm tra môi trường, backup/restore và hướng dẫn deploy/rollback cụ thể.
- Chạy clean verify Java 21 với PostgreSQL/Redis Testcontainers, frontend typecheck/test/build và dependency audit. Mọi kiểm tra chưa chạy hoặc thất bại đều được ghi rõ.

## Thứ tự thực hiện

| Giai đoạn | Công việc | Bằng chứng cần có |
| --- | --- | --- |
| 1 — Khảo sát | Đọc kiến trúc, cấu hình, migration, kiểm thử; kiểm tra checkout, identity, payment, inventory, procurement, catalog và frontend | Danh sách lỗi có đường tái hiện; giữ thay đổi sẵn có |
| 2 — Tiền và kho | Siết manual payment, trạng thái xuất/giao/hủy, hoàn tiền, ghi kho nguyên tử, khóa SKU có thứ tự | Regression test về trạng thái, số tiền, stock/ledger, request lặp và tranh chấp |
| 3 — Xác thực và API | Kiểm tra quyền, cookie, token, bootstrap, validation, timeout và lỗi HTTP | Test xác thực/phân quyền, dữ liệu lỗi và cấu hình production |
| 4 — Frontend | Sửa preview giá/coupon, giỏ hàng/phiên, API timeout, catalog/phân trang và lỗi request | TypeScript, Vitest, production build |
| 5 — Triển khai | SPA + Caddy + app, readiness, cache TTL, biến môi trường, preflight, backup/restore, CI | Compose config, build/start container, smoke HTTP nếu hạ tầng cho phép |
| 6 — Đóng kiểm chứng | Clean build, test tích hợp, dependency audit, tổng hợp kết quả và phần phải xác minh trên hạ tầng thật | Báo cáo release kèm lệnh triển khai và giới hạn còn lại |

## Các phát hiện ban đầu cần sửa

1. `PaymentServiceImpl.updatePayment` cho đổi provider và tự đặt các trạng thái tài chính không qua gateway/refund.
2. `OrderStateMachineImpl` cho xác nhận VNPay chưa thanh toán; COD giao thành công chưa có quy tắc ghi nhận thu tiền rõ.
3. `StockMovementServiceImpl.recordMovement` cộng stock từ entity đã load, không tự bảo đảm khóa/dữ liệu mới; nhập/trả nhiều SKU không có thứ tự khóa thống nhất.
4. `RefundWorkflowService` giữ attempt chưa rõ kết quả nhưng thiếu API xử lý đối soát; cần kiểm soát hoàn tiền khi có nhiều payment thành công.
5. Frontend gọi API không có timeout; preview coupon tự tính và có thể vượt subtotal; catalog store yêu cầu 1000 dòng trong khi backend giới hạn 50.
6. Production Compose thiếu frontend; `.env.production.example` thiếu Caddy/CAPTCHA và có URL refund trỏ nhầm về ứng dụng.
7. Production Redis cache chưa có TTL rõ; cần đồng bộ invalidation với commit.
8. Chưa có quy trình bootstrap admin, preflight và diễn tập backup/restore tái lập.

Các mục trong báo cáo audit cũ phải đối chiếu với source hiện tại; không mặc định chúng vẫn còn lỗi. Chẳng hạn truy vấn trừ kho hiện đã kiểm tra trạng thái active của SKU và product.

## Kết quả thực hiện

Các giai đoạn 1–6 đã hoàn tất trong workspace. Bản sửa bao gồm gate CI, migration bổ sung V58–V61, công cụ preflight/bootstrap, stack production đầy đủ và backup/restore. Giữ lại các thay đổi VNPay/giao diện có sẵn; không sửa file môi trường thật hoặc deploy công khai.

Kiểm chứng cuối: Java 21 `clean verify` **212 tests** không bỏ qua; frontend typecheck, **44 tests** và build; **6 tests** preflight; **32 bước HTTP nghiệp vụ**, **7 kiểm tra HTTP frontend**; backup DB/media và restore PostgreSQL tạm đạt. Npm audit 0 vulnerabilities; OSV 259 Maven packages, 0 packages có advisory tại thời điểm kiểm tra. Chi tiết và giới hạn nằm trong [báo cáo hoàn thiện](PRODUCTION_AUDIT.md).

Các mục còn lại thuộc release trên môi trường thật: sửa `.env.production` cho vượt preflight; cấu hình DNS/TLS và nhà cung cấp; diễn tập dữ liệu/media hiện hữu, kiểm chứng thanh toán/email/OAuth/CAPTCHA thật; cấu hình backup ngoài host và alert. Thực hiện theo [runbook](PRODUCTION_RUNBOOK.md). Không quy các bước chưa xác minh thành kết quả đã đạt.

## Các điều kiện thuộc môi trường thật

Code/test local không chứng minh DNS/TLS, merchant VNPay live, SMTP, Google OAuth, CAPTCHA, alert, tải dự kiến và backup ngoài máy chủ đang hoạt động. Runbook phải nêu chính xác bước xác minh các tích hợp này; kết quả chưa có không được ghi là đã đạt hay quy đổi thành điểm 10/10.
