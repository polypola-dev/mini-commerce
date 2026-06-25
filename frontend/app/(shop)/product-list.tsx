"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import ProductCard from "./product-card";
import type { PageResponse, Product } from "@/lib/api";

type ProductListProps = {
  initialProducts: Product[];
  initialPage: number;
  initialTotalPages: number;
  pageSize: number;
  q?: string;
};

export default function ProductList({
  initialProducts,
  initialPage,
  initialTotalPages,
  pageSize,
  q,
}: ProductListProps) {
  const [products, setProducts] = useState<Product[]>(initialProducts);
  const [page, setPage] = useState(initialPage);
  const [totalPages, setTotalPages] = useState(initialTotalPages);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  const hasMore = page + 1 < totalPages;

  useEffect(() => {
    if (typeof window === "undefined" || !("scrollRestoration" in window.history)) return;
    const prev = window.history.scrollRestoration;
    window.history.scrollRestoration = "manual";
    window.scrollTo(0, 0);
    return () => {
      window.history.scrollRestoration = prev;
    };
  }, []);

  const loadMore = useCallback(async () => {
    if (loading || !hasMore) return;
    setLoading(true);
    setError(false);
    try {
      // getProducts()는 서버 전용 절대 URL(API_BASE_URL)로 fetch하므로
      // 브라우저(클라이언트 컴포넌트)에서는 호출할 수 없다. 대신 동일한
      // 응답 형태(PageResponse<Product>)를 반환하는 BFF 프록시 라우트를 호출한다.
      const nextPage = page + 1;
      const params = new URLSearchParams({ page: String(nextPage), size: String(pageSize) });
      if (q) params.set("q", q);

      const response = await fetch(`/api/proxy/products?${params.toString()}`, { cache: "no-store" });
      if (!response.ok) throw new Error("Failed to fetch products");

      const res: PageResponse<Product> = await response.json();
      setProducts((prev) => [...prev, ...res.content]);
      setPage(res.page);
      setTotalPages(res.totalPages);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [loading, hasMore, page, pageSize, q]);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el || !hasMore) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          loadMore();
        }
      },
      { rootMargin: "200px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [hasMore, loadMore]);

  return (
    <>
      <div className="mcGrid">
        {products.length === 0 && <p className="emptyState">등록된 상품이 없습니다.</p>}
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      {hasMore && (
        <div ref={sentinelRef} className="mcInfiniteSentinel">
          {loading && <span className="mcInfiniteSpinner" aria-label="상품 더 불러오는 중" />}
        </div>
      )}

      {!hasMore && products.length > 0 && <p className="mcEndOfList">마지막 상품까지 모두 확인했어요.</p>}

      {error && <p className="mcEndOfList mcEndOfListError">상품을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.</p>}
    </>
  );
}
