import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { env } from "../../config/env";
import { getAdminAccessToken, setAdminAccessToken, getAdminSessionEpoch } from "../../services/authTokens";

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _adminRetry?: boolean;
  _sessionEpoch?: number;
}

interface AccessTokenResponse {
  accessToken: string;
}

const adminApiClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
  timeout: 45_000,
  headers: {
    Accept: "application/json",
  },
});

let refreshPromise: Promise<string> | null = null;

function expireAdminSession(): void {
  setAdminAccessToken(null);
  window.dispatchEvent(new CustomEvent("ladux:admin-auth-expired"));
  if (window.location.pathname !== "/admin/login") {
    window.location.assign("/admin/login");
  }
}

function refreshAdminSession(): Promise<string> {
  if (!refreshPromise) {
    const epoch = getAdminSessionEpoch();
    refreshPromise = axios
      .post<AccessTokenResponse>(`${env.apiBaseUrl}/admin/auth/refresh`, undefined, {
        withCredentials: true,
        timeout: 15_000,
        headers: { Accept: "application/json" },
      })
      .then((response) => {
        if (epoch !== getAdminSessionEpoch()) throw new axios.CanceledError('Admin session changed');
        setAdminAccessToken(response.data.accessToken);
        return response.data.accessToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

adminApiClient.interceptors.request.use((config) => {
  (config as RetryableRequestConfig)._sessionEpoch = getAdminSessionEpoch();
  const accessToken = getAdminAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  } else {
    delete config.headers.Authorization;
  }
  return config;
});

adminApiClient.interceptors.response.use(
  (response) => {
    if ((response.config as RetryableRequestConfig)._sessionEpoch !== getAdminSessionEpoch())
      throw new axios.CanceledError('Admin session changed');
    return response.data;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    if (originalRequest && originalRequest._sessionEpoch !== getAdminSessionEpoch())
      return Promise.reject(new axios.CanceledError('Admin session changed'));
    const url = originalRequest?.url ?? "";
    const isAuthEndpoint = [
      "/admin/auth/login",
      "/admin/auth/refresh",
      "/admin/auth/logout",
    ].some((endpoint) => url.includes(endpoint));

    if (error.response?.status === 401 && originalRequest && !originalRequest._adminRetry && !isAuthEndpoint) {
      originalRequest._adminRetry = true;
      try {
        await refreshAdminSession();
        return adminApiClient(originalRequest);
      } catch (refreshError) {
        if (axios.isAxiosError(refreshError) && [401, 403].includes(refreshError.response?.status ?? 0))
          expireAdminSession();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  },
);

export default adminApiClient;
