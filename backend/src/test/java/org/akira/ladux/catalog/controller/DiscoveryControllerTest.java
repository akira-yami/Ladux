package org.akira.ladux.catalog.controller;

import org.akira.ladux.catalog.dto.response.DiscoverySuggestionResponse;
import org.akira.ladux.catalog.service.DiscoveryService;
import org.akira.ladux.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscoveryControllerTest {
    @Test
    void acceptsFocusAndRejectsOversizedSearchBeforeCallingService() throws Exception {
        DiscoveryService service = mock(DiscoveryService.class);
        when(service.suggest(null)).thenReturn(new DiscoverySuggestionResponse(
                DiscoverySuggestionResponse.Mode.FEATURED, List.of(), List.of()));
        var mvc = MockMvcBuilders.standaloneSetup(new DiscoveryController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/discovery/suggestions")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/discovery/suggestions").param("q", "a".repeat(121)))
                .andExpect(status().isBadRequest());
        verify(service).suggest(null);
        verifyNoMoreInteractions(service);
    }
}
