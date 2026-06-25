import { createClient } from "@/lib/supabase/server";
import { getProductById, getReviews } from "@/lib/api";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import ReviewSection from "../../review-section";
import HeroHeart from "./hero-heart";
import ActionBar from "./action-bar";
import ShareButton from "./share-button";

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  let product;
  try {
    product = await getProductById(id);
  } catch {
    notFound();
  }

  const [supabase, reviewData] = await Promise.all([
    createClient(),
    getReviews(id).catch(() => null),
  ]);
  const { data: { user } } = await supabase.auth.getUser();
  const currentUserId = user?.id ?? undefined;

  return (
    <div style={{ paddingBottom: "24px" }}>
      <div className="mcDetailHero">
        <div className="mcDetailHeroImg" style={{ backgroundImage: `url('${product.imageUrl}')` }} />
        <div className="mcDetailHeroBar">
          <Link href="/" className="mcBackBtn" aria-label="뒤로">
            <svg width="20" height="20" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="m13 5-7 7 7 7" />
            </svg>
          </Link>
          <div style={{ display: "flex", gap: "8px" }}>
            <ShareButton />
            <HeroHeart productId={product.id} />
          </div>
        </div>
      </div>

      <div style={{ padding: "20px 20px 0" }}>
        <div style={{ fontSize: "21px", fontWeight: 700, lineHeight: 1.3, marginBottom: "10px" }}>{product.name}</div>
        {reviewData && reviewData.totalCount > 0 && (
          <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "14px" }}>
            <span style={{ fontSize: "13px", fontWeight: 700 }}>★ {reviewData.averageRating}</span>
            <span style={{ fontSize: "13px", color: "var(--color-muted)" }}>리뷰 {reviewData.totalCount}개</span>
          </div>
        )}
        <div style={{ fontSize: "26px", fontWeight: 800, color: "#222" }}>
          {product.price.toLocaleString("ko-KR")}원
        </div>
        <div style={{ fontSize: "13px", color: "var(--color-muted)", marginTop: "8px" }}>
          {product.availableStock > 0 ? `재고 ${product.availableStock}개` : "품절"}
        </div>
      </div>

      <div className="mcDivider8" style={{ marginTop: "22px" }} />

      <div className="mcInfoBlock">
        <div style={{ fontSize: "17px", fontWeight: 700, marginBottom: "12px" }}>상품 정보</div>
        <div style={{ fontSize: "15px", lineHeight: 1.65, color: "var(--color-body)" }}>{product.description}</div>

        {product.options.length > 0 && (
          <div className="mcInfoTable">
            <div className="mcInfoRow">
              <span>옵션</span>
              <span>{[...new Set(product.options.map((o) => o.optionGroupName))].join(" · ")}</span>
            </div>
          </div>
        )}
      </div>

      <div className="mcDivider8" />

      <div style={{ padding: "22px 20px 0" }}>
        <Suspense fallback={null}>
          <ReviewSection productId={product.id} currentUserId={currentUserId} />
        </Suspense>
      </div>

      <ActionBar product={product} />
    </div>
  );
}
