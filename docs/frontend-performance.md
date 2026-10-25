# Khắc phục giật khi cuộn và rê chuột — 21/10/2026

Đã tái hiện trên bản production Ladux tại `http://localhost:5173` và cập nhật riêng container frontend. Backend và dữ liệu không thay đổi.

## Nguyên nhân đã xác nhận

1. Header đổi chiều cao `76px → 64px` trong 500ms, đồng thời animate `grid-template-rows` của thanh điều hướng. Trình duyệt phải tính lại bố cục liên tục khi cuộn và đổi hướng.
2. Header có hai lớp backdrop blur, kèm animation nền/bóng đổ. Nội dung phía dưới tiếp tục cuộn nên chi phí vẽ/composite tăng.
3. Mỗi đảo chiều dù chỉ 1px lập tức đổi trạng thái menu, khiến chuyển động khởi động lại khi touchpad/chuột rung nhẹ.
4. Thẻ sản phẩm animate `box-shadow` và `border-color` khi hover, gây nhiều lượt paint trong lúc di chuột qua các thẻ.
5. Khi chuyển trang, `scrollTo(0, 0)` chịu ảnh hưởng của `scroll-behavior: smooth` toàn cục, có thể tạo cảm giác trang mới vẫn đang trượt.

## Thay đổi

- Giữ thanh chính ở 76px trên desktop / 68px trên mobile. Thanh điều hướng nằm riêng, ẩn/hiện bằng `transform` và `opacity`, bỏ blur của header.
- Chỉ đổi trạng thái menu sau 12px di chuyển cùng hướng; ngưỡng bóng đổ 20/40px tránh nhấp nháy quanh một mốc. Chỉ cập nhật React khi trạng thái thực sự thay đổi, giữ listener passive và gom bằng requestAnimationFrame.
- Menu ẩn có `inert` và `aria-hidden`, không nhận focus/click. Vẫn hỗ trợ reduced motion.
- Bóng đổ và viền thẻ đổi một lần khi hover; chỉ animate chuyển động nâng thẻ và ảnh.
- Reset cuộn tức thì trước khi vẽ trang mới bằng `useLayoutEffect` và `behavior: "instant"`. Nút cuộn đến catalog vẫn dùng smooth scroll.

## Đo trước/sau

Chrome 155.0.8059.85 headless, viewport 1440×900, CPU throttle 4×, bản production qua Nginx, dữ liệu backend thật. Làm nóng font/ảnh trước khi đo; cùng một chuỗi 30 thao tác wheel có đổi hướng. Đếm sự kiện `Layout` / `Paint` bằng Chrome DevTools Protocol; ResizeObserver theo dõi chiều cao header. Đây là kết quả của kịch bản cục bộ, không phải số FPS bảo đảm trên mọi thiết bị.

| Chỉ số trong bài đo cuộn | Trang chủ trước → sau | Danh mục trước → sau |
| --- | --- | --- |
| Số thẻ đang render | 37 → 37 | 8 → 8 |
| Số lần layout | 236 → **0** | 314 → **0** |
| Thời gian layout | 260,29 → **0ms** | 130,22 → **0ms** |
| Số lần paint | 444 → **45** | 632 → **54** |
| Thời gian paint | 763,08 → 460,27ms | 1.129,38 → 148,69ms |
| Số chiều cao header quan sát được | 72 → **1** | 104 → **1** |
| Khoảng cách frame p95 | 41,8 → 27,8ms | 14,8 → 7,8ms |

Phép A/B riêng cho hover trên cùng trang `/products`, 12 lượt qua thẻ và 4 lần rời vùng thẻ: bỏ transition bóng đổ/viền giảm paint **1.107 → 112** (khoảng 90%), thời gian paint **1.385,23 → 192,71ms**. Số layout bằng nhau (24) trong phép đo này. Frame timing và thời gian JavaScript có thể dao động; trang chủ vẫn có một số frame chậm khi throttle CPU 4×.

Dữ liệu và script tái chạy lưu cục bộ tại `output/performance/`: `before.json`, `after.json`, `hover.json`, `profile.cjs`, `hover.cjs`, `smoke.cjs`. Thư mục `output/` được Git ignore. Script dùng Playwright qua biến `LADUX_PLAYWRIGHT_MODULE` trỏ tới module có sẵn trong runtime. `profile.cjs` nhận nhãn kết quả làm đối số và URL qua `LADUX_URL` (mặc định localhost:5173).

## Kiểm chứng

- TypeScript đạt; 34 tests / 8 suites đạt, gồm 4 tests mới về đổi hướng, rung nhỏ, hysteresis và vị trí cuộn khôi phục. Production build đạt.
- Chrome ở 1440px và 390px: chiều cao header ổn định; cuộn xuống ẩn menu; rung nhỏ không bật menu; cuộn lên hiện lại; navigation bấm được; chuyển route về đầu trang tức thì; menu ẩn không focus được; không tràn ngang; không có pageerror.
- Đã kiểm tra ảnh chụp giao diện sáng/tối và reduced motion. Frontend Docker sau cập nhật báo healthy.
- Dropdown danh mục vẫn bấm được; tìm `thikpad` rồi bấm gợi ý mở đúng chi tiết `/products/15?variantId=30` và vị trí cuộn bằng 0.
- Chưa đo trên website public hay trực tiếp trên phiên trình duyệt/thiết bị đang gặp lỗi của người dùng.

Hướng sửa phù hợp với [hướng dẫn animation hiệu năng của Google](https://web.dev/articles/animations-guide): tránh animate thuộc tính gây layout/paint, ưu tiên transform/opacity sau khi đo nguyên nhân thực tế.
