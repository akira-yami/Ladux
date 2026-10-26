/** Keep a local developer's cookie session on one origin, including gateway returns. */
export function canonicalDevUrl(
    development: boolean,
    configuredOrigin: string | undefined,
    currentUrl: string,
    loginReturnTo?: unknown,
): string | null {
    if (!development || !configuredOrigin?.trim()) return null;
    const canonical = new URL(configuredOrigin.trim());
    const loopback = ['localhost', '127.0.0.1', '[::1]'].includes(canonical.hostname);
    if ((canonical.protocol !== 'https:' && !(canonical.protocol === 'http:' && loopback))
        || canonical.username || canonical.password || canonical.pathname !== '/' || canonical.search || canonical.hash) {
        throw new Error('VITE_DEV_CANONICAL_ORIGIN phải là origin HTTPS hoặc HTTP localhost, không chứa path/query.');
    }
    const current = new URL(currentUrl);
    if (current.origin === canonical.origin) return null;
    // A protected payment return may already have been replaced by /login.
    // Recover its relative destination without permitting an external redirect.
    if (current.pathname === '/login' && typeof loginReturnTo === 'string' && loginReturnTo.startsWith('/')) {
        const destination = new URL(loginReturnTo, canonical.origin);
        if (destination.origin === canonical.origin) return destination.href;
    }
    return `${canonical.origin}${current.pathname}${current.search}${current.hash}`;
}
