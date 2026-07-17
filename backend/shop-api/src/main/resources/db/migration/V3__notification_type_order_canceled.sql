ALTER TABLE public.notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check
    CHECK (((type)::text = ANY ((ARRAY['ORDER_PLACED'::character varying, 'ORDER_PAID'::character varying, 'ORDER_CANCELED'::character varying])::text[])));
