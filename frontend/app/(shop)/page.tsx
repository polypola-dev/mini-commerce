import Link from "next/link";
import { getProducts } from "@/lib/api";
import NotificationBell from "./notification-bell";
import ProductList from "./product-list";
import SearchTriggerButton from "./search-trigger-button";

const CATEGORIES = ["전체", "패션", "가전", "생활", "뷰티", "식품"];
const PAGE_SIZE = 20;

export default async function HomePage() {
  const initialPageResponse = await getProducts({ page: 0, size: PAGE_SIZE });

  return (
    <div>
      <div className="mcTopBar">
        <div className="mcTopBarRow">
          <div className="mcLogo">
            <span>mini</span>commerce
          </div>
          <div style={{ display: "flex", gap: "4px", alignItems: "center" }}>
            <NotificationBell />
            <Link href="/cart" className="mcIconBtn" aria-label="장바구니">
              <svg width="23" height="23" fill="none" stroke="#222" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
                <path d="M6 6h15l-1.5 9h-12z" />
                <path d="M6 6 5 2H2" />
                <circle cx="9" cy="20" r="1.4" />
                <circle cx="18" cy="20" r="1.4" />
              </svg>
            </Link>
          </div>
        </div>
        <SearchTriggerButton className="mcSearchTrigger">
          <svg width="18" height="18" fill="none" stroke="#6a6a6a" strokeWidth="1.8" strokeLinecap="round">
            <circle cx="8" cy="8" r="6" />
            <path d="m17 17-4-4" />
          </svg>
          <span>찾는 상품을 검색해보세요</span>
        </SearchTriggerButton>
      </div>

      <Link href="/promotion" className="mcBanner">
        <div className="mcBannerInner">
          <img
            src="https://images.unsplash.com/photo-1483985988355-763728e1935b?w=800&q=80&auto=format&fit=crop"
            alt="기획전"
          />
          <div className="mcBannerOverlay" />
          <div className="mcBannerText">
            <div style={{ fontSize: "12px", fontWeight: 600, opacity: 0.9, marginBottom: "6px" }}>THIS WEEK</div>
            <div style={{ fontSize: "22px", fontWeight: 800, lineHeight: 1.25 }}>
              이번 주 특가전<br />인기 상품 둘러보기
            </div>
          </div>
        </div>
      </Link>

      <div className="mcChipRow">
        {CATEGORIES.map((c) => (
          <Link key={c} href={c === "전체" ? "/search" : `/search?q=${encodeURIComponent(c)}`} className="mcChip">
            {c}
          </Link>
        ))}
      </div>

      <div className="mcSectionTitle">지금 만나보는 상품</div>
      <ProductList
        initialProducts={initialPageResponse.content}
        initialPage={initialPageResponse.page}
        initialTotalPages={initialPageResponse.totalPages}
        pageSize={PAGE_SIZE}
      />
    </div>
  );
}
