package org.akira.ladux.assistant.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SmartFitRequest(
        @NotNull(message = "Vui lòng nhập ngân sách tối đa")
        @Positive(message = "Ngân sách phải lớn hơn 0")
        @DecimalMax(value = "1000000000", message = "Ngân sách tối đa là 1 tỷ đồng")
        BigDecimal budgetMax,
        @NotNull(message = "Vui lòng chọn mục đích sử dụng") Purpose purpose,
        @NotNull(message = "Vui lòng chọn ưu tiên") Priority priority,
        @Positive(message = "RAM tối thiểu phải lớn hơn 0") @Max(1024) Integer minRamGb,
        @Positive(message = "SSD tối thiểu phải lớn hơn 0") @Max(65536) Integer minSsdGb,
        @Min(value = 3, message = "Số gợi ý phải từ 3 đến 5")
        @Max(value = 5, message = "Số gợi ý phải từ 3 đến 5") Integer limit
) {
    public enum Purpose { PROGRAMMING, GAMING, DESIGN, OFFICE }
    public enum Priority { PERFORMANCE, VALUE, MOBILITY }

    public int resultLimit() {
        return limit == null ? 3 : limit;
    }
}
