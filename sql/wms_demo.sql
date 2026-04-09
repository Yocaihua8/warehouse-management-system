USE wms;

DELETE FROM stock;
DELETE FROM customer;
DELETE FROM product;

INSERT INTO product (product_code, product_name, specification, unit, category, sale_price, remark, status)
VALUES
    ('P001', '十字螺丝刀', '中号', '把', '五金工具', 12.50, '演示数据', 1),
    ('P002', '一字螺丝刀', '大号', '把', '五金工具', 13.00, '演示数据', 1),
    ('P003', '活动扳手', '10寸', '把', '五金工具', 28.00, '演示数据', 1),
    ('P004', '电钻', '500W', '台', '电动工具', 168.00, '演示数据', 1),
    ('P005', 'PVC水管', '20mm', '根', '建材', 22.00, '演示数据', 1);

INSERT INTO customer (customer_code, customer_name, contact_person, phone, address, remark, status)
VALUES
    ('C001', '华星五金店', '张老板', '13800000001', '温州市鹿城区', '演示客户', 1),
    ('C002', '宏达建材商行', '李经理', '13800000002', '温州市瓯海区', '演示客户', 1),
    ('C003', '顺发机电', '王先生', '13800000003', '温州市龙湾区', '演示客户', 1);

INSERT INTO stock (product_id, quantity, warning_quantity)
VALUES
    (1, 120, 20),
    (2, 80, 20),
    (3, 50, 10),
    (4, 15, 5),
    (5, 200, 30);