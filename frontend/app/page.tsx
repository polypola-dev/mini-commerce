import { getProducts } from "@/lib/api";
import OrderPanel from "./order-panel";
import AuthHeader from "./auth-header";
import CartButton from "./cart-button";
import CartHeader from "./cart-header";
import SearchBar from "./search-bar";
import ReviewSection from "./review-section";
import NotificationBell from "./notification-bell";
import { createClient } from "@/lib/supabase/server";

export default async function HomePage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const { q } = await searchParams;
  const [products, supabase] = await Promise.all([getProducts(q), createClient()]);
  const { data: { user } } = await supabase.auth.getUser();
  const currentUserId = user?.id ?? undefined;

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Mini Commerce</p>
          <h1>상품 재고를 Redis 예약으로 보호하는 주문 흐름</h1>
        </div>
        <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
          <NotificationBell />
          <CartHeader />
          <AuthHeader />
        </div>
      </section>

      <SearchBar initialQuery={q ?? ""} />

      <section className="productGrid" aria-label="상품 목록">
        {products.length === 0 && (
          <p className="emptyState">검색 결과가 없습니다.</p>
        )}
        {products.map((product) => (
          <article className="productCard" key={product.id}>
            <div className="imageFrame">
              <img src={product.imageUrl} alt="" />
            </div>
            <div className="productBody">
              <div>
                <h2>{product.name}</h2>
                <p>{product.description}</p>
              </div>
              <div className="metaRow">
                <strong>{product.price.toLocaleString("ko-KR")}원</strong>
                <span>재고 {product.availableStock}</span>
              </div>
              <CartButton product={{ id: product.id, name: product.name, price: product.price, options: product.options }} />
              <OrderPanel product={product} />
              <ReviewSection productId={product.id} currentUserId={currentUserId} />
            </div>
          </article>
        ))}
      </section>
    </main>
  );
}
