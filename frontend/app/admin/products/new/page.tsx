"use client";

import { adminCreateProduct, type AdminProductRequest, type ProductOptionInput } from "@/lib/api";
import { useRouter } from "next/navigation";
import { useState } from "react";
import styles from "../../admin.module.css";

type OptionRow = { optionGroupName: string; optionValue: string; additionalPrice: number };
const EMPTY_OPTION: OptionRow = { optionGroupName: "", optionValue: "", additionalPrice: 0 };

export default function NewProductPage() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [imgLoadError, setImgLoadError] = useState(false);

  const [hasOptions, setHasOptions] = useState(false);
  const [options, setOptions] = useState<OptionRow[]>([{ ...EMPTY_OPTION }]);

  function addOption() {
    setOptions((prev) => [...prev, { ...EMPTY_OPTION }]);
  }

  function removeOption(idx: number) {
    setOptions((prev) => prev.filter((_, i) => i !== idx));
  }

  function updateOption(idx: number, field: keyof OptionRow, value: string | number) {
    setOptions((prev) => prev.map((o, i) => (i === idx ? { ...o, [field]: value } : o)));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!imageUrl.trim()) {
      setError("상품 이미지 URL은 필수입니다.");
      return;
    }
    setPending(true);
    setError(null);
    try {
      const validOptions: ProductOptionInput[] = hasOptions
        ? options
            .filter((o) => o.optionGroupName.trim() && o.optionValue.trim())
            .map((o) => ({
              optionGroupName: o.optionGroupName.trim(),
              optionValue: o.optionValue.trim(),
              additionalPrice: o.additionalPrice,
            }))
        : [];
      const data: AdminProductRequest = {
        name: name.trim(),
        description: description.trim(),
        price: Number(price),
        stock: Number(stock),
        imageUrl: imageUrl.trim(),
        options: validOptions,
      };
      await adminCreateProduct(data);
      router.push("/admin/products");
    } catch (err) {
      setError(err instanceof Error ? err.message : "등록 실패");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className={styles.content}>
      <div className={styles.pageHead}>
        <div>
          <h1 className={styles.pageTitle}>상품 등록</h1>
          <p className={styles.pageSubtitle}>새 상품 정보를 입력하세요</p>
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 320px", gap: 16, alignItems: "start" }}>
          {/* 왼쪽: 기본 정보 + 옵션 */}
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>

            {/* 기본 정보 */}
            <div className={styles.formCard}>
              <div className={styles.formCardHead}>
                <h2 className={styles.formCardTitle}>기본 정보</h2>
              </div>
              <div className={styles.formCardBody}>
                <div className={styles.formField}>
                  <label className={styles.formLabel}>
                    상품명 <span className={styles.formRequired}>*</span>
                  </label>
                  <input
                    className={styles.formInput}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="예: 프리미엄 캐시미어 니트"
                    required
                  />
                </div>

                <div className={styles.formField}>
                  <label className={styles.formLabel}>
                    상품 설명 <span className={styles.formRequired}>*</span>
                  </label>
                  <textarea
                    className={styles.formTextarea}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="상품의 특징, 소재, 주의사항 등을 입력하세요"
                    required
                    rows={3}
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                  <div className={styles.formField}>
                    <label className={styles.formLabel}>
                      판매가 (원) <span className={styles.formRequired}>*</span>
                    </label>
                    <input
                      type="number"
                      className={styles.formInput}
                      value={price}
                      onChange={(e) => setPrice(e.target.value)}
                      placeholder="0"
                      min={1}
                      required
                    />
                  </div>
                  <div className={styles.formField}>
                    <label className={styles.formLabel}>
                      재고 수량 <span className={styles.formRequired}>*</span>
                    </label>
                    <input
                      type="number"
                      className={styles.formInput}
                      value={stock}
                      onChange={(e) => setStock(e.target.value)}
                      placeholder="0"
                      min={0}
                      required
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* 옵션 설정 */}
            <div className={styles.formCard}>
              <div className={styles.formCardHead}>
                <h2 className={styles.formCardTitle}>옵션 설정</h2>
              </div>
              <div className={styles.formCardBody}>
                <div className={styles.toggleWrap}>
                  <button
                    type="button"
                    className={`${styles.toggleOption}${!hasOptions ? " " + styles.toggleOptionOn : ""}`}
                    onClick={() => setHasOptions(false)}
                  >
                    단일 상품
                  </button>
                  <button
                    type="button"
                    className={`${styles.toggleOption}${hasOptions ? " " + styles.toggleOptionOn : ""}`}
                    onClick={() => {
                      setHasOptions(true);
                      if (options.length === 0) addOption();
                    }}
                  >
                    옵션 있음
                  </button>
                </div>

                {!hasOptions && (
                  <p className={styles.formHint}>
                    옵션 없이 단일 상품으로 판매합니다. 색상·사이즈 등 선택지가 필요하면 "옵션 있음"을 선택하세요.
                  </p>
                )}

                {hasOptions && (
                  <>
                    <div style={{ border: "1px solid #e5e8eb", borderRadius: 10, overflow: "hidden" }}>
                      <table className={styles.optionsTable}>
                        <thead>
                          <tr>
                            <th>옵션 그룹명</th>
                            <th>옵션 값</th>
                            <th style={{ width: 130 }}>추가 금액 (원)</th>
                            <th style={{ width: 44 }}></th>
                          </tr>
                        </thead>
                        <tbody>
                          {options.map((opt, idx) => (
                            <tr key={idx}>
                              <td>
                                <input
                                  className={styles.optionInput}
                                  value={opt.optionGroupName}
                                  onChange={(e) => updateOption(idx, "optionGroupName", e.target.value)}
                                  placeholder="예: 색상"
                                />
                              </td>
                              <td>
                                <input
                                  className={styles.optionInput}
                                  value={opt.optionValue}
                                  onChange={(e) => updateOption(idx, "optionValue", e.target.value)}
                                  placeholder="예: 블랙"
                                />
                              </td>
                              <td>
                                <input
                                  type="number"
                                  className={styles.optionInput}
                                  value={opt.additionalPrice}
                                  onChange={(e) => updateOption(idx, "additionalPrice", Number(e.target.value))}
                                  min={0}
                                  placeholder="0"
                                />
                              </td>
                              <td style={{ textAlign: "center" }}>
                                <button
                                  type="button"
                                  className={`${styles.btnMini} ${styles.btnMiniDanger}`}
                                  onClick={() => removeOption(idx)}
                                  title="삭제"
                                >
                                  ✕
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    <div>
                      <button type="button" className={styles.btnGhost} onClick={addOption}>
                        + 옵션 행 추가
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* 오른쪽: 이미지 */}
          <div className={styles.formCard}>
            <div className={styles.formCardHead}>
              <h2 className={styles.formCardTitle}>
                상품 이미지 <span className={styles.formRequired}>*</span>
              </h2>
            </div>
            <div className={styles.formCardBody}>
              <div className={styles.imgPreviewBox}>
                {imageUrl && !imgLoadError ? (
                  <img
                    src={imageUrl}
                    alt="미리보기"
                    onError={() => setImgLoadError(true)}
                    onLoad={() => setImgLoadError(false)}
                  />
                ) : (
                  <div className={styles.imgPlaceholder}>
                    <span className={styles.imgPlaceholderIcon}>🖼️</span>
                    <span className={styles.imgPlaceholderText}>이미지 미리보기</span>
                  </div>
                )}
              </div>

              <div className={styles.formField}>
                <label className={styles.formLabel}>
                  이미지 URL <span className={styles.formRequired}>*</span>
                </label>
                <input
                  type="url"
                  className={styles.formInput}
                  value={imageUrl}
                  onChange={(e) => {
                    setImageUrl(e.target.value);
                    setImgLoadError(false);
                  }}
                  placeholder="https://..."
                  required
                />
                {imgLoadError && (
                  <p className={`${styles.formHint} ${styles.formHintError}`}>
                    이미지를 불러올 수 없습니다. URL을 확인해 주세요.
                  </p>
                )}
              </div>

              <div className={styles.imgGuide}>
                <p className={styles.imgGuideTitle}>이미지 가이드</p>
                <ul className={styles.imgGuideList}>
                  <li>지원 형식: <b>JPG, PNG, WebP</b></li>
                  <li>권장 비율: <b>4:3</b> (예: 800×600 px)</li>
                  <li>최대 용량: <b>5 MB</b></li>
                  <li>최소 해상도: 400 px 이상</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        {error && <div className={styles.formError} style={{ marginTop: 16 }}>{error}</div>}

        <div className={styles.formActions} style={{ marginTop: 16 }}>
          <button type="submit" disabled={pending} className={styles.btnPrimary}>
            {pending ? "등록 중..." : "상품 등록"}
          </button>
          <button type="button" onClick={() => router.back()} className={styles.btnGhost}>
            취소
          </button>
        </div>
      </form>
    </div>
  );
}
