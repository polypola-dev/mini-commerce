"use client";

import { useRouter } from "next/navigation";

export default function SearchChrome({
  searchBar,
  children,
  overlay = false,
}: {
  searchBar: React.ReactNode;
  children: React.ReactNode;
  overlay?: boolean;
}) {
  const router = useRouter();

  function close() {
    router.back();
  }

  return (
    <div className={overlay ? "mcSearchOverlay" : undefined}>
      <div className="mcSearchOverlayHeader">
        <button type="button" className="mcSearchBackBtn" onClick={close} aria-label="검색 닫기">
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M18 4 8 12l10 8" />
          </svg>
        </button>
        <div className="mcSearchOverlayBarSlot">{searchBar}</div>
      </div>
      <div className={overlay ? "mcSearchOverlayBody" : undefined}>{children}</div>
      <div className="mcSearchOverlayFooter">
        <span className="mcSearchOverlayHint">자동저장 끄기 · 도움말</span>
        <button type="button" className="mcSearchCloseBtn" onClick={close}>
          닫기
        </button>
      </div>
    </div>
  );
}
