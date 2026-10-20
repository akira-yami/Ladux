package org.akira.ladux.assistant.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmartFitRequestTest {
    @Test
    void validatesInputsBeforeRankingAndAllowsOptionalConstraints() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            SmartFitRequest invalid = new SmartFitRequest(BigDecimal.ZERO, null, null, -1, 0, 6);
            Set<String> fields = validator.validate(invalid).stream().map(item -> item.getPropertyPath().toString()).collect(Collectors.toSet());
            assertEquals(Set.of("budgetMax", "purpose", "priority", "minRamGb", "minSsdGb", "limit"), fields);
            SmartFitRequest optional = new SmartFitRequest(BigDecimal.valueOf(30_000_000),
                    SmartFitRequest.Purpose.PROGRAMMING, SmartFitRequest.Priority.PERFORMANCE, null, null, null);
            assertTrue(validator.validate(optional).isEmpty());
            assertEquals(3, optional.resultLimit());
            assertFalse(validator.validate(new SmartFitRequest(BigDecimal.valueOf(1_000_000_001), optional.purpose(), optional.priority(), null, null, 3)).isEmpty());
        }
    }
}
