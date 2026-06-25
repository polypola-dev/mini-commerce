import Link from "next/link";
import { getProducts } from "@/lib/api";
import ProductCard from "../product-card";

export default async function PromotionPage() {
  const { content: products } = await getProducts();

  return (
    <div style={{ paddingBottom: "28px" }}>
      <div className="mcPageHeader">
        <Link href="/" aria-label="뒤로" className="mcPlainBackBtn">
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="m14 5-7 7 7 7" />
          </svg>
        </Link>
        <span className="mcPageHeaderTitle">기획전</span>
      </div>

      <div style={{ padding: "4px 20px 0" }}>
        <div className="mcBanner">
          <div className="mcBannerInner">
            <img
              src="https://images.unsplash.com/photo-1483985988355-763728e1935b?w=800&q=80&auto=format&fit=crop"
              alt="이번 주 특가전"
            />
            <div className="mcBannerOverlay" />
            <div className="mcBannerText">
              <div style={{ fontSize: "12px", fontWeight: 600, opacity: 0.9, marginBottom: "6px" }}>THIS WEEK</div>
              <div style={{ fontSize: "22px", fontWeight: 800, lineHeight: 1.25 }}>
                이번 주 특가전<br />인기 상품 둘러보기
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="mcSectionTitle">이번 주 특가전 상품</div>
      <div className="mcGrid">
        {products.length === 0 && <p className="emptyState">등록된 상품이 없습니다.</p>}
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </div>
  );
}
