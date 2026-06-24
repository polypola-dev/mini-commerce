"use client";

import { useRouter, usePathname } from "next/navigation";
import { useTransition, useState, useEffect } from "react";

export default function SearchBar({ initialQuery }: { initialQuery: string }) {
  const router = useRouter();
  const pathname = usePathname();
  const [isPending, startTransition] = useTransition();
  const [value, setValue] = useState(initialQuery);

  useEffect(() => {
    setValue(initialQuery);
  }, [initialQuery]);

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const q = e.target.value;
    setValue(q);
    startTransition(() => {
      const params = new URLSearchParams();
      if (q) params.set("q", q);
      router.push(`${pathname}?${params.toString()}`);
    });
  }

  function handleClear() {
    setValue("");
    startTransition(() => {
      router.push(pathname);
    });
  }

  return (
    <div className="searchBar">
      <div className="searchInputWrap">
        <svg className="searchIcon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
          <circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" strokeWidth="1.6" />
          <path d="M13 13l3.5 3.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        </svg>
        <input
          type="search"
          className="searchInput"
          placeholder="상품 이름 또는 설명 검색..."
          value={value}
          onChange={handleChange}
          aria-label="상품 검색"
        />
        {value && (
          <button className="searchClearBtn" onClick={handleClear} aria-label="검색어 초기화">
            ✕
          </button>
        )}
      </div>
      {isPending && <span className="searchPending">검색 중...</span>}
    </div>
  );
}
