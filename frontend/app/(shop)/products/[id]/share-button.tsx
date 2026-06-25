"use client";

import { useState } from "react";

export default function ShareButton() {
  const [open, setOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 1900);
  }

  async function copyLink() {
    await navigator.clipboard.writeText(window.location.href);
    setOpen(false);
    showToast("링크가 복사되었어요");
  }

  return (
    <>
      <button
        type="button"
        aria-label="공유"
        onClick={() => setOpen(true)}
        style={{
          width: 38,
          height: 38,
          borderRadius: "9999px",
          border: "none",
          background: "rgba(255,255,255,0.92)",
          cursor: "pointer",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          boxShadow: "var(--shadow-card)",
        }}
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="18" cy="5" r="3" />
          <circle cx="6" cy="12" r="3" />
          <circle cx="18" cy="19" r="3" />
          <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
          <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
        </svg>
      </button>

      {open && (
        <div className="mcSheetOverlay" onClick={() => setOpen(false)}>
          <div className="mcSheet" onClick={(e) => e.stopPropagation()}>
            <div className="mcSheetHandle" />
            <div style={{ fontSize: 16, fontWeight: 700, marginBottom: 18 }}>공유하기</div>
            <div className="mcShareIconRow">
              <button type="button" className="mcShareIcon" onClick={() => setOpen(false)}>
                <span className="mcShareIconCircle" style={{ background: "#fee500", color: "#3a1d1d", fontWeight: 800 }}>
                  K
                </span>
                <span className="mcShareIconLabel">카카오톡</span>
              </button>
              <button type="button" className="mcShareIcon" onClick={() => setOpen(false)}>
                <span className="mcShareIconCircle" style={{ background: "#1d9bf0", color: "#fff" }}>
                  ✦
                </span>
                <span className="mcShareIconLabel">메시지</span>
              </button>
              <button type="button" className="mcShareIcon" onClick={() => setOpen(false)}>
                <span className="mcShareIconCircle" style={{ background: "var(--color-surface-strong)" }}>
                  ⋯
                </span>
                <span className="mcShareIconLabel">더보기</span>
              </button>
            </div>
            <button type="button" className="mcShareLinkRow" onClick={copyLink}>
              <span className="mcShareLinkText">{window.location.href}</span>
              <span className="mcShareLinkCopy">링크 복사</span>
            </button>
          </div>
        </div>
      )}

      {toast && (
        <div className="mcToast">
          <span>{toast}</span>
        </div>
      )}
    </>
  );
}
