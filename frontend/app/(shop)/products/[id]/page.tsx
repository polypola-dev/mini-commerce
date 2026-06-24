import { createClient } from "@/lib/supabase/server";
import { getProductById } from "@/lib/api";
import Link from "next/link";
import { notFound } from "next/navigation";
import OrderPanel from "@/app/order-panel";
import CartButton from "@/app/cart-button";
import ReviewSection from "@/app/review-section";

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

  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  const currentUserId = user?.id ?? undefined;

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Product</p>
          <h1>{product.name}</h1>
        </div>
        <Link href="/" style={{ fontSize: "0.875rem", color: "var(--accent)" }}>
          ← 목록으로
        </Link>
      </section>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "2rem", alignItems: "start" }}>
        <div className="imageFrame" style={{ aspectRatio: "4/3" }}>
          <img src={product.imageUrl} alt={product.name} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <div>
            <p style={{ color: "var(--muted)", marginBottom: "0.5rem" }}>{product.description}</p>
            <div className="metaRow">
              <strong style={{ fontSize: "1.5rem" }}>{product.price.toLocaleString("ko-KR")}원</strong>
              <span style={{ color: product.availableStock > 0 ? "var(--accent)" : "var(--muted)" }}>
                재고 {product.availableStock}개
              </span>
            </div>
          </div>

          {product.options.length > 0 && (
            <div>
              <p style={{ fontSize: "0.875rem", fontWeight: 600, marginBottom: "0.5rem" }}>옵션</p>
              <div style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
                {product.options.map((opt) => (
                  <div
                    key={opt.id}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      padding: "0.5rem 0.75rem",
                      background: "var(--surface)",
                      borderRadius: "6px",
                      fontSize: "0.875rem",
                    }}
                  >
                    <span>{opt.optionGroupName}: {opt.optionValue}</span>
                    {opt.additionalPrice > 0 && (
                      <span style={{ color: "var(--accent)" }}>+{opt.additionalPrice.toLocaleString("ko-KR")}원</span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          <CartButton product={{ id: product.id, name: product.name, price: product.price, options: product.options }} />
          <OrderPanel product={product} />
        </div>
      </div>

      <ReviewSection productId={product.id} currentUserId={currentUserId} />
    </main>
  );
}
