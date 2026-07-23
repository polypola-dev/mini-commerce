-- GH #19: 고객 노출용 표시 전용 주문번호(ORD-YYYYMMDD-NNNN) + 일별 리셋 채번 카운터.
--
-- orders.order_number: PK(uuid)와 분리된 표시 전용 번호. 조회/인증 키로는 쓰지 않는다(표시 전용).
--   nullable + UNIQUE: 백필하지 않은 과거(dev) 주문은 null로 남고, 신규 주문만 값을 갖는다.
--   Postgres의 UNIQUE는 NULL을 서로 다른 값으로 취급하므로 다건 null이 허용된다.
--
-- order_number_sequences: (KST) 날짜별 마지막 발급 일련번호. 채번은 이 행을 SELECT ... FOR UPDATE로
--   잠근 뒤 last_seq를 증가시켜 동시 주문에도 중복/스킵이 없게 한다(주문 저장과 같은 트랜잭션).

ALTER TABLE public.orders ADD COLUMN order_number character varying(32);

ALTER TABLE public.orders
    ADD CONSTRAINT orders_order_number_key UNIQUE (order_number);

CREATE TABLE public.order_number_sequences (
    order_date date NOT NULL,
    last_seq bigint NOT NULL DEFAULT 0,
    CONSTRAINT order_number_sequences_pkey PRIMARY KEY (order_date)
);
