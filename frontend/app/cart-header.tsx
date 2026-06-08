"use client";

import { useState } from "react";
import CartDrawer from "./cart-drawer";

export default function CartHeader() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <button className="cartIconBtn" onClick={() => setIsOpen(true)}>
        🛒 장바구니
      </button>
      <CartDrawer isOpen={isOpen} onClose={() => setIsOpen(false)} />
    </>
  );
}
