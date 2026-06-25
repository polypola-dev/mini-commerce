import { getOrderById } from "@/lib/api-server";
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

const STATUS_COLOR: Record<string, string> = {
  PENDING_PAYMENT: "var(--color-muted)",
  PAID: "var(--color-primary)",
  PAYMENT_FAILED: "var(--color-error, #c13515)",
  CANCELED: "var(--color-muted)",
  SHIPPED: "var(--color-primary)",
  DELIVERED: "var(--color-ink)",
};

export default async function OrderDetailPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ completed?: string }>;
}) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const { id } = await params;
  const { completed } = await searchParams;
  let order;
  try {
    order = await getOrderById(id);
  } catch {
    notFound();
  }

  if (completed === "1") {
    const count = order.lines?.reduce((a, l) => a + l.quantity, 0) ?? 0;
    return (
      <div className="mcCompleteWrap">
        <div className="mcCompleteCheck">
          <svg width="42" height="42" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
            <path d="m9 21 8 8 16-16" />
          </svg>
        </div>
        <div style={{ fontSize: "23px", fontWeight: 800, marginBottom: "8px" }}>주문이 완료되었어요</div>
        <div style={{ fontSize: "15px", color: "var(--color-muted)", lineHeight: 1.5, marginBottom: "30px" }}>
          배송 현황은 마이페이지에서 확인하세요.
        </div>

        <div className="mcOrderSummaryCard">
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "14px", marginBottom: "13px" }}>
            <span style={{ color: "var(--color-muted)" }}>주문번호</span>
            <span style={{ fontWeight: 700 }}>{order.orderId.slice(0, 8)}…</span>
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "14px", marginBottom: "13px" }}>
            <span style={{ color: "var(--color-muted)" }}>상품 수</span>
            <span style={{ fontWeight: 600 }}>{count}개</span>
          </div>
          <div style={{ height: 1, background: "var(--color-hairline-soft)", margin: "6px 0 14px" }} />
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
            <span style={{ fontSize: "15px", fontWeight: 700 }}>결제 금액</span>
            <span style={{ fontSize: "19px", fontWeight: 800, color: "var(--color-primary)" }}>
              {order.totalAmount.toLocaleString("ko-KR")}원
            </span>
          </div>
        </div>

        <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: "10px" }}>
          <Link href={`/orders/${order.orderId}`} className="mcBtn mcBtnPrimary" style={{ textDecoration: "none" }}>
            주문 상세 보기
          </Link>
          <Link href="/" className="mcBtn mcBtnSecondary" style={{ textDecoration: "none" }}>
            쇼핑 계속하기
          </Link>
        </div>
      </div>
    );
  }

  const itemCount = order.lines?.reduce((a, l) => a + l.quantity, 0) ?? 0;
  const statusColor = STATUS_COLOR[order.status] ?? "var(--color-ink)";

  return (
    <div style={{ paddingBottom: "24px" }}>
      <div style={{ padding: "14px 20px 12px", display: "flex", alignItems: "center", gap: "12px", borderBottom: "1px solid var(--color-hairline-soft)" }}>
        <Link href="/orders" aria-label="뒤로" style={{ border: "none", background: "transparent", cursor: "pointer", padding: 0, display: "flex" }}>
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m14 5-7 7 7 7" /></svg>
        </Link>
        <span style={{ fontSize: "18px", fontWeight: 700 }}>주문 상세</span>
      </div>

      <div style={{ padding: "20px" }}>
        <div style={{ border: "1px solid var(--color-hairline)", borderRadius: "14px", padding: "16px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
            <span style={{ fontSize: "14px", fontWeight: 700, color: statusColor }}>
              {STATUS_LABEL[order.status] ?? order.status}
            </span>
            <span style={{ fontSize: "12px", color: "var(--color-muted)" }}>
              주문번호 {order.orderId.slice(0, 8)}…
            </span>
          </div>
          <div style={{ fontSize: "12px", color: "var(--color-muted)" }}>
            주문일시 {new Date(order.createdAt).toLocaleString("ko-KR")}
          </div>
        </div>
      </div>

      <div style={{ padding: "0 20px 18px" }}>
        <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>주문 상품 {order.lines?.length ?? 0}개</div>
        <div style={{ display: "flex", flexDirection: "column" }}>
          {order.lines?.map((line, i) => (
            <div
              key={i}
              style={{
                display: "flex",
                gap: "12px",
                alignItems: "center",
                padding: "12px 0",
                borderBottom: i < order.lines.length - 1 ? "1px solid var(--color-hairline-soft)" : "none",
              }}
            >
              <div className="mcCartItemImg">🛍️</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: "14px", fontWeight: 600, lineHeight: 1.4, marginBottom: "4px" }}>
                  {line.productName}
                </div>
                <div style={{ fontSize: "12px", color: "var(--color-muted)" }}>
                  {line.selectedOptionValue || "기본"} · {line.unitPrice.toLocaleString("ko-KR")}원 × {line.quantity}개
                </div>
              </div>
              <div style={{ fontSize: "15px", fontWeight: 800 }}>
                {line.subtotal.toLocaleString("ko-KR")}원
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="mcDivider8" />

      {order.shippingAddress && (
        <div style={{ padding: "20px" }}>
          <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>배송지 정보</div>
          <div className="mcInfoTable">
            <div className="mcInfoRow">
              <span>받는 분</span>
              <span>{order.shippingRecipient}</span>
            </div>
            <div className="mcInfoRow">
              <span>연락처</span>
              <span>{order.shippingPhone}</span>
            </div>
            <div className="mcInfoRow">
              <span>주소</span>
              <span>
                ({order.shippingZipCode}) {order.shippingAddress} {order.shippingDetailAddress}
              </span>
            </div>
          </div>
        </div>
      )}

      <div className="mcDivider8" />

      <div style={{ padding: "20px" }}>
        <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>결제 정보</div>
        <div className="mcInfoTable">
          <div className="mcInfoRow">
            <span>상품 수</span>
            <span>{itemCount}개</span>
          </div>
          <div className="mcInfoRow">
            <span>결제 금액</span>
            <span style={{ fontWeight: 700, color: "var(--color-primary)" }}>
              {order.totalAmount.toLocaleString("ko-KR")}원
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
