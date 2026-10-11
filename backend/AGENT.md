# Backend Ladux — hướng dẫn phát triển

Phạm vi: `backend/`. Kiến trúc: **monolith chia module nghiệp vụ, phân tầng thông thường**.
Đọc [tài liệu kiến trúc](docs/architecture/README.md) khi thay đổi cấu trúc.

## Tổ chức code

- Một ứng dụng Spring Boot, một bản triển khai, dùng chung PostgreSQL và Redis.
- Module: `identity`, `customer`, `catalog`, `ordering`, `payment`, `inventory`,
  `procurement`, `promotion`, `notification`, `assistant`.
- Mỗi module chứa các package cần thiết: `controller`, `service`, `repository`,
  `mapper`, `dto`, `entity`, `enums`; có thể thêm `config`, `security`, `listener`,
  `event`, `util`, `scheduling` theo chức năng thực tế.
- Không dùng cấu trúc Hexagonal/Clean, `api/application/domain/infrastructure`,
  port/adapter, hoặc một lớp use case cho mỗi thao tác.
- Không tạo file/thư mục/interface rỗng cho đủ mẫu. `assistant` không sở hữu bảng riêng.
- `shared` chứa cấu hình, DTO phân trang, lỗi, lưu file và tiện ích dùng chung;
  không phụ thuộc module nghiệp vụ.

## Trách nhiệm các lớp

- Controller xử lý HTTP, validation và principal, sau đó gọi service; không gọi repository/JPA/JDBC.
- Service xử lý nghiệp vụ, phân quyền, transaction và điều phối; gọi repository trực tiếp.
- Repository dùng Spring Data JPA, có thể trả entity hoặc DTO projection cho truy vấn đọc.
- Mapper chuyển entity sang DTO; không truy cập DB, không xử lý transaction.
- DTO HTTP không chứa entity và không có phương thức ánh xạ entity.
- Kết quả nội bộ cần giữ entity nằm trong `service.result`, không được trả ra HTTP.
- Entity JPA đặt trong module sở hữu. Giữ các association hiện có để không thay đổi schema.
- Service có thể dùng service/repository của module liên quan khi nghiệp vụ cần;
  không tạo cổng API hay bản sao entity chỉ để nối các module.
- Interface và `service.impl` dùng khi có ích, không bắt buộc mọi service có interface.

## Bảo toàn hành vi

- Giữ URL, HTTP method/status, JSON, validation và phân quyền khi refactor.
- Giữ transaction, propagation, lock order, cache eviction, lịch chạy và tên ShedLock.
- Checkout, reserve/release stock, coupon và cập nhật order phải giữ rollback nguyên tử.
- Không thay cơ chế JWT, refresh rotation, OTP email, OAuth2 hoặc rate limit khi đổi package.
- Giữ kiểm tra signature/reference/amount và tính lũy đẳng của callback thanh toán.
- Không sửa Flyway migration đã áp dụng; thay schema phải có migration mới.
- Khi đổi package của DTO được cache, đổi namespace Spring Cache; không xóa session,
  rate-limit bucket hoặc toàn bộ Redis.
- Giữ thay đổi chưa commit của người dùng và tránh trộn tính năng mới vào refactor.

## Kiểm tra

- Test đặt trong cùng module/layer với code được kiểm tra.
- ArchUnit bảo vệ vị trí module/layer, controller → service → repository,
  mapper/DTO/entity và tính độc lập của `shared`; không cho phép rule rỗng xanh giả.
- Chạy `./mvnw clean verify` bằng JDK 21 khi hoàn tất.
- Test tích hợp dùng Testcontainers PostgreSQL/Redis, cần Docker đang chạy.
- Báo chính xác test đã chạy, test thất bại hoặc hạ tầng còn thiếu;
  không bỏ test hoặc đổi invariant chỉ để có build xanh.
