import { create } from 'zustand';
import {
  productService,
  brandService,
  categoryService,
  ProductResponse,
  BrandResponse,
  CategoryResponse,
  ProductRequest,
} from '@/services';

interface ProductFilterParams {
  search: string;
  categoryId: number | null;
  brandId: number | null;
  page: number;
  size: number;
  sort: string;
}

interface ProductState {
  products: ProductResponse[];
  totalElements: number;
  totalPages: number;
  selectedProduct: ProductResponse | null;
  
  brands: BrandResponse[];
  categories: CategoryResponse[];
  
  filters: ProductFilterParams;
  isLoading: boolean;
  error: string | null;

  // Actions
  fetchProducts: () => Promise<void>;
  fetchProductById: (id: number) => Promise<ProductResponse | null>;
  fetchBrands: () => Promise<void>;
  fetchCategories: () => Promise<void>;
  
  // Filter Updaters
  setSearch: (search: string) => void;
  setCategoryFilter: (categoryId: number | null) => void;
  setBrandFilter: (brandId: number | null) => void;
  setPage: (page: number) => void;
  setSize: (size: number) => void;
  setSort: (sort: string) => void;
  resetFilters: () => void;
  
  // Admin Actions
  createProduct: (data: ProductRequest) => Promise<ProductResponse>;
  updateProduct: (id: number, data: ProductRequest) => Promise<ProductResponse>;
  deleteProduct: (id: number) => Promise<void>;
  
  clearError: () => void;
}

const initialFilters: ProductFilterParams = {
  search: '',
  categoryId: null,
  brandId: null,
  page: 0,
  size: 24,
  sort: 'createdAt,desc',
};

export const useProductStore = create<ProductState>((set, get) => ({
  products: [],
  totalElements: 0,
  totalPages: 0,
  selectedProduct: null,
  brands: [],
  categories: [],
  filters: { ...initialFilters },
  isLoading: false,
  error: null,

  clearError: () => set({ error: null }),

  fetchProducts: async () => {
    set({ isLoading: true, error: null });
    try {
      const res = await productService.getAllProducts({
        page: get().filters.page,
        size: Math.min(50, get().filters.size),
        sort: get().filters.sort,
        search: get().filters.search || undefined,
        brandId: get().filters.brandId ?? undefined,
        categoryId: get().filters.categoryId ?? undefined,
      });

      set({
        products: res.content || [],
        totalElements: res.totalElements || 0,
        totalPages: res.totalPages || 0,
      });
    } catch (err: any) {
      console.error('Lỗi fetch products:', err);
      const message = err?.response?.data?.message || 'Không thể tải danh sách sản phẩm!';
      set({ error: message });
    } finally {
      set({ isLoading: false });
    }
  },

  fetchProductById: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const product = await productService.getProductById(id);
      set({ selectedProduct: product });
      return product;
    } catch (err: any) {
      console.error('Lỗi fetch product by id:', err);
      const message = err?.response?.data?.message || 'Không thể tải chi tiết sản phẩm!';
      set({ error: message, selectedProduct: null });
      return null;
    } finally {
      set({ isLoading: false });
    }
  },

  fetchBrands: async () => {
    try {
      const res = await brandService.getAllBrands({ page: 0, size: 100 });
      set({ brands: res.content || [] });
    } catch (err) {
      console.error('Lỗi fetch brands:', err);
    }
  },

  fetchCategories: async () => {
    try {
      const res = await categoryService.getAllCategories({ page: 0, size: 100 });
      set({ categories: res.content || [] });
    } catch (err) {
      console.error('Lỗi fetch categories:', err);
    }
  },

  setSearch: (search: string) => {
    set((state) => ({
      filters: { ...state.filters, search, page: 0 },
    }));
    get().fetchProducts();
  },

  setCategoryFilter: (categoryId: number | null) => {
    set((state) => ({
      filters: { ...state.filters, categoryId, page: 0 },
    }));
    get().fetchProducts();
  },

  setBrandFilter: (brandId: number | null) => {
    set((state) => ({
      filters: { ...state.filters, brandId, page: 0 },
    }));
    get().fetchProducts();
  },

  setPage: (page: number) => {
    set((state) => ({
      filters: { ...state.filters, page },
    }));
    get().fetchProducts();
  },

  setSize: (size: number) => {
    set((state) => ({
      filters: { ...state.filters, size, page: 0 },
    }));
    get().fetchProducts();
  },

  setSort: (sort: string) => {
    set((state) => ({
      filters: { ...state.filters, sort, page: 0 },
    }));
    get().fetchProducts();
  },

  resetFilters: () => {
    set({ filters: { ...initialFilters } });
    get().fetchProducts();
  },

  createProduct: async (data: ProductRequest) => {
    set({ isLoading: true, error: null });
    try {
      const newProduct = await productService.createProduct(data);
      await get().fetchProducts();
      return newProduct;
    } catch (err: any) {
      const message = err?.response?.data?.message || 'Tạo sản phẩm thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  updateProduct: async (id: number, data: ProductRequest) => {
    set({ isLoading: true, error: null });
    try {
      const updated = await productService.updateProduct(id, data);
      await get().fetchProducts();
      if (get().selectedProduct?.id === id) {
        set({ selectedProduct: updated });
      }
      return updated;
    } catch (err: any) {
      const message = err?.response?.data?.message || 'Cập nhật sản phẩm thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  deleteProduct: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await productService.deleteProductById(id);
      await get().fetchProducts();
    } catch (err: any) {
      const message = err?.response?.data?.message || 'Xóa sản phẩm thất bại!';
      set({ error: message });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },
}));
