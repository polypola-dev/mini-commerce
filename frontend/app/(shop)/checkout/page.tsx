"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { completeFakePayment, createOrder, getCart, type Cart } from "@/lib/api";

type PayMethod = "card" | "easy" | "bank";

const PAY_LABEL: Record<PayMethod, string> = {
  card: "신용 / 체크카드",
  easy: "간편결제 (Mini Pay)",
  bank: "무통장 입금",
};

export default function CheckoutPage() {
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pay, setPay] = useState<PayMethod>("card");
  const [addr, setAddr] = useState({ name: "", phone: "", a1: "", a2: "" });

  useEffect(() => {
    getCart()
      .then((c) => {
        if (c.items.length === 0) router.replace("/cart");
        setCart(c);
      })
      .catch(() => router.replace("/cart"))
      .finally(() => setLoading(false));
  }, [router]);

  const items = cart?.items ?? [];
  const subtotal = items.reduce((s, i) => s + i.subtotal, 0);
  const shipping = subtotal === 0 || subtotal >= 50000 ? 0 : 3000;
  const total = subtotal + shipping;

  async function placeOrder() {
    if (!cart || items.length === 0) return;
    setPending(true);
    setError(null);
    try {
      const order = await createOrder({
        items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        shippingRecipient: addr.name,
        shippingPhone: addr.phone,
        shippingAddress: addr.a1,
        shippingDetailAddress: addr.a2,
      });
      await completeFakePayment(order.orderId);
      router.push(`/orders/${order.orderId}?completed=1`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "주문에 실패했어요");
      setPending(false);
    }
  }

  if (loading) {
    return <p style={{ padding: "40px 20px", color: "var(--color-muted)", fontSize: "14px" }}>불러오는 중…</p>;
  }

  return (
    <div style={{ paddingBottom: "16px" }}>
      <div style={{ padding: "14px 20px 12px", display: "flex", alignItems: "center", gap: "12px", borderBottom: "1px solid var(--color-hairline-soft)" }}>
        <button type="button" onClick={() => router.back()} aria-label="뒤로" style={{ border: "none", background: "transparent", cursor: "pointer", padding: 0, display: "flex" }}>
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m14 5-7 7 7 7" /></svg>
        </button>
        <span style={{ fontSize: "18px", fontWeight: 700 }}>주문 / 결제</span>
      </div>

      <div style={{ padding: "20px" }}>
        <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>배송지</div>
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          <input className="mcCheckoutInput" placeholder="받는 분" value={addr.name} onChange={(e) => setAddr((a) => ({ ...a, name: e.target.value }))} />
          <input className="mcCheckoutInput" placeholder="연락처" value={addr.phone} onChange={(e) => setAddr((a) => ({ ...a, phone: e.target.value }))} />
          <input className="mcCheckoutInput" placeholder="주소" value={addr.a1} onChange={(e) => setAddr((a) => ({ ...a, a1: e.target.value }))} />
          <input className="mcCheckoutInput" placeholder="상세 주소" value={addr.a2} onChange={(e) => setAddr((a) => ({ ...a, a2: e.target.value }))} />
        </div>
      </div>

      <div className="mcDivider8" />

      <div style={{ padding: "20px" }}>
        <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>결제 수단</div>
        <div style={{ border: "1px solid var(--color-hairline)", borderRadius: "var(--radius-md)", overflow: "hidden" }}>
          {(Object.keys(PAY_LABEL) as PayMethod[]).map((key) => (
            <div key={key} className="mcPayRow" onClick={() => setPay(key)}>
              <span className={`mcPayRadio${pay === key ? " selected" : ""}`}>
                {pay === key && <span className="mcPayRadioDot" />}
              </span>
              <span style={{ fontSize: "15px", fontWeight: 500 }}>{PAY_LABEL[key]}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="mcDivider8" />

      <div style={{ padding: "20px" }}>
        <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>주문 상품 {items.length}개</div>
        {items.map((it) => (
          <div key={it.itemId} style={{ display: "flex", gap: "12px", marginBottom: "14px" }}>
            <div style={{ flex: "none", width: 56, height: 56, borderRadius: 10, background: "var(--color-surface-strong)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20 }}>🛍️</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: "14px", fontWeight: 600, lineHeight: 1.4 }}>{it.productName}</div>
              <div style={{ fontSize: "12px", color: "var(--color-muted)", marginTop: "3px" }}>{it.selectedOptionValue || "기본"} · {it.quantity}개</div>
            </div>
            <div style={{ fontSize: "14px", fontWeight: 700 }}>{it.subtotal.toLocaleString("ko-KR")}원</div>
          </div>
        ))}
        <div style={{ height: 1, background: "var(--color-hairline)", margin: "8px 0 16px" }} />
        <div className="mcSummaryRow"><span>상품 금액</span><span>{subtotal.toLocaleString("ko-KR")}원</span></div>
        <div className="mcSummaryRow"><span>배송비</span><span>{shipping === 0 ? "무료" : `${shipping.toLocaleString("ko-KR")}원`}</span></div>
      </div>

      {error && <p style={{ padding: "0 20px", color: "var(--color-error)", fontSize: "13px" }}>{error}</p>}

      <div className="mcActionBar" style={{ padding: "12px 16px 16px" }}>
        <button type="button" className="mcBtn mcBtnPrimary" disabled={pending} onClick={placeOrder}>
          {pending ? "주문 처리 중…" : `${total.toLocaleString("ko-KR")}원 결제하기`}
        </button>
      </div>
    </div>
  );
}
