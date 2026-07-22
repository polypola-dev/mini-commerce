"use client";

import Link from "next/link";
import { type Product } from "@/lib/api";
import { useWishlist } from "@/lib/wishlist";
import ProductCard from "../product-card";

// 상품 목록은 서버 컴포넌트(page.tsx)에서 로드해 내려준다. getProducts는 서버 전용
// API_BASE_URL로 백엔드를 직접 호출하므로 클라이언트에서 부르면 안 된다(B8). 찜 필터만
// 클라이언트에서 useWishlist ids로 수행한다.
export default function WishlistView({ products }: { products: Product[] }) {
  const { ids } = useWishlist();
  const wished = products.filter((p) => ids.includes(p.id));

  return (
    <div>
      <div className="mcListHeader">
        <h1>찜한 상품</h1>
        <span className="mcListHeaderCount">{ids.length}개</span>
      </div>

      {ids.length === 0 && (
        <div className="mcEmptyState">
          <div className="mcEmptyIcon">🤍</div>
          <div className="mcEmptyTitle">아직 찜한 상품이 없어요</div>
          <div className="mcEmptyDesc">상품의 하트를 눌러 저장해보세요.</div>
          <Link href="/" className="mcEmptyCta">상품 보러 가기</Link>
        </div>
      )}

      {wished.length > 0 && (
        <div className="mcGrid">
          {wished.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
