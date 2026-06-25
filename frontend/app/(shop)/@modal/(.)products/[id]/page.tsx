import ProductDetailBody from "@/app/(shop)/products/[id]/product-detail-body";
import ProductDetailModal from "../../product-detail-modal";
import ModalBackButton from "../../modal-back-button";

export default async function InterceptedProductModalPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return (
    <ProductDetailModal>
      <ProductDetailBody id={id} backButton={<ModalBackButton />} />
    </ProductDetailModal>
  );
}
