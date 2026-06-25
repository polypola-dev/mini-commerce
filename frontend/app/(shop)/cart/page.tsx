"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getCart, removeCartItem, updateCartItem, type Cart } from "@/lib/api";
import { CHECKOUT_SELECTED_ITEM_IDS_KEY } from "@/lib/checkoutSelection";

const MAX_CHECKOUT_LINE_ITEMS = 100;

export default function CartPage() {
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Set<string>>(new Set());

  useEffect(() => {
    getCart()
      .then(setCart)
      .catch(() => setCart(null))
      .finally(() => setLoading(false));
  }, []);

  async function changeQty(itemId: string, quantity: number) {
    if (quantity < 1) return;
    try {
      const updated = await updateCartItem(itemId, { quantity });
      setCart(updated);
    } catch {
      // ignore
    }
  }

  async function remove(itemId: string) {
    try {
      await removeCartItem(itemId);
      setCart((prev) => {
        if (!prev) return prev;
        const items = prev.items.filter((i) => i.itemId !== itemId);
        const totalAmount = items.reduce((sum, i) => sum + i.subtotal, 0);
        return { ...prev, items, totalAmount };
      });
      setSelected((prev) => {
        if (!prev.has(itemId)) return prev;
        const next = new Set(prev);
        next.delete(itemId);
        return next;
      });
    } catch {
      // ignore
    }
  }

  function toggleSelected(itemId: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(itemId)) next.delete(itemId);
      else next.add(itemId);
      return next;
    });
  }

  function toggleSelectAll() {
    setSelected((prev) => (prev.size === items.length ? new Set() : new Set(items.map((i) => i.itemId))));
  }

  async function removeSelected() {
    if (selected.size === 0) return;
    const ids = Array.from(selected);
    try {
      await Promise.all(ids.map((itemId) => removeCartItem(itemId)));
    } catch {
      // ignore individual failures and refresh from server state below
    }
    setCart((prev) => {
      if (!prev) return prev;
      const remaining = prev.items.filter((i) => !selected.has(i.itemId));
      const totalAmount = remaining.reduce((sum, i) => sum + i.subtotal, 0);
      return { ...prev, items: remaining, totalAmount };
    });
    setSelected(new Set());
  }

  function goToCheckout() {
    const selectedItems = items.filter((i) => selected.has(i.itemId));
    if (selectedItems.length === 0) {
      alert("선택된 상품이 없어요");
      return;
    }
    if (selectedItems.length > MAX_CHECKOUT_LINE_ITEMS) {
      alert(`한 번에 결제할 수 있는 상품은 최대 ${MAX_CHECKOUT_LINE_ITEMS}개예요`);
      return;
    }
    sessionStorage.setItem(CHECKOUT_SELECTED_ITEM_IDS_KEY, JSON.stringify(selectedItems.map((i) => i.itemId)));
    router.push("/checkout");
  }

  const items = cart?.items ?? [];
  const selectedItems = items.filter((i) => selected.has(i.itemId));
  const subtotal = selectedItems.reduce((s, i) => s + i.subtotal, 0);
  const shipping = subtotal === 0 || subtotal >= 50000 ? 0 : 3000;
  const total = subtotal + shipping;
  const count = items.reduce((a, i) => a + i.quantity, 0);
  const allSelected = items.length > 0 && selected.size === items.length;

  return (
    <div>
      <div className="mcListHeader">
        <h1>장바구니</h1>
        <span className="mcListHeaderCount">{count}개</span>
      </div>

      {loading && <p style={{ padding: "40px 20px", color: "var(--color-muted)", fontSize: "14px" }}>불러오는 중…</p>}

      {!loading && items.length === 0 && (
        <div className="mcEmptyState">
          <div className="mcEmptyIcon">🛒</div>
          <div className="mcEmptyTitle">장바구니가 비어 있어요</div>
          <div className="mcEmptyDesc">마음에 드는 상품을 담아보세요.</div>
          <Link href="/" className="mcEmptyCta">쇼핑하러 가기</Link>
        </div>
      )}

      {!loading && items.length > 0 && (
        <div>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 20px", borderBottom: "1px solid var(--color-hairline-soft)" }}>
            <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", fontWeight: 600, cursor: "pointer" }}>
              <input type="checkbox" checked={allSelected} onChange={toggleSelectAll} style={{ width: 18, height: 18 }} />
              전체 선택 ({selected.size}/{items.length})
            </label>
            <button
              type="button"
              onClick={removeSelected}
              disabled={selected.size === 0}
              style={{ border: "none", background: "transparent", cursor: selected.size === 0 ? "default" : "pointer", color: selected.size === 0 ? "#ccc" : "var(--color-muted)", fontSize: "13px", padding: 0 }}
            >
              선택 삭제
            </button>
          </div>

          {items.map((item) => (
            <div
              key={item.itemId}
              className="mcCartItem"
              onClick={() => router.push(`/products/${item.productId}`)}
              style={{ cursor: "pointer" }}
            >
              <input
                type="checkbox"
                checked={selected.has(item.itemId)}
                onClick={(e) => e.stopPropagation()}
                onChange={() => toggleSelected(item.itemId)}
                style={{ flex: "none", width: 18, height: 18, alignSelf: "flex-start", marginTop: "2px" }}
              />
              <div className="mcCartItemImg">🛍️</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: "8px" }}>
                  <div style={{ fontSize: "14px", fontWeight: 600, lineHeight: 1.4 }}>{item.productName}</div>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      remove(item.itemId);
                    }}
                    aria-label="삭제"
                    style={{ flex: "none", border: "none", background: "transparent", cursor: "pointer", color: "#bbb", padding: 0, fontSize: "16px" }}
                  >
                    ✕
                  </button>
                </div>
                <div style={{ fontSize: "12px", color: "var(--color-muted)", margin: "4px 0 10px" }}>
                  옵션 · {item.selectedOptionValue || "기본"}
                </div>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                  <div className="mcQtyControl" onClick={(e) => e.stopPropagation()}>
                    <button type="button" className="mcQtyBtn" onClick={() => changeQty(item.itemId, item.quantity - 1)}>−</button>
                    <span style={{ width: 34, textAlign: "center", fontSize: "14px", fontWeight: 600 }}>{item.quantity}</span>
                    <button type="button" className="mcQtyBtn" onClick={() => changeQty(item.itemId, item.quantity + 1)}>+</button>
                  </div>
                  <div style={{ fontSize: "16px", fontWeight: 800 }}>{item.subtotal.toLocaleString("ko-KR")}원</div>
                </div>
              </div>
            </div>
          ))}

          <div style={{ padding: "20px" }}>
            <div className="mcSummaryRow"><span>상품 금액</span><span>{subtotal.toLocaleString("ko-KR")}원</span></div>
            <div className="mcSummaryRow"><span>배송비</span><span>{shipping === 0 ? "무료" : `${shipping.toLocaleString("ko-KR")}원`}</span></div>
            <div style={{ height: 1, background: "var(--color-hairline)", margin: "14px 0" }} />
            <div className="mcSummaryTotal">
              <span style={{ fontSize: "16px", fontWeight: 700 }}>결제 예상 금액</span>
              <span style={{ fontSize: "20px", fontWeight: 800, color: "var(--color-primary)" }}>{total.toLocaleString("ko-KR")}원</span>
            </div>
            <button type="button" className="mcBtn mcBtnPrimary" disabled={selected.size === 0} onClick={goToCheckout}>
              {selected.size === 0 ? "상품을 선택해주세요" : `${total.toLocaleString("ko-KR")}원 결제하기 (${selected.size})`}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
