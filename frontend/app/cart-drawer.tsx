"use client";

import { useEffect, useState } from "react";
import {
  clearCart,
  createOrder,
  completeFakePayment,
  getCart,
  removeCartItem,
  updateCartItem,
  type Cart,
} from "@/lib/api";

type Props = {
  isOpen: boolean;
  onClose: () => void;
};

export default function CartDrawer({ isOpen, onClose }: Props) {
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;
    setLoading(true);
    getCart()
      .then(setCart)
      .catch(() => setCart(null))
      .finally(() => setLoading(false));
  }, [isOpen]);

  async function handleUpdateQty(itemId: string, quantity: number) {
    if (quantity < 1) return;
    try {
      const updated = await updateCartItem(itemId, { quantity });
      setCart(updated);
    } catch {
      // ignore
    }
  }

  async function handleRemove(itemId: string) {
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

  async function handleClear() {
    try {
      await clearCart();
      setCart((prev) => (prev ? { ...prev, items: [], totalAmount: 0 } : prev));
    } catch {
      // ignore
    }
  }

  async function handleOrder() {
    if (!cart || cart.items.length === 0) return;
    setMessage(null);
    try {
      const order = await createOrder({
        items: cart.items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
      });
      const paid = await completeFakePayment(order.orderId);
      setMessage(`결제 완료: ${paid.orderId}`);
      await handleClear();
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "주문 실패");
    }
  }

  if (!isOpen) return null;

  return (
    <>
      <div className="cartOverlay" onClick={onClose} />
      <div className="cartDrawer">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h2 style={{ margin: 0, fontSize: "1.1rem" }}>장바구니</h2>
          <button className="cartCloseBtn" onClick={onClose}>✕</button>
        </div>

        {loading && <p style={{ color: "#8a8278", fontSize: "13px" }}>불러오는 중…</p>}

        {!loading && cart && cart.items.length === 0 && (
          <p style={{ color: "#8a8278", fontSize: "13px" }}>장바구니가 비어 있습니다.</p>
        )}

        {!loading && !cart && (
          <p style={{ color: "#8a8278", fontSize: "13px" }}>로그인이 필요합니다.</p>
        )}

        {!loading && cart && cart.items.length > 0 && (
          <>
            <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 0 }}>
              {cart.items.map((item) => (
                <li key={item.itemId} className="cartItem">
                  <div>
                    <p className="cartItemName">{item.productName}</p>
                    {item.selectedOptionValue && (
                      <p style={{ margin: "2px 0 0", fontSize: "11px", color: "#a89f96" }}>
                        옵션: {item.selectedOptionValue}
                      </p>
                    )}
                    <p style={{ margin: "2px 0 0", fontSize: "12px", color: "#8a8278" }}>
                      {item.unitPrice.toLocaleString("ko-KR")}원
                    </p>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <div className="cartQtyRow">
                      <button className="cartQtyBtn" onClick={() => handleUpdateQty(item.itemId, item.quantity - 1)}>−</button>
                      <span style={{ minWidth: "20px", textAlign: "center" }}>{item.quantity}</span>
                      <button className="cartQtyBtn" onClick={() => handleUpdateQty(item.itemId, item.quantity + 1)}>+</button>
                    </div>
                    <span style={{ fontSize: "13px", fontWeight: 700, minWidth: "72px", textAlign: "right" }}>
                      {item.subtotal.toLocaleString("ko-KR")}원
                    </span>
                    <button className="cartDeleteBtn" onClick={() => handleRemove(item.itemId)}>✕</button>
                  </div>
                </li>
              ))}
            </ul>

            <button
              onClick={handleClear}
              style={{ alignSelf: "flex-start", border: 0, background: "transparent", color: "#8a8278", fontSize: "12px", cursor: "pointer", padding: 0, textDecoration: "underline" }}
            >
              전체 비우기
            </button>

            <div className="cartTotal">
              총 {cart.totalAmount.toLocaleString("ko-KR")}원
            </div>

            <button className="cartOrderBtn" onClick={handleOrder}>주문하기</button>
          </>
        )}

        {message && <p style={{ fontSize: "13px", color: "#006b5f" }}>{message}</p>}
      </div>
    </>
  );
}
