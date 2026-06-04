export type Product = {
  id: string;
  name: string;
  description: string;
  price: number;
  availableStock: number;
  imageUrl: string;
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
