package org.akira.ladux.identity.repository;

import org.akira.ladux.identity.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
}
