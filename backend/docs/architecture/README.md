# Kiến trúc backend Ladux

**Một Spring Boot application chia module theo nghiệp vụ**, dùng chung PostgreSQL và Redis. Trong mỗi module, luồng chính là `Controller → Service → Repository`; mapper chuyển entity thành DTO trước khi trả HTTP.

Thiết kế ưu tiên transaction rõ cho order/kho/coupon/payment và một stack có thể chạy local. Các module chưa phải service triển khai độc lập. [Engineering notes](../../../docs/ENGINEERING.md) giải thích các quyết định và trade-off; [trạng thái snapshot](../../../docs/PROJECT_STATUS.md) ghi phạm vi kiểm chứng.

## Bản đồ module

Tất cả nằm trong `backend/src/main/java/org/akira/ladux/`:

| Module | Sở hữu |
| --- | --- |
| `identity` | User/role, login, JWT/refresh, Google OAuth, OTP email, CAPTCHA, rate limit và bootstrap admin |
| `customer` | Hồ sơ, địa chỉ, điểm/hạng và thông tin liên hệ |
| `catalog` | Product/SKU, brand/category/color, ảnh, reviews/wishlist, tìm kiếm và catalog query |
| `ordering` | Cart, checkout, order/item/history, state machine, hủy/trả và expiration |
| `payment` | Payment attempts, VNPay URL/IPN/query, settlement, refund và đối soát |
| `inventory` | Reserve/release stock, điều chỉnh và stock ledger |
| `procurement` | Supplier, purchase order và nhận hàng từng phần |
| `promotion` | Coupon, kiểm tra điều kiện, dùng/hoàn lượt và giới hạn theo khách |
| `notification` | Thông báo và liên hệ |
| `assistant` | SmartFit, contextual chat, embedding và product documents |
| `shared` | Cấu hình chung, lỗi HTTP, pagination, file storage và tiện ích |

`assistant` dùng catalog/VectorStore nên không có entity/repository nghiệp vụ riêng. `LaduxApplication` nằm ở package gốc để quét các module.

## Cấu trúc bên trong một module

Ví dụ `catalog/` hiện có:

```text
catalog/
├── controller/    # ProductController, DiscoveryController, ...
├── service/       # CatalogQueryService, ProductService, ...
│   └── impl/
├── repository/
├── mapper/
├── dto/
│   ├── request/
│   └── response/
├── entity/
└── util/
```

Module có thể thêm `enums`, `config`, `security`, `event`, `listener`, `scheduling` và `service.result` khi có code cần chúng. Không tạo layer/interface rỗng chỉ để khớp một sơ đồ kiến trúc.

| Thành phần | Trách nhiệm và giới hạn |
| --- | --- |
| Controller | HTTP/validation/principal; gọi service; không gọi repository/JPA/JDBC |
| Service | Nghiệp vụ, ownership, transaction, cache và điều phối |
| Repository | JPA/JPQL/native query, locking; có thể trả DTO projection cho truy vấn đọc |
| Mapper | Ánh xạ entity → DTO; không đọc DB/gọi service |
| DTO | Hợp đồng request/response; không chứa entity HTTP hoặc logic persistence |
| Entity | Mapping JPA và quan hệ dữ liệu; không gọi tầng nghiệp vụ |
| `service.result` | Kết quả nội bộ được phép giữ entity trong transaction |
| Shared | Tiện ích chung, không phụ thuộc module nghiệp vụ |

Service gọi trực tiếp repository. Khi một nghiệp vụ cần, service có thể phối hợp service/repository của module khác; entity vẫn có association chéo module. Không có bản sao domain/JPA model hoặc lớp port/adapter chỉ để nối các tầng.

## Các đường đi tiêu biểu

- **Catalog:** `ProductController → CatalogQueryService → ProductRepository`; các mapper tạo DTO. Danh sách dùng page ID rồi fetch summary, detail tải riêng.
- **Checkout:** `OrderServiceImpl` điều phối Inventory/Promotion/Payment trong transaction; các bước ghi kho/coupon/order bắt buộc cùng rollback.
- **Nhận hàng:** Procurement kiểm tra đơn nhập và số lượng; Inventory cập nhật stock/ledger theo thứ tự SKU.
- **Thanh toán:** webhook/query xác minh đầu vào trước `VNPaySettlementService`; order lock trước payment lock.
- **Refund:** lưu attempt, gọi gateway ngoài transaction khóa DB, hoàn tất hoặc giữ review trong transaction riêng.
- **Vai trò:** `RoleController → RoleService → RoleRepository`.

Các luồng này được mô tả sâu hơn cùng code/test tại [Engineering notes](../../../docs/ENGINEERING.md).

## Cache và schema

Spring Cache dùng namespace **`ladux:modules:v2:`**, TTL hữu hạn và cache manager `transactionAware()`. TTL mặc định dev là 10 phút, prod là 5 phút; có thể cấu hình theo môi trường. Key phân trang có sort; một số luồng ghi vẫn invalidate toàn cache.

Đổi package DTO được serialize trong cache cần namespace mới hoặc quy trình dọn đúng prefix. Không dùng `FLUSHALL`/`FLUSHDB` vì Redis còn giữ bucket rate limit và các key khác. Không chạy lẫn nhiều phiên bản có namespace/invalidation không tương thích mà không có kế hoạch chuyển traffic.

Schema do Flyway quản lý tới V62 tại snapshot. Dev/test thêm `db/devdata`; prod chỉ nạp `db/migration`, `ddl-auto=validate` và tắt schema initialization tự động của VectorStore. [ERD](../../../database/ladux_erd.png) hỗ trợ đọc; migration trong source là nguồn xác định schema.

## ArchUnit và integration tests

`ArchitectureRuleTest` kiểm tra module/layer, hướng phụ thuộc controller/service/repository, trách nhiệm DTO/entity/mapper và sự độc lập của `shared`. Fixture cố ý vi phạm phải bị phát hiện; rule không được xanh vì không quét class nào.

Test đặt dưới `src/test/java/org/akira/ladux/<module>/<layer>`. Integration tests khởi động PostgreSQL/pgvector và Redis riêng bằng Testcontainers; không dùng DB local hiện có.

Từ thư mục gốc repository, dùng **JDK 21, Maven 3.9+ và Docker đang chạy**:

```bash
mvn -B -f backend/pom.xml clean verify
```

Xem `backend/target/surefire-reports` cho lần chạy thực tế và [CI](../../../.github/workflows/ci.yml) cho gate từ xa. Không thay invariant nghiệp vụ hoặc bỏ test để làm build xanh.
