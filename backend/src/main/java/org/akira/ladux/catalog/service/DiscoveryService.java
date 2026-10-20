package org.akira.ladux.catalog.service;

import org.akira.ladux.catalog.dto.response.DiscoverySuggestionResponse;

/** Smart Search: goi y khi focus (rong) va khi go tu khoa. */
public interface DiscoveryService {

    /**
     * @param q tu khoa tim kiem; null/rong -> tra best seller hoac featured + nhom kham pha nhanh.
     * @return goi y san pham + metadata che do.
     */
    DiscoverySuggestionResponse suggest(String q);
}
