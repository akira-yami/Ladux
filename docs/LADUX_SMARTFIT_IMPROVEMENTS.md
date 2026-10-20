# LADUX — Kế hoạch cải tiến SmartFit & Intelligent Discovery

> **Mục tiêu:** Biến Ladux từ website thương mại điện tử laptop thành nền tảng **Intelligent Laptop Commerce**, giúp khách hàng tìm kiếm, chọn và so sánh laptop theo nhu cầu thực tế.
>
> **Phạm vi:** MVP phục vụ demo/portfolio, ưu tiên hoàn thiện trong **20–25/10/2026**. Không mở rộng kiến trúc hoặc xây thêm quá nhiều tính năng.
>
> **Trạng thái:** Kế hoạch đề xuất — **chưa phải tính năng đã triển khai**.

## 1. Các cải tiến chính

### 1.1. Smart Search — Tìm kiếm thông minh (P0)

**Khi nhấn vào ô tìm kiếm nhưng chưa nhập:**
- Hiển thị **Top 5 laptop bán chạy trong 30 ngày**, dựa trên số lượng bán từ `orders` / `order_items`.
- Chỉ tính những đơn hợp lệ (ưu tiên `DELIVERED`, loại trừ đơn đã hủy/trả/hoàn); lọc các sản phẩm/biến thể không còn được phép bán.
- Khi dữ liệu đơn hàng không đủ, chuyển sang danh sách **“Sản phẩm nổi bật”** (không gắn nhãn “Bán chạy” sai sự thật).
- Hiển thị vài nhóm khám phá nhanh: *Laptop cho sinh viên*, *Gaming dưới 25 triệu*, *Văn phòng*, *Mỏng nhẹ*.
- Có lối tắt mở **SmartFit Advisor**.

**Khi người dùng nhập từ khóa:**
- Gợi ý tên sản phẩm, từ khóa liên quan, hình ảnh, giá thực tế và liên kết đến trang chi tiết.
- Ưu tiên khớp chính xác/tiền tố, sau đó gần đúng bằng PostgreSQL `pg_trgm` (ví dụ `thikpad` → `ThinkPad`).
- Debounce khoảng **250–300 ms**, hủy request cũ, giới hạn số gợi ý, cache dữ liệu phù hợp.
- Hỗ trợ bàn phím **↑ / ↓ / Enter / Escape**, trạng thái loading/empty/error và giao diện mobile.
- **Không gọi LLM trên từng ký tự.**

### 1.2. SmartFit Advisor — Tư vấn laptop theo nhu cầu (P0)

**Đầu vào:**
- Ngân sách tối đa.
- Mục đích chính: lập trình, gaming, đồ họa, văn phòng/học tập.
- Ưu tiên: hiệu năng, giá trị/giá tiền, pin/di động.
- Có thể chọn RAM/SSD tối thiểu.

**Xử lý:**
1. **Lọc cứng:** sản phẩm và SKU đang hoạt động, còn hàng, nằm trong ngân sách, đáp ứng yêu cầu bắt buộc.
2. **Xếp hạng có thể giải thích:** cấu hình trọng số ban đầu:
   - Phù hợp mục đích: **40%**.
   - Giá so với ngân sách: **25%**.
   - RAM và lưu trữ: **20%**.
   - Di động/trọng lượng/pin: **15%**.
3. Trả **3–5 lựa chọn** với điểm phù hợp, lý do đề xuất, điểm đánh đổi, giá/SKU hiện tại, link chi tiết.
4. Kiểm thử và điều chỉnh trọng số theo từng nhóm nhu cầu; không coi trọng số thử nghiệm là chuẩn khách quan.

**Nguyên tắc:** dữ liệu CPU/GPU/pin trong Ladux có thể là chuỗi tự do; dùng quy tắc phân loại có kiểm chứng, **không suy diễn điểm benchmark**. Thông số không đủ thì ghi **“Chưa đủ dữ liệu”**. AI chỉ hỗ trợ diễn giải, còn lọc/giá/tồn kho/ranking phải do backend và database quyết định.

### 1.3. Smart Compare — So sánh có phân tích (P1)

- So sánh **2–3 laptop**: CPU, GPU, RAM, SSD, giá, trọng lượng, pin (nếu đủ dữ liệu).
- Làm nổi bật **ưu điểm, nhược điểm và đánh đổi** theo nhu cầu người mua.
- Tái sử dụng cùng tiêu chí/ranking của SmartFit để tránh câu trả lời mâu thuẫn.
- MVP chỉ cần trang/bảng so sánh phía frontend, **chưa bắt buộc tạo API riêng**.

### 1.4. Contextual AI — Chatbot hiểu ngữ cảnh (P1, tùy thời gian)

- Tái sử dụng Spring AI/RAG/pgvector đang có.
- Truyền các sản phẩm người dùng vừa chọn hoặc so sánh vào ngữ cảnh tư vấn.
- Câu trả lời được gắn với **ID sản phẩm/SKU hợp lệ** và dữ liệu hiện tại, thay vì đề xuất card bằng cách dò từ khóa đơn giản phía frontend.
- Không triển khai agent đa năng hoặc huấn luyện mô hình mới trong sprint này.

## 2. Thiết kế API đề xuất

### `GET /api/v1/discovery/suggestions?q=asus`

- `q` rỗng: sản phẩm bán chạy/nổi bật + nhóm khám phá nhanh.
- `q` có giá trị: gợi ý sản phẩm/từ khóa phù hợp.
- Response gợi ý: `mode`, `suggestedQueries`, `products[]` (gồm `productId`, `variantId`, `name`, `price`, `imageUrl`, `available`), và thông tin nhãn **bán chạy/nổi bật** nếu thích hợp.
- Nguồn sự thật: catalog, giá của SKU hợp lệ, trạng thái bán; thống kê doanh số từ đơn hàng.

### `POST /api/v1/smartfit/recommend`

Request mẫu:

```json
{
  "budgetMax": 30000000,
  "purpose": "PROGRAMMING",
  "minRamGb": 16,
  "priority": "PERFORMANCE",
  "limit": 3
}
```

Response nên có `recommendations[]` với `productId`, `variantId`, `score`, `price`, `reasons[]`, `tradeoffs[]` và link chi tiết. **Không để AI tự tạo giá, SKU hoặc điểm số.**

## 3. Hướng tổ chức code

**Giữ modular monolith Spring Boot hiện tại**, không thêm microservice.

- `catalog`: truy vấn public, gợi ý tìm kiếm, kiểm tra SKU được phép bán, dữ liệu thông số sản phẩm.
- `ordering`: tổng hợp số lượng bán hợp lệ theo sản phẩm trong 30 ngày.
- `assistant` hoặc một service SmartFit thuộc module phù hợp: điều phối lọc, ranking và diễn giải; không nhân đôi logic catalog.
- `frontend/src/components/common/Header.tsx`: thay hành vi nhập ký tự là chuyển trang ngay bằng dropdown Smart Search; Enter/chọn gợi ý mới điều hướng.
- Frontend: tạo component `SmartSearchDropdown`, trang `SmartFitWizard` và bảng `SmartCompare` (tên đề xuất).
- Tận dụng PostgreSQL, Redis, Spring AI và UI xanh da trời–trắng đã có.

## 4. Thứ tự ưu tiên và giới hạn phạm vi

| Ưu tiên | Hạng mục | Phạm vi MVP |
|---|---|---|
| **P0** | Smart Search | Focus hiện hot/featured, autocomplete, typo tolerance, điều hướng đúng |
| **P0** | SmartFit | Form nhu cầu, lọc SKU, ranking, lý do và đánh đổi |
| **P0** | Độ tin cậy | Test, kiểm tra active/stock, build/CI, hồi quy checkout |
| **P1** | Smart Compare | Bảng so sánh 2–3 máy, tái dùng kết quả SmartFit |
| **P1** | Contextual AI | Tích hợp ngữ cảnh vào chatbot nếu còn thời gian |
| **P2** | Lịch sử/cá nhân hóa | Để phiên bản sau; thiết kế quyền riêng tư trước |
| **P3** | ML recommender/agent | **Không làm trong sprint này** |

## 5. Kế hoạch 20–25/10/2026

| Ngày | Đầu việc chính | Đầu ra |
|---|---|---|
| **20/10** | Chốt API, quy tắc thống kê hot, scoring, kiểm tra dữ liệu | Contract + test cases |
| **21/10** | Smart Search backend | API suggestions + test |
| **22/10** | Smart Search frontend | Dropdown hoạt động desktop/mobile |
| **23/10** | SmartFit backend | Filtering + deterministic ranking + test |
| **24/10** | SmartFit UI; Compare nếu kịp | Luồng tư vấn đầu cuối |
| **25/10** | Ổn định, CI, demo, README | **Ladux v1.0 — Portfolio Release** |

**Quy tắc cắt scope:** Nếu đến cuối **23/10** vẫn chưa ổn định, bỏ Contextual AI nâng cao và Smart Compare để ưu tiên hoàn chỉnh **Smart Search + SmartFit + kiểm thử**.

## 6. Điều kiện nghiệm thu

- [ ] Focus search rỗng hiển thị hot thật, hoặc **nổi bật** nếu chưa có doanh số hợp lệ.
- [ ] Gõ từ khóa có autocomplete, typo tolerance, debounce; kết quả click/Enter đúng.
- [ ] Search dùng được trên mobile và bàn phím; không gọi AI cho từng lần nhập.
- [ ] SmartFit trả top 3–5 sản phẩm phù hợp với đầu vào và giải thích được lý do.
- [ ] Không đề xuất SKU hết hàng, đã tắt bán hoặc vượt ràng buộc ngân sách bắt buộc.
- [ ] Không bịa giá, tồn kho, điểm hiệu năng hoặc mức độ bán chạy.
- [ ] Ranking nhất quán và có unit test; API được validation/rate-limit thích hợp.
- [ ] Không làm hỏng Cart → Checkout → Payment; backend/frontend tests và CI đạt.
- [ ] Kiểm tra các rủi ro tồn đọng từ audit: checkout SKU inactive, an toàn Flyway migration trên dữ liệu hiện có.
- [ ] README có kiến trúc, screenshot, video demo 2–3 phút và mô tả giới hạn MVP.

## 7. Định vị dự án trong CV

**Ladux — Intelligent Laptop Commerce Platform**

*A modular Spring Boot e-commerce platform featuring explainable laptop recommendations, smart product discovery, inventory-safe checkout, and payment lifecycle management.*

**Nguyên tắc chốt:** làm ít nhưng hoàn chỉnh. Smart Search và SmartFit phải mang lại giá trị thực cho người mua, không chỉ tạo ấn tượng là “có AI”. Sau mốc 25/10, chuyển Ladux sang maintenance mode để tập trung đồ án CV tiếp theo.
