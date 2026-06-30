"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PW_RE = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>@]).{8,}$/;

export default function SignupPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState<string | null>(null);
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [agreeAll, setAgreeAll] = useState(false);
  const [agreeRequired, setAgreeRequired] = useState(false);
  const [agreeMarketing, setAgreeMarketing] = useState(false);
  const [openTerms, setOpenTerms] = useState<"required" | "marketing" | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const isEmailValid = EMAIL_RE.test(email);

  const pwChecks = {
    length: password.length >= 8,
    letter: /[a-zA-Z]/.test(password),
    number: /\d/.test(password),
    special: /[!@#$%^&*(),.?":{}|<>@]/.test(password),
  };
  const isPasswordValid = PW_RE.test(password);
  const isPasswordMatch = passwordConfirm.length > 0 && password === passwordConfirm;

  const canSubmit = isEmailValid && isPasswordValid && isPasswordMatch && agreeRequired;

  function handleEmailChange(val: string) {
    setEmail(val);
    setEmailError(null);
  }

  function handleEmailBlur() {
    if (email && !isEmailValid) {
      setEmailError("올바른 이메일 형식이 아니에요.");
    } else {
      setEmailError(null);
    }
  }

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

  function toggleTermsAccordion(term: "required" | "marketing") {
    setOpenTerms((prev) => (prev === term ? null : term));
  }

  async function handleSignup(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!agreeRequired) {
      setError("필수 약관에 동의해 주세요.");
      return;
    }

    setPending(true);
    const supabase = createClient();
    const { data, error: signUpError } = await supabase.auth.signUp({
      email,
      password,
      options: {
        emailRedirectTo: `${window.location.origin}/auth/callback`,
      },
    });

    if (signUpError) {
      if (signUpError.message.includes("rate limit") || signUpError.message.includes("over_email_send_rate_limit")) {
        setError("잠시 후 다시 시도해 주세요. (이메일 발송 한도 초과)");
      } else if (signUpError.message.includes("is invalid") || signUpError.message.includes("email_address_invalid")) {
        setError("유효하지 않은 이메일 주소예요.");
      } else if (signUpError.message.includes("Password should be at least")) {
        setError("비밀번호가 너무 짧아요.");
      } else {
        setError("가입 중 오류가 발생했어요. 다시 시도해 주세요.");
      }
      setPending(false);
      return;
    }

    // Enumeration Protection ON 환경: 중복 이메일이어도 error가 null로 오지만
    // identities 배열이 비어있으면 이미 존재하는 계정
    if (data.user && data.user.identities?.length === 0) {
      setError("이미 사용 중인 이메일이에요.");
      setPending(false);
      return;
    }

    setDone(true);
    setPending(false);
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
          {/* 이메일 */}
          <div>
            <div className="mcFieldLabel">이메일</div>
            <input
              className="mcAuthInput"
              type="email"
              required
              placeholder="name@email.com"
              value={email}
              onChange={(e) => handleEmailChange(e.target.value)}
              onBlur={handleEmailBlur}
              disabled={pending}
            />
            {emailError && <div className="mcErrorText">{emailError}</div>}
            <div className="mcEmailHint">가입 후 이메일로 인증 링크가 발송됩니다</div>
          </div>

          {/* 비밀번호 */}
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
            {password.length > 0 && (
              <div className="mcPwHintList" style={{ marginTop: 8 }}>
                <div className={`mcPwHintRow ${pwChecks.length ? "ok" : "pending"}`}>
                  <span className="mcPwHintIcon">{pwChecks.length ? "✓" : "✗"}</span>
                  8자 이상
                </div>
                <div className={`mcPwHintRow ${pwChecks.letter ? "ok" : "pending"}`}>
                  <span className="mcPwHintIcon">{pwChecks.letter ? "✓" : "✗"}</span>
                  영문 포함
                </div>
                <div className={`mcPwHintRow ${pwChecks.number ? "ok" : "pending"}`}>
                  <span className="mcPwHintIcon">{pwChecks.number ? "✓" : "✗"}</span>
                  숫자 포함
                </div>
                <div className={`mcPwHintRow ${pwChecks.special ? "ok" : "pending"}`}>
                  <span className="mcPwHintIcon">{pwChecks.special ? "✓" : "✗"}</span>
                  특수문자 포함
                </div>
              </div>
            )}
          </div>

          {/* 비밀번호 확인 */}
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
            {passwordConfirm.length > 0 && (
              isPasswordMatch
                ? <div className="mcSuccessText">비밀번호가 일치해요.</div>
                : <div className="mcErrorText">비밀번호가 일치하지 않아요.</div>
            )}
          </div>

          {/* 약관 동의 */}
          <div className="mcAgreeBlock">
            <button type="button" className="mcAgreeAll" onClick={toggleAgreeAll}>
              <span className={`mcAgreeAllCheck${agreeAll ? " checked" : ""}`}>✓</span>
              <span>전체 동의</span>
            </button>

            {/* 필수 약관 */}
            <div className="mcAgreeItemRow">
              <button type="button" className="mcAgreeItem" onClick={toggleRequired}>
                <span className={`mcAgreeCheckbox${agreeRequired ? " checked" : ""}`}>{agreeRequired ? "✓" : ""}</span>
              </button>
              <button type="button" className="mcAgreeTermBtn" onClick={() => toggleTermsAccordion("required")}>
                <span>(필수) 이용약관 · 개인정보 처리방침</span>
                <span className="mcAgreeTermArrow">{openTerms === "required" ? "∨" : ">"}</span>
              </button>
            </div>
            {openTerms === "required" && (
              <div className="mcAgreeTermContent">
                본 이용약관은 minicommerce 서비스 이용에 관한 기본적인 사항을 규정합니다. 서비스 이용 시 본 약관에 동의하는 것으로 간주합니다. 개인정보는 서비스 제공 목적으로만 사용되며, 관련 법령에 따라 보호됩니다.
              </div>
            )}

            {/* 선택 약관 */}
            <div className="mcAgreeItemRow">
              <button type="button" className="mcAgreeItem" onClick={toggleMarketing}>
                <span className={`mcAgreeCheckbox${agreeMarketing ? " checked" : ""}`}>{agreeMarketing ? "✓" : ""}</span>
              </button>
              <button type="button" className="mcAgreeTermBtn" onClick={() => toggleTermsAccordion("marketing")}>
                <span>(선택) 마케팅 수신 동의</span>
                <span className="mcAgreeTermArrow">{openTerms === "marketing" ? "∨" : ">"}</span>
              </button>
            </div>
            {openTerms === "marketing" && (
              <div className="mcAgreeTermContent">
                마케팅 정보 수신에 동의하시면 이메일, 앱 푸시 등을 통해 신상품 안내, 할인 이벤트 등의 마케팅 정보를 받아보실 수 있습니다. 동의하지 않아도 서비스 이용에 제한이 없습니다.
              </div>
            )}
          </div>

          {error && <div className="mcErrorText">{error}</div>}

          <button type="submit" className="mcBtn mcBtnPrimary" disabled={!canSubmit || pending}>
            {pending ? "가입 처리 중…" : "가입하기"}
          </button>
        </form>
      </div>
    </div>
  );
}
