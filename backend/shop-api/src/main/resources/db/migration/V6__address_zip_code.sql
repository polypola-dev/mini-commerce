-- C3 후속 — 배송지 우편번호(zip_code) 컬럼 추가. 우편번호 검색(카카오 우편번호 서비스)으로
-- 채우는 5자리 신우편번호, 선택. order.shippingZipCode와 정렬. 기존 행은 null.
-- Hibernate 매핑(AddressJpaEntity.zipCode, String)과 일치: character varying(255) nullable.

ALTER TABLE public.addresses ADD COLUMN zip_code character varying(255);
