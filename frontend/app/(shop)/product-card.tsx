"use client";

import Link from "next/link";
import { useWishlist } from "@/lib/wishlist";
import type { Product } from "@/lib/api";

export default function ProductCard({ product }: { product: Product }) {
  const { isSaved, toggle } = useWishlist();
  const saved = isSaved(product.id);
  const soldOut = product.availableStock < 1;

  return (
    <Link href={`/products/${product.id}`} className="mcCard">
      <div className="mcCardImageFrame">
        <img src={product.imageUrl} alt={product.name} />
        {soldOut && <span className="mcCardBadge" style={{ background: "var(--color-muted)" }}>품절</span>}
        <button
          type="button"
          className="mcHeartBtn"
          aria-label="찜"
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            toggle(product.id);
          }}
        >
          <svg width="23" height="23" viewBox="0 0 32 32">
            <path
              d="M16 28S4 20.5 4 12.5C4 8.36 7.36 5 11.5 5c2.54 0 4.78 1.27 6.5 3.2C19.72 6.27 21.96 5 24.5 5 28.64 5 32 8.36 32 12.5 32 20.5 16 28 16 28Z"
              transform="translate(-1 0)"
              fill={saved ? "var(--color-primary)" : "rgba(0,0,0,0.45)"}
              stroke={saved ? "var(--color-primary)" : "#ffffff"}
              strokeWidth="2.4"
            />
          </svg>
        </button>
      </div>
      <div>
        <div className="mcCardTitle">{product.name}</div>
        <div className="mcCardPrice">{product.price.toLocaleString("ko-KR")}원</div>
      </div>
    </Link>
  );
}
