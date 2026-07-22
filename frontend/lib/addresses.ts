"use client";

import { useCallback, useEffect, useState } from "react";

// 배송지 주소록은 백엔드(shop-api)에 저장된다(C3). 기기 간 동기화를 위해 localStorage를 대체한다.
// 기본 배송지 승격 등 불변식은 서버가 계산하므로, 변경 후에는 서버를 재조회(refetch)해
// 로컬 상태를 서버 진실과 일치시킨다. 여러 인스턴스는 모듈 캐시 + 커스텀 이벤트로 공유한다.

const CHANGE_EVENT = "mc-addresses-change";

export type Address = {
  id: string;
  name: string;
  phone: string;
  address1: string;
  address2: string;
  isDefault: boolean;
};

let cache: Address[] = [];
let loaded = false;
let inflight: Promise<void> | null = null;

function broadcast() {
  window.dispatchEvent(new CustomEvent(CHANGE_EVENT));
}

async function refetch(): Promise<void> {
  if (inflight) return inflight;
  inflight = fetch("/api/proxy/addresses", { cache: "no-store" })
    .then((res) => (res.ok ? res.json() : []))
    .then((data: Address[]) => {
      cache = Array.isArray(data) ? data : [];
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

export function useAddresses() {
  const [addresses, setAddresses] = useState<Address[]>(cache);

  useEffect(() => {
    setAddresses(cache);
    const sync = () => setAddresses(cache);
    window.addEventListener(CHANGE_EVENT, sync);
    if (!loaded) void refetch();
    return () => window.removeEventListener(CHANGE_EVENT, sync);
  }, []);

  const add = useCallback((input: Omit<Address, "id" | "isDefault">) => {
    void fetch("/api/proxy/addresses", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    }).then((res) => {
      if (res.ok) return refetch();
    });
  }, []);

  const remove = useCallback((id: string) => {
    void fetch(`/api/proxy/addresses/${id}`, { method: "DELETE" }).then((res) => {
      if (res.ok) return refetch();
    });
  }, []);

  const setDefault = useCallback((id: string) => {
    void fetch(`/api/proxy/addresses/${id}/default`, { method: "PUT" }).then((res) => {
      if (res.ok) return refetch();
    });
  }, []);

  return { addresses, add, remove, setDefault };
}
