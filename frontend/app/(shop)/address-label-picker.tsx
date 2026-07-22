"use client";

import { useState } from "react";

// 배송지명(label) 선택 — 프리셋 chip + "직접입력"(최대 10자). 배송지 관리/주문서 팝업 공용.
export const ADDRESS_LABEL_PRESETS = ["집", "회사", "학교", "친구", "가족"];
export const ADDRESS_LABEL_MAX = 10;

export default function AddressLabelPicker({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  const [manual, setManual] = useState(() => value !== "" && !ADDRESS_LABEL_PRESETS.includes(value));

  function pickPreset(p: string) {
    setManual(false);
    onChange(p);
  }

  function pickManual() {
    setManual(true);
    onChange("");
  }

  const chipStyle = (active: boolean): React.CSSProperties => ({
    border: `1.5px solid ${active ? "var(--color-primary)" : "var(--color-hairline)"}`,
    background: active ? "var(--color-surface-strong)" : "#fff",
    color: active ? "var(--color-primary)" : "var(--color-body)",
    borderRadius: "999px",
    padding: "7px 15px",
    fontSize: "13px",
    fontWeight: 600,
    cursor: "pointer",
    width: "auto",
  });

  return (
    <div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: "8px", marginBottom: manual ? "10px" : 0 }}>
        {ADDRESS_LABEL_PRESETS.map((p) => (
          <button key={p} type="button" onClick={() => pickPreset(p)} style={chipStyle(!manual && value === p)}>
            {p}
          </button>
        ))}
        <button type="button" onClick={pickManual} style={chipStyle(manual)}>
          직접입력
        </button>
      </div>
      {manual && (
        <input
          className="mcCheckoutInput"
          placeholder={`배송지명 (최대 ${ADDRESS_LABEL_MAX}자)`}
          maxLength={ADDRESS_LABEL_MAX}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </div>
  );
}
