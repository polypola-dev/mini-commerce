"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

export default function ProfileEditPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const supabase = createClient();
    supabase.auth.getUser().then(({ data: { user } }) => {
      if (!user) {
        router.replace("/login");
        return;
      }
      setName(user.user_metadata?.name ?? "");
      setLoading(false);
    });
  }, [router]);

  async function handleSave() {
    setSaving(true);
    setError(null);
    setSaved(false);
    try {
      const supabase = createClient();
      const { error: updateError } = await supabase.auth.updateUser({ data: { name } });
      if (updateError) throw updateError;
      setSaved(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했어요");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <p style={{ padding: "40px 20px", color: "var(--color-muted)", fontSize: "14px" }}>불러오는 중…</p>;
  }

  return (
    <div style={{ paddingBottom: "16px" }}>
      <div style={{ padding: "14px 20px 12px", display: "flex", alignItems: "center", gap: "12px", borderBottom: "1px solid var(--color-hairline-soft)" }}>
        <button type="button" onClick={() => router.back()} aria-label="뒤로" style={{ border: "none", background: "transparent", cursor: "pointer", padding: 0, display: "flex" }}>
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m14 5-7 7 7 7" /></svg>
        </button>
        <span style={{ fontSize: "18px", fontWeight: 700 }}>회원정보 수정</span>
      </div>

      <div style={{ padding: "20px" }}>
        <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--color-muted)", marginBottom: "8px" }}>이름</div>
        <input
          className="mcCheckoutInput"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="이름을 입력하세요"
        />
      </div>

      {error && <p style={{ padding: "0 20px", color: "var(--color-error, #c13515)", fontSize: "13px" }}>{error}</p>}
      {saved && <p style={{ padding: "0 20px", color: "var(--color-primary)", fontSize: "13px" }}>저장되었습니다.</p>}

      <div className="mcActionBar" style={{ padding: "12px 16px 16px" }}>
        <button type="button" className="mcBtn mcBtnPrimary" disabled={saving} onClick={handleSave}>
          {saving ? "저장 중…" : "저장하기"}
        </button>
      </div>
    </div>
  );
}
