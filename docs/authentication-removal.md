# Gỡ MFA và OTP điện thoại

Ngày cập nhật: 11/10/2026.

Đăng nhập khách hàng và admin trả access token ngay sau khi xác thực mật khẩu. CAPTCHA, giới hạn đăng nhập theo IP/tài khoản, phân quyền admin, refresh cookie và lịch sử đăng nhập vẫn hoạt động. Đăng nhập Google không còn chặn tài khoản có quyền admin do yêu cầu MFA.

Số điện thoại là thông tin liên hệ. Khách hàng cập nhật qua `PUT /api/v1/customers/me/phone` với body `{"phone":"0988123456"}`. Backend chuẩn hóa về E.164, kiểm tra định dạng và số trùng, cập nhật hồ sơ của người đang đăng nhập. Đổi mật khẩu vẫn yêu cầu mật khẩu hiện tại và proof OTP email đã xác minh, chưa hết hạn, dùng một lần.

Đã xóa API, DTO, service, repository, entity, provider OTP điện thoại, TOTP, mã hóa secret, Redis challenge, cấu hình và giao diện liên quan. Các API gửi/xác minh OTP email được giữ lại.

## Database khi triển khai

Flyway tự chạy `V52__remove_mfa_and_phone_verification.sql` khi backend mới khởi động:

- Xóa bảng `phone_verifications` và `user_mfa_methods`, gồm dữ liệu OTP điện thoại và secret MFA cũ.
- Xóa cột `login_history.mfa_used` và các sự kiện audit thuộc MFA để không còn giá trị enum đã bị gỡ.
- Giữ số điện thoại/email liên hệ, lịch sử đăng nhập và các sự kiện audit còn lại.

Các migration còn lại trong V1–V51 được giữ nguyên; V49 được xóa theo yêu cầu riêng sau thay đổi xác thực. Database đã chạy V49 cần đối chiếu lịch sử Flyway trước khi triển khai. Những migration cũ vẫn có tên/nội dung của tính năng đã gỡ. Triển khai đồng bộ backend/frontend; sau V52, backend cũ không còn tương thích schema. Việc xác minh dùng database Testcontainers tạm, chưa áp dụng V52 vào database đang vận hành.

## Kiểm chứng

- Backend: `mvn -o clean verify`, JDK 21; 104 test thành công, không lỗi hoặc bỏ qua.
- Frontend: `npm run check`; TypeScript, 8 Vitest test và production build thành công.
- PostgreSQL: migration cho database mới và nâng cấp V51 → V52 với dữ liệu MFA/OTP cũ; kiểm tra giữ hồ sơ liên hệ, lịch sử đăng nhập và audit đăng nhập.
- Kiểm thử xác thực: đăng nhập admin cấp token/cookie trực tiếp, chặn role CUSTOMER, giữ thứ tự CAPTCHA/rate limit/mật khẩu, Google login và OTP email.
