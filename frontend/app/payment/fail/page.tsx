"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";

// Toss failUrl 대표 에러코드 → 사용자 친화적 메시지. 미매핑 코드는 Toss가 준 message를 그대로 노출.
const CODE_MESSAGE: Record<string, string> = {
  PAY_PROCESS_CANCELED: "결제를 취소했어요",
  PAY_PROCESS_ABORTED: "결제가 중단됐어요",
  REJECT_CARD_COMPANY: "카드사에서 결제를 거절했어요",
};

function PaymentFailInner() {
  const search = useSearchParams();
  const code = search.get("code");
  const message = (code && CODE_MESSAGE[code]) || search.get("message") || "결제에 실패했어요";

  return (
    <div style={{ padding: "48px 20px", textAlign: "center" }}>
      <div style={{ fontSize: "20px", fontWeight: 700, marginBottom: "10px" }}>결제 실패</div>
      <p style={{ fontSize: "14px", color: "var(--color-muted)", marginBottom: "24px" }}>{message}</p>
      <Link href="/cart" className="mcBtn mcBtnPrimary" style={{ display: "inline-block" }}>장바구니로 돌아가기</Link>
    </div>
  );
}

export default function PaymentFailPage() {
  return (
    <Suspense fallback={<div style={{ padding: "48px 20px", textAlign: "center", color: "var(--color-muted)", fontSize: "14px" }}>불러오는 중…</div>}>
      <PaymentFailInner />
    </Suspense>
  );
}
