export type Product = {
  id: string;
  name: string;
  description: string;
  price: number;
  availableStock: number;
  imageUrl: string;
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

export async function getProducts(): Promise<Product[]> {
  const response = await fetch(`${API_BASE_URL}/api/products`, {
    cache: "no-store",
  });

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
