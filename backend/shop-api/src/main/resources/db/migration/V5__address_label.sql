-- C3 후속 — 배송지명(label) 컬럼 추가. 집/회사/학교 등 사용자 지정 라벨(최대 10자, 선택).
-- 기존 행은 null로 남고, 프론트에서 기본 표기("배송지")로 대체한다.
-- Hibernate 매핑(AddressJpaEntity.label, String)과 일치: character varying(255) nullable.

ALTER TABLE public.addresses ADD COLUMN label character varying(255);
