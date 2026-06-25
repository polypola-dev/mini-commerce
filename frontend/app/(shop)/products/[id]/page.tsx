import Link from "next/link";
import ProductDetailBody from "./product-detail-body";

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return (
    <ProductDetailBody
      id={id}
      backButton={
        <Link href="/" className="mcBackBtn" aria-label="뒤로">
          <svg width="20" height="20" fill="none" stroke="#222" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="m13 5-7 7 7 7" />
          </svg>
        </Link>
      }
    />
  );
}
