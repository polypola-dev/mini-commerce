import { getProducts } from "@/lib/api";
import ProductCard from "../product-card";
import Link from "next/link";

export default async function SearchContent({ query }: { query: string }) {
  const allProducts = await getProducts();
  const results = query ? await getProducts(query) : [];

  return (
    <>
      {!query && (
        <>
          <div style={{ padding: "4px 20px 0" }}>
            <div style={{ fontSize: "15px", fontWeight: 700, marginBottom: "12px" }}>추천 검색어</div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
              {allProducts.slice(0, 5).map((p) => (
                <Link key={p.id} href={`/search?q=${encodeURIComponent(p.name)}`} className="mcChip">
                  {p.name}
                </Link>
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

      {query && results.length > 0 && (
        <>
          <div style={{ padding: "16px 20px 8px", display: "flex", alignItems: "baseline", gap: "8px" }}>
            <span style={{ fontSize: "16px", fontWeight: 700 }}>&lsquo;{query}&rsquo; 검색 결과</span>
            <span style={{ fontSize: "13px", color: "var(--color-muted-soft)" }}>{results.length}개</span>
          </div>
          <div className="mcGrid">
            {results.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </>
      )}

      {query && results.length === 0 && (
        <div className="mcEmptyState">
          <div className="mcEmptyIcon">🔍</div>
          <div className="mcEmptyTitle">검색 결과가 없어요</div>
          <div className="mcEmptyDesc">
            &lsquo;{query}&rsquo;에 대한 상품을 찾지 못했어요.<br />
            다른 검색어로 다시 시도해보세요.
          </div>
        </div>
      )}
    </>
  );
}
