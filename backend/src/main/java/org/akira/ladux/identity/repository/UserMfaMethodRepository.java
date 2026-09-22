package org.akira.ladux.identity.repository;

import java.util.Optional;
import org.akira.ladux.identity.entity.UserMfaMethod;
import org.akira.ladux.identity.enums.MfaType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMfaMethodRepository extends JpaRepository<UserMfaMethod, Long> {

    Optional<UserMfaMethod> findFirstByUserIdAndTypeAndEnabledTrueOrderByIdDesc(
            Integer userId,
            MfaType type
    );
}
