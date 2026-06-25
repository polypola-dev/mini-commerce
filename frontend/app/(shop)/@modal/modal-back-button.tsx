"use client";

import { useRouter } from "next/navigation";

export default function ModalBackButton() {
  const router = useRouter();

  return (
    <button type="button" className="mcBackBtn" aria-label="닫기" onClick={() => router.back()}>
      <svg width="20" height="20" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="m13 5-7 7 7 7" />
      </svg>
    </button>
  );
}
