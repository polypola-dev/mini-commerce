import "server-only";
import { createClient } from "@/lib/supabase/server";
import type { NotificationItem, OrderResponse } from "@/lib/api";

// Server Component(Node 런타임)는 상대경로 fetch를 지원하지 않으므로
// 로그인 세션이 필요한 호출은 항상 백엔드 절대경로로 직접 호출한다.
// 이 모듈은 서버 전용이며 클라이언트 컴포넌트에서 import하면 빌드 에러가 난다.
const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:18080";
const BFF_SECRET_KEY = process.env.BFF_SECRET_KEY;

async function authedFetch(path: string): Promise<Response> {
  if (!BFF_SECRET_KEY) {
    return new Response("Server misconfiguration", { status: 500 });
  }
  const supabase = await createClient();
  const { data: { session } } = await supabase.auth.getSession();
  if (!session) {
    return new Response("Unauthorized", { status: 401 });
  }
  return fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "X-Internal-BFF-Key": BFF_SECRET_KEY,
      Authorization: `Bearer ${session.access_token}`,
    },
    cache: "no-store",
  });
}

export async function getNotifications(): Promise<NotificationItem[]> {
  const response = await authedFetch("/api/notifications");
  if (!response.ok) throw new Error("Failed to fetch notifications");
  return response.json();
}

export async function getMyOrders(): Promise<OrderResponse[]> {
  const response = await authedFetch("/api/orders");
  if (!response.ok) throw new Error("Failed to fetch orders");
  return response.json();
}

export async function getOrderById(orderId: string): Promise<OrderResponse> {
  const response = await authedFetch(`/api/orders/${orderId}`);
  if (response.status === 404) {
    const err = new Error("Order not found") as Error & { status: number };
    err.status = 404;
    throw err;
  }
  if (!response.ok) {
    const err = new Error(`Failed to fetch order: ${response.status}`) as Error & { status: number };
    err.status = response.status;
    throw err;
  }
  return response.json();
}

export async function getProductImages(
  productIds: string[]
): Promise<Record<string, string | null>> {
  const unique = [...new Set(productIds)];
  const entries = await Promise.all(
    unique.map(async (id) => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/products/${id}`, { cache: "no-store" });
        if (!res.ok) return [id, null] as const;
        const p = await res.json();
        return [id, (p.imageUrl as string) ?? null] as const;
      } catch {
        return [id, null] as const;
      }
    })
  );
  return Object.fromEntries(entries);
}
