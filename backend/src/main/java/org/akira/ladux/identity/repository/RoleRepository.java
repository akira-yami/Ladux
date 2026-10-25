package org.akira.ladux.identity.repository;

import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Role findByName(RoleName name);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Role r where r.name = :name")
    Role findByNameForUpdate(@org.springframework.data.repository.query.Param("name") RoleName name);
}

