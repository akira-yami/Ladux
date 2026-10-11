# Kiến trúc monolith chia module

Ladux là **một ứng dụng Spring Boot duy nhất**, chia mã nguồn theo nghiệp vụ.
Trong mỗi module, luồng chính là **Controller → Service → Repository**;
mapper chịu trách nhiệm chuyển entity sang DTO. Dự án không dùng Hexagonal/Clean,
port/adapter hoặc các lớp use case trung gian.

## Cấu trúc

```text
backend/src/main/java/org/akira/ladux/
├── LaduxApplication.java
├── identity/       # tài khoản, xác thực, OTP email, OAuth2, bảo mật
├── customer/       # hồ sơ, địa chỉ, hạng và điểm khách hàng
├── catalog/        # sản phẩm, biến thể, thương hiệu, danh mục, đánh giá, yêu thích
├── ordering/       # giỏ hàng, đơn hàng, lịch sử, vòng đời đơn
├── payment/        # giao dịch, VNPay, webhook, hoàn tiền
├── inventory/      # giữ/hoàn tồn kho, sổ cái biến động kho
├── procurement/    # nhà cung cấp, đơn nhập, nhận hàng
├── promotion/      # coupon, sử dụng và hoàn lượt khuyến mãi
├── notification/   # thông báo, liên hệ
├── assistant/      # chatbot, embedding và tài liệu sản phẩm
└── shared/         # cấu hình chung, exception, pagination, file storage, tiện ích
```

Ví dụ module `catalog`:

```text
catalog/
├── controller/
│   ├── admin/
│   └── user/
├── service/
│   ├── CatalogQueryService.java
│   ├── ProductService.java
│   └── impl/
├── repository/
├── mapper/
├── dto/
│   ├── request/
│   └── response/
├── entity/
└── util/
```

Các package chỉ tồn tại khi có code thực sự. Module có thể bổ sung `enums`,
`config`, `security`, `event`, `listener`, `scheduling` hoặc `service.result`.
`assistant` dùng dữ liệu Catalog và VectorStore nên không cần entity/repository riêng.
Test nằm dưới `src/test/java/org/akira/ladux/<module>/<layer>`.

## Trách nhiệm và phụ thuộc

| Thành phần | Trách nhiệm |
| --- | --- |
| Controller | HTTP, validation, principal; gọi service và trả DTO |
| Service | Nghiệp vụ, phân quyền, transaction, cache, điều phối |
| Repository | Spring Data JPA, JPQL, locking; trả entity hoặc DTO projection |
| Mapper | Chuyển entity sang DTO, không truy cập DB hoặc gọi service |
| DTO | Request/response và dữ liệu trao đổi, không chứa entity HTTP |
| Entity | Ánh xạ JPA, quan hệ và dữ liệu lưu trữ |
| `service.result` | Kết quả xử lý nội bộ có thể giữ entity trong transaction |
| Shared | Tiện ích/cấu hình dùng chung, không phụ thuộc module nghiệp vụ |

Service gọi trực tiếp repository; có thể gọi service/repository của module khác
khi luồng nghiệp vụ cần. Không bắt buộc API module, không tạo domain model/JPA model
trùng lặp. Các liên kết entity chéo module hiện có được giữ nguyên vì đây là monolith
chung database. Ranh giới module thể hiện nơi sở hữu code và dữ liệu, không phải
các dịch vụ có thể triển khai độc lập.

Các luồng chính:

- Sản phẩm: `ProductController → CatalogQueryService → ProductRepository`,
  dữ liệu ra HTTP đi qua `ProductMapper`. Truy vấn phân trang lấy ID trước, sau đó
  lấy chi tiết và giữ lại thứ tự ID của trang.
- Checkout: `OrderServiceImpl` phối hợp Inventory, Promotion và Payment trong
  transaction hiện có; các bước ghi bắt buộc không được chuyển sang event bất đồng bộ.
- Nhập hàng: Procurement quản lý đơn nhập và nhận hàng; Inventory xử lý stock/ledger.
- Thanh toán: Payment xác minh callback và gọi xử lý vòng đời đơn; controller hoàn tiền
  truyền ID quản trị viên, việc đọc DB nằm ở service.
- Vai trò: `AdminRoleController → RoleService → RoleRepository`.

## Thay đổi từ cấu trúc cũ

- Đã chuyển các package chung `controller`, `service`, `repository`, `dto`, `model`,
  `config`, `event`, `exception`, `utils` về module sở hữu hoặc `shared`.
- Đã bỏ các khung rỗng, `workflow`, thư mục sai tên `recurement`, cùng cấu trúc
  `api/application/domain/infrastructure`.
- Đã bỏ `ProductReadPort`, `ProductRepositoryAdapter`, các repository `SpringData*`
  trùng lặp và các lớp `GetProductUseCase`/`SearchProductsUseCase`;
  nghiệp vụ đọc được gom vào `CatalogQueryService`.
- Đã tách phương thức ánh xạ khỏi response DTO sang các lớp mapper trong module.
- Đã bỏ vòng phụ thuộc giữa hai service refresh token: service xử lý reuse gọi
  repository trực tiếp, giữ propagation `REQUIRES_NEW` hiện có.
- Tài liệu kế hoạch Hexagonal cũ được hợp nhất và thay thế bởi tài liệu này.

Không thay REST URL, cấu trúc JSON, bảng database hoặc lịch sử Flyway khi di chuyển.
Các thay đổi truy vấn sản phẩm đang có trong workspace trước refactor được giữ lại,
bao gồm snapshot sản phẩm/biến thể và thứ tự phân trang.

## Cache và triển khai

Spring Cache có thể lưu tên đầy đủ của DTO trong dữ liệu serialization. Vì DTO đã
đổi package, namespace Redis cache được đổi thành `ladux:modules:v1:` trong
`application.properties`. Lần chạy đầu sẽ tạo lại cache qua truy vấn DB. Tên cache,
key nghiệp vụ và thời gian sống hiện có được giữ nguyên.

Namespace này chỉ áp dụng Spring Cache. Refresh token, OTP email và
rate-limit bucket không bị xóa hoặc đổi key. Không cần chạy `FLUSHDB`/`FLUSHALL`.
Không chạy lẫn phiên bản cũ và mới: cache eviction của hai namespace độc lập;
khi triển khai hoặc rollback, thay toàn bộ instance và làm mới riêng cache của
phiên bản được đưa vào phục vụ để tránh dữ liệu cũ. Không có migration mới cần rollback.

## Kiểm thử kiến trúc và hành vi

`ArchitectureRuleTest` kiểm tra:

1. Mọi class sản xuất nằm trong module/layer hợp lệ; tất cả module phải được quét.
2. Controller/entity/repository nằm đúng package; controller không truy cập repository/JPA/JDBC.
3. Service không phụ thuộc controller; repository không phụ thuộc service/controller/mapper.
4. DTO không chứa entity/service/repository; entity và mapper không gọi các tầng nghiệp vụ.
5. `shared` không phụ thuộc các module nghiệp vụ.
6. Fixture cố ý sai chứng minh rule bắt được repository kể cả khi không có hậu tố `Repository`.

Repository được phép dùng DTO projection cho truy vấn đọc; đây là Spring Data JPA
thông thường. Không áp dụng rule cấm entity association chéo module hoặc ma trận
API của kiến trúc cũ.

```bash
cd backend
./mvnw clean verify
```

Dùng JDK 21 và Docker đang chạy. Integration test sử dụng PostgreSQL có pgvector
và Redis qua Testcontainers; test đơn vị kiểm tra xác thực, thanh toán, mapper,
truy vấn sản phẩm và các hành vi hiện có. Xem báo cáo thực tế trong
`target/surefire-reports`; tài liệu này không thay thế kết quả chạy test.
