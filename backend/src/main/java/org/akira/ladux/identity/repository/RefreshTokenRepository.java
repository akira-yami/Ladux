package org.akira.ladux.identity.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.akira.ladux.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Fetch san user + roles de sinh access token sau khi transaction ket thuc khong bi lazy loi.
    @EntityGraph(attributePaths = {"user", "user.roles"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user.id = :userId and r.revoked = false")
    int revokeAllByUserId(@Param("userId") Integer userId);
}
