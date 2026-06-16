export type ProductOption = {
  id: string;
  optionGroupName: string;
  optionValue: string;
  additionalPrice: number;
};

export type CartItem = {
  itemId: string;
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  selectedOptionValue: string | null;
};

export type Cart = {
  customerId: string;
  items: CartItem[];
  totalAmount: number;
};

export type AddCartItemRequest = {
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  selectedOptionId?: string;
};

export type UpdateCartItemRequest = {
  quantity: number;
};

export type Product = {
  id: string;
  name: string;
  description: string;
  price: number;
  availableStock: number;
  imageUrl: string;
  active: boolean;
  options: ProductOption[];
};

export type Review = {
  id: string;
  productId: string;
  authorId: string;
  rating: number;
  content: string;
  createdAt: string;
};

export type ReviewListResponse = {
  reviews: Review[];
  averageRating: number;
  totalCount: number;
};

export type CreateReviewRequest = {
  productId: string;
  rating: number;
  content: string;
};

export type CreateOrderRequest = {
  items: Array<{
    productId: string;
    quantity: number;
    selectedOptionId?: string;
  }>;
  shippingRecipient?: string;
  shippingPhone?: string;
  shippingAddress?: string;
  shippingDetailAddress?: string;
  shippingZipCode?: string;
};

export type OrderLineItem = {
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  selectedOptionValue: string | null;
};

export type OrderResponse = {
  orderId: string;
  customerId?: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  lines: OrderLineItem[];
  shippingRecipient: string | null;
  shippingPhone: string | null;
  shippingAddress: string | null;
  shippingDetailAddress: string | null;
  shippingZipCode: string | null;
};

export type ShippingInfo = {
  shippingRecipient: string;
  shippingPhone: string;
  shippingAddress: string;
  shippingDetailAddress: string;
  shippingZipCode: string;
};

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:18080";

export async function getProducts(q?: string): Promise<Product[]> {
  const url = new URL(`${API_BASE_URL}/api/products`);
  if (q) url.searchParams.set("q", q);

  const response = await fetch(url.toString(), { cache: "no-store" });

  if (!response.ok) {
    throw new Error("Failed to fetch products");
  }

  return response.json();
}

export async function createOrder(request: CreateOrderRequest): Promise<OrderResponse> {
  const response = await fetch("/api/proxy/orders", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to create order");
  }

  return response.json();
}

export async function completeFakePayment(orderId: string): Promise<OrderResponse> {
  const response = await fetch(`/api/proxy/orders/${orderId}/complete-payment`, {
    method: "POST",
  });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to complete payment");
  }

  return response.json();
}

export async function getReviews(productId: string): Promise<ReviewListResponse> {
  const response = await fetch(`/api/proxy/products/${productId}/reviews`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error("Failed to fetch reviews");
  }

  return response.json();
}

export async function createReview(request: CreateReviewRequest): Promise<Review> {
  const response = await fetch("/api/proxy/reviews", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to create review");
  }

  return response.json();
}

export async function deleteReview(reviewId: string): Promise<void> {
  const response = await fetch(`/api/proxy/reviews/${reviewId}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to delete review");
  }
}

export async function getCart(): Promise<Cart> {
  const response = await fetch("/api/proxy/cart", { cache: "no-store" });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to fetch cart");
  }

  return response.json();
}

export async function addToCart(request: AddCartItemRequest): Promise<CartItem> {
  const response = await fetch("/api/proxy/cart", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to add to cart");
  }

  return response.json();
}

export async function updateCartItem(itemId: string, request: UpdateCartItemRequest): Promise<Cart> {
  const response = await fetch(`/api/proxy/cart/items/${itemId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to update cart item");
  }

  return response.json();
}

export async function removeCartItem(itemId: string): Promise<void> {
  const response = await fetch(`/api/proxy/cart/items/${itemId}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to remove cart item");
  }
}

export async function clearCart(): Promise<void> {
  const response = await fetch("/api/proxy/cart", { method: "DELETE" });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || "Failed to clear cart");
  }
}

export type NotificationItem = {
  id: string;
  orderId: string;
  customerId: string;
  type: string;
  status: string;
  message: string;
  createdAt: string;
  sentAt: string | null;
};

export async function getNotifications(): Promise<NotificationItem[]> {
  const response = await fetch("/api/proxy/notifications", { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to fetch notifications");
  return response.json();
}

export async function getMyOrders(): Promise<OrderResponse[]> {
  const response = await fetch("/api/proxy/orders", { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to fetch orders");
  return response.json();
}

export async function getOrderById(orderId: string): Promise<OrderResponse> {
  const response = await fetch(`/api/proxy/orders/${orderId}`, { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to fetch order");
  return response.json();
}

export async function getProductById(productId: string): Promise<Product> {
  const response = await fetch(`/api/proxy/products/${productId}`, { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to fetch product");
  return response.json();
}

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

// Admin APIs
export type AdminProductRequest = {
  name: string;
  description: string;
  price: number;
  stock: number;
  imageUrl: string;
};

export async function adminGetProducts(params?: {
  page?: number;
  size?: number;
  q?: string;
  active?: boolean;
}): Promise<PageResponse<Product>> {
  const url = new URL("/api/proxy/admin/products", "http://localhost");
  if (params?.page !== undefined) url.searchParams.set("page", String(params.page));
  if (params?.size !== undefined) url.searchParams.set("size", String(params.size));
  if (params?.q) url.searchParams.set("q", params.q);
  if (params?.active !== undefined) url.searchParams.set("active", String(params.active));
  const response = await fetch(`/api/proxy/admin/products?${url.searchParams}`, { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to fetch products");
  return response.json();
}

export async function adminCreateProduct(data: AdminProductRequest): Promise<Product> {
  const response = await fetch("/api/proxy/admin/products", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const msg = await response.text();
    throw new Error(msg || "Failed to create product");
  }
  return response.json();
}

export async function adminUpdateProduct(id: string, data: AdminProductRequest): Promise<Product> {
  const response = await fetch(`/api/proxy/admin/products/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const msg = await response.text();
    throw new Error(msg || "Failed to update product");
  }
  return response.json();
}

export async function adminDeleteProduct(id: string): Promise<void> {
  const response = await fetch(`/api/proxy/admin/products/${id}`, { method: "DELETE" });
  if (!response.ok) throw new Error("Failed to delete product");
}

export async function adminGetOrders(params?: {
  page?: number;
  size?: number;
  status?: string;
  q?: string;
  sortBy?: string;
  sortDir?: string;
}): Promise<PageResponse<OrderResponse>> {
  const url = new URL("/api/proxy/admin/orders", "http://localhost");
  if (params?.page !== undefined) url.searchParams.set("page", String(params.page));
  if (params?.size !== undefined) url.searchParams.set("size", String(params.size));
  if (params?.status && params.status !== "ALL") url.searchParams.set("status", params.status);
  if (params?.q) url.searchParams.set("q", params.q);
  if (params?.sortBy) url.searchParams.set("sortBy", params.sortBy);
  if (params?.sortDir) url.searchParams.set("sortDir", params.sortDir);
  const response = await fetch(`/api/proxy/admin/orders?${url.searchParams}`, { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to fetch orders");
  return response.json();
}

export async function adminUpdateOrderStatus(id: string, status: string): Promise<OrderResponse> {
  const response = await fetch(`/api/proxy/admin/orders/${id}/status`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  });
  if (!response.ok) {
    const msg = await response.text();
    throw new Error(msg || "Failed to update order status");
  }
  return response.json();
}
