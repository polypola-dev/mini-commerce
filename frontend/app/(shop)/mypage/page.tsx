"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";
import { getCart } from "@/lib/api";
import { useWishlist } from "@/lib/wishlist";

function displayLabel(
  user:
    | { email?: string | null; user_metadata?: { email?: string; name?: string; nickname?: string } }
    | null
    | undefined
) {
  if (!user) return null;
  return user.email || user.user_metadata?.email || user.user_metadata?.name || user.user_metadata?.nickname || "사용자";
}

export default function MyPage() {
  const router = useRouter();
  const [label, setLabel] = useState<string | null | undefined>(undefined);
  const [cartCount, setCartCount] = useState(0);
  const { ids: wishIds } = useWishlist();

  useEffect(() => {
    const supabase = createClient();
    supabase.auth.getUser().then(({ data: { user } }) => {
      if (!user) {
        router.replace("/login");
        return;
      }
      setLabel(displayLabel(user));
    });
  }, [router]);

  useEffect(() => {
    getCart()
      .then((cart) => setCartCount(cart.items.reduce((a, i) => a + i.quantity, 0)))
      .catch(() => setCartCount(0));
  }, []);

  async function handleLogout() {
    const supabase = createClient();
    await supabase.auth.signOut();
    router.push("/login");
    router.refresh();
  }

  if (!label) return null;

  return (
    <div>
      <Link href="/mypage/profile" className="mcProfileHeader" style={{ textDecoration: "none", color: "inherit" }}>
        <div className="mcAvatar">{label[0]?.toUpperCase()}</div>
        <div>
          <div style={{ fontSize: "18px", fontWeight: 700 }}>{label}</div>
        </div>
      </Link>

      <div className="mcStatsRow">
        <Link href="/cart" className="mcStatItem" style={{ textDecoration: "none", color: "inherit" }}>
          <div className="mcStatValue">{cartCount}</div>
          <div className="mcStatLabel">장바구니</div>
        </Link>
        <Link href="/wishlist" className="mcStatItem" style={{ textDecoration: "none", color: "inherit" }}>
          <div className="mcStatValue">{wishIds.length}</div>
          <div className="mcStatLabel">찜</div>
        </Link>
      </div>

      <div>
        <Link href="/orders" className="mcMenuItem">
          주문 내역<span className="mcMenuChevron">›</span>
        </Link>
        <Link href="/mypage/address" className="mcMenuItem">
          배송지 관리<span className="mcMenuChevron">›</span>
        </Link>
        <div className="mcMenuItem" style={{ color: "var(--color-muted-soft)", cursor: "default" }}>
          쿠폰 / 적립금<span className="mcMenuChevron">·</span>
        </div>
        <Link href="/customer-service" className="mcMenuItem">
          고객센터<span className="mcMenuChevron">›</span>
        </Link>
        <button type="button" className="mcMenuItem" onClick={handleLogout} style={{ color: "var(--color-muted)" }}>
          로그아웃
        </button>
      </div>
    </div>
  );
}
