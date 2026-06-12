import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { redirect } from "next/navigation";
import { AdminSidebar } from "./AdminSidebar";
import styles from "./admin.module.css";

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
    <div className={styles.adminWrap}>
      <AdminSidebar />
      <div className={styles.adminMain}>{children}</div>
    </div>
  );
}
