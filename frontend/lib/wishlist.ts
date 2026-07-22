"use client";

import { useCallback, useEffect, useState } from "react";

// 위시리스트는 백엔드(shop-api)에 저장된다(C4). 기기 간 동기화를 위해 localStorage를 대체한다.
// 여러 컴포넌트 인스턴스(상품카드·위시리스트 페이지)가 한 상태를 공유하도록 모듈 캐시 +
// 커스텀 이벤트로 브로드캐스트하고, 하트 반응성을 위해 낙관적 업데이트 후 실패 시 롤백한다.

const CHANGE_EVENT = "mc-wishlist-change";

let cache: string[] = [];
let loaded = false;
let inflight: Promise<void> | null = null;

function broadcast() {
  window.dispatchEvent(new CustomEvent(CHANGE_EVENT));
}

async function loadOnce(): Promise<void> {
  if (loaded) return;
  if (inflight) return inflight;
  inflight = fetch("/api/proxy/wishlist", { cache: "no-store" })
    .then((res) => (res.ok ? res.json() : []))
    .then((ids: string[]) => {
      cache = Array.isArray(ids) ? ids : [];
    })
    .catch(() => {
      cache = [];
    })
    .finally(() => {
      loaded = true;
      inflight = null;
      broadcast();
    });
  return inflight;
}

export function useWishlist() {
  const [ids, setIds] = useState<string[]>(cache);

  useEffect(() => {
    setIds(cache);
    const sync = () => setIds(cache);
    window.addEventListener(CHANGE_EVENT, sync);
    void loadOnce();
    return () => window.removeEventListener(CHANGE_EVENT, sync);
  }, []);

  const isSaved = useCallback((id: string) => ids.includes(id), [ids]);

  const toggle = useCallback((id: string) => {
    const previous = cache;
    const has = previous.includes(id);
    const next = has ? previous.filter((x) => x !== id) : [...previous, id];

    // 낙관적 반영
    cache = next;
    broadcast();

    const request = has
      ? fetch(`/api/proxy/wishlist/${id}`, { method: "DELETE" })
      : fetch("/api/proxy/wishlist", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ productId: id }),
        });

    request
      .then((res) => {
        if (!res.ok) {
          cache = previous;
          broadcast();
        }
      })
      .catch(() => {
        cache = previous;
        broadcast();
      });
  }, []);

  return { ids, isSaved, toggle };
}
