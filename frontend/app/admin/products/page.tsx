"use client";

import { adminDeleteProduct, adminGetProducts, type Product } from "@/lib/api";
import Link from "next/link";
import { useEffect, useState } from "react";
import styles from "../admin.module.css";

const PAGE_SIZE_OPTIONS = [20, 50, 100];

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
  const [totalElements, setTotalElements] = useState(0);
  const [serverTotalPages, setServerTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [inputSearch, setInputSearch] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  async function load({
    currentPage = page,
    currentSize = pageSize,
    currentStatus = statusFilter,
    currentSearch = appliedSearch,
  }: {
    currentPage?: number;
    currentSize?: number;
    currentStatus?: string;
    currentSearch?: string;
  } = {}) {
    setLoading(true);
    try {
      const active =
        currentStatus === "ACTIVE" ? true : currentStatus === "OFF" ? false : undefined;
      const result = await adminGetProducts({
        page: currentPage - 1,
        size: currentSize,
        active,
        q: currentSearch || undefined,
      });
      setProducts(result.content);
      setTotalElements(result.totalElements);
      setServerTotalPages(result.totalPages);
    } finally {
      setLoading(false);
    }
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

  function applySearch() {
    setAppliedSearch(inputSearch);
    setPage(1);
    load({ currentSearch: inputSearch, currentPage: 1 });
  }

  function resetSearch() {
    setInputSearch("");
    setAppliedSearch("");
    setPage(1);
    load({ currentSearch: "", currentPage: 1 });
  }

  function handleSearchKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") applySearch();
  }

  function handleStatusFilterChange(key: string) {
    setStatusFilter(key);
    setPage(1);
    load({ currentStatus: key, currentPage: 1 });
  }

  function goToPage(newPage: number) {
    setPage(newPage);
    load({ currentPage: newPage });
  }

  const maxStock = Math.max(...products.map((p) => p.availableStock), 1);
  const safePage = Math.min(page, serverTotalPages);
  const startIdx = (safePage - 1) * pageSize;

  function buildPageButtons(): (number | "ellipsis-start" | "ellipsis-end")[] {
    const delta = 2;
    const range: number[] = [];
    for (let i = Math.max(1, safePage - delta); i <= Math.min(serverTotalPages, safePage + delta); i++) {
      range.push(i);
    }
    const result: (number | "ellipsis-start" | "ellipsis-end")[] = [];
    if (range[0] > 1) { result.push(1); if (range[0] > 2) result.push("ellipsis-start"); }
    range.forEach((n) => result.push(n));
    if (range[range.length - 1] < serverTotalPages) {
      if (range[range.length - 1] < serverTotalPages - 1) result.push("ellipsis-end");
      result.push(serverTotalPages);
    }
    return result;
  }

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
                value={inputSearch}
                onChange={(e) => setInputSearch(e.target.value)}
                onKeyDown={handleSearchKeyDown}
              />
            </div>
          </div>
          <div className={styles.filterField}>
            <span className={styles.filterLabel}>&nbsp;</span>
            <div style={{ display: "flex", gap: 6 }}>
              <button className={styles.btnPrimary} onClick={applySearch}>조회</button>
              <button className={styles.btnGhost} onClick={resetSearch}>초기화</button>
            </div>
          </div>
        </div>
        <div className={styles.chips}>
          {FILTER_CHIPS.map((chip) => (
            <button
              key={chip.key}
              className={`${styles.chip}${statusFilter === chip.key ? " " + styles.chipActive : ""}`}
              onClick={() => handleStatusFilterChange(chip.key)}
            >
              {chip.label}
            </button>
          ))}
          <span className={styles.chipCount}>
            총 <b style={{ color: "#191f28" }}>{totalElements}</b>개
          </span>
        </div>
      </div>

      {loading ? (
        <div className={styles.emptyState}>불러오는 중...</div>
      ) : products.length === 0 ? (
        <div className={styles.emptyState}>상품이 없습니다.</div>
      ) : (
        <div className={styles.tableWrap}>
          <div className={styles.tableTools}>
            <span>
              <span className={styles.tableToolsCount}>{totalElements}</span>개 중 {startIdx + 1}–{startIdx + products.length} 표시
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
              {products.map((product) => {
                const ratio = Math.min(product.availableStock / maxStock, 1);
                const color = stockColor(ratio);
                return (
                  <tr key={product.id}>
                    <td>
                      <div className={styles.prodCell}>
                        {product.imageUrl ? (
                          <img src={product.imageUrl} alt="" className={styles.prodThumbImg} />
                        ) : (
                          <div className={styles.prodThumb}>📦</div>
                        )}
                        <div>
                          <div className={styles.prodName}>{product.name}</div>
                          {product.description && (
                            <div className={styles.prodMeta}>
                              {product.description.slice(0, 40)}{product.description.length > 40 ? "…" : ""}
                            </div>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className={styles.cellNum}>{product.price.toLocaleString("ko-KR")}원</td>
                    <td>
                      <div className={styles.stockWrap}>
                        <div className={styles.stockBar}>
                          <span className={styles.stockFill} style={{ width: `${ratio * 100}%`, background: color }} />
                        </div>
                        <span style={{ fontSize: 13, fontWeight: 700, color }}>
                          {product.availableStock.toLocaleString()}
                        </span>
                      </div>
                    </td>
                    <td>
                      {product.active ? (
                        <span className={`${styles.badge} ${styles.badgeActive}`}>
                          <span className={styles.badgeDot} style={{ background: "#00b86b" }} />활성
                        </span>
                      ) : (
                        <span className={`${styles.badge} ${styles.badgeInactive}`}>
                          <span className={styles.badgeDot} style={{ background: "#8b95a1" }} />비활성
                        </span>
                      )}
                    </td>
                    <td>
                      <div className={styles.rowAct}>
                        <Link href={`/admin/products/${product.id}/edit`} className={styles.btnMini}>수정</Link>
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

          <div className={styles.pager}>
            <span className={styles.pagerInfo}>
              {totalElements}개 중 {startIdx + 1}–{startIdx + products.length} 표시
            </span>

            <button
              className={`${styles.pgBtn}${safePage === 1 ? " " + styles.pgBtnDisabled : ""}`}
              onClick={() => safePage > 1 && goToPage(safePage - 1)}
              disabled={safePage === 1}
              aria-label="이전 페이지"
            >
              ‹
            </button>

            {buildPageButtons().map((item, idx) =>
              item === "ellipsis-start" || item === "ellipsis-end" ? (
                <button key={`${item}-${idx}`} className={styles.pgBtn} style={{ cursor: "default" }} disabled>…</button>
              ) : (
                <button
                  key={item}
                  className={`${styles.pgBtn}${item === safePage ? " " + styles.pgBtnOn : ""}`}
                  onClick={() => goToPage(item)}
                >
                  {item}
                </button>
              )
            )}

            <button
              className={`${styles.pgBtn}${safePage === serverTotalPages ? " " + styles.pgBtnDisabled : ""}`}
              onClick={() => safePage < serverTotalPages && goToPage(safePage + 1)}
              disabled={safePage === serverTotalPages}
              aria-label="다음 페이지"
            >
              ›
            </button>

            <select
              className={styles.pageSizeSelect}
              value={pageSize}
              onChange={(e) => {
                const newSize = Number(e.target.value);
                setPageSize(newSize);
                setPage(1);
                load({ currentSize: newSize, currentPage: 1 });
              }}
            >
              {PAGE_SIZE_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}개씩</option>
              ))}
            </select>
          </div>
        </div>
      )}
    </div>
  );
}
