package org.akira.ladux.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ColorRequest(
        @NotBlank(message = "Ten khong duoc de trong")
        @Size(max = 50, message = "Ten khong duoc vuot qua 50 ky tu")
        String name,
        @NotBlank(message = "HexCode khong duoc de trong")
        @Size(max = 7, message = "HexCode phai la mot chuoi 7 ky tu")
        String hexCode
) {
}