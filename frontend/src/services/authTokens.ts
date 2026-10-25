let storefrontAccessToken: string | null = null;
let adminAccessToken: string | null = null;
let storefrontSessionEpoch = 0;
let adminSessionEpoch = 0;

export const getStorefrontSessionEpoch = (): number => storefrontSessionEpoch;
export const getAdminSessionEpoch = (): number => adminSessionEpoch;

export function getStorefrontAccessToken(): string | null {
  return storefrontAccessToken;
}

export function setStorefrontAccessToken(token: string | null): void {
  if (token === null) storefrontSessionEpoch++;
  storefrontAccessToken = token;
}

export function getAdminAccessToken(): string | null {
  return adminAccessToken;
}

export function setAdminAccessToken(token: string | null): void {
  if (token === null) adminSessionEpoch++;
  adminAccessToken = token;
}
