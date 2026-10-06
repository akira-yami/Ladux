-- V38 originally contained demo catalog mutations in production Flyway migrations.
-- Preserve existing dev seed behavior but run it ONLY when all 12 known demo
-- products are present with their expected IDs and slugs. This prevents foreign
-- key failures on a new empty production DB and avoids rewriting live products.
--
-- V38 checksum changed: operators must review applied Flyway history before
-- reconciling an existing database (see production-hardening documentation).

DO $$
BEGIN
    IF (
        SELECT COUNT(*)
        FROM (VALUES
                (1, 'laptop-apple-macbook-air-m3-13'),
                (2, 'laptop-gaming-lenovo-legion-5-pro-16'),
                (3, 'laptop-do-hoa-dell-precision-5680'),
                (4, 'laptop-van-phong-hp-probook-450-g10'),
                (5, 'laptop-gaming-asus-tuf-gaming-f15'),
                (6, 'laptop-sinh-vien-acer-aspire-7-a715'),
                (7, 'workstation-msi-creator-z16'),
                (8, 'gaming-cao-cap-razer-blade-16'),
                (9, 'ultrabook-samsung-galaxy-book-4'),
                (10, 'msi-thin-15-b13uc'),
                (11, 'laptop-doanh-nhan-microsoft-surface-laptop-6'),
                (12, 'acer-aspire-5')
        ) AS demo(id, slug)
        JOIN products p ON p.id = demo.id AND p.slug = demo.slug
    ) = 12 THEN
        -- 1. Insert standard color options if they do not exist
        INSERT INTO colors (name, hex_code)
        VALUES 
            ('Space Black', '#1D1D1F'),
            ('Silver', '#E3E4E5'),
            ('Space Gray', '#53555B'),
            ('Midnight Green', '#1B2824'),
            ('Lunar Light', '#F0F0F0'),
            ('Shadow Black', '#222222'),
            ('Platinum', '#D9D9D9'),
            ('Cosmic Gray', '#4F5052')
        ON CONFLICT (name) DO UPDATE SET hex_code = EXCLUDED.hex_code;
        
        -- 2. Update Product Specifications, Screen Size (Display) & Detailed Descriptions
        UPDATE products
        SET 
            description = 'Apple MacBook Air 13 M3 hội tụ thiết kế nhôm nguyên khối siêu mỏng nhẹ 1.24kg, màn hình Liquid Retina 13.6 inch sắc nét và vi xử lý M3 8 nhân CPU 10 nhân GPU mạnh mẽ. Hỗ trợ Wi-Fi 6E, thời lượng pin lên đến 18 giờ liên tục, lý tưởng cho sinh viên và dân văn phòng cao cấp.',
            display = '13.6 inch Liquid Retina (2560 x 1664), 500 nits, P3 Wide Color, True Tone',
            cpu = 'Apple M3 8-Core CPU',
            gpu = 'Apple M3 10-Core GPU',
            battery = '52.6 Wh Lithium-polymer (lên tới 18 giờ)',
            weight = '1.24 kg',
            os = 'macOS Sonoma',
            number_of_fans = 0
        WHERE id = 1 OR slug = 'laptop-apple-macbook-air-m3-13';
        
        UPDATE products
        SET 
            description = 'Lenovo Legion 5 Pro 16 là chiến hạm gaming với màn hình 16.0 inch WQXGA (2560 x 1600) 165Hz IPS 100% sRGB, kết hợp bộ vi xử lý AMD Ryzen 7 7745HX và đồ họa NVIDIA GeForce RTX 4070 8GB GDDR6. Tản nhiệt Legion Coldfront 5.0 tối ưu công suất tối đa cho các tựa game AAA.',
            display = '16.0 inch WQXGA (2560 x 1600) 165Hz IPS, 500 nits, 100% sRGB, G-SYNC',
            cpu = 'AMD Ryzen 7 7745HX',
            gpu = 'NVIDIA GeForce RTX 4070 8GB GDDR6',
            battery = '4-cell 80 Wh',
            weight = '2.55 kg',
            os = 'Windows 11 Home',
            number_of_fans = 2
        WHERE id = 2 OR slug = 'laptop-gaming-lenovo-legion-5-pro-16';
        
        UPDATE products
        SET 
            description = 'Dell Precision 5680 là máy trạm đồ họa chuyên nghiệp sở hữu màn hình 16.0 inch UHD+ OLED Touch tràn viền, vi xử lý Intel Core i9-13900H vPro cùng đồ họa NVIDIA RTX 3500 Ada Generation. Thiết kế vỏ nhôm cao cấp chuẩn độ bền quân đội MIL-STD 810H.',
            display = '16.0 inch UHD+ (3840 x 2400) OLED Touch, 400 nits, 100% DCI-P3',
            cpu = 'Intel Core i9-13900H vPro',
            gpu = 'NVIDIA RTX 3500 Ada Generation 12GB GDDR6',
            battery = '6-cell 100 Wh',
            weight = '1.91 kg',
            os = 'Windows 11 Pro',
            number_of_fans = 2
        WHERE id = 3 OR slug = 'laptop-do-hoa-dell-precision-5680';
        
        UPDATE products
        SET 
            description = 'HP ProBook 450 G10 là sự lựa chọn hoàn hảo cho doanh nhân và nhân viên văn phòng năng động. Màn hình 15.6 inch FHD IPS chống chói, chip Intel Core i7-1355U tiết kiệm điện, bàn phím gõ êm chống tràn nước và tích hợp bảo mật vân tay HP Wolf Security.',
            display = '15.6 inch FHD (1920 x 1080) IPS, 250 nits, Anti-Glare',
            cpu = 'Intel Core i7-1355U',
            gpu = 'Intel Iris Xe Graphics',
            battery = '3-cell 51 Wh',
            weight = '1.79 kg',
            os = 'Windows 11 Home',
            number_of_fans = 1
        WHERE id = 4 OR slug = 'laptop-van-phong-hp-probook-450-g10';
        
        UPDATE products
        SET 
            description = 'ASUS TUF Gaming F15 mang chuẩn độ bền quân đội Mỹ MIL-STD-810H. Được trang bị màn hình 15.6 inch FHD 144Hz IPS, CPU Intel Core i7-12700H và cạc đồ họa NVIDIA GeForce RTX 4060 8GB TGP 140W cùng công nghệ MUX Switch giúp tối đa hóa FPS trong game.',
            display = '15.6 inch FHD (1920 x 1080) 144Hz IPS, G-SYNC, Anti-Glare',
            cpu = 'Intel Core i7-12700H',
            gpu = 'NVIDIA GeForce RTX 4060 8GB GDDR6 (140W)',
            battery = '4-cell 90 Wh',
            weight = '2.20 kg',
            os = 'Windows 11 Home',
            number_of_fans = 2
        WHERE id = 5 OR slug = 'laptop-gaming-asus-tuf-gaming-f15';
        
        UPDATE products
        SET 
            description = 'Acer Aspire 7 A715 sở hữu thiết kế tối giản nhưng mang sức mạnh gaming tầm trung với AMD Ryzen 5 5625U và GPU GTX 1650 / RTX 2050 4GB. Màn hình 15.6 inch FHD 144Hz mượt mà, hệ thống 2 quạt tản nhiệt độc lập giúp máy luôn mát mẻ khi làm việc và giải trí.',
            display = '15.6 inch FHD (1920 x 1080) 144Hz IPS, Acer ComfyView',
            cpu = 'AMD Ryzen 5 5625U',
            gpu = 'NVIDIA GeForce RTX 2050 4GB GDDR6',
            battery = '3-cell 50 Wh',
            weight = '2.10 kg',
            os = 'Windows 11 Home',
            number_of_fans = 2
        WHERE id = 6 OR slug = 'laptop-sinh-vien-acer-aspire-7-a715';
        
        UPDATE products
        SET 
            description = 'MSI Creator Z16 là tuyệt tác sáng tạo nội dung với màn hình 16.0 inch QHD+ (2560 x 1600) 120Hz IPS Touch độ phủ màu 100% DCI-P3 (Delta-E < 2). Sức mạnh đến từ CPU Intel Core i7-12700H, RAM 32GB và cạc đồ họa NVIDIA GeForce RTX 3060 6GB.',
            display = '16.0 inch QHD+ (2560 x 1600) 120Hz IPS Touch, 100% DCI-P3, Delta-E < 2',
            cpu = 'Intel Core i7-12700H',
            gpu = 'NVIDIA GeForce RTX 3060 6GB GDDR6',
            battery = '4-cell 90 Wh',
            weight = '2.29 kg',
            os = 'Windows 11 Home',
            number_of_fans = 3
        WHERE id = 7 OR slug = 'workstation-msi-creator-z16';
        
        UPDATE products
        SET 
            description = 'Razer Blade 16 là đỉnh cao laptop gaming với màn hình Dual-Mode Mini-LED 16.0 inch có thể chuyển đổi linh hoạt giữa UHD+ 120Hz và FHD+ 240Hz. Trang bị CPU Intel Core i9-13950HX, GPU NVIDIA GeForce RTX 4080 12GB và khung nhôm CNC nguyên khối xa xỉ.',
            display = '16.0 inch Dual-Mode Mini-LED (UHD+ 120Hz / FHD+ 240Hz), 1000 nits, DCI-P3 100%',
            cpu = 'Intel Core i9-13950HX',
            gpu = 'NVIDIA GeForce RTX 4080 12GB GDDR6',
            battery = '4-cell 95.2 Wh',
            weight = '2.45 kg',
            os = 'Windows 11 Home',
            number_of_fans = 2
        WHERE id = 8 OR slug = 'gaming-cao-cap-razer-blade-16';
        
        UPDATE products
        SET 
            description = 'Samsung Galaxy Book 4 Pro đại diện cho dòng Ultrabook cao cấp thế hệ mới với màn hình 14.0 inch Dynamic AMOLED 2X 3K (2880 x 1800) 120Hz cảm ứng sắc nét. Bộ vi xử lý Intel Core Ultra 7 155H tích hợp NPU AI và thiết kế siêu mỏng 11.6mm nhẹ 1.23kg.',
            display = '14.0 inch 3K (2880 x 1800) Dynamic AMOLED 2X Touch 120Hz, 120% DCI-P3',
            cpu = 'Intel Core Ultra 7 155H (AI NPU)',
            gpu = 'Intel Arc Graphics',
            battery = '4-cell 63 Wh',
            weight = '1.23 kg',
            os = 'Windows 11 Home',
            number_of_fans = 2
        WHERE id = 9 OR slug = 'ultrabook-samsung-galaxy-book-4';
        
        UPDATE products
        SET 
            description = 'MSI Thin 15 B13UC là chiếc laptop gaming mỏng nhẹ giá hợp lý, vỏ phay xước cá tính nặng chỉ 1.86kg. Màn hình 15.6 inch FHD 144Hz mượt mà, CPU Intel Core i5-13420H cùng cạc đồ họa NVIDIA GeForce RTX 3050 4GB chiến tốt các tựa game eSports thông dụng.',
            display = '15.6 inch FHD (1920 x 1080) 144Hz IPS, Anti-Glare',
            cpu = 'Intel Core i5-13420H',
            gpu = 'NVIDIA GeForce RTX 3050 4GB GDDR6',
            battery = '3-cell 52.4 Wh',
            weight = '1.86 kg',
            os = 'Windows 11 Home',
            number_of_fans = 1
        WHERE id = 10 OR slug = 'msi-thin-15-b13uc';
        
        UPDATE products
        SET 
            description = 'Microsoft Surface Laptop 6 dành cho doanh nhân cao cấp với màn hình 13.5 inch PixelSense Touch (2256 x 1504) tỷ lệ 3:2 tối ưu làm việc. Trang bị chip Intel Core Ultra 5 135H vPro, vỏ nhôm nguyên khối sang trọng, bảo mật khuôn mặt Windows Hello.',
            display = '13.5 inch PixelSense Touch (2256 x 1504), 400 nits, Gorilla Glass 5',
            cpu = 'Intel Core Ultra 5 135H vPro',
            gpu = 'Intel Graphics',
            battery = '47 Wh (lên tới 18.5 giờ sử dụng)',
            weight = '1.38 kg',
            os = 'Windows 11 Pro',
            number_of_fans = 1
        WHERE id = 11 OR slug = 'laptop-doanh-nhan-microsoft-surface-laptop-6';
        
        UPDATE products
        SET 
            description = 'Acer Aspire 5 là dòng laptop phổ thông cân bằng tốt giữa hiệu năng và chi phí. Trang bị màn hình 15.6 inch FHD IPS sắc nét, vi xử lý Intel Core i7-1355U 10 nhân 12 luồng, RAM 16GB / SSD 512GB đáp ứng mượt mà học tập, văn phòng và giải trí hằng ngày.',
            display = '15.6 inch FHD (1920 x 1080) IPS, Acer ComfyView',
            cpu = 'Intel Core i7-1355U',
            gpu = 'Intel Iris Xe Graphics',
            battery = '3-cell 50 Wh',
            weight = '1.70 kg',
            os = 'Windows 11 Home',
            number_of_fans = 1
        WHERE id = 12 OR slug = 'acer-aspire-5';
        
        -- Fallback update for any other products without specs/descriptions
        UPDATE products
        SET 
            display = COALESCE(NULLIF(display, ''), '15.6 inch Full HD IPS (1920 x 1080)'),
            description = COALESCE(NULLIF(description, ''), 'Thông tin mô tả sản phẩm đang được cập nhật chi tiết.'),
            cpu = COALESCE(NULLIF(cpu, ''), 'Intel Core i7 Gen 13th / AMD Ryzen 7'),
            gpu = COALESCE(NULLIF(gpu, ''), 'NVIDIA GeForce RTX Graphics'),
            battery = COALESCE(NULLIF(battery, ''), '3-cell 54 Wh'),
            weight = COALESCE(NULLIF(weight, ''), '1.80 kg'),
            os = COALESCE(NULLIF(os, ''), 'Windows 11 Home')
        WHERE display IS NULL OR description IS NULL OR cpu IS NULL;
        
        
        -- 3. Update existing default variants & insert additional realistic ProductVariants
        -- Product 1: MacBook Air M3
        UPDATE product_variants
        SET ram = '16GB', rom = '256GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Space Black' LIMIT 1), price = 28990000.00, discount_price = 27490000.00, stock_quantity = 15
        WHERE product_id = 1 AND (sku = 'MAC-AIR-M3-16-256-BLK' OR sku LIKE 'PRODUCT-1-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (1, 'MAC-AIR-M3-16-512-BLK', (SELECT id FROM colors WHERE name = 'Space Black' LIMIT 1), '16GB', '512GB SSD', 33990000.00, 32490000.00, 10, true),
            (1, 'MAC-AIR-M3-24-512-SLV', (SELECT id FROM colors WHERE name = 'Silver' LIMIT 1), '24GB', '512GB SSD', 38990000.00, 37490000.00, 8, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 2: Lenovo Legion 5 Pro
        UPDATE product_variants
        SET ram = '16GB', rom = '512GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Space Gray' LIMIT 1), price = 38500000.00, discount_price = 36990000.00, stock_quantity = 12
        WHERE product_id = 2 AND (sku = 'LEGION-5PRO-16-512-GRY' OR sku LIKE 'PRODUCT-2-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (2, 'LEGION-5PRO-32-1TB-GRY', (SELECT id FROM colors WHERE name = 'Space Gray' LIMIT 1), '32GB', '1TB SSD', 42990000.00, 40990000.00, 7, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 3: Dell Precision 5680
        UPDATE product_variants
        SET ram = '32GB', rom = '1TB SSD', color_id = (SELECT id FROM colors WHERE name = 'Space Gray' LIMIT 1), price = 75000000.00, discount_price = 72000000.00, stock_quantity = 5
        WHERE product_id = 3 AND (sku = 'PRECISION-5680-32-1TB-GRY' OR sku LIKE 'PRODUCT-3-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (3, 'PRECISION-5680-64-2TB-GRY', (SELECT id FROM colors WHERE name = 'Space Gray' LIMIT 1), '64GB', '2TB SSD', 95000000.00, 89990000.00, 3, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 4: HP ProBook 450 G10
        UPDATE product_variants
        SET ram = '16GB', rom = '512GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Silver' LIMIT 1), price = 21500000.00, discount_price = 19990000.00, stock_quantity = 20
        WHERE product_id = 4 AND (sku = 'PROBOOK-450-16-512-SLV' OR sku LIKE 'PRODUCT-4-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (4, 'PROBOOK-450-32-1TB-SLV', (SELECT id FROM colors WHERE name = 'Silver' LIMIT 1), '32GB', '1TB SSD', 25500000.00, 23990000.00, 10, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 5: ASUS TUF Gaming F15
        UPDATE product_variants
        SET ram = '16GB', rom = '512GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Shadow Black' LIMIT 1), price = 26990000.00, discount_price = 24990000.00, stock_quantity = 14
        WHERE product_id = 5 AND (sku = 'ASUS-TUF-F15-16-512-BLK' OR sku LIKE 'PRODUCT-5-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (5, 'ASUS-TUF-F15-32-1TB-BLK', (SELECT id FROM colors WHERE name = 'Shadow Black' LIMIT 1), '32GB', '1TB SSD', 30990000.00, 28990000.00, 8, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 6: Acer Aspire 7 A715
        UPDATE product_variants
        SET ram = '8GB', rom = '512GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Shadow Black' LIMIT 1), price = 17990000.00, discount_price = 15990000.00, stock_quantity = 18
        WHERE product_id = 6 AND (sku = 'ASPIRE-7-8-512-BLK' OR sku LIKE 'PRODUCT-6-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (6, 'ASPIRE-7-16-512-BLK', (SELECT id FROM colors WHERE name = 'Shadow Black' LIMIT 1), '16GB', '512GB SSD', 19500000.00, 17490000.00, 12, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 7: MSI Creator Z16
        UPDATE product_variants
        SET ram = '32GB', rom = '1TB SSD', color_id = (SELECT id FROM colors WHERE name = 'Lunar Light' LIMIT 1), price = 52990000.00, discount_price = 48990000.00, stock_quantity = 6
        WHERE product_id = 7 AND (sku = 'MSI-Z16-32-1TB-WHT' OR sku LIKE 'PRODUCT-7-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (7, 'MSI-Z16-64-2TB-WHT', (SELECT id FROM colors WHERE name = 'Lunar Light' LIMIT 1), '64GB', '2TB SSD', 64990000.00, 59990000.00, 4, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 8: Razer Blade 16
        UPDATE product_variants
        SET ram = '32GB', rom = '1TB SSD', color_id = (SELECT id FROM colors WHERE name = 'Space Black' LIMIT 1), price = 89990000.00, discount_price = 84990000.00, stock_quantity = 5
        WHERE product_id = 8 AND (sku = 'RAZER-B16-32-1TB-BLK' OR sku LIKE 'PRODUCT-8-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (8, 'RAZER-B16-64-2TB-BLK', (SELECT id FROM colors WHERE name = 'Space Black' LIMIT 1), '64GB', '2TB SSD', 109990000.00, 102990000.00, 2, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 9: Samsung Galaxy Book 4 Pro
        UPDATE product_variants
        SET ram = '16GB', rom = '512GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Space Gray' LIMIT 1), price = 34990000.00, discount_price = 32990000.00, stock_quantity = 10
        WHERE product_id = 9 AND (sku = 'GALAXY-B4-16-512-GRY' OR sku LIKE 'PRODUCT-9-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (9, 'GALAXY-B4-32-1TB-GRY', (SELECT id FROM colors WHERE name = 'Space Gray' LIMIT 1), '32GB', '1TB SSD', 41990000.00, 38990000.00, 6, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 10: MSI Thin 15 B13UC
        UPDATE product_variants
        SET ram = '8GB', rom = '512GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Shadow Black' LIMIT 1), price = 18990000.00, discount_price = 16490000.00, stock_quantity = 15
        WHERE product_id = 10 AND (sku = 'MSI-THIN15-8-512-BLK' OR sku LIKE 'PRODUCT-10-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (10, 'MSI-THIN15-16-512-BLK', (SELECT id FROM colors WHERE name = 'Shadow Black' LIMIT 1), '16GB', '512GB SSD', 20490000.00, 17990000.00, 11, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 11: Microsoft Surface Laptop 6
        UPDATE product_variants
        SET ram = '16GB', rom = '512GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Platinum' LIMIT 1), price = 37990000.00, discount_price = 35990000.00, stock_quantity = 9
        WHERE product_id = 11 AND (sku = 'SURFACE-L6-16-512-PLT' OR sku LIKE 'PRODUCT-11-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (11, 'SURFACE-L6-32-1TB-BLK', (SELECT id FROM colors WHERE name = 'Space Black' LIMIT 1), '32GB', '1TB SSD', 47990000.00, 44990000.00, 5, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Product 12: Acer Aspire 5
        UPDATE product_variants
        SET ram = '16GB', rom = '512GB SSD', color_id = (SELECT id FROM colors WHERE name = 'Silver' LIMIT 1), price = 18500000.00, discount_price = 16490000.00, stock_quantity = 16
        WHERE product_id = 12 AND (sku = 'ASPIRE-5-16-512-SLV' OR sku LIKE 'PRODUCT-12-%');
        
        INSERT INTO product_variants (product_id, sku, color_id, ram, rom, price, discount_price, stock_quantity, is_active)
        VALUES 
            (12, 'ASPIRE-5-32-1TB-SLV', (SELECT id FROM colors WHERE name = 'Silver' LIMIT 1), '32GB', '1TB SSD', 22500000.00, 19990000.00, 8, true)
        ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, discount_price = EXCLUDED.discount_price, stock_quantity = EXCLUDED.stock_quantity;
        
        -- Reset sequence for product_variants
        PERFORM setval(
            pg_get_serial_sequence('product_variants', 'id'),
            COALESCE((SELECT MAX(id) FROM product_variants), 1)
        );
    END IF;
END $$;
