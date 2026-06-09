import Link from "next/link";

export default function NotFound() {
  return (
    <main className="shell" style={{ textAlign: "center", paddingTop: "4rem" }}>
      <p className="eyebrow">404</p>
      <h1>페이지를 찾을 수 없습니다</h1>
      <p style={{ color: "var(--muted)", marginTop: "1rem", marginBottom: "2rem" }}>
        요청하신 페이지가 존재하지 않거나 이동되었습니다.
      </p>
      <Link href="/" className="button">
        홈으로 돌아가기
      </Link>
    </main>
  );
}
