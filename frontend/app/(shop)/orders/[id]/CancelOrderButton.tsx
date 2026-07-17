"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { cancelOrder } from "@/lib/api";

export default function CancelOrderButton({ orderId }: { orderId: string }) {
  const router = useRouter();
  const [confirming, setConfirming] = useState(false);
  const [reason, setReason] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submitCancel() {
    setPending(true);
    setError(null);
    try {
      await cancelOrder(orderId, reason.trim() || undefined);
      router.refresh();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "주문 취소에 실패했어요";
      setError(/취소.*상태|CANCEL_NOT_ALLOWED|409/.test(msg) ? "취소할 수 없는 주문 상태예요" : msg);
      setPending(false);
    }
  }

  if (!confirming) {
    return (
      <div style={{ padding: "0 20px 20px" }}>
        <button
          type="button"
          className="mcBtn mcBtnSecondary"
          onClick={() => setConfirming(true)}
          style={{ width: "100%" }}
        >
          주문 취소
        </button>
      </div>
    );
  }

  return (
    <div style={{ padding: "0 20px 20px" }}>
      <div style={{ border: "1px solid var(--color-hairline)", borderRadius: "14px", padding: "16px" }}>
        <div style={{ fontSize: "15px", fontWeight: 700, marginBottom: "6px" }}>주문을 취소할까요?</div>
        <div style={{ fontSize: "13px", color: "var(--color-muted)", marginBottom: "12px", lineHeight: 1.5 }}>
          결제 금액은 환불되며, 취소 후에는 되돌릴 수 없어요.
        </div>
        <input
          className="mcCheckoutInput"
          placeholder="취소 사유 (선택)"
          value={reason}
          disabled={pending}
          onChange={(e) => setReason(e.target.value)}
          style={{ width: "100%", marginBottom: "12px" }}
        />
        {error && <p style={{ color: "var(--color-error)", fontSize: "13px", marginBottom: "10px" }}>{error}</p>}
        <div style={{ display: "flex", gap: "8px" }}>
          <button
            type="button"
            className="mcBtn mcBtnSecondary"
            disabled={pending}
            onClick={() => { setConfirming(false); setError(null); }}
            style={{ flex: 1 }}
          >
            닫기
          </button>
          <button
            type="button"
            className="mcBtn mcBtnPrimary"
            disabled={pending}
            onClick={submitCancel}
            style={{ flex: 1 }}
          >
            {pending ? "취소 처리 중…" : "취소 확정"}
          </button>
        </div>
      </div>
    </div>
  );
}
