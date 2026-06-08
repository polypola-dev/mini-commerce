"use client";

import { useState } from "react";
import { addToCart, type ProductOption } from "@/lib/api";

type Props = {
  product: { id: string; name: string; price: number; options?: ProductOption[] };
};

export default function CartButton({ product }: Props) {
  const [state, setState] = useState<"idle" | "loading" | "done">("idle");
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);

  const hasOptions = product.options && product.options.length > 0;

  const isDisabled = state !== "idle" || (hasOptions && selectedOptionId === null);

  async function handleAddToCart() {
    setState("loading");
    try {
      await addToCart({
        productId: product.id,
        productName: product.name,
        unitPrice: product.price,
        quantity: 1,
        ...(selectedOptionId ? { selectedOptionId } : {}),
      });
      setState("done");
      setTimeout(() => setState("idle"), 2000);
    } catch {
      setState("idle");
    }
  }

  // 옵션을 optionGroupName 기준으로 그루핑
  const optionGroups = hasOptions
    ? product.options!.reduce<Record<string, ProductOption[]>>((acc, opt) => {
        if (!acc[opt.optionGroupName]) acc[opt.optionGroupName] = [];
        acc[opt.optionGroupName].push(opt);
        return acc;
      }, {})
    : {};

  return (
    <div style={{ display: "grid", gap: "10px" }}>
      {hasOptions &&
        Object.entries(optionGroups).map(([groupName, opts]) => (
          <div key={groupName}>
            <p style={{ margin: "0 0 6px", fontSize: "12px", fontWeight: 700, color: "#5a534b" }}>
              {groupName}
            </p>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
              {opts.map((opt) => (
                <button
                  key={opt.id}
                  className={`optionBtn${selectedOptionId === opt.id ? " selected" : ""}`}
                  onClick={() => setSelectedOptionId(opt.id)}
                  type="button"
                >
                  {opt.optionValue}
                  {opt.additionalPrice > 0 && (
                    <span style={{ marginLeft: "4px", fontSize: "11px" }}>
                      (+{opt.additionalPrice.toLocaleString("ko-KR")}원)
                    </span>
                  )}
                </button>
              ))}
            </div>
          </div>
        ))}

      <button
        className="cartAddBtn"
        disabled={isDisabled}
        onClick={handleAddToCart}
      >
        {state === "loading" ? "담는 중" : state === "done" ? "담겼어요 ✓" : "장바구니 담기"}
      </button>
    </div>
  );
}
