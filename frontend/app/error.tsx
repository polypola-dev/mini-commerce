"use client";

import { useRouter } from "next/navigation";

const NETWORK_ERROR_PATTERN = /fetch|network/i;

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const router = useRouter();
  const isNetworkError = NETWORK_ERROR_PATTERN.test(error.message || "");

  if (isNetworkError) {
    return (
      <div className="mcPage">
        <div className="mcShell">
          <div className="mcErrorWrap">
            <div className="mcErrorIcon">📡</div>
            <div className="mcErrorTitle">연결이 불안정해요</div>
            <p className="mcErrorDesc">
              인터넷 연결을 확인한 뒤
              <br />
              다시 시도해 주세요
            </p>
            <button className="mcErrorRetryBtn" onClick={reset}>
              <span>↻</span>다시 시도
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mcPage">
      <div className="mcShell">
        <div className="mcErrorWrap">
          <div className="mcServerErrorCode">500</div>
          <div className="mcErrorTitle">일시적인 오류가 발생했어요</div>
          <p className="mcErrorDesc">
            서버에 문제가 생겼어요.
            <br />
            잠시 후 다시 시도해 주세요
          </p>
          <div className="mcServerErrorBadge">오류코드 500 · 잠시 후 자동 복구</div>
          <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: "10px", marginTop: "8px" }}>
            <button className="mcBtn mcBtnPrimary" onClick={reset}>
              다시 시도
            </button>
            <button className="mcBtn mcBtnSecondary" onClick={() => router.push("/")}>
              홈으로 가기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
