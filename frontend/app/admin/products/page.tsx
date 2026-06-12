"use client";

import { adminDeleteProduct, adminGetProducts, type Product } from "@/lib/api";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import styles from "../admin.module.css";

const FILTER_CHIPS = [
  { key: "ALL",    label: "전체" },
  { key: "ACTIVE", label: "활성" },
  { key: "OFF",    label: "비활성" },
];

function stockColor(ratio: number) {
  if (ratio > 0.5) return "#3182f6";
  if (ratio > 0.2) return "#ff9500";
  return "#f04452";
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  async function load() {
    setLoading(true);
    try { setProducts(await adminGetProducts()); }
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  async function handleDelete(id: string, name: string) {
    if (!confirm(`"${name}" 상품을 비활성화하시겠습니까?`)) return;
    try {
      await adminDeleteProduct(id);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "삭제 실패");
    }
  }

  const maxStock = useMemo(
    () => Math.max(...products.map((p) => p.availableStock), 1),
    [products]
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return products.filter((p) => {
      const matchStatus =
        statusFilter === "ALL" ||
        (statusFilter === "ACTIVE" && p.active) ||
        (statusFilter === "OFF" && !p.active);
      const matchSearch = !q || p.name.toLowerCase().includes(q);
      return matchStatus && matchSearch;
    });
  }, [products, search, statusFilter]);

  return (
    <div className={styles.content}>
      <div className={styles.pageHead}>
        <div>
          <h1 className={styles.pageTitle}>상품 관리</h1>
          <p className={styles.pageSubtitle}>상품 목록을 조회하고 관리합니다</p>
        </div>
        <div className={styles.pageActions}>
          <Link href="/admin/products/new" className={styles.btnPrimary}>
            + 상품 등록
          </Link>
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
                placeholder="상품명 검색"
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
            총 <b style={{ color: "#191f28" }}>{filtered.length}</b>개
          </span>
        </div>
      </div>

      {loading ? (
        <div className={styles.emptyState}>불러오는 중...</div>
      ) : filtered.length === 0 ? (
        <div className={styles.emptyState}>상품이 없습니다.</div>
      ) : (
        <div className={styles.tableWrap}>
          <div className={styles.tableTools}>
            <span>
              <span className={styles.tableToolsCount}>{filtered.length}</span>개 표시 중
            </span>
          </div>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>상품</th>
                <th>가격</th>
                <th>재고</th>
                <th>상태</th>
                <th style={{ textAlign: "right" }}>관리</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((product) => {
                const ratio = Math.min(product.availableStock / maxStock, 1);
                const color = stockColor(ratio);
                return (
                  <tr key={product.id}>
                    <td>
                      <div className={styles.prodCell}>
                        {product.imageUrl ? (
                          <img
                            src={product.imageUrl}
                            alt=""
                            className={styles.prodThumbImg}
                          />
                        ) : (
                          <div className={styles.prodThumb}>📦</div>
                        )}
                        <div>
                          <div className={styles.prodName}>{product.name}</div>
                          {product.description && (
                            <div className={styles.prodMeta}>
                              {product.description.slice(0, 40)}
                              {product.description.length > 40 ? "…" : ""}
                            </div>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className={styles.cellNum}>
                      {product.price.toLocaleString("ko-KR")}원
                    </td>
                    <td>
                      <div className={styles.stockWrap}>
                        <div className={styles.stockBar}>
                          <span
                            className={styles.stockFill}
                            style={{ width: `${ratio * 100}%`, background: color }}
                          />
                        </div>
                        <span style={{ fontSize: 13, fontWeight: 700, color }}>
                          {product.availableStock.toLocaleString()}
                        </span>
                      </div>
                    </td>
                    <td>
                      {product.active ? (
                        <span className={`${styles.badge} ${styles.badgeActive}`}>
                          <span className={styles.badgeDot} style={{ background: "#00b86b" }} />
                          활성
                        </span>
                      ) : (
                        <span className={`${styles.badge} ${styles.badgeInactive}`}>
                          <span className={styles.badgeDot} style={{ background: "#8b95a1" }} />
                          비활성
                        </span>
                      )}
                    </td>
                    <td>
                      <div className={styles.rowAct}>
                        <Link
                          href={`/admin/products/${product.id}/edit`}
                          className={styles.btnMini}
                        >
                          수정
                        </Link>
                        {product.active && (
                          <button
                            className={`${styles.btnMini} ${styles.btnMiniDanger}`}
                            onClick={() => handleDelete(product.id, product.name)}
                          >
                            비활성화
                          </button>
                        )}
                      </div>
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
