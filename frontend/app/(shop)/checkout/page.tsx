"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { loadTossPayments, type TossPaymentsWidgets } from "@tosspayments/tosspayments-sdk";
import { createOrder, getCart, type Cart, type CartItem } from "@/lib/api";
import { CHECKOUT_SELECTED_ITEM_IDS_KEY, CHECKOUT_ORDERED_ITEM_IDS_KEY } from "@/lib/checkoutSelection";
import { useAddresses, type Address } from "@/lib/addresses";
import AddressForm, { type AddressFormValue } from "../address-form";

const MANUAL_ADDR = "__manual__";

const pickerCardBtn: React.CSSProperties = {
  width: "auto",
  border: "1px solid var(--color-hairline)",
  background: "#fff",
  color: "var(--color-ink)",
  borderRadius: "8px",
  padding: "6px 13px",
  fontSize: "12px",
  fontWeight: 600,
  cursor: "pointer",
};

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
  const [addr, setAddr] = useState({ label: "", name: "", phone: "", a1: "", a2: "" });
  const { addresses, add, update, remove } = useAddresses();
  const [selectedAddrId, setSelectedAddrId] = useState<string | null>(null);
  const [orderId, setOrderId] = useState<string | null>(null);
  const [payAmount, setPayAmount] = useState<number | null>(null);
  const [widgetReady, setWidgetReady] = useState(false);
  const widgetsRef = useRef<TossPaymentsWidgets | null>(null);
  // sessionStorage 선택값은 "한 번만 소비"하는 값이라, effect가 두 번 실행돼도
  // (React 18 Strict Mode의 dev 중복 호출 등) 두 번째 실행이 이미 지워진 값을 못 읽는 일이
  // 없도록 ref에 파싱 결과를 캐싱해둔다. 안 그러면 두 번째 실행이 "선택 없음"으로 오인해
  // 장바구니 전체를 주문 상품으로 넣어버린다.
  const selectedIdsRef = useRef<Set<string> | null | undefined>(undefined);

  // Toss 결제창(외부 도메인)에서 뒤로가기로 돌아오면 브라우저가 bfcache에 저장해둔
  // 결제 직전 스냅샷(이미 주문한 상품 목록, 위젯 상태 등)을 JS 재실행 없이 그대로 복원한다.
  // getCart 재검증이 통째로 스킵되므로, persisted 복원을 감지하면 새로고침해 최신 상태를 다시 받는다.
  useEffect(() => {
    function handlePageShow(e: PageTransitionEvent) {
      if (e.persisted) window.location.reload();
    }
    window.addEventListener("pageshow", handlePageShow);
    return () => window.removeEventListener("pageshow", handlePageShow);
  }, []);

  useEffect(() => {
    if (selectedIdsRef.current === undefined) {
      const raw = sessionStorage.getItem(CHECKOUT_SELECTED_ITEM_IDS_KEY);
      sessionStorage.removeItem(CHECKOUT_SELECTED_ITEM_IDS_KEY);
      selectedIdsRef.current = null;
      if (raw) {
        try {
          const ids = new Set<string>(JSON.parse(raw));
          if (ids.size > 0) selectedIdsRef.current = ids;
        } catch {
          // ignore malformed selection, fall back to full cart
        }
      }
    }
    const ids = selectedIdsRef.current;

    getCart()
      .then((c) => {
        const selectedItems = ids ? c.items.filter((i) => ids.has(i.itemId)) : c.items;
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

  // 배송지 팝업(풀스크린) 상태. list=목록/선택, add=신규 입력, edit=기존 수정.
  const [showPicker, setShowPicker] = useState(false);
  const [picker, setPicker] = useState<{ mode: "list" } | { mode: "add" } | { mode: "edit"; addr: Address }>({ mode: "list" });

  // 저장된 배송지가 로드되면 기본배송지(없으면 첫 항목)를 자동 선택해 입력값을 채운다.
  // 사용자가 아직 아무것도 고르지 않았을 때만 초기화하고, 이후 선택은 건드리지 않는다.
  useEffect(() => {
    if (selectedAddrId || addresses.length === 0) return;
    const def = addresses.find((a) => a.isDefault) ?? addresses[0];
    setSelectedAddrId(def.id);
    setAddr({ label: def.label ?? "", name: def.name, phone: def.phone, a1: def.address1, a2: def.address2 });
  }, [addresses, selectedAddrId]);

  function openPicker() {
    if (orderId) return;
    setPicker({ mode: "list" });
    setShowPicker(true);
  }

  function chooseSaved(id: string) {
    const a = addresses.find((x) => x.id === id);
    if (!a) return;
    setSelectedAddrId(a.id);
    setAddr({ label: a.label ?? "", name: a.name, phone: a.phone, a1: a.address1, a2: a.address2 });
    setShowPicker(false);
  }

  // 폼 저장: 주소록에 add/update한 뒤, 그 배송지를 이번 주문의 배송지로 선택한다.
  function submitAddressForm(v: AddressFormValue) {
    const payload = {
      label: v.label || null,
      name: v.name,
      phone: v.phone,
      address1: v.address1,
      address2: v.address2,
      zipCode: v.zipCode || null,
    };
    if (picker.mode === "edit") {
      update(picker.addr.id, payload);
      setSelectedAddrId(picker.addr.id);
    } else {
      add(payload);
      setSelectedAddrId(MANUAL_ADDR);
    }
    setAddr({ label: v.label, name: v.name, phone: v.phone, a1: v.address1, a2: v.address2 });
    setShowPicker(false);
  }

  function deleteSaved(id: string) {
    remove(id);
    if (selectedAddrId === id) {
      setSelectedAddrId(null);
      setAddr({ label: "", name: "", phone: "", a1: "", a2: "" });
    }
  }

  const hasAddr = addr.name.trim().length > 0 && addr.a1.trim().length > 0;

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
      sessionStorage.setItem(CHECKOUT_ORDERED_ITEM_IDS_KEY, JSON.stringify(items.map((i) => i.itemId)));
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
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "14px" }}>
          <div style={{ fontSize: "16px", fontWeight: 700 }}>배송지</div>
          {!orderId && hasAddr && (
            <button
              type="button"
              onClick={openPicker}
              style={{ border: "1px solid var(--color-hairline)", background: "#fff", color: "var(--color-ink)", borderRadius: "8px", padding: "6px 12px", fontSize: "12.5px", fontWeight: 600, cursor: "pointer", width: "auto" }}
            >
              변경
            </button>
          )}
        </div>

        {hasAddr ? (
          <div style={{ border: "1px solid var(--color-hairline)", borderRadius: "var(--radius-md)", padding: "14px 16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "7px", marginBottom: "3px" }}>
              {addr.label && (
                <span style={{ background: "var(--color-surface-strong)", color: "var(--color-primary)", fontSize: "11px", fontWeight: 800, borderRadius: "6px", padding: "2px 7px" }}>{addr.label}</span>
              )}
              <span style={{ fontSize: "14px", fontWeight: 700 }}>{addr.name}</span>
            </div>
            <div style={{ fontSize: "13px", color: "var(--color-body)" }}>{addr.a1} {addr.a2}</div>
            <div style={{ fontSize: "12.5px", color: "var(--color-muted)", marginTop: "1px" }}>{addr.phone}</div>
          </div>
        ) : (
          <button
            type="button"
            disabled={!!orderId}
            onClick={openPicker}
            style={{ width: "100%", border: "1.5px dashed var(--color-border-strong)", background: "#fff", borderRadius: "var(--radius-sm)", padding: "14px", fontSize: "13.5px", fontWeight: 700, color: "var(--color-muted)", cursor: orderId ? "default" : "pointer" }}
          >
            ＋ 배송지 선택
          </button>
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
          <button type="button" className="mcBtn mcBtnPrimary" disabled={pending || !hasAddr} onClick={startCheckout}>
            {pending ? "주문 처리 중…" : !hasAddr ? "배송지를 선택해주세요" : `${subtotal.toLocaleString("ko-KR")}원 결제하기`}
          </button>
        )}
      </div>

      {showPicker && (
        <div style={{ position: "fixed", inset: 0, background: "#fff", zIndex: 1000, display: "flex", flexDirection: "column" }}>
          {/* 헤더 */}
          <div style={{ padding: "14px 20px 12px", display: "flex", alignItems: "center", gap: "12px", borderBottom: "1px solid var(--color-hairline-soft)", flex: "none" }}>
            <button
              type="button"
              onClick={() => (picker.mode !== "list" && addresses.length > 0 ? setPicker({ mode: "list" }) : setShowPicker(false))}
              aria-label="뒤로"
              style={{ border: "none", background: "transparent", cursor: "pointer", padding: 0, display: "flex" }}
            >
              <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m14 5-7 7 7 7" /></svg>
            </button>
            <span style={{ fontSize: "18px", fontWeight: 700 }}>
              {picker.mode === "add" ? "배송지 추가하기" : picker.mode === "edit" ? "배송지 수정" : "배송지 변경"}
            </span>
          </div>

          {/* 컨텐츠 */}
          <div style={{ flex: 1, overflowY: "auto", padding: "16px 20px 24px" }}>
            {picker.mode === "list" ? (
              <>
                <button
                  type="button"
                  onClick={() => setPicker({ mode: "add" })}
                  style={{ width: "100%", border: "1.5px solid var(--color-hairline)", background: "#fff", borderRadius: "var(--radius-md)", padding: "15px", fontSize: "14px", fontWeight: 700, color: "var(--color-ink)", cursor: "pointer", marginBottom: "16px" }}
                >
                  ＋ 배송지 추가하기
                </button>

                <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                  {addresses.map((a) => {
                    const selected = selectedAddrId === a.id;
                    return (
                      <div
                        key={a.id}
                        style={{ border: `1.5px solid ${selected ? "var(--color-primary)" : "var(--color-hairline)"}`, background: selected ? "var(--color-surface-strong)" : "#fff", borderRadius: "var(--radius-md)", padding: "14px 16px" }}
                      >
                        <button type="button" onClick={() => chooseSaved(a.id)} style={{ textAlign: "left", width: "100%", border: "none", background: "transparent", cursor: "pointer", padding: 0 }}>
                          <div style={{ display: "flex", alignItems: "center", gap: "7px", marginBottom: "4px" }}>
                            {a.label && (
                              <span style={{ background: "var(--color-surface-strong)", color: "var(--color-primary)", fontSize: "11px", fontWeight: 800, borderRadius: "6px", padding: "2px 7px" }}>{a.label}</span>
                            )}
                            <span style={{ fontSize: "14px", fontWeight: 700 }}>{a.name}</span>
                            {a.isDefault && (
                              <span style={{ background: "var(--color-primary)", color: "#fff", fontSize: "10.5px", fontWeight: 800, borderRadius: "999px", padding: "1.5px 8px" }}>기본</span>
                            )}
                          </div>
                          <div style={{ fontSize: "13px", color: "var(--color-body)" }}>{a.zipCode ? `[${a.zipCode}] ` : ""}{a.address1} {a.address2}</div>
                          <div style={{ fontSize: "12.5px", color: "var(--color-muted)", marginTop: "1px" }}>{a.phone}</div>
                        </button>
                        <div style={{ display: "flex", gap: "6px", marginTop: "11px" }}>
                          <button type="button" style={pickerCardBtn} onClick={() => setPicker({ mode: "edit", addr: a })}>수정</button>
                          <button type="button" style={pickerCardBtn} onClick={() => deleteSaved(a.id)}>삭제</button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </>
            ) : (
              <AddressForm
                initial={picker.mode === "edit" ? { label: picker.addr.label ?? "", name: picker.addr.name, phone: picker.addr.phone, address1: picker.addr.address1, address2: picker.addr.address2, zipCode: picker.addr.zipCode ?? "" } : undefined}
                submitLabel={picker.mode === "edit" ? "수정하고 선택" : "저장하고 선택"}
                onSubmit={submitAddressForm}
                onCancel={() => (addresses.length > 0 ? setPicker({ mode: "list" }) : setShowPicker(false))}
              />
            )}
          </div>
        </div>
      )}
    </div>
  );
}
