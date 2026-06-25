import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mcPage">
      <div className="mcShell">
        <div className="mcNotFoundWrap">
          <Link href="/" className="mcBackBtn" aria-label="뒤로">
            <svg width="20" height="20" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="m13 5-7 7 7 7" />
            </svg>
          </Link>
          <div className="mcNotFoundBody">
            <div className="mcNotFoundCode">404</div>
            <div className="mcErrorTitle">페이지를 찾을 수 없어요</div>
            <p className="mcErrorDesc">
              주소가 변경되었거나
              <br />
              삭제된 페이지예요
            </p>
          </div>
          <div className="mcNotFoundCta">
            <Link href="/" className="mcBtn mcBtnPrimary">
              홈으로 가기
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
