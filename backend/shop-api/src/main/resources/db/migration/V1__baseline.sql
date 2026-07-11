-- Flyway baseline (GH #11) — minicommerce DB.
-- shop-api가 소유하는 minicommerce 스키마의 현행 스냅샷이다. 이전에는 Hibernate ddl-auto:update가
-- 생성하던 스키마로, 클린 볼륨에 ddl-auto:update로 띄운 실 DB를 pg_dump하여 추출했다(타입/제약이
-- Hibernate 매핑과 정확히 일치 → ddl-auto:validate 통과 보장). event_publication은 Spring Modulith
-- 아웃박스 테이블로, 이제 Modulith 자체 초기화 대신 이 baseline이 소유한다.

CREATE TABLE public.carts (
    id character varying(255) NOT NULL
);

CREATE TABLE public.cart_items (
    id character varying(255) NOT NULL,
    added_at timestamp(6) with time zone,
    product_id character varying(255),
    product_name character varying(255),
    quantity integer NOT NULL,
    selected_option_id character varying(255),
    selected_option_value character varying(255),
    unit_price numeric(38,2),
    cart_id character varying(255) NOT NULL
);

CREATE TABLE public.products (
    id character varying(255) NOT NULL,
    active boolean NOT NULL,
    description character varying(255),
    image_url character varying(255),
    name character varying(255),
    price numeric(38,2),
    stock bigint NOT NULL
);

CREATE TABLE public.product_options (
    id character varying(255) NOT NULL,
    additional_price numeric(38,2),
    option_group_name character varying(255),
    option_value character varying(255),
    product_id character varying(255)
);

CREATE TABLE public.reviews (
    id character varying(255) NOT NULL,
    author_id character varying(255),
    content character varying(255),
    created_at timestamp(6) with time zone,
    product_id character varying(255),
    rating integer NOT NULL
);

CREATE TABLE public.notifications (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    customer_id character varying(255),
    message character varying(500),
    order_id character varying(255),
    sent_at timestamp(6) with time zone,
    status character varying(255),
    type character varying(255),
    CONSTRAINT notifications_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['ORDER_PLACED'::character varying, 'ORDER_PAID'::character varying])::text[])))
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

ALTER TABLE ONLY public.product_options
    ADD CONSTRAINT product_options_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.event_publication
    ADD CONSTRAINT event_publication_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fkpcttvuq4mxppo8sxggjtn5i2c FOREIGN KEY (cart_id) REFERENCES public.carts(id);
