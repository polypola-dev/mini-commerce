import { getProducts } from "@/lib/api";
import WishlistView from "./wishlist-view";

// 서버 컴포넌트에서 상품을 로드한다(홈과 동일 패턴). 찜 여부 필터는 클라이언트(WishlistView)에서.
export default async function WishlistPage() {
  const res = await getProducts({ page: 0, size: 100 });
  return <WishlistView products={res.content} />;
}
