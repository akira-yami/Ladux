package org.akira.ladux.identity.repository;

import org.akira.ladux.identity.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    boolean existsByUserIdAndIpAddress(Integer userId, String ipAddress);

    boolean existsByUserIdAndDeviceIdHash(Integer userId, String deviceIdHash);
}
