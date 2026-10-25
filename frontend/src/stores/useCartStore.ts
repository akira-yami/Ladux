import { create } from 'zustand';
import { cartService, CartResponse, CartItemRequest, CartItemResponse } from '@/services';

interface CartState {
  cart: CartResponse | null;
  isLoading: boolean;
  error: string | null;
  totalItems: number;
  totalAmount: number;
  selectedItemIds: number[]; // ID các CartItemResponse được chọn cho checkout

  // Actions
  fetchCart: () => Promise<void>;
  addToCart: (data: CartItemRequest) => Promise<void>;
  updateQuantity: (productId: number, quantity: number) => Promise<void>;
  removeItem: (productId: number) => Promise<void>;
  clearCart: () => Promise<void>;
  toggleSelectItem: (itemId: number) => void;
  selectAllItems: () => void;
  deselectAllItems: () => void;
  getSelectedItems: () => CartItemResponse[];
  getSelectedTotalAmount: () => number;
  clearError: () => void;
  reset: () => void;
}

export const useCartStore = create<CartState>((set, get) => ({
  cart: null,
  isLoading: false,
  error: null,
  totalItems: 0,
  totalAmount: 0,
  selectedItemIds: [],

  clearError: () => set({ error: null }),

  fetchCart: async () => {
    set({ isLoading: true, error: null });
    try {
      const cartData = await cartService.getCart();
      const itemsCount = cartData.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;
      
      // Mặc định chọn tất cả item nếu chưa có item nào được chọn
      const currentSelected = get().selectedItemIds;
      const validSelected = (cartData.items || [])
        .filter((item) => currentSelected.includes(item.id))
        .map((item) => item.id);

      set({ 
        cart: cartData, 
        totalItems: itemsCount,
        totalAmount: cartData.totalPrice || 0,
        selectedItemIds: validSelected.length > 0 ? validSelected : (cartData.items || []).map(i => i.id)
      });
    } catch (error: any) {
      console.error('Lỗi fetch cart:', error);
      const message = error?.response?.data?.message || 'Không thể tải thông tin giỏ hàng!';
      set({ error: message });
    } finally {
      set({ isLoading: false });
    }
  },

  addToCart: async (data) => {
    set({ isLoading: true, error: null });
    try {
      await cartService.addItemToCart(data);
      await get().fetchCart();
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Thêm vào giỏ hàng thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  updateQuantity: async (productId, quantity) => {
    set({ isLoading: true, error: null });
    try {
      if (quantity <= 0) {
        await get().removeItem(productId);
        return;
      }
      await cartService.updateQuantity(productId, quantity);
      await get().fetchCart();
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Cập nhật số lượng thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  removeItem: async (productId) => {
    set({ isLoading: true, error: null });
    try {
      await cartService.removeItemFromCart(productId);
      await get().fetchCart();
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Xóa sản phẩm khỏi giỏ hàng thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  clearCart: async () => {
    set({ isLoading: true, error: null });
    try {
      await cartService.clearCart();
      set({ cart: null, totalItems: 0, totalAmount: 0, selectedItemIds: [] });
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Xóa toàn bộ giỏ hàng thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  toggleSelectItem: (itemId) => {
    const { selectedItemIds } = get();
    if (selectedItemIds.includes(itemId)) {
      set({ selectedItemIds: selectedItemIds.filter((id) => id !== itemId) });
    } else {
      set({ selectedItemIds: [...selectedItemIds, itemId] });
    }
  },

  selectAllItems: () => {
    const { cart } = get();
    if (!cart?.items) return;
    set({ selectedItemIds: cart.items.map((item) => item.id) });
  },

  deselectAllItems: () => {
    set({ selectedItemIds: [] });
  },

  getSelectedItems: () => {
    const { cart, selectedItemIds } = get();
    if (!cart?.items) return [];
    return cart.items.filter((item) => selectedItemIds.includes(item.id));
  },

  getSelectedTotalAmount: () => {
    const selectedItems = get().getSelectedItems();
    return selectedItems.reduce((sum, item) => {
      const price = Number(item.productVariant?.discountPrice ?? item.productVariant?.price ?? 0);
      return sum + price * item.quantity;
    }, 0);
  },

  reset: () => {
    set({
      cart: null,
      isLoading: false,
      error: null,
      totalItems: 0,
      totalAmount: 0,
      selectedItemIds: [],
    });
  },
}));
