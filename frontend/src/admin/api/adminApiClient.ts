import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { env } from "../../config/env";
import { getAdminAccessToken, setAdminAccessToken } from "../../services/authTokens";

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _adminRetry?: boolean;
}

interface AccessTokenResponse {
  accessToken: string;
}

const adminApiClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
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
    refreshPromise = axios
      .post<AccessTokenResponse>(`${env.apiBaseUrl}/admin/auth/refresh`, undefined, {
        withCredentials: true,
        headers: { Accept: "application/json" },
      })
      .then((response) => {
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
  const accessToken = getAdminAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  } else {
    delete config.headers.Authorization;
  }
  return config;
});

adminApiClient.interceptors.response.use(
  (response) => response.data,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
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
        expireAdminSession();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  },
);

export default adminApiClient;
