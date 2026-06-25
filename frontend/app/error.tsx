"use client";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="mcPage">
      <div className="mcShell">
        <div className="mcErrorWrap">
          <div className="mcErrorIcon">📡</div>
          <div className="mcErrorTitle">문제가 발생했어요</div>
          <p className="mcErrorDesc">
            {error.message || "알 수 없는 오류가 발생했습니다.\n잠시 후 다시 시도해 주세요"}
          </p>
          <button className="mcErrorRetryBtn" onClick={reset}>
            <span>↻</span>다시 시도
          </button>
        </div>
      </div>
    </div>
  );
}
