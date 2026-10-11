# Đánh giá production readiness — Ladux

Ngày đánh giá: 11/10/2026. Snapshot Git: `5e14a59`. Phạm vi: source backend/frontend, migration, test, CI và cấu hình Docker/Caddy trong workspace.

Cập nhật cùng ngày theo yêu cầu: đã gỡ MFA và OTP điện thoại; các khuyến nghị bên dưới được điều chỉnh cho đăng nhập mật khẩu và OTP email. Bảng kiểm tra ban đầu vẫn ghi kết quả của snapshot trước thay đổi; xem `authentication-removal.md` cho kiểm chứng sau thay đổi.

**Kết luận: MVP nâng cao, ứng viên cho staging; chưa đủ bằng chứng và còn các điểm chặn để vận hành bán hàng thật.** Mức độ nghiệp vụ tốt hơn một dự án CRUD mẫu: có checkout nguyên tử, stock ledger, state machine, webhook kiểm tra chữ ký/số tiền, refresh rotation và rate limit phân tán. Khoảng cách lớn nhất nằm ở nâng cấp dữ liệu thật, hoàn thiện các luồng production và quy trình vận hành.

Thang trưởng thành dùng trong báo cáo này: 1 = prototype; 2 = MVP chức năng; 3 = MVP nâng cao/ứng viên staging; 4 = production quy mô nhỏ đã được kiểm chứng; 5 = vận hành có bằng chứng về tải, khôi phục và mở rộng. Đây là thang đánh giá của lần audit này, không phải chứng nhận hay đánh giá năng lực cá nhân.

## Bằng chứng kiểm tra

| Kiểm tra | Kết quả | Giới hạn |
|---|---|---|
| Frontend `npm run check` | TypeScript pass; Vitest 8/8; Vite build pass | Test chỉ nằm trong `src/admin/utils.test.ts`, chưa chứng minh luồng mua hàng trên trình duyệt |
| Backend `mvn -o verify` | 117 test, 39 suite; 0 failure/error/skipped; BUILD SUCCESS | Build incremental, chạy với JAVA_HOME JDK 25; chưa chạy clean build JDK 21 như CI/Docker |
| PostgreSQL/Redis Testcontainers | Chạy thành công, gồm test Flyway schema mới | Database mới không chứng minh nâng cấp database đang có dữ liệu thật |
| V49 trên PostgreSQL tạm, dữ liệu riêng | Tái hiện thay đổi danh mục và lỗi FK | Container không mở port, không gắn volume dự án và đã được xóa |
| `npm audit --omit=dev --json` | 1 dependency mức high: Axios 1.18.1; có bản sửa | Advisory không đồng nghĩa đã chứng minh khai thác được trong SPA; chưa audit dependency backend/dev |

Lần chạy frontend đầu trong sandbox gặp EPERM; chạy lại ngoài sandbox đã pass. Kiểm tra không sửa source ứng dụng, dependency hay dữ liệu đang dùng. Đây là phạm vi của lần audit ban đầu; thay đổi xác thực được thực hiện sau đó theo yêu cầu riêng.

Chưa đo API p95/p99, throughput, query count trên dữ liệu lớn, Lighthouse/Core Web Vitals, failover, restore hoặc giao dịch VNPay live. Không suy ra số người dùng đồng thời hay phần trăm production readiness từ số test.

## Những nền tảng đã có

- Backend monolith chia 10 module nghiệp vụ, có ArchUnit bảo vệ phân tầng và ranh giới `shared`.
- Checkout dùng khóa giỏ hàng, cập nhật stock có điều kiện và transaction; giá mua được chốt; hủy đơn hoàn stock/coupon.
- Payment webhook có kiểm tra HMAC, merchant reference và amount; có xử lý callback đã thành công.
- Refund lưu attempt bền vững trước khi gọi gateway, gọi HTTP ngoài transaction và chỉ hoàn tất khi xác nhận thành công.
- Có stock ledger, procurement, nhập kho, review, wishlist, địa chỉ và admin operations.
- Access token trong memory, refresh cookie, rotation/reuse detection, token version và rate limit Redis.
- Có Flyway, index cho đường truy vấn chính, phân trang theo ID rồi fetch entity; `open-in-view=false`.
- Có CI backend/frontend; production Compose tách mạng data, Caddy HTTPS, runtime non-root và health probe.
- Frontend đã lazy-load trang storefront và tách AdminApp thành chunk riêng.

## P0 — xử lý trước khi nhận đơn thật

### 1. Migration V49 không bảo toàn danh mục đang tồn tại

`backend/src/main/resources/db/migration/V49__delete_parent_in_category.sql:1` dùng `DROP TABLE categories CASCADE`, sau đó seed lại 6 danh mục cố định và gắn lại FK từ products.

Tái hiện trên PostgreSQL 17:

- Danh mục ID 1 với slug `real-category`, đang có sản phẩm tham chiếu, trở thành `laptop-gaming` sau migration. FK hợp lệ nhưng ý nghĩa dữ liệu bị thay đổi.
- Danh mục ID 99 đang có sản phẩm tham chiếu làm bước thêm FK thất bại: `Key (category_id)=(99) is not present in table categories`.
- Thực thi migration trong transaction cho case ID 99 đã rollback; kết quả là nâng cấp không hoàn tất. Case ID 1 vẫn là mất ý nghĩa dữ liệu khi migration commit thành công.

**Việc cần làm:** xác định môi trường nào đã áp dụng V49; lập kế hoạch nâng cấp bảo toàn danh mục/ID/FK; kiểm thử trên bản sao dữ liệu hiện hữu. Không sửa migration đã áp dụng một cách tùy tiện. Nếu bảng đã bị thay, migration mới không tự phục hồi được dữ liệu cũ; cần backup hoặc nguồn đối chiếu.

Tài liệu `backend/docs/production-hardening-2026-10.md` cũng ghi nhận V23/V38 đã đổi nội dung và cần xử lý checksum cho database cũ. Test schema mới hiện tại không bao phủ các tình huống nâng cấp này.

### 2. Kiểm tra trạng thái được phép bán ở backend

`ProductServiceImpl.getAllProducts()` dùng `findAllIds()`; truy vấn này không lọc `isActive`. Có `findIdsByIsActiveTrue()` nhưng chưa được gọi. Storefront map danh sách sản phẩm mà không lọc trạng thái product.

`InventoryServiceImpl.reserveStockAndPriceLines()` và `ProductVariantRepository.deductStockAtomically()` chỉ kiểm tra tồn kho; chưa kiểm tra product/variant có đang hoạt động. Với giỏ hàng đã chứa SKU trước khi admin ngừng bán, checkout vẫn có đường tạo đơn cho SKU đó nếu còn stock.

**Việc cần làm:** tách quy tắc catalog public/admin; kiểm tra trạng thái bán tại checkout trong cùng transaction với reserve stock; thêm test “SKU bị tắt sau khi cho vào giỏ”. Ẩn trên frontend không đủ để bảo vệ nghiệp vụ này.

### 3. Hoàn thiện cấp tài khoản admin

Đăng nhập admin dùng mật khẩu, CAPTCHA và rate limit. Cần có bootstrap admin ban đầu được kiểm soát, quy trình khôi phục tài khoản và audit thay đổi quyền. Chạy thử trên database production mới; nếu provision bằng công cụ ngoài repo, cần đưa bằng chứng và quy trình đó vào release gate.

### 4. Đóng các luồng thanh toán và hoàn tiền dùng trong vận hành

Refund an toàn hơn nhiều so với CRUD trạng thái đơn, nhưng attempt `PROCESSING` hoặc `REVIEW_REQUIRED` vẫn phải xử lý thủ công. `RefundWorkflowService.prepare()` chặn mọi attempt tiếp theo; chưa thấy endpoint/UI để liệt kê và giải quyết các attempt chưa rõ kết quả hoặc job đối soát payment với gateway.

`payment/service/impl/PaymentServiceImpl.java:217` còn cho admin sửa provider/status của payment PENDING, tự tạo transactionNo khi chọn SUCCESS và xác nhận order. Đây là đường ghi nhận tiền thủ công cần ràng buộc rõ với phương thức thanh toán và bằng chứng thu tiền.

**Việc cần làm:** đối soát các giao dịch mất callback, danh sách refund cần kiểm tra, xác nhận xử lý kèm bằng chứng và audit; hạn chế sửa provider sau khi tạo payment; giới hạn thao tác ghi nhận thu tiền thủ công. Không tự retry refund khi kết quả chuyển tiền chưa rõ.

Nếu nhận VNPay live, phải kiểm chứng thông tin merchant, URL, IPN HTTPS, chữ ký, số tiền, callback lặp/đến muộn và đối soát với môi trường thật trước khi mở cho khách.

### 5. Kiểm chứng OTP email trên production

Số điện thoại chỉ là thông tin liên hệ và được cập nhật trực tiếp. Đổi mật khẩu dùng mã gửi qua email, mật khẩu hiện tại và proof xác thực dùng một lần. Cần kiểm chứng SMTP thật, resend/cooldown, giới hạn thử sai, hết hạn và lỗi gửi mail trước khi mở cho khách.

### 6. Chứng minh khả năng vận hành và khôi phục

Trong repo có một dump thủ công trong `backups/`, nhưng chưa thấy cấu hình backup tự động ngoài máy chủ, retention hoặc bài diễn tập restore. Compose lưu PostgreSQL/Redis/uploads trên cùng host.

**Việc cần làm:** backup DB và media ngoài host, kiểm thử restore lên môi trường mới, chốt lượng dữ liệu tối đa chấp nhận mất và thời gian khôi phục; có người nhận alert và runbook khi DB/Redis/gateway lỗi. [PostgreSQL mô tả SQL dump, filesystem backup và PITR](https://www.postgresql.org/docs/17/backup.html); chọn cách phù hợp với mục tiêu khôi phục đã chốt.

## P1 — hiệu năng và độ ổn định

| Ưu tiên | Điểm cụ thể | Cải thiện | Cách xác minh |
|---|---|---|---|
| 1 | `frontend/src/stores/useProductStore.ts:79` luôn tải page 0, size 1000; bỏ qua filters/page đã lưu | Nối lọc/sort/phân trang server; page khoảng 24–48; giới hạn page size public; debounce và hủy request cũ | Catalog trên 1000 sản phẩm vẫn tìm thấy đủ kết quả; đo payload và thời gian render |
| 1 | `frontend/src/app/AppRouter.tsx:149` tìm detail từ danh sách đã tải | Detail gọi API theo ID/slug riêng, xử lý 404/ẩn sản phẩm | Deep link đến sản phẩm ngoài page đầu vẫn mở được |
| 2 | `catalog/mapper/ProductMapper.java` đọc toàn bộ lazy reviews để tính average/count cho từng product; summary gọi lại full mapper | Aggregate rating/count bằng query hoặc dữ liệu tổng hợp; DTO danh sách gọn; kiểm tra fan-out khi fetch nhiều collection | Đo query count cold-cache và EXPLAIN trên dữ liệu có nhiều review/variant/image |
| 2 | TTL cache chỉ cấu hình trong profile dev; checkout xóa toàn cache products/carts/orders và nhiều cache khác; job hết hạn cũng xóa cache theo lịch | TTL riêng trên prod, invalidate đúng user/product sau commit; hạn chế cache các dữ liệu thay đổi liên tục; cân nhắc SCAN khi clear lớn | Cache hit ratio, Redis memory/latency, query load khi có checkout đồng thời |
| 3 | Asset build: avatar chatbot 1.63 MB, hero 1.18 MB, một logo 0.93 MB; AdminApp 431 KB raw/117 KB gzip | Ảnh WebP/AVIF đúng kích thước, responsive images, lazy-load chatbot/assets và trang admin | Đo mobile với cache lạnh; mục tiêu LCP/INP/CLS đã chốt |
| 3 | SMTP gửi đồng bộ; API client chưa đặt timeout; embedding gọi external API trong `@Transactional`, index-all load tất cả product | Timeout theo tác vụ; gửi cảnh báo/thông báo qua worker có retry; index embedding theo batch/job bền vững và rút ngắn transaction | Mô phỏng SMTP/AI chậm hoặc mất mạng; theo dõi connection pool và request bị treo |
| 4 | Job hết hạn lock toàn bộ đơn quá hạn trong một transaction; reserve SKU theo thứ tự items | Xử lý theo batch nhỏ, thứ tự khóa nhất quán; đo lock wait/deadlock trước khi tăng pool/instance | Test backlog lớn và checkout cùng nhiều SKU |
| 4 | RAG 3072 dimensions, index NONE, chat memory trong process | Đo latency/recall trước khi chọn vector index; giới hạn số phiên/TTL và ngân sách chatbot; ràng buộc session hội thoại | Dataset tăng dần, timeout nhà cung cấp, nhiều conversation mới |

TTL cache mặc định của Spring Data Redis không có expiration; cấu hình TTL dev không tự áp dụng cho prod. [Tài liệu Redis Cache](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html).

Với embedding 3072 chiều, bật HNSW thông thường trên kiểu `vector` không phải thay đổi cấu hình đủ an toàn: pgvector giới hạn index `vector` ở 2000 chiều, `halfvec` ở 4000 chiều. Có thể giảm dimensions/re-embed hoặc dùng half precision với query tương ứng, rồi đo recall. [Tài liệu pgvector](https://github.com/pgvector/pgvector#hnsw).

Mục tiêu frontend tham khảo: LCP ≤2.5s, INP ≤200ms, CLS ≤0.1 ở phân vị 75, phân biệt mobile/desktop. Đây là mục tiêu, chưa phải số đo của Ladux. [Core Web Vitals](https://web.dev/articles/vitals).

## Nền tảng tối thiểu cho lần production đầu

- **Hosting frontend:** build artifact lên static hosting/CDN hoặc cấu hình Caddy phục vụ frontend, SPA fallback và cache asset theo hash. Compose hiện chỉ reverse proxy backend.
- **Backend:** tiếp tục monolith hiện tại; triển khai image có version/digest, có staging, readiness gate, giới hạn resource và quy trình rollback tương thích schema. Compose app hiện healthcheck liveness; CI hiện kiểm tra source nhưng chưa có pipeline phát hành/triển khai.
- **Dữ liệu:** PostgreSQL có backup ngoài host và restore đã diễn tập; uploads có backup hoặc object storage dùng chung nếu chạy nhiều instance; Redis có budget memory và chính sách eviction phù hợp cache/rate-limit.
- **Quan sát:** metrics, dashboard, alert và log tập trung có request ID/redaction. `application.properties` expose tên prometheus nhưng `pom.xml` chưa có `micrometer-registry-prometheus`. Cần dependency registry và cách scrape nội bộ/xác thực phù hợp, vì `/actuator/**` đang yêu cầu ADMIN. [Spring Boot Metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html).
- **Tác vụ ngoài:** nhà cung cấp email/SMS đúng chức năng đã mở; notification/embedding có worker hoặc outbox khi cần retry bền vững. Có thể bắt đầu bằng worker và bảng job trong PostgreSQL.
- **Release:** clean build JDK 21, E2E storefront/admin, scan secret/dependency/image, kiểm tra prod env/cookie/OAuth qua domain HTTPS và smoke test sau triển khai.

Axios đang cài là 1.18.1; audit production dependency trả về một package mức high và có bản sửa. Cần cập nhật có kiểm thử, xác định advisory nào liên quan đến adapter thực tế. Một số advisory chỉ là gadget cần prototype pollution từ nơi khác, hoặc thuộc Node adapter; chưa chứng minh Ladux bị khai thác. Ví dụ [advisory do Axios công bố](https://github.com/axios/axios/security/advisories/GHSA-9fr6-4gfg-395g) ghi phiên bản vá nhánh 1.x là 1.20.0.

## Chức năng cần bổ sung theo mô hình kinh doanh

- **Hỗ trợ người dùng:** quên mật khẩu khi chưa đăng nhập; khôi phục tài khoản admin; trạng thái đơn và thông báo giao dịch đáng tin cậy. Luồng hiện tại đổi mật khẩu sau đăng nhập không thay cho quên mật khẩu.
- **Vận hành:** đối soát payment/refund, phân quyền nhân viên theo nhiệm vụ, audit thay đổi giá/tồn/tiền, quy trình đổi trả và kiểm tra chất lượng hàng hoàn. Full refund hiện có thể đủ cho lần mở bán đầu nếu chính sách công bố phù hợp.
- **Giao nhận:** hiện có phí giao hàng cấu hình và tracking nhập thủ công; API vận chuyển, tạo vận đơn, cập nhật trạng thái và thu COD cần theo khối lượng đơn thực tế.
- **Kinh doanh laptop:** serial/IMEI và bảo hành nếu bán từng máy cần truy vết; xuất chứng từ/hóa đơn và xuất báo cáo theo quy trình cửa hàng; hoàn một phần nếu cho phép trả một số dòng hàng.
- **SEO:** `frontend/index.html:14` đang `noindex, nofollow` và `lang=en`. Trước khi mở storefront công khai, chọn chính sách index, đổi ngôn ngữ phù hợp, thêm metadata/canonical/sitemap và dữ liệu sản phẩm cho công cụ tìm kiếm. Prerender/SSR cân nhắc theo nhu cầu tìm kiếm tự nhiên.

Các mục kinh doanh phụ thuộc phạm vi mở bán; không cần coi tất cả là điều kiện chặn nếu vận hành thủ công đã có quy trình rõ và đủ năng lực.

## Thứ tự thực hiện và điều kiện đạt

1. **Đóng P0 dữ liệu và nghiệp vụ:** xử lý kế hoạch V49/checksum; chặn SKU ngừng bán; bootstrap admin; chốt luồng OTP và thao tác ghi nhận tiền. Mọi bước có test hoặc diễn tập phù hợp.
2. **Chuẩn bị vận hành:** staging giống prod, image có version, backup/restore, metrics/alert, đối soát/refund và smoke test các dịch vụ thật.
3. **Sửa hiệu năng có bằng chứng:** phân trang/detail độc lập, rating aggregation và cache; tối ưu ảnh; đo p95/query count trên dữ liệu đại diện.
4. **Mở pilot có giới hạn:** quy mô do load test xác định, có người trực vận hành và khả năng rollback/restore. Mở rộng sau khi thu được số liệu thực tế.

Release gate đề xuất:

- `clean verify` JDK 21 và `npm ci && npm run check` pass; image thực tế build và khởi động prod thành công.
- Migration pass trên DB mới và bản sao DB hiện hữu; danh mục/ID/FK/dữ liệu khách/đơn được bảo toàn.
- E2E đăng ký/login/admin, thêm giỏ/checkout, callback lặp/đến muộn, hủy/hoàn stock và refund chưa rõ kết quả pass.
- Test tranh chấp SKU cuối, nhiều checkout/callback/cancel cùng lúc, timeout bên thứ ba và Redis/DB lỗi bảo toàn invariant. Chưa thấy test nhiều thread tương ứng trong suite hiện tại.
- Backup restore DB/media lên môi trường sạch thành công; alert quan trọng đã được kích hoạt thử.
- Có SLO tải dự kiến và kết quả load test; có thể đặt mục tiêu ban đầu p95 catalog <500ms và checkout nội bộ <1s nếu phù hợp hạ tầng, tách thời gian gateway khỏi số đo này. Đây là mục tiêu đề xuất, không phải benchmark đã đạt.
- Mọi cảnh báo security mức cao đã sửa hoặc có đánh giá applicability được ghi nhận; secret production được quản lý và không đi vào log/artifact.

Sau các gate này, có thể đánh giá lại mức production quy mô nhỏ. Nâng cấp stack và tăng số nền tảng không tự chứng minh độ sẵn sàng vận hành.
