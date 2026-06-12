"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import styles from "./admin.module.css";

const NAV_GROUPS = [
  {
    title: "개요",
    items: [
      { href: "/admin", label: "대시보드", icon: "◫", exact: true },
    ],
  },
  {
    title: "상품",
    items: [
      { href: "/admin/products", label: "상품 목록", icon: "▦" },
      { href: "/admin/products/new", label: "상품 등록", icon: "＋" },
    ],
  },
  {
    title: "주문",
    items: [
      { href: "/admin/orders", label: "주문 목록", icon: "≡" },
    ],
  },
];

type NavItem = { href: string; label: string; icon: string; exact?: boolean };

export function AdminSidebar() {
  const pathname = usePathname();
  const [pins, setPins] = useState<string[]>([]);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    try {
      const stored = localStorage.getItem("admin-pins");
      if (stored) setPins(JSON.parse(stored));
    } catch {}
  }, []);

  function togglePin(href: string) {
    setPins((prev) => {
      const next = prev.includes(href)
        ? prev.filter((p) => p !== href)
        : [...prev, href];
      try { localStorage.setItem("admin-pins", JSON.stringify(next)); } catch {}
      return next;
    });
  }

  function isActive(item: NavItem) {
    if (item.exact) return pathname === item.href;
    return pathname === item.href || pathname.startsWith(item.href + "/");
  }

  const allItems = NAV_GROUPS.flatMap((g) => g.items);
  const pinnedItems = mounted ? allItems.filter((i) => pins.includes(i.href)) : [];

  return (
    <nav className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.brandLogo}>M</div>
        <div>
          <p className={styles.brandName}>mini-commerce</p>
          <small className={styles.brandSub}>백오피스</small>
        </div>
      </div>

      <button className={styles.searchTrigger} onClick={() => {}}>
        <span>🔍</span>
        <span className={styles.searchPlaceholder}>검색...</span>
        <span className={styles.kbd}>
          <b className={styles.kbdKey}>⌘</b>
          <b className={styles.kbdKey}>K</b>
        </span>
      </button>

      <div className={styles.nav}>
        {pinnedItems.length > 0 && (
          <div className={styles.navGroup}>
            <p className={styles.navGroupTitle}>즐겨찾기</p>
            {pinnedItems.map((item) => (
              <NavRow
                key={item.href}
                item={item}
                active={isActive(item)}
                pinned
                onTogglePin={() => togglePin(item.href)}
              />
            ))}
          </div>
        )}

        {NAV_GROUPS.map((group) => (
          <div key={group.title} className={styles.navGroup}>
            <p className={styles.navGroupTitle}>{group.title}</p>
            {group.items.map((item) => (
              <NavRow
                key={item.href}
                item={item}
                active={isActive(item)}
                pinned={mounted && pins.includes(item.href)}
                onTogglePin={() => togglePin(item.href)}
              />
            ))}
          </div>
        ))}
      </div>

      <div className={styles.sidebarFoot}>
        <Link href="/" className={styles.backLink}>← 사이트로 돌아가기</Link>
      </div>
    </nav>
  );
}

function NavRow({
  item,
  active,
  pinned,
  onTogglePin,
}: {
  item: NavItem;
  active: boolean;
  pinned: boolean;
  onTogglePin: () => void;
}) {
  return (
    <div className={styles.navRow}>
      <Link
        href={item.href}
        className={`${styles.navLink}${active ? " " + styles.navLinkActive : ""}`}
      >
        <span className={styles.navIcon}>{item.icon}</span>
        {item.label}
      </Link>
      <button
        className={`${styles.pinBtn}${pinned ? " " + styles.pinBtnOn : ""}`}
        onClick={(e) => { e.preventDefault(); onTogglePin(); }}
        title={pinned ? "즐겨찾기 해제" : "즐겨찾기"}
      >
        {pinned ? "★" : "☆"}
      </button>
    </div>
  );
}
