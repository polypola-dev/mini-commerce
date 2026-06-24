"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { addToCart, type Product, type ProductOption } from "@/lib/api";
import { useWishlist } from "@/lib/wishlist";

type Mode = "cart" | "buy" | null;

export default function ActionBar({ product }: { product: Product }) {
  const router = useRouter();
  const { isSaved, toggle } = useWishlist();
  const [sheetMode, setSheetMode] = useState<Mode>(null);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const hasOptions = product.options.length > 0;
  const optionGroups = product.options.reduce<Record<string, ProductOption[]>>((acc, opt) => {
    (acc[opt.optionGroupName] ??= []).push(opt);
    return acc;
  }, {});

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 1900);
  }

  async function commit(mode: Mode) {
    try {
      await addToCart({
        productId: product.id,
        productName: product.name,
        unitPrice: product.price,
        quantity: 1,
        ...(selectedOptionId ? { selectedOptionId } : {}),
      });
      setSheetMode(null);
      setSelectedOptionId(null);
      if (mode === "buy") router.push("/checkout");
      else showToast("장바구니에 담았어요");
    } catch (e) {
      showToast(e instanceof Error ? e.message : "담기에 실패했어요");
    }
  }

  function open(mode: Mode) {
    if (hasOptions) {
      setSelectedOptionId(null);
      setSheetMode(mode);
    } else {
      commit(mode);
    }
  }

  const saved = isSaved(product.id);

  return (
    <>
      <div className="mcActionBar">
        <button
          type="button"
          className="mcHeartCircleBtn"
          aria-label="찜"
          onClick={() => toggle(product.id)}
        >
          <svg width="22" height="22" viewBox="0 0 32 32">
            <path
              d="M16 28S4 20.5 4 12.5C4 8.36 7.36 5 11.5 5c2.54 0 4.78 1.27 6.5 3.2C19.72 6.27 21.96 5 24.5 5 28.64 5 32 8.36 32 12.5 32 20.5 16 28 16 28Z"
              transform="translate(-1 0)"
              fill={saved ? "var(--color-primary)" : "none"}
              stroke={saved ? "var(--color-primary)" : "#222"}
              strokeWidth="2.4"
            />
          </svg>
        </button>
        <div style={{ flex: 1 }}>
          <button type="button" className="mcBtn mcBtnSecondary" onClick={() => open("cart")} disabled={product.availableStock < 1}>
            장바구니
          </button>
        </div>
        <div style={{ flex: 1 }}>
          <button type="button" className="mcBtn mcBtnPrimary" onClick={() => open("buy")} disabled={product.availableStock < 1}>
            바로 구매
          </button>
        </div>
      </div>

      {sheetMode && (
        <div className="mcSheetOverlay" onClick={() => setSheetMode(null)}>
          <div className="mcSheet" onClick={(e) => e.stopPropagation()}>
            <div className="mcSheetHandle" />
            <div style={{ display: "flex", gap: "12px", marginBottom: "18px" }}>
              <div
                style={{
                  flex: "none",
                  width: 56,
                  height: 56,
                  borderRadius: 10,
                  backgroundImage: `url('${product.imageUrl}')`,
                  backgroundSize: "cover",
                  backgroundPosition: "center",
                }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 600, lineHeight: 1.4 }}>{product.name}</div>
                <div style={{ fontSize: 16, fontWeight: 800, marginTop: 4 }}>
                  {product.price.toLocaleString("ko-KR")}원
                </div>
              </div>
            </div>

            {Object.entries(optionGroups).map(([groupName, opts]) => (
              <div key={groupName}>
                <div className="mcSheetOptionLabel">{groupName} 선택</div>
                <div className="mcSheetOptions">
                  {opts.map((opt) => (
                    <button
                      key={opt.id}
                      type="button"
                      className={`mcOptionPill${selectedOptionId === opt.id ? " selected" : ""}`}
                      onClick={() => setSelectedOptionId(opt.id)}
                    >
                      {opt.optionValue}
                      {opt.additionalPrice > 0 && ` (+${opt.additionalPrice.toLocaleString("ko-KR")}원)`}
                    </button>
                  ))}
                </div>
              </div>
            ))}

            <button
              type="button"
              className="mcBtn mcBtnPrimary"
              disabled={selectedOptionId === null}
              onClick={() => commit(sheetMode)}
            >
              {sheetMode === "buy" ? "바로 구매하기" : "장바구니 담기"}
            </button>
          </div>
        </div>
      )}

      {toast && (
        <div className="mcToast">
          <span>{toast}</span>
          <span onClick={() => router.push("/cart")} style={{ color: "#fff", fontWeight: 700, textDecoration: "underline", cursor: "pointer" }}>
            보기
          </span>
        </div>
      )}
    </>
  );
}
