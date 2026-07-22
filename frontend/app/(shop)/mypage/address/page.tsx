"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAddresses } from "@/lib/addresses";

export default function AddressManagePage() {
  const router = useRouter();
  const { addresses, add, remove, setDefault } = useAddresses();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: "", phone: "", address1: "", address2: "" });
  const [formError, setFormError] = useState<string | null>(null);

  // 최소 입력값 검증 — 빈값뿐 아니라 형식/길이까지 확인한다.
  function validate(f: typeof form): string | null {
    if (f.name.trim().length < 2) return "받는 분 이름을 2자 이상 입력해주세요.";
    const digits = f.phone.replace(/[^0-9]/g, "");
    if (digits.length < 9 || digits.length > 11) return "연락처를 정확히 입력해주세요. (숫자 9~11자리)";
    if (f.address1.trim().length < 5) return "주소를 정확히 입력해주세요.";
    return null;
  }

  function handleAdd() {
    const error = validate(form);
    if (error) {
      setFormError(error);
      return;
    }
    setFormError(null);
    add({
      name: form.name.trim(),
      phone: form.phone.trim(),
      address1: form.address1.trim(),
      address2: form.address2.trim(),
    });
    setForm({ name: "", phone: "", address1: "", address2: "" });
    setShowForm(false);
  }

  return (
    <div style={{ paddingBottom: "16px" }}>
      <div style={{ padding: "14px 20px 12px", display: "flex", alignItems: "center", gap: "12px", borderBottom: "1px solid var(--color-hairline-soft)" }}>
        <button type="button" onClick={() => router.back()} aria-label="뒤로" style={{ border: "none", background: "transparent", cursor: "pointer", padding: 0, display: "flex" }}>
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m14 5-7 7 7 7" /></svg>
        </button>
        <span style={{ fontSize: "18px", fontWeight: 700 }}>배송지 관리</span>
      </div>

      {addresses.length === 0 && !showForm && (
        <div className="mcEmptyState">
          <div className="mcEmptyIcon">📍</div>
          <div className="mcEmptyTitle">등록된 배송지가 없어요</div>
          <div className="mcEmptyDesc">배송지를 추가하면 주문 시 바로 사용할 수 있어요.</div>
          <button type="button" className="mcEmptyCta" style={{ border: "none" }} onClick={() => setShowForm(true)}>
            배송지 추가
          </button>
        </div>
      )}

      {addresses.length > 0 && (
        <div style={{ padding: "16px 20px 0", display: "flex", flexDirection: "column", gap: "12px" }}>
          {addresses.map((a) => (
            <div
              key={a.id}
              style={{
                border: "1px solid var(--color-hairline)",
                borderRadius: "var(--radius-md)",
                padding: "16px 18px",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px" }}>
                <span style={{ fontSize: "14px", fontWeight: 700 }}>{a.name}</span>
                {a.isDefault && (
                  <span
                    style={{
                      background: "var(--color-surface-strong)",
                      color: "var(--color-primary)",
                      fontSize: "11px",
                      fontWeight: 800,
                      borderRadius: "999px",
                      padding: "2px 9px",
                    }}
                  >
                    기본배송지
                  </span>
                )}
              </div>
              <div style={{ fontSize: "13px", color: "var(--color-body)", marginBottom: "2px" }}>
                {a.address1} {a.address2}
              </div>
              <div style={{ fontSize: "12.5px", color: "var(--color-muted)" }}>{a.phone}</div>
              <div style={{ display: "flex", gap: "6px", marginTop: "12px" }}>
                {!a.isDefault && (
                  <button
                    type="button"
                    style={{
                      width: "auto",
                      border: "1px solid var(--color-hairline)",
                      background: "#fff",
                      color: "var(--color-ink)",
                      borderRadius: "8px",
                      padding: "6px 11px",
                      fontSize: "12px",
                      fontWeight: 600,
                      cursor: "pointer",
                    }}
                    onClick={() => setDefault(a.id)}
                  >
                    기본설정
                  </button>
                )}
                <button
                  type="button"
                  style={{
                    width: "auto",
                    border: "1px solid var(--color-hairline)",
                    background: "#fff",
                    color: "var(--color-ink)",
                    borderRadius: "8px",
                    padding: "6px 11px",
                    fontSize: "12px",
                    fontWeight: 600,
                    cursor: "pointer",
                  }}
                  onClick={() => remove(a.id)}
                >
                  삭제
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {addresses.length > 0 && !showForm && (
        <div style={{ padding: "16px 20px 0" }}>
          <button
            type="button"
            style={{
              width: "100%",
              border: "1px dashed var(--color-border-strong)",
              background: "#fff",
              borderRadius: "var(--radius-sm)",
              padding: "13px",
              fontSize: "13.5px",
              fontWeight: 700,
              color: "var(--color-muted)",
              cursor: "pointer",
            }}
            onClick={() => setShowForm(true)}
          >
            ＋ 배송지 추가
          </button>
        </div>
      )}

      {showForm && (
        <div style={{ padding: "20px" }}>
          <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px" }}>배송지 추가</div>
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            <input className="mcCheckoutInput" placeholder="받는 분" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
            <input className="mcCheckoutInput" placeholder="연락처" value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
            <input className="mcCheckoutInput" placeholder="주소" value={form.address1} onChange={(e) => setForm((f) => ({ ...f, address1: e.target.value }))} />
            <input className="mcCheckoutInput" placeholder="상세 주소" value={form.address2} onChange={(e) => setForm((f) => ({ ...f, address2: e.target.value }))} />
          </div>
          {formError && (
            <p style={{ color: "var(--color-error)", fontSize: "12.5px", marginTop: "10px" }}>{formError}</p>
          )}
          <div style={{ display: "flex", gap: "8px", marginTop: "16px" }}>
            <button type="button" className="mcBtn mcBtnSecondary" style={{ flex: 1, width: "auto" }} onClick={() => { setShowForm(false); setFormError(null); }}>
              취소
            </button>
            <button type="button" className="mcBtn mcBtnPrimary" style={{ flex: 1, width: "auto" }} onClick={handleAdd}>
              저장
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
