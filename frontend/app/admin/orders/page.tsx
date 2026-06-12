"use client";

import { adminGetOrders, adminUpdateOrderStatus, type OrderResponse } from "@/lib/api";
import { useEffect, useMemo, useState } from "react";
import styles from "../admin.module.css";

const STATUS_OPTIONS = ["PENDING_PAYMENT", "PAID", "SHIPPED", "DELIVERED", "CANCELED"];

const STATUS_META: Record<string, { label: string; badge: string; dot: string }> = {
  PENDING_PAYMENT: { label: "결제 대기", badge: styles.badgePending, dot: "#ff9500" },
  PAID:            { label: "결제 완료", badge: styles.badgePaid,    dot: "#00b86b" },
  SHIPPED:         { label: "배송 중",   badge: styles.badgeShipped,  dot: "#3182f6" },
  DELIVERED:       { label: "배송 완료", badge: styles.badgeDelivered, dot: "#6c5ce7" },
  CANCELED:        { label: "취소됨",   badge: styles.badgeCanceled, dot: "#f04452" },
};

const FILTER_CHIPS = [
  { key: "ALL",             label: "전체" },
  { key: "PENDING_PAYMENT", label: "결제 대기" },
  { key: "PAID",            label: "결제 완료" },
  { key: "SHIPPED",         label: "배송 중" },
  { key: "DELIVERED",       label: "배송 완료" },
  { key: "CANCELED",        label: "취소됨" },
];

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  async function load() {
    setLoading(true);
    try { setOrders(await adminGetOrders()); }
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  async function handleStatusChange(orderId: string, status: string) {
    setUpdating(orderId);
    try {
      await adminUpdateOrderStatus(orderId, status);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "상태 변경 실패");
    } finally {
      setUpdating(null);
    }
  }

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return orders.filter((o) => {
      const matchStatus = statusFilter === "ALL" || o.status === statusFilter;
      const matchSearch = !q ||
        o.orderId.toLowerCase().includes(q) ||
        (o.customerId?.toLowerCase().includes(q) ?? false) ||
        o.lines?.some((l) => l.productName.toLowerCase().includes(q));
      return matchStatus && matchSearch;
    });
  }, [orders, search, statusFilter]);

  return (
    <div className={styles.content}>
      <div className={styles.pageHead}>
        <div>
          <h1 className={styles.pageTitle}>주문 관리</h1>
          <p className={styles.pageSubtitle}>전체 주문 내역을 조회하고 상태를 변경합니다</p>
        </div>
      </div>

      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <div className={styles.filterField}>
            <span className={styles.filterLabel}>검색</span>
            <div className={styles.filterControl}>
              <span>🔍</span>
              <input
                className={styles.filterInput}
                placeholder="주문번호 · 고객ID · 상품명"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>
        </div>
        <div className={styles.chips}>
          {FILTER_CHIPS.map((chip) => (
            <button
              key={chip.key}
              className={`${styles.chip}${statusFilter === chip.key ? " " + styles.chipActive : ""}`}
              onClick={() => setStatusFilter(chip.key)}
            >
              {chip.label}
            </button>
          ))}
          <span className={styles.chipCount}>
            총 <b style={{ color: "#191f28" }}>{filtered.length}</b>건
          </span>
        </div>
      </div>

      {loading ? (
        <div className={styles.emptyState}>불러오는 중...</div>
      ) : filtered.length === 0 ? (
        <div className={styles.emptyState}>주문이 없습니다.</div>
      ) : (
        <div className={styles.tableWrap}>
          <div className={styles.tableTools}>
            <span>
              <span className={styles.tableToolsCount}>{filtered.length}</span>건 표시 중
            </span>
          </div>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>주문번호</th>
                <th>고객 ID</th>
                <th>상품</th>
                <th>금액</th>
                <th>주문일시</th>
                <th>상태</th>
                <th>상태 변경</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((order) => {
                const meta = STATUS_META[order.status];
                return (
                  <tr key={order.orderId}>
                    <td className={styles.cellMono}>{order.orderId.slice(0, 8)}…</td>
                    <td className={styles.cellMono}>
                      {order.customerId ? `${order.customerId.slice(0, 8)}…` : "—"}
                    </td>
                    <td style={{ maxWidth: 220 }}>
                      {order.lines?.map((l) => l.productName).join(", ") || "—"}
                    </td>
                    <td className={styles.cellNum}>
                      {order.totalAmount.toLocaleString("ko-KR")}원
                    </td>
                    <td style={{ color: "#8b95a1", fontSize: 12.5 }}>
                      {new Date(order.createdAt).toLocaleString("ko-KR")}
                    </td>
                    <td>
                      {meta ? (
                        <span className={`${styles.badge} ${meta.badge}`}>
                          <span className={styles.badgeDot} style={{ background: meta.dot }} />
                          {meta.label}
                        </span>
                      ) : (
                        <span className={styles.badge}>{order.status}</span>
                      )}
                    </td>
                    <td>
                      <select
                        className={styles.statusSelect}
                        value={order.status}
                        disabled={updating === order.orderId}
                        onChange={(e) => handleStatusChange(order.orderId, e.target.value)}
                      >
                        {STATUS_OPTIONS.map((s) => (
                          <option key={s} value={s}>{STATUS_META[s]?.label ?? s}</option>
                        ))}
                      </select>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
