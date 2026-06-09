import { getOrderById } from "@/lib/api";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: "결제 대기",
  PAID: "결제 완료",
  PAYMENT_FAILED: "결제 실패",
  CANCELED: "취소됨",
  SHIPPED: "배송 중",
  DELIVERED: "배송 완료",
};

export default async function OrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const { id } = await params;
  let order;
  try {
    order = await getOrderById(id);
  } catch {
    notFound();
  }

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Order Detail</p>
          <h1>주문 상세</h1>
          <p style={{ fontSize: "0.875rem", color: "var(--muted)", marginTop: "0.25rem" }}>
            {order.orderId}
          </p>
        </div>
        <Link href="/orders" style={{ fontSize: "0.875rem", color: "var(--accent)" }}>
          ← 주문 목록
        </Link>
      </section>

      <div style={{ display: "grid", gap: "1.5rem" }}>
        {/* 상태 */}
        <section className="productCard" style={{ padding: "1.25rem" }}>
          <h2 style={{ marginBottom: "1rem" }}>주문 상태</h2>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div>
              <span
                style={{
                  display: "inline-block",
                  padding: "0.375rem 1rem",
                  borderRadius: "999px",
                  fontWeight: 600,
                  background: "var(--accent)",
                  color: "#fff",
                }}
              >
                {STATUS_LABEL[order.status] ?? order.status}
              </span>
              <p style={{ fontSize: "0.875rem", color: "var(--muted)", marginTop: "0.5rem" }}>
                주문일시: {new Date(order.createdAt).toLocaleString("ko-KR")}
              </p>
            </div>
            <p style={{ fontSize: "1.5rem", fontWeight: 700 }}>
              {order.totalAmount.toLocaleString("ko-KR")}원
            </p>
          </div>
        </section>

        {/* 주문 상품 */}
        <section className="productCard" style={{ padding: "1.25rem" }}>
          <h2 style={{ marginBottom: "1rem" }}>주문 상품</h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
            {order.lines?.map((line, i) => (
              <div
                key={i}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  padding: "0.75rem",
                  background: "var(--surface)",
                  borderRadius: "8px",
                }}
              >
                <div>
                  <p style={{ fontWeight: 600 }}>{line.productName}</p>
                  {line.selectedOptionValue && (
                    <p style={{ fontSize: "0.875rem", color: "var(--muted)" }}>
                      옵션: {line.selectedOptionValue}
                    </p>
                  )}
                  <p style={{ fontSize: "0.875rem", color: "var(--muted)" }}>
                    {line.unitPrice.toLocaleString("ko-KR")}원 × {line.quantity}개
                  </p>
                </div>
                <p style={{ fontWeight: 700 }}>
                  {line.subtotal.toLocaleString("ko-KR")}원
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* 배송지 */}
        {order.shippingAddress && (
          <section className="productCard" style={{ padding: "1.25rem" }}>
            <h2 style={{ marginBottom: "1rem" }}>배송지 정보</h2>
            <dl style={{ display: "grid", gridTemplateColumns: "auto 1fr", gap: "0.5rem 1rem" }}>
              <dt style={{ color: "var(--muted)", fontSize: "0.875rem" }}>받는 분</dt>
              <dd>{order.shippingRecipient}</dd>
              <dt style={{ color: "var(--muted)", fontSize: "0.875rem" }}>연락처</dt>
              <dd>{order.shippingPhone}</dd>
              <dt style={{ color: "var(--muted)", fontSize: "0.875rem" }}>주소</dt>
              <dd>({order.shippingZipCode}) {order.shippingAddress} {order.shippingDetailAddress}</dd>
            </dl>
          </section>
        )}
      </div>
    </main>
  );
}
