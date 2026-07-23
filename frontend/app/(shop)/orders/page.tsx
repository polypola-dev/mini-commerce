import { orderDisplayNumber, type OrderResponse } from "@/lib/api";
import { getMyOrders, getProductImages } from "@/lib/api-server";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { redirect } from "next/navigation";

function formatOrderDate(iso: string): string {
  const d = new Date(iso);
  const m = d.getMonth() + 1;
  const day = d.getDate();
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${m}.${day} ${hh}:${mm} 주문`;
}

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

  const firstLineProductIds = orders
    .map((o) => o.lines?.[0]?.productId)
    .filter((id): id is string => !!id);
  const imageMap = await getProductImages(firstLineProductIds);

  return (
    <div>
      <div className="mcListHeader">
        <h1>주문 내역</h1>
        <span className="mcListHeaderCount">{orders.length}건</span>
      </div>

      {orders.length === 0 ? (
        <div className="mcEmptyState">
          <div className="mcEmptyIcon">📦</div>
          <div className="mcEmptyTitle">주문 내역이 없어요</div>
          <div className="mcEmptyDesc">첫 주문을 시작해보세요.</div>
          <Link href="/" className="mcEmptyCta">쇼핑하러 가기</Link>
        </div>
      ) : (
        <div style={{ padding: "8px 20px 28px", display: "flex", flexDirection: "column", gap: "14px" }}>
          {orders.map((order) => {
            const firstLine = order.lines?.[0];
            const extraCount = (order.lines?.length ?? 0) - 1;
            return (
              <div
                key={order.orderId}
                style={{
                  border: "1px solid var(--color-hairline)",
                  borderRadius: "14px",
                  padding: "16px",
                  display: "flex",
                  flexDirection: "column",
                  gap: "13px",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span style={{ fontSize: "12px", color: "var(--color-muted)" }}>
                    {formatOrderDate(order.createdAt)}
                  </span>
                  <span style={{ fontSize: "13px", fontWeight: 700, color: STATUS_COLOR[order.status] ?? "var(--color-ink)" }}>
                    {STATUS_LABEL[order.status] ?? order.status}
                  </span>
                </div>
                <Link
                  href={`/orders/${order.orderId}`}
                  style={{ display: "flex", gap: "12px", alignItems: "center", textDecoration: "none", color: "inherit" }}
                >
                  {imageMap[firstLine?.productId ?? ""] ? (
                    <img
                      src={imageMap[firstLine!.productId]!}
                      alt={firstLine!.productName}
                      style={{ width: 60, height: 60, borderRadius: 10, objectFit: "cover", flexShrink: 0 }}
                    />
                  ) : (
                    <div className="mcCartItemImg" style={{ fontSize: 28 }}>🛍️</div>
                  )}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: "14px", fontWeight: 600, lineHeight: 1.4, marginBottom: "4px" }}>
                      {firstLine?.productName ?? "-"}
                      {extraCount > 0 ? ` 외 ${extraCount}건` : ""}
                    </div>
                    <div style={{ fontSize: "12px", color: "var(--color-muted)" }}>
                      주문번호 {orderDisplayNumber(order)} · {order.totalAmount.toLocaleString("ko-KR")}원
                    </div>
                  </div>
                  <span style={{ color: "#bbb" }}>›</span>
                </Link>
                <Link
                  href={`/orders/${order.orderId}`}
                  style={{
                    flex: 1,
                    border: "1px solid var(--color-hairline)",
                    borderRadius: "8px",
                    textAlign: "center",
                    padding: "10px 0",
                    fontSize: "13px",
                    color: "var(--color-body)",
                    textDecoration: "none",
                  }}
                >
                  주문 상세
                </Link>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
