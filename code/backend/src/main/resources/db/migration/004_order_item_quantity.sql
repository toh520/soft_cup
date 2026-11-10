-- 手动点餐允许多份；历史套餐每道菜均一份，升级不改写原有营养快照。
ALTER TABLE order_items ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1 CHECK(quantity BETWEEN 1 AND 20);
