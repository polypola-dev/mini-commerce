import Link from "next/link";

const FAQS = [
  { q: "주문은 어떻게 취소하나요?", a: "마이페이지 > 주문 내역에서 결제 대기 상태인 주문을 선택해 취소할 수 있어요." },
  { q: "배송은 얼마나 걸리나요?", a: "결제 완료 후 1~3일 이내 출고되며, 출고 후 1~2일 내 도착해요." },
  { q: "교환/환불은 어떻게 하나요?", a: "상품 수령일로부터 7일 이내에 고객센터로 문의해주시면 안내드려요." },
  { q: "회원정보는 어디서 수정하나요?", a: "마이페이지 > 프로필 영역을 눌러 이름과 이메일을 수정할 수 있어요." },
];

export default function CustomerServicePage() {
  return (
    <div style={{ paddingBottom: "16px" }}>
      <div style={{ padding: "14px 20px 12px", display: "flex", alignItems: "center", gap: "12px", borderBottom: "1px solid var(--color-hairline-soft)" }}>
        <Link href="/mypage" aria-label="뒤로" style={{ border: "none", background: "transparent", cursor: "pointer", padding: 0, display: "flex" }}>
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m14 5-7 7 7 7" /></svg>
        </Link>
        <span style={{ fontSize: "18px", fontWeight: 700 }}>고객센터</span>
      </div>

      <div style={{ padding: "20px" }}>
        <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>자주 묻는 질문</div>
        <div style={{ border: "1px solid var(--color-hairline)", borderRadius: "var(--radius-md)", overflow: "hidden" }}>
          {FAQS.map((item, i) => (
            <details key={item.q} style={{ borderBottom: i === FAQS.length - 1 ? "none" : "1px solid var(--color-hairline-soft)" }}>
              <summary
                style={{
                  padding: "15px 16px",
                  fontSize: "14px",
                  fontWeight: 600,
                  cursor: "pointer",
                  listStyle: "none",
                }}
              >
                {item.q}
              </summary>
              <p style={{ margin: 0, padding: "0 16px 16px", fontSize: "13px", color: "var(--color-muted)", lineHeight: 1.6 }}>
                {item.a}
              </p>
            </details>
          ))}
        </div>
      </div>

      <div className="mcDivider8" />

      <div style={{ padding: "20px" }}>
        <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>1:1 문의</div>
        <div
          style={{
            border: "1px solid var(--color-hairline)",
            borderRadius: "var(--radius-md)",
            padding: "18px",
            display: "flex",
            flexDirection: "column",
            gap: "8px",
          }}
        >
          <div style={{ fontSize: "14px", color: "var(--color-ink)" }}>고객센터 운영시간: 평일 09:00 ~ 18:00</div>
          <div style={{ fontSize: "14px", color: "var(--color-ink)" }}>전화: 1588-0000</div>
          <div style={{ fontSize: "14px", color: "var(--color-ink)" }}>이메일: help@mini-commerce.example</div>
        </div>
      </div>
    </div>
  );
}
