# Ladux Frontend

React 18 + TypeScript SPA cho storefront và admin portal. Hai khu vực chia sẻ catalog/nghiệp vụ backend nhưng có vùng xác thực và cookie refresh riêng.

[Giới thiệu dự án](../README.md) · [Chạy đầy đủ](../docs/LOCAL_SETUP.md) · [Thiết kế backend](../docs/ENGINEERING.md)

## Phạm vi giao diện

| Khu vực | Các luồng chính |
| --- | --- |
| Storefront | Catalog/filter/sort/page, chi tiết đúng SKU, wishlist, cart/checkout, đơn hàng, hồ sơ và địa chỉ |
| Smart discovery | Autocomplete gần đúng, SmartFit theo nhu cầu, compare đúng cấu hình, contextual chat |
| Admin `/admin` | Catalog/SKU, đơn và thanh toán, đối soát/refund, kho, nhà cung cấp/nhập hàng và tài khoản |

UI không xác định giá cuối, tính hợp lệ của trạng thái đơn hoặc quyền truy cập dữ liệu. Các quyết định đó nằm ở backend.

## Chạy với Vite

Khởi động backend/DB/Redis theo [local setup](../docs/LOCAL_SETUP.md). Nếu frontend Nginx đang chiếm cổng 5173, dừng service `frontend` trước.

Trong thư mục `frontend/`, copy `.env.example` thành `.env` nếu chưa có:

```dotenv
VITE_API_BASE_URL=/api/v1
VITE_ENABLE_UNSAFE_ORDER_RETURN_TRANSITIONS=false
```

```bash
npm ci
npm run dev -- --port 5173 --strictPort
```

Storefront: http://localhost:5173 · Admin: http://localhost:5173/admin.

`VITE_API_BASE_URL=/api/v1` dùng Vite proxy cho API cùng origin; `/uploads`, `/oauth2` và `/login/oauth2` cũng được proxy. Có thể dùng một origin backend đầy đủ, nhưng cần CORS/cookie tương ứng. Với Google OAuth local, callback backend là `http://localhost:8080/login/oauth2/code/google`.

## Cấu hình frontend

| Biến | Ý nghĩa |
| --- | --- |
| `VITE_API_BASE_URL` | Bắt buộc; prefix API tương đối hoặc origin backend, có thể đã gồm `/api/v1` |
| `VITE_RECAPTCHA_SITE_KEY` | Public site key khi backend bật CAPTCHA; secret chỉ nằm ở backend |
| `LADUX_DEV_PUBLIC_URL` | Hostname tunnel được Vite cho phép trong dev |
| `VITE_DEV_CANONICAL_ORIGIN` | Tùy chọn đưa browser về một origin dev thống nhất; để trống nếu chủ động thử từ thiết bị khác |
| `VITE_SITE_URL`, `VITE_ALLOW_INDEXING` | Build robots/sitemap và chính sách index; staging để indexing tắt |
| `VITE_ENABLE_UNSAFE_ORDER_RETURN_TRANSITIONS` | Giữ `false`; UI dùng workflow trả/hoàn tiền chuyên trách |

Tất cả biến `VITE_*` được nhúng vào bundle lúc build: chỉ chứa dữ liệu public. Đổi biến này sau khi container chạy không tự cập nhật bundle. Dockerfile không copy `.env`; Compose truyền build arguments cần thiết.

## Tổ chức code và phiên

- `src/app/`: router, route paths và các thành phần ứng dụng.
- `src/components/`: giao diện storefront, SmartFit/compare, search và chatbot.
- `src/admin/`: admin app, auth provider, API client, trang và định nghĩa resource.
- `src/services/`: API client, service theo nghiệp vụ, token memory và session isolation.
- `src/stores/`: state dùng chung bằng Zustand; TanStack Query được dùng cho các truy vấn liên quan.
- `src/utils/`: logic giá checkout, payment flow, lựa chọn SmartFit và navigation.

Access token giữ trong memory; refresh dùng cookie HttpOnly. Request/response có session epoch để response tài khoản cũ không đi vào phiên mới. API client giới hạn timeout, gộp refresh đồng thời trong cùng vùng phiên và chỉ redirect khi refresh bị xác nhận không hợp lệ. Authorization theo vai trò/chủ sở hữu vẫn do backend thực thi.

Chatbot render Markdown qua `react-markdown`, GFM và xử lý xuống dòng; raw HTML bị bỏ qua, liên kết đi qua kiểm tra URL. Product card được render từ response catalog đã xác thực, tách khỏi văn bản mô hình.

## Kiểm tra

```bash
npm run check
npm audit --audit-level=moderate
```

`check` chạy TypeScript, Vitest và production build. Snapshot 27/10/2026 đạt **57 tests / 14 files**; xem [bằng chứng CI và giới hạn](../docs/PROJECT_STATUS.md). Vitest chưa thay cho bộ browser E2E toàn bộ ứng dụng.

Hướng dẫn tích hợp: [SmartFit API](../docs/SMARTFIT_GUIDE.md), [VNPay flow](../docs/VNPAY_FLOW.md), [cấu hình callback/cookie](../docs/VNPAY_SETUP.md).
