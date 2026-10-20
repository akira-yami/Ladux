# LADUX — Kế hoạch cải tiến SmartFit & Intelligent Discovery

> **Mục tiêu:** Biến Ladux từ website thương mại điện tử laptop thành nền tảng **Intelligent Laptop Commerce**, giúp khách hàng tìm kiếm, chọn và so sánh laptop theo nhu cầu thực tế.
>
> **Phạm vi:** MVP phục vụ demo/portfolio, ưu tiên hoàn thiện trong **20–25/10/2026**. Không mở rộng kiến trúc hoặc xây thêm quá nhiều tính năng.
>
> **Trạng thái 20/10/2026:** Smart Search, SmartFit và Smart Compare đã triển khai. Sau khi đồng bộ `main`, backend `clean verify` đạt 156 tests và frontend đạt 30 tests cùng typecheck/build. Có 10 ảnh thật, video walkthrough 150 giây và kiểm tra Chrome 390 px; media lưu snapshot trước các PR #6/#7 về giá/header. Contextual Chat có API/UI, test HTTP/chat và kiểm chứng fallback; chưa xác nhận toàn bộ luồng LLM thực. Local dùng JDK 25, target Java 21; CI Java 21 từ xa chưa được xác nhận.
>
> Xem [hướng dẫn triển khai, API thực tế và giới hạn MVP](SMARTFIT_GUIDE.md). Các mục bên dưới giữ lại mục tiêu ban đầu; checklist cuối tài liệu ghi nhận mức hoàn thành thực tế.

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

**Triển khai hiện tại:** trọng số gốc trên được điều chỉnh theo ưu tiên: `PERFORMANCE` **50/15/25/10**, `VALUE` **35/40/15/10**, `MOBILITY` **30/20/15/35** (mục đích/ngân sách/bộ nhớ/di động). Backend trả trọng số thực dùng trong response. Di động hiện chấm theo trọng lượng, chưa suy ra thời lượng pin. Tối đa 500 SKU được lấy theo ID trước bước parse RAM/SSD; không bảo đảm tìm tối ưu toàn bộ catalog khi vượt giới hạn này.

Chuỗi ROM chỉ có `512GB`/`1TB` không xác nhận SSD; giữ `ssdGb = null` và loại khỏi kết quả nếu người dùng bắt buộc SSD tối thiểu. Không đoán công nghệ lưu trữ cho dữ liệu cũ. Nếu không đủ lựa chọn, trả ít hơn 3 hoặc rỗng và giữ nguyên ràng buộc.

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

## 2. API đã triển khai

Các endpoint trả **DTO trực tiếp**, không có envelope `{ data: ... }`. Contract đầy đủ, validation, cache và endpoint Contextual Chat được ghi trong [SMARTFIT_GUIDE.md](SMARTFIT_GUIDE.md#2-api-thực-tế).

### `GET /api/v1/discovery/suggestions?q=asus`

- `q` rỗng: sản phẩm bán chạy/nổi bật + nhóm khám phá nhanh.
- `q` có giá trị: gợi ý sản phẩm/từ khóa phù hợp.
- Response: `mode` (`BEST_SELLER`/`FEATURED`/`SEARCH`), `suggestedQueries`, `products[]` (gồm `productId`, `variantId`, `name`, `slug`, `price`, `discountPrice`, `imageUrl`, `available`).
- Nguồn sự thật: catalog, giá của SKU hợp lệ, trạng thái bán; thống kê doanh số từ đơn hàng.
- “Bán chạy” dùng số lượng của đơn **hiện vẫn DELIVERED**, với lần giao thành công đầu tiên trong lịch sử nằm trong 30 ngày. Không bù sản phẩm nổi bật vào nhóm bán chạy nếu có ít hơn 5.
- Frontend cache tối đa **60 giây / 30 truy vấn**; backend đọc dữ liệu live. Backend checkout kiểm tra lại giá/tồn kho; dropdown và trang chi tiết không giữ chỗ hoặc khóa giá.

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

Response có `recommendations[]` với `productId`, `variantId`, `name`, `slug`, `imageUrl`, `score`, `price`, `reasons[]`, `tradeoffs[]`, `specs`, cùng `weights` và `disclaimer`. Frontend tạo link `/products/{productId}?variantId={variantId}` để mở đúng cấu hình. **Không để AI tự tạo giá, SKU hoặc điểm số.**

### `POST /api/v1/chatbot/contextual-chat`

Nhận `{query, conversationId, variantIds?}` (tối đa 5 SKU), trả `{text, products[]}` với thẻ sản phẩm do backend dựng sau khi kiểm tra catalog live. Đã kiểm tra HTTP/test và giao diện fallback; không coi fallback/mock test là xác nhận đã chạy thành công với LLM thực.

## 3. Hướng tổ chức code

**Giữ modular monolith Spring Boot hiện tại**, không thêm microservice.

- `catalog`: truy vấn public, gợi ý tìm kiếm, kiểm tra SKU được phép bán, dữ liệu thông số sản phẩm.
- `ordering`: tổng hợp số lượng bán hợp lệ theo sản phẩm trong 30 ngày.
- `assistant`: `SmartFitService` điều phối lọc/ranking/giải thích theo quy tắc; `ChatbotCatalogService` xác thực catalog trước khi LLM diễn giải.
- `frontend/src/components/common/Header.tsx`: thay hành vi nhập ký tự là chuyển trang ngay bằng dropdown Smart Search; Enter/chọn gợi ý mới điều hướng.
- Frontend đã có `SmartSearchDropdown`, trang `/smartfit` (`SmartFitWizard`) và bảng `SmartCompare`.
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

Đánh dấu `[x]` ghi nhận mã triển khai và kiểm thử đã có; các kiểm chứng môi trường thật còn lại được tách riêng để tránh coi kế hoạch là kết quả đã đạt.

- [x] Focus search rỗng có nhãn bán chạy từ doanh số hợp lệ, hoặc **nổi bật** nếu không có kết quả bán chạy.
- [x] Autocomplete, typo tolerance PostgreSQL, debounce 275 ms, hủy request cũ; điều hướng khi chọn/Enter.
- [x] Có giao diện mobile và bàn phím ↑ / ↓ / Enter / Escape; không gọi AI trên từng ký tự.
- [x] SmartFit trả tối đa 3–5 sản phẩm với lý do/đánh đổi; trả ít hơn hoặc rỗng nếu không đủ SKU đáp ứng.
- [x] Lọc sản phẩm/SKU active, còn hàng, giá dương, trong ngân sách và đáp ứng RAM/SSD bắt buộc.
- [x] Giá/SKU/điểm/nhãn do backend và database quyết định; thông số thiếu ghi “Chưa đủ dữ liệu”.
- [x] Ranking ổn định có unit test; API có Bean Validation và rate limit qua cơ chế hiện có.
- [x] Smart Compare 2–3 SKU dùng lại thông số/giá/lý do/điểm SmartFit.
- [x] Frontend typecheck, 30 tests và production build đạt tại lần kiểm tra hiện tại.
- [x] Checkout kiểm tra SKU inactive; V56 sửa FK sản phẩm/SKU của dòng đơn lịch sử và giữ snapshot giá/số lượng; Flyway được kiểm tra trên Testcontainers mới/nâng cấp và database demo cô lập. Database dev hiện có của người dùng chưa được migrate.
- [x] V55 chỉ dành dev/test tắt SKU demo còn nguyên giá cũ khi các guard khớp; 4 integration test đạt, giữ active cho SKU đã được V54 cập nhật giá và không thay snapshot/tồn kho.
- [x] README và hướng dẫn có kiến trúc, API thực tế, giới hạn MVP, kịch bản demo.
- [x] Backend `clean verify` đạt 156 tests sau khi đồng bộ `main`; 0 failures/errors/skipped, đóng gói thành công. Local JDK 25, target Java 21.
- [x] Có ảnh thật SmartFit form/mobile; Chrome mobile 390 px không tràn ngang.
- [x] Kiểm chứng 3 kết quả SmartFit, so sánh 2 giữ nguyên giá/điểm, chat fallback đúng 2 thẻ và link chọn đúng SKU; có ảnh thật kết quả/so sánh/chat/detail.
- [x] HTTP/chat test bổ sung đã nằm trong lần chạy backend toàn suite.
- [x] Có ảnh focus `FEATURED` 5 sản phẩm/giá thật, `thikpad` ra một ThinkPad, ArrowDown chọn đúng và ngân sách 1 đồng trả rỗng. Enter có unit test; chưa hoàn tất kiểm tra thủ công Enter trong browser.
- [x] Đủ 10 ảnh giao diện thật và [video walkthrough 150 giây](media/smartfit-walkthrough.mp4), 1440×1080, giải mã toàn bộ thành công; video dựng từ ảnh chụp thực, không phải bản ghi thao tác thời gian thực.
- [ ] Xác nhận CI từ xa.
- [ ] Kiểm chứng mới toàn luồng Cart → Checkout → Payment trên môi trường chạy thật.
- [ ] Kiểm chứng Contextual Chat với nhà cung cấp LLM thực và đánh giá nội dung trả lời.

**Giữ ngoài sprint:** P2 lịch sử/cá nhân hóa và P3 ML recommender/agent chưa triển khai. Chưa tuyên bố hoàn thành toàn bộ nghiệm thu khi các kiểm chứng môi trường thật còn lại chưa đạt.

## 7. Định vị dự án trong CV

**Ladux — Intelligent Laptop Commerce Platform**

*A modular Spring Boot e-commerce platform featuring explainable laptop recommendations, smart product discovery, inventory-safe checkout, and payment lifecycle management.*

**Nguyên tắc chốt:** làm ít nhưng hoàn chỉnh. Smart Search và SmartFit phải mang lại giá trị thực cho người mua, không chỉ tạo ấn tượng là “có AI”. Sau mốc 25/10, chuyển Ladux sang maintenance mode để tập trung đồ án CV tiếp theo.
