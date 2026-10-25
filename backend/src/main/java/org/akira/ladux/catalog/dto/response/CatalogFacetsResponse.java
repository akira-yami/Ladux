package org.akira.ladux.catalog.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CatalogFacetsResponse(List<String> ram, List<String> rom, BigDecimal maxPrice) implements java.io.Serializable {}
