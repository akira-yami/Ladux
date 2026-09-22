package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Ho va ten khong duoc de trong")
        @Size(min = 2, max = 100, message = "Ho ten phai tu 2 den 100 ky tu")
        String fullName,

        @NotBlank(message = "Username khong duoc de trong")
        @Size(min = 4, max = 60, message = "Username phai tu 4 den 60 ky tu")
        String username,

        @NotBlank(message = "Mat khau khong duoc de trong")
        @Size(min = 6, max = 100, message = "Mat khau phai tu 6 den 100 ky tu")
        String password
) {}
