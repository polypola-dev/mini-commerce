-- C3/C4 — 배송지 주소록·위시리스트 백엔드 저장 (minicommerce DB, shop-api 소유).
-- 컬럼 타입/nullable은 Hibernate 매핑(AddressJpaEntity/WishlistJpaEntity)과 정확히 일치시켜
-- ddl-auto:validate 통과를 보장한다. String→character varying(255), boolean→boolean,
-- Instant→timestamp(6) with time zone.

CREATE TABLE public.addresses (
    id character varying(255) NOT NULL,
    customer_id character varying(255) NOT NULL,
    recipient_name character varying(255),
    phone character varying(255),
    address1 character varying(255),
    address2 character varying(255),
    is_default boolean NOT NULL,
    created_at timestamp(6) with time zone
);

CREATE TABLE public.wishlist_items (
    id character varying(255) NOT NULL,
    customer_id character varying(255) NOT NULL,
    product_id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone
);

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT addresses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_pkey PRIMARY KEY (id);

-- 한 사용자가 같은 상품을 중복 찜하지 못하도록 (서비스 멱등 가드의 DB 레벨 방어).
ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_customer_product_key UNIQUE (customer_id, product_id);

-- 소유자 스코프 조회가 주 접근 경로.
CREATE INDEX idx_addresses_customer_id ON public.addresses (customer_id);
CREATE INDEX idx_wishlist_items_customer_id ON public.wishlist_items (customer_id);
