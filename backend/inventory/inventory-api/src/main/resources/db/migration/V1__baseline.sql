-- inventorydb 베이스라인(GH #3 전략 c). 타입/제약은 orderdb V1(pg_dump 추출본)과 동일 형식을
-- 유지해 ddl-auto:validate 통과를 보장한다. orderdb와 달리 신규 DB라 데이터 이관은 없다
-- (기존 로컬 orderdb의 inventory_reservations는 폐기 — FK 없음 확인, 계획 문서 참조).
-- status CHECK는 orderdb V3(RESTOCKED 추가)까지 반영된 최종본이다.
-- unique(order_id)는 예약 ID=orderId 멱등 설계(S3)의 DB 측 가드다 — 1주문 1예약.

CREATE TABLE public.inventory_reservations (
    id uuid NOT NULL,
    expires_at timestamp(6) with time zone,
    order_id uuid,
    status character varying(255),
    CONSTRAINT inventory_reservations_status_check CHECK (((status)::text = ANY ((ARRAY['RESERVED'::character varying, 'CONFIRMED'::character varying, 'RELEASED'::character varying, 'EXPIRED'::character varying, 'RESTOCKED'::character varying])::text[])))
);

CREATE TABLE public.inventory_reservation_lines (
    inventory_reservation_id uuid NOT NULL,
    product_id uuid,
    quantity bigint
);

ALTER TABLE ONLY public.inventory_reservations
    ADD CONSTRAINT inventory_reservations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inventory_reservations
    ADD CONSTRAINT uq_inventory_reservations_order_id UNIQUE (order_id);

ALTER TABLE ONLY public.inventory_reservation_lines
    ADD CONSTRAINT fk_inventory_reservation_lines_reservation FOREIGN KEY (inventory_reservation_id) REFERENCES public.inventory_reservations(id);

-- 리퍼(ExpiredReservationReleaser)의 findByStatusAndExpiresAtBefore 조회 경로.
CREATE INDEX ix_inventory_reservations_status_expires ON public.inventory_reservations (status, expires_at);

-- Spring Modulith 이벤트 아웃박스(S3에서 inventory.reservation.expired 발행에 사용).
-- orderdb V1과 동일 형식 — JPA 엔티티가 아니라 validate 대상은 아니다.
CREATE TABLE public.event_publication (
    id uuid NOT NULL,
    completion_date timestamp(6) with time zone,
    event_type character varying(255),
    listener_id character varying(255),
    publication_date timestamp(6) with time zone,
    serialized_event character varying(255)
);

ALTER TABLE ONLY public.event_publication
    ADD CONSTRAINT event_publication_pkey PRIMARY KEY (id);
