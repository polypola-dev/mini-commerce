"use client";

import { adminGetProducts, adminUpdateProduct, type AdminProductRequest } from "@/lib/api";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function EditProductPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<AdminProductRequest | null>(null);

  useEffect(() => {
    adminGetProducts().then((products) => {
      const found = products.find((p) => p.id === id);
      if (found) {
        setForm({ name: found.name, description: found.description, price: found.price, stock: found.availableStock, imageUrl: found.imageUrl });
      }
    });
  }, [id]);

  function update(field: keyof AdminProductRequest, value: string | number) {
    setForm((prev) => prev ? { ...prev, [field]: value } : prev);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form) return;
    setPending(true);
    setError(null);
    try {
      await adminUpdateProduct(id, form);
      router.push("/admin/products");
    } catch (err) {
      setError(err instanceof Error ? err.message : "수정 실패");
    } finally {
      setPending(false);
    }
  }

  if (!form) return <main className="shell"><p className="emptyState">불러오는 중...</p></main>;

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Admin / Products</p>
          <h1>상품 수정</h1>
        </div>
      </section>

      <form onSubmit={handleSubmit} style={{ maxWidth: "520px", display: "flex", flexDirection: "column", gap: "1rem" }}>
        {[
          { label: "상품명", field: "name" as const, type: "text" },
          { label: "설명", field: "description" as const, type: "text" },
          { label: "가격 (원)", field: "price" as const, type: "number" },
          { label: "재고", field: "stock" as const, type: "number" },
          { label: "이미지 URL", field: "imageUrl" as const, type: "text" },
        ].map(({ label, field, type }) => (
          <label key={field} style={{ display: "flex", flexDirection: "column", gap: "0.25rem", fontSize: "0.875rem" }}>
            {label}
            <input
              type={type}
              value={form[field]}
              onChange={(e) => update(field, type === "number" ? Number(e.target.value) : e.target.value)}
              required
              style={{ padding: "0.5rem 0.75rem", borderRadius: "6px", border: "1px solid var(--border)", fontSize: "0.875rem" }}
            />
          </label>
        ))}

        {error && <p style={{ color: "#ef4444", fontSize: "0.875rem" }}>{error}</p>}

        <div style={{ display: "flex", gap: "0.75rem", marginTop: "0.5rem" }}>
          <button type="submit" disabled={pending} className="button">
            {pending ? "저장 중..." : "저장"}
          </button>
          <button type="button" onClick={() => router.back()}
            style={{ padding: "0.5rem 1rem", borderRadius: "6px", border: "1px solid var(--border)", background: "none", cursor: "pointer" }}>
            취소
          </button>
        </div>
      </form>
    </main>
  );
}
