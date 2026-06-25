"use client";

import { useSearchOverlay } from "@/lib/search-overlay";

export default function SearchTriggerButton({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  const { openSearch } = useSearchOverlay();

  return (
    <button type="button" className={className} onClick={() => openSearch()}>
      {children}
    </button>
  );
}
