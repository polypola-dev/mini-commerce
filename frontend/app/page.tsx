import { getProducts } from "@/lib/api";
import OrderPanel from "./order-panel";
import AuthHeader from "./auth-header";

export default async function HomePage() {
  const products = await getProducts();

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Mini Commerce</p>
          <h1>상품 재고를 Redis 예약으로 보호하는 주문 흐름</h1>
        </div>
        <AuthHeader />
      </section>

      <section className="productGrid" aria-label="상품 목록">
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
              <OrderPanel product={product} />
            </div>
          </article>
        ))}
      </section>
    </main>
  );
}
