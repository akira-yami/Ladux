package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;
import java.util.List;

/**
 * Response cho GET /api/v1/discovery/suggestions.
 *
 * <p>mode cho biet du lieu dang o che do nao:
 * <ul>
 *   <li>{@code BEST_SELLER}: q rong, co du doanh so hop le trong 30 ngay -> top ban chay that.</li>
 *   <li>{@code FEATURED}: q rong nhung chua du doanh so -> san pham noi bat (khong gan nhan "ban chay").</li>
 *   <li>{@code SEARCH}: q co gia tri -> goi y khop ten/tien to/gan dung.</li>
 * </ul>
 *
 * <p>suggestedQueries: cac nhom kham pha nhanh (khi q rong) hoac tu khoa lien quan (khi q co gia tri).
 */
public record DiscoverySuggestionResponse(
        Mode mode,
        List<String> suggestedQueries,
        List<SuggestionProductResponse> products
) implements Serializable {

    public enum Mode {
        BEST_SELLER,
        FEATURED,
        SEARCH
    }
}
