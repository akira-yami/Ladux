package org.akira.ladux.identity.service.impl;

/** Test-only bridge to the package-private secret policy of {@link JwtServiceImpl}. */
public final class JwtSecretPolicyAccess {
    private JwtSecretPolicyAccess() {
    }

    public static byte[] resolve(String secret) {
        return JwtServiceImpl.resolveKeyBytes(secret);
    }
}
