# Ladux Frontend

Ứng dụng React + Tailwind CSS gồm storefront tại `/` và Ladux Admin Portal độc lập tại `/admin`.

## Cấu hình

Sao chép `.env.example` thành `.env` và cấu hình:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_ENABLE_UNSAFE_ORDER_RETURN_TRANSITIONS=false
```

`VITE_API_BASE_URL` có thể là origin backend hoặc URL đã có `/api/v1`. Admin Portal dùng HttpOnly Cookie và không lưu access/refresh token trong browser storage.

## Chạy dự án

Chạy cả frontend, backend, PostgreSQL và Redis từ thư mục gốc `Ladux/`:

```bash
docker compose up -d
```

Mở `http://localhost:5173` hoặc `http://localhost:5173/admin`. Frontend Docker dùng bản build tĩnh qua Nginx và gọi API tại `http://localhost:8080`. Cấu hình và dữ liệu backend dùng lại từ Compose hiện có; xem hướng dẫn tại `../README.md`.

Sau khi sửa source, chạy `docker compose up -d --build` tại thư mục gốc để cập nhật image. `frontend/.env` không được copy vào image; các biến `VITE_*` cần được truyền lúc build qua Dockerfile/Compose. `VITE_RECAPTCHA_SITE_KEY` có thể đặt trong `.env` gốc.

Để phát triển với Vite hot reload, chạy trong thư mục `frontend/`:

```bash
npm install
npm run dev
```

Kiểm tra production build:

```bash
npm run typecheck
npm run test
npm run build
```

Các API backend còn thiếu được ghi tại `../docs/backend-integration-gaps.md`.
