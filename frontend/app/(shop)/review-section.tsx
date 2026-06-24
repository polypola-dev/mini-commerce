"use client";

import { useEffect, useState } from "react";
import { createReview, deleteReview, getReviews, type Review, type ReviewListResponse } from "@/lib/api";

type Props = {
  productId: string;
  currentUserId?: string;
};

function StarRating({ value, onChange }: { value: number; onChange?: (v: number) => void }) {
  return (
    <span className="starRating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          className={`star ${n <= value ? "star--filled" : ""}`}
          onClick={() => onChange?.(n)}
          aria-label={`${n}점`}
        >
          ★
        </button>
      ))}
    </span>
  );
}

export default function ReviewSection({ productId, currentUserId }: Props) {
  const [open, setOpen] = useState(false);
  const [data, setData] = useState<ReviewListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!open || data) return;
    setLoading(true);
    getReviews(productId)
      .then(setData)
      .catch(() => setMessage("리뷰를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [open, productId, data]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;
    setSubmitting(true);
    setMessage(null);
    try {
      const review = await createReview({ productId, rating, content });
      setData((prev) =>
        prev
          ? {
              reviews: [review, ...prev.reviews],
              totalCount: prev.totalCount + 1,
              averageRating: parseFloat(
                ((prev.averageRating * prev.totalCount + rating) / (prev.totalCount + 1)).toFixed(1)
              ),
            }
          : null
      );
      setContent("");
      setRating(5);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "리뷰 작성 실패");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(review: Review) {
    try {
      await deleteReview(review.id);
      setData((prev) => {
        if (!prev) return prev;
        const remaining = prev.reviews.filter((r) => r.id !== review.id);
        const avg =
          remaining.length === 0
            ? 0
            : parseFloat(
                (remaining.reduce((s, r) => s + r.rating, 0) / remaining.length).toFixed(1)
              );
        return { reviews: remaining, totalCount: remaining.length, averageRating: avg };
      });
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "삭제 실패");
    }
  }

  return (
    <div className="reviewSection">
      <button
        className="reviewToggle"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
      >
        {open ? "리뷰 접기" : `리뷰 보기${data ? ` (${data.totalCount})` : ""}`}
      </button>

      {open && (
        <div className="reviewBody">
          {loading && <p className="reviewLoading">불러오는 중…</p>}

          {data && data.totalCount > 0 && (
            <p className="reviewAvg">
              평균 ★ {data.averageRating} &nbsp;({data.totalCount}개)
            </p>
          )}

          {currentUserId && (
            <form className="reviewForm" onSubmit={handleSubmit}>
              <StarRating value={rating} onChange={setRating} />
              <textarea
                className="reviewTextarea"
                placeholder="리뷰를 작성하세요 (최대 500자)"
                maxLength={500}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={3}
                required
              />
              <button type="submit" disabled={submitting} className="reviewSubmit">
                {submitting ? "등록 중…" : "리뷰 등록"}
              </button>
            </form>
          )}

          {!currentUserId && <p className="reviewHint">로그인하면 리뷰를 작성할 수 있습니다.</p>}

          {message && <p className="message">{message}</p>}

          <ul className="reviewList">
            {data?.reviews.map((review) => (
              <li key={review.id} className="reviewItem">
                <div className="reviewItemHeader">
                  <span className="reviewStars">{"★".repeat(review.rating)}{"☆".repeat(5 - review.rating)}</span>
                  <span className="reviewDate">
                    {new Date(review.createdAt).toLocaleDateString("ko-KR")}
                  </span>
                  {currentUserId === review.authorId && (
                    <button
                      className="reviewDelete"
                      onClick={() => handleDelete(review)}
                    >
                      삭제
                    </button>
                  )}
                </div>
                <p className="reviewContent">{review.content}</p>
              </li>
            ))}
          </ul>

          {data && data.reviews.length === 0 && !loading && (
            <p className="reviewEmpty">아직 리뷰가 없습니다.</p>
          )}
        </div>
      )}
    </div>
  );
}
