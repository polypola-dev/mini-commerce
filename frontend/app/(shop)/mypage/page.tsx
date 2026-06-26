"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";
import { getCart, getMyOrders } from "@/lib/api";
import type { OrderResponse } from "@/lib/api";
import { useWishlist } from "@/lib/wishlist";

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: "결제 대기",
  PAID: "결제 완료",
  PAYMENT_FAILED: "결제 실패",
  CANCELED: "취소됨",
  SHIPPED: "배송 중",
  DELIVERED: "배송 완료",
};

const STATUS_COLOR: Record<string, string> = {
  PENDING_PAYMENT: "var(--color-muted)",
  PAID: "var(--color-primary)",
  PAYMENT_FAILED: "var(--color-error, #c13515)",
  CANCELED: "var(--color-muted)",
  SHIPPED: "var(--color-primary)",
  DELIVERED: "var(--color-ink)",
};

function displayLabel(
  user:
    | { email?: string | null; user_metadata?: { email?: string; name?: string; nickname?: string } }
    | null
    | undefined
) {
  if (!user) return null;
  return user.email || user.user_metadata?.email || user.user_metadata?.name || user.user_metadata?.nickname || "사용자";
}

export default function MyPage() {
  const router = useRouter();
  const [label, setLabel] = useState<string | null | undefined>(undefined);
  const [cartCount, setCartCount] = useState(0);
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [orderImages, setOrderImages] = useState<Record<string, string | null>>({});
  const { ids: wishIds } = useWishlist();

  useEffect(() => {
    const supabase = createClient();
    supabase.auth.getUser().then(({ data: { user } }) => {
      if (!user) {
        router.replace("/login");
        return;
      }
      setLabel(displayLabel(user));
    });
  }, [router]);

  useEffect(() => {
    getCart()
      .then((cart) => setCartCount(cart.items.reduce((a, i) => a + i.quantity, 0)))
      .catch(() => setCartCount(0));
  }, []);

  useEffect(() => {
    getMyOrders()
      .then(async (data) => {
        const recent = data.slice(0, 10);
        setOrders(recent);
        const ids = recent.map((o) => o.lines?.[0]?.productId).filter(Boolean) as string[];
        const unique = [...new Set(ids)];
        const entries = await Promise.all(
          unique.map(async (id) => {
            try {
              const res = await fetch(`/api/proxy/products/${id}`);
              if (!res.ok) return [id, null] as const;
              const p = await res.json();
              return [id, p.imageUrl ?? null] as const;
            } catch {
              return [id, null] as const;
            }
          })
        );
        setOrderImages(Object.fromEntries(entries));
      })
      .catch(() => {});
  }, []);

  async function handleLogout() {
    const supabase = createClient();
    await supabase.auth.signOut();
    router.push("/login");
    router.refresh();
  }

  if (!label) return null;

  return (
    <div>
      <Link href="/mypage/profile" className="mcProfileHeader" style={{ textDecoration: "none", color: "inherit" }}>
        <div className="mcAvatar">{label[0]?.toUpperCase()}</div>
        <div>
          <div style={{ fontSize: "18px", fontWeight: 700 }}>{label}</div>
        </div>
      </Link>

      {orders.length > 0 && (
        <div style={{ padding: "20px 0 8px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "0 20px 12px" }}>
            <div style={{ display: "flex", alignItems: "baseline", gap: "6px" }}>
              <span style={{ fontSize: "16px", fontWeight: 700 }}>주문/배송내역</span>
              <span style={{ fontSize: "12px", color: "var(--color-muted)" }}>옆으로 밀어보세요</span>
            </div>
            <Link href="/orders" style={{ fontSize: "13px", color: "var(--color-primary)", textDecoration: "none" }}>
              전체보기 ›
            </Link>
          </div>
          <div
            style={{
              display: "flex",
              gap: "12px",
              overflowX: "auto",
              padding: "0 20px 8px",
              scrollbarWidth: "none",
              msOverflowStyle: "none",
            }}
          >
            {orders.map((order) => {
              const firstLine = order.lines?.[0];
              const extraCount = (order.lines?.length ?? 0) - 1;
              const imgUrl = orderImages[firstLine?.productId ?? ""] ?? null;
              const statusLabel = STATUS_LABEL[order.status] ?? order.status;
              const statusColor = STATUS_COLOR[order.status] ?? "var(--color-ink)";
              return (
                <Link
                  key={order.orderId}
                  href={`/orders/${order.orderId}`}
                  style={{ textDecoration: "none", color: "inherit", flexShrink: 0 }}
                >
                  <div
                    style={{
                      width: 150,
                      border: "1px solid var(--color-hairline)",
                      borderRadius: "14px",
                      padding: "12px",
                      display: "flex",
                      flexDirection: "column",
                      gap: "8px",
                      background: "var(--color-surface, #fff)",
                    }}
                  >
                    <div
                      style={{
                        width: "100%",
                        aspectRatio: "1",
                        borderRadius: "10px",
                        overflow: "hidden",
                        background: "var(--color-hairline-soft, #f5f5f5)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                      }}
                    >
                      {imgUrl ? (
                        <img
                          src={imgUrl}
                          alt={firstLine?.productName ?? ""}
                          style={{ width: "100%", height: "100%", objectFit: "cover" }}
                        />
                      ) : (
                        <span style={{ fontSize: "36px" }}>🛍️</span>
                      )}
                    </div>
                    <div style={{ fontSize: "13px", fontWeight: 700, color: statusColor }}>{statusLabel}</div>
                    <div style={{ fontSize: "12px", color: "var(--color-body)", lineHeight: 1.3, display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
                      {firstLine?.productName ?? "-"}
                      {extraCount > 0 ? ` 외 ${extraCount}건` : ""}
                    </div>
                    <button
                      type="button"
                      onClick={(e) => { e.preventDefault(); window.location.href = "/cart"; }}
                      style={{
                        width: "100%",
                        border: "1px solid var(--color-hairline)",
                        borderRadius: "8px",
                        padding: "7px 0",
                        fontSize: "12px",
                        background: "transparent",
                        cursor: "pointer",
                        color: "var(--color-body)",
                      }}
                    >
                      재구매
                    </button>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      )}

      <div className="mcStatsRow">
        <Link href="/cart" className="mcStatItem" style={{ textDecoration: "none", color: "inherit" }}>
          <div className="mcStatValue">{cartCount}</div>
          <div className="mcStatLabel">장바구니</div>
        </Link>
        <Link href="/wishlist" className="mcStatItem" style={{ textDecoration: "none", color: "inherit" }}>
          <div className="mcStatValue">{wishIds.length}</div>
          <div className="mcStatLabel">찜</div>
        </Link>
      </div>

      <div>
        <Link href="/orders" className="mcMenuItem">
          주문 내역<span className="mcMenuChevron">›</span>
        </Link>
        <Link href="/mypage/address" className="mcMenuItem">
          배송지 관리<span className="mcMenuChevron">›</span>
        </Link>
        <div className="mcMenuItem" style={{ color: "var(--color-muted-soft)", cursor: "default" }}>
          쿠폰 / 적립금<span className="mcMenuChevron">·</span>
        </div>
        <Link href="/customer-service" className="mcMenuItem">
          고객센터<span className="mcMenuChevron">›</span>
        </Link>
        <button type="button" className="mcMenuItem" onClick={handleLogout} style={{ color: "var(--color-muted)" }}>
          로그아웃
        </button>
      </div>
    </div>
  );
}
