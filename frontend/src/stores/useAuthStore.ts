import { create } from 'zustand';
import {
  authService,
  customerService,
  userService,
  LoginRequest,
  RegisterRequest,
  UserResponse,
  UserUpdatePasswordRequest,
  PersonalInformationUpdateRequest,
  getCaptchaToken,
} from '@/services';
import { setStorefrontAccessToken, getStorefrontSessionEpoch } from '@/services/authTokens';

interface AuthState {
  user: UserResponse | null;
  accessToken: string | null;
  isLoggedIn: boolean;
  isLoading: boolean;
  error: string | null;

  // Actions
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<UserResponse>;
  logout: () => Promise<void>;
  fetchCurrentUser: () => Promise<void>;
  updatePersonalInformation: (data: PersonalInformationUpdateRequest) => Promise<void>;
  changePassword: (data: UserUpdatePasswordRequest) => Promise<void>;
  uploadAvatar: (file: File) => Promise<void>;
  setAccessToken: (token: string | null) => void;
  clearSession: () => void;
  clearError: () => void;
  isAdmin: () => boolean;
  isStaff: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: null,
  isLoggedIn: false,
  isLoading: false,
  error: null,

  setAccessToken: (token) => {
    setStorefrontAccessToken(token);
    set({ accessToken: token, isLoggedIn: !!token });
  },

  clearSession: () => {
    setStorefrontAccessToken(null);
    set({ accessToken: null, user: null, isLoggedIn: false, isLoading: false, error: null });
  },

  clearError: () => set({ error: null }),

  login: async (credentials) => {
    const epoch = getStorefrontSessionEpoch();
    set({ isLoading: true, error: null });
    try {
      const captchaToken = await getCaptchaToken('login');
      const response = await authService.login({ ...credentials, captchaToken });
      get().setAccessToken(response.accessToken);
      await get().fetchCurrentUser();
      if (!get().isLoggedIn) throw new Error('Không thể xác nhận phiên đăng nhập. Vui lòng thử lại.');
    } catch (err: any) {
      if (epoch !== getStorefrontSessionEpoch()) throw err;
      const message = err?.response?.data?.message || err?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin!';
      setStorefrontAccessToken(null);
      set({ error: message, accessToken: null, isLoggedIn: false, user: null });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  register: async (data) => {
    set({ isLoading: true, error: null });
    try {
      const res = await authService.register(data);
      return res;
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Đăng ký thất bại. Vui lòng thử lại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  logout: async () => {
    set({ isLoading: true, error: null });
    try {
      await authService.logout();
    } catch (error) {
      console.error('Lỗi khi logout backend:', error);
    } finally {
      get().clearSession();
    }
  },

  fetchCurrentUser: async () => {
    const epoch = getStorefrontSessionEpoch();
    set({ isLoading: true, error: null });
    try {
      if (!get().accessToken) {
        const response = await authService.refresh();
        get().setAccessToken(response.accessToken);
      }
      const userData = await userService.getCurrentUser();
      if (epoch !== getStorefrontSessionEpoch()) return;
      set({ user: userData, isLoggedIn: true });
    } catch (error) {
      if (epoch !== getStorefrontSessionEpoch()) return;
      // Nếu session không hợp lệ hoặc hết hạn thì clear session state
      get().clearSession();
    } finally {
      if (epoch === getStorefrontSessionEpoch()) set({ isLoading: false });
    }
  },

  updatePersonalInformation: async (data) => {
    set({ isLoading: true, error: null });
    try {
      const updatedUser = await customerService.updatePersonalInformation(data);
      set({ user: updatedUser });
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Cập nhật họ và tên thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  changePassword: async (data) => {
    set({ isLoading: true, error: null });
    try {
      await userService.changePassword(data);
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Đổi mật khẩu thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  uploadAvatar: async (file) => {
    set({ isLoading: true, error: null });
    try {
      const updatedUser = await userService.uploadAvatar(file);
      set({ user: updatedUser });
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Tải ảnh đại diện thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  isAdmin: () => {
    const user = get().user;
    if (!user || !user.roles) return false;
    return user.roles.some((role) => role.toUpperCase() === 'ROLE_ADMIN' || role.toUpperCase() === 'ADMIN');
  },

  isStaff: () => {
    const user = get().user;
    if (!user || !user.roles) return false;
    return user.roles.some(
      (role) => role.toUpperCase() === 'ROLE_STAFF' || role.toUpperCase() === 'STAFF' || role.toUpperCase() === 'ROLE_ADMIN'
    );
  },
}));
