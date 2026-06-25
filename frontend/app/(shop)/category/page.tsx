import Link from "next/link";
import { getProducts } from "@/lib/api";
import ProductCard from "../product-card";

const CATEGORIES = [
  { label: "전체", photo: "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=200&q=80&auto=format&fit=crop" },
  { label: "패션", photo: "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=200&q=80&auto=format&fit=crop" },
  { label: "가전", photo: "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=200&q=80&auto=format&fit=crop" },
  { label: "생활", photo: "https://images.unsplash.com/photo-1556911220-bff31c812dba?w=200&q=80&auto=format&fit=crop" },
  { label: "뷰티", photo: "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=200&q=80&auto=format&fit=crop" },
  { label: "식품", photo: "https://images.unsplash.com/photo-1542838132-92c53300491e?w=200&q=80&auto=format&fit=crop" },
];

const POPULAR_KEYWORDS = ["여성 원피스", "무선 이어폰", "캠핑용품"];

export default async function CategoryPage() {
  const products = await getProducts();

  return (
    <div>
      <div className="mcPageHeader">
        <Link href="/" aria-label="뒤로" className="mcPlainBackBtn">
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="m14 5-7 7 7 7" />
          </svg>
        </Link>
        <span className="mcPageHeaderTitle">카테고리</span>
      </div>

      <div style={{ padding: "4px 20px 0" }}>
        <Link href="/search" className="mcSearchTrigger">
          <svg width="17" height="17" fill="none" stroke="#6a6a6a" strokeWidth="1.8" strokeLinecap="round">
            <circle cx="8" cy="8" r="6" />
            <path d="m17 17-4-4" />
          </svg>
          <span>상품, 브랜드 검색</span>
        </Link>
      </div>

      <div className="mcCategoryTileGrid">
        {CATEGORIES.map((c) => (
          <Link
            key={c.label}
            href={c.label === "전체" ? "/search" : `/search?q=${encodeURIComponent(c.label)}`}
            className="mcCategoryTile"
          >
            <div className="mcCategoryTileIcon" style={{ backgroundImage: `url('${c.photo}')` }} role="img" aria-label={c.label} />
            <span>{c.label}</span>
          </Link>
        ))}
      </div>

      <div className="mcSectionTitle">자주 찾는 카테고리</div>
      <div className="mcKeywordList">
        {POPULAR_KEYWORDS.map((keyword) => (
          <Link key={keyword} href={`/search?q=${encodeURIComponent(keyword)}`} className="mcKeywordListItem">
            <span>{keyword}</span>
            <span className="mcKeywordListChevron">›</span>
          </Link>
        ))}
      </div>

      <div className="mcSectionTitle">추천 상품</div>
      <div className="mcGrid">
        {products.length === 0 && <p className="emptyState">등록된 상품이 없습니다.</p>}
        {products.slice(0, 4).map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </div>
  );
}
