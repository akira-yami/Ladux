package org.akira.ladux.identity.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.akira.ladux.identity.entity.EmailVerification;
import org.akira.ladux.identity.enums.EmailVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification>
    findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(
            Integer customerId,
            EmailVerificationPurpose purpose
    );

    boolean existsByVerificationIdAndCustomerId(
            String verificationId,
            Integer customerId
    );

    @Modifying
    @Query("""
        update EmailVerification verification
           set verification.status = :status
         where verification.customerId = :customerId
           and verification.purpose = :purpose
           and (
               verification.status = org.akira.ladux.identity.enums.EmailVerificationStatus.PENDING
               or verification.status = org.akira.ladux.identity.enums.EmailVerificationStatus.VERIFIED
           )
           and verification.consumedAt is null
    """)
    int invalidateActiveByCustomerIdAndPurpose(
            @Param("customerId") Integer customerId,
            @Param("purpose") EmailVerificationPurpose purpose,
            @Param("status") org.akira.ladux.identity.enums.EmailVerificationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select verification
          from EmailVerification verification
         where verification.verificationId = :verificationId
           and verification.customerId = :customerId
    """)
    Optional<EmailVerification> findForUpdate(
            @Param("verificationId") String verificationId,
            @Param("customerId") Integer customerId
    );
}
