package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserAdminUpdateRequest(
        @Size(max = 150, message = "FullName khong duoc vuot qua 150 ky tu")
        String fullName,

        @Size(max = 255, message = "Avatar khong duoc vuot qua 255 ky tu")
        String avatar,

        Boolean isActive,

        List<@Positive(message = "RoleId phai la so duong") Integer> roleIds
) {}
