package org.akira.ladux.identity.repository;

import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Role findByName(RoleName name);
}

