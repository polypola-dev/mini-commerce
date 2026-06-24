"use client";

import { useState } from "react";
import { getNotifications, NotificationItem } from "@/lib/api";

const TYPE_LABEL: Record<string, string> = {
  ORDER_PLACED: "주문접수",
  ORDER_PAID: "결제완료",
};

const STATUS_COLOR: Record<string, string> = {
  SENT: "#16a34a",
  FAILED: "#dc2626",
  PENDING: "#6b7280",
};

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);

  async function handleToggle() {
    const nextOpen = !open;
    setOpen(nextOpen);
    if (nextOpen) {
      setLoading(true);
      try {
        const data = await getNotifications();
        setNotifications(data);
      } catch {
        setNotifications([]);
      } finally {
        setLoading(false);
      }
    }
  }

  return (
    <div style={{ position: "relative" }}>
      <button
        onClick={handleToggle}
        style={{
          background: "none",
          border: "none",
          cursor: "pointer",
          fontSize: "20px",
          padding: "4px 8px",
          lineHeight: 1,
        }}
        aria-label="알림 보기"
      >
        🔔
      </button>

      {open && (
        <div
          style={{
            position: "absolute",
            top: "calc(100% + 8px)",
            right: 0,
            width: "320px",
            background: "#ffffff",
            border: "1px solid #e5e7eb",
            borderRadius: "8px",
            boxShadow: "0 4px 16px rgba(0,0,0,0.12)",
            zIndex: 1000,
            maxHeight: "400px",
            overflowY: "auto",
          }}
        >
          <div
            style={{
              padding: "12px 16px",
              borderBottom: "1px solid #e5e7eb",
              fontWeight: 600,
              fontSize: "14px",
              color: "#111827",
            }}
          >
            알림
          </div>

          {loading ? (
            <div style={{ padding: "16px", textAlign: "center", color: "#6b7280", fontSize: "14px" }}>
              로딩 중...
            </div>
          ) : notifications.length === 0 ? (
            <div style={{ padding: "16px", textAlign: "center", color: "#6b7280", fontSize: "14px" }}>
              알림이 없습니다
            </div>
          ) : (
            <ul style={{ margin: 0, padding: 0, listStyle: "none" }}>
              {notifications.map((n) => (
                <li
                  key={n.id}
                  style={{
                    padding: "12px 16px",
                    borderBottom: "1px solid #f3f4f6",
                    display: "flex",
                    flexDirection: "column",
                    gap: "4px",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <span
                      style={{
                        fontSize: "11px",
                        fontWeight: 600,
                        padding: "2px 6px",
                        borderRadius: "4px",
                        background: "#f3f4f6",
                        color: "#374151",
                      }}
                    >
                      {TYPE_LABEL[n.type] ?? n.type}
                    </span>
                    <span
                      style={{
                        fontSize: "11px",
                        fontWeight: 600,
                        color: STATUS_COLOR[n.status] ?? "#6b7280",
                      }}
                    >
                      ●
                    </span>
                  </div>
                  <p style={{ margin: 0, fontSize: "13px", color: "#111827" }}>{n.message}</p>
                  <span style={{ fontSize: "11px", color: "#9ca3af" }}>
                    {new Date(n.createdAt).toLocaleString("ko-KR")}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
