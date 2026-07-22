"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { loadTossPayments, type TossPaymentsWidgets } from "@tosspayments/tosspayments-sdk";
import { createOrder, getCart, type Cart, type CartItem } from "@/lib/api";
import { CHECKOUT_SELECTED_ITEM_IDS_KEY } from "@/lib/checkoutSelection";
import { useAddresses } from "@/lib/addresses";

const MANUAL_ADDR = "__manual__";

const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ?? "";

function makeCustomerKey(): string {
  return `mc_${crypto.randomUUID()}`;
}

export default function CheckoutPage() {
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [items, setItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [addr, setAddr] = useState({ name: "", phone: "", a1: "", a2: "" });
  const { addresses } = useAddresses();
  const [selectedAddrId, setSelectedAddrId] = useState<string | null>(null);
  const [orderId, setOrderId] = useState<string | null>(null);
  const [payAmount, setPayAmount] = useState<number | null>(null);
  const [widgetReady, setWidgetReady] = useState(false);
  const widgetsRef = useRef<TossPaymentsWidgets | null>(null);

  useEffect(() => {
    getCart()
      .then((c) => {
        let selectedItems = c.items;
        const raw = sessionStorage.getItem(CHECKOUT_SELECTED_ITEM_IDS_KEY);
        if (raw) {
          sessionStorage.removeItem(CHECKOUT_SELECTED_ITEM_IDS_KEY);
          try {
            const ids = new Set<string>(JSON.parse(raw));
            if (ids.size > 0) selectedItems = c.items.filter((i) => ids.has(i.itemId));
          } catch {
            // ignore malformed selection, fall back to full cart
          }
        }
        if (selectedItems.length === 0) {
          router.replace("/cart");
          return;
        }
        setCart(c);
        setItems(selectedItems);
      })
      .catch(() => router.replace("/cart"))
      .finally(() => setLoading(false));
  }, [router]);

  // 저장된 배송지가 로드되면 기본배송지(없으면 첫 항목)를 자동 선택해 입력값을 채운다.
  // 사용자가 아직 아무것도 고르지 않았을 때만 초기화하고, 이후 선택은 건드리지 않는다.
  useEffect(() => {
    if (selectedAddrId || addresses.length === 0) return;
    const def = addresses.find((a) => a.isDefault) ?? addresses[0];
    setSelectedAddrId(def.id);
    setAddr({ name: def.name, phone: def.phone, a1: def.address1, a2: def.address2 });
  }, [addresses, selectedAddrId]);

  function selectSavedAddress(id: string) {
    const a = addresses.find((x) => x.id === id);
    if (!a) return;
    setSelectedAddrId(a.id);
    setAddr({ name: a.name, phone: a.phone, a1: a.address1, a2: a.address2 });
  }

  function selectManualAddress() {
    setSelectedAddrId(MANUAL_ADDR);
    setAddr({ name: "", phone: "", a1: "", a2: "" });
  }

  // 저장된 배송지가 없거나, 사용자가 "직접 입력"을 고른 경우에만 입력 필드를 노출한다.
  const showManualFields = addresses.length === 0 || selectedAddrId === MANUAL_ADDR;

  const subtotal = items.reduce((s, i) => s + i.subtotal, 0);
  const shipping = subtotal === 0 || subtotal >= 50000 ? 0 : 3000;

  // 주문 생성 후 결제위젯을 마운트한다. 위젯은 setAmount가 선행돼야 renderPaymentMethods가 동작.
  // 금액은 반드시 서버가 검증할 order.totalAmount(payAmount) 기준 — 백엔드는 배송비를 청구하지 않으므로
  // 로컬 배송비 포함 합계를 넘기면 confirm 단계에서 PAYMENT_AMOUNT_MISMATCH로 전량 실패한다.
  useEffect(() => {
    if (!orderId || payAmount == null) return;
    let cancelled = false;
    (async () => {
      try {
        const tossPayments = await loadTossPayments(TOSS_CLIENT_KEY);
        const widgets = tossPayments.widgets({ customerKey: makeCustomerKey() });
        await widgets.setAmount({ currency: "KRW", value: payAmount });
        if (cancelled) return;
        await Promise.all([
          widgets.renderPaymentMethods({ selector: "#payment-method" }),
          widgets.renderAgreement({ selector: "#agreement" }),
        ]);
        if (cancelled) return;
        widgetsRef.current = widgets;
        setWidgetReady(true);
      } catch {
        if (!cancelled) setError("결제 모듈을 불러오지 못했어요");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [orderId, payAmount]);

  async function startCheckout() {
    if (!cart || items.length === 0) return;
    setPending(true);
    setError(null);
    try {
      const order = await createOrder({
        items: items.map((i) => ({
          productId: i.productId,
          quantity: i.quantity,
          ...(i.selectedOptionId ? { selectedOptionId: i.selectedOptionId } : {}),
        })),
        shippingRecipient: addr.name,
        shippingPhone: addr.phone,
        shippingAddress: addr.a1,
        shippingDetailAddress: addr.a2,
      });
      setPayAmount(order.totalAmount);
      setOrderId(order.orderId);
    } catch (e) {
      setError(e instanceof Error ? e.message : "주문에 실패했어요");
      setPending(false);
    }
  }

  async function requestPayment() {
    const widgets = widgetsRef.current;
    if (!widgets || !orderId) return;
    setError(null);
    const first = items[0]?.productName ?? "주문 상품";
    const orderName = items.length > 1 ? `${first} 외 ${items.length - 1}건` : first;
    try {
      await widgets.requestPayment({
        orderId,
        orderName,
        successUrl: `${window.location.origin}/payment/success`,
        failUrl: `${window.location.origin}/payment/fail`,
        ...(addr.name ? { customerName: addr.name } : {}),
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : "결제 요청에 실패했어요");
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

        {addresses.length > 0 && (
          <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginBottom: showManualFields ? "12px" : 0 }}>
            {addresses.map((a) => {
              const selected = selectedAddrId === a.id;
              return (
                <button
                  key={a.id}
                  type="button"
                  disabled={!!orderId}
                  onClick={() => selectSavedAddress(a.id)}
                  style={{
                    textAlign: "left",
                    width: "100%",
                    border: `1.5px solid ${selected ? "var(--color-primary)" : "var(--color-hairline)"}`,
                    background: selected ? "var(--color-surface-strong)" : "#fff",
                    borderRadius: "var(--radius-md)",
                    padding: "13px 15px",
                    cursor: orderId ? "default" : "pointer",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "7px", marginBottom: "3px" }}>
                    <span style={{ fontSize: "13.5px", fontWeight: 700 }}>{a.name}</span>
                    {a.isDefault && (
                      <span style={{ background: "var(--color-primary)", color: "#fff", fontSize: "10.5px", fontWeight: 800, borderRadius: "999px", padding: "1.5px 8px" }}>
                        기본
                      </span>
                    )}
                  </div>
                  <div style={{ fontSize: "12.5px", color: "var(--color-body)" }}>{a.address1} {a.address2}</div>
                  <div style={{ fontSize: "12px", color: "var(--color-muted)", marginTop: "1px" }}>{a.phone}</div>
                </button>
              );
            })}
            <button
              type="button"
              disabled={!!orderId}
              onClick={selectManualAddress}
              style={{
                width: "100%",
                border: `1.5px dashed ${selectedAddrId === MANUAL_ADDR ? "var(--color-primary)" : "var(--color-border-strong)"}`,
                background: "#fff",
                borderRadius: "var(--radius-sm)",
                padding: "11px",
                fontSize: "13px",
                fontWeight: 700,
                color: "var(--color-muted)",
                cursor: orderId ? "default" : "pointer",
              }}
            >
              ＋ 새 배송지 직접 입력
            </button>
          </div>
        )}

        {showManualFields && (
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            <input className="mcCheckoutInput" placeholder="받는 분" value={addr.name} disabled={!!orderId} onChange={(e) => setAddr((a) => ({ ...a, name: e.target.value }))} />
            <input className="mcCheckoutInput" placeholder="연락처" value={addr.phone} disabled={!!orderId} onChange={(e) => setAddr((a) => ({ ...a, phone: e.target.value }))} />
            <input className="mcCheckoutInput" placeholder="주소" value={addr.a1} disabled={!!orderId} onChange={(e) => setAddr((a) => ({ ...a, a1: e.target.value }))} />
            <input className="mcCheckoutInput" placeholder="상세 주소" value={addr.a2} disabled={!!orderId} onChange={(e) => setAddr((a) => ({ ...a, a2: e.target.value }))} />
          </div>
        )}
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

      {orderId && (
        <>
          <div className="mcDivider8" />
          <div style={{ padding: "20px" }}>
            <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>결제 수단</div>
            <div id="payment-method" />
            <div id="agreement" />
          </div>
        </>
      )}

      {error && <p style={{ padding: "0 20px", color: "var(--color-error)", fontSize: "13px" }}>{error}</p>}

      <div className="mcActionBar" style={{ padding: "12px 16px 16px" }}>
        {orderId ? (
          <button type="button" className="mcBtn mcBtnPrimary" disabled={!widgetReady || payAmount == null} onClick={requestPayment}>
            {widgetReady && payAmount != null ? `${payAmount.toLocaleString("ko-KR")}원 결제하기` : "결제 준비 중…"}
          </button>
        ) : (
          <button type="button" className="mcBtn mcBtnPrimary" disabled={pending} onClick={startCheckout}>
            {pending ? "주문 처리 중…" : `${subtotal.toLocaleString("ko-KR")}원 결제하기`}
          </button>
        )}
      </div>
    </div>
  );
}
