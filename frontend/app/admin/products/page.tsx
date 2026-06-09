"use client";

import { adminDeleteProduct, adminGetProducts, type Product } from "@/lib/api";
import Link from "next/link";
import { useEffect, useState } from "react";

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      setProducts(await adminGetProducts());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleDelete(id: string, name: string) {
    if (!confirm(`"${name}" 상품을 비활성화하시겠습니까?`)) return;
    try {
      await adminDeleteProduct(id);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "삭제 실패");
    }
  }

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Admin / Products</p>
          <h1>상품 관리</h1>
        </div>
        <Link href="/admin/products/new" className="button">+ 상품 등록</Link>
      </section>

      {loading ? (
        <p className="emptyState">불러오는 중...</p>
      ) : (
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.875rem" }}>
          <thead>
            <tr style={{ borderBottom: "2px solid var(--border)", textAlign: "left" }}>
              <th style={{ padding: "0.75rem" }}>상품명</th>
              <th style={{ padding: "0.75rem" }}>가격</th>
              <th style={{ padding: "0.75rem" }}>재고</th>
              <th style={{ padding: "0.75rem" }}>상태</th>
              <th style={{ padding: "0.75rem" }}>관리</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.id} style={{ borderBottom: "1px solid var(--border)" }}>
                <td style={{ padding: "0.75rem" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                    {product.imageUrl && (
                      <img src={product.imageUrl} alt="" style={{ width: 40, height: 40, objectFit: "cover", borderRadius: "4px" }} />
                    )}
                    <span style={{ fontWeight: 600 }}>{product.name}</span>
                  </div>
                </td>
                <td style={{ padding: "0.75rem" }}>{product.price.toLocaleString("ko-KR")}원</td>
                <td style={{ padding: "0.75rem" }}>{product.availableStock}</td>
                <td style={{ padding: "0.75rem" }}>
                  <span style={{
                    padding: "0.2rem 0.6rem",
                    borderRadius: "999px",
                    fontSize: "0.75rem",
                    background: "var(--accent)",
                    color: "#fff",
                  }}>활성</span>
                </td>
                <td style={{ padding: "0.75rem", display: "flex", gap: "0.5rem" }}>
                  <Link
                    href={`/admin/products/${product.id}/edit`}
                    style={{
                      padding: "0.25rem 0.75rem",
                      borderRadius: "6px",
                      border: "1px solid var(--border)",
                      fontSize: "0.75rem",
                      textDecoration: "none",
                      color: "inherit",
                    }}
                  >
                    수정
                  </Link>
                  <button
                    onClick={() => handleDelete(product.id, product.name)}
                    style={{
                      padding: "0.25rem 0.75rem",
                      borderRadius: "6px",
                      border: "1px solid #ef4444",
                      fontSize: "0.75rem",
                      color: "#ef4444",
                      background: "none",
                      cursor: "pointer",
                    }}
                  >
                    비활성화
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}
