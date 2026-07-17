ALTER TABLE public.inventory_reservations DROP CONSTRAINT inventory_reservations_status_check;
ALTER TABLE public.inventory_reservations ADD CONSTRAINT inventory_reservations_status_check
    CHECK (((status)::text = ANY ((ARRAY['RESERVED'::character varying, 'CONFIRMED'::character varying, 'RELEASED'::character varying, 'EXPIRED'::character varying, 'RESTOCKED'::character varying])::text[])));
