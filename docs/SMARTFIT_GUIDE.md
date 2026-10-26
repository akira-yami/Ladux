# SmartFit & Intelligent Discovery — hướng dẫn triển khai

Cập nhật theo snapshot portfolio **27/10/2026**. Smart Search, SmartFit và Smart Compare đã triển khai; hợp đồng API và các giới hạn bên dưới mô tả code hiện tại. Kết quả kiểm thử tổng thể xem [Project status](PROJECT_STATUS.md), không dùng số test của các đợt cũ làm trạng thái mới nhất. Contextual Chat có API/UI và phản hồi dự phòng; chưa xác nhận toàn bộ luồng với nhà cung cấp LLM thực. [Kế hoạch gốc](LADUX_SMARTFIT_IMPROVEMENTS.md) được giữ làm lịch sử, không phải roadmap đang chạy.

## 1. Trải nghiệm và kiến trúc

- **Smart Search:** focus ô tìm kiếm mở tối đa 5 sản phẩm, nhóm khám phá nhanh và lối vào SmartFit. Nhập tên có debounce 275 ms, hủy request cũ, bàn phím ↑ / ↓ / Enter / Escape, trạng thái tải/rỗng/lỗi và giao diện mobile. Chỉ Enter hoặc chọn gợi ý mới chuyển trang.
- **SmartFit `/smartfit`:** ngân sách, mục đích, ưu tiên, RAM/SSD tối thiểu tùy chọn; yêu cầu tối đa 3–5 kết quả. Có thể nhận ít hơn 3 hoặc rỗng nếu không đủ sản phẩm hợp lệ. Mỗi kết quả có đúng giá/SKU, điểm, lý do và đánh đổi do backend cung cấp.
- **Smart Compare:** chọn 2–3 kết quả để so sánh CPU, GPU, RAM, SSD, giá, trọng lượng và pin. Bảng giữ nguyên điểm/lý do/đánh đổi từ SmartFit, không xếp hạng CPU/GPU bằng benchmark tự suy diễn.
- **Contextual Chat:** nút hỏi trợ lý truyền tối đa 5 `variantIds`; lựa chọn so sánh được ưu tiên, nếu chưa chọn thì dùng kết quả tư vấn. Backend kiểm tra lại SKU và trả thẻ sản phẩm có cấu trúc. Khi model không khả dụng, phần diễn giải có fallback; không coi fallback là bằng chứng LLM đã hoạt động.

Luồng vẫn thuộc **modular monolith Spring Boot**: `catalog` truy vấn sản phẩm/SKU; `assistant` điều phối SmartFit và chat; SQL Discovery tổng hợp dữ liệu `ordering`. Không thêm microservice, mô hình ML hoặc agent mới. PostgreSQL là nguồn dữ liệu catalog/đơn hàng; Redis tiếp tục phục vụ rate limit; Spring AI/pgvector phục vụ ngữ cảnh chat. Search và ranking SmartFit không gọi LLM.

Các điểm vào chính:

| Trách nhiệm | Vị trí |
|---|---|
| Discovery API / SQL | `backend/src/main/java/org/akira/ladux/catalog/{controller,service,repository}` |
| Lọc, ranking, giải thích | `backend/src/main/java/org/akira/ladux/assistant/service/SmartFitService.java` |
| Đọc thông số bảo thủ | `backend/src/main/java/org/akira/ladux/assistant/util/SmartFitSpecs.java` |
| Search dropdown | `frontend/src/components/common/SmartSearchDropdown.tsx` |
| Form, kết quả, hủy request | `frontend/src/pages/SmartFitWizard.tsx` |
| So sánh đúng SKU | `frontend/src/components/common/SmartCompare.tsx` |
| Chat theo catalog hiện tại | `backend/src/main/java/org/akira/ladux/assistant/service/ChatbotCatalogService.java` |

## 2. API thực tế

Các endpoint dưới đây trả **DTO JSON trực tiếp**, không bọc trong `{ data: ... }`. Axios interceptor hiện tại đã trả `response.data`, nên service frontend nhận chính DTO. Các giá trị `null` là dữ liệu chưa đủ, không phải số 0.

### `GET /api/v1/discovery/suggestions?q=...`

`q` tùy chọn, tối đa 120 ký tự. Response:

```ts
{
  mode: "BEST_SELLER" | "FEATURED" | "SEARCH";
  suggestedQueries: string[];
  products: Array<{
    productId: number;
    variantId: number | null;
    name: string;
    slug: string | null;
    price: number | null;
    discountPrice: number | null;
    imageUrl: string | null;
    available: boolean;
  }>;
}
```

`price` là giá niêm yết của SKU; nếu `discountPrice` khác `null` thì đó là giá bán dùng để chọn SKU hiển thị. Sản phẩm/SKU phải active, SKU còn hàng và giá bán dương. Chọn SKU hợp lệ có giá bán thấp nhất cho từng sản phẩm.

**Nhãn bán chạy:** chỉ cộng số lượng dương của dòng đơn có trạng thái hiện tại `DELIVERED`, với lần chuyển `DELIVERED` **đầu tiên** trong `order_histories` nằm trong 30 ngày gần nhất và không ở tương lai. Điều này loại đơn đã hủy/trả/hoàn, đơn cũ được ghi lại lịch sử giao hàng và dữ liệu không có lịch sử giao hàng hợp lệ. Sản phẩm hiện tại vẫn phải được phép bán. Nếu có ít hơn 5 sản phẩm bán chạy thì trả số thực có; không bổ sung sản phẩm nổi bật dưới cùng nhãn. Chỉ fallback `FEATURED` khi không còn kết quả bán chạy hợp lệ; nổi bật sắp theo sản phẩm mới tạo.

**Tìm gần đúng:** exact → prefix → contains → PostgreSQL `word_similarity` (`pg_trgm`, ngưỡng **0,4**, chỉ áp dụng nhánh fuzzy khi từ khóa có ít nhất 3 ký tự), sau cùng theo ID để ổn định thứ tự. Ngưỡng được kiểm tra với `thikpad` → ThinkPad, giảm nhiễu từ tên chứa “Thin”. Từ khóa ngắn vẫn khớp exact/prefix/contains. Escape `%`, `_`, `\` trong truy vấn LIKE.

**Cache:** frontend lưu tối đa **30 truy vấn / 60 giây**, có hủy request và bỏ kết quả đến muộn. Backend Discovery đọc dữ liệu live, không cache kết quả bán chạy/giá theo TTL này. Dropdown có thể hiển thị snapshot cũ trong 60 giây; trang chi tiết cũng dựa vào catalog frontend đã tải, không phải xác nhận giữ giá/tồn kho. Backend checkout kiểm tra lại cấu hình/giá/tồn kho khi tạo đơn.

### `POST /api/v1/smartfit/recommend`

```json
{
  "budgetMax": 30000000,
  "purpose": "PROGRAMMING",
  "priority": "PERFORMANCE",
  "minRamGb": 16,
  "minSsdGb": 512,
  "limit": 3
}
```

- `budgetMax`: bắt buộc, lớn hơn 0 và không quá 1.000.000.000 VNĐ. UI dùng số nguyên đồng; API nhận số thập phân hợp lệ.
- `purpose`: `PROGRAMMING | GAMING | DESIGN | OFFICE`.
- `priority`: `PERFORMANCE | VALUE | MOBILITY`.
- `minRamGb`: tùy chọn, số nguyên 1–1024; `minSsdGb`: tùy chọn, số nguyên 1–65536.
- `limit`: tùy chọn, 3–5; mặc định 3.

```ts
{
  recommendations: Array<{
    productId: number;
    variantId: number;
    name: string;
    slug: string | null;
    imageUrl: string | null;
    price: number; // Giá bán thực của đúng SKU tại lần tư vấn
    score: number; // 0–100, làm tròn một chữ số thập phân
    reasons: string[];
    tradeoffs: string[];
    specs: {
      cpu: string | null;
      gpu: string | null;
      ramGb: number | null;
      ssdGb: number | null;
      weightKg: number | null;
      battery: string | null;
    };
  }>;
  weights: { purpose: number; budget: number; memory: number; mobility: number };
  disclaimer: string;
}
```

Frontend liên kết `/products/{productId}?variantId={variantId}`. Trang chi tiết chỉ chọn SKU thuộc sản phẩm và đang active trong catalog đã tải; hiển thị thông báo nếu SKU trong liên kết không hợp lệ trong dữ liệu đó. SmartFit không cache kết quả. Endpoint có Bean Validation và rate limit IP qua cơ chế Redis/Bucket4j hiện có; vượt giới hạn trả HTTP 429.

### `POST /api/v1/chatbot/contextual-chat`

```json
{
  "query": "Hai cấu hình này có đánh đổi gì khi mang đi học?",
  "conversationId": "demo-smartfit",
  "variantIds": [101, 202]
}
```

`query` bắt buộc, tối đa 2.000 ký tự; `conversationId` bắt buộc, tối đa 100 ký tự; `variantIds` tùy chọn, tối đa 5 số nguyên dương. Response trực tiếp:

```ts
{
  text: string;
  products: Array<{
    productId: number;
    variantId: number;
    name: string;
    price: number;
    imageUrl: string | null;
  }>;
}
```

Nếu đã truyền SKU, backend chỉ giữ những SKU còn active/còn hàng/giá dương trong chính lựa chọn đó; không thay SKU không còn bán bằng cấu hình khác. Nếu không có lựa chọn, pgvector chỉ cấp ID ứng viên; catalog live quyết định thẻ/giá. LLM được giao diễn giải, thẻ sản phẩm được dựng từ dữ liệu đã xác thực. Chưa xác nhận chất lượng hay toàn bộ luồng LLM thực; test mock/fallback không thay thế kiểm thử đó.

## 3. Lọc và ranking có thể giải thích

1. Đọc live các SKU có sản phẩm active, SKU active, tồn kho > 0, giá bán dương và không vượt ngân sách.
2. Lấy tối đa **500 SKU theo ID tăng dần**, rồi parse RAM/SSD, áp dụng yêu cầu tối thiểu và chấm điểm.
3. Sắp theo điểm giảm dần, giá tăng dần, ID sản phẩm rồi ID SKU. Mỗi sản phẩm chỉ giữ SKU đạt điểm tốt nhất, trả tối đa `limit` sản phẩm.

**Giới hạn 500 ứng viên:** được áp dụng **trước** kiểm tra RAM/SSD và ranking. Với catalog vượt giới hạn, SKU phù hợp ở ngoài nhóm này có thể bị bỏ qua; kết quả rỗng không chứng minh toàn catalog không có lựa chọn. Cần thay chính sách truy vấn/chuẩn hóa thông số trước khi mở rộng, không gọi kết quả MVP là tối ưu toàn cục.

Trọng số gốc trong đề xuất là 40/25/20/15. Triển khai hiện tại điều chỉnh theo ưu tiên và trả trọng số đã áp dụng trong response:

| Ưu tiên | Mục đích | Ngân sách | RAM/lưu trữ | Di động |
|---|---:|---:|---:|---:|
| `PERFORMANCE` | 50% | 15% | 25% | 10% |
| `VALUE` | 35% | 40% | 15% | 10% |
| `MOBILITY` | 30% | 20% | 15% | 35% |

Các thành phần được cộng theo trọng số, không dùng LLM để quyết định điểm:

- Mốc RAM/SSD: văn phòng **8/256 GB**, lập trình/gaming **16/512 GB**, đồ họa **32/1024 GB**. Dung lượng đạt mốc thì thành phần đạt 1; thấp hơn theo tỉ lệ; thiếu dữ liệu là 0. Đây là mốc gợi ý thử nghiệm, khác ràng buộc tối thiểu do người mua chọn.
- Mục đích dùng tổ hợp nhận diện nhóm CPU, GPU và tỉ lệ bộ nhớ. Nhận diện Core/Ryzen/Apple M/Snapdragon X hoặc GPU rời không có nghĩa các model trong nhóm nhanh ngang nhau.
- Ngân sách: `0.4 + 0.6 × (1 − giá bán / ngân sách)`; giá thấp hơn tăng điểm thành phần này.
- Bộ nhớ: `0.65 × RAM_fit + 0.35 × SSD_fit`.
- Di động chỉ dùng trọng lượng xác nhận: ≤1,3 kg: 1; ≤1,6: 0,85; ≤1,9: 0,6; ≤2,3: 0,35; còn lại: 0,1; thiếu dữ liệu: 0. Pin chỉ hiển thị chuỗi catalog, chưa chấm thời lượng sử dụng.

**Không đủ dữ liệu:** RAM/lưu trữ hỗn hợp, nhiều cấu hình hoặc “hỗ trợ tối đa” không được đoán. `512GB` hay `1TB` đơn lẻ trong ROM **không xác nhận công nghệ SSD**, nên `ssdGb = null`; chỉ chuỗi có `SSD`/`NVMe` và dung lượng rõ mới được parse. Dòng có HDD/eMMC hoặc lưu trữ hỗn hợp bị coi chưa xác định. Khi người mua yêu cầu SSD tối thiểu, SKU chưa xác nhận SSD bị loại. Không sửa hàng loạt dữ liệu cũ để gán SSD khi không có bằng chứng. UI hiển thị **“Chưa đủ dữ liệu”**.

## 4. Checkout và Flyway

Đề xuất/so sánh không giữ chỗ tồn kho hay khóa giá. Cart → Checkout → Payment tiếp tục dùng nghiệp vụ hiện có; backend kiểm tra lại sản phẩm và SKU active, lượng còn hàng và giá khi tạo đơn, rồi trừ kho nguyên tử.

Migration mới **`V56__align_order_item_product_references.sql`** sửa lệch tham chiếu lịch sử: V29 từng để `order_items.product_id` trỏ SKU, V30 thêm `product_variant_id` nhưng chưa chuyển dữ liệu và khôi phục FK sản phẩm. V56 backfill SKU từ quan hệ cũ khi cần, đồng bộ `product_id` theo sản phẩm của SKU và khôi phục FK tới `products`. **Giữ nguyên snapshot giá mua, số lượng và lịch sử đơn**, không xóa dòng lịch sử hoặc sửa checksum migration đã áp dụng.

Nếu dòng cũ không xác định được SKU, migration thất bại trong transaction thay vì xóa dữ liệu âm thầm. Bổ sung index cho lịch sử giao hàng và SKU đang bán. Flyway đã được kiểm tra trên **database Testcontainers mới/nâng cấp** và **database demo cô lập**. **Chưa migrate hay thay đổi database dev hiện có của người dùng**; các kết quả này không thay thế kiểm tra bản sao dữ liệu của môi trường triển khai khác.

Migration **dev/test** `db/devdata/V55__retire_legacy_demo_skus.sql` xử lý các SKU `LAP-0001..0012` còn mang nguyên giá demo cũ từ seed V3. Chỉ tắt `is_active` khi toàn bộ 12 ID/slug seed đã được xác nhận; từng SKU phải khớp `LAP-*`, `products.sku`, chính xác giá cũ ở cả product/variant, và có SKU VND thay thế active với mã cùng giá đúng seed V38. Catalog đã chỉnh sửa sẽ được bỏ qua. **Không đổi đơn vị tiền, sửa giá/tồn kho, xóa SKU hoặc thay snapshot đơn hàng.** Bốn integration test kiểm tra các guard, bảo toàn lịch sử và giữ active cho SKU đã được migration V54 trên `main` cập nhật giá. Migration V56 được đánh số sau V54/V55 để không trùng phiên bản giữa `db/migration` và `db/devdata`.

## 5. Kiểm chứng và phần còn lại

Tại lần cập nhật tài liệu này:

- Frontend `npm run check`: **30 tests đạt**, TypeScript và production build đạt. Unit test bao phủ debounce/hủy request/cache, quick links, ràng buộc form, chọn đúng SKU, giới hạn so sánh và DTO/AbortSignal.
- Backend `clean verify` sau khi đồng bộ `main`: **156 tests, 0 failures, 0 errors, 0 skipped**, đóng gói thành công. Bao phủ filtering, parsing, ranking, PostgreSQL/Testcontainers, HTTP/chat, Google SDK fallback, thống kê bán chạy, checkout và migration. Local dùng **JDK 25, target Java 21**; **chưa chạy CI Java 21 từ xa**.
- Đã kiểm tra giao diện thật có 3 kết quả SmartFit; chọn 2 cấu hình so sánh giữ nguyên giá/điểm. Chat fallback trả đúng 2 thẻ đã chọn. Link `?variantId=49` chọn đúng SKU RAM 16 GB giá **11.490.000 ₫**, thay vì SKU 8 GB rẻ hơn **9.990.000 ₫** của cùng sản phẩm trong dữ liệu demo.
- Smart Search thực tế: focus rỗng có `FEATURED` với 5 sản phẩm/giá catalog; `thikpad` trả một ThinkPad giá **39.990.000 ₫** trong dữ liệu demo, với ngưỡng fuzzy 0,4. ArrowDown chọn đúng gợi ý; Enter có unit test, chưa hoàn tất kiểm tra thủ công trong browser ở lần này. Ngân sách **1 đồng** trả **0 kết quả**.
- Có đủ 10 ảnh thật: form, kết quả desktop, so sánh, chat fallback, SKU detail, mobile, focus search, typo, keyboard và empty. Chrome với viewport mobile **390 px** không thấy tràn ngang.
- [Video walkthrough](media/smartfit-walkthrough.mp4) đã tạo: **150,0 giây, 1440×1080**, dựng từ ảnh chụp thực, **không phải bản ghi thao tác thời gian thực**. Giải mã toàn video bằng ffmpeg thành công; đã xem frame đầu và xác nhận chữ đọc được.
- Chưa xác nhận giao dịch VNPay mới hoặc toàn bộ luồng với LLM thực; các kiểm tra chat phía giao diện dùng fallback, không gọi nhà cung cấp LLM thực.
- P2 lịch sử/cá nhân hóa và P3 ML recommender/agent được hoãn đúng phạm vi sprint.

Chạy từ các thư mục tương ứng, với Java 21, Docker/Testcontainers và cấu hình môi trường sẵn có:

```powershell
# backend/
.\mvnw.cmd test

# frontend/
npm run check
```

Nếu Vitest trên Windows sandbox báo EPERM khi rename thư mục temp, đặt `TEMP` và `TMP` vào thư mục cache bên trong workspace rồi chạy lại:

```powershell
New-Item -ItemType Directory -Force node_modules/.cache/vitest-tmp | Out-Null
$env:TEMP = (Resolve-Path node_modules/.cache/vitest-tmp).Path
$env:TMP = $env:TEMP
npm run check
```

## 6. Kịch bản demo 2–3 phút

| Thời gian | Thao tác | Điều cần chỉ ra |
|---|---|---|
| 0:00–0:25 | Focus search rỗng, đọc nhãn; nhập `thikpad` | Bán chạy theo dữ liệu hoặc nổi bật; tìm gần đúng, không tự chuyển trang khi gõ |
| 0:25–0:45 | Dùng ↑ / ↓ / Enter; mở nhóm Gaming dưới 25 triệu | Chọn đúng SKU; quick link đặt sẵn nhu cầu/ngân sách |
| 0:45–1:25 | Mở SmartFit; chọn lập trình, 30 triệu, RAM 16 GB | Giá, điểm, lý do, đánh đổi và trọng số từ backend; chỉ rõ thông số thiếu |
| 1:25–1:55 | Chọn 2–3 kết quả; xem bảng rồi mở chi tiết | So sánh đúng SKU/giá; không suy diễn CPU benchmark hoặc thời lượng pin |
| 1:55–2:20 | Đặt ngân sách thấp/RAM cao; thử hủy yêu cầu | Empty state giữ ràng buộc; có hủy/loading/error |
| 2:20–2:45 | Chuyển mobile; tùy chọn mở trợ lý theo các SKU đã chọn | Responsive, ngữ cảnh và thẻ do server kiểm tra; ghi rõ nếu LLM đang fallback |

Ảnh đã có: [form nhu cầu](media/smartfit-form.jpg), [kết quả desktop](media/smartfit-desktop.jpg), [so sánh](media/smart-compare.jpg), [chat fallback](media/contextual-chat.jpg), [chi tiết SKU](media/smartfit-sku-detail.jpg), [mobile 390 px](media/smartfit-mobile.jpg), [focus search](media/smart-search.jpg), [tìm sai chính tả](media/smart-search-typo.jpg), [chọn bằng bàn phím](media/smart-search-keyboard.jpg), [empty state](media/smartfit-empty.jpg).

[Xem video walkthrough 150 giây, 1440×1080](media/smartfit-walkthrough.mp4). Video dựng từ ảnh chụp thực, **không phải bản ghi thao tác thời gian thực**. Bảng kịch bản trên có thể dùng cho một lượt demo tương tác hoặc ghi hình bổ sung sau này; sự tồn tại của video không thay thế các kiểm chứng VNPay/LLM/CI còn lại.

Ảnh/video là snapshot trước khi đồng bộ PR #6/#7 về giá demo và header. Các mức giá, điểm và bố cục trong media có thể khác seed hiện tại; API luôn dùng giá database của môi trường đang chạy.
