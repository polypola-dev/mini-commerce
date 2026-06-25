"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

export default function EmailChangePage() {
  const router = useRouter();
  const [currentEmail, setCurrentEmail] = useState("");
  const [newEmail, setNewEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [requested, setRequested] = useState(false);

  useEffect(() => {
    const supabase = createClient();
    supabase.auth.getUser().then(({ data: { user } }) => {
      if (!user) {
        router.replace("/login");
        return;
      }
      setCurrentEmail(user.email ?? "");
      setLoading(false);
    });
  }, [router]);

  async function handleSave() {
    if (!newEmail) return;
    setSaving(true);
    setError(null);
    setRequested(false);
    try {
      const supabase = createClient();
      const { error: updateError } = await supabase.auth.updateUser({ email: newEmail });
      if (updateError) throw updateError;
      setRequested(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "이메일 변경 요청에 실패했어요");
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
        <span style={{ fontSize: "18px", fontWeight: 700 }}>이메일 변경</span>
      </div>

      <div style={{ padding: "20px", display: "flex", flexDirection: "column", gap: "16px" }}>
        <div>
          <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--color-muted)", marginBottom: "8px" }}>현재 이메일</div>
          <input className="mcCheckoutInput" value={currentEmail} disabled style={{ color: "var(--color-muted)" }} />
        </div>
        <div>
          <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--color-muted)", marginBottom: "8px" }}>새 이메일</div>
          <input
            className="mcCheckoutInput"
            type="email"
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            placeholder="새 이메일을 입력하세요"
          />
        </div>
      </div>

      {error && <p style={{ padding: "0 20px", color: "var(--color-error, #c13515)", fontSize: "13px" }}>{error}</p>}
      {requested && (
        <p style={{ padding: "0 20px", color: "var(--color-primary)", fontSize: "13px", lineHeight: 1.5 }}>
          확인 메일을 보냈어요. 새 이메일의 받은 메일함에서 인증 링크를 눌러 변경을 완료해주세요.
        </p>
      )}

      <div className="mcActionBar" style={{ padding: "12px 16px 16px" }}>
        <button type="button" className="mcBtn mcBtnPrimary" disabled={saving || !newEmail} onClick={handleSave}>
          {saving ? "요청 중…" : "이메일 변경 요청"}
        </button>
      </div>
    </div>
  );
}
