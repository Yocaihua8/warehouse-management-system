USE wms;

-- 供应商演示数据
INSERT INTO supplier (supplier_code, supplier_name, contact_person, phone, address, remark, status)
VALUES
    ('SUP-DEMO-001', '演示供应商A', '张三', '13800000001', '演示地址A', 'MVP演示数据', 1),
    ('SUP-DEMO-002', '演示供应商B', '李四', '13800000002', '演示地址B', 'MVP演示数据', 1)
ON DUPLICATE KEY UPDATE
    supplier_name = VALUES(supplier_name),
    contact_person = VALUES(contact_person),
    phone = VALUES(phone),
    address = VALUES(address),
    remark = VALUES(remark),
    status = VALUES(status);

-- 客户演示数据
INSERT INTO customer (customer_code, customer_name, contact_person, phone, address, remark, status)
VALUES
    ('CUS-DEMO-001', '演示客户A', '王五', '13900000001', '演示客户地址A', 'MVP演示数据', 1),
    ('CUS-DEMO-002', '演示客户B', '赵六', '13900000002', '演示客户地址B', 'MVP演示数据', 1)
ON DUPLICATE KEY UPDATE
    customer_name = VALUES(customer_name),
    contact_person = VALUES(contact_person),
    phone = VALUES(phone),
    address = VALUES(address),
    remark = VALUES(remark),
    status = VALUES(status);

-- 商品演示数据
INSERT INTO product (product_code, product_name, specification, unit, category, sale_price, custom_fields_json, remark, status)
VALUES
    ('PRD-DEMO-001', '演示商品A', '500g', '袋', '演示分类', 12.50, NULL, 'MVP演示数据', 1),
    ('PRD-DEMO-002', '演示商品B', '1kg', '袋', '演示分类', 23.00, NULL, 'MVP演示数据', 1),
    ('PRD-DEMO-003', '演示商品C', '750ml', '瓶', '演示分类', 18.80, NULL, 'MVP演示数据', 1)
ON DUPLICATE KEY UPDATE
    product_name = VALUES(product_name),
    specification = VALUES(specification),
    unit = VALUES(unit),
    category = VALUES(category),
    sale_price = VALUES(sale_price),
    remark = VALUES(remark),
    status = VALUES(status);

-- 库存演示数据（存在则更新，不存在则新增）
INSERT INTO stock (product_id, quantity, warning_quantity)
SELECT p.id, 200, 20
FROM product p
WHERE p.product_code IN ('PRD-DEMO-001', 'PRD-DEMO-002', 'PRD-DEMO-003')
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    warning_quantity = VALUES(warning_quantity);

-- 角色账号兜底（与主初始化脚本保持一致）
INSERT INTO user (username, password, nickname, status, role)
VALUES ('admin', '123456', '管理员', 1, 'ADMIN')
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    status = VALUES(status),
    role = VALUES(role);

INSERT INTO user (username, password, nickname, status, role)
VALUES ('operator', '123456', '操作员', 1, 'OPERATOR')
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    status = VALUES(status),
    role = VALUES(role);

