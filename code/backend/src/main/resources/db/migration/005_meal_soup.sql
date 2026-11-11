-- 扩展汤分类时保留菜品 ID、订单快照和份数，重建引用表避免关闭外键校验。
CREATE TABLE dishes_next (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT NOT NULL CHECK(category IN ('staple','protein','vegetable','soup')), portion_g REAL NOT NULL CHECK(portion_g > 0), energy_kcal REAL NOT NULL CHECK(energy_kcal > 0), protein_g REAL NOT NULL CHECK(protein_g >= 0), fat_g REAL NOT NULL CHECK(fat_g >= 0), carbohydrate_g REAL NOT NULL CHECK(carbohydrate_g >= 0), ingredient_tags TEXT NOT NULL DEFAULT '[]', is_vegetarian INTEGER NOT NULL CHECK(is_vegetarian IN (0,1)), is_available INTEGER NOT NULL CHECK(is_available IN (0,1)));
INSERT INTO dishes_next SELECT * FROM dishes;
CREATE TABLE order_items_next (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL REFERENCES orders(id), dish_id INTEGER NOT NULL REFERENCES dishes_next(id), dish_name TEXT NOT NULL, portion_g REAL NOT NULL, energy_kcal REAL NOT NULL, protein_g REAL NOT NULL, fat_g REAL NOT NULL, carbohydrate_g REAL NOT NULL, quantity INTEGER NOT NULL DEFAULT 1 CHECK(quantity BETWEEN 1 AND 20), UNIQUE(order_id, dish_id));
INSERT INTO order_items_next SELECT * FROM order_items;
UPDATE sqlite_sequence SET seq=MAX(seq,COALESCE((SELECT seq FROM sqlite_sequence WHERE name='dishes'),0)) WHERE name='dishes_next';
UPDATE sqlite_sequence SET seq=MAX(seq,COALESCE((SELECT seq FROM sqlite_sequence WHERE name='order_items'),0)) WHERE name='order_items_next';
DROP TABLE order_items;
DROP TABLE dishes;
ALTER TABLE dishes_next RENAME TO dishes;
ALTER TABLE order_items_next RENAME TO order_items;
INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available) SELECT '番茄蛋花汤','soup',250,80,5,3,8,'["番茄","鸡蛋"]',1,1 WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name='番茄蛋花汤');
INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available) SELECT '紫菜豆腐汤','soup',250,70,6,2,7,'["紫菜","大豆"]',1,1 WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name='紫菜豆腐汤');
INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available) SELECT '冬瓜菌菇汤','soup',250,50,2,1,8,'["冬瓜","菌菇"]',1,1 WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name='冬瓜菌菇汤');
