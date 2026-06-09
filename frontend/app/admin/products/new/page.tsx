"use client";

import { adminCreateProduct, type AdminProductRequest } from "@/lib/api";
import { useRouter } from "next/navigation";
import { useState } from "react";

export default function NewProductPage() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<AdminProductRequest>({
    name: "",
    description: "",
    price: 0,
    stock: 0,
    imageUrl: "",
  });

  function update(field: keyof AdminProductRequest, value: string | number) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setError(null);
    try {
      await adminCreateProduct(form);
      router.push("/admin/products");
    } catch (err) {
      setError(err instanceof Error ? err.message : "등록 실패");
    } finally {
      setPending(false);
    }
  }

  return (
    <main className="shell">
      <section className="masthead">
        <div>
          <p className="eyebrow">Admin / Products</p>
          <h1>상품 등록</h1>
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
            {pending ? "등록 중..." : "상품 등록"}
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
