import Link from "next/link";
import type { NotificationItem } from "@/lib/api";
import { getNotifications } from "@/lib/api-server";
import { createClient } from "@/lib/supabase/server";
import { redirect } from "next/navigation";

const TYPE_LABEL: Record<string, string> = {
  ORDER_PLACED: "주문접수",
  ORDER_PAID: "결제완료",
};

export default async function NotificationsPage() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  let notifications: NotificationItem[] = [];
  try {
    notifications = await getNotifications();
  } catch {
    notifications = [];
  }

  return (
    <div style={{ paddingBottom: "16px" }}>
      <div style={{ padding: "14px 20px 12px", display: "flex", alignItems: "center", gap: "12px", borderBottom: "1px solid var(--color-hairline-soft)" }}>
        <Link href="/mypage" aria-label="뒤로" style={{ border: "none", background: "transparent", cursor: "pointer", padding: 0, display: "flex" }}>
          <svg width="22" height="22" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m14 5-7 7 7 7" /></svg>
        </Link>
        <span style={{ fontSize: "18px", fontWeight: 700 }}>알림</span>
      </div>

      {notifications.length === 0 ? (
        <div className="mcEmptyState">
          <div className="mcEmptyIcon">🔔</div>
          <div className="mcEmptyTitle">받은 알림이 없어요</div>
          <div className="mcEmptyDesc">주문/배송 알림이 도착하면 여기에 표시돼요.</div>
        </div>
      ) : (
        <ul style={{ margin: 0, padding: 0, listStyle: "none" }}>
          {notifications.map((n) => (
            <li
              key={n.id}
              style={{
                padding: "16px 20px",
                borderBottom: "1px solid var(--color-hairline-soft)",
                display: "flex",
                flexDirection: "column",
                gap: "6px",
              }}
            >
              <span
                style={{
                  fontSize: "11px",
                  fontWeight: 700,
                  padding: "2px 8px",
                  borderRadius: "999px",
                  background: "var(--color-surface-strong)",
                  color: "var(--color-primary)",
                  alignSelf: "flex-start",
                }}
              >
                {TYPE_LABEL[n.type] ?? n.type}
              </span>
              <p style={{ margin: 0, fontSize: "14px", color: "var(--color-ink)", lineHeight: 1.5 }}>{n.message}</p>
              <span style={{ fontSize: "12px", color: "var(--color-muted)" }}>
                {new Date(n.createdAt).toLocaleString("ko-KR")}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
