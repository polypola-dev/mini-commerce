-- 로컬 개발 편의용 더미 데이터. spring.profiles.active=local일 때만
-- spring.flyway.locations에 이 디렉토리(db/seed)가 포함되어 적용된다
-- (application-local.yml 참고). prod(Render)는 profile 미설정으로 제외된다.

INSERT INTO public.products (id, active, description, image_url, name, price, stock) VALUES
    ('sku-keyboard', true, '조용한 타건감의 업무용 키보드', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=900&q=80', 'Low Profile Keyboard', 129000, 100),
    ('sku-headphones', true, '장시간 착용 가능한 모니터링 헤드폰', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 'Studio Headphones', 89000, 80),
    ('sku-lamp', true, '밝기 조절이 쉬운 알루미늄 데스크 램프', 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80', 'Desk Lamp', 64000, 50)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.product_options (id, additional_price, option_group_name, option_value, product_id) VALUES
    ('option-keyboard-black', 0, '색상', '블랙', 'sku-keyboard'),
    ('option-keyboard-white', 10000, '색상', '화이트', 'sku-keyboard'),
    ('option-headphones-std', 0, '모델', 'Standard', 'sku-headphones'),
    ('option-headphones-pro', 30000, '모델', 'Pro', 'sku-headphones'),
    ('option-lamp-warm', 0, '색온도', '웜', 'sku-lamp'),
    ('option-lamp-cool', 5000, '색온도', '쿨', 'sku-lamp')
ON CONFLICT (id) DO NOTHING;
