"use client";

import { useEffect, useState } from "react";
import { useSearchOverlay } from "@/lib/search-overlay";
import type { Product } from "@/lib/api";
import ProductCard from "./product-card";

export default function SearchOverlayPanel() {
  const { isOpen, initialQuery, closeSearch } = useSearchOverlay();
  const [value, setValue] = useState(initialQuery);
  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [results, setResults] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) setValue(initialQuery);
  }, [isOpen, initialQuery]);

  useEffect(() => {
    if (!isOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    fetch("/api/proxy/products")
      .then((r) => r.json())
      .then((data) => setAllProducts(data.content ?? []))
      .catch(() => setAllProducts([]));
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const trimmed = value.trim();
    if (!trimmed) {
      setResults([]);
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    fetch(`/api/proxy/products?q=${encodeURIComponent(trimmed)}`, { signal: controller.signal })
      .then((r) => r.json())
      .then((data) => setResults(data.content ?? []))
      .catch(() => {})
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, [isOpen, value]);

  if (!isOpen) return null;

  const trimmed = value.trim();

  return (
    <div className="mcSearchOverlay">
      <div className="mcSearchOverlayHeader">
        <button type="button" className="mcSearchBackBtn" onClick={closeSearch} aria-label="검색 닫기">
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M18 4 8 12l10 8" />
          </svg>
        </button>
        <div className="mcSearchOverlayBarSlot">
          <div className="mcSearchInputWrap">
            <svg width="18" height="18" fill="none" stroke="#222" strokeWidth="1.9" strokeLinecap="round" aria-hidden="true">
              <circle cx="8" cy="8" r="6" />
              <path d="m17 17-4-4" />
            </svg>
            <input
              type="search"
              placeholder="상품명, 브랜드 검색"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              aria-label="상품 검색"
              autoFocus
            />
            {value && (
              <button type="button" className="mcSearchClear" onClick={() => setValue("")} aria-label="검색어 초기화">
                ✕
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="mcSearchOverlayBody">
        {!trimmed && (
          <>
            <div style={{ padding: "4px 20px 0" }}>
              <div style={{ fontSize: "15px", fontWeight: 700, marginBottom: "12px" }}>추천 검색어</div>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
                {allProducts.slice(0, 5).map((p) => (
                  <button key={p.id} type="button" className="mcChip" onClick={() => setValue(p.name)}>
                    {p.name}
                  </button>
                ))}
              </div>
            </div>
            <div className="mcSectionTitle">이런 상품은 어때요</div>
            <div className="mcGrid">
              {allProducts.slice(0, 4).map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </div>
          </>
        )}

        {trimmed && results.length > 0 && (
          <>
            <div style={{ padding: "16px 20px 8px", display: "flex", alignItems: "baseline", gap: "8px" }}>
              <span style={{ fontSize: "16px", fontWeight: 700 }}>&lsquo;{trimmed}&rsquo; 검색 결과</span>
              <span style={{ fontSize: "13px", color: "var(--color-muted-soft)" }}>{results.length}개</span>
            </div>
            <div className="mcGrid">
              {results.map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </div>
          </>
        )}

        {trimmed && !loading && results.length === 0 && (
          <div className="mcEmptyState">
            <div className="mcEmptyIcon">🔍</div>
            <div className="mcEmptyTitle">검색 결과가 없어요</div>
            <div className="mcEmptyDesc">
              &lsquo;{trimmed}&rsquo;에 대한 상품을 찾지 못했어요.<br />
              다른 검색어로 다시 시도해보세요.
            </div>
          </div>
        )}
      </div>

      <div className="mcSearchOverlayFooter">
        <span className="mcSearchOverlayHint">자동저장 끄기 · 도움말</span>
        <button type="button" className="mcSearchCloseBtn" onClick={closeSearch}>
          닫기
        </button>
      </div>
    </div>
  );
}
