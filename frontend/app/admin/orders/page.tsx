"use client";

import { adminGetOrders, adminUpdateOrderStatus, type OrderResponse } from "@/lib/api";
import { useEffect, useState } from "react";

const STATUS_OPTIONS = ["PENDING_PAYMENT", "PAID", "SHIPPED", "DELIVERED", "CANCELED"];

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: "결제 대기",
  PAID: "결제 완료",
  PAYMENT_FAILED: "결제 실패",
  CANCELED: "취소됨",
  SHIPPED: "배송 중",
  DELIVERED: "배송 완료",
};

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    try {
      setOrders(await adminGetOrders());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleStatusChange(orderId: string, status: string) {
    setUpdating(orderId);
    try {
      await adminUpdateOrderStatus(orderId, status);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "상태 변경 실패");
    } finally {
      setUpdating(null);
    }
  }

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Admin / Orders</p>
          <h1>주문 관리</h1>
        </div>
      </section>

      {loading ? (
        <p className="emptyState">불러오는 중...</p>
      ) : orders.length === 0 ? (
        <p className="emptyState">주문이 없습니다.</p>
      ) : (
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.875rem" }}>
          <thead>
            <tr style={{ borderBottom: "2px solid var(--border)", textAlign: "left" }}>
              <th style={{ padding: "0.75rem" }}>주문번호</th>
              <th style={{ padding: "0.75rem" }}>고객 ID</th>
              <th style={{ padding: "0.75rem" }}>상품</th>
              <th style={{ padding: "0.75rem" }}>금액</th>
              <th style={{ padding: "0.75rem" }}>주문일시</th>
              <th style={{ padding: "0.75rem" }}>상태 변경</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.orderId} style={{ borderBottom: "1px solid var(--border)" }}>
                <td style={{ padding: "0.75rem", fontFamily: "monospace" }}>{order.orderId.slice(0, 8)}…</td>
                <td style={{ padding: "0.75rem", fontFamily: "monospace", fontSize: "0.75rem", color: "var(--muted)" }}>
                  {/* customerId는 OrderResponse에 없으므로 생략 */}—
                </td>
                <td style={{ padding: "0.75rem" }}>
                  {order.lines?.map((l) => l.productName).join(", ") || "-"}
                </td>
                <td style={{ padding: "0.75rem", fontWeight: 600 }}>
                  {order.totalAmount.toLocaleString("ko-KR")}원
                </td>
                <td style={{ padding: "0.75rem", color: "var(--muted)" }}>
                  {new Date(order.createdAt).toLocaleString("ko-KR")}
                </td>
                <td style={{ padding: "0.75rem" }}>
                  <select
                    value={order.status}
                    disabled={updating === order.orderId}
                    onChange={(e) => handleStatusChange(order.orderId, e.target.value)}
                    style={{
                      padding: "0.25rem 0.5rem",
                      borderRadius: "6px",
                      border: "1px solid var(--border)",
                      fontSize: "0.75rem",
                      cursor: "pointer",
                    }}
                  >
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>{STATUS_LABEL[s] ?? s}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}
