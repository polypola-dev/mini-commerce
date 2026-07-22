-- Flyway baseline (GH #11, GH #20, GH #22) — minicommerce DB.
-- shop-api가 소유하는 minicommerce 스키마의 통합 스냅샷이다. 정식 런칭 전(실사용자 없음)이라
-- 기존 증분 마이그레이션(V3~V6)을 이력 보존 없이 하나의 V1으로 통합 재작성했다.
--
-- GH #20(PK 전략 확장): 모든 PK/FK 성격 컬럼을 character varying(255)에서 native uuid로 전환한다.
--   값은 애플리케이션이 UUIDv7(UuidV7)로 생성한다. Hibernate 6는 java.util.UUID 필드 ↔ Postgres
--   uuid 컬럼을 별도 애너테이션 없이 기본 매핑하므로 ddl-auto:validate를 통과한다.
--   author_id/customer_id는 Supabase Auth user id(항상 UUID 형식)를 그대로 저장하므로 안전하다.
-- GH #22(상품 SKU): products에 사람이 읽는 SKU 컬럼(varchar, UNIQUE)을 추가한다.
--
-- event_publication은 Spring Modulith 아웃박스로 원래도 uuid이며 이 baseline이 소유한다.

CREATE TABLE public.carts (
    id uuid NOT NULL
);

CREATE TABLE public.cart_items (
    id uuid NOT NULL,
    added_at timestamp(6) with time zone,
    product_id uuid,
    product_name character varying(255),
    quantity integer NOT NULL,
    selected_option_id uuid,
    selected_option_value character varying(255),
    unit_price numeric(38,2),
    cart_id uuid NOT NULL
);

CREATE TABLE public.products (
    id uuid NOT NULL,
    active boolean NOT NULL,
    description character varying(255),
    image_url character varying(255),
    name character varying(255),
    price numeric(38,2),
    sku character varying(64) NOT NULL,
    stock bigint NOT NULL
);

CREATE TABLE public.product_options (
    id uuid NOT NULL,
    additional_price numeric(38,2),
    option_group_name character varying(255),
    option_value character varying(255),
    product_id uuid
);

CREATE TABLE public.reviews (
    id uuid NOT NULL,
    author_id uuid,
    content character varying(255),
    created_at timestamp(6) with time zone,
    product_id uuid,
    rating integer NOT NULL
);

CREATE TABLE public.notifications (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    customer_id uuid,
    message character varying(500),
    order_id uuid,
    sent_at timestamp(6) with time zone,
    status character varying(255),
    type character varying(255),
    CONSTRAINT notifications_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['ORDER_PLACED'::character varying, 'ORDER_PAID'::character varying, 'ORDER_CANCELED'::character varying])::text[])))
);

-- C3/C4 — 배송지 주소록. label(배송지명, 최대 10자 선택)·zip_code(신우편번호 5자리 선택)는
-- 원래도 문자열 컬럼이라 varchar 유지. id/customer_id만 uuid로 전환.
CREATE TABLE public.addresses (
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    recipient_name character varying(255),
    phone character varying(255),
    address1 character varying(255),
    address2 character varying(255),
    zip_code character varying(255),
    label character varying(255),
    is_default boolean NOT NULL,
    created_at timestamp(6) with time zone
);

-- C3/C4 — 위시리스트.
CREATE TABLE public.wishlist_items (
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    product_id uuid NOT NULL,
    created_at timestamp(6) with time zone
);

-- Spring Modulith 이벤트 아웃박스 (spring-modulith-starter-jpa).
CREATE TABLE public.event_publication (
    id uuid NOT NULL,
    completion_date timestamp(6) with time zone,
    event_type character varying(255),
    listener_id character varying(255),
    publication_date timestamp(6) with time zone,
    serialized_event character varying(255)
);

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);

-- GH #22 — SKU는 상품 식별 코드라 중복 불가.
ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_sku_key UNIQUE (sku);

ALTER TABLE ONLY public.product_options
    ADD CONSTRAINT product_options_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT addresses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.event_publication
    ADD CONSTRAINT event_publication_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fkpcttvuq4mxppo8sxggjtn5i2c FOREIGN KEY (cart_id) REFERENCES public.carts(id);

-- 한 사용자가 같은 상품을 중복 찜하지 못하도록 (서비스 멱등 가드의 DB 레벨 방어).
ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_customer_product_key UNIQUE (customer_id, product_id);

-- 소유자 스코프 조회가 주 접근 경로.
CREATE INDEX idx_addresses_customer_id ON public.addresses (customer_id);
CREATE INDEX idx_wishlist_items_customer_id ON public.wishlist_items (customer_id);
