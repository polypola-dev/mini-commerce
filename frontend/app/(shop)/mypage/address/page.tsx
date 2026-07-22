"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAddresses, type Address } from "@/lib/addresses";
import AddressForm, { type AddressFormValue } from "../../address-form";

type Mode = { type: "none" } | { type: "add" } | { type: "edit"; addr: Address };

function toFormValue(a: Address): AddressFormValue {
  return { label: a.label ?? "", name: a.name, phone: a.phone, address1: a.address1, address2: a.address2, zipCode: a.zipCode ?? "" };
}

const smallBtn: React.CSSProperties = {
  width: "auto",
  border: "1px solid var(--color-hairline)",
  background: "#fff",
  color: "var(--color-ink)",
  borderRadius: "8px",
  padding: "6px 11px",
  fontSize: "12px",
  fontWeight: 600,
  cursor: "pointer",
};

export default function AddressManagePage() {
  const router = useRouter();
  const { addresses, add, update, remove, setDefault } = useAddresses();
  const [mode, setMode] = useState<Mode>({ type: "none" });

  function handleSubmit(v: AddressFormValue) {
    const payload = {
      label: v.label || null,
      name: v.name,
      phone: v.phone,
      address1: v.address1,
      address2: v.address2,
      zipCode: v.zipCode || null,
    };
    if (mode.type === "edit") update(mode.addr.id, payload);
    else add(payload);
    setMode({ type: "none" });
  }

  return (
    <div style={{ paddingBottom: "16px" }}>
      <div style={{ padding: "14px 20px 12px", display: "flex", alignItems: "center", gap: "12px", borderBottom: "1px solid var(--color-hairline-soft)" }}>
        <button type="button" onClick={() => router.back()} aria-label="뒤로" style={{ border: "none", background: "transparent", cursor: "pointer", padding: 0, display: "flex" }}>
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m14 5-7 7 7 7" /></svg>
        </button>
        <span style={{ fontSize: "18px", fontWeight: 700 }}>배송지 관리</span>
      </div>

      {mode.type !== "none" ? (
        <div style={{ padding: "20px" }}>
          <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "16px" }}>{mode.type === "edit" ? "배송지 수정" : "배송지 추가"}</div>
          <AddressForm
            initial={mode.type === "edit" ? toFormValue(mode.addr) : undefined}
            submitLabel={mode.type === "edit" ? "수정" : "저장"}
            onSubmit={handleSubmit}
            onCancel={() => setMode({ type: "none" })}
          />
        </div>
      ) : (
        <>
          {addresses.length === 0 && (
            <div className="mcEmptyState">
              <div className="mcEmptyIcon">📍</div>
              <div className="mcEmptyTitle">등록된 배송지가 없어요</div>
              <div className="mcEmptyDesc">배송지를 추가하면 주문 시 바로 사용할 수 있어요.</div>
              <button type="button" className="mcEmptyCta" style={{ border: "none" }} onClick={() => setMode({ type: "add" })}>
                배송지 추가
              </button>
            </div>
          )}

          {addresses.length > 0 && (
            <div style={{ padding: "16px 20px 0", display: "flex", flexDirection: "column", gap: "12px" }}>
              {addresses.map((a) => (
                <div key={a.id} style={{ border: "1px solid var(--color-hairline)", borderRadius: "var(--radius-md)", padding: "16px 18px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px" }}>
                    {a.label && (
                      <span style={{ background: "var(--color-surface-strong)", color: "var(--color-primary)", fontSize: "11px", fontWeight: 800, borderRadius: "6px", padding: "2px 7px" }}>
                        {a.label}
                      </span>
                    )}
                    <span style={{ fontSize: "14px", fontWeight: 700 }}>{a.name}</span>
                    {a.isDefault && (
                      <span style={{ background: "var(--color-surface-strong)", color: "var(--color-primary)", fontSize: "11px", fontWeight: 800, borderRadius: "999px", padding: "2px 9px" }}>
                        기본배송지
                      </span>
                    )}
                  </div>
                  <div style={{ fontSize: "13px", color: "var(--color-body)", marginBottom: "2px" }}>
                    {a.zipCode ? `[${a.zipCode}] ` : ""}{a.address1} {a.address2}
                  </div>
                  <div style={{ fontSize: "12.5px", color: "var(--color-muted)" }}>{a.phone}</div>
                  <div style={{ display: "flex", gap: "6px", marginTop: "12px" }}>
                    {!a.isDefault && (
                      <button type="button" style={smallBtn} onClick={() => setDefault(a.id)}>기본설정</button>
                    )}
                    <button type="button" style={smallBtn} onClick={() => setMode({ type: "edit", addr: a })}>수정</button>
                    <button type="button" style={smallBtn} onClick={() => remove(a.id)}>삭제</button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {addresses.length > 0 && (
            <div style={{ padding: "16px 20px 0" }}>
              <button
                type="button"
                style={{ width: "100%", border: "1px dashed var(--color-border-strong)", background: "#fff", borderRadius: "var(--radius-sm)", padding: "13px", fontSize: "13.5px", fontWeight: 700, color: "var(--color-muted)", cursor: "pointer" }}
                onClick={() => setMode({ type: "add" })}
              >
                ＋ 배송지 추가
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
