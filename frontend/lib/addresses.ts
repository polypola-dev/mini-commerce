"use client";

import { useCallback, useEffect, useState } from "react";

const STORAGE_KEY = "mc_addresses";

export type Address = {
  id: string;
  name: string;
  phone: string;
  address1: string;
  address2: string;
  isDefault: boolean;
};

function readAddresses(): Address[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Address[]) : [];
  } catch {
    return [];
  }
}

function writeAddresses(addresses: Address[]) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(addresses));
  window.dispatchEvent(new CustomEvent("mc-addresses-change"));
}

export function useAddresses() {
  const [addresses, setAddresses] = useState<Address[]>([]);

  useEffect(() => {
    setAddresses(readAddresses());
    const sync = () => setAddresses(readAddresses());
    window.addEventListener("storage", sync);
    window.addEventListener("mc-addresses-change", sync);
    return () => {
      window.removeEventListener("storage", sync);
      window.removeEventListener("mc-addresses-change", sync);
    };
  }, []);

  const add = useCallback((input: Omit<Address, "id" | "isDefault">) => {
    const current = readAddresses();
    const next: Address = {
      ...input,
      id: crypto.randomUUID(),
      isDefault: current.length === 0,
    };
    const updated = [...current, next];
    writeAddresses(updated);
    setAddresses(updated);
  }, []);

  const remove = useCallback((id: string) => {
    const current = readAddresses();
    const removed = current.find((a) => a.id === id);
    let updated = current.filter((a) => a.id !== id);
    if (removed?.isDefault && updated.length > 0) {
      updated = updated.map((a, i) => (i === 0 ? { ...a, isDefault: true } : a));
    }
    writeAddresses(updated);
    setAddresses(updated);
  }, []);

  const setDefault = useCallback((id: string) => {
    const current = readAddresses();
    const updated = current.map((a) => ({ ...a, isDefault: a.id === id }));
    writeAddresses(updated);
    setAddresses(updated);
  }, []);

  return { addresses, add, remove, setDefault };
}
