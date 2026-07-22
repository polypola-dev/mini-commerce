"use client";

import { useState } from "react";
import AddressLabelPicker from "./address-label-picker";
import { openPostcodeSearch } from "@/lib/postcode";

// 배송지 입력 폼 — 배송지 관리/주문서 팝업 공용. 우편번호 검색으로 실제 주소만 채우고
// (자유 입력 금지), 프론트에서도 백엔드와 동일한 규칙으로 1차 검증한다.
export type AddressFormValue = {
  label: string;
  name: string;
  phone: string;
  address1: string;
  address2: string;
  zipCode: string;
};

const EMPTY: AddressFormValue = { label: "집", name: "", phone: "", address1: "", address2: "", zipCode: "" };

// 백엔드 SaveAddressRequest의 @Pattern/@Size와 동일 규칙(이중 검증).
export function validateAddress(f: AddressFormValue): string | null {
  if (!/^[가-힣a-zA-Z ]{2,30}$/.test(f.name.trim())) return "받는 분 이름을 2~30자, 한글/영문으로 입력해주세요.";
  if (!/^01[0-9]-?\d{3,4}-?\d{4}$/.test(f.phone.trim())) return "연락처 형식이 올바르지 않아요. (예: 010-1234-5678)";
  if (f.address1.trim().length < 5) return "우편번호 검색으로 주소를 선택해주세요.";
  return null;
}

export default function AddressForm({
  initial,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  initial?: Partial<AddressFormValue>;
  submitLabel: string;
  onSubmit: (v: AddressFormValue) => void;
  onCancel: () => void;
}) {
  const [form, setForm] = useState<AddressFormValue>({ ...EMPTY, ...initial });
  const [error, setError] = useState<string | null>(null);

  function searchPostcode() {
    openPostcodeSearch((r) => {
      setForm((f) => ({ ...f, zipCode: r.zipCode, address1: r.roadAddress }));
      setError(null);
    }).catch((e) => setError(e instanceof Error ? e.message : "우편번호 서비스를 불러오지 못했어요."));
  }

  function submit() {
    const err = validateAddress(form);
    if (err) {
      setError(err);
      return;
    }
    setError(null);
    onSubmit({
      label: form.label.trim(),
      name: form.name.trim(),
      phone: form.phone.trim(),
      address1: form.address1.trim(),
      address2: form.address2.trim(),
      zipCode: form.zipCode.trim(),
    });
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
      <div>
        <div style={{ fontSize: "13px", fontWeight: 600, marginBottom: "9px", color: "var(--color-body)" }}>배송지명</div>
        <AddressLabelPicker value={form.label} onChange={(v) => setForm((f) => ({ ...f, label: v }))} />
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
        <input className="mcCheckoutInput" placeholder="받는 분" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
        <input className="mcCheckoutInput" placeholder="연락처 (010-1234-5678)" inputMode="tel" value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />

        <div style={{ display: "flex", gap: "8px" }}>
          <input className="mcCheckoutInput" placeholder="우편번호" value={form.zipCode} readOnly style={{ flex: 1, background: "var(--color-surface-strong)" }} />
          <button
            type="button"
            onClick={searchPostcode}
            style={{ flex: "none", width: "auto", border: "1px solid var(--color-border-strong)", background: "#fff", borderRadius: "var(--radius-sm)", padding: "0 16px", fontSize: "13px", fontWeight: 700, cursor: "pointer" }}
          >
            우편번호 검색
          </button>
        </div>
        <input
          className="mcCheckoutInput"
          placeholder="주소 (우편번호 검색)"
          value={form.address1}
          readOnly
          onClick={searchPostcode}
          style={{ background: "var(--color-surface-strong)", cursor: "pointer" }}
        />
        <input className="mcCheckoutInput" placeholder="상세 주소" value={form.address2} onChange={(e) => setForm((f) => ({ ...f, address2: e.target.value }))} />
      </div>

      {error && <p style={{ color: "var(--color-error)", fontSize: "12.5px", margin: 0 }}>{error}</p>}

      <div style={{ display: "flex", gap: "8px" }}>
        <button type="button" className="mcBtn mcBtnSecondary" style={{ flex: 1, width: "auto" }} onClick={onCancel}>취소</button>
        <button type="button" className="mcBtn mcBtnPrimary" style={{ flex: 1, width: "auto" }} onClick={submit}>{submitLabel}</button>
      </div>
    </div>
  );
}
