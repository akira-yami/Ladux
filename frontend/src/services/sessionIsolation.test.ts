import { afterEach, describe, expect, it, vi } from 'vitest';
import type { AxiosAdapter, AxiosResponse } from 'axios';
import apiClient from './apiClient';
import adminApiClient from '../admin/api/adminApiClient';
import { setStorefrontAccessToken, setAdminAccessToken } from './authTokens';

// These interceptor tests use in-memory adapters and must not load a developer's .env.
vi.mock('../config/env', () => ({
  env: { apiBaseUrl: 'https://api.ladux.test/api/v1' },
}));

const storefrontAdapter = apiClient.defaults.adapter;
const adminAdapter = adminApiClient.defaults.adapter;
afterEach(() => {
  apiClient.defaults.adapter = storefrontAdapter;
  adminApiClient.defaults.adapter = adminAdapter;
  setStorefrontAccessToken(null);
  setAdminAccessToken(null);
});

describe('account response isolation', () => {
  it.each(['storefront', 'admin'] as const)('rejects an old %s response after logout/account change', async kind => {
    const client = kind === 'storefront' ? apiClient : adminApiClient;
    const setToken = kind === 'storefront' ? setStorefrontAccessToken : setAdminAccessToken;
    setToken('account-A');
    let complete: (() => void) | undefined;
    const adapter: AxiosAdapter = config => new Promise<AxiosResponse>(resolve => {
      complete = () => resolve({ data: { privateAddress: 'account-A-only' }, status: 200, statusText: 'OK', headers: {}, config });
    });
    client.defaults.adapter = adapter;
    const pending = client.get('/user-addresses');
    const assertion = expect(pending).rejects.toMatchObject({ code: 'ERR_CANCELED' });
    await new Promise(resolve => setTimeout(resolve, 0));
    setToken(null);
    setToken('account-B');
    expect(complete).toBeTypeOf('function');
    complete!();
    await assertion;
  });

  it('keeps a response from the current account', async () => {
    setStorefrontAccessToken('account-A');
    apiClient.defaults.adapter = async config => ({ data: { id: 42 }, status: 200, statusText: 'OK', headers: {}, config });
    await expect(apiClient.get('/users/me')).resolves.toEqual({ id: 42 });
  });
});
