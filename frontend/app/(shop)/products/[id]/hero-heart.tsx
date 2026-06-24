"use client";

import { useWishlist } from "@/lib/wishlist";

export default function HeroHeart({ productId }: { productId: string }) {
  const { isSaved, toggle } = useWishlist();
  const saved = isSaved(productId);

  return (
    <button
      type="button"
      aria-label="찜"
      onClick={() => toggle(productId)}
      style={{
        width: 38,
        height: 38,
        borderRadius: "9999px",
        border: "none",
        background: "rgba(255,255,255,0.92)",
        cursor: "pointer",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        boxShadow: "var(--shadow-card)",
      }}
    >
      <svg width="19" height="19" viewBox="0 0 32 32">
        <path
          d="M16 28S4 20.5 4 12.5C4 8.36 7.36 5 11.5 5c2.54 0 4.78 1.27 6.5 3.2C19.72 6.27 21.96 5 24.5 5 28.64 5 32 8.36 32 12.5 32 20.5 16 28 16 28Z"
          transform="translate(-1 0)"
          fill={saved ? "var(--color-primary)" : "none"}
          stroke={saved ? "var(--color-primary)" : "#222"}
          strokeWidth="2.4"
        />
      </svg>
    </button>
  );
}
