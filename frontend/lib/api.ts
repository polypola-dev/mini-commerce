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
  }>;
};

export type OrderResponse = {
  orderId: string;
  status: string;
  totalAmount: number;
};

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:18080";

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
