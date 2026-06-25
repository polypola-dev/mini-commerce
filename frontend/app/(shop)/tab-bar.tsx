"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { getCart } from "@/lib/api";
import { useWishlist } from "@/lib/wishlist";
import { useSearchOverlay } from "@/lib/search-overlay";

const TABS = [
  {
    href: "/",
    match: (p: string) => p === "/",
    label: "홈",
    icon: (c: string) => (
      <svg width="24" height="24" fill="none" stroke={c} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 11.5 12 4l8 7.5" />
        <path d="M6 10v9a1 1 0 0 0 1 1h3v-6h4v6h3a1 1 0 0 0 1-1v-9" />
      </svg>
    ),
  },
  {
    href: "/search",
    match: (p: string) => p.startsWith("/search"),
    label: "검색",
    icon: (c: string) => (
      <svg width="24" height="24" fill="none" stroke={c} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="10.5" cy="10.5" r="6.5" />
        <path d="m20 20-4.5-4.5" />
      </svg>
    ),
  },
  {
    href: "/wishlist",
    match: (p: string) => p.startsWith("/wishlist"),
    label: "찜",
    icon: (c: string) => (
      <svg width="24" height="24" fill="none" stroke={c} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 21S3 15 3 8.5A4.5 4.5 0 0 1 12 6a4.5 4.5 0 0 1 9 2.5C21 15 12 21 12 21Z" />
      </svg>
    ),
  },
  {
    href: "/cart",
    match: (p: string) => p.startsWith("/cart"),
    label: "장바구니",
    icon: (c: string) => (
      <svg width="24" height="24" fill="none" stroke={c} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M6 7h15l-1.5 9.5h-12z" />
        <path d="M6 7 5 3H2" />
        <circle cx="9" cy="20.5" r="1.3" />
        <circle cx="18" cy="20.5" r="1.3" />
      </svg>
    ),
  },
  {
    href: "/mypage",
    match: (p: string) => p.startsWith("/mypage") || p.startsWith("/orders"),
    label: "마이",
    icon: (c: string) => (
      <svg width="24" height="24" fill="none" stroke={c} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="8" r="4" />
        <path d="M5 20c0-3.5 3-6 7-6s7 2.5 7 6" />
      </svg>
    ),
  },
];

const HIDDEN_ON = [/^\/products\//, /^\/checkout/];

export default function TabBar() {
  const pathname = usePathname();
  const [cartCount, setCartCount] = useState(0);
  const { ids: wishIds } = useWishlist();
  const { isOpen: searchOpen, openSearch } = useSearchOverlay();

  useEffect(() => {
    getCart()
      .then((cart) => setCartCount(cart.items.reduce((a, i) => a + i.quantity, 0)))
      .catch(() => setCartCount(0));
  }, [pathname]);

  if (HIDDEN_ON.some((re) => re.test(pathname))) return null;

  return (
    <nav className="mcTabBar">
      {TABS.map((tab) => {
        const active = tab.href === "/search" ? searchOpen || tab.match(pathname) : tab.match(pathname);
        const color = active ? "var(--color-primary)" : "var(--color-muted-soft)";
        const content = (
          <>
            <div style={{ position: "relative" }}>
              {tab.icon(color)}
              {tab.href === "/cart" && cartCount > 0 && (
                <span className="mcTabBadge">{cartCount}</span>
              )}
              {tab.href === "/wishlist" && wishIds.length > 0 && (
                <span className="mcTabBadge">{wishIds.length}</span>
              )}
            </div>
            <span className="mcTabLabel" style={{ color }}>
              {tab.label}
            </span>
          </>
        );

        if (tab.href === "/search") {
          return (
            <button key={tab.href} type="button" className="mcTabItem" onClick={() => openSearch()}>
              {content}
            </button>
          );
        }

        return (
          <Link key={tab.href} href={tab.href} className="mcTabItem">
            {content}
          </Link>
        );
      })}
    </nav>
  );
}
