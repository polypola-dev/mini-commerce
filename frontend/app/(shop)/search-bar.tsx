"use client";

import { useRouter, usePathname } from "next/navigation";
import { useTransition, useState, useEffect } from "react";

export default function SearchBar({ initialQuery }: { initialQuery: string }) {
  const router = useRouter();
  const pathname = usePathname();
  const [, startTransition] = useTransition();
  const [value, setValue] = useState(initialQuery);

  useEffect(() => {
    setValue(initialQuery);
  }, [initialQuery]);

  function push(q: string) {
    startTransition(() => {
      const params = new URLSearchParams();
      if (q) params.set("q", q);
      router.push(`${pathname}?${params.toString()}`);
    });
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const q = e.target.value;
    setValue(q);
    push(q);
  }

  function handleClear() {
    setValue("");
    push("");
  }

  return (
    <div className="mcSearchInputWrap">
      <svg width="18" height="18" fill="none" stroke="#222" strokeWidth="1.9" strokeLinecap="round" aria-hidden="true">
        <circle cx="8" cy="8" r="6" />
        <path d="m17 17-4-4" />
      </svg>
      <input
        type="search"
        placeholder="상품명, 브랜드 검색"
        value={value}
        onChange={handleChange}
        aria-label="상품 검색"
        autoFocus
      />
      {value && (
        <button type="button" className="mcSearchClear" onClick={handleClear} aria-label="검색어 초기화">
          ✕
        </button>
      )}
    </div>
  );
}
