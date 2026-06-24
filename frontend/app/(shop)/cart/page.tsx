"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getCart, removeCartItem, updateCartItem, type Cart } from "@/lib/api";

export default function CartPage() {
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);

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
    } catch {
      // ignore
    }
  }

  const items = cart?.items ?? [];
  const subtotal = items.reduce((s, i) => s + i.subtotal, 0);
  const shipping = subtotal === 0 || subtotal >= 50000 ? 0 : 3000;
  const total = subtotal + shipping;
  const count = items.reduce((a, i) => a + i.quantity, 0);

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
          {items.map((item) => (
            <div key={item.itemId} className="mcCartItem">
              <div className="mcCartItemImg">🛍️</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: "8px" }}>
                  <div style={{ fontSize: "14px", fontWeight: 600, lineHeight: 1.4 }}>{item.productName}</div>
                  <button
                    type="button"
                    onClick={() => remove(item.itemId)}
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
                  <div className="mcQtyControl">
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
            <button type="button" className="mcBtn mcBtnPrimary" onClick={() => router.push("/checkout")}>
              {total.toLocaleString("ko-KR")}원 결제하기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
