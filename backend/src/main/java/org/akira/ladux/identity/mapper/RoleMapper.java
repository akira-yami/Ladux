package org.akira.ladux.identity.mapper;

import org.akira.ladux.identity.dto.response.RoleResponse;
import org.akira.ladux.identity.entity.Role;

public final class RoleMapper {
    private RoleMapper() {
    }

    public static RoleResponse fromEntity(Role role) {
        return new RoleResponse(role.getId(), role.getName().name());
    }
}
