DROP TABLE phone_verifications;
DROP TABLE user_mfa_methods;

ALTER TABLE login_history DROP COLUMN mfa_used;

-- Retired enum values must not remain in rows read by the application.
DELETE FROM security_events
WHERE event_type IN ('MFA_REQUIRED', 'MFA_SUCCESS', 'MFA_FAILED');
