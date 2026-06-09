/// <reference types="@testing-library/jest-dom" />
import React from "react";
import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import CartDrawer from "@/app/cart-drawer";
import * as api from "@/lib/api";

jest.mock("@/lib/api");
const mockApi = api as jest.Mocked<typeof api>;

const mockCart: api.Cart = {
  customerId: "customer-1",
  items: [
    {
      itemId: "item-1",
      productId: "prod-1",
      productName: "상품 A",
      quantity: 2,
      unitPrice: 10000,
      subtotal: 20000,
      selectedOptionValue: null,
    },
    {
      itemId: "item-2",
      productId: "prod-2",
      productName: "상품 B",
      quantity: 1,
      unitPrice: 15000,
      subtotal: 15000,
      selectedOptionValue: null,
    },
  ],
  totalAmount: 35000,
};

describe("CartDrawer", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockApi.getCart.mockResolvedValue({
      ...mockCart,
      items: mockCart.items.map((i) => ({ ...i })),
    });
  });

  describe("Bug #1: 아이템 삭제 후 totalAmount 재계산", () => {
    it("item-1 삭제 후 totalAmount가 남은 item-2의 subtotal(15000)로 갱신된다", async () => {
      mockApi.removeCartItem.mockResolvedValue(undefined);

      const { container } = render(
        <CartDrawer isOpen={true} onClose={() => {}} />
      );

      await waitFor(() => {
        expect(screen.getByText("상품 A")).toBeInTheDocument();
      });

      expect(screen.getByText("총 35,000원")).toBeInTheDocument();

      const deleteButtons = container.querySelectorAll(".cartDeleteBtn");
      await act(async () => {
        fireEvent.click(deleteButtons[0]);
      });

      await waitFor(() => {
        expect(screen.getByText("총 15,000원")).toBeInTheDocument();
      });
    });
  });

  describe("Bug #2: 주문 완료 메시지 표시", () => {
    it("주문 완료 후 장바구니가 비워져도 결제 완료 메시지가 표시된다", async () => {
      const mockOrder = {
        orderId: "order-123",
        status: "PAID",
        totalAmount: 35000,
        createdAt: "2026-06-09T00:00:00Z",
        lines: [],
        shippingRecipient: null,
        shippingPhone: null,
        shippingAddress: null,
        shippingDetailAddress: null,
        shippingZipCode: null,
      };
      mockApi.createOrder.mockResolvedValue(mockOrder);
      mockApi.completeFakePayment.mockResolvedValue(mockOrder);
      mockApi.clearCart.mockResolvedValue(undefined);

      render(<CartDrawer isOpen={true} onClose={() => {}} />);

      await waitFor(() => {
        expect(screen.getByText("상품 A")).toBeInTheDocument();
      });

      const orderButton = screen.getByText("주문하기");
      await act(async () => {
        fireEvent.click(orderButton);
      });

      await waitFor(() => {
        expect(screen.getByText("결제 완료: order-123")).toBeInTheDocument();
      });
    });
  });
});
