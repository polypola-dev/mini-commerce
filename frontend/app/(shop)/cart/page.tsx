"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getCart, getProductById, removeCartItem, updateCartItem, type Cart, type CartItem } from "@/lib/api";
import { CHECKOUT_SELECTED_ITEM_IDS_KEY } from "@/lib/checkoutSelection";

const MAX_CHECKOUT_LINE_ITEMS = 100;
const QTY_DEBOUNCE_MS = 350;

type CartGroup = {
  productId: string;
  productName: string;
  items: CartItem[];
};

function groupByProduct(items: CartItem[]): CartGroup[] {
  const groups: CartGroup[] = [];
  const index = new Map<string, CartGroup>();
  for (const item of items) {
    let group = index.get(item.productId);
    if (!group) {
      group = { productId: item.productId, productName: item.productName, items: [] };
      index.set(item.productId, group);
      groups.push(group);
    }
    group.items.push(item);
  }
  return groups;
}

export default function CartPage() {
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [productImages, setProductImages] = useState<Record<string, string>>({});
  const [pendingQty, setPendingQty] = useState<Record<string, number>>({});
  const qtyTimers = useRef<Record<string, ReturnType<typeof setTimeout>>>({});

  useEffect(() => {
    getCart()
      .then(setCart)
      .catch(() => setCart(null))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!cart) return;
    const productIds = Array.from(new Set(cart.items.map((i) => i.productId)));
    const missing = productIds.filter((id) => !(id in productImages));
    if (missing.length === 0) return;
    Promise.all(
      missing.map((id) =>
        getProductById(id)
          .then((product) => [id, product.imageUrl] as const)
          .catch(() => [id, ""] as const)
      )
    ).then((entries) => {
      setProductImages((prev) => ({ ...prev, ...Object.fromEntries(entries) }));
    });
  }, [cart, productImages]);

  useEffect(() => {
    return () => {
      Object.values(qtyTimers.current).forEach(clearTimeout);
    };
  }, []);

  function effectiveQuantity(item: CartItem) {
    return pendingQty[item.itemId] ?? item.quantity;
  }

  function bumpQty(item: CartItem, delta: number) {
    const nextQuantity = effectiveQuantity(item) + delta;
    if (nextQuantity < 1) return;
    setPendingQty((prev) => ({ ...prev, [item.itemId]: nextQuantity }));

    const timers = qtyTimers.current;
    if (timers[item.itemId]) clearTimeout(timers[item.itemId]);
    timers[item.itemId] = setTimeout(async () => {
      delete timers[item.itemId];
      try {
        const updated = await updateCartItem(item.itemId, { quantity: nextQuantity });
        setCart(updated);
      } catch {
        // ignore
      } finally {
        setPendingQty((prev) => {
          const { [item.itemId]: _omit, ...rest } = prev;
          return rest;
        });
      }
    }, QTY_DEBOUNCE_MS);
  }

  async function remove(itemId: string) {
    if (qtyTimers.current[itemId]) {
      clearTimeout(qtyTimers.current[itemId]);
      delete qtyTimers.current[itemId];
    }
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

  function toggleGroupSelected(group: CartGroup) {
    const groupIds = group.items.map((i) => i.itemId);
    const allSelected = groupIds.every((id) => selected.has(id));
    setSelected((prev) => {
      const next = new Set(prev);
      if (allSelected) {
        groupIds.forEach((id) => next.delete(id));
      } else {
        groupIds.forEach((id) => next.add(id));
      }
      return next;
    });
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

  function effectiveSubtotal(item: CartItem) {
    return item.unitPrice * effectiveQuantity(item);
  }

  const items = cart?.items ?? [];
  const selectedItems = items.filter((i) => selected.has(i.itemId));
  const subtotal = selectedItems.reduce((s, i) => s + effectiveSubtotal(i), 0);
  const shipping = subtotal === 0 || subtotal >= 50000 ? 0 : 3000;
  const total = subtotal + shipping;
  const count = items.reduce((a, i) => a + effectiveQuantity(i), 0);
  const allSelected = items.length > 0 && selected.size === items.length;
  const groups = groupByProduct(items);

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

          {groups.map((group) => {
            const groupSelectedItems = group.items.filter((i) => selected.has(i.itemId));
            const groupSubtotal = groupSelectedItems.reduce((s, i) => s + effectiveSubtotal(i), 0);
            const groupShipping = groupSubtotal === 0 || groupSubtotal >= 50000 ? 0 : 3000;
            const groupTotal = groupSubtotal + groupShipping;
            const groupAllSelected = group.items.every((i) => selected.has(i.itemId));

            return (
              <div key={group.productId} className="mcCartGroup">
                <div className="mcCartGroupHeader">
                  <input
                    type="checkbox"
                    checked={groupAllSelected}
                    onChange={() => toggleGroupSelected(group)}
                    style={{ flex: "none", width: 18, height: 18 }}
                  />
                  <div
                    style={{ display: "flex", alignItems: "center", gap: "12px", flex: 1, minWidth: 0, cursor: "pointer" }}
                    onClick={() => router.push(`/products/${group.productId}`)}
                  >
                    {productImages[group.productId] ? (
                      <img
                        src={productImages[group.productId]}
                        alt={group.productName}
                        className="mcCartItemImg"
                        style={{ width: 44, height: 44, objectFit: "cover" }}
                      />
                    ) : (
                      <div className="mcCartItemImg" style={{ width: 44, height: 44, fontSize: 18 }}>🛍️</div>
                    )}
                    <div className="mcCartGroupName">{group.productName}</div>
                  </div>
                </div>

                {group.items.map((item) => (
                  <div key={item.itemId} className="mcCartGroupRow">
                    <input
                      type="checkbox"
                      checked={selected.has(item.itemId)}
                      onChange={() => toggleSelected(item.itemId)}
                      style={{ flex: "none", width: 18, height: 18, alignSelf: "flex-start", marginTop: "2px" }}
                    />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", gap: "8px" }}>
                        <div style={{ fontSize: "13px", color: "var(--color-muted)" }}>
                          {item.selectedOptionValue || "기본"}
                        </div>
                        <button
                          type="button"
                          onClick={() => remove(item.itemId)}
                          aria-label="삭제"
                          style={{ flex: "none", border: "none", background: "transparent", cursor: "pointer", color: "#bbb", padding: 0, fontSize: "16px" }}
                        >
                          ✕
                        </button>
                      </div>
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginTop: "10px" }}>
                        <div className="mcQtyControl">
                          <button type="button" className="mcQtyBtn" onClick={() => bumpQty(item, -1)}>−</button>
                          <span style={{ width: 34, textAlign: "center", fontSize: "14px", fontWeight: 600 }}>{effectiveQuantity(item)}</span>
                          <button type="button" className="mcQtyBtn" onClick={() => bumpQty(item, 1)}>+</button>
                        </div>
                        <div style={{ fontSize: "16px", fontWeight: 800 }}>{effectiveSubtotal(item).toLocaleString("ko-KR")}원</div>
                      </div>
                    </div>
                  </div>
                ))}

                <div className="mcCartGroupFooter">
                  <span>총 배송비</span>
                  <span>{groupShipping === 0 ? "무료" : `${groupShipping.toLocaleString("ko-KR")}원`}</span>
                </div>
                <div className="mcCartGroupFooter">
                  <span>예상 주문금액</span>
                  <span style={{ fontWeight: 700 }}>{groupTotal.toLocaleString("ko-KR")}원</span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {!loading && items.length > 0 && (
        <div className="mcCartSummaryBar">
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
      )}
    </div>
  );
}
