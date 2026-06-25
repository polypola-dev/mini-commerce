"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

export default function ResetPasswordPage() {
  const router = useRouter();
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const hasMinLength = password.length >= 8;
  const hasLetterAndDigit = /[a-zA-Z]/.test(password) && /\d/.test(password);

  async function handleReset(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (password !== passwordConfirm) {
      setError("비밀번호가 일치하지 않아요.");
      return;
    }

    setPending(true);
    const supabase = createClient();
    const { error } = await supabase.auth.updateUser({ password });

    if (error) {
      setError(error.message);
      setPending(false);
    } else {
      router.push("/login");
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
          <span className="mcAuthHeaderTitle">새 비밀번호 설정</span>
        </div>

        <form className="mcAuthBody" onSubmit={handleReset}>
          <div style={{ fontSize: "15px", color: "var(--color-body)", lineHeight: 1.6 }}>
            안전한 새 비밀번호를 입력해 주세요.
          </div>

          <div>
            <div className="mcFieldLabel">새 비밀번호</div>
            <input
              className="mcAuthInput mcAuthInputStrong"
              type="password"
              required
              placeholder="새 비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={pending}
            />
          </div>

          <div>
            <div className="mcFieldLabel">새 비밀번호 확인</div>
            <input
              className="mcAuthInput"
              type="password"
              required
              placeholder="다시 입력"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              disabled={pending}
            />
          </div>

          <div className="mcPwHintList">
            <div className={`mcPwHintRow ${hasMinLength ? "ok" : "pending"}`}>
              <span className="mcPwHintIcon">{hasMinLength ? "✓" : "○"}</span>
              <span>8자 이상</span>
            </div>
            <div className={`mcPwHintRow ${hasLetterAndDigit ? "ok" : "pending"}`}>
              <span className="mcPwHintIcon">{hasLetterAndDigit ? "✓" : "○"}</span>
              <span>영문·숫자 조합</span>
            </div>
          </div>

          {error && <div className="mcErrorText">{error}</div>}

          <button type="submit" className="mcBtn mcBtnPrimary" disabled={pending}>
            {pending ? "변경 중…" : "비밀번호 변경"}
          </button>
        </form>
      </div>
    </div>
  );
}
