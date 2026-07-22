-- 로컬 개발 편의용 더미 데이터. spring.profiles.active=local일 때만
-- spring.flyway.locations에 이 디렉토리(db/seed)가 포함되어 적용된다
-- (application-local.yml 참고). prod(Render)는 profile 미설정으로 제외된다.
--
-- GH #20: id/product_id가 uuid로 전환되어 고정 UUID를 사용한다(재적용 멱등을 위해 상수).
-- GH #22: products.sku(사람이 읽는 상품코드, NOT NULL UNIQUE)를 채운다.

INSERT INTO public.products (id, active, description, image_url, name, price, sku, stock) VALUES
    ('00000000-0000-7000-8000-000000000001', true, '조용한 타건감의 업무용 키보드', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=900&q=80', 'Low Profile Keyboard', 129000, 'KB-LP-001', 100),
    ('00000000-0000-7000-8000-000000000002', true, '장시간 착용 가능한 모니터링 헤드폰', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 'Studio Headphones', 89000, 'HP-ST-001', 80),
    ('00000000-0000-7000-8000-000000000003', true, '밝기 조절이 쉬운 알루미늄 데스크 램프', 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80', 'Desk Lamp', 64000, 'LM-DK-001', 50)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.product_options (id, additional_price, option_group_name, option_value, product_id) VALUES
    ('00000000-0000-7000-8000-0000000000a1', 0, '색상', '블랙', '00000000-0000-7000-8000-000000000001'),
    ('00000000-0000-7000-8000-0000000000a2', 10000, '색상', '화이트', '00000000-0000-7000-8000-000000000001'),
    ('00000000-0000-7000-8000-0000000000b1', 0, '모델', 'Standard', '00000000-0000-7000-8000-000000000002'),
    ('00000000-0000-7000-8000-0000000000b2', 30000, '모델', 'Pro', '00000000-0000-7000-8000-000000000002'),
    ('00000000-0000-7000-8000-0000000000c1', 0, '색온도', '웜', '00000000-0000-7000-8000-000000000003'),
    ('00000000-0000-7000-8000-0000000000c2', 5000, '색온도', '쿨', '00000000-0000-7000-8000-000000000003')
ON CONFLICT (id) DO NOTHING;
