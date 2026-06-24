import { getMyOrders, OrderResponse } from "@/lib/api";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { redirect } from "next/navigation";

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: "결제 대기",
  PAID: "결제 완료",
  PAYMENT_FAILED: "결제 실패",
  CANCELED: "취소됨",
  SHIPPED: "배송 중",
  DELIVERED: "배송 완료",
};

export default async function OrdersPage() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  let orders: OrderResponse[] = [];
  try {
    orders = await getMyOrders();
  } catch {
    orders = [];
  }

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">My Orders</p>
          <h1>주문 내역</h1>
        </div>
        <Link href="/" style={{ fontSize: "0.875rem", color: "var(--accent)" }}>
          ← 쇼핑 계속하기
        </Link>
      </section>

      {orders.length === 0 ? (
        <p className="emptyState">주문 내역이 없습니다.</p>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          {orders.map((order) => (
            <Link
              key={order.orderId}
              href={`/orders/${order.orderId}`}
              style={{ textDecoration: "none", color: "inherit" }}
            >
              <article
                className="productCard"
                style={{ cursor: "pointer", padding: "1.25rem" }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <div>
                    <p style={{ fontSize: "0.75rem", color: "var(--muted)", marginBottom: "0.25rem" }}>
                      주문번호: {order.orderId.slice(0, 8)}…
                    </p>
                    <p style={{ fontWeight: 600, marginBottom: "0.5rem" }}>
                      {order.lines?.map((l) => l.productName).join(", ") || "-"}
                    </p>
                    <p style={{ fontSize: "0.875rem", color: "var(--muted)" }}>
                      {new Date(order.createdAt).toLocaleString("ko-KR")}
                    </p>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <span
                      style={{
                        display: "inline-block",
                        padding: "0.25rem 0.75rem",
                        borderRadius: "999px",
                        fontSize: "0.75rem",
                        fontWeight: 600,
                        background: order.status === "PAID" || order.status === "DELIVERED"
                          ? "var(--accent)"
                          : "var(--surface)",
                        color: order.status === "PAID" || order.status === "DELIVERED"
                          ? "#fff"
                          : "var(--fg)",
                        marginBottom: "0.5rem",
                      }}
                    >
                      {STATUS_LABEL[order.status] ?? order.status}
                    </span>
                    <p style={{ fontWeight: 700, fontSize: "1.1rem" }}>
                      {order.totalAmount.toLocaleString("ko-KR")}원
                    </p>
                  </div>
                </div>
              </article>
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}
