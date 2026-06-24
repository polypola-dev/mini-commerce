"use client";

import { useCallback, useEffect, useState } from "react";

const STORAGE_KEY = "mc_wishlist";

function readWishlist(): string[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as string[]) : [];
  } catch {
    return [];
  }
}

function writeWishlist(ids: string[]) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
  window.dispatchEvent(new CustomEvent("mc-wishlist-change"));
}

export function useWishlist() {
  const [ids, setIds] = useState<string[]>([]);

  useEffect(() => {
    setIds(readWishlist());
    const sync = () => setIds(readWishlist());
    window.addEventListener("storage", sync);
    window.addEventListener("mc-wishlist-change", sync);
    return () => {
      window.removeEventListener("storage", sync);
      window.removeEventListener("mc-wishlist-change", sync);
    };
  }, []);

  const isSaved = useCallback((id: string) => ids.includes(id), [ids]);

  const toggle = useCallback((id: string) => {
    const current = readWishlist();
    const next = current.includes(id)
      ? current.filter((x) => x !== id)
      : [...current, id];
    writeWishlist(next);
    setIds(next);
  }, []);

  return { ids, isSaved, toggle };
}
