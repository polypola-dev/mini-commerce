"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { confirmPayment } from "@/lib/api";

function PaymentSuccessInner() {
  const router = useRouter();
  const search = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    const paymentKey = search.get("paymentKey");
    const orderId = search.get("orderId");
    const amount = search.get("amount");

    if (!paymentKey || !orderId || !amount) {
      setError("결제 정보가 올바르지 않아요");
      return;
    }

    confirmPayment(orderId, paymentKey, Number(amount))
      .then(() => router.replace(`/orders/${orderId}?completed=1`))
      .catch((e) => setError(e instanceof Error ? e.message : "결제 승인에 실패했어요"));
  }, [search, router]);

  if (error) {
    return (
      <div style={{ padding: "48px 20px", textAlign: "center" }}>
        <div style={{ fontSize: "20px", fontWeight: 700, marginBottom: "10px" }}>결제 승인 실패</div>
        <p style={{ fontSize: "14px", color: "var(--color-muted)", marginBottom: "24px" }}>{error}</p>
        <Link href="/cart" className="mcBtn mcBtnPrimary" style={{ display: "inline-block" }}>장바구니로</Link>
      </div>
    );
  }

  return (
    <div style={{ padding: "48px 20px", textAlign: "center", color: "var(--color-muted)", fontSize: "14px" }}>
      결제를 승인하는 중…
    </div>
  );
}

export default function PaymentSuccessPage() {
  return (
    <Suspense fallback={<div style={{ padding: "48px 20px", textAlign: "center", color: "var(--color-muted)", fontSize: "14px" }}>불러오는 중…</div>}>
      <PaymentSuccessInner />
    </Suspense>
  );
}
