# Khôi phục database dev đã chạy V49 bị xóa khỏi source

Ứng dụng build được nhưng dừng ở Flyway với `Detected applied migration not resolved locally: 49`.
V49 cũ xóa/tạo lại bảng danh mục và đã được loại khỏi source có chủ đích. Không khôi phục SQL đó,
không xóa volume, không tắt validation và không repair tự động khi ứng dụng khởi động.

`RetiredMigrationRepair` là lệnh vận hành riêng cho profile `dev`. Lệnh chỉ chấp nhận lỗi thiếu V49,
tên script `V49__delete_parent_in_category.sql`, checksum `-1702186671` đã đối chiếu với lịch sử Git.
Nếu còn lỗi checksum, migration thiếu khác hoặc dấu vết V49 khác, lệnh dừng trước khi ghi dữ liệu.
Flyway repair chỉ ghi nhận migration đã bị xóa; không chạy lại V49 hay các migration đang pending.

Từ thư mục gốc dự án, backup database hiện tại trước. Ví dụ PowerShell (giữ file backup riêng,
không commit; đổi tên nếu chạy lại):

```powershell
docker compose stop frontend app
New-Item -ItemType Directory -Force backups | Out-Null
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d ladux -Fc -f /tmp/ladux-before-v49.dump'
docker compose cp postgres:/tmp/ladux-before-v49.dump ./backups/ladux-before-v49.dump
```

Chỉ tiếp tục sau khi `pg_dump` và sao chép backup thành công. Với dữ liệu cần giữ, nên restore
backup sang database tạm rồi thử lệnh repair và khởi động app trên bản sao trước.

Build image và chạy lệnh repair riêng (không tự chạy khi `compose up`):

```powershell
docker compose build app
docker compose run --rm --no-deps --entrypoint java app `
  '-Dloader.main=org.akira.ladux.shared.util.RetiredMigrationRepair' `
  -cp /app/ladux.jar org.springframework.boot.loader.launch.PropertiesLauncher `
  --repair-retired-v49
docker compose up -d app frontend
docker compose ps
```

Giữ nguyên các biến DB của Compose; không đưa mật khẩu vào câu lệnh hoặc commit backup.
Kiểm tra app/frontend healthy, lịch sử Flyway validate thành công và API public trả 200.
Việc đánh dấu migration đã xóa không phục hồi dữ liệu danh mục mà V49 từng ghi đè; cần backup cũ
hoặc nguồn dữ liệu riêng nếu cần phục hồi nội dung đó.

## Kiểm chứng trên database dev ngày 2026-10-20

- Backup bằng `pg_dump -Fc`, restore vào PostgreSQL tạm và thử repair/khởi động trước khi áp dụng.
- Repair chỉ thêm dấu `DELETE` cho V49; app sau đó áp dụng V54–V56 và vượt qua Hibernate schema validation.
- App trên bản sao và stack dev hiện tại đều healthy; giữ 30 sản phẩm, 13 đơn hàng, 6 danh mục.
- Đối chiếu hash xác nhận danh mục, toàn bộ đơn hàng và các trường dòng đơn ngoài hai khóa tham chiếu
  do V56 sửa không đổi. V54 cập nhật giá catalog demo; snapshot giá/số lượng đơn cũ giữ nguyên.
- `mvn -B clean verify` trên JDK 21 trong Docker: **158 tests, 0 failures/errors/skipped**.
  Có hai test PostgreSQL mới cho repair đúng V49, chạy lại, bảo toàn dữ liệu và từ chối lỗi checksum/
  migration thiếu khác. Image backend build thành công; API danh mục, sản phẩm, tìm kiếm, SmartFit
  và frontend trả HTTP 200 sau nâng cấp.
