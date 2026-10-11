import apiClient from './apiClient';
import { UserResponse } from './userService';
import { env } from '../config/env';

const API_BASE_URL = env.apiBaseUrl;
const OAUTH2_RETURN_TO_KEY = 'ladux.oauth2.returnTo';

function normalizeReturnTo(returnTo?: string): string {
  if (!returnTo || !returnTo.startsWith('/') || returnTo.startsWith('//')) {
    return '/account';
  }
  return returnTo;
}

function googleAuthorizationUrl(): string {
  const apiUrl = new URL(API_BASE_URL, window.location.origin);
  return new URL('/oauth2/authorization/google', apiUrl.origin).toString();
}

function storeOAuth2ReturnTo(returnTo?: string): void {
  try {
    sessionStorage.setItem(OAUTH2_RETURN_TO_KEY, normalizeReturnTo(returnTo));
  } catch {
    // OAuth2 vẫn hoạt động nếu trình duyệt chặn sessionStorage; callback sẽ về /account.
  }
}

function takeOAuth2ReturnTo(): string {
  try {
    const returnTo = normalizeReturnTo(sessionStorage.getItem(OAUTH2_RETURN_TO_KEY) || undefined);
    sessionStorage.removeItem(OAUTH2_RETURN_TO_KEY);
    return returnTo;
  } catch {
    return '/account';
  }
}

function removeOAuth2ReturnTo(): void {
  try {
    sessionStorage.removeItem(OAUTH2_RETURN_TO_KEY);
  } catch {
    // Không cần chặn luồng đăng nhập nếu storage không khả dụng.
  }
}

export interface RegisterRequest {
  fullName: string;
  username: string;
  email?: string;
  password: string;
  phone?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  captchaToken?: string;
}

export interface LoginResponse {
  message: string;
  userId: string;
  username: string;
  accessToken: string;
  tokenType: 'Bearer';
}

export interface RefreshResponse {
  message: string;
  accessToken: string;
  tokenType: 'Bearer';
}

export const authService = {
  /**
   * Chuyển trình duyệt tới Spring Security để bắt đầu Authorization Code flow.
   * Điểm đến sau đăng nhập được lưu trong sessionStorage, không đưa vào OAuth state/token URL.
  */
  startGoogleLogin: (returnTo?: string): void => {
    storeOAuth2ReturnTo(returnTo);
    window.location.assign(googleAuthorizationUrl());
  },

  consumeOAuth2ReturnTo: takeOAuth2ReturnTo,

  clearOAuth2ReturnTo: removeOAuth2ReturnTo,

  /**
   * Đăng ký tài khoản người dùng mới
   * POST /api/v1/auth/register
   */
  register: (data: RegisterRequest): Promise<UserResponse> => {
    return apiClient.post('/auth/register', data);
  },

  /**
   * Đăng nhập: access token trả trong body, refresh token nằm trong HttpOnly cookie.
   * POST /api/v1/auth/login
   */
  login: (data: LoginRequest): Promise<LoginResponse> => {
    return apiClient.post('/auth/login', data);
  },

  /**
   * Làm mới AccessToken bằng RefreshToken cookie
   * POST /api/v1/auth/refresh
   */
  refresh: (): Promise<RefreshResponse> => {
    return apiClient.post('/auth/refresh');
  },

  /**
   * Đăng xuất hệ thống (revoke session và xóa refresh-token cookie)
   * POST /api/v1/auth/logout
   */
  logout: (): Promise<void> => {
    return apiClient.post('/auth/logout');
  },
};
