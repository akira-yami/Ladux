# Tài liệu Ladux

Điểm bắt đầu cho source snapshot đóng băng tính năng ngày **27/10/2026**. [README dự án](../README.md) giới thiệu sản phẩm; các tài liệu dưới đây giải thích cách chạy, thiết kế và phạm vi đã kiểm chứng.

## Đọc theo mục đích

| Bạn muốn | Tài liệu |
| --- | --- |
| Chạy từ một bản clone mới | [Local setup](LOCAL_SETUP.md) |
| Hiểu transaction, concurrency và các quyết định thiết kế | [Engineering notes](ENGINEERING.md) |
| Tìm module, tầng và vị trí code | [Kiến trúc backend](../backend/docs/architecture/README.md) |
| Làm việc với giao diện, API client và phiên đăng nhập | [Frontend README](../frontend/README.md) |
| Hiểu Smart Search, ranking, compare và contextual chat | [SmartFit guide](SMARTFIT_GUIDE.md) |
| Hiểu thanh toán, IPN, query và refund | [VNPay flow](VNPAY_FLOW.md) |
| Cấu hình merchant, callback và xử lý lỗi gateway | [VNPay setup](VNPAY_SETUP.md) |
| Xem test/CI, giới hạn và trạng thái đóng băng | [Project status](PROJECT_STATUS.md) |
| Chuẩn bị triển khai, backup, restore hoặc rollback | [Production runbook](PRODUCTION_RUNBOOK.md) |
| Kiểm tra schema | [Flyway migrations](../backend/src/main/resources/db/migration), [dev/test seed](../backend/src/main/resources/db/devdata), [ERD tham khảo](../database/ladux_erd.png) |

## Nguồn xác định hành vi

1. Source và các test tương ứng xác định hành vi đã triển khai.
2. [CI](../.github/workflows/ci.yml) xác định gate tự động; [Project status](PROJECT_STATUS.md) ghi snapshot kiểm chứng và giới hạn.
3. Migration mới bổ sung schema; không sửa migration đã áp dụng để khớp tài liệu.
4. Các báo cáo có ngày là bằng chứng của thời điểm được ghi, không phải trạng thái runtime hiện tại.

Tài liệu dùng cấu hình giả và tên biến. Credential thật nằm trong file môi trường bị Git ignore; không sao chép chúng vào README, ảnh demo hoặc issue.

## Báo cáo và quyết định lịch sử

Giữ lại để giải thích quá trình phát triển. Số test, lỗi đang mở và cấu hình trong các snapshot có thể đã thay đổi; đọc [trạng thái hiện tại](PROJECT_STATUS.md) trước.

| Tài liệu | Vai trò |
| --- | --- |
| [Production audit — 25/10](PRODUCTION_AUDIT.md) | Các lỗi tiền/kho/phiên đã xử lý và smoke test fixture |
| [Production plan — 25/10](PRODUCTION_PLAN.md) | Phạm vi và tiêu chí của đợt gia cố |
| [Readiness audit — 27/10](production-readiness-2026-10-27.md) | Phát hiện ban đầu; hai lỗi CI đã được sửa sau audit |
| [Readiness audit — 11/10](production-readiness-2026-10-11.md) | Snapshot sớm của hệ thống và khoảng trống lúc đó |
| [Hardening — 06/10](../backend/docs/production-hardening-2026-10.md) | Thiết kế/điều kiện migration của đợt cũ; guard refund sau đó đã đổi sang từng payment |
| [Gỡ MFA và OTP điện thoại](authentication-removal.md) | Quyết định thu hẹp xác thực và migration V52 |
| [Kế hoạch SmartFit ban đầu](LADUX_SMARTFIT_IMPROVEMENTS.md) | Yêu cầu và checklist lịch sử; các ý tưởng P2/P3 chưa phải tính năng đã có |
| [Frontend performance](frontend-performance.md) | Phép đo cuộn/hover local và cách tái hiện |
| [V49 recovery](../backend/docs/retired-v49-recovery.md) | Runbook chỉ dành database đã có dấu vết migration bị rút |

## Khi quay lại trước phỏng vấn

- Chạy theo local setup, thực hiện một luồng COD và một luồng hủy/hoàn kho trên dữ liệu demo.
- Đọc checkout, settlement và refund cùng test của từng phần.
- Giải thích vì sao dùng chung transaction, cách xử lý callback lặp và ý nghĩa của kết quả gateway chưa rõ.
- Đối chiếu lời giới thiệu trong CV với [phạm vi và bằng chứng thực tế](PROJECT_STATUS.md).
