"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getProducts, type Product } from "@/lib/api";
import { useWishlist } from "@/lib/wishlist";
import ProductCard from "../product-card";

export default function WishlistPage() {
  const { ids } = useWishlist();
  const [products, setProducts] = useState<Product[] | null>(null);

  useEffect(() => {
    getProducts()
      .then(setProducts)
      .catch(() => setProducts([]));
  }, []);

  const wished = (products ?? []).filter((p) => ids.includes(p.id));

  return (
    <div>
      <div className="mcListHeader">
        <h1>찜한 상품</h1>
        <span className="mcListHeaderCount">{ids.length}개</span>
      </div>

      {products && ids.length === 0 && (
        <div className="mcEmptyState">
          <div className="mcEmptyIcon">🤍</div>
          <div className="mcEmptyTitle">아직 찜한 상품이 없어요</div>
          <div className="mcEmptyDesc">상품의 하트를 눌러 저장해보세요.</div>
          <Link href="/" className="mcEmptyCta">상품 보러 가기</Link>
        </div>
      )}

      {products && wished.length > 0 && (
        <div className="mcGrid">
          {wished.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
