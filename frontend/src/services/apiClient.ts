import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { env } from "../config/env";
import { getStorefrontAccessToken, setStorefrontAccessToken, getStorefrontSessionEpoch } from "./authTokens";

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  _sessionEpoch?: number;
}

interface AccessTokenResponse {
  accessToken: string;
}

const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
  timeout: 20_000,
  headers: {
    Accept: "application/json",
  },
});

let refreshPromise: Promise<string> | null = null;

function redirectAfterSessionExpired(): void {
  setStorefrontAccessToken(null);
  window.dispatchEvent(new CustomEvent("ladux:auth-expired"));
  if (window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
}

function refreshSession(): Promise<string> {
  if (!refreshPromise) {
    const epoch = getStorefrontSessionEpoch();
    refreshPromise = axios
      .post<AccessTokenResponse>(`${env.apiBaseUrl}/auth/refresh`, undefined, {
        withCredentials: true,
        timeout: 15_000,
        headers: { Accept: "application/json" },
      })
      .then((response) => {
        if (epoch !== getStorefrontSessionEpoch()) throw new axios.CanceledError('Session changed');
        setStorefrontAccessToken(response.data.accessToken);
        return response.data.accessToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

apiClient.interceptors.request.use((config) => {
  (config as RetryableRequestConfig)._sessionEpoch = getStorefrontSessionEpoch();
  const accessToken = getStorefrontAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  } else {
    delete config.headers.Authorization;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    if ((response.config as RetryableRequestConfig)._sessionEpoch !== getStorefrontSessionEpoch())
      throw new axios.CanceledError('Session changed');
    return response.data;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    if (originalRequest && originalRequest._sessionEpoch !== getStorefrontSessionEpoch())
      return Promise.reject(new axios.CanceledError('Session changed'));
    const url = originalRequest?.url ?? "";
    const isAuthEndpoint = ["/auth/login", "/auth/register", "/auth/refresh", "/auth/logout"].some(
      (endpoint) => url.includes(endpoint),
    );

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true;

      try {
        await refreshSession();
        return apiClient(originalRequest);
      } catch (refreshError) {
        if (axios.isAxiosError(refreshError) && [401, 403].includes(refreshError.response?.status ?? 0))
          redirectAfterSessionExpired();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  },
);

export default apiClient;
