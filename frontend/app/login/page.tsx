"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { createClient } from "@/lib/supabase/client";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [needsConfirm, setNeedsConfirm] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendDone, setResendDone] = useState(false);

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setError(null);
    setNeedsConfirm(false);
    setResendDone(false);

    const supabase = createClient();
    const { error } = await supabase.auth.signInWithPassword({ email, password });

    if (error) {
      if (error.message.includes("Email not confirmed")) {
        setNeedsConfirm(true);
        setError("이메일 인증이 필요해요. 가입 시 발송된 메일을 확인해 주세요.");
      } else if (error.message.includes("Invalid login credentials")) {
        setError("이메일 또는 비밀번호가 올바르지 않아요.");
      } else {
        setError("로그인 중 오류가 발생했어요. 다시 시도해 주세요.");
      }
      setPending(false);
    } else {
      router.push("/");
      router.refresh();
    }
  }

  async function handleResend() {
    setResending(true);
    setResendDone(false);
    const supabase = createClient();
    await supabase.auth.resend({
      type: "signup",
      email,
      options: { emailRedirectTo: `${window.location.origin}/auth/callback` },
    });
    setResending(false);
    setResendDone(true);
  }

  async function handleGoogleLogin() {
    setPending(true);
    setError(null);
    const supabase = createClient();
    const { error } = await supabase.auth.signInWithOAuth({
      provider: "google",
      options: {
        redirectTo: `${window.location.origin}/auth/callback`,
      },
    });
    if (error) {
      setError(error.message);
      setPending(false);
    }
  }

  async function handleNaverLogin() {
    setPending(true);
    setError(null);
    const supabase = createClient();
    const { error } = await supabase.auth.signInWithOAuth({
      provider: "custom:naver",
      options: {
        redirectTo: `${window.location.origin}/auth/callback`,
      },
    });
    if (error) {
      setError(error.message);
      setPending(false);
    }
  }

  return (
    <div className="mcPage">
      <div className="mcShell" style={{ minHeight: "100vh" }}>
        <div className="mcLoginWrap">
          <div className="mcLoginSpacer" />
          <div className="mcLoginLogo">
            <span>mini</span>
            <span>commerce</span>
          </div>
          <div className="mcLoginTitle">로그인</div>

          <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            <input
              className="mcAuthInput"
              type="email"
              required
              placeholder="이메일 또는 휴대폰 번호"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={pending}
            />
            <input
              className="mcAuthInput"
              type="password"
              required
              placeholder="비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={pending}
            />

            {error && <div className="mcErrorText">{error}</div>}
            {needsConfirm && (
              <div style={{ textAlign: "center" }}>
                {resendDone ? (
                  <span className="mcSuccessText">인증 메일을 재발송했어요. 메일함을 확인해 주세요.</span>
                ) : (
                  <button
                    type="button"
                    className="mcTextBtn"
                    onClick={handleResend}
                    disabled={resending}
                  >
                    {resending ? "발송 중…" : "인증 메일 재발송"}
                  </button>
                )}
              </div>
            )}

            <div style={{ marginTop: "4px" }}>
              <button type="submit" className="mcBtn mcBtnPrimary" disabled={pending}>
                {pending ? "로그인 중…" : "로그인"}
              </button>
            </div>
          </form>

          <div className="mcAuthLinks">
            <Link href="/signup">회원가입</Link>
            <span>·</span>
            <Link href="/forgot-password">비밀번호 찾기</Link>
          </div>

          <div className="mcAuthDivider">
            <div className="mcAuthDividerLine" />
            <span>간편 로그인</span>
            <div className="mcAuthDividerLine" />
          </div>

          <div className="mcSocialRow">
            <button type="button" className="mcSocialCircle mcSocialCircleKakao" disabled aria-label="카카오로 로그인">
              K
            </button>
            <button
              type="button"
              className="mcSocialCircle mcSocialCircleNaver"
              onClick={handleNaverLogin}
              disabled={pending}
              aria-label="네이버로 로그인"
            >
              N
            </button>
            <button
              type="button"
              className="mcSocialCircle mcSocialCircleGoogle"
              onClick={handleGoogleLogin}
              disabled={pending}
              aria-label="Google로 로그인"
            >
              G
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
