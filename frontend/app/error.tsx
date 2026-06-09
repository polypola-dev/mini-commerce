"use client";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="shell" style={{ textAlign: "center", paddingTop: "4rem" }}>
      <p className="eyebrow">오류 발생</p>
      <h1>문제가 발생했습니다</h1>
      <p style={{ color: "var(--muted)", marginTop: "1rem", marginBottom: "2rem" }}>
        {error.message || "알 수 없는 오류가 발생했습니다."}
      </p>
      <button className="button" onClick={reset}>
        다시 시도
      </button>
    </main>
  );
}
