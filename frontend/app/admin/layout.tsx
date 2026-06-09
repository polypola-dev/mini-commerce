import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { redirect } from "next/navigation";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();

  if (!user) redirect("/login");

  const role = (user.app_metadata as Record<string, string>)?.role;
  if (role !== "admin") {
    return (
      <main className="shell" style={{ textAlign: "center", paddingTop: "4rem" }}>
        <p className="eyebrow">403</p>
        <h1>접근 권한이 없습니다</h1>
        <p style={{ color: "var(--muted)", marginTop: "1rem", marginBottom: "2rem" }}>
          관리자 계정으로 로그인해 주세요.
        </p>
        <Link href="/" className="button">홈으로</Link>
      </main>
    );
  }

  return (
    <div style={{ display: "grid", gridTemplateColumns: "200px 1fr", minHeight: "100vh" }}>
      <nav
        style={{
          background: "var(--surface)",
          borderRight: "1px solid var(--border)",
          padding: "1.5rem 1rem",
          display: "flex",
          flexDirection: "column",
          gap: "0.5rem",
        }}
      >
        <p style={{ fontWeight: 700, fontSize: "1rem", marginBottom: "1rem" }}>관리자</p>
        <Link href="/admin" style={{ padding: "0.5rem 0.75rem", borderRadius: "6px", fontSize: "0.875rem" }}>
          대시보드
        </Link>
        <Link href="/admin/products" style={{ padding: "0.5rem 0.75rem", borderRadius: "6px", fontSize: "0.875rem" }}>
          상품 관리
        </Link>
        <Link href="/admin/orders" style={{ padding: "0.5rem 0.75rem", borderRadius: "6px", fontSize: "0.875rem" }}>
          주문 관리
        </Link>
        <hr style={{ borderColor: "var(--border)", margin: "0.5rem 0" }} />
        <Link href="/" style={{ padding: "0.5rem 0.75rem", borderRadius: "6px", fontSize: "0.875rem", color: "var(--muted)" }}>
          ← 사이트로
        </Link>
      </nav>
      <div>{children}</div>
    </div>
  );
}
