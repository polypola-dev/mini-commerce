"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

export default function SignupPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [phone, setPhone] = useState("");
  const [agreeAll, setAgreeAll] = useState(false);
  const [agreeRequired, setAgreeRequired] = useState(false);
  const [agreeMarketing, setAgreeMarketing] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  function toggleAgreeAll() {
    const next = !(agreeAll && agreeRequired && agreeMarketing);
    setAgreeAll(next);
    setAgreeRequired(next);
    setAgreeMarketing(next);
  }

  function toggleRequired() {
    const next = !agreeRequired;
    setAgreeRequired(next);
    setAgreeAll(next && agreeMarketing);
  }

  function toggleMarketing() {
    const next = !agreeMarketing;
    setAgreeMarketing(next);
    setAgreeAll(agreeRequired && next);
  }

  async function handleSignup(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (password !== passwordConfirm) {
      setError("비밀번호가 일치하지 않아요.");
      return;
    }
    if (!agreeRequired) {
      setError("필수 약관에 동의해 주세요.");
      return;
    }

    setPending(true);
    const supabase = createClient();
    const { error } = await supabase.auth.signUp({ email, password });

    if (error) {
      setError(error.message);
      setPending(false);
    } else {
      setDone(true);
      setPending(false);
    }
  }

  if (done) {
    return (
      <div className="mcPage">
        <div className="mcShell" style={{ minHeight: "100vh" }}>
          <div className="mcAuthHeaderBar">
            <button type="button" className="mcAuthBackBtn" onClick={() => router.push("/login")} aria-label="뒤로">
              <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="m14 5-7 7 7 7" />
              </svg>
            </button>
            <span className="mcAuthHeaderTitle">회원가입</span>
          </div>
          <div className="mcAuthSuccessBox">
            <p style={{ fontSize: "15px", color: "var(--color-body)", lineHeight: 1.6 }}>
              가입 확인 메일을 보냈어요. 메일함에서 인증을 완료한 뒤 로그인해 주세요.
            </p>
            <button type="button" className="mcBtn mcBtnPrimary" onClick={() => router.push("/login")}>
              로그인하러 가기
            </button>
          </div>
        </div>
      </div>
    );
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
          <span className="mcAuthHeaderTitle">회원가입</span>
        </div>

        <form className="mcAuthBody" onSubmit={handleSignup}>
          <div>
            <div className="mcFieldLabel">이메일</div>
            <div className="mcInputRow">
              <input
                className="mcAuthInput"
                type="email"
                required
                placeholder="name@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={pending}
              />
              <button type="button" className="mcInlineBtn" disabled>
                중복확인
              </button>
            </div>
          </div>

          <div>
            <div className="mcFieldLabel">비밀번호</div>
            <input
              className="mcAuthInput"
              type="password"
              required
              placeholder="8자 이상, 영문+숫자+특수문자"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={pending}
            />
          </div>

          <div>
            <div className="mcFieldLabel">비밀번호 확인</div>
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

          <div>
            <div className="mcFieldLabel">휴대폰 인증</div>
            <div className="mcInputRow">
              <input
                className="mcAuthInput"
                placeholder="010-0000-0000"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                disabled={pending}
              />
              <button type="button" className="mcInlineBtn" disabled>
                인증요청
              </button>
            </div>
          </div>

          <div className="mcAgreeBlock">
            <button type="button" className="mcAgreeAll" onClick={toggleAgreeAll}>
              <span className={`mcAgreeAllCheck${agreeAll ? " checked" : ""}`}>✓</span>
              <span>전체 동의</span>
            </button>
            <button type="button" className="mcAgreeItem" onClick={toggleRequired}>
              <span className={`mcAgreeCheckbox${agreeRequired ? " checked" : ""}`}>{agreeRequired ? "✓" : ""}</span>
              <span>(필수) 이용약관 · 개인정보 처리방침</span>
            </button>
            <button type="button" className="mcAgreeItem" onClick={toggleMarketing}>
              <span className={`mcAgreeCheckbox${agreeMarketing ? " checked" : ""}`}>{agreeMarketing ? "✓" : ""}</span>
              <span>(선택) 마케팅 수신 동의</span>
            </button>
          </div>

          {error && <div className="mcErrorText">{error}</div>}

          <button type="submit" className="mcBtn mcBtnPrimary" disabled={pending}>
            {pending ? "가입 처리 중…" : "가입하기"}
          </button>
        </form>
      </div>
    </div>
  );
}
