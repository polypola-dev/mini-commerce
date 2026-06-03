"use client";

import { useState } from "react";
import { completeFakePayment, createOrder, type Product } from "@/lib/api";

type Props = {
  product: Product;
};

export default function OrderPanel({ product }: Props) {
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function submitOrder() {
    setPending(true);
    setMessage(null);

    try {
      const order = await createOrder({
        items: [{ productId: product.id, quantity }],
      });
      const paidOrder = await completeFakePayment(order.orderId);
      setMessage(`결제 완료: ${paidOrder.orderId} / ${paidOrder.status}`);
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
      <button disabled={pending || product.availableStock < 1} onClick={submitOrder}>
        {pending ? "주문 중" : "주문하기"}
      </button>
      {message ? <p className="message">{message}</p> : null}
    </div>
  );
}
