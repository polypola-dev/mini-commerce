import { adminGetOrders, adminGetProducts } from "@/lib/api";
import Link from "next/link";

export default async function AdminDashboardPage() {
  const [productsPage, ordersPage] = await Promise.all([
    adminGetProducts({ page: 0, size: 5 }).catch(() => ({ content: [], totalElements: 0, totalPages: 1, page: 0, size: 5 })),
    adminGetOrders({ page: 0, size: 5 }).catch(() => ({ content: [], totalElements: 0, totalPages: 1, page: 0, size: 5 })),
  ]);

  const products = productsPage.content;
  const orders = ordersPage.content;

  const activeProducts = productsPage.totalElements;
  const pendingOrders = orders.filter((o) => o.status === "PENDING_PAYMENT").length;
  const paidOrders = orders.filter((o) => o.status === "PAID").length;

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>대시보드</h1>
        </div>
      </section>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "1rem", marginBottom: "2rem" }}>
        {[
          { label: "전체 상품", value: activeProducts, href: "/admin/products" },
          { label: "결제 대기 주문", value: pendingOrders, href: "/admin/orders" },
          { label: "결제 완료 주문", value: paidOrders, href: "/admin/orders" },
        ].map((stat) => (
          <Link key={stat.label} href={stat.href} style={{ textDecoration: "none", color: "inherit" }}>
            <div
              className="productCard"
              style={{ padding: "1.5rem", textAlign: "center", cursor: "pointer" }}
            >
              <p style={{ fontSize: "2.5rem", fontWeight: 700, color: "var(--accent)" }}>{stat.value}</p>
              <p style={{ color: "var(--muted)", fontSize: "0.875rem" }}>{stat.label}</p>
            </div>
          </Link>
        ))}
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1.5rem" }}>
        <section className="productCard" style={{ padding: "1.25rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
            <h2>최근 주문</h2>
            <Link href="/admin/orders" style={{ fontSize: "0.875rem", color: "var(--accent)" }}>전체 보기</Link>
          </div>
          {orders.slice(0, 5).map((order) => (
            <div key={order.orderId} style={{ padding: "0.5rem 0", borderBottom: "1px solid var(--border)", fontSize: "0.875rem" }}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>{order.orderId.slice(0, 8)}…</span>
                <span style={{ color: "var(--accent)", fontWeight: 600 }}>{order.status}</span>
              </div>
              <span style={{ color: "var(--muted)" }}>{order.totalAmount.toLocaleString("ko-KR")}원</span>
            </div>
          ))}
        </section>

        <section className="productCard" style={{ padding: "1.25rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
            <h2>상품 목록</h2>
            <Link href="/admin/products/new" style={{ fontSize: "0.875rem", color: "var(--accent)" }}>+ 상품 등록</Link>
          </div>
          {products.slice(0, 5).map((product) => (
            <div key={product.id} style={{ padding: "0.5rem 0", borderBottom: "1px solid var(--border)", fontSize: "0.875rem" }}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>{product.name}</span>
                <span style={{ color: "var(--muted)" }}>재고 {product.availableStock}</span>
              </div>
              <span style={{ color: "var(--accent)", fontWeight: 600 }}>{product.price.toLocaleString("ko-KR")}원</span>
            </div>
          ))}
        </section>
      </div>
    </main>
  );
}
