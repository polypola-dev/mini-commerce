-- inventory 완전분리(GH #3 전략 c) — 재고 예약 원장이 inventorydb(inventory-api 소유)로 이전됐다.
-- order-api는 더 이상 inventory-core에 의존하지 않아 InventoryReservation 엔티티가 클래스패스에서
-- 사라졌고(Hibernate validate 대상 아님), orderdb의 이 테이블들은 아무 엔티티도 매핑하지 않는
-- 死데이터다. 로컬/CI 데이터는 이관 없이 폐기한다(FK가 order 테이블로 향하지 않음 — 안전).
-- inventory_reservation_lines가 inventory_reservations를 FK 참조하므로 자식부터 드롭한다.
DROP TABLE IF EXISTS public.inventory_reservation_lines;
DROP TABLE IF EXISTS public.inventory_reservations;
