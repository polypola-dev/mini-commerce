"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  async function handleSendReset(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setError(null);

    const supabase = createClient();
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/reset-password`,
    });

    if (error) {
      setError(error.message);
      setPending(false);
    } else {
      setSent(true);
      setPending(false);
    }
  }

  return (
    <div className="mcPage">
      <div className="mcShell" style={{ minHeight: "100vh" }}>
        <div className="mcAuthHeaderBar">
          <button type="button" className="mcAuthBackBtn" onClick={() => router.back()} aria-label="뒤로">
            <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="m14 5-7 7 7 7" />
            </svg>
          </button>
          <span className="mcAuthHeaderTitle">비밀번호 찾기</span>
        </div>

        {sent ? (
          <div className="mcAuthSuccessBox">
            <p style={{ fontSize: "15px", color: "var(--color-body)", lineHeight: 1.6 }}>
              {email} 주소로 재설정 링크를 보냈어요. 메일함을 확인해 주세요.
            </p>
            <button type="button" className="mcBtn mcBtnPrimary" onClick={() => router.push("/login")}>
              로그인으로 돌아가기
            </button>
          </div>
        ) : (
          <form className="mcAuthBody" onSubmit={handleSendReset}>
            <div style={{ fontSize: "15px", color: "var(--color-body)", lineHeight: 1.6 }}>
              가입하신 이메일로 재설정 링크를 보내드릴게요.
            </div>
            <div>
              <div className="mcFieldLabel">이메일</div>
              <input
                className="mcAuthInput mcAuthInputStrong"
                type="email"
                required
                placeholder="name@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={pending}
              />
            </div>

            <div className="mcInfoNote">
              <span className="mcInfoNoteIcon">i</span>
              <span>메일이 오지 않으면 스팸함을 확인해 주세요.</span>
            </div>

            {error && <div className="mcErrorText">{error}</div>}

            <button type="submit" className="mcBtn mcBtnPrimary" disabled={pending}>
              {pending ? "전송 중…" : "재설정 메일 보내기"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
