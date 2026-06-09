"use client";

import { useState } from "react";
import { completeFakePayment, createOrder, type Product } from "@/lib/api";

type ShippingForm = {
  shippingRecipient: string;
  shippingPhone: string;
  shippingAddress: string;
  shippingDetailAddress: string;
  shippingZipCode: string;
};

type Props = {
  product: Product;
};

export default function OrderPanel({ product }: Props) {
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [showShipping, setShowShipping] = useState(false);
  const [shipping, setShipping] = useState<ShippingForm>({
    shippingRecipient: "",
    shippingPhone: "",
    shippingAddress: "",
    shippingDetailAddress: "",
    shippingZipCode: "",
  });

  function updateShipping(field: keyof ShippingForm, value: string) {
    setShipping((prev) => ({ ...prev, [field]: value }));
  }

  async function submitOrder() {
    setPending(true);
    setMessage(null);

    try {
      const order = await createOrder({
        items: [{ productId: product.id, quantity }],
        ...shipping,
      });
      const paidOrder = await completeFakePayment(order.orderId);
      setMessage(`결제 완료 ✓ 주문번호: ${paidOrder.orderId.slice(0, 8)}…`);
      setShowShipping(false);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "주문 실패");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="orderPanel">
      <label>
        수량
        <input
          min={1}
          max={Math.max(product.availableStock, 1)}
          type="number"
          value={quantity}
          onChange={(event) => setQuantity(Number(event.target.value))}
        />
      </label>

      {!showShipping ? (
        <button
          disabled={product.availableStock < 1}
          onClick={() => setShowShipping(true)}
        >
          주문하기
        </button>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem", marginTop: "0.5rem" }}>
          <p style={{ fontSize: "0.8rem", fontWeight: 600, color: "var(--muted)" }}>배송지 정보</p>
          {[
            { label: "받는 분", field: "shippingRecipient" as const, placeholder: "홍길동" },
            { label: "연락처", field: "shippingPhone" as const, placeholder: "010-0000-0000" },
            { label: "우편번호", field: "shippingZipCode" as const, placeholder: "12345" },
            { label: "주소", field: "shippingAddress" as const, placeholder: "서울시 강남구..." },
            { label: "상세주소", field: "shippingDetailAddress" as const, placeholder: "101동 201호" },
          ].map(({ label, field, placeholder }) => (
            <label key={field} style={{ fontSize: "0.8rem", display: "flex", flexDirection: "column", gap: "0.2rem" }}>
              {label}
              <input
                type="text"
                placeholder={placeholder}
                value={shipping[field]}
                onChange={(e) => updateShipping(field, e.target.value)}
                style={{ padding: "0.35rem 0.5rem", borderRadius: "4px", border: "1px solid var(--border)", fontSize: "0.8rem" }}
              />
            </label>
          ))}
          <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.25rem" }}>
            <button disabled={pending} onClick={submitOrder} style={{ flex: 1 }}>
              {pending ? "주문 중..." : "결제하기"}
            </button>
            <button
              onClick={() => setShowShipping(false)}
              style={{ padding: "0.4rem 0.75rem", borderRadius: "6px", border: "1px solid var(--border)", background: "none", cursor: "pointer", fontSize: "0.85rem" }}
            >
              취소
            </button>
          </div>
        </div>
      )}
      {message ? <p className="message">{message}</p> : null}
    </div>
  );
}
