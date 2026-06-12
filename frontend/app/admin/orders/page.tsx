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

function fmtDate(iso: string) {
  const d = new Date(iso);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

type SortCol = "createdAt" | "status" | "totalAmount" | null;
type SortDir = "asc" | "desc";

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [sortCol, setSortCol] = useState<SortCol>(null);
  const [sortDir, setSortDir] = useState<SortDir>("desc");
  const [drawerOrder, setDrawerOrder] = useState<OrderResponse | null>(null);
  const [drawerStatus, setDrawerStatus] = useState("");

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
      if (drawerOrder?.orderId === orderId) {
        setDrawerOrder((prev) => prev ? { ...prev, status } : null);
      }
    } catch (e) {
      alert(e instanceof Error ? e.message : "상태 변경 실패");
    } finally {
      setUpdating(null);
    }
  }

  async function handleDrawerStatusChange() {
    if (!drawerOrder) return;
    await handleStatusChange(drawerOrder.orderId, drawerStatus);
  }

  function handleSortClick(col: SortCol) {
    if (sortCol === col) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortCol(col);
      setSortDir("desc");
    }
  }

  function openDrawer(order: OrderResponse) {
    setDrawerOrder(order);
    setDrawerStatus(order.status);
  }

  function closeDrawer() {
    setDrawerOrder(null);
  }

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    const result = orders.filter((o) => {
      const matchStatus = statusFilter === "ALL" || o.status === statusFilter;
      const matchSearch = !q ||
        o.orderId.toLowerCase().includes(q) ||
        (o.customerId?.toLowerCase().includes(q) ?? false) ||
        o.lines?.some((l) => l.productName.toLowerCase().includes(q));
      return matchStatus && matchSearch;
    });

    if (!sortCol) return result;

    return [...result].sort((a, b) => {
      let av: string | number;
      let bv: string | number;
      if (sortCol === "createdAt") {
        av = a.createdAt;
        bv = b.createdAt;
      } else if (sortCol === "status") {
        av = a.status;
        bv = b.status;
      } else {
        av = a.totalAmount;
        bv = b.totalAmount;
      }
      if (av < bv) return sortDir === "asc" ? -1 : 1;
      if (av > bv) return sortDir === "asc" ? 1 : -1;
      return 0;
    });
  }, [orders, search, statusFilter, sortCol, sortDir]);

  const allIds = filtered.map((o) => o.orderId);
  const allChecked = allIds.length > 0 && allIds.every((id) => selected.has(id));
  const someChecked = allIds.some((id) => selected.has(id)) && !allChecked;

  function toggleAll() {
    if (allChecked) {
      setSelected((prev) => {
        const next = new Set(prev);
        allIds.forEach((id) => next.delete(id));
        return next;
      });
    } else {
      setSelected((prev) => new Set([...prev, ...allIds]));
    }
  }

  function toggleOne(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function sortArrow(col: SortCol) {
    if (sortCol !== col) return null;
    return <span className={styles.sortArrow}>{sortDir === "asc" ? "▲" : "▼"}</span>;
  }

  const selectedCount = allIds.filter((id) => selected.has(id)).length;

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
            {selectedCount > 0 && (
              <span className={styles.selectedBadge}>{selectedCount}건 선택됨</span>
            )}
          </div>
          <table className={styles.table}>
            <thead>
              <tr>
                <th style={{ width: 40, paddingLeft: 18 }}>
                  <input
                    type="checkbox"
                    className={styles.ck}
                    checked={allChecked}
                    ref={(el) => { if (el) el.indeterminate = someChecked; }}
                    onChange={toggleAll}
                  />
                </th>
                <th>주문번호</th>
                <th>고객 ID</th>
                <th>상품</th>
                <th
                  className={`${styles.thSortable}${sortCol === "totalAmount" ? " " + styles.thSortActive : ""}`}
                  onClick={() => handleSortClick("totalAmount")}
                >
                  금액 {sortArrow("totalAmount")}
                </th>
                <th
                  className={`${styles.thSortable}${sortCol === "createdAt" ? " " + styles.thSortActive : ""}`}
                  onClick={() => handleSortClick("createdAt")}
                >
                  주문일시 {sortArrow("createdAt")}
                </th>
                <th
                  className={`${styles.thSortable}${sortCol === "status" ? " " + styles.thSortActive : ""}`}
                  onClick={() => handleSortClick("status")}
                >
                  상태 {sortArrow("status")}
                </th>
                <th>상태 변경</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((order) => {
                const meta = STATUS_META[order.status];
                return (
                  <tr
                    key={order.orderId}
                    className={styles.rowClickable}
                    onClick={() => openDrawer(order)}
                  >
                    <td style={{ paddingLeft: 18 }} onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        className={styles.ck}
                        checked={selected.has(order.orderId)}
                        onChange={() => toggleOne(order.orderId)}
                      />
                    </td>
                    <td className={styles.cellMono} title={order.orderId}>{order.orderId.slice(0, 8)}…</td>
                    <td className={styles.cellMono}>
                      {order.customerId ? (
                        <span>
                          <span style={{ display: "block", fontWeight: 700 }}>
                            {order.customerId.slice(0, 8)}
                          </span>
                          <span className={styles.pmeta}>
                            u_{order.customerId.slice(0, 4)}…
                          </span>
                        </span>
                      ) : "—"}
                    </td>
                    <td style={{ maxWidth: 220 }}>
                      {order.lines?.map((l) => l.productName).join(", ") || "—"}
                    </td>
                    <td className={styles.cellNum}>
                      {order.totalAmount.toLocaleString("ko-KR")}원
                    </td>
                    <td style={{ color: "#8b95a1", fontSize: 12.5 }}>
                      {fmtDate(order.createdAt)}
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
                    <td onClick={(e) => e.stopPropagation()}>
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

      {drawerOrder && (
        <div className={styles.drawerMask} onClick={closeDrawer} />
      )}
      <div className={`${styles.drawer}${drawerOrder ? " " + styles.drawerOpen : ""}`}>
        {drawerOrder && (
          <>
            <div className={styles.drawerHead}>
              <span style={{ fontWeight: 700, fontSize: 15 }}>주문 상세</span>
              <button
                onClick={closeDrawer}
                style={{
                  marginLeft: "auto",
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  fontSize: 18,
                  color: "#8b95a1",
                  lineHeight: 1,
                  padding: "4px 6px",
                }}
              >
                ✕
              </button>
            </div>
            <div className={styles.drawerBody}>
              <dl className={styles.dl}>
                <dt className={styles.dlTerm}>주문번호</dt>
                <dd className={`${styles.dlDesc} ${styles.cellMono}`}>{drawerOrder.orderId}</dd>

                <dt className={styles.dlTerm}>주문일시</dt>
                <dd className={styles.dlDesc}>
                  {fmtDate(drawerOrder.createdAt)}
                </dd>

                <dt className={styles.dlTerm}>고객 ID</dt>
                <dd className={`${styles.dlDesc} ${styles.cellMono}`}>
                  {drawerOrder.customerId ?? "—"}
                </dd>
              </dl>

              <p className={styles.sectTitle}>주문 상품</p>
              {drawerOrder.lines?.length > 0 ? (
                drawerOrder.lines.map((line, i) => (
                  <div key={i} className={styles.lineItem}>
                    <span className={styles.lineItemName}>
                      {line.productName}
                      {line.selectedOptionValue && (
                        <span className={styles.pmeta}> ({line.selectedOptionValue})</span>
                      )}
                    </span>
                    <span className={styles.lineItemMeta}>
                      {line.quantity}개 · {line.subtotal.toLocaleString("ko-KR")}원
                    </span>
                  </div>
                ))
              ) : (
                <p style={{ color: "#8b95a1", fontSize: 13 }}>상품 정보 없음</p>
              )}

              <p className={styles.sectTitle}>배송지</p>
              <dl className={styles.dl}>
                <dt className={styles.dlTerm}>수령인</dt>
                <dd className={styles.dlDesc}>{drawerOrder.shippingRecipient ?? "—"}</dd>

                <dt className={styles.dlTerm}>주소</dt>
                <dd className={styles.dlDesc}>
                  {drawerOrder.shippingAddress
                    ? `${drawerOrder.shippingAddress} ${drawerOrder.shippingDetailAddress ?? ""}`.trim()
                    : "—"}
                </dd>
              </dl>

              <dl className={styles.dl} style={{ marginTop: 16 }}>
                <dt className={styles.dlTerm}>총 결제금액</dt>
                <dd className={`${styles.dlDesc} ${styles.cellNum}`} style={{ fontSize: 15 }}>
                  {drawerOrder.totalAmount.toLocaleString("ko-KR")}원
                </dd>
              </dl>
            </div>
            <div className={styles.drawerFoot}>
              <select
                className={styles.statusSelect}
                value={drawerStatus}
                disabled={updating === drawerOrder.orderId}
                onChange={(e) => setDrawerStatus(e.target.value)}
                style={{ flex: 1 }}
              >
                {STATUS_OPTIONS.map((s) => (
                  <option key={s} value={s}>{STATUS_META[s]?.label ?? s}</option>
                ))}
              </select>
              <button
                className={styles.btnPrimary}
                disabled={updating === drawerOrder.orderId}
                onClick={handleDrawerStatusChange}
              >
                상태 변경
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
